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
                            if (!localOnly && jobj.has("localOnly") && jobj.get("localOnly").getAsBoolean()) {
                                localOnly = true;
                            }
                            if (!toastMe && jobj.has("toastMe") && jobj.get("toastMe").getAsBoolean()) {
                                toastMe = true;
                            }
                            try { // try interpreting as a string, replace the singular message.
                                message = message.replaceAll(jobj.get("msgsearch").getAsString(), jobj.get("msgreplacement").getAsString()); // fix up message
                            } catch (Exception ignored) { // interpret as a json array
                                Integer loopiter = jobj.get("msgreplacement").getAsJsonArray().size();
                                for (JsonElement moremsgs: jobj.get("msgreplacement").getAsJsonArray()) {
                                    loopiter = loopiter-1;
                                    if (loopiter == 0) { // send the last message with this event trigger.
                                        message = message.replaceAll(jobj.get("msgsearch").getAsString(), moremsgs.getAsString());
                                    } else { // send the other messages via other event triggers.
                                        if (localOnly) { // run toast instead of sending chat message
                                            MinecraftClient.getInstance().player.sendMessage(Text.of(
                                                    message.replaceAll(jobj.get("msgsearch").getAsString(), moremsgs.getAsString())
                                            ), toastMe);
                                        } else {
                                            MinecraftClient.getInstance().player.sendChatMessage(
                                                    message.replaceAll(jobj.get("msgsearch").getAsString(), moremsgs.getAsString())
                                            );
                                        }
                                    }
                                }
                            }

                        }
                    }
                }
            }
            if (localOnly) { // run toast instead of sending chat message
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
