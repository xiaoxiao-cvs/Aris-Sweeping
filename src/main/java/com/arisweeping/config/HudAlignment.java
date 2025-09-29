package com.arisweeping.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

/**
 * HUD对齐方式枚举
 * 基于MiniHUD的HudAlignment设计
 */
public enum HudAlignment implements IConfigOptionListEntry {
    TOP_LEFT("top_left"),
    TOP_RIGHT("top_right"),
    TOP_CENTER("top_center"),
    BOTTOM_LEFT("bottom_left"), 
    BOTTOM_RIGHT("bottom_right"),
    BOTTOM_CENTER("bottom_center");
    
    private final String configString;
    
    HudAlignment(String configString) {
        this.configString = configString;
    }
    
    @Override
    public String getStringValue() {
        return this.configString;
    }
    
    @Override
    public String getDisplayName() {
        return StringUtils.translate("arisweeping.config.hud_alignment." + this.configString);
    }
    
    @Override
    public IConfigOptionListEntry cycle(boolean forward) {
        int id = this.ordinal();
        
        if (forward) {
            if (++id >= values().length) {
                id = 0;
            }
        } else {
            if (--id < 0) {
                id = values().length - 1;
            }
        }
        
        return values()[id % values().length];
    }
    
    @Override
    public HudAlignment fromString(String name) {
        for (HudAlignment alignment : values()) {
            if (alignment.configString.equalsIgnoreCase(name)) {
                return alignment;
            }
        }
        return TOP_LEFT; // 默认值
    }
}