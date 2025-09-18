package com.arisweeping.cleaning;

import java.util.*;

/**
 * 清理请求类
 * 
 * 封装清理操作的参数和配置
 */
public class CleaningRequest {
    private final UUID taskId;
    private final boolean cleanItems;
    private final boolean cleanAnimals;
    private final UUID playerUUID;
    private final double radius;
    private final Set<net.minecraft.world.level.ChunkPos> chunks;
    private final Map<String, Object> parameters;
    
    private CleaningRequest(Builder builder) {
        this.taskId = builder.taskId;
        this.cleanItems = builder.cleanItems;
        this.cleanAnimals = builder.cleanAnimals;
        this.playerUUID = builder.playerUUID;
        this.radius = builder.radius;
        this.chunks = builder.chunks != null ? Set.copyOf(builder.chunks) : Collections.emptySet();
        this.parameters = builder.parameters != null ? Map.copyOf(builder.parameters) : Collections.emptyMap();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public UUID getTaskId() { return taskId; }
    public boolean shouldCleanItems() { return cleanItems; }
    public boolean shouldCleanAnimals() { return cleanAnimals; }
    public UUID getPlayerUUID() { return playerUUID; }
    public double getRadius() { return radius; }
    public Set<net.minecraft.world.level.ChunkPos> getChunks() { return chunks; }
    public Map<String, Object> getParameters() { return parameters; }
    
    public boolean hasSpecificChunks() {
        return chunks != null && !chunks.isEmpty();
    }
    
    public boolean hasPlayerRadius() {
        return playerUUID != null && radius > 0;
    }
    
    @Override
    public String toString() {
        return String.format("CleaningRequest{taskId=%s, items=%s, animals=%s, player=%s, radius=%.1f, chunks=%d}",
            taskId, cleanItems, cleanAnimals, playerUUID, radius, chunks.size());
    }
    
    public static class Builder {
        private UUID taskId = UUID.randomUUID();
        private boolean cleanItems = true;
        private boolean cleanAnimals = true;
        private UUID playerUUID;
        private double radius = 0;
        private Set<net.minecraft.world.level.ChunkPos> chunks;
        private Map<String, Object> parameters;
        
        public Builder setTaskId(UUID taskId) {
            this.taskId = taskId;
            return this;
        }
        
        public Builder setCleanItems(boolean cleanItems) {
            this.cleanItems = cleanItems;
            return this;
        }
        
        public Builder setCleanAnimals(boolean cleanAnimals) {
            this.cleanAnimals = cleanAnimals;
            return this;
        }
        
        public Builder setPlayerRadius(UUID playerUUID, double radius) {
            this.playerUUID = playerUUID;
            this.radius = radius;
            return this;
        }
        
        public Builder setChunks(Set<net.minecraft.world.level.ChunkPos> chunks) {
            this.chunks = chunks;
            return this;
        }
        
        public Builder setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }
        
        public CleaningRequest build() {
            return new CleaningRequest(this);
        }
    }
}