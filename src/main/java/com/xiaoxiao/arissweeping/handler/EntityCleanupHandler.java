package com.xiaoxiao.arissweeping.handler;

import com.xiaoxiao.arissweeping.ArisSweeping;
import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.util.CleanupStats;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber(modid = ArisSweeping.MODID)
public class EntityCleanupHandler {
    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(2);
    private static int tickCounter = 0;
    private static boolean isCleanupRunning = false;
    
    public static void init() {
        // 启动定时清理任务
        EXECUTOR.scheduleAtFixedRate(() -> {
            if (!isCleanupRunning) {
                performCleanup();
            }
        }, ModConfig.CLEANUP_INTERVAL.get(), ModConfig.CLEANUP_INTERVAL.get(), TimeUnit.SECONDS);
        
        ArisSweeping.LOGGER.info("Entity cleanup handler initialized with interval: {} seconds", 
                ModConfig.CLEANUP_INTERVAL.get());
    }
    
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        tickCounter++;
        
        // 每20tick（1秒）检查一次实体密度
        if (tickCounter % 20 == 0) {
            checkEntityDensity();
        }
    }
    
    private static void checkEntityDensity() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        
        for (ServerLevel level : server.getAllLevels()) {
            if (ModConfig.ASYNC_CLEANUP.get()) {
                CompletableFuture.runAsync(() -> checkLevelEntityDensity(level), EXECUTOR);
            } else {
                checkLevelEntityDensity(level);
            }
        }
    }
    
    private static void checkLevelEntityDensity(ServerLevel level) {
        Map<ChunkPos, List<Entity>> chunkEntities = new HashMap<>();
        
        // 收集所有实体按区块分组
        for (Entity entity : level.getAllEntities()) {
            ChunkPos chunkPos = new ChunkPos(entity.blockPosition());
            chunkEntities.computeIfAbsent(chunkPos, k -> new ArrayList<>()).add(entity);
        }
        
        // 检查每个区块的实体密度
        for (Map.Entry<ChunkPos, List<Entity>> entry : chunkEntities.entrySet()) {
            List<Entity> entities = entry.getValue();
            
            // 统计不同类型的实体
            long itemCount = entities.stream().filter(e -> e instanceof ItemEntity).count();
            long totalEntityCount = entities.size();
            
            // 如果超过阈值，进行清理
            if (itemCount > ModConfig.MAX_ITEMS_PER_CHUNK.get() || 
                totalEntityCount > ModConfig.MAX_ENTITIES_PER_CHUNK.get()) {
                
                cleanupChunkEntities(level, entities);
            }
        }
    }
    
    private static void performCleanup() {
        isCleanupRunning = true;
        
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            
            CleanupStats stats = new CleanupStats();
            
            for (ServerLevel level : server.getAllLevels()) {
                if (ModConfig.ASYNC_CLEANUP.get()) {
                    CompletableFuture.runAsync(() -> cleanupLevel(level, stats), EXECUTOR)
                            .join(); // 等待完成以确保统计准确
                } else {
                    cleanupLevel(level, stats);
                }
            }
            
            // 广播清理结果
            if (ModConfig.BROADCAST_CLEANUP.get() && stats.getTotalCleaned() > 0) {
                broadcastCleanupMessage(server, stats);
            }
            
            ArisSweeping.LOGGER.info("Cleanup completed: {}", stats.toString());
            
        } finally {
            isCleanupRunning = false;
        }
    }
    
    private static void cleanupLevel(ServerLevel level, CleanupStats stats) {
        List<Entity> toRemove = new ArrayList<>();
        
        for (Entity entity : level.getAllEntities()) {
            if (shouldCleanupEntity(entity)) {
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
    }
    
    private static void cleanupChunkEntities(ServerLevel level, List<Entity> entities) {
        List<Entity> toRemove = new ArrayList<>();
        
        for (Entity entity : entities) {
            if (shouldCleanupEntity(entity)) {
                toRemove.add(entity);
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
    }
    
    private static boolean shouldCleanupEntity(Entity entity) {
        // 不清理玩家
        if (entity instanceof ServerPlayer) {
            return false;
        }
        
        // 掉落物清理
        if (entity instanceof ItemEntity itemEntity) {
            if (!ModConfig.CLEANUP_ITEMS.get()) return false;
            
            // 检查物品年龄
            int ageThreshold = ModConfig.ITEM_AGE_THRESHOLD.get() * 20; // 转换为tick
            return itemEntity.getAge() > ageThreshold;
        }
        
        // 经验球清理
        if (entity instanceof ExperienceOrb) {
            return ModConfig.CLEANUP_EXPERIENCE_ORBS.get();
        }
        
        // 箭矢清理
        if (entity instanceof AbstractArrow) {
            return ModConfig.CLEANUP_ARROWS.get();
        }
        
        // 敌对生物清理
        if (entity instanceof Monster) {
            return ModConfig.CLEANUP_HOSTILE_MOBS.get();
        }
        
        // 被动生物清理（需要更精确的判断）
        if (ModConfig.CLEANUP_PASSIVE_MOBS.get()) {
            // 这里可以添加更多被动生物的判断逻辑
            return !entity.hasCustomName() && !(entity instanceof ServerPlayer);
        }
        
        return false;
    }
    
    private static void broadcastCleanupMessage(MinecraftServer server, CleanupStats stats) {
        Component message;
        
        if (ModConfig.SHOW_CLEANUP_STATS.get()) {
            message = Component.literal(String.format(
                    "§6[爱丽丝扫地] §f清理完成！移除了 §c%d §f个实体 (物品: §e%d§f, 经验球: §e%d§f, 箭矢: §e%d§f, 生物: §e%d§f)",
                    stats.getTotalCleaned(),
                    stats.getItemsCleaned(),
                    stats.getExperienceOrbsCleaned(),
                    stats.getArrowsCleaned(),
                    stats.getMobsCleaned()
            ));
        } else {
            message = Component.literal(String.format(
                    "§6[爱丽丝扫地] §f清理完成！移除了 §c%d §f个实体",
                    stats.getTotalCleaned()
            ));
        }
        
        // 向所有在线玩家广播消息
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(message);
        }
    }
    
    public static void shutdown() {
        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}