package computer.brads.flowchat.bungee;

import com.github.retrooper.packetevents.PacketEvents;
import computer.brads.flowchat.core.FlowChatConfig;
import computer.brads.flowchat.core.FlowChatTestRunner;
import computer.brads.flowchat.packet.ChatPacketInterceptor;
import io.github.retrooper.packetevents.bungee.factory.BungeePacketEventsBuilder;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.List;

public class FlowChatBungee extends Plugin {
    private FlowChatConfig config;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(BungeePacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings().reEncodeByDefault(true).checkForUpdates(false);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        config = new FlowChatConfig(getDataFolder().toPath());
        config.load();

        String proxyName = "bungee:" + getProxy().getConfig().getListeners().iterator().next().getHost().getPort();
        PacketEvents.getAPI().getEventManager().registerListener(new ChatPacketInterceptor(config, proxyName));
        PacketEvents.getAPI().init();

        // Register /flowchat command
        getProxy().getPluginManager().registerCommand(this, new Command("flowchat", "flowchat.admin") {
            @Override
            public void execute(CommandSender sender, String[] args) {
                if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                    config.load();
                    sender.sendMessage(new TextComponent(ChatColor.GREEN + "[FlowChat] Config reloaded."));
                } else if (args.length > 0 && args[0].equalsIgnoreCase("toggle")) {
                    config.setDisabled(!config.isDisabled());
                    sender.sendMessage(new TextComponent(ChatColor.GREEN + "[FlowChat] " +
                            (config.isDisabled() ? "Disabled" : "Enabled")));
                } else if (args.length > 0 && args[0].equalsIgnoreCase("test")) {
                    runSelfTest(sender);
                } else {
                    sender.sendMessage(new TextComponent(ChatColor.YELLOW + "[FlowChat] Usage: /flowchat <reload|toggle|test>"));
                }
            }
        });

        getLogger().info("FlowChat " + getDescription().getVersion() + " enabled on BungeeCord");
    }

    @Override
    public void onDisable() { PacketEvents.getAPI().terminate(); }

    private void runSelfTest(CommandSender sender) {
        List<FlowChatTestRunner.TestResult> results = FlowChatTestRunner.runCommonTests();
        int passed = 0;
        for (FlowChatTestRunner.TestResult r : results) {
            if (r.passed) {
                passed++;
                sender.sendMessage(new TextComponent(ChatColor.GREEN + "  \u2713 " + r.number + ". " + r.name));
            } else {
                sender.sendMessage(new TextComponent(ChatColor.RED + "  \u2717 " + r.number + ". " + r.name + " \u2014 " + r.error));
            }
        }
        sender.sendMessage(new TextComponent(ChatColor.YELLOW + "[FlowChat Test] " + passed + "/" + results.size() + " passed"));
    }
}
