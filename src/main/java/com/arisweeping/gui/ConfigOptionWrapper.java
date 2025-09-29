package com.arisweeping.gui;

import java.util.ArrayList;
import java.util.List;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.config.options.ConfigString;

/**
 * 配置选项包装器
 * 基于 MiniHUD 的设计模式，用于适配不同类型的配置选项到GUI
 */
public class ConfigOptionWrapper {
    
    /**
     * 为配置选项列表创建包装器
     */
    public static List<ConfigOptionWrapper> createFor(List<IConfigBase> configs) {
        List<ConfigOptionWrapper> wrappers = new ArrayList<>();
        
        for (IConfigBase config : configs) {
            wrappers.add(new ConfigOptionWrapper(config));
        }
        
        return wrappers;
    }
    
    private final IConfigBase config;
    
    public ConfigOptionWrapper(IConfigBase config) {
        this.config = config;
    }
    
    public IConfigBase getConfig() {
        return this.config;
    }
    
    public String getName() {
        return this.config.getName();
    }
    
    public String getDisplayName() {
        return this.config.getPrettyName();
    }
    
    public String getComment() {
        return this.config.getComment();
    }
    
    /**
     * 检查配置类型并创建相应的GUI组件
     */
    public boolean isBoolean() {
        return this.config instanceof ConfigBoolean || 
               this.config instanceof ConfigBooleanHotkeyed;
    }
    
    public boolean isInteger() {
        return this.config instanceof ConfigInteger;
    }
    
    public boolean isDouble() {
        return this.config instanceof ConfigDouble;
    }
    
    public boolean isString() {
        return this.config instanceof ConfigString;
    }
    
    public boolean isOptionList() {
        return this.config instanceof ConfigOptionList;
    }
    
    public boolean isHotkey() {
        return this.config instanceof ConfigHotkey;
    }
}