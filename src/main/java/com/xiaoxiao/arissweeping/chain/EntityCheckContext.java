package com.xiaoxiao.arissweeping.chain;

import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.util.CleanupStats;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

/**
 * 实体检查上下文
 * 在责任链中传递检查所需的信息
 */
public class EntityCheckContext {
    
    private final ModConfig config;
    private final World world;
    private final Chunk chunk;
    private final Set<Player> nearbyPlayers;
    private final Map<String, Object> properties;
    private final long currentTime;
    
    // 统计信息
    private int entitiesInChunk = 0;
    private int itemsInChunk = 0;
    private int mobsInChunk = 0;
    
    // 密度阈值常量
    private static final int MAX_ENTITIES_PER_CHUNK = 100;
    private static final int MAX_ITEMS_PER_CHUNK = 50;
    private static final int MAX_MOBS_PER_CHUNK = 20;
    
    public EntityCheckContext(ModConfig config, World world, Chunk chunk) {
        this.config = config;
        this.world = world;
        this.chunk = chunk;
        this.nearbyPlayers = new HashSet<>();
        this.properties = new HashMap<>();
        this.currentTime = System.currentTimeMillis();
        
        // 查找附近的玩家
        findNearbyPlayers();
    }
    
    private void findNearbyPlayers() {
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        
        for (Player player : world.getPlayers()) {
            Chunk playerChunk = player.getLocation().getChunk();
            int playerChunkX = playerChunk.getX();
            int playerChunkZ = playerChunk.getZ();
            
            // 检查玩家是否在附近的区块中（3x3区域）
            if (Math.abs(playerChunkX - chunkX) <= 1 && Math.abs(playerChunkZ - chunkZ) <= 1) {
                nearbyPlayers.add(player);
            }
        }
    }
    
    // Getters
    public ModConfig getConfig() {
        return config;
    }
    
    public World getWorld() {
        return world;
    }
    
    public Chunk getChunk() {
        return chunk;
    }
    
    public Set<Player> getNearbyPlayers() {
        return nearbyPlayers;
    }
    
    public long getCurrentTime() {
        return currentTime;
    }
    
    public boolean hasNearbyPlayers() {
        return !nearbyPlayers.isEmpty();
    }
    
    // 属性管理
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, Class<T> type) {
        Object value = properties.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    public <T> T getProperty(String key, Class<T> type, T defaultValue) {
        T value = getProperty(key, type);
        return value != null ? value : defaultValue;
    }
    
    // 统计信息管理
    public void incrementEntitiesInChunk() {
        entitiesInChunk++;
    }
    
    public void incrementItemsInChunk() {
        itemsInChunk++;
    }
    
    public void incrementMobsInChunk() {
        mobsInChunk++;
    }
    
    public int getEntitiesInChunk() {
        return entitiesInChunk;
    }
    
    public int getItemsInChunk() {
        return itemsInChunk;
    }
    
    public int getMobsInChunk() {
        return mobsInChunk;
    }
    
    // 便捷方法
    public boolean isChunkOverloaded() {
        return entitiesInChunk > MAX_ENTITIES_PER_CHUNK;
    }
    
    public boolean hasExcessiveItems() {
        return itemsInChunk > MAX_ITEMS_PER_CHUNK;
    }
    
    public boolean hasExcessiveMobs() {
        return mobsInChunk > MAX_MOBS_PER_CHUNK;
    }
    
    // Setter methods for context modification
    public void setNearbyPlayers(java.util.List<Player> players) {
        this.nearbyPlayers.clear();
        if (players != null) {
            this.nearbyPlayers.addAll(players);
        }
    }
    
    public void setCurrentTime(long time) {
        // Note: currentTime is final, this method is for compatibility
        // In practice, time should be set during construction
    }
    

    
    public void setStats(Object stats) {
        this.setProperty("stats", stats);
    }
    
    public CleanupStats getStats() {
        Object stats = getProperty("stats", Object.class);
        return stats instanceof CleanupStats ? (CleanupStats) stats : null;
    }
    
}