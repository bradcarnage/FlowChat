package computer.brads.flowchat.velocity;

import com.github.retrooper.packetevents.PacketEvents;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import computer.brads.flowchat.core.FlowChatConfig;
import computer.brads.flowchat.packet.ChatPacketInterceptor;
import io.github.retrooper.packetevents.velocity.factory.VelocityPacketEventsBuilder;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "flowchat", name = "FlowChat", version = "2.1.0",
        description = "Regex-powered chat processor for Velocity",
        authors = {"bradcarnage"}, url = "https://github.com/bradcarnage/FlowChat")
public class FlowChatVelocity {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private FlowChatConfig config;

    @Inject
    public FlowChatVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;

        // PacketEvents Velocity requires PluginContainer, not the plugin class
        PluginContainer container = server.getPluginManager().ensurePluginContainer(this);
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
        logger.info("FlowChat enabled on Velocity proxy");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) { PacketEvents.getAPI().terminate(); }
}
