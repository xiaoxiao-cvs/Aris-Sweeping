package com.arisweeping.data;

import com.arisweeping.core.Constants;

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
    
    public ConfigData() {
        // 使用默认值初始化
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
}