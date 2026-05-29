package computer.brads.chatflow.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import computer.brads.chatflow.ChatHelper;
import computer.brads.chatflow.FlowChat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.Instant;
import java.util.regex.Pattern;

/**
 * Intercepts outgoing chat messages for regex replacement.
 * In 1.21.x, chat messages go through ClientPlayNetworkHandler.sendChatMessage(String).
 * Commands go through sendCommand(String) separately (since 1.19.1).
 */
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayerEntityMixin {

    @ModifyVariable(method = "sendChatMessage", at = @At("HEAD"), ordinal = 0)
    private String flowchat$modifyOutgoing(String message) {
        if (FlowChat.filter_rules == null || FlowChat.disabled) return message;
        if (!FlowChat.filter_rules.has("outgoing")) return message;

        boolean localOnly = false;
        boolean toastMe = false;
        String origmsg = message;

        try {
            for (JsonElement element : FlowChat.filter_rules.get("outgoing").getAsJsonArray()) {
                JsonObject jobj = element.getAsJsonObject();

                if (!jobj.has("msgsearch") || !jobj.has("msgreplacement")) continue;

                // Optional server filter
                if (jobj.has("serversearch") && !FlowChat.server_ip.matches(jobj.get("serversearch").getAsString())) {
                    continue;
                }

                if (!Pattern.compile(jobj.get("msgsearch").getAsString()).matcher(message).find()) continue;

                // Check flags
                if (!localOnly && jobj.has("localOnly") && jobj.get("localOnly").getAsBoolean()) {
                    localOnly = true;
                }
                if (!toastMe && jobj.has("toastMe") && jobj.get("toastMe").getAsBoolean()) {
                    toastMe = true;
                }

                // Apply replacement (string or array)
                JsonElement replElem = jobj.get("msgreplacement");
                if (replElem.isJsonArray()) {
                    var arr = replElem.getAsJsonArray();
                    for (int i = 0; i < arr.size(); i++) {
                        String replaced = message.replaceAll(
                                jobj.get("msgsearch").getAsString(),
                                ChatHelper.replaceTags(arr.get(i).getAsString()));
                        if (i < arr.size() - 1) {
                            // Send extra messages
                            if (localOnly) {
                                if (toastMe) {
                                    ChatHelper.showActionBar(replaced);
                                } else {
                                    ChatHelper.showLocalMessage(replaced);
                                }
                            } else {
                                ChatHelper.sendChat(replaced);
                            }
                        } else {
                            // Last one becomes the main message
                            message = replaced;
                        }
                    }
                } else {
                    message = message.replaceAll(
                            jobj.get("msgsearch").getAsString(),
                            ChatHelper.replaceTags(replElem.getAsString()));
                }
            }

            if (localOnly) {
                // Show locally instead of sending
                if (toastMe) {
                    ChatHelper.showActionBar(message);
                } else {
                    ChatHelper.showLocalMessage(message);
                }
                // Cancel by returning marker
                message = "§flowchat§cancel";
            } else {
                FlowChat.last_cmd_sent = message;
                FlowChat.when_last_cmd_sent = Instant.now().toEpochMilli();
            }

            if (!origmsg.equals(message) && !message.equals("§flowchat§cancel")) {
                FlowChat.LOGGER.debug("Modified outgoing: {} -> {}", origmsg, message);
            }
        } catch (Exception e) {
            FlowChat.LOGGER.error("Error processing outgoing message", e);
        }

        return message;
    }

    @Inject(at = @At("HEAD"), method = "sendChatMessage", cancellable = true)
    private void flowchat$cancelMessage(String message, CallbackInfo ci) {
        if ("§flowchat§cancel".equals(message)) {
            ci.cancel();
        }
    }
}
