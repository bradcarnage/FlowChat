package computer.brads.flowchat.fabric.mixin;

import computer.brads.flowchat.core.FlowChatRule;
import computer.brads.flowchat.core.MessageProcessor;
import computer.brads.flowchat.fabric.FabricChatHelper;
import computer.brads.flowchat.fabric.FlowChatFabric;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.Instant;
import java.util.List;

@Mixin(ClientPlayNetworkHandler.class)
public class OutgoingChatMixin {
    @ModifyVariable(method = "sendChatMessage", at = @At("HEAD"), ordinal = 0)
    private String flowchat$modifyOutgoing(String message) {
        if (FlowChatFabric.config == null || FlowChatFabric.config.isDisabled()) return message;

        List<FlowChatRule> rules = FlowChatFabric.config.getOutgoingRules();
        if (rules.isEmpty()) return message;

        String username = null;
        String serverName = "Singleplayer";
        var mcPlayer = net.minecraft.client.MinecraftClient.getInstance().player;
        if (mcPlayer != null) username = mcPlayer.getName().getString();
        var entry = net.minecraft.client.MinecraftClient.getInstance().getCurrentServerEntry();
        if (entry != null) serverName = entry.name;

        MessageProcessor.Result result = FlowChatFabric.processor.process(message, rules, FlowChatFabric.serverIp, username, serverName);
        if (!result.wasModified()) return message;

        if (result.toast || result.cancelled) {
            if (result.toast) FabricChatHelper.showActionBar(result.processedText);
            else FabricChatHelper.showLocalMessage(result.processedText);
            return "\u00a7flowchat\u00a7cancel";
        }

        FlowChatFabric.lastCmdSent = result.processedText;
        FlowChatFabric.whenLastCmdSent = Instant.now().toEpochMilli();
        return result.processedText;
    }

    @Inject(at = @At("HEAD"), method = "sendChatMessage", cancellable = true)
    private void flowchat$cancelMessage(String message, CallbackInfo ci) {
        if ("\u00a7flowchat\u00a7cancel".equals(message)) ci.cancel();
    }
}
