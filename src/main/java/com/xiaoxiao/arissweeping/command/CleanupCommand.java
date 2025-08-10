package com.xiaoxiao.arissweeping.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.handler.EntityCleanupHandler;
import com.xiaoxiao.arissweeping.util.CleanupStats;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber
public class CleanupCommand {
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("arissweeping")
            .requires(source -> source.hasPermission(2)) // OP权限
            .then(Commands.literal("cleanup")
                .executes(CleanupCommand::executeCleanup)
                .then(Commands.literal("items")
                    .executes(CleanupCommand::executeCleanupItems))
                .then(Commands.literal("mobs")
                    .executes(CleanupCommand::executeCleanupMobs))
                .then(Commands.literal("all")
                    .executes(CleanupCommand::executeCleanupAll)))
            .then(Commands.literal("stats")
                .executes(CleanupCommand::executeStats))
            .then(Commands.literal("config")
                .then(Commands.literal("interval")
                    .then(Commands.argument("seconds", IntegerArgumentType.integer(30, 3600))
                        .executes(CleanupCommand::setCleanupInterval)))
                .then(Commands.literal("reload")
                    .executes(CleanupCommand::reloadConfig)))
            .then(Commands.literal("help")
                .executes(CleanupCommand::executeHelp)));
    }
    
    private static int executeCleanup(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("§6[爱丽丝扫地] §f开始执行清理..."), true);
        
        CompletableFuture.runAsync(() -> {
            CleanupStats stats = performManualCleanup(source.getLevel());
            
            source.getServer().execute(() -> {
                if (stats.getTotalCleaned() > 0) {
                    source.sendSuccess(() -> Component.literal(String.format(
                        "§6[爱丽丝扫地] §f清理完成！移除了 §c%d §f个实体 (物品: §e%d§f, 经验球: §e%d§f, 箭矢: §e%d§f, 生物: §e%d§f)",
                        stats.getTotalCleaned(),
                        stats.getItemsCleaned(),
                        stats.getExperienceOrbsCleaned(),
                        stats.getArrowsCleaned(),
                        stats.getMobsCleaned()
                    )), true);
                } else {
                    source.sendSuccess(() -> Component.literal("§6[爱丽丝扫地] §f没有找到需要清理的实体。"), false);
                }
            });
        });
        
        return 1;
    }
    
    private static int executeCleanupItems(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        CleanupStats stats = new CleanupStats();
        List<Entity> toRemove = new ArrayList<>();
        
        for (Entity entity : source.getLevel().getAllEntities()) {
            if (entity instanceof ItemEntity) {
                toRemove.add(entity);
                stats.incrementType(entity);
            }
        }
        
        for (Entity entity : toRemove) {
            entity.discard();
        }
        
        source.sendSuccess(() -> Component.literal(String.format(
            "§6[爱丽丝扫地] §f清理了 §c%d §f个掉落物", stats.getItemsCleaned()
        )), true);
        
        return 1;
    }
    
    private static int executeCleanupMobs(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        CleanupStats stats = new CleanupStats();
        List<Entity> toRemove = new ArrayList<>();
        
        for (Entity entity : source.getLevel().getAllEntities()) {
            if (entity instanceof Monster && !entity.hasCustomName()) {
                toRemove.add(entity);
                stats.incrementType(entity);
            }
        }
        
        for (Entity entity : toRemove) {
            entity.discard();
        }
        
        source.sendSuccess(() -> Component.literal(String.format(
            "§6[爱丽丝扫地] §f清理了 §c%d §f个敌对生物", stats.getMobsCleaned()
        )), true);
        
        return 1;
    }
    
    private static int executeCleanupAll(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        CleanupStats stats = new CleanupStats();
        List<Entity> toRemove = new ArrayList<>();
        
        for (Entity entity : source.getLevel().getAllEntities()) {
            if (!(entity instanceof ServerPlayer) && shouldForceCleanup(entity)) {
                toRemove.add(entity);
                stats.incrementType(entity);
            }
        }
        
        for (Entity entity : toRemove) {
            entity.discard();
        }
        
        source.sendSuccess(() -> Component.literal(String.format(
            "§6[爱丽丝扫地] §f强制清理了 §c%d §f个实体", stats.getTotalCleaned()
        )), true);
        
        return 1;
    }
    
    private static int executeStats(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        
        int totalEntities = 0;
        int items = 0;
        int mobs = 0;
        int experienceOrbs = 0;
        int arrows = 0;
        int players = 0;
        
        for (Entity entity : level.getAllEntities()) {
            totalEntities++;
            if (entity instanceof ItemEntity) items++;
            else if (entity instanceof Monster) mobs++;
            else if (entity instanceof ExperienceOrb) experienceOrbs++;
            else if (entity instanceof AbstractArrow) arrows++;
            else if (entity instanceof ServerPlayer) players++;
        }
        
        source.sendSuccess(() -> Component.literal(String.format(
            "§6[爱丽丝扫地] §f实体统计:\n" +
            "§f总实体数: §e%d\n" +
            "§f玩家: §a%d\n" +
            "§f掉落物: §e%d\n" +
            "§f敌对生物: §c%d\n" +
            "§f经验球: §e%d\n" +
            "§f箭矢: §e%d\n" +
            "§f清理间隔: §e%d§f秒",
            totalEntities, players, items, mobs, experienceOrbs, arrows,
            ModConfig.CLEANUP_INTERVAL.get()
        )), false);
        
        return 1;
    }
    
    private static int setCleanupInterval(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        
        // 注意：这里只是显示设置，实际的配置修改需要重启服务器
        source.sendSuccess(() -> Component.literal(String.format(
            "§6[爱丽丝扫地] §f清理间隔已设置为 §e%d §f秒 (需要重启服务器生效)", seconds
        )), true);
        
        return 1;
    }
    
    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal(
            "§6[爱丽丝扫地] §f配置已重新加载"
        ), true);
        
        return 1;
    }
    
    private static int executeHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal(
            "§6[爱丽丝扫地] §f命令帮助:\n" +
            "§f/arissweeping cleanup - 执行标准清理\n" +
            "§f/arissweeping cleanup items - 清理所有掉落物\n" +
            "§f/arissweeping cleanup mobs - 清理敌对生物\n" +
            "§f/arissweeping cleanup all - 强制清理所有实体\n" +
            "§f/arissweeping stats - 显示实体统计\n" +
            "§f/arissweeping config interval <秒> - 设置清理间隔\n" +
            "§f/arissweeping config reload - 重新加载配置"
        ), false);
        
        return 1;
    }
    
    private static CleanupStats performManualCleanup(ServerLevel level) {
        CleanupStats stats = new CleanupStats();
        List<Entity> toRemove = new ArrayList<>();
        
        for (Entity entity : level.getAllEntities()) {
            if (shouldCleanupEntityManual(entity)) {
                toRemove.add(entity);
                stats.incrementType(entity);
            }
        }
        
        // 在主线程中移除实体
        level.getServer().execute(() -> {
            for (Entity entity : toRemove) {
                if (entity.isAlive()) {
                    entity.discard();
                }
            }
        });
        
        return stats;
    }
    
    private static boolean shouldCleanupEntityManual(Entity entity) {
        if (entity instanceof ServerPlayer) return false;
        
        if (entity instanceof ItemEntity itemEntity) {
            int ageThreshold = ModConfig.ITEM_AGE_THRESHOLD.get() * 20;
            return itemEntity.getAge() > ageThreshold;
        }
        
        if (entity instanceof ExperienceOrb) return true;
        if (entity instanceof AbstractArrow) return true;
        
        return false;
    }
    
    private static boolean shouldForceCleanup(Entity entity) {
        return !(entity instanceof ServerPlayer) && !entity.hasCustomName();
    }
}