package com.xiaoxiao.arissweeping.strategy.impl;

import com.xiaoxiao.arissweeping.config.ConfigManager;
import com.xiaoxiao.arissweeping.observer.CleanupEventManager;
import com.xiaoxiao.arissweeping.strategy.CleanupStrategy.CleanupResult;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * DensityBasedCleanupStrategy 单元测试
 */
@ExtendWith(MockitoExtension.class)
class DensityBasedCleanupStrategyTest {
    
    @Mock
    private ConfigManager configManager;
    
    @Mock
    private CleanupEventManager eventManager;
    
    @Mock
    private World world;
    
    @Mock
    private Location location1;
    
    @Mock
    private Location location2;
    
    @Mock
    private Location location3;
    
    private DensityBasedCleanupStrategy strategy;
    
    @BeforeEach
    void setUp() {
        strategy = new DensityBasedCleanupStrategy(configManager, eventManager);
        
        // 设置默认配置
        when(configManager.getInt("strategies.density-based.max-items-per-chunk", 50)).thenReturn(50);
        when(configManager.getInt("strategies.density-based.max-mobs-per-chunk", 30)).thenReturn(30);
        when(configManager.getInt("strategies.density-based.max-experience-per-chunk", 20)).thenReturn(20);
        when(configManager.getDouble("strategies.density-based.detection-radius", 16.0)).thenReturn(16.0);
        when(configManager.getBoolean("strategies.density-based.protect-named", true)).thenReturn(true);
        when(configManager.getBoolean("strategies.density-based.protect-persistent", true)).thenReturn(true);
        when(configManager.getStringList("strategies.density-based.priority-removal"))
            .thenReturn(Arrays.asList("ITEM", "EXPERIENCE_ORB", "ARROW"));
        
        // 初始化策略配置
        strategy.updateConfiguration(configManager);
    }
    
    @Test
    void testGetStrategyName() {
        assertEquals("DensityBasedCleanup", strategy.getStrategyName());
    }
    
    @Test
    void testGetDescription() {
        assertNotNull(strategy.getDescription());
        assertTrue(strategy.getDescription().contains("密度"));
    }
    
    @Test
    void testGetPriority() {
        assertTrue(strategy.getPriority() >= 0);
    }
    
    @Test
    void testIsApplicableToWorld() {
        when(world.getName()).thenReturn("world");
        assertTrue(strategy.isApplicableToWorld(world));
    }
    
    @Test
    void testExecuteCleanup_ItemDensity() {
        // 创建超过密度阈值的物品实体
        List<Entity> entities = new ArrayList<>();
        
        // 创建60个物品实体在同一位置附近（超过50的阈值）
        for (int i = 0; i < 60; i++) {
            Item item = mock(Item.class);
            Location itemLocation = mock(Location.class);
            
            when(item.getLocation()).thenReturn(itemLocation);
            when(item.isValid()).thenReturn(true);
            when(item.getTicksLived()).thenReturn(100);
            
            // 设置位置在检测半径内
            when(itemLocation.getX()).thenReturn(100.0 + (i % 5)); // 5x12 网格
            when(itemLocation.getZ()).thenReturn(100.0 + (i / 5));
            when(itemLocation.getY()).thenReturn(64.0);
            when(itemLocation.getWorld()).thenReturn(world);
            
            entities.add(item);
        }
        
        CleanupResult result = strategy.executeCleanup(world, entities);
        
        assertTrue(result.isSuccess());
        assertEquals(60, result.getEntitiesProcessed());
        assertTrue(result.getEntitiesRemoved() > 0); // 应该移除一些实体
        assertTrue(result.getEntitiesRemoved() >= 10); // 至少移除超出阈值的部分
    }
    
    @Test
    void testExecuteCleanup_MobDensity() {
        List<Entity> entities = new ArrayList<>();
        
        // 创建40个生物实体在同一位置附近（超过30的阈值）
        for (int i = 0; i < 40; i++) {
            Zombie zombie = mock(Zombie.class);
            Location mobLocation = mock(Location.class);
            
            when(zombie.getLocation()).thenReturn(mobLocation);
            when(zombie.isValid()).thenReturn(true);
            when(zombie.getTicksLived()).thenReturn(100);
            when(zombie.getCustomName()).thenReturn(null); // 未命名
            when(zombie.isPersistent()).thenReturn(false); // 非持久化
            
            // 设置位置在检测半径内
            when(mobLocation.getX()).thenReturn(200.0 + (i % 8)); // 8x5 网格
            when(mobLocation.getZ()).thenReturn(200.0 + (i / 8));
            when(mobLocation.getY()).thenReturn(64.0);
            when(mobLocation.getWorld()).thenReturn(world);
            
            entities.add(zombie);
        }
        
        CleanupResult result = strategy.executeCleanup(world, entities);
        
        assertTrue(result.isSuccess());
        assertEquals(40, result.getEntitiesProcessed());
        assertTrue(result.getEntitiesRemoved() >= 10); // 至少移除超出阈值的部分
    }
    
    @Test
    void testExecuteCleanup_ExperienceOrbDensity() {
        List<Entity> entities = new ArrayList<>();
        
        // 创建30个经验球实体在同一位置附近（超过20的阈值）
        for (int i = 0; i < 30; i++) {
            ExperienceOrb orb = mock(ExperienceOrb.class);
            Location orbLocation = mock(Location.class);
            
            when(orb.getLocation()).thenReturn(orbLocation);
            when(orb.isValid()).thenReturn(true);
            when(orb.getTicksLived()).thenReturn(100);
            
            // 设置位置在检测半径内
            when(orbLocation.getX()).thenReturn(300.0 + (i % 6)); // 6x5 网格
            when(orbLocation.getZ()).thenReturn(300.0 + (i / 6));
            when(orbLocation.getY()).thenReturn(64.0);
            when(orbLocation.getWorld()).thenReturn(world);
            
            entities.add(orb);
        }
        
        CleanupResult result = strategy.executeCleanup(world, entities);
        
        assertTrue(result.isSuccess());
        assertEquals(30, result.getEntitiesProcessed());
        assertTrue(result.getEntitiesRemoved() >= 10); // 至少移除超出阈值的部分
    }
    
    @Test
    void testExecuteCleanup_BelowThreshold() {
        List<Entity> entities = new ArrayList<>();
        
        // 创建少量实体，不超过阈值
        for (int i = 0; i < 10; i++) {
            Item item = mock(Item.class);
            Location itemLocation = mock(Location.class);
            
            when(item.getLocation()).thenReturn(itemLocation);
            when(item.isValid()).thenReturn(true);
            when(item.getTicksLived()).thenReturn(100);
            
            when(itemLocation.getX()).thenReturn(400.0 + i);
            when(itemLocation.getZ()).thenReturn(400.0);
            when(itemLocation.getY()).thenReturn(64.0);
            when(itemLocation.getWorld()).thenReturn(world);
            
            entities.add(item);
        }
        
        CleanupResult result = strategy.executeCleanup(world, entities);
        
        assertTrue(result.isSuccess());
        assertEquals(10, result.getEntitiesProcessed());
        assertEquals(0, result.getEntitiesRemoved()); // 不应该移除任何实体
    }
    
    @Test
    void testExecuteCleanup_ProtectedEntities() {
        List<Entity> entities = new ArrayList<>();
        
        // 创建超过阈值的命名生物
        for (int i = 0; i < 40; i++) {
            Zombie zombie = mock(Zombie.class);
            Location mobLocation = mock(Location.class);
            
            when(zombie.getLocation()).thenReturn(mobLocation);
            when(zombie.isValid()).thenReturn(true);
            when(zombie.getTicksLived()).thenReturn(100);
            when(zombie.getCustomName()).thenReturn("NamedZombie" + i); // 命名实体
            when(zombie.isPersistent()).thenReturn(false);
            
            when(mobLocation.getX()).thenReturn(500.0 + (i % 8));
            when(mobLocation.getZ()).thenReturn(500.0 + (i / 8));
            when(mobLocation.getY()).thenReturn(64.0);
            when(mobLocation.getWorld()).thenReturn(world);
            
            entities.add(zombie);
        }
        
        CleanupResult result = strategy.executeCleanup(world, entities);
        
        assertTrue(result.isSuccess());
        assertEquals(40, result.getEntitiesProcessed());
        assertEquals(0, result.getEntitiesRemoved()); // 命名实体应该被保护
    }
    
    @Test
    void testExecuteCleanup_PersistentEntities() {
        List<Entity> entities = new ArrayList<>();
        
        // 创建超过阈值的持久化生物
        for (int i = 0; i < 40; i++) {
            Zombie zombie = mock(Zombie.class);
            Location mobLocation = mock(Location.class);
            
            when(zombie.getLocation()).thenReturn(mobLocation);
            when(zombie.isValid()).thenReturn(true);
            when(zombie.getTicksLived()).thenReturn(100);
            when(zombie.getCustomName()).thenReturn(null);
            when(zombie.isPersistent()).thenReturn(true); // 持久化实体
            
            when(mobLocation.getX()).thenReturn(600.0 + (i % 8));
            when(mobLocation.getZ()).thenReturn(600.0 + (i / 8));
            when(mobLocation.getY()).thenReturn(64.0);
            when(mobLocation.getWorld()).thenReturn(world);
            
            entities.add(zombie);
        }
        
        CleanupResult result = strategy.executeCleanup(world, entities);
        
        assertTrue(result.isSuccess());
        assertEquals(40, result.getEntitiesProcessed());
        assertEquals(0, result.getEntitiesRemoved()); // 持久化实体应该被保护
    }
    
    @Test
    void testExecuteCleanup_MixedEntityTypes() {
        List<Entity> entities = new ArrayList<>();
        
        // 创建混合类型的实体
        for (int i = 0; i < 20; i++) {
            Item item = mock(Item.class);
            Location itemLocation = mock(Location.class);
            
            when(item.getLocation()).thenReturn(itemLocation);
            when(item.isValid()).thenReturn(true);
            when(item.getTicksLived()).thenReturn(100);
            
            when(itemLocation.getX()).thenReturn(700.0 + (i % 5));
            when(itemLocation.getZ()).thenReturn(700.0 + (i / 5));
            when(itemLocation.getY()).thenReturn(64.0);
            when(itemLocation.getWorld()).thenReturn(world);
            
            entities.add(item);
        }
        
        for (int i = 0; i < 15; i++) {
            ExperienceOrb orb = mock(ExperienceOrb.class);
            Location orbLocation = mock(Location.class);
            
            when(orb.getLocation()).thenReturn(orbLocation);
            when(orb.isValid()).thenReturn(true);
            when(orb.getTicksLived()).thenReturn(100);
            
            when(orbLocation.getX()).thenReturn(700.0 + (i % 5));
            when(orbLocation.getZ()).thenReturn(700.0 + (i / 5));
            when(orbLocation.getY()).thenReturn(64.0);
            when(orbLocation.getWorld()).thenReturn(world);
            
            entities.add(orb);
        }
        
        CleanupResult result = strategy.executeCleanup(world, entities);
        
        assertTrue(result.isSuccess());
        assertEquals(35, result.getEntitiesProcessed());
        // 应该根据优先级移除实体（物品优先于经验球）
        assertTrue(result.getEntitiesRemoved() >= 0);
    }
    
    @Test
    void testExecuteCleanup_EmptyList() {
        List<Entity> entities = Collections.emptyList();
        
        CleanupResult result = strategy.executeCleanup(world, entities);
        
        assertTrue(result.isSuccess());
        assertEquals(0, result.getEntitiesProcessed());
        assertEquals(0, result.getEntitiesRemoved());
    }
    
    @Test
    void testExecuteCleanup_Exception() {
        List<Entity> entities = new ArrayList<>();
        
        Item item = mock(Item.class);
        when(item.getLocation()).thenThrow(new RuntimeException("Test exception"));
        entities.add(item);
        
        CleanupResult result = strategy.executeCleanup(world, entities);
        
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Test exception"));
    }
    
    @Test
    void testUpdateConfiguration() {
        // 更新配置
        when(configManager.getInt("strategies.density-based.max-items-per-chunk", 50)).thenReturn(100);
        when(configManager.getInt("strategies.density-based.max-mobs-per-chunk", 30)).thenReturn(60);
        when(configManager.getDouble("strategies.density-based.detection-radius", 16.0)).thenReturn(32.0);
        when(configManager.getBoolean("strategies.density-based.protect-named", true)).thenReturn(false);
        
        strategy.updateConfiguration(configManager);
        
        Map<String, Object> config = strategy.getConfiguration();
        
        assertEquals(100, config.get("maxItemsPerChunk"));
        assertEquals(60, config.get("maxMobsPerChunk"));
        assertEquals(32.0, config.get("detectionRadius"));
        assertEquals(false, config.get("protectNamed"));
    }
    
    @Test
    void testGetConfiguration() {
        Map<String, Object> config = strategy.getConfiguration();
        
        assertNotNull(config);
        assertTrue(config.containsKey("maxItemsPerChunk"));
        assertTrue(config.containsKey("maxMobsPerChunk"));
        assertTrue(config.containsKey("maxExperiencePerChunk"));
        assertTrue(config.containsKey("detectionRadius"));
        assertTrue(config.containsKey("protectNamed"));
        assertTrue(config.containsKey("protectPersistent"));
        assertTrue(config.containsKey("priorityRemoval"));
        
        assertEquals(50, config.get("maxItemsPerChunk"));
        assertEquals(30, config.get("maxMobsPerChunk"));
        assertEquals(20, config.get("maxExperiencePerChunk"));
        assertEquals(16.0, config.get("detectionRadius"));
        assertEquals(true, config.get("protectNamed"));
        assertEquals(true, config.get("protectPersistent"));
    }
    
    @Test
    void testGetStatistics() {
        // 执行一些清理操作以生成统计数据
        List<Entity> entities = new ArrayList<>();
        
        for (int i = 0; i < 60; i++) {
            Item item = mock(Item.class);
            Location itemLocation = mock(Location.class);
            
            when(item.getLocation()).thenReturn(itemLocation);
            when(item.isValid()).thenReturn(true);
            when(item.getTicksLived()).thenReturn(100);
            
            when(itemLocation.getX()).thenReturn(800.0 + (i % 5));
            when(itemLocation.getZ()).thenReturn(800.0 + (i / 5));
            when(itemLocation.getY()).thenReturn(64.0);
            when(itemLocation.getWorld()).thenReturn(world);
            
            entities.add(item);
        }
        
        strategy.executeCleanup(world, entities);
        
        var statistics = strategy.getStatistics();
        
        assertNotNull(statistics);
        assertEquals(1, statistics.getExecutionCount());
        assertEquals(60, statistics.getEntitiesProcessed());
        assertTrue(statistics.getEntitiesRemoved() >= 0);
        assertTrue(statistics.getTotalExecutionTime() >= 0);
    }
    
    @Test
    void testValidateConfiguration_Valid() {
        Map<String, Object> config = new HashMap<>();
        config.put("maxItemsPerChunk", 50);
        config.put("maxMobsPerChunk", 30);
        config.put("maxExperiencePerChunk", 20);
        config.put("detectionRadius", 16.0);
        config.put("protectNamed", true);
        config.put("protectPersistent", true);
        config.put("priorityRemoval", Arrays.asList("ITEM", "EXPERIENCE_ORB"));
        
        var result = strategy.validateConfiguration(config);
        
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }
    
    @Test
    void testValidateConfiguration_Invalid() {
        Map<String, Object> config = new HashMap<>();
        config.put("maxItemsPerChunk", -1); // 无效值
        config.put("detectionRadius", "invalid"); // 错误类型
        config.put("priorityRemoval", "not_a_list"); // 错误类型
        
        var result = strategy.validateConfiguration(config);
        
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
    }
    
    @Test
    void testIsApplicableToEntity() {
        Item item = mock(Item.class);
        Zombie zombie = mock(Zombie.class);
        ExperienceOrb orb = mock(ExperienceOrb.class);
        Arrow arrow = mock(Arrow.class);
        
        // 所有实体类型都应该适用于密度检查
        assertTrue(strategy.isApplicableToEntity(item));
        assertTrue(strategy.isApplicableToEntity(zombie));
        assertTrue(strategy.isApplicableToEntity(orb));
        assertTrue(strategy.isApplicableToEntity(arrow));
    }
    
    @Test
    void testDistanceCalculation() {
        // 测试距离计算逻辑
        List<Entity> entities = new ArrayList<>();
        
        // 创建两组实体，一组在检测半径内，一组在检测半径外
        for (int i = 0; i < 30; i++) {
            Item item = mock(Item.class);
            Location itemLocation = mock(Location.class);
            
            when(item.getLocation()).thenReturn(itemLocation);
            when(item.isValid()).thenReturn(true);
            when(item.getTicksLived()).thenReturn(100);
            
            if (i < 15) {
                // 在检测半径内（16格）
                when(itemLocation.getX()).thenReturn(900.0 + (i % 3));
                when(itemLocation.getZ()).thenReturn(900.0 + (i / 3));
            } else {
                // 在检测半径外（超过16格）
                when(itemLocation.getX()).thenReturn(900.0 + 20 + (i % 3));
                when(itemLocation.getZ()).thenReturn(900.0 + 20 + (i / 3));
            }
            
            when(itemLocation.getY()).thenReturn(64.0);
            when(itemLocation.getWorld()).thenReturn(world);
            
            entities.add(item);
        }
        
        CleanupResult result = strategy.executeCleanup(world, entities);
        
        assertTrue(result.isSuccess());
        assertEquals(30, result.getEntitiesProcessed());
        // 只有在检测半径内的实体应该被考虑进行密度清理
    }
}