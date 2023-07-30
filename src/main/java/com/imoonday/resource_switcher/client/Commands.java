package com.imoonday.resource_switcher.client;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

@Environment(EnvType.CLIENT)
public class Commands {

    public static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(literal("resource-switcher")
                .then(literal("set")
                        .then(literal("autoSaveTime").then(argument("minute", IntegerArgumentType.integer(0)).executes(Commands::setAutoSaveTime)))
                        .then(literal("displaySaveLog").then(argument("display", BoolArgumentType.bool()).executes(Commands::toggleDisplaySaveLog))))
                .then(literal("get")
                        .then(literal("autoSaveTime").executes(context -> getConfig(context, Text.translatable("text.resource_switcher.autoSaveTime", Config.SETTINGS.autoSaveTime))))
                        .then(literal("displaySaveLog").executes(context -> getConfig(context, Text.translatable("text.resource_switcher.displaySaveLog", Config.SETTINGS.displaySaveLog)))))
                .then(literal("reset").executes(Commands::reset))
                .then(literal("reload").executes(Commands::reload))
                .then(literal("save").executes(Commands::save))));
    }

    private static int save(CommandContext<FabricClientCommandSource> context) {
        boolean success = Config.save();
        sendMessage(context, success, "save.success", "save.failed");
        return 1;
    }

    private static int reload(CommandContext<FabricClientCommandSource> context) {
        Config.load();
        sendMessage(context, "reload");
        return 1;
    }

    private static int reset(CommandContext<FabricClientCommandSource> context) {
        Config.initSettings();
        Config.save();
        sendMessage(context, "reset");
        return 1;
    }

    private static int getConfig(CommandContext<FabricClientCommandSource> context, Text text) {
        Config.load();
        context.getSource().sendFeedback(ResourceSwitcher.createWithPrefix(text));
        return 1;
    }

    private static int toggleDisplaySaveLog(CommandContext<FabricClientCommandSource> context) {
        Config.SETTINGS.displaySaveLog = BoolArgumentType.getBool(context, "display");
        boolean success = Config.save();
        sendMessage(context, success, Text.translatable("text.resource_switcher.modify.success", Config.SETTINGS.displaySaveLog ? Text.translatable("text.resource_switcher.displaySaveLog.open") : Text.translatable("text.resource_switcher.displaySaveLog.close")), Text.translatable("text.resource_switcher.modify.failed"));
        return 1;
    }

    private static int setAutoSaveTime(CommandContext<FabricClientCommandSource> context) {
        Config.SETTINGS.autoSaveTime = IntegerArgumentType.getInteger(context, "minute");
        boolean success = Config.save();
        sendMessage(context, success, Config.SETTINGS.autoSaveTime == 0 ? Text.translatable("text.resource_switcher.autoSaveTime.close") : Text.translatable("text.resource_switcher.modify.success", Text.translatable("text.resource_switcher.autoSaveTime.modify", Config.SETTINGS.autoSaveTime)), Text.translatable("text.resource_switcher.modify.failed"));
        return 1;
    }

    private static void sendMessage(CommandContext<FabricClientCommandSource> context, String feedbackKey) {
        sendMessage(context, true, feedbackKey, null);
    }

    private static void sendMessage(CommandContext<FabricClientCommandSource> context, boolean success, String feedbackKey, String errorKey) {
        sendMessage(context, success, Text.translatable("text.resource_switcher." + feedbackKey), Text.translatable("text.resource_switcher." + errorKey));
    }

    private static void sendMessage(CommandContext<FabricClientCommandSource> context, boolean success, Text feedback, Text error) {
        if (success) {
            context.getSource().sendFeedback(ResourceSwitcher.createWithPrefix(feedback));
        } else {
            context.getSource().sendError(ResourceSwitcher.createWithPrefix(error));
        }
    }

}
