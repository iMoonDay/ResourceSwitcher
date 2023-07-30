package com.imoonday.resource_switcher.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyCodes;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ExclusionSelectionScreen extends Screen {

    private ExclusionSelectionListWidget exclusionSelectionList;
    private final Screen parent;
    boolean changed;

    public ExclusionSelectionScreen(Screen parent) {
        super(Text.literal("选择排除资源包"));
        this.parent = parent;
    }

    @Override
    public void close() {
        if (client != null) {
            if (changed) {
                client.reloadResources();
            }
            client.setScreen(parent);
        }
        Config.save();
    }

    @Override
    protected void init() {
        this.exclusionSelectionList = new ExclusionSelectionListWidget(this.client);
        this.addSelectableChild(this.exclusionSelectionList);
        this.addDrawableChild(ButtonWidget.builder(Text.literal("打开选择资源包界面"), button -> {
            if (client != null) {
                KeyBindings.openResourcePackManagerScreen(client, this);
            }
        }).dimensions(this.width / 2 - 155, this.height - 38, 150, 20).build());
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> this.close()).dimensions(this.width / 2 - 155 + 160, this.height - 38, 150, 20).build());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        ExclusionSelectionListWidget.ResourcePackEntry packEntry = this.exclusionSelectionList.getSelectedOrNull();
        if (KeyCodes.isToggle(keyCode) && packEntry != null) {
            packEntry.onPressed();
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.exclusionSelectionList.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 16, 0xFFFFFF);
        ExclusionSelectionListWidget.ResourcePackEntry packEntry = this.exclusionSelectionList.getSelectedOrNull();
        if (packEntry != null) {
            context.drawCenteredTextWithShadow(this.textRenderer, packEntry.profile.getDescription(), this.width / 2, this.height - 56, 0x808080);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Environment(value = EnvType.CLIENT)
    class ExclusionSelectionListWidget extends AlwaysSelectedEntryListWidget<ExclusionSelectionListWidget.ResourcePackEntry> {
        public ExclusionSelectionListWidget(MinecraftClient client) {
            super(client, ExclusionSelectionScreen.this.width, ExclusionSelectionScreen.this.height, 32, ExclusionSelectionScreen.this.height - 65 + 4, 18);
            List<ResourcePackProfile> list = new ArrayList<>(client.getResourcePackManager().getEnabledProfiles());
            Collections.reverse(list);
            list.stream().filter(profile -> !profile.isAlwaysEnabled()).map(ResourcePackEntry::new).forEach(this::addEntry);
            client.getResourcePackManager().getProfiles().stream()
                    .filter(profile -> !profile.isAlwaysEnabled() && !list.contains(profile))
                    .map(ResourcePackEntry::new)
                    .forEach(this::addEntry);
        }

        @Override
        protected int getScrollbarPositionX() {
            return super.getScrollbarPositionX() + 20;
        }

        @Override
        public int getRowWidth() {
            return super.getRowWidth() + 50;
        }

        @Override
        protected void renderBackground(DrawContext context) {
            ExclusionSelectionScreen.this.renderBackground(context);
        }

        @Environment(value = EnvType.CLIENT)
        public class ResourcePackEntry extends AlwaysSelectedEntryListWidget.Entry<ResourcePackEntry> {
            private final ResourcePackProfile profile;

            public ResourcePackEntry(ResourcePackProfile profile) {
                this.profile = profile;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                ResourcePackEntry packEntry = ExclusionSelectionListWidget.this.getSelectedOrNull();
                MutableText text = this.profile.getDisplayName().copy();
                if (this == packEntry) {
                    text = text.formatted(Formatting.UNDERLINE);
                }
                String name = profile.getName();
                if (Config.excludedNames.contains(name)) {
                    text = text.formatted(Formatting.STRIKETHROUGH);
                }
                if (client.getResourcePackManager().getEnabledProfiles().contains(profile)) {
                    text = text.append(Text.literal(" (已启用)").formatted(Formatting.GREEN, Formatting.BOLD).styled(style -> style.withStrikethrough(false).withUnderline(false)));
                }
                if (Config.excludedNames.contains(name)) {
                    text = text.append(Text.literal(" (已排除)").formatted(Formatting.RED, Formatting.BOLD).styled(style -> style.withStrikethrough(false).withUnderline(false)));
                }
                context.drawCenteredTextWithShadow(ExclusionSelectionScreen.this.textRenderer, text, ExclusionSelectionListWidget.this.width / 2, y + 1, 0xFFFFFF);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                switch (button) {
                    case 0 -> {
                        ResourcePackEntry packEntry = ExclusionSelectionListWidget.this.getSelectedOrNull();
                        if (this == packEntry) {
                            switchExclusion();
                        }
                    }
                    case 1 -> switchState();
                    default -> {
                        return false;
                    }
                }
                this.onPressed();
                return true;
            }

            private void switchState() {
                ResourcePackManager manager = ExclusionSelectionListWidget.this.client.getResourcePackManager();
                if (manager.getEnabledProfiles().contains(profile)) {
                    manager.disable(profile.getName());
                } else {
                    manager.enable(profile.getName());
                }
                ExclusionSelectionScreen.this.changed = true;
            }

            private void switchExclusion() {
                String name = profile.getName();
                if (Config.excludedNames.contains(name)) {
                    Config.excludedNames.remove(name);
                } else {
                    Config.excludedNames.add(name);
                }
                Config.save();
            }

            void onPressed() {
                ExclusionSelectionListWidget.this.setSelected(this);
            }

            @Override
            public Text getNarration() {
                return Text.translatable("narrator.select", this.profile.getDisplayName());
            }
        }
    }
}
