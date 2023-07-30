package com.imoonday.resource_switcher.client;

import com.imoonday.resource_switcher.client.screens.ExclusionSelectionScreen;
import com.imoonday.resource_switcher.client.screens.LanguageSwitchSettingScreen;
import com.imoonday.resource_switcher.client.screens.ResourcePackGroupScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public class ResourceSwitcher implements ClientModInitializer {

    public static final String MOD_ID = "resource_switcher";
    private long saveTime;

    @Override
    public void onInitializeClient() {
        Config.initConfig();
        KeyBindings.registerKeys();
        Commands.registerCommands();
        registerAutoSave();
    }

    private void registerAutoSave() {
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Config.save());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> Config.save());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            int time = Config.SETTINGS.autoSaveTime;
            if (time <= 0) {
                return;
            }
            if (Util.getMeasuringTimeMs() - saveTime > time * 60L * 1000L) {
                boolean success = Config.save();
                if (Config.SETTINGS.displaySaveLog && client.player != null) {
                    client.player.sendMessage(createWithPrefix(Text.translatable(success ? "text.resource_switcher.displaySaveLog.success" : "text.resource_switcher.displaySaveLog.failed")));
                }
                this.saveTime = Util.getMeasuringTimeMs();
            }
        });
    }

    public static void openResourcePackGroupScreen(MinecraftClient client) {
        client.setScreen(new ResourcePackGroupScreen(client.currentScreen, client.getResourcePackManager()));
    }

    public static void openSelectExclusionsScreen(MinecraftClient client) {
        client.setScreen(new ExclusionSelectionScreen(client.currentScreen, client.getResourcePackManager()));
    }

    public static void openLanguageSwitchSettingScreen(MinecraftClient client) {
        client.setScreen(new LanguageSwitchSettingScreen(client.currentScreen, client.getLanguageManager()));
    }

    public static void openResourcePackManagerScreen(MinecraftClient client, Screen parent) {
        client.setScreen(new PackScreen(client.getResourcePackManager(), resourcePackManager -> {
            client.options.refreshResourcePacks(resourcePackManager);
            client.setScreen(parent);
        }, client.getResourcePackDir(), Text.translatable("resourcePack.title")));
    }

    public static void openLanguageOptionsScreen(MinecraftClient client, Screen parent) {
        client.setScreen(new LanguageOptionsScreen(parent, client.options, client.getLanguageManager()));
    }

    public static Text createWithPrefix(Text text, Formatting... formattings) {
        return Text.translatable("text." + MOD_ID + ".prefix").formatted(Formatting.YELLOW, Formatting.BOLD).append(text.copy().styled(style -> style.withBold(false)).formatted(formattings));
    }

    public static Text createWithPrefix(Text text) {
        return createWithPrefix(text, Formatting.WHITE);
    }
}
