package net.bradcarnage.ripoff.chatflow.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.bradcarnage.ripoff.chatflow.FlowChat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.time.Instant;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    @ModifyVariable(method = "sendChatMessage", at = @At("HEAD"), ordinal = 0)
    private String injected(String message) {
        String origmsg = message;
        String serverIp = "singleplayer";
        try { serverIp = MinecraftClient.getInstance().getCurrentServerEntry().address; } catch (Exception ignored) { }
        for (JsonElement element: FlowChat.filter_rules.get("outgoing").getAsJsonArray()) {
            JsonObject jobj = element.getAsJsonObject();
            // optional "serversearch" regex filter.
            if (!jobj.has("serversearch") || serverIp.matches(jobj.get("serversearch").getAsString())) {
                message = message.replaceAll(jobj.get("msgsearch").getAsString(), jobj.get("msgreplacement").getAsString());
            }
        }
        if (!origmsg.equals(message)) {
            System.out.println("FlowChat changed outgoing command from: "+origmsg+" to: "+message+" ServerIP: "+serverIp);
        }
        FlowChat.last_cmd_sent = message;
        FlowChat.when_last_cmd_sent = Instant.now().toEpochMilli();
        return message;
    }
}
