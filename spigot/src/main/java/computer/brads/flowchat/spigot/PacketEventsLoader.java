package computer.brads.flowchat.spigot;

import com.github.retrooper.packetevents.PacketEvents;
import computer.brads.flowchat.core.FlowChatConfig;
import computer.brads.flowchat.packet.ChatPacketInterceptor;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Isolated PE loader — all PE imports are in this class so that
 * FlowChatSpigot doesn't trigger ClassNotFoundException when PE
 * is not present (pre-1.13 servers).
 */
public class PacketEventsLoader {
    public static void load(JavaPlugin plugin) {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(plugin));
        PacketEvents.getAPI().getSettings().reEncodeByDefault(true).checkForUpdates(false);
        PacketEvents.getAPI().load();
    }

    public static void init(FlowChatConfig config, String serverName) {
        PacketEvents.getAPI().getEventManager().registerListener(
                new ChatPacketInterceptor(config, serverName));
        PacketEvents.getAPI().init();
    }

    public static void terminate() {
        PacketEvents.getAPI().terminate();
    }
}
