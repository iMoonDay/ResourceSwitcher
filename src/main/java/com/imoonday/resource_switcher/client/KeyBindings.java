package com.imoonday.resource_switcher.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.client.util.InputUtil;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Language;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static com.imoonday.resource_switcher.client.ResourceSwitcher.MOD_ID;

@Environment(EnvType.CLIENT)
public class KeyBindings {

    private static final String CATEGORY = "category.resource_switcher.switcher";

    public static KeyBinding toggleAll;
    public static KeyBinding openScreen;
    public static KeyBinding selectExclusions;
    public static KeyBinding toggleLanguage;
    public static KeyBinding openLanguageSetting;
    public static KeyBinding openGroupSetting;

    public static KeyBinding useGroup1 = registerKey("useGroup1", GLFW.GLFW_KEY_UNKNOWN, (client, keyBinding) -> {
        while (keyBinding.wasPressed()) {
            modifyResourcePacks(Group.GROUP_1, client);
        }
    });

    public static KeyBinding useGroup2 = registerKey("useGroup2", GLFW.GLFW_KEY_UNKNOWN, (client, keyBinding) -> {
        while (keyBinding.wasPressed()) {
            modifyResourcePacks(Group.GROUP_2, client);
        }
    });

    public static KeyBinding useGroup3 = registerKey("useGroup3", GLFW.GLFW_KEY_UNKNOWN, (client, keyBinding) -> {
        while (keyBinding.wasPressed()) {
            modifyResourcePacks(Group.GROUP_3, client);
        }
    });

    public static void registerKeys() {
        registerToggleAllKey();
        registerOpenScreenKey();
        registerSelectExclusionsKey();
        registerToggleLanguageKey();
        registerOpenLanguageSettingKey();
        registerOpenGroupSettingKey();
    }

    private static void modifyResourcePacks(Group key, MinecraftClient client) {
        Config.SETTINGS.packGroups.stream().filter(group -> group.getKey() == key).findFirst().ifPresent(group -> {
            ResourcePackManager manager = client.getResourcePackManager();
            manager.getEnabledProfiles().stream().filter(profile -> !profile.isAlwaysEnabled()).map(ResourcePackProfile::getName).forEach(manager::disable);
            List<String> names = new ArrayList<>(group.getPacks());
            Collections.reverse(names);
            names.forEach(manager::enable);
            client.options.refreshResourcePacks(client.getResourcePackManager());
        });
    }

    private static void registerOpenGroupSettingKey() {
        KeyBindings.openGroupSetting = registerKey("openGroupSetting", GLFW.GLFW_KEY_G, (client, keyBinding) -> {
            while (keyBinding.wasPressed()) {
                ResourceSwitcher.openResourcePackGroupScreen(client);
            }
        });
    }

    private static void registerOpenLanguageSettingKey() {
        KeyBindings.openLanguageSetting = registerKey("openLanguageSetting", GLFW.GLFW_KEY_B, (client, keyBinding) -> {
            while (keyBinding.wasPressed()) {
                ResourceSwitcher.openLanguageSwitchSettingScreen(client);
            }
        });
    }

    private static void registerToggleLanguageKey() {
        KeyBindings.toggleLanguage = registerKey("toggleLanguage", GLFW.GLFW_KEY_I, (client, keyBinding) -> {
            if (keyBinding.isPressed()) {
                keyBinding.setPressed(false);
                LanguageManager languageManager = client.getLanguageManager();
                loadLanguageConfig(languageManager);
                String code = languageManager.getLanguage().equals(Config.SETTINGS.mainLanguage) ? Config.SETTINGS.alternateLanguage : Config.SETTINGS.mainLanguage;
                languageManager.setLanguage(code);
                client.reloadResources();
                client.options.write();
                if (client.player != null) {
                    client.player.sendMessage(ResourceSwitcher.createWithPrefix(Text.translatable("key.resource_switcher.toggleLanguage.message").append(Objects.requireNonNull(languageManager.getLanguage(languageManager.getLanguage())).getDisplayText())));
                }
            }
        });
    }

    private static void loadLanguageConfig(LanguageManager languageManager) {
        Config.load();
        if (Config.SETTINGS.mainLanguage == null) {
            Config.SETTINGS.mainLanguage = languageManager.getLanguage();
        }
        if (Config.SETTINGS.alternateLanguage == null) {
            Config.SETTINGS.alternateLanguage = Language.DEFAULT_LANGUAGE;
        }
    }

    private static void registerSelectExclusionsKey() {
        KeyBindings.selectExclusions = registerKey("selectExclusions", GLFW.GLFW_KEY_N, (client, keyBinding) -> {
            while (keyBinding.wasPressed()) {
                ResourceSwitcher.openSelectExclusionsScreen(client);
            }
        });
    }

    private static void registerOpenScreenKey() {
        KeyBindings.openScreen = registerKey("openScreen", GLFW.GLFW_KEY_V, (client, keyBinding) -> {
            while (keyBinding.wasPressed()) {
                ResourceSwitcher.openResourcePackManagerScreen(client, null);
            }
        });
    }

    private static void registerToggleAllKey() {
        KeyBindings.toggleAll = registerKey("toggleAll", GLFW.GLFW_KEY_R, (client, keyBinding) -> {
            if (client.getOverlay() == null && keyBinding.isPressed()) {
                keyBinding.setPressed(false);
                ResourcePackManager manager = client.getResourcePackManager();
                if (Config.SETTINGS.disabledPacks.isEmpty()) {
                    for (ResourcePackProfile profile : manager.getEnabledProfiles()) {
                        String name = profile.getName();
                        if (!profile.isAlwaysEnabled() && !Config.SETTINGS.excludedPacks.contains(name)) {
                            if (manager.disable(name)) {
                                Config.SETTINGS.disabledPacks.add(name);
                            }
                        }
                    }
                    if (client.player != null) {
                        boolean empty = Config.SETTINGS.disabledPacks.isEmpty();
                        Text message = empty ? ResourceSwitcher.createWithPrefix(Text.translatable("text." + MOD_ID + ".disableAll.empty")) : ResourceSwitcher.createWithPrefix(Text.translatable("text." + MOD_ID + ".disableAll", Config.SETTINGS.disabledPacks.size()).styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Texts.join(parseToProfiles(manager, Config.SETTINGS.disabledPacks), ResourcePackProfile::getDisplayName)))));
                        client.player.sendMessage(message);
                        if (empty) {
                            return;
                        }
                    }
                } else {
                    for (ResourcePackProfile profile : manager.getEnabledProfiles()) {
                        String name = profile.getName();
                        if (!profile.isAlwaysEnabled() && !Config.SETTINGS.excludedPacks.contains(name)) {
                            manager.disable(name);
                        }
                    }
                    List<String> profiles = new ArrayList<>();
                    for (String name : Config.SETTINGS.disabledPacks) {
                        if (manager.enable(name)) {
                            profiles.add(name);
                        }
                    }
                    Config.SETTINGS.disabledPacks.clear();
                    if (client.player != null) {
                        client.player.sendMessage(ResourceSwitcher.createWithPrefix(Text.translatable("text." + MOD_ID + ".enableAll", profiles.size()).styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Texts.join(parseToProfiles(manager, Config.SETTINGS.disabledPacks), ResourcePackProfile::getDisplayName))))));
                    }
                }
                client.options.refreshResourcePacks(client.getResourcePackManager());
                Config.save();
            }
        });
    }

    private static Set<ResourcePackProfile> parseToProfiles(ResourcePackManager manager, Set<String> stringList) {
        return stringList.stream().map(manager::getProfile).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private static KeyBinding registerKey(String id, int keyCode, @Nullable BiConsumer<MinecraftClient, KeyBinding> listener) {
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

    @Environment(EnvType.CLIENT)
    public enum Group {
        GROUP_1(KeyBindings.useGroup1, "1"),
        GROUP_2(KeyBindings.useGroup2, "2"),
        GROUP_3(KeyBindings.useGroup3, "3");

        public final KeyBinding keyBinding;
        public final String translationKey;

        Group(KeyBinding keyBinding, String translationKey) {
            this.keyBinding = keyBinding;
            this.translationKey = "group.resource_switcher." + translationKey;
        }
    }
}
