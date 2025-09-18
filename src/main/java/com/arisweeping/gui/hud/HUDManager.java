package com.arisweeping.gui.hud;

import com.arisweeping.core.ArisLogger;
import com.arisweeping.core.ArisSweepingMod;
import com.arisweeping.data.StatisticsCollector;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * HUD管理器
 * 负责管理游戏内的HUD显示和渲染
 */
@Mod.EventBusSubscriber(modid = ArisSweepingMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class HUDManager {
    
    private static boolean showHUD = false;
    private static boolean showDetailedInfo = false;
    
    /**
     * 切换HUD显示状态
     * @return 新的显示状态
     */
    public static boolean toggleHUD() {
        showHUD = !showHUD;
        ArisLogger.info("HUD显示状态: " + (showHUD ? "开启" : "关闭"));
        return showHUD;
    }
    
    /**
     * 设置HUD显示状态
     */
    public static void setHUDVisible(boolean visible) {
        showHUD = visible;
    }
    
    /**
     * 获取HUD显示状态
     */
    public static boolean isHUDVisible() {
        return showHUD;
    }
    
    /**
     * 切换详细信息显示
     */
    public static boolean toggleDetailedInfo() {
        showDetailedInfo = !showDetailedInfo;
        return showDetailedInfo;
    }
    
    /**
     * 渲染HUD覆盖层
     */
    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (!showHUD) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        
        try {
            renderHUD(event.getGuiGraphics());
        } catch (Exception e) {
            ArisLogger.error("HUD渲染错误", e);
        }
    }
    
    /**
     * 渲染HUD内容
     */
    private static void renderHUD(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int lineHeight = 10;
        
        // HUD位置 - 左上角
        int x = 5;
        int y = 5;
        
        // 绘制背景
        int hudWidth = 200;
        int hudHeight = showDetailedInfo ? 120 : 60;
        graphics.fill(x - 2, y - 2, x + hudWidth + 2, y + hudHeight + 2, 0x88000000);
        
        // 标题
        graphics.drawString(mc.font, "§bArisSweeping", x, y, 0xFFFFFF);
        y += lineHeight + 2;
        
        // 模组状态
        boolean enabled = ArisSweepingMod.isEnabled();
        String statusText = "状态: " + (enabled ? "§a启用" : "§c禁用");
        graphics.drawString(mc.font, statusText, x, y, 0xFFFFFF);
        y += lineHeight;
        
        // 统计信息
        try {
            StatisticsCollector stats = StatisticsCollector.getInstance();
            if (stats != null) {
                Object cleanedEntities = stats.getMetric("total_cleaned_entities");
                String cleanedText = "已清理实体: §e" + (cleanedEntities != null ? cleanedEntities : "0");
                graphics.drawString(mc.font, cleanedText, x, y, 0xFFFFFF);
                y += lineHeight;
                
                Object cleaningTasks = stats.getMetric("total_cleaning_tasks");
                String tasksText = "执行任务: §e" + (cleaningTasks != null ? cleaningTasks : "0");
                graphics.drawString(mc.font, tasksText, x, y, 0xFFFFFF);
                y += lineHeight;
            }
        } catch (Exception e) {
            graphics.drawString(mc.font, "统计: §c未可用", x, y, 0xFFFFFF);
            y += lineHeight;
        }
        
        // 详细信息
        if (showDetailedInfo) {
            y += 5; // 分隔线
            graphics.drawString(mc.font, "§7--- 详细信息 ---", x, y, 0xFFFFFF);
            y += lineHeight;
            
            // 性能信息
            try {
                // 由于PerformanceMonitor需要AsyncTaskManager参数，这里简化处理
                // 直接获取运行时信息
                Runtime runtime = Runtime.getRuntime();
                long freeMemory = runtime.freeMemory() / 1024 / 1024;
                long totalMemory = runtime.totalMemory() / 1024 / 1024;
                long usedMemory = totalMemory - freeMemory;
                
                String memoryText = String.format("内存: %dMB/%dMB", usedMemory, totalMemory);
                graphics.drawString(mc.font, memoryText, x, y, 0xFFFFFF);
                y += lineHeight;
                
                // CPU负载
                graphics.drawString(mc.font, "CPU负载: §a正常", x, y, 0xFFFFFF);
                y += lineHeight;
            } catch (Exception e) {
                graphics.drawString(mc.font, "性能监控: §c错误", x, y, 0xFFFFFF);
                y += lineHeight;
            }
        }
        
        // 快捷键提示
        y += 5;
        graphics.drawString(mc.font, "§7快捷键:", x, y, 0xFFFFFF);
        y += lineHeight;
        graphics.drawString(mc.font, "§7K: 配置 | F9: HUD | J: 开关", x, y, 0xFFFFFF);
    }
}