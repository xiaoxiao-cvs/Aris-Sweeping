package com.arisweeping.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.IConfigInteger;
import fi.dy.masa.malilib.config.IHotkeyTogglable;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyCallbackToggleBoolean;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import fi.dy.masa.malilib.util.StringUtils;
import com.arisweeping.core.ArisSweepingMod;

/**
 * ArisSweeping HUD信息切换配置
 * 基于MiniHUD的InfoToggle设计模式
 */
public enum InfoToggle implements IConfigInteger, IHotkeyTogglable {
    
    // 基础信息
    STATUS              ("infoStatus",              true,   ""),  // 模组状态
    STATISTICS          ("infoStatistics",         true,   ""),  // 统计信息  
    PERFORMANCE         ("infoPerformance",        false,  ""),  // 性能监控
    MEMORY_USAGE        ("infoMemoryUsage",        false,  ""),  // 内存使用
    
    // 清理信息
    CLEANED_ENTITIES    ("infoCleanedEntities",    true,   ""),  // 已清理实体
    CLEANING_TASKS      ("infoCleaningTasks",      true,   ""),  // 执行任务
    LAST_CLEANING       ("infoLastCleaning",       false,  ""),  // 最后清理时间
    ACTIVE_FILTERS      ("infoActiveFilters",      false,  ""),  // 活动过滤器
    
    // 实时数据
    NEARBY_ENTITIES     ("infoNearbyEntities",     false,  ""),  // 附近实体数量
    CHUNK_ENTITIES      ("infoChunkEntities",      false,  ""),  // 当前区块实体
    UNDO_AVAILABLE      ("infoUndoAvailable",      false,  ""),  // 可撤销操作
    TASK_QUEUE          ("infoTaskQueue",          false,  ""),  // 任务队列状态
    
    // 配置信息
    ACTIVE_CONFIGS      ("infoActiveConfigs",      false,  ""),  // 激活的配置
    HOTKEYS_HELP        ("infoHotkeysHelp",        true,   "");  // 快捷键帮助

    public static final ImmutableList<InfoToggle> VALUES = ImmutableList.copyOf(values());
    
    private final String name;
    private final String comment;
    private final IKeybind keybind;
    private final boolean defaultValueBoolean;
    private final int integerValue;

    InfoToggle(String name, boolean defaultValue, String defaultHotkey) {
        this(name, defaultValue, defaultHotkey, KeybindSettings.DEFAULT);
    }

    InfoToggle(String name, boolean defaultValue, String defaultHotkey, KeybindSettings settings) {
        this.name = name;
        this.comment = name + ".comment";
        this.defaultValueBoolean = defaultValue;
        this.integerValue = defaultValue ? 1 : 0;
        this.keybind = KeybindMulti.fromStorageString(defaultHotkey, settings);
        this.keybind.setCallback(new KeyCallbackToggleBoolean(this));
    }

    @Override
    public ConfigType getType() {
        return ConfigType.HOTKEY;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getConfigGuiDisplayName() {
        String key = ArisSweepingMod.MODID + ".config.info_toggle." + this.name;
        return StringUtils.translate(key);
    }

    @Override
    public String getComment() {
        String key = ArisSweepingMod.MODID + ".config.info_toggle." + this.comment;
        return StringUtils.translate(key);
    }

    @Override
    public IKeybind getKeybind() {
        return this.keybind;
    }

    @Override
    public boolean getBooleanValue() {
        return this.integerValue != 0;
    }

    @Override
    public void setBooleanValue(boolean value) {
        this.setIntegerValue(value ? 1 : 0);
    }

    @Override
    public int getIntegerValue() {
        return this.integerValue;
    }

    @Override
    public void setIntegerValue(int value) {
        // 由于这是枚举，值实际上通过配置文件管理
        // 这里只是接口实现
    }

    @Override
    public int getDefaultIntegerValue() {
        return this.defaultValueBoolean ? 1 : 0;
    }
    
    @Override
    public int getMinIntegerValue() {
        return 0;
    }
    
    @Override
    public int getMaxIntegerValue() {
        return 1;
    }

    @Override
    public void resetToDefault() {
        this.setIntegerValue(this.getDefaultIntegerValue());
    }

    @Override
    public boolean isModified() {
        return this.integerValue != this.getDefaultIntegerValue();
    }

    @Override
    public boolean isModified(String newValue) {
        try {
            int value = Integer.parseInt(newValue);
            return value != this.getDefaultIntegerValue();
        } catch (NumberFormatException e) {
            return true;
        }
    }

    @Override
    public void setValueFromString(String value) {
        try {
            this.setIntegerValue(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            // Use a logger from the mod class or create a simple log
            System.err.println("Invalid integer value for " + this.name + ": " + value);
        }
    }

    @Override
    public String getStringValue() {
        return String.valueOf(this.integerValue);
    }

    @Override
    public String getDefaultStringValue() {
        return String.valueOf(this.getDefaultIntegerValue());
    }
    
    /**
     * 检查此信息项是否应该显示
     */
    public boolean shouldDisplay() {
        // 临时实现，稍后集成到配置系统
        return this.getBooleanValue();
    }
}