package computer.brads.flowchat.spigot;

import computer.brads.flowchat.core.FlowChatConfig;
import computer.brads.flowchat.core.FlowChatTestRunner;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class FlowChatSpigot extends JavaPlugin {
    private FlowChatConfig config;
    private boolean usingPacketEvents = false;

    @Override
    public void onLoad() {
        // Try to load PacketEvents — may not be available on pre-1.13 servers
        if (isPacketEventsAvailable()) {
            try {
                PacketEventsLoader.load(this);
                usingPacketEvents = true;
            } catch (Throwable e) {
                getLogger().warning("[FlowChat] PacketEvents load failed: " + e.getMessage());
                getLogger().info("[FlowChat] Will use Bukkit events for chat interception.");
            }
        } else {
            getLogger().info("[FlowChat] PacketEvents not found. Using Bukkit event-based chat interception.");
        }
    }

    @Override
    public void onEnable() {
        // Check enforce-secure-profile
        if (getServer().getOnlineMode()) {
            getLogger().warning("[FlowChat] Server is in online-mode. Outgoing rule modifications " +
                    "may fail if enforce-secure-profile=true in server.properties.");
        }

        config = new FlowChatConfig(getDataFolder().toPath());
        config.load();

        String serverName = getServer().getName() + ":" + getServer().getPort();

        if (usingPacketEvents) {
            try {
                PacketEventsLoader.init(config, serverName);
                getLogger().info("[FlowChat] Using PacketEvents for packet-level chat interception.");
            } catch (Throwable e) {
                getLogger().warning("[FlowChat] PacketEvents init failed: " + e.getMessage());
                getLogger().info("[FlowChat] Falling back to Bukkit events.");
                usingPacketEvents = false;
                registerBukkitListener(serverName);
            }
        } else {
            registerBukkitListener(serverName);
        }

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
    public void onDisable() {
        if (usingPacketEvents) {
            try {
                PacketEventsLoader.terminate();
            } catch (Exception ignored) {}
        }
    }

    private boolean isPacketEventsAvailable() {
        try {
            Class.forName("com.github.retrooper.packetevents.PacketEvents");
            return getServer().getPluginManager().getPlugin("packetevents") != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void registerBukkitListener(String serverName) {
        getServer().getPluginManager().registerEvents(
                new BukkitChatListener(config, this, serverName), this);
    }

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
