package com.imoonday.resource_switcher.client.screens;

import com.imoonday.resource_switcher.client.Config;
import com.imoonday.resource_switcher.client.KeyBindings;
import com.imoonday.resource_switcher.client.ResourcePackGroup;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyCodes;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class ResourcePackGroupScreen extends Screen {

    private final Screen parent;
    private static final String ELLIPSIS = "...";
    private ResourcePackGroupListWidget groupList;
    private ResourcePackListWidget disabledList;
    private ResourcePackListWidget enabledList;
    private final ResourcePackManager packManager;
    public String selectedGroup;
    private ButtonWidget upButton;
    private ButtonWidget downButton;
    private ButtonWidget groupKeyButton;
    private ButtonWidget deleteButton;
    private TextFieldWidget textField;
    private ButtonWidget selectAllButton;

    public ResourcePackGroupScreen(Screen parent, ResourcePackManager packManager) {
        super(Text.translatable("screen.resource_switcher.resourcePackGroup.title"));
        this.parent = parent;
        this.packManager = packManager;
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
        this.selectedGroup = !Config.SETTINGS.packGroups.isEmpty() ? Config.SETTINGS.packGroups.iterator().next().getId() : "new";

        this.groupList = new ResourcePackGroupListWidget(this.client, Text.translatable("screen.resource_switcher.resourcePackGroup.subtitle.group"));
        this.addSelectableChild(this.groupList);

        int width1 = (this.width - this.width / 5) / 2;
        this.enabledList = new ResourcePackListWidget(this.client, this.getEnabledProfiles(), false, width1, Text.translatable("screen.resource_switcher.resourcePackGroup.subtitle.selected"));
        this.enabledList.setX(this.width - this.enabledList.getRowWidth());
        this.addSelectableChild(this.enabledList);

        this.disabledList = new ResourcePackListWidget(this.client, this.getDisabledProfiles(), true, this.width - this.width / 5 - width1, Text.translatable("screen.resource_switcher.resourcePackGroup.subtitle.available"));
        this.disabledList.setX(this.groupList.getRowWidth());
        this.addSelectableChild(this.disabledList);

        this.upButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.resource_switcher.resourcePackGroup.button.up"), button -> {
            ResourcePackListWidget.ResourcePackEntry packEntry = enabledList.getSelectedOrNull();
            if (packEntry != null) {
                if (enabledList.up(packEntry.profile)) {
                    enabledList.initProfiles();
                    save();
                }
            }
        }).dimensions(this.width / 2 - 155, this.height - 38, 70, 20).build());

        this.downButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.resource_switcher.resourcePackGroup.button.down"), button -> {
            ResourcePackListWidget.ResourcePackEntry packEntry = enabledList.getSelectedOrNull();
            if (packEntry != null) {
                if (enabledList.down(packEntry.profile)) {
                    enabledList.initProfiles();
                    save();
                }
            }
        }).dimensions(this.width / 2 - 155 + 70 + 10, this.height - 38, 70, 20).build());

        this.groupKeyButton = this.addDrawableChild(ButtonWidget.builder(getKeyText(), button -> {
            getSelectedGroup().ifPresent(ResourcePackGroup::nextKey);
            Config.save();
            button.setMessage(getKeyText());
        }).dimensions(this.width / 2 - 155 + 70 + 10 + 70 + 10, this.height - 38, 70, 20).build());

        this.textField = new TextFieldWidget(textRenderer, this.width - 10 - 70, 7, 70, 20, Text.translatable("screen.resource_switcher.resourcePackGroup.button.groupName"));
        this.textField.setText(selectedGroup);
        this.textField.setChangedListener(s -> {
            if (selectedGroup.equals(s)) {
                return;
            }
            if (Config.SETTINGS.packGroups.stream().anyMatch(resourcePackGroup -> resourcePackGroup.isIdEquals(s))) {
                this.textField.setText(selectedGroup);
                return;
            }
            getSelectedGroup().ifPresent(group -> {
                if (s != null) {
                    group.setId(s);
                    selectedGroup = s;
                    save();
                }
            });
        });
        this.addDrawableChild(this.textField);

        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> this.close()).dimensions(this.width / 2 - 155 + 160 + 70 + 10, this.height - 38, 70, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.resource_switcher.resourcePackGroup.button.new"), button -> {
            String group = "new";
            int count = 0;
            while (true) {
                boolean b = true;
                for (ResourcePackGroup group1 : Config.SETTINGS.packGroups) {
                    if (group1.isIdEquals(group)) {
                        b = false;
                        break;
                    }
                }
                if (b) break;
                group = "new%d".formatted(++count);
            }
            Config.SETTINGS.packGroups.add(new ResourcePackGroup(group));
            Config.save();
            selectedGroup = group;
            this.textField.setText(selectedGroup);
            refreshList();
        }).dimensions(10, 7, 35, 20).build());

        this.deleteButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.resource_switcher.resourcePackGroup.button.delete"), button -> {
            String key = getNeighbourId(Config.SETTINGS.packGroups, selectedGroup);
            Config.SETTINGS.packGroups.removeIf(group -> group.isIdEquals(selectedGroup));
            if (key == null) {
                key = "new";
                if (Config.SETTINGS.packGroups.stream().noneMatch(group -> group.isIdEquals("new"))) {
                    Config.SETTINGS.packGroups.add(new ResourcePackGroup(key));
                }
            }
            this.selectedGroup = key;
            Config.save();
            this.textField.setText(selectedGroup);
            refreshList();
        }).dimensions(10 + 35 + 5, 7, 35, 20).build());

        this.selectAllButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.resource_switcher.resourcePackGroup.button.selectAll"), button -> {
            if (this.disabledList.getSelectedOrNull() != null || this.enabledList.profiles.isEmpty()) {
                this.enabledList.profiles.addAll(this.disabledList.profiles);
                this.disabledList.profiles.clear();
                this.disabledList.setSelected(null);
            } else if (this.enabledList.getSelectedOrNull() != null || this.disabledList.profiles.isEmpty()) {
                this.disabledList.profiles.addAll(this.enabledList.profiles);
                this.enabledList.profiles.clear();
                this.enabledList.setSelected(null);
            }
            save();
        }).dimensions(this.width - 10 - 70 - 10 - 35 - 5 - 35, 7, 35, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.resource_switcher.resourcePackGroup.button.synchronous"), button -> getSelectedGroup().ifPresent(group -> {
            List<String> list = packManager.getEnabledProfiles().stream().filter(profile -> !profile.isAlwaysEnabled()).map(ResourcePackProfile::getName).collect(Collectors.toList());
            Collections.reverse(list);
            group.setPacks(list);
            Config.save();
            refreshList();
        })).dimensions(this.width - 10 - 70 - 10 - 35, 7, 35, 20).build());

        save();
    }

    private Text getKeyText() {
        Text keyText = Text.translatable("screen.resource_switcher.resourcePackGroup.button.unassigned");
        Optional<ResourcePackGroup> group = getSelectedGroup();
        if (group.isPresent()) {
            ResourcePackGroup resourcePackGroup = group.get();
            KeyBindings.Group key = resourcePackGroup.getKey();
            if (key != null) {
                Text text = key.keyBinding.getBoundKeyLocalizedText();
                keyText = Text.translatable(key.translationKey).append(": ").append(text).formatted(isKeyRepetitive(resourcePackGroup) ? Formatting.RED : Formatting.WHITE);
            }
        }
        return keyText;
    }

    private boolean isKeyRepetitive(ResourcePackGroup group) {
        if (group.getKey() == null) {
            return false;
        }
        Set<KeyBindings.Group> keys = new HashSet<>();
        for (ResourcePackGroup packGroup : Config.SETTINGS.packGroups) {
            KeyBindings.Group groupKey = packGroup.getKey();
            if (groupKey != null) {
                if (keys.contains(groupKey)) {
                    return true;
                }
                if (group.equals(packGroup)) {
                    break;
                }
                keys.add(groupKey);
            }
        }
        return false;
    }

    private Optional<ResourcePackGroup> getSelectedGroup() {
        return Config.SETTINGS.packGroups.stream().filter(group -> group.isIdEquals(selectedGroup)).findFirst();
    }

    @Nullable
    public static String getNeighbourId(LinkedHashSet<ResourcePackGroup> groups, String id) {
        ArrayList<ResourcePackGroup> list = new ArrayList<>(groups);
        for (int i = 0; i < list.size(); i++) {
            ResourcePackGroup rpg = list.get(i);
            if (id.equals(rpg.getId())) {
                int index = i - 1;
                if (index >= 0 && index < list.size()) {
                    return list.get(index).getId();
                } else if (index == -1 && i + 1 < list.size()) {
                    return list.get(i + 1).getId();
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    void refreshList() {
        this.disabledList.resetProfiles();
        this.enabledList.resetProfiles();
        this.groupList.resetGroups();
        this.groupList.children().stream().filter(entry -> entry.group.isIdEquals(selectedGroup)).findFirst().ifPresent(resourcePackGroupEntry -> this.groupList.setSelected(resourcePackGroupEntry));
    }

    void save() {
        List<String> stringList = this.enabledList.profiles.stream().map(ResourcePackProfile::getName).collect(Collectors.toList());
        getSelectedGroup().ifPresentOrElse(group -> group.setPacks(stringList), () -> Config.SETTINGS.packGroups.add(new ResourcePackGroup(selectedGroup, stringList)));
        Config.save();
        refreshList();
    }

    public OrderedText trimTextToWidth(MinecraftClient client, Text text, int maxWidth) {
        int i = client.textRenderer.getWidth(text);
        if (i > maxWidth) {
            StringVisitable stringVisitable = StringVisitable.concat(client.textRenderer.trimToWidth(text, maxWidth - client.textRenderer.getWidth(ELLIPSIS)), StringVisitable.plain(ELLIPSIS));
            return Language.getInstance().reorder(stringVisitable);
        }
        return text.asOrderedText();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        ResourcePackGroupScreen.ResourcePackListWidget.ResourcePackEntry packEntry = this.disabledList.getSelectedOrNull();
        if (KeyCodes.isToggle(keyCode) && packEntry != null) {
            packEntry.onPressed(false);
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.groupList.render(context, mouseX, mouseY, delta);
        this.disabledList.render(context, mouseX, mouseY, delta);
        this.enabledList.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 16, 0xFFFFFF);
        ResourcePackGroupScreen.ResourcePackListWidget.ResourcePackEntry packEntry = this.disabledList.getSelectedOrNull();
        if (packEntry == null) {
            packEntry = this.enabledList.getSelectedOrNull();
        }
        if (packEntry != null) {
            ResourcePackProfile profile = packEntry.profile;
            if (client != null) {
                context.drawCenteredTextWithShadow(this.textRenderer, trimTextToWidth(client, profile.getDescription(), this.width), this.width / 2, this.height - 56, 0x808080);
            }
        }
        refreshButton();
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void tick() {
        super.tick();
        refreshButton();
    }

    private void refreshButton() {
        ResourcePackListWidget.ResourcePackEntry packEntry = enabledList.getSelectedOrNull();
        this.upButton.active = packEntry != null && enabledList.profiles.indexOf(packEntry.profile) > 0;
        this.downButton.active = packEntry != null && enabledList.profiles.contains(packEntry.profile) && enabledList.profiles.indexOf(packEntry.profile) < enabledList.profiles.size() - 1;
        this.deleteButton.active = canDelete();
        this.selectAllButton.active = this.disabledList.getSelectedOrNull() != null || this.enabledList.getSelectedOrNull() != null || this.disabledList.profiles.isEmpty() || this.enabledList.profiles.isEmpty();
        this.groupKeyButton.setMessage(getKeyText());
    }

    private boolean canDelete() {
        if (Config.SETTINGS.packGroups.size() > 1) {
            return true;
        }
        if (Config.SETTINGS.packGroups.size() == 1) {
            ResourcePackGroup group = new ArrayList<>(Config.SETTINGS.packGroups).get(0);
            return !group.getId().equals("new") || !group.getPacks().isEmpty();
        }
        return false;
    }

    public List<ResourcePackProfile> getEnabledProfiles() {
        for (ResourcePackGroup group : Config.SETTINGS.packGroups) {
            if (group.isIdEquals(selectedGroup)) {
                return group.getPacks().stream().map(packManager::getProfile).filter(Objects::nonNull).collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }

    public List<ResourcePackProfile> getDisabledProfiles() {
        return packManager.getProfiles().stream().filter(profile -> !profile.isAlwaysEnabled() && !this.getEnabledProfiles().contains(profile)).collect(Collectors.toList());
    }

    void selectProfile(boolean disabled, ResourcePackProfile profile) {
        if (disabled) {
            this.disabledList.profiles.remove(profile);
            this.enabledList.profiles.add(profile);
        } else {
            this.enabledList.profiles.remove(profile);
            this.disabledList.profiles.add(profile);
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackgroundTexture(context);
    }

    @Environment(value = EnvType.CLIENT)
    class ResourcePackListWidget extends AlwaysSelectedEntryListWidget<ResourcePackListWidget.ResourcePackEntry> {

        final List<ResourcePackProfile> profiles;
        private final boolean disabled;
        private final Text title;

        public ResourcePackListWidget(MinecraftClient client, List<ResourcePackProfile> profiles, boolean disabled, int width, Text title) {
            super(client, width, ResourcePackGroupScreen.this.height - 65 + 4 - 32, 32, 18);
            this.profiles = profiles;
            this.disabled = disabled;
            this.title = title;
            initProfiles();
            this.setRenderHeader(true, (int) (9.0f * 1.5f));
        }

        @Override
        protected void renderHeader(DrawContext context, int x, int y) {
            MutableText text = Text.empty().append(this.title).formatted(Formatting.BOLD);
            if (this.getSelectedOrNull() != null) {
                text = text.formatted(Formatting.UNDERLINE);
            }
            context.drawText(this.client.textRenderer, text, x + this.width / 2 - this.client.textRenderer.getWidth(text) / 2, Math.min(this.getY() + 3, y), 0xFFFFFF, false);
        }

        private void initProfiles() {
            this.children().clear();
            this.profiles.forEach(profile -> this.addEntry(new ResourcePackEntry(profile)));
        }

        void resetProfiles() {
            this.setProfiles(this.disabled ? getDisabledProfiles() : getEnabledProfiles());
            this.initProfiles();
            this.setScrollAmount(this.getScrollAmount());
        }

        private void setProfiles(Collection<ResourcePackProfile> profiles) {
            this.profiles.clear();
            this.profiles.addAll(profiles);
        }

        @Override
        protected int getScrollbarPositionX() {
            return getRowLeft() + getRowWidth() - 8;
        }

        @Override
        public int getRowWidth() {
            return this.width;
        }

        boolean up(ResourcePackProfile profile) {
            for (int i = 0; i < profiles.size(); i++) {
                if (profiles.get(i).equals(profile)) {
                    if (i != 0) {
                        profiles.add(i - 1, profiles.remove(i));
                        return true;
                    }
                }
            }
            return false;
        }

        boolean down(ResourcePackProfile profile) {
            for (int i = 0; i < profiles.size(); i++) {
                if (profiles.get(i).equals(profile)) {
                    if (i != profiles.size() - 1) {
                        profiles.add(i + 1, profiles.remove(i));
                        return true;
                    }
                }
            }
            return false;
        }

        @Environment(value = EnvType.CLIENT)
        public class ResourcePackEntry extends AlwaysSelectedEntryListWidget.Entry<ResourcePackListWidget.ResourcePackEntry> {

            private final ResourcePackProfile profile;

            public ResourcePackEntry(ResourcePackProfile profile) {
                this.profile = profile;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                Text name = this.profile.getDisplayName();
                ResourcePackEntry packEntry = getSelectedOrNull();
                if (this == packEntry) {
                    name = name.copy().formatted(Formatting.UNDERLINE);
                }
                context.drawCenteredTextWithShadow(ResourcePackGroupScreen.this.textRenderer, trimTextToWidth(client, name, ResourcePackGroupScreen.this.width / 5 * 2), ResourcePackListWidget.this.getX() + ResourcePackListWidget.this.width / 2, y + 1, 0xFFFFFF);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0 || button == 1) {
                    ResourcePackEntry packEntry = ResourcePackListWidget.this.getSelectedOrNull();
                    boolean selected = this == packEntry;
                    if (selected || button == 1) {
                        selectProfile(disabled, profile);
                        save();
                    }
                    this.onPressed(selected || button == 1);
                    return true;
                }
                return false;
            }

            void onPressed(boolean selected) {
                if (selected) {
                    enabledList.setSelected(null);
                    disabledList.setSelected(null);
                } else {
                    ResourcePackListWidget.this.setSelected(this);
                    if (disabled) {
                        enabledList.setSelected(null);
                    } else {
                        disabledList.setSelected(null);
                    }
                }
            }

            @Override
            public Text getNarration() {
                return Text.translatable("narrator.select", this.profile.getDisplayName());
            }
        }
    }

    @Environment(value = EnvType.CLIENT)
    class ResourcePackGroupListWidget extends AlwaysSelectedEntryListWidget<ResourcePackGroupListWidget.ResourcePackGroupEntry> {

        private final Text title;

        public ResourcePackGroupListWidget(MinecraftClient client, Text title) {
            super(client, ResourcePackGroupScreen.this.width / 5, ResourcePackGroupScreen.this.height - 65 + 4 - 32, 32, 18);
            this.title = title;
            initGroups();
            this.setRenderHeader(true, (int) (9.0f * 1.5f));
        }

        @Override
        protected void renderHeader(DrawContext context, int x, int y) {
            MutableText text = Text.empty().append(this.title).formatted(Formatting.UNDERLINE, Formatting.BOLD);
            context.drawText(this.client.textRenderer, text, x + this.width / 2 - this.client.textRenderer.getWidth(text) / 2, Math.min(this.getY() + 3, y), 0xFFFFFF, false);
        }

        public void resetGroups() {
            this.children().clear();
            initGroups();
            this.setScrollAmount(this.getScrollAmount());
        }

        private void initGroups() {
            Config.SETTINGS.packGroups.forEach(group -> this.addEntry(new ResourcePackGroupEntry(group)));
        }

        @Override
        protected int getScrollbarPositionX() {
            return getRowWidth() - 6;
        }

        @Override
        public int getRowWidth() {
            return this.width;
        }

        @Environment(value = EnvType.CLIENT)
        public class ResourcePackGroupEntry extends AlwaysSelectedEntryListWidget.Entry<ResourcePackGroupListWidget.ResourcePackGroupEntry> {

            private final ResourcePackGroup group;

            public ResourcePackGroupEntry(ResourcePackGroup group) {
                this.group = group;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                MutableText text = Text.literal(group.getId());
                if (this.group.isIdEquals(selectedGroup)) {
                    text = text.formatted(Formatting.UNDERLINE);
                }
                if (isKeyRepetitive(group)) {
                    text = text.formatted(Formatting.RED);
                }
                if (this.group.getKey() != null) {
                    text = text.append("(").append(this.group.getKey().keyBinding.getBoundKeyLocalizedText()).append(")");
                }
                context.drawCenteredTextWithShadow(ResourcePackGroupScreen.this.textRenderer, trimTextToWidth(client, text, ResourcePackGroupScreen.this.width / 5), ResourcePackGroupListWidget.this.width / 2, y + 1, 0xFFFFFF);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    if (!ResourcePackGroupScreen.this.selectedGroup.equals(group.getId())) {
                        ResourcePackGroupScreen.this.selectedGroup = group.getId();
                        disabledList.setSelected(null);
                        enabledList.setSelected(null);
                    }
                    this.onPressed();
                    return true;
                }
                return false;
            }

            void onPressed() {
                ResourcePackGroupListWidget.this.setSelected(this);
                refreshList();
                ResourcePackGroupScreen.this.textField.setText(selectedGroup);
            }

            @Override
            public Text getNarration() {
                return Text.translatable("narrator.select", this.group.getId());
            }
        }
    }
}
