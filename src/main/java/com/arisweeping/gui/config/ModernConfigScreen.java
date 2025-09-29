package com.arisweeping.gui.config;

import java.util.ArrayList;
import java.util.List;

import com.arisweeping.core.ArisLogger;
import com.arisweeping.core.ArisSweepingMod;
import com.arisweeping.data.ConfigData;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * 现代化ArisSweeping配置界面 - 基于MiniHUD设计
 * 采用Tab式布局和现代UI设计
 */
public class ModernConfigScreen extends Screen {
    
    // 界面常量
    private static final int HEADER_HEIGHT = 60;
    private static final int TAB_HEIGHT = 24;
    private static final int TAB_SPACING = 2;
    private static final int CONTENT_PADDING = 20;
    private static final int FOOTER_HEIGHT = 40;
    
    // 颜色常量 - 现代深色主题
    private static final int BG_COLOR = 0xFF1A1A1A;           // 深色背景
    private static final int HEADER_COLOR = 0xFF2D2D30;       // 标题栏颜色
    private static final int TAB_INACTIVE_COLOR = 0xFF3E3E42; // 非活动Tab
    private static final int TAB_ACTIVE_COLOR = 0xFF007ACC;   // 活动Tab (蓝色)
    private static final int TAB_HOVER_COLOR = 0xFF094771;    // Tab悬停
    private static final int BORDER_COLOR = 0xFF464647;       // 边框颜色
    private static final int TEXT_PRIMARY = 0xFFFFFFFF;       // 主文本
    private static final int TEXT_SECONDARY = 0xFFCCCCCC;     // 次要文本
    private static final int ACCENT_COLOR = 0xFF007ACC;       // 强调色
    
    /**
     * 配置分类标签
     */
    public enum ConfigTab {
        GENERAL("通用设置", "general"),
        ITEM_CLEANING("物品清理", "items"), 
        ENTITY_CLEANING("实体清理", "entities"),
        PERFORMANCE("性能优化", "performance"),
        DISPLAY("显示设置", "display"),
        ADVANCED("高级选项", "advanced");
        
        private final String displayName;
        private final String key;
        
        ConfigTab(String displayName, String key) {
            this.displayName = displayName;
            this.key = key;
        }
        
        public String getDisplayName() { return displayName; }
        public String getKey() { return key; }
    }
    
    private final Screen parentScreen;
    private final ConfigData configData;
    private ConfigTab currentTab = ConfigTab.GENERAL;
    
    // UI组件
    private final List<TabButton> tabButtons = new ArrayList<>();
    private ConfigTabRenderer currentRenderer;
    private GridLayout layout;
    
    // 滚动支持
    private double scrollOffset = 0;
    private boolean dragging = false;
    
    public ModernConfigScreen(Screen parentScreen) {
        super(Component.translatable("gui.arisweeping.config.title"));
        this.parentScreen = parentScreen;
        
        // 确保配置数据存在
        ConfigData tempConfigData = ArisSweepingMod.getConfigData();
        if (tempConfigData == null) {
            tempConfigData = new ConfigData();
            ArisSweepingMod.updateConfigData(tempConfigData);
            ArisLogger.warn("配置数据为null，已创建默认配置数据");
        }
        this.configData = tempConfigData;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // 清理现有组件
        clearWidgets();
        tabButtons.clear();
        
        // 创建Tab按钮
        createTabButtons();
        
        // 创建底部按钮
        createFooterButtons();
        
        // 切换到当前标签
        switchToTab(currentTab);
    }
    
    /**
     * 创建Tab按钮
     */
    private void createTabButtons() {
        int totalTabWidth = 0;
        for (ConfigTab tab : ConfigTab.values()) {
            totalTabWidth += font.width(tab.getDisplayName()) + 20; // 20为内边距
        }
        totalTabWidth += (ConfigTab.values().length - 1) * TAB_SPACING;
        
        int startX = (width - totalTabWidth) / 2;
        int tabY = HEADER_HEIGHT - TAB_HEIGHT;
        int currentX = startX;
        
        for (ConfigTab tab : ConfigTab.values()) {
            int tabWidth = font.width(tab.getDisplayName()) + 20;
            TabButton button = new TabButton(currentX, tabY, tabWidth, TAB_HEIGHT, tab);
            tabButtons.add(button);
            addRenderableWidget(button);
            currentX += tabWidth + TAB_SPACING;
        }
    }
    
    /**
     * 创建底部按钮
     */
    private void createFooterButtons() {
        int buttonWidth = 80;
        int buttonHeight = 20;
        int spacing = 10;
        int totalWidth = buttonWidth * 3 + spacing * 2;
        int startX = (width - totalWidth) / 2;
        int buttonY = height - FOOTER_HEIGHT + 10;
        
        // 完成按钮
        addRenderableWidget(Button.builder(Component.literal("完成"), 
                button -> {
                    saveConfig();
                    minecraft.setScreen(parentScreen);
                })
                .bounds(startX, buttonY, buttonWidth, buttonHeight)
                .build());
        
        // 重置按钮
        addRenderableWidget(Button.builder(Component.literal("重置"), 
                button -> resetCurrentTabConfig())
                .bounds(startX + buttonWidth + spacing, buttonY, buttonWidth, buttonHeight)
                .build());
        
        // 取消按钮
        addRenderableWidget(Button.builder(Component.literal("取消"), 
                button -> minecraft.setScreen(parentScreen))
                .bounds(startX + (buttonWidth + spacing) * 2, buttonY, buttonWidth, buttonHeight)
                .build());
    }
    
    /**
     * 切换到指定标签
     */
    public void switchToTab(ConfigTab tab) {
        this.currentTab = tab;
        
        // 更新Tab按钮状态
        for (TabButton button : tabButtons) {
            button.setSelected(button.tab == tab);
        }
        
        // 创建对应的渲染器
        this.currentRenderer = createTabRenderer(tab);
        this.scrollOffset = 0;
    }
    
    /**
     * 创建标签渲染器
     */
    private ConfigTabRenderer createTabRenderer(ConfigTab tab) {
        return switch (tab) {
            case GENERAL -> new GeneralTabRenderer(this, configData);
            case ITEM_CLEANING -> new ItemCleaningTabRenderer(this, configData);
            case ENTITY_CLEANING -> new EntityCleaningTabRenderer(this, configData);
            case PERFORMANCE -> new PerformanceTabRenderer(this, configData);
            case DISPLAY -> new DisplayTabRenderer(this, configData);
            case ADVANCED -> new AdvancedTabRenderer(this, configData);
        };
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // 渲染背景
        renderBackground(graphics);
        
        // 渲染标题栏
        renderHeader(graphics);
        
        // 渲染内容区域
        renderContent(graphics, mouseX, mouseY, partialTicks);
        
        // 渲染底部
        renderFooter(graphics);
        
        // 渲染组件
        super.render(graphics, mouseX, mouseY, partialTicks);
        
        // 渲染工具提示
        if (currentRenderer != null) {
            currentRenderer.renderTooltips(graphics, mouseX, mouseY);
        }
    }
    
    /**
     * 渲染背景
     */
    public void renderBackground(GuiGraphics graphics) {
        // 主背景
        graphics.fill(0, 0, width, height, BG_COLOR);
        
        // 标题栏背景
        graphics.fill(0, 0, width, HEADER_HEIGHT, HEADER_COLOR);
        
        // 内容区域边框
        int contentY = HEADER_HEIGHT;
        int contentHeight = height - HEADER_HEIGHT - FOOTER_HEIGHT;
        graphics.fill(0, contentY, width, contentY + contentHeight, BG_COLOR);
        graphics.fill(0, contentY, 1, contentY + contentHeight, BORDER_COLOR); // 左边框
        graphics.fill(width - 1, contentY, width, contentY + contentHeight, BORDER_COLOR); // 右边框
        graphics.fill(0, contentY + contentHeight, width, contentY + contentHeight + 1, BORDER_COLOR); // 底边框
    }
    
    /**
     * 渲染标题栏
     */
    private void renderHeader(GuiGraphics graphics) {
        // 标题文本
        String title = "ArisSweeping 配置面板";
        int titleWidth = font.width(title);
        int titleX = (width - titleWidth) / 2;
        int titleY = 15;
        
        graphics.drawString(font, title, titleX, titleY, TEXT_PRIMARY, false);
        
        // 版本信息
        String version = "v1.0.0-alpha";
        int versionX = width - font.width(version) - 10;
        int versionY = 15;
        graphics.drawString(font, version, versionX, versionY, TEXT_SECONDARY, false);
    }
    
    /**
     * 渲染内容区域
     */
    private void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (currentRenderer == null) return;
        
        int contentX = CONTENT_PADDING;
        int contentY = HEADER_HEIGHT + 10;
        int contentWidth = width - CONTENT_PADDING * 2;
        int contentHeight = height - HEADER_HEIGHT - FOOTER_HEIGHT - 20;
        
        // 设置剪裁区域
        graphics.enableScissor(0, contentY, width, contentY + contentHeight);
        
        // 应用滚动偏移
        graphics.pose().pushPose();
        graphics.pose().translate(0, -scrollOffset, 0);
        
        // 渲染标签内容
        currentRenderer.render(graphics, contentX, contentY, contentWidth, mouseX, mouseY, partialTicks);
        
        graphics.pose().popPose();
        graphics.disableScissor();
        
        // 渲染滚动条
        renderScrollbar(graphics, contentY, contentHeight);
    }
    
    /**
     * 渲染底部区域
     */
    private void renderFooter(GuiGraphics graphics) {
        int footerY = height - FOOTER_HEIGHT;
        graphics.fill(0, footerY, width, height, HEADER_COLOR);
        graphics.fill(0, footerY, width, footerY + 1, BORDER_COLOR); // 顶边框
    }
    
    /**
     * 渲染滚动条
     */
    private void renderScrollbar(GuiGraphics graphics, int contentY, int contentHeight) {
        if (currentRenderer == null) return;
        
        int maxScroll = currentRenderer.getContentHeight() - contentHeight;
        if (maxScroll <= 0) return; // 不需要滚动条
        
        int scrollbarX = width - 10;
        int scrollbarWidth = 6;
        int scrollbarHeight = Math.max(20, (int) ((double) contentHeight / currentRenderer.getContentHeight() * contentHeight));
        int scrollbarY = contentY + (int) (scrollOffset / maxScroll * (contentHeight - scrollbarHeight));
        
        // 滚动条轨道
        graphics.fill(scrollbarX - 1, contentY, scrollbarX + scrollbarWidth + 1, contentY + contentHeight, 0xFF2D2D30);
        
        // 滚动条滑块
        graphics.fill(scrollbarX, scrollbarY, scrollbarX + scrollbarWidth, scrollbarY + scrollbarHeight, ACCENT_COLOR);
    }
    
    /**
     * Tab按钮类
     */
    private class TabButton extends Button {
        private final ConfigTab tab;
        private boolean selected = false;
        
        public TabButton(int x, int y, int width, int height, ConfigTab tab) {
            super(x, y, width, height, Component.literal(tab.getDisplayName()), 
                  button -> switchToTab(tab), DEFAULT_NARRATION);
            this.tab = tab;
        }
        
        public void setSelected(boolean selected) {
            this.selected = selected;
        }
        
        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            int bgColor;
            int textColor = TEXT_PRIMARY;
            
            if (selected) {
                bgColor = TAB_ACTIVE_COLOR;
            } else if (isHovered()) {
                bgColor = TAB_HOVER_COLOR;
            } else {
                bgColor = TAB_INACTIVE_COLOR;
                textColor = TEXT_SECONDARY;
            }
            
            // 渲染Tab背景
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bgColor);
            
            // 渲染Tab边框
            if (selected) {
                graphics.fill(getX(), getY() + getHeight() - 2, getX() + getWidth(), getY() + getHeight(), TAB_ACTIVE_COLOR);
            }
            
            // 渲染文本
            int textX = getX() + (getWidth() - font.width(tab.getDisplayName())) / 2;
            int textY = getY() + (getHeight() - font.lineHeight) / 2;
            graphics.drawString(font, tab.getDisplayName(), textX, textY, textColor, false);
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (currentRenderer != null) {
            int maxScroll = Math.max(0, currentRenderer.getContentHeight() - (height - HEADER_HEIGHT - FOOTER_HEIGHT - 20));
            double scrollAmount = delta * 20;
            scrollOffset = Mth.clamp(scrollOffset - scrollAmount, 0, maxScroll);
            return true;
        }
        return false;
    }
    
    /**
     * 保存配置
     */
    private void saveConfig() {
        try {
            // TODO: 实现配置保存逻辑
            ArisLogger.info("配置已保存");
        } catch (Exception e) {
            ArisLogger.error("保存配置失败", e);
        }
    }
    
    /**
     * 重置当前标签配置
     */
    private void resetCurrentTabConfig() {
        if (currentRenderer != null) {
            currentRenderer.resetToDefaults();
            ArisLogger.info("已重置 {} 配置", currentTab.getDisplayName());
        }
    }
    
    public ConfigData getConfigData() {
        return configData;
    }
}