package computer.brads.flowchat.velocity;

import com.github.retrooper.packetevents.PacketEvents;
import com.google.inject.Inject;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import computer.brads.flowchat.core.FlowChatConfig;
import computer.brads.flowchat.core.FlowChatTestRunner;
import computer.brads.flowchat.packet.ChatPacketInterceptor;
import io.github.retrooper.packetevents.velocity.factory.VelocityPacketEventsBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.List;

@Plugin(id = "flowchat", name = "FlowChat", version = "2.1.0",
        description = "Regex-powered chat processor for Velocity",
        authors = {"bradcarnage"}, url = "https://github.com/bradcarnage/FlowChat")
public class FlowChatVelocity {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private FlowChatConfig config;

    @Inject
    public FlowChatVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, PluginContainer container) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;

        PacketEvents.setAPI(VelocityPacketEventsBuilder.build(server, container, logger, dataDirectory));
        PacketEvents.getAPI().getSettings().reEncodeByDefault(true).checkForUpdates(false);
        PacketEvents.getAPI().load();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        config = new FlowChatConfig(dataDirectory);
        config.load();
        String proxyName = "velocity:" + server.getBoundAddress().getPort();
        PacketEvents.getAPI().getEventManager().registerListener(new ChatPacketInterceptor(config, proxyName));
        PacketEvents.getAPI().init();

        // Register /flowchat command
        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("flowchat").build(),
                new FlowChatCommand()
        );

        logger.info("FlowChat enabled on Velocity proxy");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) { PacketEvents.getAPI().terminate(); }

    private class FlowChatCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            String[] args = invocation.arguments();
            var source = invocation.source();

            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                config.load();
                source.sendMessage(Component.text("[FlowChat] Config reloaded.", NamedTextColor.GREEN));
            } else if (args.length > 0 && args[0].equalsIgnoreCase("toggle")) {
                config.setDisabled(!config.isDisabled());
                source.sendMessage(Component.text("[FlowChat] " +
                        (config.isDisabled() ? "Disabled" : "Enabled"), NamedTextColor.GREEN));
            } else if (args.length > 0 && args[0].equalsIgnoreCase("test")) {
                List<FlowChatTestRunner.TestResult> results = FlowChatTestRunner.runCommonTests();
                int passed = 0;
                for (FlowChatTestRunner.TestResult r : results) {
                    if (r.passed) {
                        passed++;
                        source.sendMessage(Component.text("  \u2713 " + r.number + ". " + r.name, NamedTextColor.GREEN));
                    } else {
                        source.sendMessage(Component.text("  \u2717 " + r.number + ". " + r.name + " \u2014 " + r.error, NamedTextColor.RED));
                    }
                }
                source.sendMessage(Component.text("[FlowChat Test] " + passed + "/" + results.size() + " passed", NamedTextColor.YELLOW));
            } else {
                source.sendMessage(Component.text("[FlowChat] Usage: /flowchat <reload|toggle|test>", NamedTextColor.YELLOW));
            }
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            return invocation.source().hasPermission("flowchat.admin");
        }
    }
}
