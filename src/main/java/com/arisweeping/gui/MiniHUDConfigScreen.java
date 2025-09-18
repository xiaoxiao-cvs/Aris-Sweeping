package com.arisweeping.gui;

import com.arisweeping.core.ArisSweepingMod;
import com.arisweeping.data.ConfigData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * MiniHUD风格的配置主界面
 * 采用侧边栏分类导航和主内容区域的布局
 */
public class MiniHUDConfigScreen extends Screen {
    
    // 界面布局常量
    private static final int SIDEBAR_WIDTH = 120;
    private static final int CONTENT_PADDING = 10;
    private static final int CATEGORY_BUTTON_HEIGHT = 20;
    private static final int CATEGORY_SPACING = 2;
    
    // 颜色常量
    private static final int BACKGROUND_COLOR = 0xFF2D2D30;
    private static final int SIDEBAR_COLOR = 0xFF1E1E1E;
    private static final int BORDER_COLOR = 0xFF404040;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int ACCENT_COLOR = 0xFF007ACC;
    
    /**
     * 配置分类
     */
    public enum ConfigCategory {
        ITEM_CLEANING("物品清理", "item_cleaning"),
        ANIMAL_CLEANING("动物清理", "animal_cleaning"),
        TASK_MANAGEMENT("任务管理", "task_management"),
        PERFORMANCE("性能设置", "performance"),
        STATISTICS("统计信息", "statistics");
        
        private final String displayName;
        private final String key;
        
        ConfigCategory(String displayName, String key) {
            this.displayName = displayName;
            this.key = key;
        }
        
        public String getDisplayName() { return displayName; }
        public String getKey() { return key; }
    }
    
    private final Screen parentScreen;
    private final ConfigData configData;
    private ConfigCategory selectedCategory = ConfigCategory.ITEM_CLEANING;
    
    // UI组件
    private final List<Button> categoryButtons = new ArrayList<>();
    private ConfigCategoryRenderer currentCategoryRenderer;
    
    // 滚动支持
    private int scrollOffset = 0;
    private int maxScroll = 0;
    
    public MiniHUDConfigScreen(Screen parentScreen) {
        super(Component.translatable("gui.arisweeping.config.title"));
        this.parentScreen = parentScreen;
        this.configData = ArisSweepingMod.getConfigData();
    }
    
    @Override
    protected void init() {
        super.init();
        
        // 清除现有组件
        categoryButtons.clear();
        clearWidgets();
        
        // 创建分类按钮
        createCategoryButtons();
        
        // 创建通用控制按钮
        createControlButtons();
        
        // 初始化当前分类渲染器
        switchToCategory(selectedCategory);
    }
    
    /**
     * 创建侧边栏分类按钮
     */
    private void createCategoryButtons() {
        int buttonY = 30;
        
        for (ConfigCategory category : ConfigCategory.values()) {
            Button categoryButton = Button.builder(
                Component.translatable("config.arisweeping.category." + category.getKey()),
                btn -> switchToCategory(category)
            )
            .bounds(CONTENT_PADDING, buttonY, SIDEBAR_WIDTH - CONTENT_PADDING * 2, CATEGORY_BUTTON_HEIGHT)
            .build();
            
            categoryButtons.add(categoryButton);
            addRenderableWidget(categoryButton);
            
            buttonY += CATEGORY_BUTTON_HEIGHT + CATEGORY_SPACING;
        }
    }
    
    /**
     * 创建底部控制按钮
     */
    private void createControlButtons() {
        int buttonWidth = 60;
        int buttonHeight = 20;
        int spacing = 5;
        
        int totalButtonsWidth = buttonWidth * 3 + spacing * 2;
        int startX = (width - totalButtonsWidth) / 2;
        int buttonY = height - 30;
        
        // 重置按钮
        Button resetButton = Button.builder(
            Component.translatable("config.arisweeping.reset"),
            btn -> resetToDefaults()
        )
        .bounds(startX, buttonY, buttonWidth, buttonHeight)
        .build();
        addRenderableWidget(resetButton);
        
        // 取消按钮
        Button cancelButton = Button.builder(
            Component.translatable("config.arisweeping.cancel"),
            btn -> onClose()
        )
        .bounds(startX + buttonWidth + spacing, buttonY, buttonWidth, buttonHeight)
        .build();
        addRenderableWidget(cancelButton);
        
        // 保存按钮
        Button saveButton = Button.builder(
            Component.translatable("config.arisweeping.save"),
            btn -> saveAndClose()
        )
        .bounds(startX + (buttonWidth + spacing) * 2, buttonY, buttonWidth, buttonHeight)
        .build();
        addRenderableWidget(saveButton);
    }
    
    /**
     * 切换到指定分类
     */
    private void switchToCategory(ConfigCategory category) {
        this.selectedCategory = category;
        this.scrollOffset = 0;
        
        // 更新按钮样式
        updateCategoryButtonStyles();
        
        // 创建对应的分类渲染器
        this.currentCategoryRenderer = createCategoryRenderer(category);
    }
    
    /**
     * 更新分类按钮样式
     */
    private void updateCategoryButtonStyles() {
        for (int i = 0; i < categoryButtons.size(); i++) {
            Button button = categoryButtons.get(i);
            ConfigCategory category = ConfigCategory.values()[i];
            
            // 这里可以通过自定义按钮样式来高亮选中的分类
            // 由于Minecraft GUI限制，我们在渲染时处理高亮效果
        }
    }
    
    /**
     * 创建分类渲染器
     */
    private ConfigCategoryRenderer createCategoryRenderer(ConfigCategory category) {
        switch (category) {
            case ITEM_CLEANING:
                return new ItemCleaningCategoryRenderer(this, configData);
            case ANIMAL_CLEANING:
                return new AnimalCleaningCategoryRenderer(this, configData);
            case TASK_MANAGEMENT:
                return new TaskManagementCategoryRenderer(this, configData);
            case PERFORMANCE:
                return new PerformanceCategoryRenderer(this, configData);
            case STATISTICS:
                return new StatisticsCategoryRenderer(this, configData);
            default:
                return new ItemCleaningCategoryRenderer(this, configData);
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染背景
        renderBackground(guiGraphics);
        
        // 渲染侧边栏
        renderSidebar(guiGraphics);
        
        // 渲染主内容区域
        renderContent(guiGraphics, mouseX, mouseY, partialTick);
        
        // 渲染标题
        renderTitle(guiGraphics);
        
        // 渲染子组件
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // 渲染统计信息
        renderStatistics(guiGraphics);
    }
    
    /**
     * 渲染背景
     */
    private void renderBackground(GuiGraphics guiGraphics) {
        // 主背景
        guiGraphics.fill(0, 0, width, height, BACKGROUND_COLOR);
        
        // 侧边栏背景
        guiGraphics.fill(0, 0, SIDEBAR_WIDTH, height, SIDEBAR_COLOR);
        
        // 边界线
        guiGraphics.fill(SIDEBAR_WIDTH, 0, SIDEBAR_WIDTH + 1, height, BORDER_COLOR);
    }
    
    /**
     * 渲染侧边栏
     */
    private void renderSidebar(GuiGraphics guiGraphics) {
        // 高亮选中的分类按钮
        for (int i = 0; i < categoryButtons.size(); i++) {
            Button button = categoryButtons.get(i);
            ConfigCategory category = ConfigCategory.values()[i];
            
            if (category == selectedCategory) {
                // 绘制选中状态的背景
                guiGraphics.fill(
                    button.getX() - 2,
                    button.getY(),
                    button.getX() + button.getWidth() + 2,
                    button.getY() + button.getHeight(),
                    ACCENT_COLOR
                );
            }
        }
    }
    
    /**
     * 渲染主内容区域
     */
    private void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (currentCategoryRenderer != null) {
            int contentX = SIDEBAR_WIDTH + CONTENT_PADDING;
            int contentY = 30;
            int contentWidth = width - SIDEBAR_WIDTH - CONTENT_PADDING * 2;
            int contentHeight = height - 80; // 留出底部按钮空间
            
            // 设置裁剪区域
            guiGraphics.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);
            
            // 渲染分类内容
            currentCategoryRenderer.render(guiGraphics, contentX, contentY - scrollOffset, 
                                         contentWidth, mouseX, mouseY, partialTick);
            
            guiGraphics.disableScissor();
            
            // 渲染滚动条（如果需要）
            if (maxScroll > 0) {
                renderScrollbar(guiGraphics, contentX + contentWidth - 8, contentY, contentHeight);
            }
        }
    }
    
    /**
     * 渲染标题
     */
    private void renderTitle(GuiGraphics guiGraphics) {
        String title = "ArisSweeping 配置";
        int titleX = SIDEBAR_WIDTH + CONTENT_PADDING;
        int titleY = 10;
        
        guiGraphics.drawString(font, title, titleX, titleY, TEXT_COLOR, false);
        
        // 当前分类名称
        String categoryTitle = selectedCategory.getDisplayName();
        int categoryTitleX = titleX;
        int categoryTitleY = titleY + 12;
        
        guiGraphics.drawString(font, categoryTitle, categoryTitleX, categoryTitleY, 0xFFCCCCCC, false);
    }
    
    /**
     * 渲染滚动条
     */
    private void renderScrollbar(GuiGraphics guiGraphics, int x, int y, int height) {
        // 滚动条背景
        guiGraphics.fill(x, y, x + 6, y + height, 0xFF444444);
        
        // 滚动条滑块
        int sliderHeight = Math.max(10, height * height / (height + maxScroll));
        int sliderY = y + scrollOffset * (height - sliderHeight) / maxScroll;
        
        guiGraphics.fill(x + 1, sliderY, x + 5, sliderY + sliderHeight, 0xFF888888);
    }
    
    /**
     * 渲染统计信息
     */
    private void renderStatistics(GuiGraphics guiGraphics) {
        // 在右下角显示简单统计信息
        String stats = String.format("任务: %d | 清理: %d", 
                                    getActiveTaskCount(), getTotalCleanedCount());
        
        int statsX = width - font.width(stats) - 10;
        int statsY = height - 15;
        
        guiGraphics.drawString(font, stats, statsX, statsY, 0xFF999999, false);
    }
    
    /**
     * 处理鼠标滚轮
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX > SIDEBAR_WIDTH && currentCategoryRenderer != null) {
            int oldScrollOffset = scrollOffset;
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)(delta * 10)));
            
            return scrollOffset != oldScrollOffset;
        }
        
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    /**
     * 重置为默认值
     */
    private void resetToDefaults() {
        configData.resetToDefaults();
        
        // 刷新当前分类渲染器
        currentCategoryRenderer = createCategoryRenderer(selectedCategory);
    }
    
    /**
     * 保存并关闭
     */
    private void saveAndClose() {
        // 保存配置数据
        configData.save();
        
        onClose();
    }
    
    @Override
    public void onClose() {
        minecraft.setScreen(parentScreen);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    // 获取统计数据的辅助方法
    private int getActiveTaskCount() {
        // 这里应该从TaskManager获取活动任务数量
        return 0; // 临时返回值
    }
    
    private int getTotalCleanedCount() {
        // 这里应该从统计系统获取总清理数量
        return 0; // 临时返回值
    }
    
    // Getter方法供分类渲染器使用
    public int getScrollOffset() { return scrollOffset; }
    public void setMaxScroll(int maxScroll) { this.maxScroll = maxScroll; }
    public ConfigData getConfigData() { return configData; }
}