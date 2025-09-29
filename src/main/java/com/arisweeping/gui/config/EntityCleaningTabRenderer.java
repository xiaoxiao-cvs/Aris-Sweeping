package com.arisweeping.gui.config;

import com.arisweeping.data.ConfigData;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 实体清理标签渲染器
 */
public class EntityCleaningTabRenderer extends ConfigTabRenderer {
    
    public EntityCleaningTabRenderer(ModernConfigScreen parentScreen, ConfigData configData) {
        super(parentScreen, configData);
    }
    
    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY, float partialTicks) {
        graphics.drawString(getFont(), "§l实体清理设置", x, y, 0xFFFFFFFF, false);
        graphics.drawString(getFont(), "配置动物、怪物的清理规则", x, y + 20, 0xFFCCCCCC, false);
    }
    
    @Override
    public int getContentHeight() {
        return 100;
    }
}

/**
 * 性能标签渲染器
 */
class PerformanceTabRenderer extends ConfigTabRenderer {
    
    public PerformanceTabRenderer(ModernConfigScreen parentScreen, ConfigData configData) {
        super(parentScreen, configData);
    }
    
    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY, float partialTicks) {
        graphics.drawString(getFont(), "§l性能优化", x, y, 0xFFFFFFFF, false);
        graphics.drawString(getFont(), "调整性能相关参数", x, y + 20, 0xFFCCCCCC, false);
    }
    
    @Override
    public int getContentHeight() {
        return 100;
    }
}

/**
 * 显示设置标签渲染器
 */
class DisplayTabRenderer extends ConfigTabRenderer {
    
    public DisplayTabRenderer(ModernConfigScreen parentScreen, ConfigData configData) {
        super(parentScreen, configData);
    }
    
    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY, float partialTicks) {
        graphics.drawString(getFont(), "§l显示设置", x, y, 0xFFFFFFFF, false);
        graphics.drawString(getFont(), "配置HUD和界面显示", x, y + 20, 0xFFCCCCCC, false);
    }
    
    @Override
    public int getContentHeight() {
        return 100;
    }
}

/**
 * 高级选项标签渲染器
 */
class AdvancedTabRenderer extends ConfigTabRenderer {
    
    public AdvancedTabRenderer(ModernConfigScreen parentScreen, ConfigData configData) {
        super(parentScreen, configData);
    }
    
    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY, float partialTicks) {
        graphics.drawString(getFont(), "§l高级选项", x, y, 0xFFFFFFFF, false);
        graphics.drawString(getFont(), "专家级配置选项", x, y + 20, 0xFFCCCCCC, false);
    }
    
    @Override
    public int getContentHeight() {
        return 100;
    }
}