package com.arisweeping.gui;

import com.arisweeping.data.ConfigData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 配置分类渲染器抽象基类
 */
public abstract class ConfigCategoryRenderer {
    
    protected final MiniHUDConfigScreen parentScreen;
    protected final ConfigData configData;
    
    public ConfigCategoryRenderer(MiniHUDConfigScreen parentScreen, ConfigData configData) {
        this.parentScreen = parentScreen;
        
        // 确保configData不为null
        if (configData == null) {
            throw new IllegalArgumentException("ConfigData cannot be null");
        }
        this.configData = configData;
    }
    
    /**
     * 获取字体实例
     */
    protected net.minecraft.client.gui.Font getFont() {
        return Minecraft.getInstance().font;
    }
    
    /**
     * 渲染分类内容
     */
    public abstract void render(GuiGraphics guiGraphics, int x, int y, int width, 
                              int mouseX, int mouseY, float partialTick);
    
    /**
     * 处理鼠标点击
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }
    
    /**
     * 处理键盘输入
     */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }
    
    /**
     * 获取内容高度（用于滚动计算）
     */
    public abstract int getContentHeight();
}

/**
 * 物品清理分类渲染器
 */
class ItemCleaningCategoryRenderer extends ConfigCategoryRenderer {
    
    public ItemCleaningCategoryRenderer(MiniHUDConfigScreen parentScreen, ConfigData configData) {
        super(parentScreen, configData);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int width, 
                      int mouseX, int mouseY, float partialTick) {
        // 双重null检查防护
        if (configData == null) {
            guiGraphics.drawString(getFont(), "§c配置数据未加载", x, y + 10, 0xFFFFFF, false);
            return;
        }
        
        if (configData.getItemCleaningConfig() == null) {
            guiGraphics.drawString(getFont(), "§c物品清理配置未加载", x, y + 10, 0xFFFFFF, false);
            return;
        }
        
        int currentY = y + 10;
        
        // 启用/禁用开关
        renderToggleOption(guiGraphics, x, currentY, width, 
                          "启用物品清理", configData.getItemCleaningConfig().isEnabled());
        currentY += 25;
        
        // 存活时间设置
        renderSliderOption(guiGraphics, x, currentY, width,
                          "物品存活时间 (秒)", 
                          configData.getItemCleaningConfig().getItemLifetimeSeconds(),
                          30, 600, 30);
        currentY += 25;
        
        // 区块范围限制
        renderSliderOption(guiGraphics, x, currentY, width,
                          "清理范围 (区块)", 
                          configData.getItemCleaningConfig().getChunkRange(),
                          1, 16, 1);
        currentY += 25;
        
        // 最小物品数量
        renderSliderOption(guiGraphics, x, currentY, width,
                          "最小清理数量", 
                          configData.getItemCleaningConfig().getMinItemCount(),
                          1, 100, 1);
        currentY += 25;
        
        // 白名单/黑名单选项
        renderListOption(guiGraphics, x, currentY, width,
                        "物品白名单", configData.getItemCleaningConfig().getItemWhitelist());
        currentY += 60;
        
        renderListOption(guiGraphics, x, currentY, width,
                        "物品黑名单", configData.getItemCleaningConfig().getItemBlacklist());
        currentY += 60;
        
        // 更新最大滚动值
        parentScreen.setMaxScroll(Math.max(0, currentY - y - parentScreen.height + 150));
    }
    
    @Override
    public int getContentHeight() {
        return 300; // 估算高度
    }
    
    private void renderToggleOption(GuiGraphics guiGraphics, int x, int y, int width,
                                   String label, boolean value) {
        // 渲染标签
        guiGraphics.drawString(Minecraft.getInstance().font, label, x, y + 5, 0xFFFFFFFF, false);
        
        // 渲染开关按钮
        int toggleX = x + width - 50;
        int toggleY = y;
        int toggleWidth = 40;
        int toggleHeight = 15;
        
        int bgColor = value ? 0xFF007ACC : 0xFF444444;
        int fgColor = 0xFFFFFFFF;
        
        guiGraphics.fill(toggleX, toggleY, toggleX + toggleWidth, toggleY + toggleHeight, bgColor);
        
        String text = value ? "开" : "关";
        int textX = toggleX + (toggleWidth - getFont().width(text)) / 2;
        int textY = toggleY + (toggleHeight - 8) / 2;
        
        guiGraphics.drawString(getFont(), text, textX, textY, fgColor, false);
    }
    
    private void renderSliderOption(GuiGraphics guiGraphics, int x, int y, int width,
                                   String label, int value, int min, int max, int step) {
        // 渲染标签和当前值
        String labelWithValue = label + ": " + value;
        guiGraphics.drawString(getFont(), labelWithValue, x, y, 0xFFFFFFFF, false);
        
        // 渲染滑块
        int sliderX = x;
        int sliderY = y + 12;
        int sliderWidth = width - 50;
        int sliderHeight = 4;
        
        // 滑块背景
        guiGraphics.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + sliderHeight, 0xFF444444);
        
        // 滑块进度
        float progress = (float) (value - min) / (max - min);
        int progressWidth = (int) (sliderWidth * progress);
        guiGraphics.fill(sliderX, sliderY, sliderX + progressWidth, sliderY + sliderHeight, 0xFF007ACC);
        
        // 滑块手柄
        int handleX = sliderX + progressWidth - 2;
        int handleY = sliderY - 2;
        int handleSize = 8;
        
        guiGraphics.fill(handleX, handleY, handleX + handleSize, handleY + handleSize, 0xFFFFFFFF);
    }
    
    private void renderListOption(GuiGraphics guiGraphics, int x, int y, int width,
                                 String label, java.util.List<String> items) {
        // 渲染标签
        guiGraphics.drawString(getFont(), label, x, y, 0xFFFFFFFF, false);
        
        // 渲染列表框
        int listX = x;
        int listY = y + 12;
        int listWidth = width - 50;
        int listHeight = 40;
        
        // 列表背景
        guiGraphics.fill(listX, listY, listX + listWidth, listY + listHeight, 0xFF1E1E1E);
        guiGraphics.fill(listX, listY, listX + listWidth, listY + 1, 0xFF404040);
        guiGraphics.fill(listX, listY + listHeight - 1, listX + listWidth, listY + listHeight, 0xFF404040);
        guiGraphics.fill(listX, listY, listX + 1, listY + listHeight, 0xFF404040);
        guiGraphics.fill(listX + listWidth - 1, listY, listX + listWidth, listY + listHeight, 0xFF404040);
        
        // 渲染列表项
        int itemY = listY + 2;
        for (String item : items) {
            if (itemY + 10 > listY + listHeight - 2) break;
            
            guiGraphics.drawString(getFont(), item, listX + 5, itemY, 0xFFCCCCCC, false);
            itemY += 10;
        }
        
        // 添加按钮
        int addButtonX = x + width - 45;
        int addButtonY = y + 15;
        int addButtonWidth = 40;
        int addButtonHeight = 15;
        
        guiGraphics.fill(addButtonX, addButtonY, addButtonX + addButtonWidth, addButtonY + addButtonHeight, 0xFF007ACC);
        
        String addText = "添加";
        int addTextX = addButtonX + (addButtonWidth - getFont().width(addText)) / 2;
        int addTextY = addButtonY + (addButtonHeight - 8) / 2;
        
        guiGraphics.drawString(getFont(), addText, addTextX, addTextY, 0xFFFFFFFF, false);
    }
}

/**
 * 动物清理分类渲染器
 */
class AnimalCleaningCategoryRenderer extends ConfigCategoryRenderer {
    
    public AnimalCleaningCategoryRenderer(MiniHUDConfigScreen parentScreen, ConfigData configData) {
        super(parentScreen, configData);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int width, 
                      int mouseX, int mouseY, float partialTick) {
        // 双重null检查防护
        if (configData == null) {
            guiGraphics.drawString(getFont(), "§c配置数据未加载", x, y + 10, 0xFFFFFF, false);
            return;
        }
        
        if (configData.getAnimalCleaningConfig() == null) {
            guiGraphics.drawString(getFont(), "§c动物清理配置未加载", x, y + 10, 0xFFFFFF, false);
            return;
        }
        
        int currentY = y + 10;
        
        // 启用/禁用开关
        renderToggleOption(guiGraphics, x, currentY, width, 
                          "启用动物清理", configData.getAnimalCleaningConfig().isEnabled());
        currentY += 30;
        
        // 动物密度限制
        renderSliderOption(guiGraphics, x, currentY, width,
                          "密度限制 (只/区块)", 
                          configData.getAnimalCleaningConfig().getMaxAnimalsPerChunk(),
                          5, 50, 1);
        currentY += 30;
        
        // 检查半径
        renderSliderOption(guiGraphics, x, currentY, width,
                          "检查半径 (格)", 
                          configData.getAnimalCleaningConfig().getCheckRadius(),
                          8, 64, 8);
        currentY += 30;
        
        // 繁殖保护
        renderToggleOption(guiGraphics, x, currentY, width,
                          "保护繁殖动物", configData.getAnimalCleaningConfig().isProtectBreeding());
        currentY += 30;
        
        // 幼崽保护
        renderToggleOption(guiGraphics, x, currentY, width,
                          "保护幼崽", configData.getAnimalCleaningConfig().isProtectBabies());
        currentY += 30;
        
        parentScreen.setMaxScroll(Math.max(0, currentY - y - parentScreen.height + 150));
    }
    
    @Override
    public int getContentHeight() {
        return 200;
    }
    
    private void renderToggleOption(GuiGraphics guiGraphics, int x, int y, int width,
                                   String label, boolean value) {
        // 复用ItemCleaningCategoryRenderer的实现
        guiGraphics.drawString(getFont(), label, x, y + 5, 0xFFFFFFFF, false);
        
        int toggleX = x + width - 50;
        int toggleY = y;
        int toggleWidth = 40;
        int toggleHeight = 15;
        
        int bgColor = value ? 0xFF007ACC : 0xFF444444;
        int fgColor = 0xFFFFFFFF;
        
        guiGraphics.fill(toggleX, toggleY, toggleX + toggleWidth, toggleY + toggleHeight, bgColor);
        
        String text = value ? "开" : "关";
        int textX = toggleX + (toggleWidth - getFont().width(text)) / 2;
        int textY = toggleY + (toggleHeight - 8) / 2;
        
        guiGraphics.drawString(getFont(), text, textX, textY, fgColor, false);
    }
    
    private void renderSliderOption(GuiGraphics guiGraphics, int x, int y, int width,
                                   String label, int value, int min, int max, int step) {
        String labelWithValue = label + ": " + value;
        guiGraphics.drawString(getFont(), labelWithValue, x, y, 0xFFFFFFFF, false);
        
        int sliderX = x;
        int sliderY = y + 12;
        int sliderWidth = width - 50;
        int sliderHeight = 4;
        
        guiGraphics.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + sliderHeight, 0xFF444444);
        
        float progress = (float) (value - min) / (max - min);
        int progressWidth = (int) (sliderWidth * progress);
        guiGraphics.fill(sliderX, sliderY, sliderX + progressWidth, sliderY + sliderHeight, 0xFF007ACC);
        
        int handleX = sliderX + progressWidth - 2;
        int handleY = sliderY - 2;
        int handleSize = 8;
        
        guiGraphics.fill(handleX, handleY, handleX + handleSize, handleY + handleSize, 0xFFFFFFFF);
    }
}

/**
 * 任务管理分类渲染器
 */
class TaskManagementCategoryRenderer extends ConfigCategoryRenderer {
    
    public TaskManagementCategoryRenderer(MiniHUDConfigScreen parentScreen, ConfigData configData) {
        super(parentScreen, configData);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int width, 
                      int mouseX, int mouseY, float partialTick) {
        int currentY = y + 10;
        
        // 任务状态显示
        guiGraphics.drawString(getFont(), "当前任务状态:", x, currentY, 0xFFFFFFFF, false);
        currentY += 15;
        
        guiGraphics.drawString(getFont(), "• 活动任务: 0", x + 10, currentY, 0xFFCCCCCC, false);
        currentY += 12;
        
        guiGraphics.drawString(getFont(), "• 队列任务: 0", x + 10, currentY, 0xFFCCCCCC, false);
        currentY += 12;
        
        guiGraphics.drawString(getFont(), "• 可撤销操作: 0", x + 10, currentY, 0xFFCCCCCC, false);
        currentY += 30;
        
        // 撤销操作区域
        guiGraphics.drawString(getFont(), "撤销操作:", x, currentY, 0xFFFFFFFF, false);
        currentY += 15;
        
        // 撤销按钮
        renderButton(guiGraphics, x, currentY, 80, 20, "撤销上次");
        renderButton(guiGraphics, x + 90, currentY, 80, 20, "清除历史");
        currentY += 35;
        
        // 任务控制区域
        guiGraphics.drawString(getFont(), "任务控制:", x, currentY, 0xFFFFFFFF, false);
        currentY += 15;
        
        renderButton(guiGraphics, x, currentY, 80, 20, "暂停任务");
        renderButton(guiGraphics, x + 90, currentY, 80, 20, "恢复任务");
        currentY += 35;
        
        parentScreen.setMaxScroll(Math.max(0, currentY - y - parentScreen.height + 150));
    }
    
    @Override
    public int getContentHeight() {
        return 250;
    }
    
    private void renderButton(GuiGraphics guiGraphics, int x, int y, int width, int height, String text) {
        // 按钮背景
        guiGraphics.fill(x, y, x + width, y + height, 0xFF555555);
        guiGraphics.fill(x, y, x + width, y + 1, 0xFF777777);
        guiGraphics.fill(x, y, x + 1, y + height, 0xFF777777);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xFF333333);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFF333333);
        
        // 按钮文本
        int textX = x + (width - getFont().width(text)) / 2;
        int textY = y + (height - 8) / 2;
        
        guiGraphics.drawString(getFont(), text, textX, textY, 0xFFFFFFFF, false);
    }
}

/**
 * 性能设置分类渲染器
 */
class PerformanceCategoryRenderer extends ConfigCategoryRenderer {
    
    public PerformanceCategoryRenderer(MiniHUDConfigScreen parentScreen, ConfigData configData) {
        super(parentScreen, configData);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int width, 
                      int mouseX, int mouseY, float partialTick) {
        int currentY = y + 10;
        
        guiGraphics.drawString(getFont(), "线程池配置:", x, currentY, 0xFFFFFFFF, false);
        currentY += 20;
        
        guiGraphics.drawString(getFont(), "• 核心线程数: 4", x + 10, currentY, 0xFFCCCCCC, false);
        currentY += 12;
        
        guiGraphics.drawString(getFont(), "• IO线程数: 2", x + 10, currentY, 0xFFCCCCCC, false);
        currentY += 12;
        
        guiGraphics.drawString(getFont(), "• 调度线程数: 1", x + 10, currentY, 0xFFCCCCCC, false);
        currentY += 30;
        
        guiGraphics.drawString(getFont(), "内存使用:", x, currentY, 0xFFFFFFFF, false);
        currentY += 20;
        
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();
        
        guiGraphics.drawString(getFont(), String.format("• 已用内存: %d MB", used / 1024 / 1024), 
                              x + 10, currentY, 0xFFCCCCCC, false);
        currentY += 12;
        
        guiGraphics.drawString(getFont(), String.format("• 最大内存: %d MB", max / 1024 / 1024), 
                              x + 10, currentY, 0xFFCCCCCC, false);
        currentY += 30;
        
        parentScreen.setMaxScroll(Math.max(0, currentY - y - parentScreen.height + 150));
    }
    
    @Override
    public int getContentHeight() {
        return 200;
    }
}

/**
 * 统计信息分类渲染器
 */
class StatisticsCategoryRenderer extends ConfigCategoryRenderer {
    
    public StatisticsCategoryRenderer(MiniHUDConfigScreen parentScreen, ConfigData configData) {
        super(parentScreen, configData);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int width, 
                      int mouseX, int mouseY, float partialTick) {
        int currentY = y + 10;
        
        guiGraphics.drawString(getFont(), "清理统计:", x, currentY, 0xFFFFFFFF, false);
        currentY += 20;
        
        guiGraphics.drawString(getFont(), "• 总清理实体数: 0", x + 10, currentY, 0xFFCCCCCC, false);
        currentY += 12;
        
        guiGraphics.drawString(getFont(), "• 清理物品数: 0", x + 10, currentY, 0xFFCCCCCC, false);
        currentY += 12;
        
        guiGraphics.drawString(getFont(), "• 清理动物数: 0", x + 10, currentY, 0xFFCCCCCC, false);
        currentY += 12;
        
        guiGraphics.drawString(getFont(), "• 撤销操作数: 0", x + 10, currentY, 0xFFCCCCCC, false);
        currentY += 30;
        
        guiGraphics.drawString(getFont(), "任务统计:", x, currentY, 0xFFFFFFFF, false);
        currentY += 20;
        
        guiGraphics.drawString(getFont(), "• 总执行任务数: 0", x + 10, currentY, 0xFFCCCCCC, false);
        currentY += 12;
        
        guiGraphics.drawString(getFont(), "• 成功任务数: 0", x + 10, currentY, 0xFFCCCCCC, false);
        currentY += 12;
        
        guiGraphics.drawString(getFont(), "• 失败任务数: 0", x + 10, currentY, 0xFFCCCCCC, false);
        currentY += 30;
        
        parentScreen.setMaxScroll(Math.max(0, currentY - y - parentScreen.height + 150));
    }
    
    @Override
    public int getContentHeight() {
        return 200;
    }
}