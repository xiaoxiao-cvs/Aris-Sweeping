package com.xiaoxiao.arissweeping.strategy.impl;

import com.xiaoxiao.arissweeping.config.ConfigManager;
import com.xiaoxiao.arissweeping.observer.CleanupEventManager;
import com.xiaoxiao.arissweeping.strategy.CleanupStrategy.CleanupResult;
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
 * AgeBasedCleanupStrategy 单元测试
 */
@ExtendWith(MockitoExtension.class)
class AgeBasedCleanupStrategyTest {
    
    @Mock
    private ConfigManager configManager;
    
    @Mock
    private CleanupEventManager eventManager;
    
    @Mock
    private World world;
    
    @Mock
    private Item item;
    
    @Mock
    private ExperienceOrb experienceOrb;
    
    @Mock
    private Arrow arrow;
    
    @Mock
    private Zombie zombie;
    
    private AgeBasedCleanupStrategy strategy;
    
    @BeforeEach
    void setUp() {
        strategy = new AgeBasedCleanupStrategy(configManager, eventManager);
        
        // 设置默认配置
        when(configManager.getInt("strategies.age-based.max-item-age", 300)).thenReturn(300);
        when(configManager.getInt("strategies.age-based.max-experience-age", 120)).thenReturn(120);
        when(configManager.getInt("strategies.age-based.max-arrow-age", 60)).thenReturn(60);
        when(configManager.getInt("strategies.age-based.max-living-age", 600)).thenReturn(600);
        when(configManager.getBoolean("strategies.age-based.protect-named", true)).thenReturn(true);
        when(configManager.getBoolean("strategies.age-based.protect-persistent", true)).thenReturn(true);
        
        // 初始化策略配置
        strategy.updateConfiguration(configManager);
    }
    
    @Test
    void testGetStrategyName() {
        assertEquals("AgeBasedCleanup", strategy.getStrategyName());
    }
    
    @Test
    void testGetDescription() {
        assertNotNull(strategy.getDescription());
        assertTrue(strategy.getDescription().contains("年龄"));
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
    void testIsApplicableToEntity_Item() {
        when(item.getTicksLived()).thenReturn(400); // 20秒 (400 ticks)
        assertTrue(strategy.isApplicableToEntity(item));
        
        when(item.getTicksLived()).thenReturn(200); // 10秒 (200 ticks)
        assertFalse(strategy.isApplicableToEntity(item));
    }
    
    @Test
    void testIsApplicableToEntity_ExperienceOrb() {
        when(experienceOrb.getTicksLived()).thenReturn(150); // 7.5秒 (150 ticks)
        assertTrue(strategy.isApplicableToEntity(experienceOrb));
        
        when(experienceOrb.getTicksLived()).thenReturn(100); // 5秒 (100 ticks)
        assertFalse(strategy.isApplicableToEntity(experienceOrb));
    }
    
    @Test
    void testIsApplicableToEntity_Arrow() {
        when(arrow.getTicksLived()).thenReturn(80); // 4秒 (80 ticks)
        assertTrue(strategy.isApplicableToEntity(arrow));
        
        when(arrow.getTicksLived()).thenReturn(40); // 2秒 (40 ticks)
        assertFalse(strategy.isApplicableToEntity(arrow));
    }
    
    @Test
    void testIsApplicableToEntity_LivingEntity() {
        when(zombie.getTicksLived()).thenReturn(800); // 40秒 (800 ticks)
        assertTrue(strategy.isApplicableToEntity(zombie));
        
        when(zombie.getTicksLived()).thenReturn(400); // 20秒 (400 ticks)
        assertFalse(strategy.isApplicableToEntity(zombie));
    }
    
    @Test
    void testIsApplicableToEntity_NamedEntity() {
        when(zombie.getTicksLived()).thenReturn(800);
        when(zombie.getCustomName()).thenReturn("NamedZombie");
        
        // 命名实体应该被保护
        assertFalse(strategy.isApplicableToEntity(zombie));
    }
    
    @Test
    void testIsApplicableToEntity_PersistentEntity() {
        when(zombie.getTicksLived()).thenReturn(800);
        when(zombie.isPersistent()).thenReturn(true);
        
        // 持久化实体应该被保护
        assertFalse(strategy.isApplicableToEntity(zombie));
    }
    
    @Test
    void testExecuteCleanup_Success() {
        List<Entity> entities = Arrays.asList(item, experienceOrb, arrow);
        
        // 设置实体年龄超过阈值
        when(item.getTicksLived()).thenReturn(400);
        when(experienceOrb.getTicksLived()).thenReturn(150);
        when(arrow.getTicksLived()).thenReturn(80);
        
        // 模拟成功移除
        when(item.isValid()).thenReturn(true);
        when(experienceOrb.isValid()).thenReturn(true);
        when(arrow.isValid()).thenReturn(true);
        
        CleanupResult result = strategy.executeCleanup(world, entities);
        
        assertTrue(result.isSuccess());
        assertEquals(3, result.getEntitiesProcessed());
        assertEquals(3, result.getEntitiesRemoved());
        
        // 验证实体被移除
        verify(item).remove();
        verify(experienceOrb).remove();
        verify(arrow).remove();
    }
    
    @Test
    void testExecuteCleanup_PartialSuccess() {
        List<Entity> entities = Arrays.asList(item, experienceOrb, arrow);
        
        // 只有部分实体年龄超过阈值
        when(item.getTicksLived()).thenReturn(400); // 超过阈值
        when(experienceOrb.getTicksLived()).thenReturn(100); // 未超过阈值
        when(arrow.getTicksLived()).thenReturn(80); // 超过阈值
        
        when(item.isValid()).thenReturn(true);
        when(arrow.isValid()).thenReturn(true);
        
        CleanupResult result = strategy.executeCleanup(world, entities);
        
        assertTrue(result.isSuccess());
        assertEquals(3, result.getEntitiesProcessed());
        assertEquals(2, result.getEntitiesRemoved());
        
        // 验证只有符合条件的实体被移除
        verify(item).remove();
        verify(experienceOrb, never()).remove();
        verify(arrow).remove();
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
        List<Entity> entities = Arrays.asList(item);
        
        when(item.getTicksLived()).thenReturn(400);
        when(item.isValid()).thenReturn(true);
        doThrow(new RuntimeException("Test exception")).when(item).remove();
        
        CleanupResult result = strategy.executeCleanup(world, entities);
        
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Test exception"));
    }
    
    @Test
    void testUpdateConfiguration() {
        // 更新配置
        when(configManager.getInt("strategies.age-based.max-item-age", 300)).thenReturn(600);
        when(configManager.getInt("strategies.age-based.max-experience-age", 120)).thenReturn(240);
        when(configManager.getBoolean("strategies.age-based.protect-named", true)).thenReturn(false);
        
        strategy.updateConfiguration(configManager);
        
        // 测试新配置是否生效
        when(item.getTicksLived()).thenReturn(500); // 25秒
        assertTrue(strategy.isApplicableToEntity(item)); // 应该仍然适用，因为新阈值是600
        
        when(experienceOrb.getTicksLived()).thenReturn(200); // 10秒
        assertTrue(strategy.isApplicableToEntity(experienceOrb)); // 应该适用，因为新阈值是240
        
        // 测试命名保护被禁用
        when(zombie.getTicksLived()).thenReturn(800);
        when(zombie.getCustomName()).thenReturn("NamedZombie");
        assertTrue(strategy.isApplicableToEntity(zombie)); // 应该适用，因为命名保护被禁用
    }
    
    @Test
    void testGetConfiguration() {
        Map<String, Object> config = strategy.getConfiguration();
        
        assertNotNull(config);
        assertTrue(config.containsKey("maxItemAge"));
        assertTrue(config.containsKey("maxExperienceAge"));
        assertTrue(config.containsKey("maxArrowAge"));
        assertTrue(config.containsKey("maxLivingAge"));
        assertTrue(config.containsKey("protectNamed"));
        assertTrue(config.containsKey("protectPersistent"));
        
        assertEquals(300, config.get("maxItemAge"));
        assertEquals(120, config.get("maxExperienceAge"));
        assertEquals(60, config.get("maxArrowAge"));
        assertEquals(600, config.get("maxLivingAge"));
        assertEquals(true, config.get("protectNamed"));
        assertEquals(true, config.get("protectPersistent"));
    }
    
    @Test
    void testGetStatistics() {
        // 执行一些清理操作以生成统计数据
        List<Entity> entities = Arrays.asList(item, experienceOrb);
        
        when(item.getTicksLived()).thenReturn(400);
        when(experienceOrb.getTicksLived()).thenReturn(150);
        when(item.isValid()).thenReturn(true);
        when(experienceOrb.isValid()).thenReturn(true);
        
        strategy.executeCleanup(world, entities);
        
        var statistics = strategy.getStatistics();
        
        assertNotNull(statistics);
        assertEquals(1, statistics.getExecutionCount());
        assertEquals(2, statistics.getEntitiesProcessed());
        assertEquals(2, statistics.getEntitiesRemoved());
        assertTrue(statistics.getTotalExecutionTime() >= 0);
        assertEquals(100.0, statistics.getSuccessRate(), 0.01);
    }
    
    @Test
    void testValidateConfiguration_Valid() {
        Map<String, Object> config = new HashMap<>();
        config.put("maxItemAge", 300);
        config.put("maxExperienceAge", 120);
        config.put("maxArrowAge", 60);
        config.put("maxLivingAge", 600);
        config.put("protectNamed", true);
        config.put("protectPersistent", true);
        
        var result = strategy.validateConfiguration(config);
        
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }
    
    @Test
    void testValidateConfiguration_Invalid() {
        Map<String, Object> config = new HashMap<>();
        config.put("maxItemAge", -1); // 无效值
        config.put("maxExperienceAge", "invalid"); // 错误类型
        
        var result = strategy.validateConfiguration(config);
        
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
    }
    
    @Test
    void testEntityTypeHandling() {
        // 测试不同实体类型的处理
        Entity unknownEntity = mock(Entity.class);
        when(unknownEntity.getTicksLived()).thenReturn(1000);
        
        // 未知实体类型应该不被处理
        assertFalse(strategy.isApplicableToEntity(unknownEntity));
    }
    
    @Test
    void testTicksToSecondsConversion() {
        // 测试 ticks 到秒的转换逻辑
        when(item.getTicksLived()).thenReturn(300); // 15秒 (300 ticks = 15 seconds)
        assertTrue(strategy.isApplicableToEntity(item)); // 应该适用，因为阈值是300秒
        
        when(item.getTicksLived()).thenReturn(6000); // 300秒 (6000 ticks = 300 seconds)
        assertTrue(strategy.isApplicableToEntity(item)); // 应该适用，正好达到阈值
        
        when(item.getTicksLived()).thenReturn(6001); // 300.05秒 (6001 ticks > 300 seconds)
        assertTrue(strategy.isApplicableToEntity(item)); // 应该适用，超过阈值
    }
}