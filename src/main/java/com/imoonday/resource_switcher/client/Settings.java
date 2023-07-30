package com.imoonday.resource_switcher.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Language;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;

@Environment(EnvType.CLIENT)
public class Settings {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public LinkedHashSet<String> excludedPacks;
    public LinkedHashSet<String> disabledPacks;
    public LinkedHashSet<String> lockedPacks;
    public String mainLanguage;
    public String alternateLanguage;
    public int autoSaveTime;
    public boolean displaySaveLog;
    public LinkedHashSet<ResourcePackGroup> packGroups;

    public Settings() {
        this(new LinkedHashSet<>(), new LinkedHashSet<>(), new LinkedHashSet<>(), Language.DEFAULT_LANGUAGE, Language.DEFAULT_LANGUAGE, 5, false, new LinkedHashSet<>());
    }

    public Settings(LinkedHashSet<String> excludedPacks, LinkedHashSet<String> disabledPacks, LinkedHashSet<String> lockedPacks, String mainLanguage, String alternateLanguage, int autoSaveTime, boolean displaySaveLog, LinkedHashSet<ResourcePackGroup> packGroups) {
        this.excludedPacks = excludedPacks;
        this.disabledPacks = disabledPacks;
        this.lockedPacks = lockedPacks;
        this.mainLanguage = mainLanguage;
        this.alternateLanguage = alternateLanguage;
        this.autoSaveTime = autoSaveTime;
        this.displaySaveLog = displaySaveLog;
        this.packGroups = packGroups;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    @Nullable
    public static Settings fromJson(String json) {
        Settings settings = GSON.fromJson(json, Settings.class);
        if (settings == null) {
            return null;
        }
        return settings.withNotNullSet();
    }

    private Settings withNotNullSet() {
        if (excludedPacks == null) {
            excludedPacks = new LinkedHashSet<>();
        }
        if (disabledPacks == null) {
            disabledPacks = new LinkedHashSet<>();
        }
        if (lockedPacks == null) {
            lockedPacks = new LinkedHashSet<>();
        }
        if (packGroups == null) {
            packGroups = new LinkedHashSet<>();
        }
        return this;
    }
}
