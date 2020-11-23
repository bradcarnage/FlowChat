package net.bradcarnage.ripoff.chatflow.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.bradcarnage.ripoff.chatflow.FlowChat;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Pattern;

import static net.minecraft.client.toast.SystemToast.Type.TUTORIAL_HINT;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    @ModifyVariable(method = "addMessage", at = @At("HEAD"), ordinal = 0)
    private Text injected(Text message) {
        boolean toastMe = false;
        String msg = message.getString();
        System.out.println("FlowChat incoming: "+msg);
        for (JsonElement element: FlowChat.filter_rules) {
            JsonObject jobj = element.getAsJsonObject();
//            System.out.println(jobj.get("description").getAsString());
            System.out.println(jobj.get("toastMe").getAsBoolean());
            if (!toastMe && jobj.get("toastMe").getAsBoolean() && Pattern.compile(jobj.get("search").getAsString()).matcher(msg).find()) {
                System.out.println("Toastify message according to "+jobj.get("search").getAsString());
                toastMe = true;
            }
            msg = msg.replaceAll(jobj.get("search").getAsString(), jobj.get("replacement").getAsString());
        }
        System.out.println("filtered: "+msg);
        if (toastMe) {
            MinecraftClient.getInstance().player.sendMessage(Text.of(msg), true);
            return Text.of("pleasecancelthismessageihavenoideahowtodoacallbackcancelherelol");
        }
        return Text.of(msg);
    }

    @Inject(at = @At("HEAD"), method = "addMessage")
    private void addMessage(Text message, CallbackInfo ci) {
        if (message.getString().equals("pleasecancelthismessageihavenoideahowtodoacallbackcancelherelol")) { ci.cancel(); }
    }
}
