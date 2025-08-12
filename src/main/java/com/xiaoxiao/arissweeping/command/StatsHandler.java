package com.xiaoxiao.arissweeping.command;

import com.xiaoxiao.arissweeping.ArisSweeping;
import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.permission.PermissionManager;
import com.xiaoxiao.arissweeping.util.EntityHotspotDetector;
import me.lucko.spark.api.Spark;
import me.lucko.spark.api.SparkProvider;
import me.lucko.spark.api.statistic.StatisticWindow;
import me.lucko.spark.api.statistic.types.DoubleStatistic;
import me.lucko.spark.api.statistic.types.GenericStatistic;
import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统计处理器 - 负责处理统计相关命令
 */
public class StatsHandler {
    private final ArisSweeping plugin;
    private final ModConfig config;
    private final PermissionManager permissionManager;
    private final Spark spark;
    private final EntityHotspotDetector hotspotDetector;

    public StatsHandler(ArisSweeping plugin) {
        this.plugin = plugin;
        this.config = plugin.getModConfig();
        this.permissionManager = plugin.getPermissionManager();
        try {
            this.spark = SparkProvider.get();
            this.hotspotDetector = new EntityHotspotDetector(plugin);
            plugin.getLogger().info("[StatsHandler] 成功初始化Spark API和实体热点检测器");
        } catch (Exception e) {
            plugin.getLogger().severe("[StatsHandler] 初始化失败: " + e.getMessage());
            throw new RuntimeException("StatsHandler初始化失败", e);
        }
    }

    /**
     * 处理统计命令
     */
    public void handleStatsCommand(CommandSender sender) {
        // 检查权限
        if (!hasPermission(sender, PermissionManager.STATS)) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 老师~没有权限查看统计哦~");
            return;
        }

        // 异步执行统计，避免阻塞主线程
        new BukkitRunnable() {
            @Override
            public void run() {
                final StringBuilder stats = new StringBuilder();
                
                try {
                    // 服务器基本信息
                    stats.append(ChatColor.GOLD).append("═══════════════════════════════════════\n");
                    stats.append(ChatColor.GOLD).append("        ").append(ChatColor.YELLOW).append("📊 服务器统计信息 📊").append(ChatColor.GOLD).append("\n");
                    stats.append(ChatColor.GOLD).append("═══════════════════════════════════════\n");
                    stats.append(ChatColor.WHITE).append("\n");
                    
                    // 在线玩家
                    int onlinePlayers = Bukkit.getOnlinePlayers().size();
                    int maxPlayers = Bukkit.getMaxPlayers();
                    stats.append(ChatColor.AQUA).append("👥 玩家信息:\n");
                    stats.append(ChatColor.WHITE).append("  在线玩家: ").append(ChatColor.YELLOW).append(onlinePlayers).append(ChatColor.WHITE).append("/").append(ChatColor.YELLOW).append(maxPlayers).append("\n");
                    
                    // 世界信息
                    int worldCount = Bukkit.getWorlds().size();
                    stats.append(ChatColor.WHITE).append("  加载世界: ").append(ChatColor.YELLOW).append(worldCount).append("\n");
                    stats.append(ChatColor.WHITE).append("\n");
                    
                    // 实体统计
                    stats.append(ChatColor.LIGHT_PURPLE).append("🌍 世界详情:\n");
                    int totalEntities = 0;
                    int totalChunks = 0;
                    
                    for (World world : Bukkit.getWorlds()) {
                        if (world == null) continue;
                        
                        int worldEntities = world.getEntities().size();
                        int worldChunks = world.getLoadedChunks().length;
                        
                        totalEntities += worldEntities;
                        totalChunks += worldChunks;
                        
                        stats.append(ChatColor.WHITE).append("  ").append(ChatColor.GREEN).append(world.getName()).append(ChatColor.WHITE).append(": ")
                             .append(ChatColor.YELLOW).append(worldEntities).append(ChatColor.WHITE).append(" 实体, ")
                             .append(ChatColor.YELLOW).append(worldChunks).append(ChatColor.WHITE).append(" 区块\n");
                    }
                    
                    stats.append(ChatColor.WHITE).append("\n");
                    stats.append(ChatColor.AQUA).append("📈 总计:\n");
                    stats.append(ChatColor.WHITE).append("  总实体数: ").append(ChatColor.YELLOW).append(totalEntities).append("\n");
                    stats.append(ChatColor.WHITE).append("  总区块数: ").append(ChatColor.YELLOW).append(totalChunks).append("\n");
                    stats.append(ChatColor.WHITE).append("\n");
                    
                    // 内存信息（使用Spark API增强）
                    appendMemoryStats(stats);
                    
                    // 性能信息（使用Spark API）
                    appendPerformanceStats(stats);
                    
                    // 清理配置状态
                    stats.append(ChatColor.DARK_AQUA).append("🔧 清理配置:\n");
                    stats.append(ChatColor.WHITE).append("  插件状态: ").append(config.isPluginEnabled() ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用").append("\n");
                    stats.append(ChatColor.WHITE).append("  清理间隔: ").append(ChatColor.YELLOW).append(config.getCleanupInterval()).append(ChatColor.WHITE).append(" 秒\n");
                    stats.append(ChatColor.WHITE).append("  异步清理: ").append(config.isAsyncCleanup() ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用").append("\n");
                    stats.append(ChatColor.WHITE).append("\n");
                    
                    stats.append(ChatColor.GRAY).append("💡 提示: 使用 /arissweeping tps 查看详细TPS信息\n");
                    stats.append(ChatColor.GRAY).append("💡 提示: 使用 /arissweeping livestock-stats 查看畜牧业统计\n");
                    stats.append(ChatColor.GOLD).append("═══════════════════════════════════════");
                    
                } catch (Exception e) {
                    plugin.getLogger().severe("[StatsHandler] 获取统计信息时发生异常: " + e.getMessage());
                    stats.setLength(0);
                    stats.append(ChatColor.RED).append("[邦邦卡邦！] 获取统计信息时发生错误，请查看控制台日志");
                }
                
                // 回到主线程发送消息
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        sender.sendMessage(stats.toString());
                    }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * 处理TPS命令（使用Spark API增强）
     */
    public void handleTpsCommand(CommandSender sender) {
        // 检查权限
        if (!hasPermission(sender, PermissionManager.STATS)) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 老师~没有权限查看TPS哦~");
            return;
        }

        try {
            sender.sendMessage(ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "详细性能状态:");
            
            // TPS信息（使用Spark API）
            DoubleStatistic<StatisticWindow.TicksPerSecond> tpsStatistic = spark.tps();
            if (tpsStatistic != null) {
                double tps10s = tpsStatistic.poll(StatisticWindow.TicksPerSecond.SECONDS_10);
                double tps1m = tpsStatistic.poll(StatisticWindow.TicksPerSecond.MINUTES_1);
                double tps5m = tpsStatistic.poll(StatisticWindow.TicksPerSecond.MINUTES_5);
                double tps15m = tpsStatistic.poll(StatisticWindow.TicksPerSecond.MINUTES_15);
                
                ChatColor tpsColor = tps1m >= 18.0 ? ChatColor.GREEN : 
                                   tps1m >= 15.0 ? ChatColor.YELLOW : ChatColor.RED;
                
                sender.sendMessage(ChatColor.YELLOW + "📊 TPS (每秒刻数):");
                sender.sendMessage(ChatColor.WHITE + "  10秒: " + tpsColor + String.format("%.2f", tps10s));
                sender.sendMessage(ChatColor.WHITE + "  1分钟: " + tpsColor + String.format("%.2f", tps1m));
                sender.sendMessage(ChatColor.WHITE + "  5分钟: " + tpsColor + String.format("%.2f", tps5m));
                sender.sendMessage(ChatColor.WHITE + "  15分钟: " + tpsColor + String.format("%.2f", tps15m));
                
                // 状态评估
                String status = tps1m >= 19.5 ? "优秀" : tps1m >= 18.0 ? "良好" : tps1m >= 15.0 ? "一般" : "较差";
                sender.sendMessage(ChatColor.WHITE + "  状态: " + tpsColor + status);
            }
            
            // MSPT信息
            GenericStatistic<DoubleAverageInfo, StatisticWindow.MillisPerTick> msptStatistic = spark.mspt();
            if (msptStatistic != null) {
                DoubleAverageInfo mspt10s = msptStatistic.poll(StatisticWindow.MillisPerTick.SECONDS_10);
                DoubleAverageInfo mspt1m = msptStatistic.poll(StatisticWindow.MillisPerTick.MINUTES_1);
                DoubleAverageInfo mspt5m = msptStatistic.poll(StatisticWindow.MillisPerTick.MINUTES_5);
                
                if (mspt1m != null) {
                    ChatColor msptColor = mspt1m.mean() <= 50 ? ChatColor.GREEN :
                                         mspt1m.mean() <= 80 ? ChatColor.YELLOW : ChatColor.RED;
                    
                    sender.sendMessage(ChatColor.YELLOW + "⏱️ MSPT (毫秒每刻):");
                    if (mspt10s != null) {
                        sender.sendMessage(ChatColor.WHITE + "  10秒: " + msptColor + String.format("%.2fms", mspt10s.mean()));
                    }
                    sender.sendMessage(ChatColor.WHITE + "  1分钟: " + msptColor + String.format("%.2fms", mspt1m.mean()));
                    if (mspt5m != null) {
                        sender.sendMessage(ChatColor.WHITE + "  5分钟: " + msptColor + String.format("%.2fms", mspt5m.mean()));
                    }
                }
            }
            
            // CPU使用率
            DoubleStatistic<StatisticWindow.CpuUsage> cpuStatistic = spark.cpuProcess();
            if (cpuStatistic != null) {
                double cpu1m = cpuStatistic.poll(StatisticWindow.CpuUsage.MINUTES_1);
                double cpu15m = cpuStatistic.poll(StatisticWindow.CpuUsage.MINUTES_15);
                
                ChatColor cpuColor = cpu1m <= 0.5 ? ChatColor.GREEN :
                                   cpu1m <= 0.8 ? ChatColor.YELLOW : ChatColor.RED;
                
                sender.sendMessage(ChatColor.YELLOW + "🖥️ CPU使用率:");
                sender.sendMessage(ChatColor.WHITE + "  1分钟: " + cpuColor + String.format("%.1f%%", cpu1m * 100));
                sender.sendMessage(ChatColor.WHITE + "  15分钟: " + cpuColor + String.format("%.1f%%", cpu15m * 100));
            }
            
            // 性能建议
            DoubleStatistic<StatisticWindow.TicksPerSecond> tpsCheck = spark.tps();
            if (tpsCheck != null) {
                double currentTps = tpsCheck.poll(StatisticWindow.TicksPerSecond.MINUTES_1);
                if (currentTps < 15.0) {
                    sender.sendMessage(ChatColor.RED + "服务器TPS较低，建议检查服务器负载！");
                    sender.sendMessage(ChatColor.YELLOW + "提示: 使用 /arissweeping cleanup 进行实体清理");
                } else if (currentTps >= 19.5) {
                    sender.sendMessage(ChatColor.GREEN + "服务器性能良好！");
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("[StatsHandler] 获取TPS信息时发生异常: " + e.getMessage());
            // 回退到基本TPS显示
            if (config.isTpsMonitorEnabled() && plugin.getCleanupHandler().getTpsMonitor() != null) {
                double currentTps = plugin.getCleanupHandler().getTpsMonitor().getCurrentTps();
                String tpsStatus = plugin.getCleanupHandler().getTpsMonitor().getTpsStatus();
                
                ChatColor tpsColor = currentTps >= 18.0 ? ChatColor.GREEN : 
                                   currentTps >= 15.0 ? ChatColor.YELLOW : ChatColor.RED;
                
                sender.sendMessage(ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "TPS 状态:");
                sender.sendMessage(ChatColor.WHITE + "  当前TPS: " + tpsColor + String.format("%.2f", currentTps));
                sender.sendMessage(ChatColor.WHITE + "  状态: " + tpsColor + tpsStatus);
            } else {
                sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 无法获取性能数据，请检查Spark插件状态！");
            }
        }
    }

    /**
     * 处理畜牧业统计命令（使用EntityHotspotDetector增强）
     */
    /**
     * 处理畜牧业统计命令 - 使用Spark API增强版
     */
    public void handleLivestockStatsCommand(CommandSender sender) {
        // 检查权限
        if (!hasPermission(sender, PermissionManager.STATS)) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 老师~没有权限查看畜牧业统计哦~");
            return;
        }
        
        sender.sendMessage(ChatColor.YELLOW + "[邦邦卡邦！] 正在使用Spark API扫描畜牧业热点，请稍候...");
        
        // 使用新的Spark API增强版畜牧业热点检测
        hotspotDetector.scanLivestockHotspotsAsync(new EntityHotspotDetector.LivestockScanCallback() {
            @Override
            public void onComplete(java.util.List<EntityHotspotDetector.LivestockHotspotInfo> hotspots, EntityHotspotDetector.LivestockStatistics statistics) {
                processEnhancedLivestockStats(sender, hotspots, statistics);
            }
            
            @Override
            public void onError(Exception error) {
                plugin.getLogger().severe("[StatsHandler] 获取Spark增强畜牧业统计时发生异常: " + error.getMessage());
                sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 获取畜牧业统计时发生错误，请查看控制台日志");
            }
        });
    }
    
    /**
     * 处理Spark API增强版畜牧业统计结果
     */
    private void processEnhancedLivestockStats(CommandSender sender, java.util.List<EntityHotspotDetector.LivestockHotspotInfo> hotspots, EntityHotspotDetector.LivestockStatistics statistics) {
        try {
            // 使用Spark API增强版统计数据
            Map<String, Integer> worldAnimalStats = new HashMap<>();
            Map<String, Map<String, Integer>> worldTypeStats = new HashMap<>();
            Map<String, Integer> worldChunkStats = new HashMap<>();
            Map<String, Integer> worldViolationStats = new HashMap<>();
            
            // 从LivestockStatistics获取总体数据
            int totalAnimals = statistics.getTotalAnimals();
            int totalViolations = statistics.getViolatingChunks().size(); // 修复类型不匹配
            int totalHotspots = hotspots.size();
            double avgDensity = statistics.getAverageDensity();
            double avgPerformanceImpact = statistics.getAveragePerformanceImpact();
            
            // 统计所有已加载区块
            int totalChunks = 0;
            for (World world : Bukkit.getWorlds()) {
                if (world != null) {
                    int chunkCount = world.getLoadedChunks().length;
                    worldChunkStats.put(world.getName(), chunkCount);
                    totalChunks += chunkCount;
                }
            }
            
            // 处理LivestockHotspotInfo数据
            for (EntityHotspotDetector.LivestockHotspotInfo hotspot : hotspots) {
                String worldName = hotspot.getWorldName();
                int animalCount = hotspot.getTotalAnimals();
                
                // 更新世界统计
                worldAnimalStats.put(worldName, worldAnimalStats.getOrDefault(worldName, 0) + animalCount);
                
                // 更新类型统计
                Map<String, Integer> worldTypes = worldTypeStats.computeIfAbsent(worldName, k -> new HashMap<>());
                for (Map.Entry<org.bukkit.entity.EntityType, Integer> entry : hotspot.getAnimalCounts().entrySet()) {
                    String typeName = entry.getKey().name();
                    int count = entry.getValue();
                    worldTypes.put(typeName, worldTypes.getOrDefault(typeName, 0) + count);
                }
                
                // 检查是否违规
                if (animalCount > config.getMaxAnimalsPerChunk()) {
                    worldViolationStats.put(worldName, worldViolationStats.getOrDefault(worldName, 0) + 1);
                }
            }
            
            // 构建Spark消息
            StringBuilder message = new StringBuilder();
            message.append(ChatColor.GOLD).append("═══════════════════════════════════════\n");
            message.append(ChatColor.GOLD).append("     ").append(ChatColor.YELLOW).append("Spark畜牧业统计").append(ChatColor.GOLD).append("\n");
            message.append(ChatColor.GOLD).append("═══════════════════════════════════════\n");
            message.append(ChatColor.WHITE).append("\n");
            
            // Spark增强版总体统计
            message.append(ChatColor.AQUA).append("Spark性能分析统计:\n");
            message.append(ChatColor.WHITE).append("  总动物数量: ").append(ChatColor.YELLOW).append(totalAnimals).append("\n");
            message.append(ChatColor.WHITE).append("  已加载区块: ").append(ChatColor.YELLOW).append(totalChunks).append("\n");
            message.append(ChatColor.WHITE).append("  畜牧业热点: ").append(ChatColor.AQUA).append(totalHotspots).append("\n");
            message.append(ChatColor.WHITE).append("  超标区块: ").append(totalViolations > 0 ? ChatColor.RED : ChatColor.GREEN).append(totalViolations).append("\n");
            message.append(ChatColor.WHITE).append("  密度阈值: ").append(ChatColor.YELLOW).append(config.getMaxAnimalsPerChunk()).append(" 只/区块\n");
            
            // Spark性能指标
            ChatColor densityColor = avgDensity <= 5 ? ChatColor.GREEN : 
                                   avgDensity <= 15 ? ChatColor.YELLOW : ChatColor.RED;
            message.append(ChatColor.WHITE).append("  平均密度: ").append(densityColor).append(String.format("%.2f", avgDensity)).append(" 只/区块\n");
            
            ChatColor performanceColor = avgPerformanceImpact <= 0.1 ? ChatColor.GREEN :
                                       avgPerformanceImpact <= 0.3 ? ChatColor.YELLOW : ChatColor.RED;
            message.append(ChatColor.WHITE).append("  性能影响: ").append(performanceColor).append(String.format("%.3f", avgPerformanceImpact)).append(" (Spark评分)\n");
            message.append(ChatColor.WHITE).append("\n");
            
            // 各世界统计
            message.append(ChatColor.LIGHT_PURPLE).append("各世界详情:\n");
            for (Map.Entry<String, Integer> entry : worldAnimalStats.entrySet()) {
                String worldName = entry.getKey();
                int animalCount = entry.getValue();
                int chunkCount = worldChunkStats.getOrDefault(worldName, 0);
                int violationCount = worldViolationStats.getOrDefault(worldName, 0);
                Map<String, Integer> types = worldTypeStats.get(worldName);
                
                double worldDensity = chunkCount > 0 ? (double) animalCount / chunkCount : 0;
                ChatColor worldColor = violationCount > 0 ? ChatColor.RED : 
                                     worldDensity > 10 ? ChatColor.YELLOW : ChatColor.GREEN;
                
                message.append(ChatColor.WHITE).append("  ").append(ChatColor.GREEN).append(worldName).append(ChatColor.WHITE).append(": ")
                       .append(ChatColor.YELLOW).append(animalCount).append(" 只")
                       .append(ChatColor.GRAY).append(" (密度: ").append(worldColor).append(String.format("%.1f", worldDensity)).append(ChatColor.GRAY).append(")\n");
                
                if (violationCount > 0) {
                    message.append(ChatColor.WHITE).append("    ").append(ChatColor.RED).append("超标区块: ").append(violationCount).append("\n");
                }
                
                if (config.isDebugMode() && types != null && !types.isEmpty()) {
                    message.append(ChatColor.GRAY).append("    类型分布: ");
                    String typeInfo = types.entrySet().stream()
                        .map(typeEntry -> typeEntry.getKey() + "(" + typeEntry.getValue() + ")")
                        .collect(Collectors.joining(", "));
                    message.append(typeInfo).append("\n");
                }
            }
            
            // Spark增强版热点区块信息
            if (totalViolations > 0) {
                message.append(ChatColor.WHITE).append("\n");
                message.append(ChatColor.RED).append("Spark检测到的高密度区块:\n");
                
                java.util.List<EntityHotspotDetector.LivestockHotspotInfo> topViolations = hotspots.stream()
                    .filter(h -> h.getTotalAnimals() > config.getMaxAnimalsPerChunk())
                    .sorted((a, b) -> Double.compare(b.getPerformanceImpact(), a.getPerformanceImpact()))
                    .limit(5)
                    .collect(Collectors.toList());
                
                for (EntityHotspotDetector.LivestockHotspotInfo hotspot : topViolations) {
                    String coordinates = String.format("(%d, %d)", hotspot.getChunkX() * 16, hotspot.getChunkZ() * 16);
                    
                    ChatColor impactColor = hotspot.getPerformanceImpact() <= 0.1 ? ChatColor.YELLOW :
                                          hotspot.getPerformanceImpact() <= 0.3 ? ChatColor.RED : ChatColor.DARK_RED;
                    
                    message.append(ChatColor.WHITE).append("  ").append(ChatColor.GOLD).append(coordinates)
                           .append(ChatColor.WHITE).append(" (").append(ChatColor.GREEN).append(hotspot.getWorldName()).append(ChatColor.WHITE).append(")")
                           .append(ChatColor.WHITE).append(": ").append(ChatColor.RED).append(hotspot.getTotalAnimals()).append(" 只")
                           .append(ChatColor.GRAY).append(" (性能影响: ").append(impactColor).append(String.format("%.3f", hotspot.getPerformanceImpact())).append(ChatColor.GRAY).append(")\n");
                }
            }
            
            message.append(ChatColor.WHITE).append("\n");
            message.append(ChatColor.GRAY).append("提示: 请在YAML配置文件中调整畜牧业设置\n");
            message.append(ChatColor.GRAY).append("提示: 现在使用Spark API进行智能性能监控\n");
            message.append(ChatColor.GRAY).append("数据来源: Spark性能分析引擎\n");
            message.append(ChatColor.GOLD).append("═══════════════════════════════════════");
            
            sender.sendMessage(message.toString());
            
        } catch (Exception e) {
            plugin.getLogger().severe("[StatsHandler] 处理畜牧业统计结果时发生异常: " + e.getMessage());
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 处理统计数据时发生错误，请查看控制台日志");
        }
    }
    
    /**
     * 检查实体类型是否为动物
     */
    private boolean isAnimalType(org.bukkit.entity.EntityType type) {
        switch (type) {
            case COW:
            case PIG:
            case SHEEP:
            case CHICKEN:
            case HORSE:
            case DONKEY:
            case MULE:
            case LLAMA:
            case RABBIT:
            case WOLF:
            case CAT:
            case OCELOT:
            case PARROT:
            case TURTLE:
            case PANDA:
            case FOX:
            case BEE:
            case POLAR_BEAR:
            case MUSHROOM_COW:
            case GOAT:
            case AXOLOTL:
                return true;
            default:
                return false;
        }
    }

    /**
     * 添加性能统计信息（使用Spark API）
     */
    private void appendPerformanceStats(StringBuilder stats) {
        try {
            stats.append(ChatColor.GOLD).append("⚡ 性能信息:\n");
            
            // TPS信息
            DoubleStatistic<StatisticWindow.TicksPerSecond> tpsStatistic = spark.tps();
            if (tpsStatistic != null) {
                double tps1m = tpsStatistic.poll(StatisticWindow.TicksPerSecond.MINUTES_1);
                double tps5m = tpsStatistic.poll(StatisticWindow.TicksPerSecond.MINUTES_5);
                double tps15m = tpsStatistic.poll(StatisticWindow.TicksPerSecond.MINUTES_15);
                
                ChatColor tpsColor = tps1m >= 18.0 ? ChatColor.GREEN : 
                                   tps1m >= 15.0 ? ChatColor.YELLOW : ChatColor.RED;
                
                stats.append(ChatColor.WHITE).append("  TPS: ").append(tpsColor)
                     .append(String.format("1分钟: %.2f, 5分钟: %.2f, 15分钟: %.2f", tps1m, tps5m, tps15m))
                     .append("\n");
            }
            
            // MSPT信息
            GenericStatistic<DoubleAverageInfo, StatisticWindow.MillisPerTick> msptStatistic = spark.mspt();
            if (msptStatistic != null) {
                DoubleAverageInfo mspt1m = msptStatistic.poll(StatisticWindow.MillisPerTick.MINUTES_1);
                DoubleAverageInfo mspt5m = msptStatistic.poll(StatisticWindow.MillisPerTick.MINUTES_5);
                
                if (mspt1m != null && mspt5m != null) {
                    ChatColor msptColor = mspt1m.mean() <= 50 ? ChatColor.GREEN :
                                         mspt1m.mean() <= 80 ? ChatColor.YELLOW : ChatColor.RED;
                    
                    stats.append(ChatColor.WHITE).append("  MSPT: ").append(msptColor)
                         .append(String.format("1分钟: %.2fms, 5分钟: %.2fms", mspt1m.mean(), mspt5m.mean()))
                         .append("\n");
                }
            }
            
            // CPU使用率
            DoubleStatistic<StatisticWindow.CpuUsage> cpuStatistic = spark.cpuProcess();
            if (cpuStatistic != null) {
                double cpu1m = cpuStatistic.poll(StatisticWindow.CpuUsage.MINUTES_1);
                double cpu15m = cpuStatistic.poll(StatisticWindow.CpuUsage.MINUTES_15);
                
                ChatColor cpuColor = cpu1m <= 0.5 ? ChatColor.GREEN :
                                   cpu1m <= 0.8 ? ChatColor.YELLOW : ChatColor.RED;
                
                stats.append(ChatColor.WHITE).append("  CPU使用率: ").append(cpuColor)
                     .append(String.format("1分钟: %.1f%%, 15分钟: %.1f%%", cpu1m * 100, cpu15m * 100))
                     .append("\n");
            }
            
            stats.append(ChatColor.WHITE).append("\n");
            
        } catch (Exception e) {
            plugin.getLogger().warning("[StatsHandler] 获取性能统计时发生异常: " + e.getMessage());
            // 回退到基本TPS显示
            if (config.isTpsMonitorEnabled() && plugin.getCleanupHandler().getTpsMonitor() != null) {
                double currentTps = plugin.getCleanupHandler().getTpsMonitor().getCurrentTps();
                ChatColor tpsColor = currentTps >= 18.0 ? ChatColor.GREEN : 
                                   currentTps >= 15.0 ? ChatColor.YELLOW : ChatColor.RED;
                
                stats.append(ChatColor.GOLD).append("⚡ TPS 信息:\n");
                stats.append(ChatColor.WHITE).append("  当前TPS: ").append(tpsColor).append(String.format("%.2f", currentTps)).append("\n");
                stats.append(ChatColor.WHITE).append("\n");
            }
        }
    }
    
    /**
     * 添加内存统计信息
     */
    private void appendMemoryStats(StringBuilder stats) {
        try {
            // 基本内存信息
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / 1024 / 1024; // MB
            long totalMemory = runtime.totalMemory() / 1024 / 1024; // MB
            long freeMemory = runtime.freeMemory() / 1024 / 1024; // MB
            long usedMemory = totalMemory - freeMemory;
            
            stats.append(ChatColor.RED).append("💾 内存使用:\n");
            stats.append(ChatColor.WHITE).append("  已用内存: ").append(ChatColor.YELLOW).append(usedMemory).append(ChatColor.WHITE).append(" MB\n");
            stats.append(ChatColor.WHITE).append("  总分配内存: ").append(ChatColor.YELLOW).append(totalMemory).append(ChatColor.WHITE).append(" MB\n");
            stats.append(ChatColor.WHITE).append("  最大内存: ").append(ChatColor.YELLOW).append(maxMemory).append(ChatColor.WHITE).append(" MB\n");
            stats.append(ChatColor.WHITE).append("  内存使用率: ");
            
            double memoryUsage = (double) usedMemory / maxMemory * 100;
            ChatColor memoryColor = memoryUsage > 80 ? ChatColor.RED : memoryUsage > 60 ? ChatColor.YELLOW : ChatColor.GREEN;
            stats.append(memoryColor).append(String.format("%.1f%%", memoryUsage)).append("\n");
            stats.append(ChatColor.WHITE).append("\n");
            
        } catch (Exception e) {
            plugin.getLogger().warning("[StatsHandler] 获取内存统计时发生异常: " + e.getMessage());
        }
    }
    
    /**
     * 检查权限
     */
    private boolean hasPermission(CommandSender sender, String permission) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            return permissionManager.hasPermission(player.getName(), permission) || player.hasPermission(permission);
        }
        return true; // 控制台总是有权限
    }
}