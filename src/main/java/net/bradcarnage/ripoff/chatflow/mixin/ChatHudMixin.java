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

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    @ModifyVariable(method = "addMessage", at = @At("HEAD"), ordinal = 0)
    private Text injected(Text message) {
        String serverIp = "singleplayer";
        boolean toastMe = false;
        String msg = message.getString().replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n").replaceAll("§\\w", "");
        String origmsg = msg;
        try { serverIp = MinecraftClient.getInstance().getCurrentServerEntry().address; } catch (Exception ignored) { }
        try {
//        OrderedText orderedText = message.asOrderedText();
//        System.out.println(orderedText);
//        Style style = message.getStyle();
//        System.out.println(style);
            for (JsonElement element: FlowChat.filter_rules.get("incoming").getAsJsonArray()) {
                JsonObject jobj = element.getAsJsonObject();
                // optional "serversearch" regex filter.
                if (!jobj.has("serversearch") || serverIp.matches(jobj.get("serversearch").getAsString())) {
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
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            // too lazy to implement proper game ticking for antiAFK, so we're using the chat event. :p
            if (FlowChat.filter_rules.has("antiAFK")) {
                JsonObject jobj = FlowChat.filter_rules.get("antiAFK").getAsJsonObject();
                if (jobj.has("afterSeconds") && jobj.has("command")) {
                    System.out.println(FlowChat.when_last_cmd_sent+(jobj.get("afterSeconds").getAsLong()*1000));
                    System.out.println("vs");
                    System.out.println(Instant.now().toEpochMilli());
                    if (FlowChat.when_last_cmd_sent+(jobj.get("afterSeconds").getAsLong()*1000) < Instant.now().toEpochMilli()) {
                        System.out.println("Sending antiAFK message.");
                        MinecraftClient.getInstance().player.sendChatMessage(jobj.get("command").getAsString());
                    }
                }
            }
        } catch (Exception ignored) {};


        if (toastMe) { // run toast instead of chat log entry
            System.out.println("FlowChat toasted: "+msg+" ServerIP: "+serverIp);
            MinecraftClient.getInstance().player.sendMessage(Text.of(msg), true);
            return Text.of("pleasecancelthismessage");
        }
        if (!Objects.equals(msg, origmsg)) {
            System.out.println("FlowChat changed incoming message from: "+origmsg+" to: "+message+" ServerIP: "+serverIp);
            return Text.of(msg);
        } else {
            return message;
        }
    }

    @Inject(at = @At("HEAD"), method = "addMessage", cancellable = true)
    private void addMessage(Text message, CallbackInfo ci) {
        if (message.getString().equals("pleasecancelthismessage")) { ci.cancel(); }
    }
}
