package computer.brads.flowchat.spigot;

import com.github.retrooper.packetevents.PacketEvents;
import computer.brads.flowchat.core.FlowChatConfig;
import computer.brads.flowchat.core.FlowChatTestRunner;
import computer.brads.flowchat.packet.ChatPacketInterceptor;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

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
        // Check enforce-secure-profile
        if (getServer().getOnlineMode()) {
            // If online mode, secure profile enforcement may be active
            getLogger().warning("[FlowChat] Server is in online-mode. Outgoing rule modifications " +
                    "may fail if enforce-secure-profile=true in server.properties.");
        }

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
            } else if (args.length > 0 && args[0].equalsIgnoreCase("test")) {
                runSelfTest(sender);
            } else {
                sender.sendMessage("\u00a7e[FlowChat] Usage: /flowchat <reload|toggle|test>");
            }
            return true;
        });

        getLogger().info("FlowChat " + getDescription().getVersion() + " enabled");
    }

    @Override
    public void onDisable() { PacketEvents.getAPI().terminate(); }

    private void runSelfTest(org.bukkit.command.CommandSender sender) {
        List<FlowChatTestRunner.TestResult> results = FlowChatTestRunner.runCommonTests();
        int passed = 0;
        for (FlowChatTestRunner.TestResult r : results) {
            if (r.passed) {
                passed++;
                sender.sendMessage("\u00a7a  \u2713 " + r.number + ". " + r.name);
            } else {
                sender.sendMessage("\u00a7c  \u2717 " + r.number + ". " + r.name + " \u2014 " + r.error);
            }
        }
        sender.sendMessage("\u00a7e[FlowChat Test] " + passed + "/" + results.size() + " passed");
    }
}
