package com.arisweeping.data;

import com.arisweeping.core.Constants;
import java.util.List;
import java.util.ArrayList;

/**
 * 配置数据管理类
 * 
 * 负责存储和管理模组的所有配置选项
 */
public class ConfigData {
    
    // 清理配置
    private boolean itemCleaningEnabled = true;
    private boolean animalCleaningEnabled = false;
    private int cleaningIntervalTicks = Constants.Cleaning.DEFAULT_CLEANING_INTERVAL_TICKS;
    
    // 物品清理配置
    private int itemAgeThreshold = Constants.Cleaning.DEFAULT_ITEM_AGE_THRESHOLD;
    private double itemCleaningRadius = 64.0;
    private boolean respectItemWhitelist = false;
    
    // 动物清理配置
    private double animalDensityRadius = Constants.Cleaning.DEFAULT_ANIMAL_DENSITY_RADIUS;
    private int animalDensityThreshold = Constants.Cleaning.DEFAULT_ANIMAL_DENSITY_THRESHOLD;
    private boolean protectBreedingAnimals = true;
    
    // 任务管理配置
    private int maxUndoOperations = Constants.TaskManagement.MAX_UNDO_OPERATIONS;
    private long undoTimeoutMinutes = Constants.TaskManagement.UNDO_TIMEOUT_MINUTES;
    private boolean enableTaskHistory = true;
    
    // 性能配置
    private int threadPoolSize = Constants.AsyncProcessing.CORE_THREAD_POOL_SIZE;
    private boolean enablePerformanceMonitoring = true;
    private int memoryThresholdMB = 512;
    
    // GUI配置
    private boolean showHUD = true;
    private boolean showStatistics = true;
    private int guiScale = 1;
    
    // 配置对象
    public final ItemCleaningConfig itemCleaning;
    public final AnimalCleaningConfig animalCleaning;
    
    public ConfigData() {
        // 使用默认值初始化
        this.itemCleaning = new ItemCleaningConfig();
        this.animalCleaning = new AnimalCleaningConfig();
    }
    
    // Getter和Setter方法
    
    public boolean isItemCleaningEnabled() {
        return itemCleaningEnabled;
    }
    
    public void setItemCleaningEnabled(boolean itemCleaningEnabled) {
        this.itemCleaningEnabled = itemCleaningEnabled;
    }
    
    public boolean isAnimalCleaningEnabled() {
        return animalCleaningEnabled;
    }
    
    public void setAnimalCleaningEnabled(boolean animalCleaningEnabled) {
        this.animalCleaningEnabled = animalCleaningEnabled;
    }
    
    public int getCleaningIntervalTicks() {
        return cleaningIntervalTicks;
    }
    
    public void setCleaningIntervalTicks(int cleaningIntervalTicks) {
        this.cleaningIntervalTicks = Math.max(20, cleaningIntervalTicks); // 最小1秒
    }
    
    public int getItemAgeThreshold() {
        return itemAgeThreshold;
    }
    
    public void setItemAgeThreshold(int itemAgeThreshold) {
        this.itemAgeThreshold = Math.max(100, itemAgeThreshold); // 最小5秒
    }
    
    public double getItemCleaningRadius() {
        return itemCleaningRadius;
    }
    
    public void setItemCleaningRadius(double itemCleaningRadius) {
        this.itemCleaningRadius = Math.min(Constants.Cleaning.MAX_CLEANING_RADIUS, Math.max(16.0, itemCleaningRadius));
    }
    
    public boolean isRespectItemWhitelist() {
        return respectItemWhitelist;
    }
    
    public void setRespectItemWhitelist(boolean respectItemWhitelist) {
        this.respectItemWhitelist = respectItemWhitelist;
    }
    
    public double getAnimalDensityRadius() {
        return animalDensityRadius;
    }
    
    public void setAnimalDensityRadius(double animalDensityRadius) {
        this.animalDensityRadius = Math.max(8.0, animalDensityRadius);
    }
    
    public int getAnimalDensityThreshold() {
        return animalDensityThreshold;
    }
    
    public void setAnimalDensityThreshold(int animalDensityThreshold) {
        this.animalDensityThreshold = Math.max(1, animalDensityThreshold);
    }
    
    public boolean isProtectBreedingAnimals() {
        return protectBreedingAnimals;
    }
    
    public void setProtectBreedingAnimals(boolean protectBreedingAnimals) {
        this.protectBreedingAnimals = protectBreedingAnimals;
    }
    
    public int getMaxUndoOperations() {
        return maxUndoOperations;
    }
    
    public void setMaxUndoOperations(int maxUndoOperations) {
        this.maxUndoOperations = Math.max(1, Math.min(50, maxUndoOperations));
    }
    
    public long getUndoTimeoutMinutes() {
        return undoTimeoutMinutes;
    }
    
    public void setUndoTimeoutMinutes(long undoTimeoutMinutes) {
        this.undoTimeoutMinutes = Math.max(1, undoTimeoutMinutes);
    }
    
    public boolean isEnableTaskHistory() {
        return enableTaskHistory;
    }
    
    public void setEnableTaskHistory(boolean enableTaskHistory) {
        this.enableTaskHistory = enableTaskHistory;
    }
    
    public int getThreadPoolSize() {
        return threadPoolSize;
    }
    
    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = Math.max(1, Math.min(Constants.AsyncProcessing.MAX_THREAD_POOL_SIZE, threadPoolSize));
    }
    
    public boolean isEnablePerformanceMonitoring() {
        return enablePerformanceMonitoring;
    }
    
    public void setEnablePerformanceMonitoring(boolean enablePerformanceMonitoring) {
        this.enablePerformanceMonitoring = enablePerformanceMonitoring;
    }
    
    public int getMemoryThresholdMB() {
        return memoryThresholdMB;
    }
    
    public void setMemoryThresholdMB(int memoryThresholdMB) {
        this.memoryThresholdMB = Math.max(128, memoryThresholdMB);
    }
    
    public boolean isShowHUD() {
        return showHUD;
    }
    
    public void setShowHUD(boolean showHUD) {
        this.showHUD = showHUD;
    }
    
    public boolean isShowStatistics() {
        return showStatistics;
    }
    
    public void setShowStatistics(boolean showStatistics) {
        this.showStatistics = showStatistics;
    }
    
    public int getGuiScale() {
        return guiScale;
    }
    
    public void setGuiScale(int guiScale) {
        this.guiScale = Math.max(1, Math.min(4, guiScale));
    }
    
    /**
     * 验证配置数据的有效性
     */
    public boolean validate() {
        try {
            // 验证关键配置项
            if (cleaningIntervalTicks < 20) return false;
            if (itemAgeThreshold < 100) return false;
            if (itemCleaningRadius <= 0 || itemCleaningRadius > Constants.Cleaning.MAX_CLEANING_RADIUS) return false;
            if (animalDensityRadius <= 0) return false;
            if (animalDensityThreshold <= 0) return false;
            if (maxUndoOperations <= 0 || maxUndoOperations > 50) return false;
            if (undoTimeoutMinutes <= 0) return false;
            if (threadPoolSize <= 0 || threadPoolSize > Constants.AsyncProcessing.MAX_THREAD_POOL_SIZE) return false;
            if (memoryThresholdMB < 128) return false;
            if (guiScale < 1 || guiScale > 4) return false;
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 重置为默认配置
     */
    public void resetToDefaults() {
        this.itemCleaningEnabled = true;
        this.animalCleaningEnabled = false;
        this.cleaningIntervalTicks = Constants.Cleaning.DEFAULT_CLEANING_INTERVAL_TICKS;
        this.itemAgeThreshold = Constants.Cleaning.DEFAULT_ITEM_AGE_THRESHOLD;
        this.itemCleaningRadius = 64.0;
        this.respectItemWhitelist = false;
        this.animalDensityRadius = Constants.Cleaning.DEFAULT_ANIMAL_DENSITY_RADIUS;
        this.animalDensityThreshold = Constants.Cleaning.DEFAULT_ANIMAL_DENSITY_THRESHOLD;
        this.protectBreedingAnimals = true;
        this.maxUndoOperations = Constants.TaskManagement.MAX_UNDO_OPERATIONS;
        this.undoTimeoutMinutes = Constants.TaskManagement.UNDO_TIMEOUT_MINUTES;
        this.enableTaskHistory = true;
        this.threadPoolSize = Constants.AsyncProcessing.CORE_THREAD_POOL_SIZE;
        this.enablePerformanceMonitoring = true;
        this.memoryThresholdMB = 512;
        this.showHUD = true;
        this.showStatistics = true;
        this.guiScale = 1;
    }
    
    @Override
    public String toString() {
        return "ConfigData{" +
                "itemCleaningEnabled=" + itemCleaningEnabled +
                ", animalCleaningEnabled=" + animalCleaningEnabled +
                ", cleaningIntervalTicks=" + cleaningIntervalTicks +
                ", itemAgeThreshold=" + itemAgeThreshold +
                ", itemCleaningRadius=" + itemCleaningRadius +
                ", animalDensityThreshold=" + animalDensityThreshold +
                ", threadPoolSize=" + threadPoolSize +
                '}';
    }
    
    /**
     * 获取物品清理配置
     */
    public ItemCleaningConfig getItemCleaningConfig() {
        return itemCleaning;
    }
    
    /**
     * 获取动物清理配置
     */
    public AnimalCleaningConfig getAnimalCleaningConfig() {
        return animalCleaning;
    }
    
    /**
     * 保存配置到文件
     */
    public void save() {
        // TODO: 实现配置保存功能
        // 可以保存到文件或使用Forge的配置系统
    }
    
    /**
     * 物品清理配置类
     */
    public class ItemCleaningConfig {
        public boolean enabled = true;
        public String strategy = "age_based";
        private List<String> itemWhitelist = new ArrayList<>();
        private List<String> itemBlacklist = new ArrayList<>();
        private int itemLifetimeSeconds = 300; // 5分钟
        private int chunkRange = 3;
        private int minItemCount = 10;
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getStrategy() { return strategy; }
        public void setStrategy(String strategy) { this.strategy = strategy; }
        
        public List<String> getItemWhitelist() { return new ArrayList<>(itemWhitelist); }
        public void setItemWhitelist(List<String> itemWhitelist) { this.itemWhitelist = new ArrayList<>(itemWhitelist); }
        
        public List<String> getItemBlacklist() { return new ArrayList<>(itemBlacklist); }
        public void setItemBlacklist(List<String> itemBlacklist) { this.itemBlacklist = new ArrayList<>(itemBlacklist); }
        
        public int getItemLifetimeSeconds() { return itemLifetimeSeconds; }
        public void setItemLifetimeSeconds(int itemLifetimeSeconds) { this.itemLifetimeSeconds = itemLifetimeSeconds; }
        
        public int getChunkRange() { return chunkRange; }
        public void setChunkRange(int chunkRange) { this.chunkRange = chunkRange; }
        
        public int getMinItemCount() { return minItemCount; }
        public void setMinItemCount(int minItemCount) { this.minItemCount = minItemCount; }
    }
    
    /**
     * 动物清理配置类
     */
    public class AnimalCleaningConfig {
        public boolean enabled = false;
        public String strategy = "density_based";
        private int maxAnimalsPerChunk = 20;
        private int checkRadius = 5;
        private boolean protectBreeding = true;
        private boolean protectBabies = true;
        private List<String> excludedEntityTypes = new ArrayList<>();
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getStrategy() { return strategy; }
        public void setStrategy(String strategy) { this.strategy = strategy; }
        
        public int getMaxAnimalsPerChunk() { return maxAnimalsPerChunk; }
        public void setMaxAnimalsPerChunk(int maxAnimalsPerChunk) { this.maxAnimalsPerChunk = maxAnimalsPerChunk; }
        
        public int getCheckRadius() { return checkRadius; }
        public void setCheckRadius(int checkRadius) { this.checkRadius = checkRadius; }
        
        public boolean isProtectBreeding() { return protectBreeding; }
        public void setProtectBreeding(boolean protectBreeding) { this.protectBreeding = protectBreeding; }
        
        public boolean isProtectBabies() { return protectBabies; }
        public void setProtectBabies(boolean protectBabies) { this.protectBabies = protectBabies; }
        
        public List<String> getExcludedEntityTypes() { return new ArrayList<>(excludedEntityTypes); }
        public void setExcludedEntityTypes(List<String> excludedEntityTypes) { this.excludedEntityTypes = new ArrayList<>(excludedEntityTypes); }
    }
}