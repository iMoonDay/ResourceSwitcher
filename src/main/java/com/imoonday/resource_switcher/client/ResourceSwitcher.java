package com.imoonday.resource_switcher.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.client.util.InputUtil;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

@Environment(EnvType.CLIENT)
public class ResourceSwitcherClient implements ClientModInitializer {

    public static final String MOD_ID = "resource_switcher";
    private static final String CATEGORY = "category." + MOD_ID + ".switcher";
    public static KeyBinding toggleAll;
    public static KeyBinding openScreen;
    public static KeyBinding selectExclusions;
    public static KeyBinding toggleLanguage;
    private final List<ResourcePackProfile> temporary = new ArrayList<>();
    private String mainLanguage;
    private String alternateLanguage;

    @Override
    public void onInitializeClient() {
        Config.loadConfig();

        registerToggleAllKey();
        registerOpenScreenKey();
        registerSelectExclusionsKey();
        registerToggleLanguageKey();
        //黑名单模式+白名单模式
        //配置文件
        //材质分组快捷键*3
    }

    private void registerToggleLanguageKey() {
        toggleLanguage = registerKey("toggleLanguage", GLFW.GLFW_KEY_I, (client, keyBinding) -> {
            if (keyBinding.isPressed()) {
                keyBinding.setPressed(false);
                LanguageManager languageManager = client.getLanguageManager();
                loadLanguageConfig(languageManager);
                String code = languageManager.getLanguage().equals(mainLanguage) ? alternateLanguage : mainLanguage;
                languageManager.setLanguage(code);
                client.reloadResources();
                if (client.player != null) {
                    client.player.sendMessage(createWithPrefix(Text.literal("已切换为 ").append(Objects.requireNonNull(languageManager.getLanguage(languageManager.getLanguage())).getDisplayText())));
                }
            }
        });
    }

    private void loadLanguageConfig(LanguageManager languageManager) {
        if (mainLanguage == null) {
            mainLanguage = languageManager.getLanguage();
        }
        if (alternateLanguage == null) {
            alternateLanguage = LanguageManager.DEFAULT_LANGUAGE_CODE;
        }
    }

    private void registerSelectExclusionsKey() {
        selectExclusions = registerKey("selectExclusions", GLFW.GLFW_KEY_N, (client, keyBinding) -> {
            while (keyBinding.wasPressed()) {
                openSelectExclusionsScreen(client);
            }
        });
    }

    private void registerOpenScreenKey() {
        openScreen = registerKey("openScreen", GLFW.GLFW_KEY_V, (client, keyBinding) -> {
            while (keyBinding.wasPressed()) {
                openResourcePackManagerScreen(client, null);
            }
        });
    }

    public static void openResourcePackManagerScreen(MinecraftClient client, Screen parent) {
        client.setScreen(new PackScreen(client.getResourcePackManager(), resourcePackManager -> {
            client.options.refreshResourcePacks(resourcePackManager);
            client.setScreen(parent);
        }, client.getResourcePackDir(), Text.translatable("resourcePack.title")));
    }

    private void registerToggleAllKey() {
        toggleAll = registerKey("toggleAll", GLFW.GLFW_KEY_R, (client, keyBinding) -> {
            if (client.getOverlay() == null && keyBinding.isPressed()) {
                keyBinding.setPressed(false);
                ResourcePackManager manager = client.getResourcePackManager();
                if (temporary.isEmpty()) {
                    for (ResourcePackProfile profile : manager.getEnabledProfiles()) {
                        if (!profile.isAlwaysEnabled() && !ExclusionSelectionScreen.EXCLUSIONS.contains(profile)) {
                            if (manager.disable(profile.getName())) {
                                temporary.add(profile);
                            }
                        }
                    }
                    if (client.player != null) {
                        boolean empty = temporary.isEmpty();
                        Text message = empty ? createWithPrefix(Text.translatable("text." + MOD_ID + ".disableAll.empty")) : createWithPrefix(Text.translatable("text." + MOD_ID + ".disableAll", temporary.size()).styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Texts.join(temporary, ResourcePackProfile::getDisplayName)))));
                        client.player.sendMessage(message);
                        if (empty) {
                            return;
                        }
                    }
                } else {
                    for (ResourcePackProfile profile : manager.getEnabledProfiles()) {
                        if (!profile.isAlwaysEnabled() && !ExclusionSelectionScreen.EXCLUSIONS.contains(profile)) {
                            manager.disable(profile.getName());
                        }
                    }
                    List<ResourcePackProfile> profiles = new ArrayList<>();
                    for (ResourcePackProfile profile : temporary) {
                        if (manager.enable(profile.getName())) {
                            profiles.add(profile);
                        }
                    }
                    temporary.clear();
                    if (client.player != null) {
                        client.player.sendMessage(createWithPrefix(Text.translatable("text." + MOD_ID + ".enableAll", profiles.size()).styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Texts.join(profiles, ResourcePackProfile::getDisplayName))))));
                    }
                }
                client.reloadResources();
            }
        });
    }

    public void openSelectExclusionsScreen(MinecraftClient client) {
        client.setScreen(new ExclusionSelectionScreen(client.currentScreen));
    }

    private KeyBinding registerKey(String id, int keyCode, @Nullable BiConsumer<MinecraftClient, KeyBinding> listener) {
        KeyBinding keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + MOD_ID + "." + id,
                InputUtil.Type.KEYSYM,
                keyCode,
                CATEGORY
        ));
        if (listener != null) {
            ClientTickEvents.END_CLIENT_TICK.register(client -> listener.accept(client, keyBinding));
        }
        return keyBinding;
    }

    public static Text createWithPrefix(Text text) {
        return Text.translatable("text." + MOD_ID + ".prefix").formatted(Formatting.YELLOW, Formatting.BOLD).append(text.copy().formatted(Formatting.WHITE).styled(style -> style.withBold(false)));
    }
}
