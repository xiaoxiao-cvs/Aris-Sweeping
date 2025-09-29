package com.arisweeping.gui.config;

import com.arisweeping.data.ConfigData;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 物品清理标签渲染器
 * 简化版本，使用正确的 ConfigData 属性
 */
public class ItemCleaningTabRenderer extends ConfigTabRenderer {
    
    public ItemCleaningTabRenderer(ModernConfigScreen parentScreen, ConfigData configData) {
        super(parentScreen, configData);
    }
    
    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY, float partialTicks) {
        graphics.drawString(getFont(), "§l物品清理设置", x, y, 0xFFFFFFFF, false);
        graphics.drawString(getFont(), "配置物品自动清理规则", x, y + 20, 0xFFCCCCCC, false);
        
        // 基本设置
        renderToggleOption(graphics, "启用物品清理", 
            configData.itemCleaning.enabled, 
            x, y + 50, width, mouseX, mouseY);
            
        renderSliderOption(graphics, "物品生存时间 (秒)", 
            configData.itemCleaning.getItemLifetimeSeconds(), 60, 1200, 
            x, y + 80, width, mouseX, mouseY);
            
        renderSliderOption(graphics, "检查范围 (区块)", 
            configData.itemCleaning.getChunkRange(), 1, 16, 
            x, y + 110, width, mouseX, mouseY);
            
        renderSliderOption(graphics, "最小物品数", 
            configData.itemCleaning.getMinItemCount(), 1, 100, 
            x, y + 140, width, mouseX, mouseY);
    }
    
    @Override
    public int getContentHeight() {
        return 200;
    }
    
    private void renderToggleOption(GuiGraphics graphics, String label, boolean value, 
                                   int x, int y, int width, int mouseX, int mouseY) {
        graphics.drawString(getFont(), label + ": " + (value ? "开启" : "关闭"), x, y, 0xFFFFFFFF, false);
    }
    
    private void renderSliderOption(GuiGraphics graphics, String label, int value, int min, int max,
                                   int x, int y, int width, int mouseX, int mouseY) {
        graphics.drawString(getFont(), label + ": " + value, x, y, 0xFFFFFFFF, false);
    }
}