package com.arisweeping.gui.config;

import com.arisweeping.data.ConfigData;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 配置标签渲染器基类
 */
public abstract class ConfigTabRenderer {
    
    protected final ModernConfigScreen parentScreen;
    protected final ConfigData configData;
    
    public ConfigTabRenderer(ModernConfigScreen parentScreen, ConfigData configData) {
        this.parentScreen = parentScreen;
        this.configData = configData;
    }
    
    /**
     * 渲染标签内容
     */
    public abstract void render(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY, float partialTicks);
    
    /**
     * 渲染工具提示
     */
    public void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        // 默认空实现，子类可以重写
    }
    
    /**
     * 获取内容高度（用于滚动条计算）
     */
    public abstract int getContentHeight();
    
    /**
     * 重置为默认值
     */
    public void resetToDefaults() {
        // 默认空实现，子类可以重写
    }
    
    /**
     * 处理鼠标点击
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }
    
    /**
     * 获取字体
     */
    protected net.minecraft.client.gui.Font getFont() {
        return parentScreen.getMinecraft().font;
    }
}