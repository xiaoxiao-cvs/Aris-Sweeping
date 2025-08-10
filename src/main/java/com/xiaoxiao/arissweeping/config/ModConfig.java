package com.xiaoxiao.arissweeping.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

public class ModConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    
    // 清理间隔设置
    public static final IntValue CLEANUP_INTERVAL;
    
    // 实体清理设置
    public static final BooleanValue CLEANUP_ITEMS;
    public static final BooleanValue CLEANUP_EXPERIENCE_ORBS;
    public static final BooleanValue CLEANUP_ARROWS;
    public static final BooleanValue CLEANUP_HOSTILE_MOBS;
    public static final BooleanValue CLEANUP_PASSIVE_MOBS;
    
    // 清理阈值设置
    public static final IntValue MAX_ITEMS_PER_CHUNK;
    public static final IntValue MAX_ENTITIES_PER_CHUNK;
    public static final IntValue ITEM_AGE_THRESHOLD;
    
    // 性能设置
    public static final BooleanValue ASYNC_CLEANUP;
    public static final IntValue MAX_CHUNKS_PER_TICK;
    
    // 消息设置
    public static final BooleanValue BROADCAST_CLEANUP;
    public static final BooleanValue SHOW_CLEANUP_STATS;
    
    static {
        BUILDER.comment("Aris Sweeping Configuration").push("general");
        
        CLEANUP_INTERVAL = BUILDER
                .comment("清理间隔（秒）")
                .defineInRange("cleanupInterval", 300, 30, 3600);
        
        BUILDER.pop().push("entity_cleanup");
        
        CLEANUP_ITEMS = BUILDER
                .comment("是否清理掉落物")
                .define("cleanupItems", true);
        
        CLEANUP_EXPERIENCE_ORBS = BUILDER
                .comment("是否清理经验球")
                .define("cleanupExperienceOrbs", true);
        
        CLEANUP_ARROWS = BUILDER
                .comment("是否清理箭矢")
                .define("cleanupArrows", true);
        
        CLEANUP_HOSTILE_MOBS = BUILDER
                .comment("是否清理敌对生物")
                .define("cleanupHostileMobs", false);
        
        CLEANUP_PASSIVE_MOBS = BUILDER
                .comment("是否清理被动生物")
                .define("cleanupPassiveMobs", false);
        
        BUILDER.pop().push("thresholds");
        
        MAX_ITEMS_PER_CHUNK = BUILDER
                .comment("每个区块最大物品数量")
                .defineInRange("maxItemsPerChunk", 50, 10, 500);
        
        MAX_ENTITIES_PER_CHUNK = BUILDER
                .comment("每个区块最大实体数量")
                .defineInRange("maxEntitiesPerChunk", 100, 20, 1000);
        
        ITEM_AGE_THRESHOLD = BUILDER
                .comment("物品存在时间阈值（秒）")
                .defineInRange("itemAgeThreshold", 300, 60, 1800);
        
        BUILDER.pop().push("performance");
        
        ASYNC_CLEANUP = BUILDER
                .comment("是否使用异步清理")
                .define("asyncCleanup", true);
        
        MAX_CHUNKS_PER_TICK = BUILDER
                .comment("每tick处理的最大区块数")
                .defineInRange("maxChunksPerTick", 5, 1, 20);
        
        BUILDER.pop().push("messages");
        
        BROADCAST_CLEANUP = BUILDER
                .comment("是否广播清理消息")
                .define("broadcastCleanup", true);
        
        SHOW_CLEANUP_STATS = BUILDER
                .comment("是否显示清理统计")
                .define("showCleanupStats", true);
        
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}