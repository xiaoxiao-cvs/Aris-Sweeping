package com.xiaoxiao.arissweeping.livestock;

import com.xiaoxiao.arissweeping.util.EntityHotspotDetector.LivestockHotspotInfo;
import org.bukkit.entity.EntityType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 畜牧业违规信息类 - 存储违规的详细信息
 */
public class LivestockViolationInfo {
    private final LivestockHotspotInfo hotspotInfo;
    private final int animalCount;
    private final int excessCount;
    private final List<String> animalTypes;
    private final double performanceImpact;
    private final boolean isPerformanceCritical;
    private final long detectionTime;
    private final int cleanupPriority;
    
    public LivestockViolationInfo(LivestockHotspotInfo hotspotInfo) {
        this.hotspotInfo = hotspotInfo;
        this.animalCount = hotspotInfo.getTotalAnimals();
        this.excessCount = Math.max(0, animalCount - getConfigLimit());
        this.animalTypes = buildAnimalTypesList(hotspotInfo.getAnimalCounts());
        this.performanceImpact = hotspotInfo.getPerformanceImpact();
        this.isPerformanceCritical = hotspotInfo.getSparkMetrics() != null && 
            hotspotInfo.getSparkMetrics().isPerformanceCritical();
        this.detectionTime = System.currentTimeMillis();
        this.cleanupPriority = hotspotInfo.getCleanupPriority();
    }
    
    /**
     * 构建动物类型列表
     */
    private static List<String> buildAnimalTypesList(Map<EntityType, Integer> animalCounts) {
        return animalCounts.entrySet().stream()
            .map(entry -> entry.getKey().name() + "(" + entry.getValue() + ")")
            .collect(Collectors.toList());
    }
    
    /**
     * 获取配置限制（这里使用默认值，实际应该从配置中获取）
     */
    private static int getConfigLimit() {
        return 30; // 可以从配置中获取
    }
    
    /**
     * 获取位置字符串
     */
    public String getLocationString() {
        return String.format("%s (%d, %d)", 
            hotspotInfo.getWorldName(), 
            hotspotInfo.getChunkX() * 16, 
            hotspotInfo.getChunkZ() * 16);
    }
    
    /**
     * 获取详细描述
     */
    public String getDetailedDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("位置: ").append(getLocationString()).append("\n");
        desc.append("动物数量: ").append(animalCount).append("\n");
        desc.append("超出数量: ").append(excessCount).append("\n");
        desc.append("性能影响: ").append(String.format("%.2f", performanceImpact)).append("\n");
        desc.append("清理优先级: ").append(cleanupPriority).append("\n");
        desc.append("动物类型: ").append(String.join(", ", animalTypes));
        return desc.toString();
    }
    
    /**
     * 检查是否为紧急情况
     */
    public boolean isEmergency() {
        return isPerformanceCritical || excessCount > 100 || performanceImpact > 0.9;
    }
    
    /**
     * 获取违规严重程度
     */
    public ViolationSeverity getSeverity() {
        if (isPerformanceCritical || excessCount > 100) {
            return ViolationSeverity.CRITICAL;
        } else if (excessCount > 50 || performanceImpact > 0.6) {
            return ViolationSeverity.HIGH;
        } else if (excessCount > 20 || performanceImpact > 0.3) {
            return ViolationSeverity.MEDIUM;
        } else {
            return ViolationSeverity.LOW;
        }
    }
    
    /**
     * 违规严重程度枚举
     */
    public enum ViolationSeverity {
        LOW("低", 1),
        MEDIUM("中", 2),
        HIGH("高", 3),
        CRITICAL("严重", 4);
        
        private final String displayName;
        private final int priority;
        
        ViolationSeverity(String displayName, int priority) {
            this.displayName = displayName;
            this.priority = priority;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public int getPriority() {
            return priority;
        }
    }
    
    // Getter方法
    public LivestockHotspotInfo getHotspotInfo() {
        return hotspotInfo;
    }
    
    public int getAnimalCount() {
        return animalCount;
    }
    
    public int getExcessCount() {
        return excessCount;
    }
    
    public List<String> getAnimalTypes() {
        return animalTypes;
    }
    
    public double getPerformanceImpact() {
        return performanceImpact;
    }
    
    public boolean isPerformanceCritical() {
        return isPerformanceCritical;
    }
    
    public long getDetectionTime() {
        return detectionTime;
    }
    
    public int getCleanupPriority() {
        return cleanupPriority;
    }
    
    /**
     * 获取违规持续时间（毫秒）
     */
    public long getViolationDuration() {
        return System.currentTimeMillis() - detectionTime;
    }
    
    /**
     * 检查违规是否已过期
     */
    public boolean isExpired(long maxAgeMs) {
        return getViolationDuration() > maxAgeMs;
    }
    
    @Override
    public String toString() {
        return String.format("LivestockViolation{location=%s, animals=%d, excess=%d, severity=%s}",
            getLocationString(), animalCount, excessCount, getSeverity().getDisplayName());
    }
}