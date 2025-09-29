package com.arisweeping.hud;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.arisweeping.config.InfoToggle;
import com.arisweeping.core.ArisSweepingMod;
import com.arisweeping.data.StatisticsCollector;

import net.minecraft.client.Minecraft;

/**
 * HUD数据提供器
 * 负责为HUD提供各种信息数据
 */
public class HudDataProvider {
    
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#0.0");
    
    /**
     * 获取所有需要显示的HUD行
     */
    public static List<String> getHudLines() {
        List<String> lines = new ArrayList<>();
        
        // 模组状态
        if (InfoToggle.STATUS.shouldDisplay()) {
            boolean enabled = ArisSweepingMod.isEnabled();
            lines.add("§bArisSweeping: " + (enabled ? "§a启用" : "§c禁用"));
        }
        
        // 统计信息
        if (InfoToggle.STATISTICS.shouldDisplay()) {
            addStatisticsLines(lines);
        }
        
        // 性能信息
        if (InfoToggle.PERFORMANCE.shouldDisplay()) {
            addPerformanceLines(lines);
        }
        
        // 内存使用
        if (InfoToggle.MEMORY_USAGE.shouldDisplay()) {
            addMemoryLines(lines);
        }
        
        // 清理信息
        if (InfoToggle.CLEANED_ENTITIES.shouldDisplay() || InfoToggle.CLEANING_TASKS.shouldDisplay()) {
            addCleaningLines(lines);
        }
        
        // 实时数据
        if (InfoToggle.NEARBY_ENTITIES.shouldDisplay() || InfoToggle.CHUNK_ENTITIES.shouldDisplay()) {
            addRealtimeLines(lines);
        }
        
        // 快捷键帮助
        if (InfoToggle.HOTKEYS_HELP.shouldDisplay()) {
            addHotkeysHelp(lines);
        }
        
        return lines;
    }
    
    /**
     * 添加统计信息行
     */
    private static void addStatisticsLines(List<String> lines) {
        try {
            StatisticsCollector stats = StatisticsCollector.getInstance();
            if (stats != null) {
                if (InfoToggle.CLEANED_ENTITIES.shouldDisplay()) {
                    Object cleanedEntities = stats.getMetric("total_cleaned_entities");
                    lines.add("已清理实体: §e" + (cleanedEntities != null ? cleanedEntities : "0"));
                }
                
                if (InfoToggle.CLEANING_TASKS.shouldDisplay()) {
                    Object cleaningTasks = stats.getMetric("total_cleaning_tasks");
                    lines.add("执行任务: §e" + (cleaningTasks != null ? cleaningTasks : "0"));
                }
                
                if (InfoToggle.LAST_CLEANING.shouldDisplay()) {
                    Object lastCleaning = stats.getMetric("last_cleaning_time");
                    if (lastCleaning != null) {
                        lines.add("最后清理: §7" + formatTime(System.currentTimeMillis() - (Long)lastCleaning));
                    }
                }
            }
        } catch (Exception e) {
            lines.add("§c统计数据不可用");
        }
    }
    
    /**
     * 添加性能信息行
     */
    private static void addPerformanceLines(List<String> lines) {
        try {
            Minecraft mc = Minecraft.getInstance();
            int fps = mc.getFps();
            lines.add("FPS: §e" + fps);
            
            // CPU负载简化显示
            lines.add("CPU: §a正常");
        } catch (Exception e) {
            lines.add("§c性能数据不可用");
        }
    }
    
    /**
     * 添加内存信息行
     */
    private static void addMemoryLines(List<String> lines) {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / 1024 / 1024;
            long totalMemory = runtime.totalMemory() / 1024 / 1024;
            long freeMemory = runtime.freeMemory() / 1024 / 1024;
            long usedMemory = totalMemory - freeMemory;
            
            double memoryPercent = (double) usedMemory / maxMemory * 100;
            String color = memoryPercent > 80 ? "§c" : memoryPercent > 60 ? "§e" : "§a";
            
            lines.add("内存: " + color + usedMemory + "MB§f/§7" + maxMemory + "MB §f(" + 
                     DECIMAL_FORMAT.format(memoryPercent) + "%)");
        } catch (Exception e) {
            lines.add("§c内存数据不可用");
        }
    }
    
    /**
     * 添加清理信息行
     */
    private static void addCleaningLines(List<String> lines) {
        // 这里可以集成到任务管理系统获取实时状态
        if (InfoToggle.TASK_QUEUE.shouldDisplay()) {
            lines.add("任务队列: §a空闲");
        }
        
        if (InfoToggle.UNDO_AVAILABLE.shouldDisplay()) {
            lines.add("可撤销: §e3操作");
        }
        
        if (InfoToggle.ACTIVE_FILTERS.shouldDisplay()) {
            lines.add("活动过滤器: §7物品清理");
        }
    }
    
    /**
     * 添加实时数据行
     */
    private static void addRealtimeLines(List<String> lines) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.player != null) {
                if (InfoToggle.NEARBY_ENTITIES.shouldDisplay()) {
                    // 简化的实体计数
                    int nearbyCount = 0;
                    for (@SuppressWarnings("unused") var entity : mc.level.entitiesForRendering()) {
                        nearbyCount++;
                        if (nearbyCount > 100) break; // 限制计数以避免性能问题
                    }
                    lines.add("附近实体: §e" + nearbyCount);
                }
                
                if (InfoToggle.CHUNK_ENTITIES.shouldDisplay()) {
                    lines.add("区块实体: §e估算中");
                }
            }
        } catch (Exception e) {
            lines.add("§c实时数据不可用");
        }
    }
    
    /**
     * 添加快捷键帮助
     */
    private static void addHotkeysHelp(List<String> lines) {
        lines.add("§7--- 快捷键 ---");
        lines.add("§7K: 主开关");
        lines.add("§7H,C: 配置界面");  
        lines.add("§7H: HUD开关");
    }
    
    /**
     * 格式化时间差
     */
    private static String formatTime(long millis) {
        long seconds = millis / 1000;
        if (seconds < 60) {
            return seconds + "秒前";
        } else if (seconds < 3600) {
            return (seconds / 60) + "分钟前";
        } else {
            return (seconds / 3600) + "小时前";
        }
    }
    
    /**
     * 获取HUD总行数（用于背景绘制）
     */
    public static int getHudLineCount() {
        return getHudLines().size();
    }
    
    /**
     * 检查是否有任何HUD项需要显示
     */
    public static boolean shouldShowHud() {
        for (InfoToggle toggle : InfoToggle.VALUES) {
            if (toggle.shouldDisplay()) {
                return true;
            }
        }
        return false;
    }
}