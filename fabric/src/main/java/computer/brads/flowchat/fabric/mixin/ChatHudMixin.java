package computer.brads.flowchat.fabric.mixin;

import computer.brads.flowchat.core.FlowChatRule;
import computer.brads.flowchat.core.MessageProcessor;
import computer.brads.flowchat.fabric.FabricChatHelper;
import computer.brads.flowchat.fabric.FlowChatFabric;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatHud.class)
public class ChatHudMixin {
    @Unique private boolean flowchat$processing = false;

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
    private void flowchat$interceptMessage(Text message, CallbackInfo ci) {
        if (flowchat$processing) return;
        if (FlowChatFabric.config == null || FlowChatFabric.config.isDisabled()) return;

        List<FlowChatRule> rules = FlowChatFabric.config.getIncomingRules();
        if (rules.isEmpty()) return;

        String plainText = message.getString();
        MessageProcessor.Result result = FlowChatFabric.processor.process(plainText, rules, FlowChatFabric.serverIp);
        if (!result.wasModified()) return;

        if (result.playSound) FabricChatHelper.playNotificationSound(result.soundName);
        for (String resp : result.autoResponses) {
            if (!resp.equals(FlowChatFabric.lastCmdSent)) FabricChatHelper.sendChat(resp);
        }

        if (result.toastMe) {
            FabricChatHelper.showActionBar(result.processedText);
            ci.cancel();
            return;
        }

        if (!plainText.equals(result.processedText)) {
            ci.cancel();
            flowchat$processing = true;
            try { ((ChatHud)(Object)this).addMessage(new net.minecraft.text.LiteralText(result.processedText)); }
            finally { flowchat$processing = false; }
        }
    }
}
