package com.imoonday.resource_switcher.client.screens;

import com.imoonday.resource_switcher.client.Config;
import com.imoonday.resource_switcher.client.ResourceSwitcher;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyCodes;
import net.minecraft.client.resource.language.LanguageDefinition;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public class LanguageSwitchSettingScreen extends Screen {

    private final Screen parent;
    private LanguageSwitchSettingListWidget languageSwitchSettingList;
    final LanguageManager languageManager;

    public LanguageSwitchSettingScreen(Screen parent, LanguageManager languageManager) {
        super(Text.translatable("screen.resource_switcher.languageSwitchSetting.title"));
        this.parent = parent;
        this.languageManager = languageManager;
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
        Config.save();
    }

    @Override
    protected void init() {
        this.languageSwitchSettingList = new LanguageSwitchSettingListWidget(this.client);
        this.addSelectableChild(this.languageSwitchSettingList);
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.resource_switcher.languageSwitchSetting.openScreen"), button -> {
            if (client != null) {
                ResourceSwitcher.openLanguageOptionsScreen(client, this);
            }
        }).dimensions(this.width / 2 - 155, this.height - 38, 150, 20).build());
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> this.close()).dimensions(this.width / 2 - 155 + 160, this.height - 38, 150, 20).build());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        LanguageSwitchSettingListWidget.LanguageEntry languageEntry = this.languageSwitchSettingList.getSelectedOrNull();
        if (KeyCodes.isToggle(keyCode) && languageEntry != null) {
            languageEntry.onPressed();
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.languageSwitchSettingList.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 16, 0xFFFFFF);
        LanguageSwitchSettingListWidget.LanguageEntry languageEntry = this.languageSwitchSettingList.getSelectedOrNull();
        if (languageEntry != null && client != null) {
            Text text = Text.translatable("screen.resource_switcher.languageSwitchSetting.mainLanguage").append(getOrEmpty(Config.SETTINGS.mainLanguage)).append(ScreenTexts.SPACE).append(Text.translatable("screen.resource_switcher.languageSwitchSetting.alternateLanguage").append(getOrEmpty(Config.SETTINGS.alternateLanguage))).formatted(Formatting.GRAY);
            context.drawCenteredTextWithShadow(this.textRenderer, text, this.width / 2, this.height - 56, 0x808080);
        }
    }

    private Text getOrEmpty(String code) {
        LanguageDefinition definition = languageManager.getLanguage(code);
        return definition != null ? definition.getDisplayText() : Text.empty();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackgroundTexture(context);
    }

    @Environment(value = EnvType.CLIENT)
    class LanguageSwitchSettingListWidget extends AlwaysSelectedEntryListWidget<LanguageSwitchSettingListWidget.LanguageEntry> {
        public LanguageSwitchSettingListWidget(MinecraftClient client) {
            super(client, LanguageSwitchSettingScreen.this.width, LanguageSwitchSettingScreen.this.height - 65 + 4 - 32, 32, 18);
            String string = LanguageSwitchSettingScreen.this.languageManager.getLanguage();
            LanguageSwitchSettingScreen.this.languageManager.getAllLanguages().forEach((languageCode, languageDefinition) -> {
                LanguageSwitchSettingScreen.LanguageSwitchSettingListWidget.LanguageEntry languageEntry = new LanguageSwitchSettingScreen.LanguageSwitchSettingListWidget.LanguageEntry(languageCode, languageDefinition);
                this.addEntry(languageEntry);
                if (string.equals(languageCode)) {
                    this.setSelected(languageEntry);
                }
            });
            if (this.getSelectedOrNull() != null) {
                this.centerScrollOn(this.getSelectedOrNull());
            }
        }

        @Override
        protected int getScrollbarPositionX() {
            return super.getScrollbarPositionX() + 20;
        }

        @Override
        public int getRowWidth() {
            return super.getRowWidth() + 50;
        }

        @Environment(value = EnvType.CLIENT)
        public class LanguageEntry extends AlwaysSelectedEntryListWidget.Entry<LanguageEntry> {
            final String languageCode;
            private final Text languageDefinition;
            private long clickTime;

            public LanguageEntry(String languageCode, LanguageDefinition languageDefinition) {
                this.languageCode = languageCode;
                this.languageDefinition = languageDefinition.getDisplayText();
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                MutableText text = this.languageDefinition.copy();
                if (languageCode.equals(languageManager.getLanguage())) {
                    text = text.append(Text.translatable("screen.resource_switcher.languageSwitchSetting.remark.currentLanguage").formatted(Formatting.AQUA, Formatting.BOLD));
                }
                if (languageCode.equals(Config.SETTINGS.mainLanguage)) {
                    text = text.append(Text.translatable("screen.resource_switcher.languageSwitchSetting.remark.mainLanguage").formatted(Formatting.GREEN, Formatting.BOLD));
                }
                if (languageCode.equals(Config.SETTINGS.alternateLanguage)) {
                    text = text.append(Text.translatable("screen.resource_switcher.languageSwitchSetting.remark.alternateLanguage").formatted(Formatting.GRAY, Formatting.BOLD));
                }
                context.drawCenteredTextWithShadow(LanguageSwitchSettingScreen.this.textRenderer, text, LanguageSwitchSettingListWidget.this.width / 2, y + 1, 0xFFFFFF);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                switch (button) {
                    case 0 -> {
                        this.onPressed();
                        if (Util.getMeasuringTimeMs() - this.clickTime < 250L) {
                            Config.SETTINGS.mainLanguage = languageCode.equals(Config.SETTINGS.mainLanguage) ? languageManager.getLanguage() : languageCode;
                            Config.save();
                        }
                        this.clickTime = Util.getMeasuringTimeMs();
                    }
                    case 1 -> {
                        this.onPressed();
                        Config.SETTINGS.alternateLanguage = languageCode.equals(Config.SETTINGS.alternateLanguage) ? Language.DEFAULT_LANGUAGE : languageCode;
                        Config.save();
                    }
                    case 2 -> {
                        languageManager.setLanguage(languageCode);
                        client.reloadResources();
                        client.options.write();
                    }
                    default -> {
                        this.clickTime = Util.getMeasuringTimeMs();
                        return false;
                    }
                }
                return true;
            }

            void onPressed() {
                LanguageSwitchSettingListWidget.this.setSelected(this);
            }

            @Override
            public Text getNarration() {
                return Text.translatable("narrator.select", this.languageDefinition);
            }
        }
    }
}
