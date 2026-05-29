package computer.brads.flowchat.fabric.mixin;

import computer.brads.flowchat.core.FlowChatRule;
import computer.brads.flowchat.core.MessageProcessor;
import computer.brads.flowchat.fabric.FabricChatHelper;
import computer.brads.flowchat.fabric.FlowChatFabric;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatHud.class)
public class ChatHudMixin {
    @Unique private boolean flowchat$processing = false;

    @Inject(
        method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
        at = @At("HEAD"), cancellable = true
    )
    private void flowchat$interceptMessage(Text message, @Nullable MessageSignatureData signature,
                                            @Nullable MessageIndicator indicator, CallbackInfo ci) {
        if (flowchat$processing) return;
        if (FlowChatFabric.config == null || FlowChatFabric.config.isDisabled()) return;

        List<FlowChatRule> rules = FlowChatFabric.config.getIncomingRules();
        if (rules.isEmpty()) return;

        String plainText = message.getString();

        // Feature #6: Extract raw JSON for matchJson rules
        String rawJson = null;
        try {
            rawJson = Text.Serialization.toJsonString(message, MinecraftClient.getInstance().world.getRegistryManager());
        } catch (Exception ignored) {
            // Fallback if world not available — just skip JSON matching for this message
        }

        String username = null;
        String serverName = "Singleplayer";
        var player = MinecraftClient.getInstance().player;
        if (player != null) username = player.getName().getString();
        var entry = MinecraftClient.getInstance().getCurrentServerEntry();
        if (entry != null) serverName = entry.name;

        MessageProcessor.Result result = FlowChatFabric.processor.process(
                plainText, rules, FlowChatFabric.serverIp, username, serverName, rawJson);

        if (!result.wasModified()) return;

        if (result.playSound) FabricChatHelper.playNotificationSound(result.soundId);

        for (String resp : result.autoResponses) {
            if (!resp.equals(FlowChatFabric.lastCmdSent)) FabricChatHelper.sendChat(resp);
        }

        if (result.toast) {
            String notifyText = MessageProcessor.formatColors(result.processedText);
            FabricChatHelper.showNotification(notifyText, result.notifyStyle);
            ci.cancel();
            return;
        }

        if (!plainText.equals(result.processedText)) {
            ci.cancel();
            flowchat$processing = true;
            try { ((ChatHud)(Object)this).addMessage(Text.of(MessageProcessor.formatColors(result.processedText))); }
            finally { flowchat$processing = false; }
        }
    }
}
