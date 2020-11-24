package net.bradcarnage.ripoff.chatflow.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.bradcarnage.ripoff.chatflow.FlowChat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    @ModifyVariable(method = "sendChatMessage", at = @At("HEAD"), ordinal = 0)
    private String injected(String message) {
        String serverIp = "singleplayer";
        try { serverIp = MinecraftClient.getInstance().getCurrentServerEntry().address; } catch (Exception ignored) { }
        System.out.println("FlowChat outgoing: "+message+" ServerIP: "+serverIp);
        for (JsonElement element: FlowChat.filter_rules.get("outgoing").getAsJsonArray()) {
            JsonObject jobj = element.getAsJsonObject();
            if (serverIp.matches(jobj.get("serversearch").getAsString())) {
                message = message.replaceAll(jobj.get("msgsearch").getAsString(), jobj.get("msgreplacement").getAsString());
            }
        }
        System.out.println("FlowChat filtered: "+message);
        return message;
    }
}
