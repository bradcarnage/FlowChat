package computer.brads.flowchat.bungee;

import com.github.retrooper.packetevents.PacketEvents;
import computer.brads.flowchat.core.FlowChatConfig;
import computer.brads.flowchat.packet.ChatPacketInterceptor;
import io.github.retrooper.packetevents.bungee.factory.BungeePacketEventsBuilder;
import net.md_5.bungee.api.plugin.Plugin;

import java.nio.file.Path;

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

        getLogger().info("FlowChat " + getDescription().getVersion() + " enabled on BungeeCord");
    }

    @Override
    public void onDisable() { PacketEvents.getAPI().terminate(); }
}
