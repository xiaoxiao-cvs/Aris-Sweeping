package com.arisweeping.config;package com.arisweeping.config;package com.arisweeping.config;package com.arisweeping.config;



import java.util.ArrayList;

import java.util.List;

import java.util.ArrayList;

import com.google.common.collect.ImmutableList;

import java.util.List;

import fi.dy.masa.malilib.config.IConfigHandler;

import fi.dy.masa.malilib.config.options.*;import java.util.List;import java.util.List;

import fi.dy.masa.malilib.hotkeys.IHotkey;

import fi.dy.masa.malilib.hotkeys.KeybindSettings;import com.google.common.collect.ImmutableList;

import fi.dy.masa.malilib.util.FileUtils;

import fi.dy.masa.malilib.interfaces.IConfigBase;import com.google.common.collect.ImmutableList;import com.google.common.collect.ImmutableList;

import fi.dy.masa.malilib.config.ConfigUtils;

import fi.dy.masa.malilib.config.IConfigHandler;

/**

 * ArisSweeping配置系统import fi.dy.masa.malilib.config.options.*;import fi.dy.masa.malilib.config.ConfigUtils;import fi.dy.masa.malilib.config.ConfigUtils;

 * 基于MiniHUD设计模式，使用malilib配置框架

 */import fi.dy.masa.malilib.hotkeys.IHotkey;

public class Configs implements IConfigHandler {

    import fi.dy.masa.malilib.hotkeys.KeybindSettings;import fi.dy.masa.malilib.config.IConfigHandler;import fi.dy.masa.malilib.config.IConfigHandler;

    public static final String CONFIG_FILE_NAME = "arisweeping.json";

    public static final int CONFIG_VERSION = 1;import fi.dy.masa.malilib.util.FileUtils;

    

    public static class General {import fi.dy.masa.malilib.util.JsonUtils;import fi.dy.masa.malilib.config.IConfigBase;import fi.dy.masa.malilib.config.IConfigBase;

        public static final String GENERAL_KEY = "General";

        import fi.dy.masa.malilib.interfaces.IConfigBase;

        public static final ConfigBooleanHotkeyed MAIN_TOGGLE = new ConfigBooleanHotkeyed("mainToggle", false, "K", KeybindSettings.GUI_DEFAULT);

        public static final ConfigHotkey CONFIG_GUI_HOTKEY = new ConfigHotkey("configGuiHotkey", "H,C", KeybindSettings.GUI_DEFAULT);import fi.dy.masa.malilib.config.ConfigUtils;import fi.dy.masa.malilib.config.options.*;import fi.dy.masa.malilib.config.options.*;

        public static final ConfigBoolean ENABLE_DEBUG_MODE = new ConfigBoolean("enableDebugMode", false);

        public static final ConfigBoolean SHOW_HUD = new ConfigBoolean("showHUD", true);

        public static final ConfigBoolean SHOW_STATISTICS = new ConfigBoolean("showStatistics", true);

        public static final ConfigInteger GUI_SCALE = new ConfigInteger("guiScale", 2, 1, 4);/**import fi.dy.masa.malilib.hotkeys.IHotkey;import fi.dy.masa.malilib.hotkeys.IHotkey;

        public static final ConfigOptionList HUD_ALIGNMENT = new ConfigOptionList("hudAlignment", HudAlignment.TOP_LEFT);

        public static final ConfigDouble FONT_SCALE = new ConfigDouble("fontScale", 1.0, 0.5, 2.0); * ArisSweeping配置系统

        public static final ConfigBoolean USE_FONT_SHADOW = new ConfigBoolean("useFontShadow", true);

        public static final ConfigBoolean USE_TEXT_BACKGROUND = new ConfigBoolean("useTextBackground", false); * 基于MiniHUD设计模式，使用malilib配置框架import fi.dy.masa.malilib.hotkeys.KeybindSettings;import fi.dy.masa.malilib.hotkeys.KeybindSettings;

        

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of( */

            MAIN_TOGGLE,

            CONFIG_GUI_HOTKEY,public class Configs implements IConfigHandler {import fi.dy.masa.malilib.util.FileUtils;import fi.dy.masa.malilib.util.FileUtils;

            ENABLE_DEBUG_MODE,

            SHOW_HUD,    

            SHOW_STATISTICS,

            GUI_SCALE,    public static final String CONFIG_FILE_NAME = "arisweeping.json";import fi.dy.masa.malilib.util.JsonUtils;import fi.dy.masa.malilib.util.JsonUtils;

            HUD_ALIGNMENT,

            FONT_SCALE,    public static final int CONFIG_VERSION = 1;

            USE_FONT_SHADOW,

            USE_TEXT_BACKGROUND    import com.arisweeping.core.ArisLogger;import com.arisweeping.core.ArisLogger;

        );

    }    public static class General {

    

    public static class ItemCleaning {        public static final String GENERAL_KEY = "General";import com.arisweeping.core.ArisSweepingMod;import com.arisweeping.core.ArisSweepingMod;

        public static final String ITEM_CLEANING_KEY = "ItemCleaning";

                

        public static final ConfigBoolean ENABLED = new ConfigBoolean("enabled", true);

        public static final ConfigString STRATEGY = new ConfigString("strategy", "age_based");        public static final ConfigBooleanHotkeyed MAIN_TOGGLE = new ConfigBooleanHotkeyed("mainToggle", false, "K", KeybindSettings.GUI_DEFAULT).apply(GENERAL_KEY);

        public static final ConfigInteger ITEM_LIFETIME_SECONDS = new ConfigInteger("itemLifetimeSeconds", 300, 60, 1200);

        public static final ConfigInteger CLEANING_INTERVAL_TICKS = new ConfigInteger("cleaningIntervalTicks", 1200, 100, 6000);        public static final ConfigHotkey CONFIG_GUI_HOTKEY = new ConfigHotkey("configGuiHotkey", "H,C", KeybindSettings.GUI_DEFAULT).apply(GENERAL_KEY);

        public static final ConfigInteger CHUNK_RANGE = new ConfigInteger("chunkRange", 8, 1, 16);

        public static final ConfigDouble CLEANING_RADIUS = new ConfigDouble("cleaningRadius", 64.0, 16.0, 128.0);        public static final ConfigBoolean ENABLE_DEBUG_MODE = new ConfigBoolean("enableDebugMode", false).apply(GENERAL_KEY);/**/**

        public static final ConfigBoolean PROTECT_VALUABLE_ITEMS = new ConfigBoolean("protectValuableItems", true);

                public static final ConfigBoolean SHOW_HUD = new ConfigBoolean("showHUD", true).apply(GENERAL_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(

            ENABLED,        public static final ConfigBoolean SHOW_STATISTICS = new ConfigBoolean("showStatistics", true).apply(GENERAL_KEY); * ArisSweeping MaFgLib 配置系统 * ArisSweeping MaFgLib 配置系统

            STRATEGY,

            ITEM_LIFETIME_SECONDS,        public static final ConfigInteger GUI_SCALE = new ConfigInteger("guiScale", 2, 1, 4).apply(GENERAL_KEY);

            CLEANING_INTERVAL_TICKS,

            CHUNK_RANGE,        public static final ConfigOptionList HUD_ALIGNMENT = new ConfigOptionList("hudAlignment", HudAlignment.TOP_LEFT).apply(GENERAL_KEY); * 基于 MiniHUD 的设计模式，使用 malilib 配置框架 * 基于 MiniHUD 的设计模式，使用 malilib 配置框架

            CLEANING_RADIUS,

            PROTECT_VALUABLE_ITEMS        public static final ConfigDouble FONT_SCALE = new ConfigDouble("fontScale", 1.0, 0.5, 2.0).apply(GENERAL_KEY);

        );

    }        public static final ConfigBoolean USE_FONT_SHADOW = new ConfigBoolean("useFontShadow", true).apply(GENERAL_KEY); */ */

    

    public static class EntityCleaning {        public static final ConfigBoolean USE_TEXT_BACKGROUND = new ConfigBoolean("useTextBackground", false).apply(GENERAL_KEY);

        public static final String ENTITY_CLEANING_KEY = "EntityCleaning";

                public class Configs implements IConfigHandler {public class Configs implements IConfigHandler {

        public static final ConfigBoolean ENABLED = new ConfigBoolean("enabled", false);
        public static final ConfigString STRATEGY = new ConfigString("strategy", "density_based");        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(

        public static final ConfigInteger MAX_ANIMALS_PER_CHUNK = new ConfigInteger("maxAnimalsPerChunk", 20, 5, 50);

        public static final ConfigDouble DENSITY_RADIUS = new ConfigDouble("densityRadius", 32.0, 16.0, 64.0);            MAIN_TOGGLE,        

        public static final ConfigBoolean PROTECT_BREEDING = new ConfigBoolean("protectBreeding", true);

        public static final ConfigBoolean PROTECT_BABIES = new ConfigBoolean("protectBabies", true);            CONFIG_GUI_HOTKEY,

        public static final ConfigBoolean PROTECT_NAMED = new ConfigBoolean("protectNamed", true);

                    ENABLE_DEBUG_MODE,    private static final String CONFIG_FILE_NAME = ArisSweepingMod.MODID + ".json";    private static final String CONFIG_FILE_NAME = ArisSweepingMod.MODID + ".json";

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(

            ENABLED,            SHOW_HUD,

            STRATEGY,

            MAX_ANIMALS_PER_CHUNK,            SHOW_STATISTICS,    private static final int CONFIG_VERSION = 1;    private static final int CONFIG_VERSION = 1;

            DENSITY_RADIUS,

            PROTECT_BREEDING,            GUI_SCALE,

            PROTECT_BABIES,

            PROTECT_NAMED            HUD_ALIGNMENT,        

        );

    }            FONT_SCALE,

    

    public static class Performance {            USE_FONT_SHADOW,    // 配置分类键    // 配置分类键

        public static final String PERFORMANCE_KEY = "Performance";

                    USE_TEXT_BACKGROUND

        public static final ConfigInteger THREAD_POOL_SIZE = new ConfigInteger("threadPoolSize", 2, 1, 8);

        public static final ConfigInteger MEMORY_THRESHOLD_MB = new ConfigInteger("memoryThresholdMB", 512, 128, 2048);        );    private static final String GENERAL_KEY = ArisSweepingMod.MODID + ".config.general";    private static final String GENERAL_KEY = ArisSweepingMod.MODID + ".config.general";

        public static final ConfigInteger MAX_UNDO_OPERATIONS = new ConfigInteger("maxUndoOperations", 10, 5, 50);

        public static final ConfigInteger UNDO_TIMEOUT_MINUTES = new ConfigInteger("undoTimeoutMinutes", 30, 5, 120);    }

        

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(        private static final String ITEM_CLEANING_KEY = ArisSweepingMod.MODID + ".config.item_cleaning";    private static final String ITEM_CLEANING_KEY = ArisSweepingMod.MODID + ".config.item_cleaning";

            THREAD_POOL_SIZE,

            MEMORY_THRESHOLD_MB,    public static class ItemCleaning {

            MAX_UNDO_OPERATIONS,

            UNDO_TIMEOUT_MINUTES        public static final String ITEM_CLEANING_KEY = "ItemCleaning";    private static final String ENTITY_CLEANING_KEY = ArisSweepingMod.MODID + ".config.entity_cleaning";    private static final String ENTITY_CLEANING_KEY = ArisSweepingMod.MODID + ".config.entity_cleaning";

        );

    }        

    

    public static final List<IConfigBase> ALL_OPTIONS = createAllOptionsList();        public static final ConfigBoolean ENABLED = new ConfigBoolean("enabled", true).apply(ITEM_CLEANING_KEY);    private static final String PERFORMANCE_KEY = ArisSweepingMod.MODID + ".config.performance";    private static final String PERFORMANCE_KEY = ArisSweepingMod.MODID + ".config.performance";

    

    private static List<IConfigBase> createAllOptionsList() {        public static final ConfigString STRATEGY = new ConfigString("strategy", "age_based").apply(ITEM_CLEANING_KEY);

        List<IConfigBase> options = new ArrayList<>();

        options.addAll(General.OPTIONS);        public static final ConfigInteger ITEM_LIFETIME_SECONDS = new ConfigInteger("itemLifetimeSeconds", 300, 60, 1200).apply(ITEM_CLEANING_KEY);        

        options.addAll(ItemCleaning.OPTIONS);

        options.addAll(EntityCleaning.OPTIONS);        public static final ConfigInteger CLEANING_INTERVAL_TICKS = new ConfigInteger("cleaningIntervalTicks", 1200, 100, 6000).apply(ITEM_CLEANING_KEY);

        options.addAll(Performance.OPTIONS);

        return options;        public static final ConfigInteger CHUNK_RANGE = new ConfigInteger("chunkRange", 8, 1, 16).apply(ITEM_CLEANING_KEY);    /**    /**

    }

            public static final ConfigDouble CLEANING_RADIUS = new ConfigDouble("cleaningRadius", 64.0, 16.0, 128.0).apply(ITEM_CLEANING_KEY);

    public static List<IHotkey> getAllHotkeys() {

        return ImmutableList.of(        public static final ConfigBoolean PROTECT_VALUABLE_ITEMS = new ConfigBoolean("protectValuableItems", true).apply(ITEM_CLEANING_KEY);     * 通用配置选项     * 通用配置选项

            General.MAIN_TOGGLE.getKeybind(),

            General.CONFIG_GUI_HOTKEY        

        );

    }        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(     */     */

    

    @Override            ENABLED,

    public void load() {

        ConfigUtils.readConfigBase(            STRATEGY,    public static class General {    public static class General {

            FileUtils.getConfigDirectory().toPath().resolve(CONFIG_FILE_NAME),

            CONFIG_VERSION,            ITEM_LIFETIME_SECONDS,

            ALL_OPTIONS

        );            CLEANING_INTERVAL_TICKS,        // 主开关和快捷键        // 基本设置

    }

                CHUNK_RANGE,

    @Override

    public void save() {            CLEANING_RADIUS,        public static final ConfigBooleanHotkeyed MAIN_TOGGLE = new ConfigBooleanHotkeyed("mainToggle", true, "K").apply(GENERAL_KEY);        public static final ConfigBoolean ENABLE_DEBUG_MODE = new ConfigBoolean("enableDebugMode", false).apply(GENERAL_KEY);

        ConfigUtils.writeConfigBase(

            FileUtils.getConfigDirectory().toPath().resolve(CONFIG_FILE_NAME),            PROTECT_VALUABLE_ITEMS

            CONFIG_VERSION,

            ALL_OPTIONS        );        public static final ConfigHotkey CONFIG_GUI_HOTKEY = new ConfigHotkey("configGuiHotkey", "H,C").apply(GENERAL_KEY);        public static final ConfigBoolean SHOW_HUD = new ConfigBoolean("showHUD", true).apply(GENERAL_KEY);

        );

    }    }

}
                    public static final ConfigBoolean SHOW_STATISTICS = new ConfigBoolean("showStatistics", true).apply(GENERAL_KEY);

    public static class EntityCleaning {

        public static final String ENTITY_CLEANING_KEY = "EntityCleaning";        // 基本设置        public static final ConfigInteger GUI_SCALE = new ConfigInteger("guiScale", 1, 1, 4).apply(GENERAL_KEY);

        

        public static final ConfigBoolean ENABLED = new ConfigBoolean("enabled", false).apply(ENTITY_CLEANING_KEY);        public static final ConfigBoolean ENABLE_DEBUG_MODE = new ConfigBoolean("enableDebugMode", false).apply(GENERAL_KEY);        

        public static final ConfigString STRATEGY = new ConfigString("strategy", "density_based").apply(ENTITY_CLEANING_KEY);

        public static final ConfigInteger MAX_ANIMALS_PER_CHUNK = new ConfigInteger("maxAnimalsPerChunk", 20, 5, 50).apply(ENTITY_CLEANING_KEY);        public static final ConfigBoolean SHOW_HUD = new ConfigBoolean("showHUD", true).apply(GENERAL_KEY);        // HUD配置选项

        public static final ConfigDouble DENSITY_RADIUS = new ConfigDouble("densityRadius", 32.0, 16.0, 64.0).apply(ENTITY_CLEANING_KEY);

        public static final ConfigBoolean PROTECT_BREEDING = new ConfigBoolean("protectBreeding", true).apply(ENTITY_CLEANING_KEY);        public static final ConfigBoolean SHOW_STATISTICS = new ConfigBoolean("showStatistics", true).apply(GENERAL_KEY);        public static final ConfigOptionList HUD_ALIGNMENT = new ConfigOptionList("hudAlignment", HudAlignment.TOP_LEFT).apply(GENERAL_KEY);

        public static final ConfigBoolean PROTECT_BABIES = new ConfigBoolean("protectBabies", true).apply(ENTITY_CLEANING_KEY);

        public static final ConfigBoolean PROTECT_NAMED = new ConfigBoolean("protectNamed", true).apply(ENTITY_CLEANING_KEY);        public static final ConfigInteger GUI_SCALE = new ConfigInteger("guiScale", 1, 1, 4).apply(GENERAL_KEY);        public static final ConfigDouble FONT_SCALE = new ConfigDouble("fontScale", 1.0, 0.5, 2.0).apply(GENERAL_KEY);

        

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(                public static final ConfigBoolean USE_FONT_SHADOW = new ConfigBoolean("useFontShadow", true).apply(GENERAL_KEY);

            ENABLED,

            STRATEGY,        // HUD配置选项        public static final ConfigBoolean USE_TEXT_BACKGROUND = new ConfigBoolean("useTextBackground", true).apply(GENERAL_KEY);

            MAX_ANIMALS_PER_CHUNK,

            DENSITY_RADIUS,        public static final ConfigOptionList HUD_ALIGNMENT = new ConfigOptionList("hudAlignment", HudAlignment.TOP_LEFT).apply(GENERAL_KEY);        

            PROTECT_BREEDING,

            PROTECT_BABIES,        public static final ConfigDouble FONT_SCALE = new ConfigDouble("fontScale", 1.0, 0.5, 2.0).apply(GENERAL_KEY);        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(

            PROTECT_NAMED

        );        public static final ConfigBoolean USE_FONT_SHADOW = new ConfigBoolean("useFontShadow", true).apply(GENERAL_KEY);            MAIN_TOGGLE,

    }

            public static final ConfigBoolean USE_TEXT_BACKGROUND = new ConfigBoolean("useTextBackground", true).apply(GENERAL_KEY);            CONFIG_GUI_HOTKEY,

    public static class Performance {

        public static final String PERFORMANCE_KEY = "Performance";                    ENABLE_DEBUG_MODE,

        

        public static final ConfigInteger THREAD_POOL_SIZE = new ConfigInteger("threadPoolSize", 2, 1, 8).apply(PERFORMANCE_KEY);        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(            SHOW_HUD,

        public static final ConfigInteger MEMORY_THRESHOLD_MB = new ConfigInteger("memoryThresholdMB", 512, 128, 2048).apply(PERFORMANCE_KEY);

        public static final ConfigInteger MAX_UNDO_OPERATIONS = new ConfigInteger("maxUndoOperations", 10, 5, 50).apply(PERFORMANCE_KEY);            MAIN_TOGGLE,            SHOW_STATISTICS,

        public static final ConfigInteger UNDO_TIMEOUT_MINUTES = new ConfigInteger("undoTimeoutMinutes", 30, 5, 120).apply(PERFORMANCE_KEY);

                    CONFIG_GUI_HOTKEY,            GUI_SCALE,

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(

            THREAD_POOL_SIZE,            ENABLE_DEBUG_MODE,            HUD_ALIGNMENT,

            MEMORY_THRESHOLD_MB,

            MAX_UNDO_OPERATIONS,            SHOW_HUD,            FONT_SCALE,

            UNDO_TIMEOUT_MINUTES

        );            SHOW_STATISTICS,            USE_FONT_SHADOW,

    }

                GUI_SCALE,            USE_TEXT_BACKGROUND

    public static final List<IConfigBase> ALL_OPTIONS = createAllOptionsList();

                HUD_ALIGNMENT,        );

    private static List<IConfigBase> createAllOptionsList() {

        List<IConfigBase> options = new ArrayList<>();            FONT_SCALE,    }

        options.addAll(General.OPTIONS);

        options.addAll(ItemCleaning.OPTIONS);            USE_FONT_SHADOW,    

        options.addAll(EntityCleaning.OPTIONS);

        options.addAll(Performance.OPTIONS);            USE_TEXT_BACKGROUND    /**

        return options;

    }        );     * 物品清理配置选项

    

    public static List<IHotkey> getAllHotkeys() {    }     */

        return ImmutableList.of(

            General.MAIN_TOGGLE.getKeybind(),        public static class ItemCleaning {

            General.CONFIG_GUI_HOTKEY

        );    /**        // 基本开关

    }

         * 物品清理配置选项        public static final ConfigBoolean ENABLED = new ConfigBoolean("enabled", true).apply(ITEM_CLEANING_KEY);

    @Override

    public void load() {     */        public static final ConfigString STRATEGY = new ConfigString("strategy", "age_based").apply(ITEM_CLEANING_KEY);

        ConfigUtils.readConfigBase(

            FileUtils.getConfigDirectory().toPath().resolve(CONFIG_FILE_NAME),    public static class ItemCleaning {        

            CONFIG_VERSION,

            ALL_OPTIONS        // 基本开关        // 时间和间隔设置

        );

    }        public static final ConfigBoolean ENABLED = new ConfigBoolean("enabled", true).apply(ITEM_CLEANING_KEY);        public static final ConfigInteger ITEM_LIFETIME_SECONDS = new ConfigInteger("itemLifetimeSeconds", 300, 60, 1200).apply(ITEM_CLEANING_KEY);

    

    @Override        public static final ConfigString STRATEGY = new ConfigString("strategy", "age_based").apply(ITEM_CLEANING_KEY);        public static final ConfigInteger CLEANING_INTERVAL_TICKS = new ConfigInteger("cleaningIntervalTicks", 600, 100, 6000).apply(ITEM_CLEANING_KEY);

    public void save() {

        ConfigUtils.writeConfigBase(                

            FileUtils.getConfigDirectory().toPath().resolve(CONFIG_FILE_NAME),

            CONFIG_VERSION,        // 时间和间隔设置        // 范围和数量限制

            ALL_OPTIONS

        );        public static final ConfigInteger ITEM_LIFETIME_SECONDS = new ConfigInteger("itemLifetimeSeconds", 300, 60, 1200).apply(ITEM_CLEANING_KEY);        public static final ConfigInteger CHUNK_RANGE = new ConfigInteger("chunkRange", 3, 1, 16).apply(ITEM_CLEANING_KEY);

    }

}        public static final ConfigInteger CLEANING_INTERVAL_TICKS = new ConfigInteger("cleaningIntervalTicks", 600, 100, 6000).apply(ITEM_CLEANING_KEY);        public static final ConfigInteger MIN_ITEM_COUNT = new ConfigInteger("minItemCount", 10, 1, 100).apply(ITEM_CLEANING_KEY);

                public static final ConfigDouble CLEANING_RADIUS = new ConfigDouble("cleaningRadius", 64.0, 16.0, 128.0).apply(ITEM_CLEANING_KEY);

        // 范围和数量限制        

        public static final ConfigInteger CHUNK_RANGE = new ConfigInteger("chunkRange", 3, 1, 16).apply(ITEM_CLEANING_KEY);        // 保护设置

        public static final ConfigInteger MIN_ITEM_COUNT = new ConfigInteger("minItemCount", 10, 1, 100).apply(ITEM_CLEANING_KEY);        public static final ConfigBoolean RESPECT_WHITELIST = new ConfigBoolean("respectWhitelist", false).apply(ITEM_CLEANING_KEY);

        public static final ConfigDouble CLEANING_RADIUS = new ConfigDouble("cleaningRadius", 64.0, 16.0, 128.0).apply(ITEM_CLEANING_KEY);        public static final ConfigBoolean PROTECT_VALUABLE_ITEMS = new ConfigBoolean("protectValuableItems", true).apply(ITEM_CLEANING_KEY);

                

        // 保护设置        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(

        public static final ConfigBoolean RESPECT_WHITELIST = new ConfigBoolean("respectWhitelist", false).apply(ITEM_CLEANING_KEY);            ENABLED,

        public static final ConfigBoolean PROTECT_VALUABLE_ITEMS = new ConfigBoolean("protectValuableItems", true).apply(ITEM_CLEANING_KEY);            STRATEGY,

                    ITEM_LIFETIME_SECONDS,

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(            CLEANING_INTERVAL_TICKS,

            ENABLED,            CHUNK_RANGE,

            STRATEGY,            MIN_ITEM_COUNT,

            ITEM_LIFETIME_SECONDS,            CLEANING_RADIUS,

            CLEANING_INTERVAL_TICKS,            RESPECT_WHITELIST,

            CHUNK_RANGE,            PROTECT_VALUABLE_ITEMS

            MIN_ITEM_COUNT,        );

            CLEANING_RADIUS,    }

            RESPECT_WHITELIST,    

            PROTECT_VALUABLE_ITEMS    /**

        );     * 实体清理配置选项

    }     */

        public static class EntityCleaning {

    /**        // 基本开关

     * 实体清理配置选项        public static final ConfigBoolean ENABLED = new ConfigBoolean("enabled", false).apply(ENTITY_CLEANING_KEY);

     */        public static final ConfigString STRATEGY = new ConfigString("strategy", "density_based").apply(ENTITY_CLEANING_KEY);

    public static class EntityCleaning {        

        // 基本开关        // 密度控制

        public static final ConfigBoolean ENABLED = new ConfigBoolean("enabled", false).apply(ENTITY_CLEANING_KEY);        public static final ConfigInteger MAX_ANIMALS_PER_CHUNK = new ConfigInteger("maxAnimalsPerChunk", 20, 5, 50).apply(ENTITY_CLEANING_KEY);

        public static final ConfigString STRATEGY = new ConfigString("strategy", "density_based").apply(ENTITY_CLEANING_KEY);        public static final ConfigInteger CHECK_RADIUS = new ConfigInteger("checkRadius", 5, 1, 16).apply(ENTITY_CLEANING_KEY);

                public static final ConfigDouble DENSITY_RADIUS = new ConfigDouble("densityRadius", 32.0, 16.0, 64.0).apply(ENTITY_CLEANING_KEY);

        // 密度和范围设置        

        public static final ConfigInteger MAX_ANIMALS_PER_CHUNK = new ConfigInteger("maxAnimalsPerChunk", 20, 5, 50).apply(ENTITY_CLEANING_KEY);        // 保护设置

        public static final ConfigInteger CHECK_RADIUS = new ConfigInteger("checkRadius", 5, 1, 16).apply(ENTITY_CLEANING_KEY);        public static final ConfigBoolean PROTECT_BREEDING = new ConfigBoolean("protectBreeding", true).apply(ENTITY_CLEANING_KEY);

        public static final ConfigDouble DENSITY_RADIUS = new ConfigDouble("densityRadius", 32.0, 16.0, 64.0).apply(ENTITY_CLEANING_KEY);        public static final ConfigBoolean PROTECT_BABIES = new ConfigBoolean("protectBabies", true).apply(ENTITY_CLEANING_KEY);

                public static final ConfigBoolean PROTECT_NAMED = new ConfigBoolean("protectNamed", true).apply(ENTITY_CLEANING_KEY);

        // 保护设置        

        public static final ConfigBoolean PROTECT_BREEDING = new ConfigBoolean("protectBreeding", true).apply(ENTITY_CLEANING_KEY);        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(

        public static final ConfigBoolean PROTECT_BABIES = new ConfigBoolean("protectBabies", true).apply(ENTITY_CLEANING_KEY);            ENABLED,

        public static final ConfigBoolean PROTECT_NAMED = new ConfigBoolean("protectNamed", true).apply(ENTITY_CLEANING_KEY);            STRATEGY,

                    MAX_ANIMALS_PER_CHUNK,

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(            CHECK_RADIUS,

            ENABLED,            DENSITY_RADIUS,

            STRATEGY,            PROTECT_BREEDING,

            MAX_ANIMALS_PER_CHUNK,            PROTECT_BABIES,

            CHECK_RADIUS,            PROTECT_NAMED

            DENSITY_RADIUS,        );

            PROTECT_BREEDING,    }

            PROTECT_BABIES,    

            PROTECT_NAMED    /**

        );     * 性能配置选项

    }     */

        public static class Performance {

    /**        // 线程池设置

     * 性能配置选项        public static final ConfigInteger THREAD_POOL_SIZE = new ConfigInteger("threadPoolSize", 2, 1, 8).apply(PERFORMANCE_KEY);

     */        public static final ConfigBoolean ENABLE_MONITORING = new ConfigBoolean("enableMonitoring", true).apply(PERFORMANCE_KEY);

    public static class Performance {        

        // 线程和内存设置        // 内存和处理限制

        public static final ConfigInteger THREAD_POOL_SIZE = new ConfigInteger("threadPoolSize", 2, 1, 8).apply(PERFORMANCE_KEY);        public static final ConfigInteger MEMORY_THRESHOLD_MB = new ConfigInteger("memoryThresholdMB", 512, 128, 2048).apply(PERFORMANCE_KEY);

        public static final ConfigBoolean ENABLE_MONITORING = new ConfigBoolean("enableMonitoring", true).apply(PERFORMANCE_KEY);        public static final ConfigInteger MAX_UNDO_OPERATIONS = new ConfigInteger("maxUndoOperations", 10, 5, 50).apply(PERFORMANCE_KEY);

        public static final ConfigInteger MEMORY_THRESHOLD_MB = new ConfigInteger("memoryThresholdMB", 512, 128, 2048).apply(PERFORMANCE_KEY);        public static final ConfigInteger UNDO_TIMEOUT_MINUTES = new ConfigInteger("undoTimeoutMinutes", 30, 5, 120).apply(PERFORMANCE_KEY);

        public static final ConfigInteger MAX_UNDO_OPERATIONS = new ConfigInteger("maxUndoOperations", 10, 5, 50).apply(PERFORMANCE_KEY);        

        public static final ConfigInteger UNDO_TIMEOUT_MINUTES = new ConfigInteger("undoTimeoutMinutes", 30, 5, 120).apply(PERFORMANCE_KEY);        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(

                    THREAD_POOL_SIZE,

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(            ENABLE_MONITORING,

            THREAD_POOL_SIZE,            MEMORY_THRESHOLD_MB,

            ENABLE_MONITORING,            MAX_UNDO_OPERATIONS,

            MEMORY_THRESHOLD_MB,            UNDO_TIMEOUT_MINUTES

            MAX_UNDO_OPERATIONS,        );

            UNDO_TIMEOUT_MINUTES    }

        );    

    }    /**

     * 获取所有配置选项列表

    @Override     */

    public void load() {    public static List<IConfigBase> getAllOptions() {

        ConfigUtils.readConfigBase(FileUtils.getConfigDirectory().toPath().resolve(CONFIG_FILE_NAME), CONFIG_VERSION, getAllOptions());        List<IConfigBase> options = new java.util.ArrayList<>();

    }        options.addAll(General.OPTIONS);

        options.addAll(ItemCleaning.OPTIONS);

    @Override        options.addAll(EntityCleaning.OPTIONS);

    public void save() {        options.addAll(Performance.OPTIONS);

        ConfigUtils.writeConfigBase(FileUtils.getConfigDirectory().toPath().resolve(CONFIG_FILE_NAME), CONFIG_VERSION, getAllOptions());        return options;

    }    }

    

    @Override    /**

    public List<IHotkey> getAllHotkeys() {     * 获取所有热键配置

        return ImmutableList.of(     */

            General.MAIN_TOGGLE,    public static List<IHotkey> getAllHotkeys() {

            General.CONFIG_GUI_HOTKEY        return ImmutableList.of(

        );            General.MAIN_TOGGLE,

    }            General.CONFIG_GUI_HOTKEY

            );

    /**    }

     * 获取所有配置选项    

     */    @Override

    public static List<IConfigBase> getAllOptions() {    public void load() {

        ImmutableList.Builder<IConfigBase> builder = ImmutableList.builder();        try {

        builder.addAll(General.OPTIONS);            FileUtils.createDirectoriesIfMissing(FileUtils.getConfigDirectory());

        builder.addAll(ItemCleaning.OPTIONS);            ConfigUtils.readConfigBase(FileUtils.getConfigDirectory().toPath().resolve(CONFIG_FILE_NAME), CONFIG_VERSION, getAllOptions());

        builder.addAll(EntityCleaning.OPTIONS);            ArisLogger.info("配置已加载: " + CONFIG_FILE_NAME);

        builder.addAll(Performance.OPTIONS);        } catch (Exception e) {

        return builder.build();            ArisLogger.error("加载配置失败: " + e.getMessage());

    }        }

}    }
    
    @Override
    public void save() {
        try {
            ConfigUtils.writeConfigBase(FileUtils.getConfigDirectory().toPath().resolve(CONFIG_FILE_NAME), CONFIG_VERSION, getAllOptions());
            ArisLogger.info("配置已保存: " + CONFIG_FILE_NAME);
        } catch (Exception e) {
            ArisLogger.error("保存配置失败: " + e.getMessage());
        }
    }
    
    @Override
    public void onConfigsChanged() {
        // 配置更改时的回调处理
        ArisLogger.debug("配置已更改");
        save();
    }
}