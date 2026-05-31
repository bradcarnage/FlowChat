package computer.brads.flowchat.fabric.mixin;

import computer.brads.flowchat.core.FlowChatRule;
import computer.brads.flowchat.core.MessageProcessor;
import computer.brads.flowchat.fabric.FabricChatHelper;
import computer.brads.flowchat.fabric.FlowChatFabric;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.resources.RegistryOps;
import com.mojang.serialization.JsonOps;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatComponent.class)
public class ChatHudMixin {
    @Unique private boolean flowchat$processing = false;

    @Inject(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
        at = @At("HEAD"), cancellable = true
    )
    private void flowchat$interceptMessage(Component message, @Nullable MessageSignature signature,
                                            GuiMessageSource source, @Nullable GuiMessageTag indicator, CallbackInfo ci) {
        if (flowchat$processing) return;
        if (FlowChatFabric.config == null || FlowChatFabric.config.isDisabled()) return;

        List<FlowChatRule> rules = FlowChatFabric.config.getIncomingRules();
        if (rules.isEmpty()) return;

        String plainText = message.getString();

        // Extract raw JSON for matchJson rules
        String rawJson = null;
        try {
            var ops = RegistryOps.create(JsonOps.INSTANCE, Minecraft.getInstance().level.registryAccess());
            rawJson = ComponentSerialization.CODEC.encodeStart(ops, message).result().map(e -> e.toString()).orElse(null);
        } catch (Exception ignored) {}

        String username = null;
        String serverName = "Singleplayer";
        var player = Minecraft.getInstance().player;
        if (player != null) username = player.getName().getString();
        var entry = Minecraft.getInstance().getCurrentServer();
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
            try { ((ChatComponent)(Object)this).addClientSystemMessage(Component.literal(MessageProcessor.formatColors(result.processedText))); }
            finally { flowchat$processing = false; }
        }
    }
}
