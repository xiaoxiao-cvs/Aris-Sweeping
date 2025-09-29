package com.arisweeping.gui.hud;

import java.util.List;

import com.arisweeping.config.Configs;
import com.arisweeping.core.ArisLogger;
import com.arisweeping.core.ArisSweepingMod;
import com.arisweeping.hud.HudDataProvider;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 重构后的HUD管理器
 * 基于malilib设计模式，集成InfoToggle配置系统
 */
@Mod.EventBusSubscriber(modid = ArisSweepingMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class HUDManager {
    
    // HUD渲染常量
    private static final int LINE_HEIGHT = 12;
    private static final int MARGIN = 4;
    private static final int BACKGROUND_COLOR = 0x80000000;
    private static final int TEXT_COLOR = 0xFFFFFF;
    
    /**
     * 渲染HUD覆盖层事件处理
     */
    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (!shouldRenderHud()) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }
        
        try {
            renderModernHud(event.getGuiGraphics(), mc);
        } catch (Exception e) {
            ArisLogger.error("HUD渲染错误", e);
        }
    }
    
    /**
     * 检查是否应该渲染HUD
     */
    private static boolean shouldRenderHud() {
        try {
            // 检查主HUD配置开关
            return Configs.General.SHOW_HUD.getBooleanValue() && 
                   HudDataProvider.shouldShowHud();
        } catch (Exception e) {
            // 如果配置系统未初始化，使用默认行为
            return true;
        }
    }
    
    /**
     * 渲染现代化的HUD
     */
    private static void renderModernHud(GuiGraphics graphics, Minecraft mc) {
        List<String> hudLines = HudDataProvider.getHudLines();
        if (hudLines.isEmpty()) {
            return;
        }
        
        // 计算HUD尺寸和位置
        int maxWidth = calculateMaxWidth(hudLines, mc);
        int hudHeight = hudLines.size() * LINE_HEIGHT + MARGIN * 2;
        
        // HUD位置 - 左上角（稍后可配置）
        int x = MARGIN;
        int y = MARGIN;
        
        // 绘制背景
        if (shouldDrawBackground()) {
            graphics.fill(x - MARGIN, y - MARGIN, 
                         x + maxWidth + MARGIN, y + hudHeight, 
                         BACKGROUND_COLOR);
        }
        
        // 绘制文本行
        for (int i = 0; i < hudLines.size(); i++) {
            String line = hudLines.get(i);
            int textY = y + i * LINE_HEIGHT;
            
            boolean useShadow = shouldUseFontShadow();
            graphics.drawString(mc.font, line, x, textY, TEXT_COLOR, useShadow);
        }
    }
    
    /**
     * 计算最大文本宽度
     */
    private static int calculateMaxWidth(List<String> lines, Minecraft mc) {
        int maxWidth = 0;
        for (String line : lines) {
            int width = mc.font.width(line);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        return maxWidth;
    }
    
    /**
     * 检查是否绘制背景
     */
    private static boolean shouldDrawBackground() {
        try {
            return Configs.General.USE_TEXT_BACKGROUND.getBooleanValue();
        } catch (Exception e) {
            return true; // 默认绘制背景
        }
    }
    
    /**
     * 检查是否使用字体阴影
     */
    private static boolean shouldUseFontShadow() {
        try {
            return Configs.General.USE_FONT_SHADOW.getBooleanValue();
        } catch (Exception e) {
            return true; // 默认使用阴影
        }
    }
    
    // === 向后兼容的公共API ===
    
    /**
     * 切换HUD显示状态
     * @deprecated 使用配置系统代替
     */
    @Deprecated
    public static boolean toggleHUD() {
        try {
            boolean newValue = !Configs.General.SHOW_HUD.getBooleanValue();
            Configs.General.SHOW_HUD.setBooleanValue(newValue);
            ArisLogger.info("HUD显示状态: " + (newValue ? "开启" : "关闭"));
            return newValue;
        } catch (Exception e) {
            ArisLogger.error("无法切换HUD状态", e);
            return false;
        }
    }
    
    /**
     * 设置HUD显示状态
     * @deprecated 使用配置系统代替
     */
    @Deprecated
    public static void setHUDVisible(boolean visible) {
        try {
            Configs.General.SHOW_HUD.setBooleanValue(visible);
        } catch (Exception e) {
            ArisLogger.error("无法设置HUD状态", e);
        }
    }
    
    /**
     * 获取HUD显示状态
     * @deprecated 使用配置系统代替
     */
    @Deprecated
    public static boolean isHUDVisible() {
        try {
            return Configs.General.SHOW_HUD.getBooleanValue();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 切换详细信息显示
     * @deprecated 使用InfoToggle系统代替
     */
    @Deprecated
    public static boolean toggleDetailedInfo() {
        // 这个功能现在通过InfoToggle.PERFORMANCE等控制
        ArisLogger.info("详细信息切换已移至InfoToggle配置系统");
        return true;
    }
}