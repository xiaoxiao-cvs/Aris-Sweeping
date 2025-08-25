package com.xiaoxiao.arissweeping.livestock;

import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.util.LivestockHotspotInfo;
import com.xiaoxiao.arissweeping.util.LivestockStatistics;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 畜牧业违规检测器 - 负责检测和分析畜牧业密度违规
 */
public class LivestockViolationDetector {
    private final ModConfig config;
    private final Map<String, Long> recentWarnings = new HashMap<>();
    
    public LivestockViolationDetector(ModConfig config) {
        this.config = config;
    }
    
    /**
     * 处理热点扫描结果
     */
    public void processHotspotResults(List<LivestockHotspotInfo> hotspots, LivestockStatistics statistics, 
                                    LivestockCleanupManager cleanupManager) {
        Map<String, LivestockViolationInfo> violations = detectViolations(hotspots);
        
        if (config.isDebugMode()) {
            logScanResults(hotspots.size(), statistics.getTotalAnimals(), violations.size(), 
                         statistics.getAveragePerformanceImpact());
        }
        
        if (!violations.isEmpty() && config.isWarningEnabled()) {
            handleViolations(violations, cleanupManager);
        }
    }
    
    /**
     * 检测违规情况
     */
    private Map<String, LivestockViolationInfo> detectViolations(List<LivestockHotspotInfo> hotspots) {
        Map<String, LivestockViolationInfo> violations = new HashMap<>();
        
        for (LivestockHotspotInfo hotspot : hotspots) {
            if (hotspot.getTotalAnimals() > config.getMaxAnimalsPerChunk()) {
                String location = buildLocationString(hotspot);
                violations.put(location, new LivestockViolationInfo(hotspot));
            }
        }
        
        return violations;
    }
    
    /**
     * 处理违规情况
     */
    private void handleViolations(Map<String, LivestockViolationInfo> violations, 
                                LivestockCleanupManager cleanupManager) {
        Map<String, LivestockViolationInfo> filteredViolations = filterRecentWarnings(violations);
        
        if (!filteredViolations.isEmpty()) {
            cleanupManager.sendWarningMessage(filteredViolations);
            cleanupManager.scheduleCleanup(filteredViolations);
            recordWarningTimes(filteredViolations);
        } else if (config.isDebugMode()) {
            config.getPlugin().getLogger().info("[LivestockViolationDetector] 所有违规位置都在冷却期内，跳过重复警告");
        }
    }
    
    /**
     * 过滤最近已经警告过的位置
     */
    private Map<String, LivestockViolationInfo> filterRecentWarnings(Map<String, LivestockViolationInfo> violations) {
        Map<String, LivestockViolationInfo> filtered = new HashMap<>();
        long currentTime = System.currentTimeMillis();
        long warningCooldownMs = config.getWarningCooldown() * 1000L;
        
        // 清理过期的警告记录
        recentWarnings.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > warningCooldownMs);
        
        // 过滤掉冷却期内的位置
        for (Map.Entry<String, LivestockViolationInfo> entry : violations.entrySet()) {
            String location = entry.getKey();
            Long lastWarningTime = recentWarnings.get(location);
            
            if (lastWarningTime == null || currentTime - lastWarningTime > warningCooldownMs) {
                filtered.put(location, entry.getValue());
            } else if (config.isDebugMode()) {
                long remainingCooldown = warningCooldownMs - (currentTime - lastWarningTime);
                config.getPlugin().getLogger().info(String.format(
                    "[LivestockViolationDetector] 位置 %s 仍在冷却期内，剩余 %d 秒", 
                    location, remainingCooldown / 1000));
            }
        }
        
        return filtered;
    }
    
    /**
     * 记录警告时间
     */
    private void recordWarningTimes(Map<String, LivestockViolationInfo> violations) {
        long currentTime = System.currentTimeMillis();
        for (String location : violations.keySet()) {
            recentWarnings.put(location, currentTime);
        }
        
        if (config.isDebugMode()) {
            config.getPlugin().getLogger().info(String.format(
                "[LivestockViolationDetector] 已记录 %d 个位置的警告时间，冷却期 %d 秒", 
                violations.size(), config.getWarningCooldown()));
        }
    }
    
    /**
     * 构建位置字符串
     */
    private String buildLocationString(LivestockHotspotInfo hotspot) {
        return String.format("%s (%d, %d)", 
            hotspot.getWorldName(), 
            hotspot.getChunkX() * 16, 
            hotspot.getChunkZ() * 16);
    }
    
    /**
     * 记录扫描结果
     */
    private void logScanResults(int hotspotsChecked, int totalAnimals, int violationCount, double avgPerformanceImpact) {
        config.getPlugin().getLogger().info(String.format(
            "[LivestockViolationDetector] 热点检查完成 - 检查热点: %d, 发现动物: %d, 违规区块: %d, 性能影响: %.2f",
            hotspotsChecked, totalAnimals, violationCount, avgPerformanceImpact
        ));
    }
    
    /**
     * 检查实体类型是否为动物
     */
    public static boolean isAnimalType(EntityType type) {
        switch (type) {
            case COW:
            case PIG:
            case SHEEP:
            case CHICKEN:
            case HORSE:
            case DONKEY:
            case MULE:
            case LLAMA:
            case TRADER_LLAMA:
            case RABBIT:
            case WOLF:
            case CAT:
            case OCELOT:
            case PARROT:
            case FOX:
            case BEE:
            case TURTLE:
            case PANDA:
            case POLAR_BEAR:
            case MOOSHROOM:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * 清理警告记录
     */
    public void clearWarningHistory() {
        recentWarnings.clear();
    }
    
    /**
     * 获取警告历史大小
     */
    public int getWarningHistorySize() {
        return recentWarnings.size();
    }
}