package net.bradcarnage.ripoff.chatflow.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.bradcarnage.ripoff.chatflow.FlowChat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.regex.Pattern;

import static net.bradcarnage.ripoff.chatflow.FlowChat.server_ip;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    @ModifyVariable(method = "addMessage", at = @At("HEAD"), ordinal = 0)
    private Text injected(Text message) {
        boolean toastMe = false;
        String msg = message.getString().replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n").replaceAll("§\\w", "");
        String origmsg = msg;
        try {
//        OrderedText orderedText = message.asOrderedText();
//        System.out.println(orderedText);
//        Style style = message.getStyle();
//        System.out.println(style);
            for (JsonElement element: FlowChat.filter_rules.get("incoming").getAsJsonArray()) {
                JsonObject jobj = element.getAsJsonObject();
                // optional "serversearch" regex filter.
                if (!jobj.has("serversearch") || server_ip.matches(jobj.get("serversearch").getAsString())) {
                    if (Pattern.compile(jobj.get("search").getAsString()).matcher(msg).find()) { // if matches search regex
                        if (jobj.has("respondMsg")) { // if response message
                            String sendcmd = msg.replaceAll(jobj.get("search").getAsString(), jobj.get("respondMsg").getAsString());
                            if (!sendcmd.equals(FlowChat.last_cmd_sent) || (jobj.has("noAntiSpam") && jobj.get("noAntiSpam").getAsBoolean())) {
                                System.out.println("Sending "+sendcmd+" according to "+jobj.get("search").getAsString());
                                // respond by sending response regex replacement (for regex capture/usage)
                                MinecraftClient.getInstance().player.sendChatMessage(sendcmd);
                            } else {
                                System.out.println("Cancelling sending "+sendcmd+" due to AntiSpamFilter. noAntiSpam:true parameter to bypass");
                            }
                        }
                        if (!toastMe && jobj.has("toastMe") && jobj.get("toastMe").getAsBoolean()) {
//                        System.out.println("Toastify message according to "+jobj.get("search").getAsString());
                            toastMe = true;
                        }
                    }
                    msg = msg.replaceAll(jobj.get("search").getAsString(), jobj.get("replacement").getAsString());
                }
            }
            if (toastMe) { // run toast instead of chat log entry
                System.out.println("FlowChat toasted: "+msg+" ServerIP: "+server_ip);
                MinecraftClient.getInstance().player.sendMessage(Text.of(msg), true);
                return Text.of("pleasecancelthismessage");
            }
            if (!Objects.equals(msg, origmsg)) {
                System.out.println("FlowChat changed incoming message from: "+origmsg+" to: "+message+" ServerIP: "+server_ip);
                return Text.of(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return message;
    }

    @Inject(at = @At("HEAD"), method = "addMessage", cancellable = true)
    private void addMessage(Text message, CallbackInfo ci) {
        if (message.getString().equals("pleasecancelthismessage")) { ci.cancel(); }
    }
}
