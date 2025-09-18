package com.arisweeping.cleaning;
import com.arisweeping.core.ArisLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


import com.arisweeping.async.AsyncTaskManager;
import com.arisweeping.cleaning.filters.AnimalDensityFilter;
import com.arisweeping.cleaning.filters.ItemEntityFilter;
import com.arisweeping.cleaning.strategies.CleaningStrategy;
import com.arisweeping.cleaning.strategies.DensityBasedStrategy;
import com.arisweeping.cleaning.strategies.DistanceBasedStrategy;
import com.arisweeping.cleaning.strategies.TimeBasedStrategy;
import com.arisweeping.data.ConfigData;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

/**
 * 实体清理核心系统
 * 
 * 负责异步清理掉落物和过密畜牧实体，支持多种清理策略和过滤器
 */
public class EntityCleaner {
    
    private final AsyncTaskManager asyncTaskManager;
    private final ConfigData configData;
    private final Map<String, CleaningStrategy> strategies;
    
    // 统计信息
    private final AtomicLong totalItemsRemoved = new AtomicLong(0);
    private final AtomicLong totalAnimalsRemoved = new AtomicLong(0);
    private final AtomicLong totalOperations = new AtomicLong(0);
    
    // 过滤器
    private final ItemEntityFilter itemFilter;
    private final AnimalDensityFilter animalFilter;
    
    public EntityCleaner(AsyncTaskManager asyncTaskManager, ConfigData configData) {
        this.asyncTaskManager = asyncTaskManager;
        this.configData = configData;
        
        // 初始化过滤器
        this.itemFilter = new ItemEntityFilter(configData);
        this.animalFilter = new AnimalDensityFilter(configData);
        
        // 初始化清理策略
        this.strategies = new ConcurrentHashMap<>();
        initializeStrategies();
        
        ArisLogger.info("EntityCleaner initialized with {} strategies", strategies.size());
    }
    
    /**
     * 初始化清理策略
     */
    private void initializeStrategies() {
        strategies.put("time", new TimeBasedStrategy(configData));
        strategies.put("distance", new DistanceBasedStrategy(configData));
        strategies.put("density", new DensityBasedStrategy(configData));
    }
    
    /**
     * 执行完整的清理操作
     */
    public CompletableFuture<CleaningResult> performCleaningOperation(ServerLevel level, CleaningRequest request) {
        return asyncTaskManager.submitTask(() -> {
            ArisLogger.info("Starting cleaning operation: {}", request);
            
            long startTime = System.currentTimeMillis();
            CleaningResult.Builder resultBuilder = CleaningResult.builder()
                .setTaskId(request.getTaskId())
                .setStartTime(startTime)
                .setLevel(level.dimension().toString());
            
            try {
                // 执行物品清理
                if (request.shouldCleanItems() && configData.itemCleaning.enabled) {
                    CleaningResult itemResult = cleanItems(level, request);
                    resultBuilder.addItemsRemoved(itemResult.getItemsRemoved());
                    resultBuilder.addRemovedEntities(itemResult.getRemovedEntities());
                }
                
                // 执行动物清理
                if (request.shouldCleanAnimals() && configData.animalCleaning.enabled) {
                    CleaningResult animalResult = cleanAnimals(level, request);
                    resultBuilder.addAnimalsRemoved(animalResult.getAnimalsRemoved());
                    resultBuilder.addRemovedEntities(animalResult.getRemovedEntities());
                }
                
                long endTime = System.currentTimeMillis();
                CleaningResult result = resultBuilder
                    .setEndTime(endTime)
                    .setDuration(endTime - startTime)
                    .setSuccessful(true)
                    .build();
                
                // 更新统计信息
                updateStatistics(result);
                
                ArisLogger.info("Cleaning operation completed: {} items, {} animals removed in {}ms", 
                    result.getItemsRemoved(), result.getAnimalsRemoved(), result.getDuration());
                
                return result;
                
            } catch (Exception e) {
                ArisLogger.error("Cleaning operation failed", e);
                return resultBuilder
                    .setEndTime(System.currentTimeMillis())
                    .setSuccessful(false)
                    .setErrorMessage(e.getMessage())
                    .build();
            }
        });
    }
    
    /**
     * 清理物品实体
     */
    private CleaningResult cleanItems(ServerLevel level, CleaningRequest request) {
        ArisLogger.debug("Starting item cleaning for level: {}", level.dimension());
        
        List<ItemEntity> candidateItems = findItemEntities(level, request);
        List<ItemEntity> itemsToRemove = itemFilter.filter(candidateItems);
        
        // 应用清理策略
        String strategyName = configData.itemCleaning.strategy;
        CleaningStrategy strategy = strategies.get(strategyName);
        if (strategy != null) {
            itemsToRemove = strategy.applyStrategy(itemsToRemove.stream()
                .map(entity -> (Entity) entity)
                .collect(Collectors.toList()))
                .stream()
                .map(entity -> (ItemEntity) entity)
                .collect(Collectors.toList());
        }
        
        // 安全移除实体
        List<EntityRemovalInfo> removedEntities = new ArrayList<>();
        AtomicInteger removedCount = new AtomicInteger(0);
        
        itemsToRemove.forEach(item -> {
            if (safelyRemoveEntity(item)) {
                removedEntities.add(EntityRemovalInfo.forItem(item));
                removedCount.incrementAndGet();
            }
        });
        
        return CleaningResult.builder()
            .setItemsRemoved(removedCount.get())
            .addRemovedEntities(removedEntities)
            .setSuccessful(true)
            .build();
    }
    
    /**
     * 清理动物实体
     */
    private CleaningResult cleanAnimals(ServerLevel level, CleaningRequest request) {
        ArisLogger.debug("Starting animal cleaning for level: {}", level.dimension());
        
        List<Animal> candidateAnimals = findAnimalEntities(level, request);
        List<Animal> animalsToRemove = animalFilter.filter(candidateAnimals);
        
        // 应用清理策略
        String strategyName = configData.animalCleaning.strategy;
        CleaningStrategy strategy = strategies.get(strategyName);
        if (strategy != null) {
            animalsToRemove = strategy.applyStrategy(animalsToRemove.stream()
                .map(entity -> (Entity) entity)
                .collect(Collectors.toList()))
                .stream()
                .map(entity -> (Animal) entity)
                .collect(Collectors.toList());
        }
        
        // 安全移除实体
        List<EntityRemovalInfo> removedEntities = new ArrayList<>();
        AtomicInteger removedCount = new AtomicInteger(0);
        
        animalsToRemove.forEach(animal -> {
            if (safelyRemoveEntity(animal)) {
                removedEntities.add(EntityRemovalInfo.forAnimal(animal));
                removedCount.incrementAndGet();
            }
        });
        
        return CleaningResult.builder()
            .setAnimalsRemoved(removedCount.get())
            .addRemovedEntities(removedEntities)
            .setSuccessful(true)
            .build();
    }
    
    /**
     * 查找物品实体
     */
    private List<ItemEntity> findItemEntities(ServerLevel level, CleaningRequest request) {
        List<ItemEntity> items = new ArrayList<>();
        
        // 根据请求范围查找实体
        if (request.hasSpecificChunks()) {
            // 指定区块范围
            for (ChunkPos chunkPos : request.getChunks()) {
                // 使用level.getEntities来获取指定区块范围内的实体
                AABB chunkAABB = new AABB(
                    chunkPos.getMinBlockX(), level.getMinBuildHeight(), chunkPos.getMinBlockZ(),
                    chunkPos.getMaxBlockX() + 1, level.getMaxBuildHeight(), chunkPos.getMaxBlockZ() + 1
                );
                level.getEntities(null, chunkAABB)
                    .stream()
                    .filter(entity -> entity instanceof ItemEntity)
                    .map(entity -> (ItemEntity) entity)
                    .forEach(items::add);
            }
        } else if (request.hasPlayerRadius()) {
            // 玩家周围范围
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(request.getPlayerUUID());
            if (player != null) {
                double radius = request.getRadius();
                level.getEntitiesOfClass(ItemEntity.class, 
                    player.getBoundingBox().inflate(radius))
                    .forEach(items::add);
            }
        } else {
            // 全世界范围
            level.getAllEntities().forEach(entity -> {
                if (entity instanceof ItemEntity) {
                    items.add((ItemEntity) entity);
                }
            });
        }
        
        return items;
    }
    
    /**
     * 查找动物实体
     */
    private List<Animal> findAnimalEntities(ServerLevel level, CleaningRequest request) {
        List<Animal> animals = new ArrayList<>();
        
        // 根据请求范围查找实体
        if (request.hasSpecificChunks()) {
            // 指定区块范围
            for (ChunkPos chunkPos : request.getChunks()) {
                // 使用level.getEntities来获取指定区块范围内的实体
                AABB chunkAABB = new AABB(
                    chunkPos.getMinBlockX(), level.getMinBuildHeight(), chunkPos.getMinBlockZ(),
                    chunkPos.getMaxBlockX() + 1, level.getMaxBuildHeight(), chunkPos.getMaxBlockZ() + 1
                );
                level.getEntities(null, chunkAABB)
                    .stream()
                    .filter(entity -> entity instanceof Animal)
                    .map(entity -> (Animal) entity)
                    .forEach(animals::add);
            }
        } else if (request.hasPlayerRadius()) {
            // 玩家周围范围
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(request.getPlayerUUID());
            if (player != null) {
                double radius = request.getRadius();
                level.getEntitiesOfClass(Animal.class, 
                    player.getBoundingBox().inflate(radius))
                    .forEach(animals::add);
            }
        } else {
            // 全世界范围
            level.getAllEntities().forEach(entity -> {
                if (entity instanceof Animal) {
                    animals.add((Animal) entity);
                }
            });
        }
        
        return animals;
    }
    
    /**
     * 安全移除实体
     */
    private boolean safelyRemoveEntity(Entity entity) {
        try {
            // 检查实体是否仍然有效
            if (!entity.isAlive() || entity.isRemoved()) {
                return false;
            }
            
            // 在主线程中安全移除
            entity.level().getServer().executeIfPossible(() -> {
                entity.discard();
            });
            
            return true;
            
        } catch (Exception e) {
            ArisLogger.error("Failed to remove entity: {}", entity, e);
            return false;
        }
    }
    
    /**
     * 更新统计信息
     */
    private void updateStatistics(CleaningResult result) {
        totalItemsRemoved.addAndGet(result.getItemsRemoved());
        totalAnimalsRemoved.addAndGet(result.getAnimalsRemoved());
        totalOperations.incrementAndGet();
    }
    
    /**
     * 获取清理统计信息
     */
    public CleaningStatistics getStatistics() {
        return new CleaningStatistics(
            totalOperations.get(),
            totalItemsRemoved.get(),
            totalAnimalsRemoved.get()
        );
    }
    
    /**
     * 重置统计信息
     */
    public void resetStatistics() {
        totalOperations.set(0);
        totalItemsRemoved.set(0);
        totalAnimalsRemoved.set(0);
    }
    
    /**
     * 清理统计信息
     */
    public static class CleaningStatistics {
        private final long totalOperations;
        private final long totalItemsRemoved;
        private final long totalAnimalsRemoved;
        
        public CleaningStatistics(long totalOperations, long totalItemsRemoved, long totalAnimalsRemoved) {
            this.totalOperations = totalOperations;
            this.totalItemsRemoved = totalItemsRemoved;
            this.totalAnimalsRemoved = totalAnimalsRemoved;
        }
        
        public long getTotalOperations() { return totalOperations; }
        public long getTotalItemsRemoved() { return totalItemsRemoved; }
        public long getTotalAnimalsRemoved() { return totalAnimalsRemoved; }
        
        @Override
        public String toString() {
            return String.format("CleaningStatistics{operations=%d, items=%d, animals=%d}",
                totalOperations, totalItemsRemoved, totalAnimalsRemoved);
        }
    }
}