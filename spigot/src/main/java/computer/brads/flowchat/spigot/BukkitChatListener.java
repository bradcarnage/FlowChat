package computer.brads.flowchat.spigot;

import computer.brads.flowchat.core.FlowChatConfig;
import computer.brads.flowchat.core.MessageProcessor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.logging.Logger;

/**
 * Bukkit event-based chat listener — fallback for servers where PacketEvents
 * cannot inject (pre-1.13). Handles both incoming (server→player) and
 * outgoing (player→server) chat via Bukkit events.
 *
 * Limitations vs PE-based interception:
 * - Only intercepts player chat and console say/tellraw, not all server packets
 * - Cannot intercept system messages from other plugins
 * - Toast/overlay requires Bukkit's ActionBar API (1.11+) or title packet
 * - Sound requires Bukkit's playSound API
 */
public class BukkitChatListener implements Listener {
    private final FlowChatConfig config;
    private final MessageProcessor processor;
    private final Plugin plugin;
    private final Logger logger;
    private final String serverIdentifier;

    public BukkitChatListener(FlowChatConfig config, Plugin plugin, String serverIdentifier) {
        this.config = config;
        this.processor = new MessageProcessor();
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.serverIdentifier = serverIdentifier;
    }

    /**
     * Intercept outgoing player chat (player→server).
     * AsyncPlayerChatEvent fires before the message is broadcast.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (config.isDisabled()) return;

        String message = event.getMessage();
        if (message == null || message.isEmpty()) return;

        // Process through outgoing rules first
        List<computer.brads.flowchat.core.FlowChatRule> outgoingRules = config.getOutgoingRules();
        if (!outgoingRules.isEmpty()) {
            MessageProcessor.Result outResult = processor.process(
                    message, outgoingRules, serverIdentifier);
            if (outResult.wasModified()) {
                if (outResult.cancelled || outResult.toast) {
                    event.setCancelled(true);
                    if (outResult.toast) {
                        String colored = MessageProcessor.formatColors(outResult.processedText);
                        sendActionBar(event.getPlayer(), colored);
                    }
                    return;
                }
                if (!message.equals(outResult.processedText)) {
                    event.setMessage(MessageProcessor.formatColors(outResult.processedText));
                }
            }
        }

        // Process through incoming rules (modify what players see)
        List<computer.brads.flowchat.core.FlowChatRule> incomingRules = config.getIncomingRules();
        if (!incomingRules.isEmpty()) {
            String currentMsg = event.getMessage();
            MessageProcessor.Result inResult = processor.process(
                    currentMsg, incomingRules, serverIdentifier);
            if (inResult.wasModified()) {
                if (inResult.cancelled) {
                    event.setCancelled(true);
                    return;
                }
                if (inResult.toast) {
                    event.setCancelled(true);
                    String colored = MessageProcessor.formatColors(inResult.processedText);
                    sendActionBar(event.getPlayer(), colored);
                    return;
                }
                String colored = MessageProcessor.formatColors(inResult.processedText);
                // Modify the format to include the processed text
                event.setMessage(colored);

                if (inResult.playSound) {
                    playSound(event.getPlayer(), inResult.soundId);
                }

                for (String response : inResult.autoResponses) {
                    sendDelayedMessage(event.getPlayer(), response);
                }
            }
        }
    }

    /**
     * Intercept console commands like 'say' and 'tellraw' to process their output.
     * This handles the case where RCON sends 'say hello_test' — we intercept the
     * broadcast format before it reaches players.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerCommand(ServerCommandEvent event) {
        if (config.isDisabled()) return;
        // Skip if this is a RemoteServerCommandEvent — handled by onRemoteCommand
        // to avoid double processing (RemoteServerCommandEvent extends ServerCommandEvent)
        if (event instanceof RemoteServerCommandEvent) return;
        processSayCommand(event.getCommand(), event);
    }

    /**
     * Intercept RCON commands — on older servers (1.7.x), RCON commands may only
     * fire RemoteServerCommandEvent. This ensures coverage across all versions.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRemoteCommand(RemoteServerCommandEvent event) {
        if (config.isDisabled()) return;
        processSayCommand(event.getCommand(), event);
    }

    private void processSayCommand(String cmd, ServerCommandEvent event) {
        if (cmd == null) return;

        // Process 'say' commands — these broadcast to all players
        if (cmd.startsWith("say ")) {
            String message = cmd.substring(4);
            List<computer.brads.flowchat.core.FlowChatRule> incomingRules = config.getIncomingRules();
            if (!incomingRules.isEmpty()) {
                MessageProcessor.Result result = processor.process(
                        message, incomingRules, serverIdentifier);
                if (result.wasModified()) {
                    if (result.cancelled) {
                        event.setCancelled(true);
                        return;
                    }
                    if (result.toast) {
                        event.setCancelled(true);
                        String colored = MessageProcessor.formatColors(result.processedText);
                        // Send as actionbar to all online players
                        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                            sendActionBar(p, colored);
                        }
                        return;
                    }
                    if (!message.equals(result.processedText)) {
                        String colored = MessageProcessor.formatColors(result.processedText);
                        event.setCommand("say " + colored);
                    }
                    if (result.playSound) {
                        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                            playSound(p, result.soundId);
                        }
                    }
                    for (String response : result.autoResponses) {
                        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                            sendDelayedMessage(p, response);
                        }
                    }
                }
            }
        }
    }

    private void sendActionBar(org.bukkit.entity.Player player, String message) {
        // Use Spigot API if available (1.9+), otherwise fallback to chat
        try {
            player.spigot().sendMessage(
                    net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
        } catch (Exception e) {
            // Fallback: just send as regular message
            player.sendMessage(message);
        }
    }

    private void playSound(org.bukkit.entity.Player player, String soundId) {
        if (soundId == null) return;
        try {
            // Try to resolve sound
            String sound = soundId.contains(":") ? soundId.split(":", 2)[1] : soundId;
            // Use string-based playSound for cross-version compatibility
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception e) {
            logger.fine("Failed to play sound: " + soundId);
        }
    }

    private void sendDelayedMessage(org.bukkit.entity.Player player, String message) {
        // Send on next tick to avoid CME with async chat event
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.sendMessage(MessageProcessor.formatColors(message));
        });
    }
}
