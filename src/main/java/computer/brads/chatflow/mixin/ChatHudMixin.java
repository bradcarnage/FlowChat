package computer.brads.chatflow.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import computer.brads.chatflow.FlowChat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Mixin(ChatHud.class)
public class ChatHudMixin {

    @ModifyVariable(method = "addMessage", at = @At("HEAD"), ordinal = 0)
    private Text injected(Text message) {
        boolean toastMe = false;
        String msg = message.getString().replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n").replaceAll("§\\w", "");
        String origmsg = msg;
        try {
//        OrderedText orderedText = message.asOrderedText();
//        System.out.println(orderedText);
//        Style style = message.getStyle();
//        System.out.println(style);
            for (JsonElement element: FlowChat.filter_rules.get("incoming").getAsJsonArray()) {
                JsonObject jobj = element.getAsJsonObject();
                // optional "serversearch" regex filter.
                if (!jobj.has("serversearch") || FlowChat.server_ip.matches(jobj.get("serversearch").getAsString())) {
                    Matcher pmatch = Pattern.compile(jobj.get("search").getAsString()).matcher(msg);
                    String replstr = jobj.get("replacement").getAsString();
                    if (pmatch.find()) { // if matches search regex
                        if (jobj.has("respondMsg")) { // if response message
                            String sendcmd = "";
                            try { // try interpreting as a string, replace the singular message.
                                sendcmd = msg.replaceAll(jobj.get("search").getAsString(), jobj.get("respondMsg").getAsString());
                            } catch (Exception ignored) { // interpret as a json array
                                Integer loopiter = jobj.get("respondMsg").getAsJsonArray().size();
                                for (JsonElement moremsgs: jobj.get("respondMsg").getAsJsonArray()) {
                                    loopiter = loopiter-1;
                                    if (loopiter == 0) { // send the last message with this event trigger.
                                        sendcmd = msg.replaceAll(jobj.get("search").getAsString(), moremsgs.getAsString());
                                    } else { // send the other messages via other event triggers.
                                        MinecraftClient.getInstance().player.sendChatMessage(
                                                msg.replaceAll(jobj.get("search").getAsString(), moremsgs.getAsString())
                                        );
                                    }
                                }
                            }
                            if (!sendcmd.equals(FlowChat.last_cmd_sent) || (jobj.has("noAntiSpam") && jobj.get("noAntiSpam").getAsBoolean())) {
                                System.out.println("Sending "+sendcmd+" according to "+jobj.get("search").getAsString());
                                // respond by sending response regex replacement (for regex capture/usage)
                                MinecraftClient.getInstance().player.sendChatMessage(sendcmd);
                            } else {
                                System.out.println("Cancelling sending "+sendcmd+" due to AntiSpamFilter. noAntiSpam:true parameter to bypass");
                            }
                        }
                        if (!toastMe && jobj.has("toastMe") && jobj.get("toastMe").getAsBoolean()) {
//                        System.out.println("Toastify message according to "+jobj.get("search").getAsString());
                            toastMe = true;
                        }
//                    FlowChat.stacked_value_cacher.isEmpty();
                        try { // valuestack implementation (super bodgy; but ok)
                            String stackerrepl = jobj.get("replacement").getAsString();
                            JsonObject vstack = jobj.get("valuestack").getAsJsonObject();
                            if (vstack.has("stack_values")) {
                                if (vstack.has("ignore_diffs")) {
                                    for (JsonElement repl: vstack.get("ignore_diffs").getAsJsonArray()) {
                                        stackerrepl = stackerrepl.replaceAll("\\$"+repl.getAsInt(), "");
                                    }
                                }
                                stackerrepl = stackerrepl.replaceAll("\\$\\^i", "");
                                for (JsonElement repl: vstack.get("stack_values").getAsJsonArray()) {
                                    stackerrepl = stackerrepl.replaceAll("\\$"+repl.getAsInt(), "");
                                    stackerrepl = stackerrepl.replaceAll("\\$\\^"+repl.getAsInt(), "");
                                }
                                String stackermatcher = msg.replaceAll(jobj.get("search").getAsString(), stackerrepl);
//                                System.out.println("stackermatcher: "+stackermatcher);
                                int expire_sec = 4;
                                if (vstack.has("expire_after")) {
                                    expire_sec = vstack.get("expire_after").getAsInt();
                                }
                                if (!FlowChat.stacked_value_cacher.containsKey(stackermatcher)) {
                                    FlowChat.stacked_value_cacher.put(stackermatcher, new FlowChat.SVCP(expire_sec));
                                }
                                FlowChat.SVCP valcache = FlowChat.stacked_value_cacher.get(stackermatcher);
                                for (JsonElement repl: vstack.get("stack_values").getAsJsonArray()) {
                                    int rind = repl.getAsInt();
                                    Double stack_val = Double.parseDouble(pmatch.group(rind));
                                    if (valcache.stacked_values.containsKey(rind) &&
                                            valcache.expire_after_epoch > (int) (Instant.now().toEpochMilli()/1000)) {
                                        stack_val = stack_val+valcache.stacked_values.get(rind);
                                    }
                                    valcache.stacked_values.put(rind, stack_val);
                                    double pp_stack_val = new BigDecimal(stack_val).setScale(5, RoundingMode.HALF_EVEN).doubleValue();
                                    replstr = replstr.replaceAll("\\$\\^"+rind, String.valueOf(pp_stack_val).replaceFirst("\\.0$", ""));
                                }
                                if (valcache.expire_after_epoch > (int) (Instant.now().toEpochMilli()/1000)) {
                                    valcache.iter_count = valcache.iter_count+1;
                                } else {
                                    valcache.iter_count = 1;
                                }
                                replstr = replstr.replaceAll("\\$\\^i", String.valueOf(valcache.iter_count));
                                valcache.expire_after_epoch = (int) ((Instant.now().toEpochMilli()/1000)+expire_sec);
                                FlowChat.stacked_value_cacher.put(stackermatcher, valcache);
                            }
                        } catch (Exception ignored) { }
                    }
                    msg = msg.replaceAll(jobj.get("search").getAsString(), replstr);
                }
            }
            if (toastMe) { // run toast instead of chat log entry
                System.out.println("FlowChat toasted: "+msg+" ServerIP: "+ FlowChat.server_ip);
                MinecraftClient.getInstance().player.sendMessage(Text.of(msg), true);
                return Text.of("pleasecancelthismessage");
            }
            if (!Objects.equals(msg, origmsg)) {
                System.out.println("FlowChat changed incoming message from: "+origmsg+" to: "+message+" ServerIP: "+ FlowChat.server_ip);
                return Text.of(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return message;
    }

    @Inject(at = @At("HEAD"), method = "addMessage", cancellable = true)
    private void addMessage(Text message, CallbackInfo ci) {
        if (message.getString().equals("pleasecancelthismessage")) { ci.cancel(); }
    }
}
