package net.bradcarnage.ripoff.chatflow.modmenu;

import io.github.prospector.modmenu.api.ConfigScreenFactory;
import io.github.prospector.modmenu.api.ModMenuApi;
import net.bradcarnage.ripoff.chatflow.gui.FlowChatConfig;

public class ModMenuEntrypointImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return FlowChatConfig::new;
    }
}
