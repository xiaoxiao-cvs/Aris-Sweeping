package com.arisweeping.gui;

import java.util.Collections;
import java.util.List;

import com.arisweeping.config.Configs;
import com.arisweeping.core.ArisSweepingMod;

import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.StringUtils;

/**
 * ArisSweeping 配置 GUI
 * 基于 MiniHUD 的设计模式，使用 malilib 配置界面框架
 */
public class GuiConfigsAris extends GuiConfigsBase {
    
    public static ConfigGuiTab tab = ConfigGuiTab.GENERAL;
    
    public GuiConfigsAris() {
        super(10, 50, ArisSweepingMod.MODID, null, 
              "arisweeping.gui.title.configs", 
              "ArisSweepingMod", 
              "1.0.0");
    }
    
    @Override
    public void initGui() {
        super.initGui();
        this.clearOptions();
        
        int x = 10;
        int y = 26;
        int rows = 1;
        
        for (ConfigGuiTab configTab : ConfigGuiTab.values()) {
            int width = this.getStringWidth(configTab.getDisplayName()) + 10;
            
            if (x >= this.width - width - 10) {
                x = 10;
                y += 22;
                rows++;
            }
            
            x += this.createButton(x, y, width, configTab);
        }
        
        if (rows > 1) {
            int scrollbarPosition = this.getListWidget().getScrollbar().getValue();
            this.setListPosition(this.getListX(), 50 + (rows - 1) * 22);
            this.reCreateListWidget();
            this.getListWidget().getScrollbar().setValue(scrollbarPosition);
            this.getListWidget().refreshEntries();
        }
    }
    
    private int createButton(int x, int y, int width, ConfigGuiTab configTab) {
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, configTab.getDisplayName());
        button.setEnabled(GuiConfigsAris.tab != configTab);
        this.addButton(button, new ButtonListenerConfigTabs(configTab, this));
        
        return button.getWidth() + 2;
    }
    
    @Override
    protected int getConfigWidth() {
        ConfigGuiTab currentTab = GuiConfigsAris.tab;
        
        if (currentTab == ConfigGuiTab.GENERAL) {
            return 200;
        } else if (currentTab == ConfigGuiTab.ITEM_CLEANING || 
                   currentTab == ConfigGuiTab.ENTITY_CLEANING ||
                   currentTab == ConfigGuiTab.PERFORMANCE) {
            return 280;
        }
        
        return super.getConfigWidth();
    }
    
    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        ConfigGuiTab currentTab = GuiConfigsAris.tab;
        
        if (currentTab == ConfigGuiTab.GENERAL) {
            return ConfigOptionWrapper.createFor(Configs.General.OPTIONS);
        } else if (currentTab == ConfigGuiTab.ITEM_CLEANING) {
            return ConfigOptionWrapper.createFor(Configs.ItemCleaning.OPTIONS);
        } else if (currentTab == ConfigGuiTab.ENTITY_CLEANING) {
            return ConfigOptionWrapper.createFor(Configs.EntityCleaning.OPTIONS);
        } else if (currentTab == ConfigGuiTab.PERFORMANCE) {
            return ConfigOptionWrapper.createFor(Configs.Performance.OPTIONS);
        }
        
        return Collections.emptyList();
    }
    
    /**
     * 配置标签页枚举
     */
    public enum ConfigGuiTab {
        GENERAL         ("arisweeping.gui.button.config_gui.general"),
        ITEM_CLEANING   ("arisweeping.gui.button.config_gui.item_cleaning"),
        ENTITY_CLEANING ("arisweeping.gui.button.config_gui.entity_cleaning"),
        PERFORMANCE     ("arisweeping.gui.button.config_gui.performance");
        
        private final String translationKey;
        
        ConfigGuiTab(String translationKey) {
            this.translationKey = translationKey;
        }
        
        public String getDisplayName() {
            return StringUtils.translate(this.translationKey);
        }
    }
    
    private static class ButtonListenerConfigTabs implements IButtonActionListener {
        private final GuiConfigsAris parent;
        private final ConfigGuiTab tab;
        
        public ButtonListenerConfigTabs(ConfigGuiTab tab, GuiConfigsAris parent) {
            this.tab = tab;
            this.parent = parent;
        }
        
        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
            GuiConfigsAris.tab = this.tab;
            this.parent.reCreateListWidget(); // 应用新的配置宽度
            this.parent.getListWidget().resetScrollbarPosition();
            this.parent.initGui();
        }
    }
}