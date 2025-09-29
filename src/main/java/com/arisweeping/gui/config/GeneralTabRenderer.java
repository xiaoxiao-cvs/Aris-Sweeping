package com.arisweeping.gui.config;

import com.arisweeping.data.ConfigData;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 通用设置标签渲染器
 */
public class GeneralTabRenderer extends ConfigTabRenderer {
    
    private static final int ITEM_HEIGHT = 25;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int LABEL_COLOR = 0xFFCCCCCC;
    
    public GeneralTabRenderer(ModernConfigScreen parentScreen, ConfigData configData) {
        super(parentScreen, configData);
    }
    
    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY, float partialTicks) {
        int currentY = y;
        
        // 标题
        graphics.drawString(getFont(), "§l通用设置", x, currentY, TEXT_COLOR, false);
        currentY += 20;
        
        // 模组启用开关
        renderToggleOption(graphics, x, currentY, width, "启用ArisSweeping", true);
        currentY += ITEM_HEIGHT;
        
        // 调试模式
        renderToggleOption(graphics, x, currentY, width, "调试模式", false);
        currentY += ITEM_HEIGHT;
        
        // 自动保存配置
        renderToggleOption(graphics, x, currentY, width, "自动保存配置", true);
        currentY += ITEM_HEIGHT;
        
        // 语言设置
        renderDropdownOption(graphics, x, currentY, width, "界面语言", "简体中文");
        currentY += ITEM_HEIGHT;
        
        // 配置文件路径
        renderTextOption(graphics, x, currentY, width, "配置文件", "config/arisweeping.json");
        currentY += ITEM_HEIGHT;
    }
    
    @Override
    public int getContentHeight() {
        return 20 + ITEM_HEIGHT * 5 + 50; // 标题 + 5个选项 + 底部间距
    }
    
    /**
     * 渲染开关选项
     */
    private void renderToggleOption(GuiGraphics graphics, int x, int y, int width, String label, boolean value) {
        // 标签
        graphics.drawString(getFont(), label, x, y + 6, LABEL_COLOR, false);
        
        // 开关按钮
        int buttonX = x + width - 50;
        int buttonY = y + 2;
        int buttonWidth = 40;
        int buttonHeight = 16;
        
        int bgColor = value ? 0xFF4CAF50 : 0xFF757575;
        int knobColor = 0xFFFFFFFF;
        
        // 按钮背景
        graphics.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, bgColor);
        
        // 滑块
        int knobX = value ? buttonX + buttonWidth - 14 : buttonX + 2;
        graphics.fill(knobX, buttonY + 2, knobX + 12, buttonY + buttonHeight - 2, knobColor);
        
        // 状态文本
        String statusText = value ? "开启" : "关闭";
        int statusX = buttonX - getFont().width(statusText) - 5;
        graphics.drawString(getFont(), statusText, statusX, y + 6, value ? 0xFF4CAF50 : 0xFF757575, false);
    }
    
    /**
     * 渲染下拉选项
     */
    private void renderDropdownOption(GuiGraphics graphics, int x, int y, int width, String label, String value) {
        // 标签
        graphics.drawString(getFont(), label, x, y + 6, LABEL_COLOR, false);
        
        // 下拉框
        int boxX = x + width - 120;
        int boxY = y + 2;
        int boxWidth = 100;
        int boxHeight = 16;
        
        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF3E3E42);
        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + 1, 0xFF464647); // 上边框
        graphics.fill(boxX, boxY + boxHeight - 1, boxX + boxWidth, boxY + boxHeight, 0xFF464647); // 下边框
        graphics.fill(boxX, boxY, boxX + 1, boxY + boxHeight, 0xFF464647); // 左边框
        graphics.fill(boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF464647); // 右边框
        
        // 值文本
        graphics.drawString(getFont(), value, boxX + 5, y + 6, TEXT_COLOR, false);
        
        // 下拉箭头
        graphics.drawString(getFont(), "▼", boxX + boxWidth - 15, y + 6, 0xFF999999, false);
    }
    
    /**
     * 渲染文本选项
     */
    private void renderTextOption(GuiGraphics graphics, int x, int y, int width, String label, String value) {
        // 标签
        graphics.drawString(getFont(), label, x, y + 6, LABEL_COLOR, false);
        
        // 值（只读）
        graphics.drawString(getFont(), value, x + 100, y + 6, 0xFF888888, false);
    }
    
    @Override
    public void resetToDefaults() {
        // 重置通用设置为默认值
        // TODO: 实现重置逻辑
    }
}