package net.bradcarnage.ripoff.chatflow.gui;

import com.google.gson.*;
import net.bradcarnage.ripoff.chatflow.FlowChat;
import net.bradcarnage.ripoff.chatflow.SettingsManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Predicate;

import static net.bradcarnage.ripoff.chatflow.SettingsManager.loadFilterRules;

public class FlowChatConfig extends Screen {
    private final Screen parent;

    private final Predicate<String> filenameFilter = (string) -> {
        if (string.matches("[^\\w\\.]")) {
            return false;
        } else {
            return true;
        }
    };

    private final Predicate<String> numberFilter = (string) -> {
        if (string.length() == 0) {
            return true;
        } else {
            try {
                Integer.parseInt(string);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    };

    private ConfigButtonBooleanWidget filterChat;
    private ConfigTextWidget regexRuleFileField;

    public FlowChatConfig(Screen parent) {
        super(new LiteralText("FlowChat Config"));
        this.parent = parent;
    }

    @Override
    public void init(MinecraftClient client, int width, int height) {
        super.init(client, width, height);
        filterChat = addButton(new ConfigButtonBooleanWidget(10, 30, 170, 20, "filterChat"));
        regexRuleFileField = addChild(new ConfigTextWidget(textRenderer, filterChat.getRight() + 20, 30, 170, 20, "regexRuleFile"));
        regexRuleFileField.setTextPredicate(filenameFilter);

        addButton(new ButtonWidget(width - 110, height - 30, 100, 20, new LiteralText("Cancel"), (onPress) -> {
            MinecraftClient.getInstance().openScreen(parent);
        }));

        addButton(new ButtonWidget(width - 220, height - 30, 100, 20, new LiteralText("Confirm"), (onPress) -> {
            filterChat.saveValue();
            regexRuleFileField.saveValue();
            // if we loaded the filter rules successfully, return, else keep on same screen. (eventually log to text element)
            if (loadFilterRules()) {
                MinecraftClient.getInstance().openScreen(parent);
            }
        }));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        drawCenteredString(matrices, textRenderer, "FlowChat Config", width / 2, 10, 16777215);
        regexRuleFileField.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
