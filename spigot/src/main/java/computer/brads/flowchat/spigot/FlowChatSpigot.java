package computer.brads.flowchat.spigot;

import com.github.retrooper.packetevents.PacketEvents;
import computer.brads.flowchat.core.FlowChatConfig;
import computer.brads.flowchat.packet.ChatPacketInterceptor;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.java.JavaPlugin;

public class FlowChatSpigot extends JavaPlugin {
    private FlowChatConfig config;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings().reEncodeByDefault(true).checkForUpdates(false);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        config = new FlowChatConfig(getDataFolder().toPath());
        config.load();

        String serverName = getServer().getName() + ":" + getServer().getPort();
        PacketEvents.getAPI().getEventManager().registerListener(new ChatPacketInterceptor(config, serverName));
        PacketEvents.getAPI().init();

        getCommand("flowchat").setExecutor((sender, cmd, label, args) -> {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                config.load();
                sender.sendMessage("\u00a7a[FlowChat] Config reloaded.");
            } else if (args.length > 0 && args[0].equalsIgnoreCase("toggle")) {
                config.setDisabled(!config.isDisabled());
                sender.sendMessage("\u00a7a[FlowChat] " + (config.isDisabled() ? "Disabled" : "Enabled"));
            } else {
                sender.sendMessage("\u00a7e[FlowChat] Usage: /flowchat <reload|toggle>");
            }
            return true;
        });

        getLogger().info("FlowChat " + getDescription().getVersion() + " enabled");
    }

    @Override
    public void onDisable() { PacketEvents.getAPI().terminate(); }
}
