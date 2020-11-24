package net.bradcarnage.ripoff.chatflow.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.bradcarnage.ripoff.chatflow.FlowChat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.regex.Pattern;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    @ModifyVariable(method = "addMessage", at = @At("HEAD"), ordinal = 0)
    private Text injected(Text message) {
        boolean toastMe = false;
        String msg = message.getString().replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n").replaceAll("§\\w", "");
        String origmsg = msg;
        System.out.println("FlowChat incoming: "+msg);
        OrderedText orderedText = message.asOrderedText();
        System.out.println(orderedText);
        Style style = message.getStyle();
        System.out.println(style);
        for (JsonElement element: FlowChat.filter_rules.get("incoming").getAsJsonArray()) {
            JsonObject jobj = element.getAsJsonObject();
            if (Pattern.compile(jobj.get("search").getAsString()).matcher(msg).find()) { // if matches search regex
                if (jobj.has("respondMsg")) { // if response message
                    System.out.println("Responding to message according to "+jobj.get("search").getAsString());
                    // respond by sending response regex replacement (for regex capture/usage)
                    MinecraftClient.getInstance().player.sendChatMessage(msg.replaceAll(jobj.get("search").getAsString(), jobj.get("respondMsg").getAsString()));
                }
                if (!toastMe && jobj.has("toastMe") && jobj.get("toastMe").getAsBoolean()) {
                    System.out.println("Toastify message according to "+jobj.get("search").getAsString());
                    toastMe = true;
                }
            }
            msg = msg.replaceAll(jobj.get("search").getAsString(), jobj.get("replacement").getAsString());
        }
        System.out.println("filtered: "+msg);
        if (toastMe) {
            MinecraftClient.getInstance().player.sendMessage(Text.of(msg), true);
            return Text.of("pleasecancelthismessage");
        }
        if (!Objects.equals(msg, origmsg)) {
            return Text.of(msg);
        } else {
            return message;
        }
    }

    @Inject(at = @At("HEAD"), method = "addMessage")
    private void addMessage(Text message, CallbackInfo ci) {
        if (message.getString().equals("pleasecancelthismessage")) { ci.cancel(); }
    }
}
