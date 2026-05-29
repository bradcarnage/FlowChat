package computer.brads.chatflow.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import computer.brads.chatflow.ChatHelper;
import computer.brads.chatflow.FlowChat;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.text.NumberFormat;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Mixin(ChatHud.class)
public class ChatHudMixin {

    @Unique
    private boolean flowchat$processing = false;

    /**
     * Intercept ALL incoming chat messages via the 3-arg addMessage.
     * Process rules, modify text, handle toasts, and cancel if needed.
     */
    @Inject(
        method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void flowchat$interceptMessage(Text message, @Nullable MessageSignatureData signature, @Nullable MessageIndicator indicator, CallbackInfo ci) {
        if (flowchat$processing) return; // Recursion guard
        if (FlowChat.filter_rules == null || FlowChat.disabled) return;
        if (!FlowChat.filter_rules.has("incoming")) return;

        boolean toastMe = false;
        boolean playSound = false;
        String soundName = null;
        boolean anyMatch = false;

        String msg = message.getString()
                .replaceAll("\r", "\\\\r")
                .replaceAll("\n", "\\\\n")
                .replaceAll("§\\w", "");
        String origmsg = msg;

        try {
            for (JsonElement element : FlowChat.filter_rules.get("incoming").getAsJsonArray()) {
                JsonObject jobj = element.getAsJsonObject();

                if (jobj.has("serversearch") && !FlowChat.server_ip.matches(jobj.get("serversearch").getAsString())) {
                    continue;
                }

                Matcher pmatch = Pattern.compile(jobj.get("search").getAsString()).matcher(msg);
                String replstr = jobj.get("replacement").getAsString();

                if (pmatch.find()) {
                    anyMatch = true;

                    if (jobj.has("respondMsg")) {
                        handleAutoRespond(jobj, msg);
                    }

                    if (!toastMe && jobj.has("toastMe") && jobj.get("toastMe").getAsBoolean()) {
                        toastMe = true;
                    }

                    if (!playSound && jobj.has("playSound") && jobj.get("playSound").getAsBoolean()) {
                        playSound = true;
                        soundName = jobj.has("soundName") ? jobj.get("soundName").getAsString() : null;
                    }

                    replstr = handleValueStacking(jobj, pmatch, msg, replstr);
                    replstr = ChatHelper.replaceTags(replstr);
                    msg = msg.replaceAll(jobj.get("search").getAsString(), replstr);
                }
            }

            if (!anyMatch) return;

            if (playSound) {
                ChatHelper.playNotificationSound(soundName);
            }

            // Toast: show on action bar, suppress from chat
            if (toastMe) {
                FlowChat.LOGGER.debug("Toasted: {} (server: {})", msg, FlowChat.server_ip);
                ChatHelper.showActionBar(msg);
                ci.cancel();
                return;
            }

            // Message was modified — cancel original, re-call with new text
            if (!Objects.equals(msg, origmsg)) {
                FlowChat.LOGGER.debug("Modified incoming: {} -> {}", origmsg, msg);
                ci.cancel();
                flowchat$processing = true;
                try {
                    ((ChatHud)(Object)this).addMessage(Text.of(msg));
                } finally {
                    flowchat$processing = false;
                }
            }
        } catch (Exception e) {
            FlowChat.LOGGER.error("Error processing incoming message", e);
        }
    }

    @Unique
    private void handleAutoRespond(JsonObject jobj, String msg) {
        try {
            JsonElement respondElem = jobj.get("respondMsg");

            if (respondElem.isJsonArray()) {
                var arr = respondElem.getAsJsonArray();
                for (int i = 0; i < arr.size(); i++) {
                    String sendcmd = msg.replaceAll(
                            jobj.get("search").getAsString(),
                            ChatHelper.replaceTags(arr.get(i).getAsString()));
                    sendIfNotSpam(sendcmd, jobj);
                }
            } else {
                String sendcmd = msg.replaceAll(
                        jobj.get("search").getAsString(),
                        ChatHelper.replaceTags(respondElem.getAsString()));
                sendIfNotSpam(sendcmd, jobj);
            }
        } catch (Exception e) {
            FlowChat.LOGGER.error("Error sending auto-response", e);
        }
    }

    @Unique
    private void sendIfNotSpam(String cmd, JsonObject jobj) {
        boolean noAntiSpam = jobj.has("noAntiSpam") && jobj.get("noAntiSpam").getAsBoolean();
        if (!cmd.equals(FlowChat.last_cmd_sent) || noAntiSpam) {
            FlowChat.LOGGER.debug("Auto-responding: {}", cmd);
            ChatHelper.sendChat(cmd);
        } else {
            FlowChat.LOGGER.debug("Blocked duplicate auto-response: {}", cmd);
        }
    }

    @Unique
    private String handleValueStacking(JsonObject jobj, Matcher pmatch, String msg, String replstr) {
        try {
            if (!jobj.has("valuestack")) return replstr;

            String stackerrepl = jobj.get("replacement").getAsString();
            JsonObject vstack = jobj.get("valuestack").getAsJsonObject();
            String separator = vstack.has("seperate_float_with")
                    ? vstack.get("seperate_float_with").getAsString() : ".";

            if (!vstack.has("stack_values")) return replstr;

            if (vstack.has("ignore_diffs")) {
                for (JsonElement repl : vstack.get("ignore_diffs").getAsJsonArray()) {
                    stackerrepl = stackerrepl.replaceAll("\\$" + repl.getAsInt(), "");
                }
            }
            stackerrepl = stackerrepl.replaceAll("\\$\\^i", "");
            for (JsonElement repl : vstack.get("stack_values").getAsJsonArray()) {
                stackerrepl = stackerrepl.replaceAll("\\$" + repl.getAsInt(), "");
                stackerrepl = stackerrepl.replaceAll("\\$\\^" + repl.getAsInt(), "");
            }

            String stackermatcher = msg.replaceAll(jobj.get("search").getAsString(), stackerrepl);
            int expire_sec = vstack.has("expire_after") ? vstack.get("expire_after").getAsInt() : 4;

            if (!FlowChat.stacked_value_cacher.containsKey(stackermatcher)) {
                FlowChat.stacked_value_cacher.put(stackermatcher, new FlowChat.SVCP(expire_sec));
            }

            FlowChat.SVCP valcache = FlowChat.stacked_value_cacher.get(stackermatcher);
            int now = (int) (Instant.now().toEpochMilli() / 1000);

            for (JsonElement repl : vstack.get("stack_values").getAsJsonArray()) {
                int rind = repl.getAsInt();
                String raw = pmatch.group(rind).replace(separator, ".").replaceAll("[^\\d.]", "");
                double stack_val = Double.parseDouble(raw);

                if (valcache.stacked_values.containsKey(rind) && valcache.expire_after_epoch > now) {
                    stack_val += valcache.stacked_values.get(rind);
                }
                valcache.stacked_values.put(rind, stack_val);
                replstr = replstr.replaceAll("\\$\\^" + rind, NumberFormat.getInstance().format(stack_val));
            }

            if (valcache.expire_after_epoch > now) {
                valcache.iter_count++;
            } else {
                valcache.iter_count = 1;
            }
            replstr = replstr.replaceAll("\\$\\^i", String.valueOf(valcache.iter_count));

            valcache.expire_after_epoch = now + expire_sec;
            FlowChat.stacked_value_cacher.put(stackermatcher, valcache);

        } catch (Exception ignored) {}
        return replstr;
    }
}
