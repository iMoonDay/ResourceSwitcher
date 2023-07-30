package com.imoonday.resource_switcher.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;

@Environment(EnvType.CLIENT)
public class Config {

    private static File file;
    private static File backupFile;
    public static Settings SETTINGS;

    public static void initSettings() {
        SETTINGS = new Settings();
    }

    private static void prepareConfigFile() {
        if (file == null) {
            file = new File(FabricLoader.getInstance().getConfigDir().toFile(), ResourceSwitcher.MOD_ID + ".json");
        }
        if (backupFile == null) {
            backupFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), ResourceSwitcher.MOD_ID + ".json.bak");
        }
    }

    public static void initConfig() {
        initSettings();
        load();
    }

    public static void load() {
        prepareConfigFile();

        try {
            if (!file.exists()) {
                save();
            }
            if (file.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(file));
                JsonElement jsonElement = JsonParser.parseReader(br);
                Settings settings = Settings.fromJson(jsonElement.toString());
                if (settings != null) {
                    SETTINGS = settings;
                } else {
                    save();
                }
            }
        } catch (Exception e) {
            System.err.println("Couldn't load Resource Switcher configuration file; backup and reverting to defaults");
            e.printStackTrace();
            backup();
            save();
        }
    }

    public static boolean save() {
        prepareConfigFile();
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(SETTINGS.toJson());
            return true;
        } catch (IOException e) {
            System.err.println("Couldn't save Resource Switcher configuration file");
            e.printStackTrace();
            return false;
        }
    }

    public static void backup() {
        prepareConfigFile();
        try {
            if (file.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(file));
                JsonElement jsonElement = JsonParser.parseReader(br);
                if (jsonElement.isJsonObject()) {
                    JsonObject json = jsonElement.getAsJsonObject();

                    try (FileWriter fileWriter = new FileWriter(backupFile)) {
                        fileWriter.write(Settings.GSON.toJson(json));
                    } catch (IOException e) {
                        System.err.println("Couldn't save Resource Switcher configuration backup file");
                        e.printStackTrace();
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Couldn't load Resource Switcher configuration file; backup failed");
            e.printStackTrace();
        }
    }
}
