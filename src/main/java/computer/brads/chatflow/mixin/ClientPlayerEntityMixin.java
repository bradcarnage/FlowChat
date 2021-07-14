package computer.brads.chatflow.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import computer.brads.chatflow.FlowChat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.Instant;
import java.util.regex.Pattern;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    @ModifyVariable(method = "sendChatMessage", at = @At("HEAD"), ordinal = 0)
    private String injected(String message) {
        boolean localOnly = false;
        boolean toastMe = false;
        String origmsg = message;
        try {
            for (JsonElement element: FlowChat.filter_rules.get("outgoing").getAsJsonArray()) {
                JsonObject jobj = element.getAsJsonObject();
                // optional "serversearch" regex filter.
                if (jobj.has("msgsearch") && jobj.has("msgreplacement")) {
                    if (!jobj.has("serversearch") || FlowChat.server_ip.matches(jobj.get("serversearch").getAsString())) { // if matches server ip
                        if (Pattern.compile(jobj.get("msgsearch").getAsString()).matcher(message).find()) { // if matches message search regex
                            message = message.replaceAll(jobj.get("msgsearch").getAsString(), jobj.get("msgreplacement").getAsString()); // fix up message
                            if (!localOnly && jobj.has("localOnly") && jobj.get("localOnly").getAsBoolean()) {
                                localOnly = true;
                            }
                            if (!toastMe && jobj.has("toastMe") && jobj.get("toastMe").getAsBoolean()) {
                                toastMe = true;
                            }
                        }
                    }
                }
            }
            if (localOnly) { // run toast instead of sending chat message
                System.out.println("FlowChat locally sending: "+message+" ServerIP: "+ FlowChat.server_ip+" Toasting: "+toastMe);
                MinecraftClient.getInstance().player.sendMessage(Text.of(message), toastMe);
                message = "pleasecancelthismessage";
            } else {
                FlowChat.last_cmd_sent = message;
                FlowChat.when_last_cmd_sent = Instant.now().toEpochMilli();
            }
            if (!origmsg.equals(message)) {
                System.out.println("FlowChat changed outgoing command from: "+origmsg+" to: "+message+" ServerIP: "+ FlowChat.server_ip);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return message;
    }

    @Inject(at = @At("HEAD"), method = "sendChatMessage", cancellable = true)
    private void sendChatMessage(String message, CallbackInfo ci) {
        if (message.equals("pleasecancelthismessage")) { ci.cancel(); }
    }
}
