package com.xiaoxiao.arissweeping.livestock;

import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.util.SparkEntityMetrics;
import com.xiaoxiao.arissweeping.util.CleanupStateManager;
import com.xiaoxiao.arissweeping.cleanup.CleanupServiceManager;
import com.xiaoxiao.arissweeping.util.CleanupStateManager.CleanupType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 畜牧业清理管理器 - 负责警告消息、清理调度和执行
 */
public class LivestockCleanupManager {
    private final Plugin plugin;
    private final ModConfig config;
    private final Map<String, LivestockViolationInfo> pendingCleanups = new HashMap<>();
    private final CleanupStateManager stateManager;
    private final CleanupServiceManager serviceManager;
    private BukkitTask warningTask;
    
    public LivestockCleanupManager(Plugin plugin, ModConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.stateManager = CleanupStateManager.getInstance();
        this.serviceManager = new CleanupServiceManager((com.xiaoxiao.arissweeping.ArisSweeping) plugin);
    }
    
    /**
     * 发送警告消息
     */
    public void sendWarningMessage(Map<String, LivestockViolationInfo> violations) {
        if (!config.isWarningEnabled()) {
            return;
        }
        
        if (config.getBoolean("livestock.warning.region-based", false)) {
            sendRegionBasedWarningMessage(violations);
        } else {
            sendStandardWarningMessage(violations);
        }
    }
    
    /**
     * 发送基于区域的警告消息
     */
    private void sendRegionBasedWarningMessage(Map<String, LivestockViolationInfo> violations) {
        Map<String, List<LivestockViolationInfo>> regionGroups = groupViolationsByRegion(violations);
        
        for (Map.Entry<String, List<LivestockViolationInfo>> entry : regionGroups.entrySet()) {
            String region = entry.getKey();
            List<LivestockViolationInfo> regionViolations = entry.getValue();
            
            int totalAnimals = regionViolations.stream().mapToInt(LivestockViolationInfo::getAnimalCount).sum();
            int totalExcess = regionViolations.stream().mapToInt(LivestockViolationInfo::getExcessCount).sum();
            
            String message = String.format(
                "%s[畜牧业管理] %s区域检测到 %d 个违规位置，共 %d 只动物（超出 %d 只）",
                ChatColor.YELLOW, region, regionViolations.size(), totalAnimals, totalExcess
            );
            
            Bukkit.broadcastMessage(message);
            
            if (config.isDebugMode()) {
                for (LivestockViolationInfo violation : regionViolations) {
                    Bukkit.broadcastMessage(String.format(
                        "%s  - %s: %d只动物 (超出%d只)",
                        ChatColor.GRAY, violation.getLocationString(), 
                        violation.getAnimalCount(), violation.getExcessCount()
                    ));
                }
            }
        }
    }
    
    /**
     * 发送标准警告消息
     */
    private void sendStandardWarningMessage(Map<String, LivestockViolationInfo> violations) {
        int totalViolations = violations.size();
        int totalAnimals = violations.values().stream().mapToInt(LivestockViolationInfo::getAnimalCount).sum();
        int totalExcess = violations.values().stream().mapToInt(LivestockViolationInfo::getExcessCount).sum();
        
        // 发送详细的格式化警告消息
        for (LivestockViolationInfo violation : violations.values()) {
            sendDetailedWarningMessage(violation);
        }
        
        // 发送简要统计信息
        String mainMessage = String.format(
            "%s[畜牧业管理] 检测到 %d 个区块动物密度过高，共 %d 只动物（超出 %d 只）",
            ChatColor.YELLOW, totalViolations, totalAnimals, totalExcess
        );
        
        Bukkit.broadcastMessage(mainMessage);
        
        // 显示最严重的几个违规位置
        List<LivestockViolationInfo> sortedViolations = violations.values().stream()
            .sorted((a, b) -> Integer.compare(b.getExcessCount(), a.getExcessCount()))
            .limit(3)
            .collect(Collectors.toList());
        
        for (LivestockViolationInfo violation : sortedViolations) {
            String locationMessage = String.format(
                "%s  - %s: %d只动物 (%s严重程度)",
                ChatColor.GRAY, violation.getLocationString(), 
                violation.getAnimalCount(), violation.getSeverity().getDisplayName()
            );
            Bukkit.broadcastMessage(locationMessage);
        }
        
        if (violations.size() > 3) {
            Bukkit.broadcastMessage(String.format(
                "%s  ... 还有 %d 个其他违规位置",
                ChatColor.GRAY, violations.size() - 3
            ));
        }
    }
    
    /**
     * 发送详细的警告消息
     */
    private void sendDetailedWarningMessage(LivestockViolationInfo violation) {
        // 获取区域信息
        String regionInfo = parseRegionInfo(violation.getHotspotInfo().getWorldName(), 
            violation.getHotspotInfo().getChunkX(), violation.getHotspotInfo().getChunkZ());
        
        // 发送格式化的详细警告消息
        String title = config.getLivestockWarningMessage("title");
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', title));
        
        String separator = config.getLivestockWarningMessage("separator");
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', separator));
        
        String locationMsg = config.getLivestockWarningMessage("location")
             .replace("{region}", regionInfo)
             .replace("{world}", violation.getHotspotInfo().getWorldName())
             .replace("{x}", String.valueOf(violation.getHotspotInfo().getChunkX() * 16))
             .replace("{z}", String.valueOf(violation.getHotspotInfo().getChunkZ() * 16));
         Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', locationMsg));
        Bukkit.broadcastMessage("");
        
        // 超标情况
        String violationHeader = config.getLivestockWarningMessage("violation_header");
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', violationHeader));
        
        String detailsMsg = config.getLivestockWarningMessage("details");
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', detailsMsg));
        
        String violationMsg = config.getLivestockWarningMessage("violation")
             .replace("{current}", String.valueOf(violation.getAnimalCount()))
             .replace("{limit}", String.valueOf(config.getMaxAnimalsPerChunk()))
             .replace("{excess}", String.valueOf(violation.getExcessCount()));
         Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', violationMsg));
         
         String totalAnimalsMsg = config.getLivestockWarningMessage("total_animals")
             .replace("{total}", String.valueOf(violation.getAnimalCount()))
             .replace("{excess}", String.valueOf(violation.getExcessCount()));
         Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', totalAnimalsMsg));
        
        // 显示动物类型统计
         Map<org.bukkit.entity.EntityType, Integer> animalCounts = violation.getHotspotInfo().getAnimalCounts();
         if (animalCounts != null && !animalCounts.isEmpty()) {
             StringBuilder animalTypes = new StringBuilder();
             for (Map.Entry<org.bukkit.entity.EntityType, Integer> entry : animalCounts.entrySet()) {
                 if (animalTypes.length() > 0) animalTypes.append(", ");
                 String animalName = config.getChineseAnimalName(entry.getKey().name());
                 animalTypes.append(animalName).append("(").append(entry.getValue()).append("只)");
             }
             String animalTypesMsg = config.getLivestockWarningMessage("animal_types")
                 .replace("{types}", animalTypes.toString());
             Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', animalTypesMsg));
         }
        Bukkit.broadcastMessage("");
        
        // 清理通知
        int warningTimeMinutes = config.getWarningTimeMinutes();
        String cleanupNoticeMsg = config.getLivestockWarningMessage("cleanup_notice");
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', cleanupNoticeMsg));
        
        String cleanupTimeMsg = config.getLivestockWarningMessage("cleanup_time")
            .replace("{time}", String.valueOf(warningTimeMinutes));
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', cleanupTimeMsg));
        
        String actionReminderMsg = config.getLivestockWarningMessage("action_reminder")
             .replace("{title}", config.getPlayerTitle());
         Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', actionReminderMsg));
        Bukkit.broadcastMessage("");
        
        // 清理列表
        String cleanupListMsg = config.getLivestockWarningMessage("cleanup_list");
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', cleanupListMsg));
        
        if (animalCounts != null && !animalCounts.isEmpty()) {
            for (Map.Entry<org.bukkit.entity.EntityType, Integer> entry : animalCounts.entrySet()) {
                int toRemove = Math.min(entry.getValue(), violation.getExcessCount());
                if (toRemove > 0) {
                    String animalName = config.getChineseAnimalName(entry.getKey().name());
                    String cleanupItemMsg = config.getLivestockWarningMessage("cleanup_item")
                        .replace("{type}", animalName)
                        .replace("{count}", String.valueOf(toRemove));
                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', cleanupItemMsg));
                }
            }
        }
        Bukkit.broadcastMessage("");
    }
    
    /**
     * 根据区域分组发送违规信息
     */
    private Map<String, List<LivestockViolationInfo>> groupViolationsByRegion(Map<String, LivestockViolationInfo> violations) {
        Map<String, List<LivestockViolationInfo>> regionGroups = new HashMap<>();
        
        for (LivestockViolationInfo violation : violations.values()) {
            String region = parseRegionInfo(
                violation.getHotspotInfo().getWorldName(),
                violation.getHotspotInfo().getChunkX(),
                violation.getHotspotInfo().getChunkZ()
            );
            
            regionGroups.computeIfAbsent(region, k -> new ArrayList<>()).add(violation);
        }
        
        return regionGroups;
    }
    
    /**
     * 解析区域信息
     */
    private String parseRegionInfo(String worldName, int chunkX, int chunkZ) {
        // 将区块坐标转换为世界坐标
        int worldX = chunkX * 16;
        int worldZ = chunkZ * 16;
        
        // 首先尝试从配置的区域中查找
        String configuredRegion = config.findRegionByCoordinates(worldName, worldX, worldZ);
        if (configuredRegion != null) {
            return configuredRegion;
        }
        
        // 如果没有找到配置的区域，使用默认的方向划分逻辑
        int regionX = chunkX / 32; // 每32个区块为一个区域
        int regionZ = chunkZ / 32;
        
        String regionName;
        if (regionX >= 0 && regionZ >= 0) {
            regionName = "东北";
        } else if (regionX < 0 && regionZ >= 0) {
            regionName = "西北";
        } else if (regionX < 0 && regionZ < 0) {
            regionName = "西南";
        } else {
            regionName = "东南";
        }
        
        return String.format("%s%s区域", worldName, regionName);
    }
    
    /**
     * 调度清理任务
     */
    public void scheduleCleanup(Map<String, LivestockViolationInfo> violations) {
        if (!config.isAutoCleanupEnabled()) {
            return;
        }
        
        // 将违规信息添加到待清理列表
        synchronized (pendingCleanups) {
            pendingCleanups.putAll(violations);
        }
        
        // 检查是否有紧急情况需要立即清理
        boolean hasEmergency = violations.values().stream().anyMatch(LivestockViolationInfo::isEmergency);
        
        if (hasEmergency) {
            plugin.getLogger().warning("[LivestockCleanupManager] 检测到紧急情况，立即执行清理");
            performLivestockCleanup();
        } else {
            // 启动倒计时清理
            int warningTimeMinutes = config.getWarningTimeMinutes();
            startSmartLivestockCountdown(warningTimeMinutes, violations.size());
        }
    }
    
    /**
     * 启动智能倒计时
     */
    private void startSmartLivestockCountdown(int warningTimeMinutes, int violationCount) {
        if (warningTask != null) {
            warningTask.cancel();
        }
        
        List<Integer> warningTimes = getLivestockWarningTimes(warningTimeMinutes);
        
        warningTask = new BukkitRunnable() {
            private int timeLeft = warningTimeMinutes * 60;
            
            @Override
            public void run() {
                if (timeLeft <= 0) {
                    // 倒计时结束，执行清理
                    performLivestockCleanup();
                    cancel();
                    return;
                }
                
                // 检查是否需要发送警告
                if (warningTimes.contains(timeLeft)) {
                    sendLivestockCountdownMessage(timeLeft, violationCount);
                }
                
                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L); // 每秒执行一次
    }
    
    /**
     * 获取警告时间点
     */
    private List<Integer> getLivestockWarningTimes(int warningTimeMinutes) {
        List<Integer> times = new ArrayList<>();
        int totalSeconds = warningTimeMinutes * 60;
        
        // 添加标准警告时间点
        if (totalSeconds >= 300) times.add(300); // 5分钟
        if (totalSeconds >= 180) times.add(180); // 3分钟
        if (totalSeconds >= 120) times.add(120); // 2分钟
        if (totalSeconds >= 60) times.add(60);   // 1分钟
        if (totalSeconds >= 30) times.add(30);   // 30秒
        if (totalSeconds >= 10) times.add(10);   // 10秒
        if (totalSeconds >= 5) times.add(5);     // 5秒
        
        return times;
    }
    
    /**
     * 发送倒计时消息
     */
    private void sendLivestockCountdownMessage(int timeLeft, int violationCount) {
        String timeStr;
        if (timeLeft >= 60) {
            timeStr = (timeLeft / 60) + "分钟";
        } else {
            timeStr = timeLeft + "秒";
        }
        
        String countdownMsg = config.getLivestockWarningMessage("countdown")
            .replace("{time}", timeStr)
            .replace("{count}", String.valueOf(violationCount));
        
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', countdownMsg));
    }
    
    /**
     * 执行智能清理
     */
    public void performSmartCleanup(SparkEntityMetrics metrics) {
        if (!stateManager.tryStartCleanup(CleanupStateManager.CleanupType.LIVESTOCK, "SMART_CLEANUP")) {
            return;
        }
        
        plugin.getLogger().info(String.format(
            "[LivestockCleanupManager] 触发智能清理 - TPS: %.2f, MSPT: %.2f, 实体数: %d",
            metrics.getTps(), metrics.getMspt(), metrics.getTotalEntities()
        ));
        
        performLivestockCleanup();
    }
    
    /**
     * 执行畜牧业清理
     */
    public void performLivestockCleanup() {
        if (!config.isAutoCleanupEnabled()) {
            plugin.getLogger().info("[LivestockCleanupManager] 自动清理已禁用，跳过清理");
            return;
        }
        
        // 使用新的服务架构执行清理
        serviceManager.executeCleanup(CleanupType.LIVESTOCK, null, config.isAsyncCleanup())
            .thenAccept(stats -> {
                // 在主线程中处理通知
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // 发送清理完成消息
                        String completionMessage = String.format(
                            "%s[畜牧业管理] 清理完成！移除了 %d 只动物",
                            ChatColor.GREEN, stats.getMobsCleaned()
                        );
                        Bukkit.broadcastMessage(completionMessage);
                        
                        plugin.getLogger().info(String.format(
                            "[LivestockCleanupManager] 畜牧业清理完成 - 移除动物: %d",
                            stats.getMobsCleaned()
                        ));
                    }
                }.runTask(plugin);
            })
            .exceptionally(throwable -> {
                plugin.getLogger().severe("[LivestockCleanupManager] 执行畜牧业清理时发生异常: " + throwable.getMessage());
                throwable.printStackTrace();
                return null;
            });
    }
    
    /**
     * 清理指定位置
     */
    private int cleanupLocation(LivestockViolationInfo violation) {
        String worldName = violation.getHotspotInfo().getWorldName();
        int chunkX = violation.getHotspotInfo().getChunkX();
        int chunkZ = violation.getHotspotInfo().getChunkZ();
        
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("[LivestockCleanupManager] 世界不存在: " + worldName);
            return 0;
        }
        
        Chunk chunk = world.getChunkAt(chunkX, chunkZ);
        if (!chunk.isLoaded()) {
            chunk.load();
        }
        
        List<Animals> animals = new ArrayList<>();
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Animals) {
                animals.add((Animals) entity);
            }
        }
        
        if (animals.isEmpty()) {
            return 0;
        }
        
        // 计算需要移除的数量
        int currentCount = animals.size();
        int maxAllowed = config.getMaxAnimalsPerChunk();
        int toRemove = Math.max(0, currentCount - maxAllowed);
        
        if (toRemove <= 0) {
            return 0;
        }
        
        // 智能选择移除策略
        List<Animals> toRemoveList = selectAnimalsForRemoval(animals, toRemove);
        
        // 执行移除
        int actuallyRemoved = 0;
        for (Animals animal : toRemoveList) {
            if (animal.isValid() && !animal.isDead()) {
                animal.remove();
                actuallyRemoved++;
            }
        }
        
        return actuallyRemoved;
    }
    
    /**
     * 智能选择要移除的动物
     */
    private List<Animals> selectAnimalsForRemoval(List<Animals> animals, int toRemove) {
        List<Animals> candidates = new ArrayList<>(animals);
        
        // 优先移除：未命名的、非繁殖状态的、年龄较大的动物
        candidates.sort((a, b) -> {
            // 优先级1: 是否有自定义名称（有名称的优先保留）
            boolean aHasName = a.getCustomName() != null;
            boolean bHasName = b.getCustomName() != null;
            if (aHasName != bHasName) {
                return Boolean.compare(aHasName, bHasName);
            }
            
            // 优先级2: 是否为幼体（幼体优先保留）
            boolean aIsBaby = !a.isAdult();
            boolean bIsBaby = !b.isAdult();
            if (aIsBaby != bIsBaby) {
                return Boolean.compare(aIsBaby, bIsBaby);
            }
            
            // 优先级3: 年龄（年龄大的优先移除）
            return Integer.compare(b.getTicksLived(), a.getTicksLived());
        });
        
        return candidates.subList(0, Math.min(toRemove, candidates.size()));
    }
    
    /**
     * 清理待处理的清理任务
     */
    public void clearPendingCleanups() {
        synchronized (pendingCleanups) {
            pendingCleanups.clear();
        }
        
        if (warningTask != null) {
            warningTask.cancel();
            warningTask = null;
        }
    }
    
    /**
     * 获取状态信息
     */
    public String getStatusInfo() {
        StringBuilder status = new StringBuilder();
        status.append("=== 清理管理器状态 ===\n");
        
        synchronized (pendingCleanups) {
            status.append("待清理位置: ").append(pendingCleanups.size()).append("\n");
        }
        
        status.append("清理任务: ").append(serviceManager.isCleanupRunning(CleanupType.LIVESTOCK) ? "运行中" : "空闲").append("\n");
        status.append("倒计时任务: ").append(warningTask != null ? "运行中" : "无").append("\n");
        
        return status.toString();
    }
    
    // Getter方法
    public boolean isCleanupRunning() {
        return serviceManager.isCleanupRunning(CleanupType.LIVESTOCK);
    }
    
    public int getPendingCleanupCount() {
        synchronized (pendingCleanups) {
            return pendingCleanups.size();
        }
    }
    

}