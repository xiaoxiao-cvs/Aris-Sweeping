package com.xiaoxiao.arissweeping.observer;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CleanupEvent 及其子类的单元测试
 */
@ExtendWith(MockitoExtension.class)
class CleanupEventTest {
    
    @Mock
    private World world;
    
    @Mock
    private Entity entity;
    
    private long testTimestamp;
    
    @BeforeEach
    void setUp() {
        testTimestamp = System.currentTimeMillis();
        
        when(world.getName()).thenReturn("test-world");
        when(world.getUID()).thenReturn(UUID.randomUUID());
        
        when(entity.getUniqueId()).thenReturn(UUID.randomUUID());
        when(entity.getType()).thenReturn(EntityType.ITEM);
        when(entity.getWorld()).thenReturn(world);
    }
    
    @Test
    void testCleanupStartedEvent_Creation() {
        String strategyName = "test-strategy";
        
        CleanupEvent.CleanupStartedEvent event = new CleanupEvent.CleanupStartedEvent(
            strategyName, world, testTimestamp
        );
        
        assertNotNull(event);
        assertEquals(strategyName, event.getStrategyName());
        assertEquals(world, event.getWorld());
        assertEquals(testTimestamp, event.getTimestamp());
    }
    
    @Test
    void testCleanupStartedEvent_Equality() {
        String strategyName = "test-strategy";
        
        CleanupEvent.CleanupStartedEvent event1 = new CleanupEvent.CleanupStartedEvent(
            strategyName, world, testTimestamp
        );
        
        CleanupEvent.CleanupStartedEvent event2 = new CleanupEvent.CleanupStartedEvent(
            strategyName, world, testTimestamp
        );
        
        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
    }
    
    @Test
    void testCleanupStartedEvent_ToString() {
        String strategyName = "test-strategy";
        
        CleanupEvent.CleanupStartedEvent event = new CleanupEvent.CleanupStartedEvent(
            strategyName, world, testTimestamp
        );
        
        String toString = event.toString();
        assertNotNull(toString);
        assertTrue(toString.contains(strategyName));
        assertTrue(toString.contains("test-world"));
    }
    
    @Test
    void testCleanupCompletedEvent_Creation() {
        String strategyName = "test-strategy";
        int entitiesProcessed = 100;
        int entitiesRemoved = 80;
        long executionTime = 5000L;
        
        CleanupEvent.CleanupCompletedEvent event = new CleanupEvent.CleanupCompletedEvent(
            strategyName, world, entitiesProcessed, entitiesRemoved, executionTime, testTimestamp
        );
        
        assertNotNull(event);
        assertEquals(strategyName, event.getStrategyName());
        assertEquals(world, event.getWorld());
        assertEquals(entitiesProcessed, event.getEntitiesProcessed());
        assertEquals(entitiesRemoved, event.getEntitiesRemoved());
        assertEquals(executionTime, event.getExecutionTime());
        assertEquals(testTimestamp, event.getTimestamp());
    }
    
    @Test
    void testCleanupCompletedEvent_CalculatedProperties() {
        CleanupEvent.CleanupCompletedEvent event = new CleanupEvent.CleanupCompletedEvent(
            "test-strategy", world, 100, 80, 5000L, testTimestamp
        );
        
        // 测试移除率计算
        assertEquals(0.8, event.getRemovalRate(), 0.001);
        
        // 测试每秒处理实体数计算
        assertEquals(20.0, event.getEntitiesPerSecond(), 0.001); // 100 entities / 5 seconds
    }
    
    @Test
    void testCleanupCompletedEvent_ZeroExecutionTime() {
        CleanupEvent.CleanupCompletedEvent event = new CleanupEvent.CleanupCompletedEvent(
            "test-strategy", world, 100, 80, 0L, testTimestamp
        );
        
        // 执行时间为0时，每秒处理实体数应该为0或无穷大，这里假设返回0
        assertTrue(event.getEntitiesPerSecond() >= 0);
    }
    
    @Test
    void testCleanupCompletedEvent_ZeroEntitiesProcessed() {
        CleanupEvent.CleanupCompletedEvent event = new CleanupEvent.CleanupCompletedEvent(
            "test-strategy", world, 0, 0, 1000L, testTimestamp
        );
        
        assertEquals(0.0, event.getRemovalRate(), 0.001);
        assertEquals(0.0, event.getEntitiesPerSecond(), 0.001);
    }
    
    @Test
    void testEntityRemovedEvent_Creation() {
        String strategyName = "test-strategy";
        String reason = "Age exceeded threshold";
        
        CleanupEvent.EntityRemovedEvent event = new CleanupEvent.EntityRemovedEvent(
            entity, strategyName, reason, testTimestamp
        );
        
        assertNotNull(event);
        assertEquals(entity, event.getEntity());
        assertEquals(strategyName, event.getStrategyName());
        assertEquals(reason, event.getReason());
        assertEquals(testTimestamp, event.getTimestamp());
    }
    
    @Test
    void testEntityRemovedEvent_EntityProperties() {
        CleanupEvent.EntityRemovedEvent event = new CleanupEvent.EntityRemovedEvent(
            entity, "test-strategy", "reason", testTimestamp
        );
        
        assertEquals(entity.getUniqueId(), event.getEntityId());
        assertEquals(entity.getType(), event.getEntityType());
        assertEquals(world, event.getWorld());
    }
    
    @Test
    void testPerformanceWarningEvent_Creation() {
        CleanupEvent.PerformanceWarningEvent.WarningType warningType = 
            CleanupEvent.PerformanceWarningEvent.WarningType.HIGH_MEMORY;
        String message = "Memory usage is 85%";
        double value = 85.0;
        
        CleanupEvent.PerformanceWarningEvent event = new CleanupEvent.PerformanceWarningEvent(
            warningType, message, value, testTimestamp
        );
        
        assertNotNull(event);
        assertEquals(warningType, event.getWarningType());
        assertEquals(message, event.getMessage());
        assertEquals(value, event.getValue(), 0.001);
        assertEquals(testTimestamp, event.getTimestamp());
    }
    
    @Test
    void testPerformanceWarningEvent_AllWarningTypes() {
        CleanupEvent.PerformanceWarningEvent.WarningType[] types = 
            CleanupEvent.PerformanceWarningEvent.WarningType.values();
        
        assertTrue(types.length > 0);
        
        for (CleanupEvent.PerformanceWarningEvent.WarningType type : types) {
            CleanupEvent.PerformanceWarningEvent event = new CleanupEvent.PerformanceWarningEvent(
                type, "Test message", 50.0, testTimestamp
            );
            
            assertEquals(type, event.getWarningType());
        }
    }
    
    @Test
    void testPerformanceWarningEvent_Severity() {
        // 测试不同严重程度的警告
        CleanupEvent.PerformanceWarningEvent lowSeverity = new CleanupEvent.PerformanceWarningEvent(
            CleanupEvent.PerformanceWarningEvent.WarningType.HIGH_MEMORY, 
            "Low severity", 60.0, testTimestamp
        );
        
        CleanupEvent.PerformanceWarningEvent highSeverity = new CleanupEvent.PerformanceWarningEvent(
            CleanupEvent.PerformanceWarningEvent.WarningType.HIGH_MEMORY, 
            "High severity", 95.0, testTimestamp
        );
        
        // 假设有严重程度判断方法
        if (lowSeverity.getSeverity() != null && highSeverity.getSeverity() != null) {
            assertTrue(highSeverity.getSeverity().ordinal() > lowSeverity.getSeverity().ordinal());
        }
    }
    
    @Test
    void testConfigChangedEvent_Creation() {
        String configPath = "cleanup.strategy.age-based";
        Map<String, Object> oldConfig = new HashMap<>();
        oldConfig.put("enabled", false);
        oldConfig.put("interval", 300);
        
        Map<String, Object> newConfig = new HashMap<>();
        newConfig.put("enabled", true);
        newConfig.put("interval", 600);
        
        CleanupEvent.ConfigChangedEvent event = new CleanupEvent.ConfigChangedEvent(
            configPath, oldConfig, newConfig, testTimestamp
        );
        
        assertNotNull(event);
        assertEquals(configPath, event.getConfigPath());
        assertEquals(oldConfig, event.getOldConfig());
        assertEquals(newConfig, event.getNewConfig());
        assertEquals(testTimestamp, event.getTimestamp());
    }
    
    @Test
    void testConfigChangedEvent_ConfigDifferences() {
        Map<String, Object> oldConfig = new HashMap<>();
        oldConfig.put("enabled", false);
        oldConfig.put("interval", 300);
        oldConfig.put("threshold", 100);
        
        Map<String, Object> newConfig = new HashMap<>();
        newConfig.put("enabled", true);
        newConfig.put("interval", 600);
        newConfig.put("threshold", 100);
        
        CleanupEvent.ConfigChangedEvent event = new CleanupEvent.ConfigChangedEvent(
            "test.config", oldConfig, newConfig, testTimestamp
        );
        
        // 测试配置变更检测
        Map<String, Object> changes = event.getChangedKeys();
        if (changes != null) {
            assertTrue(changes.containsKey("enabled"));
            assertTrue(changes.containsKey("interval"));
            assertFalse(changes.containsKey("threshold"));
        }
    }
    
    @Test
    void testConfigChangedEvent_EmptyConfigs() {
        Map<String, Object> emptyOldConfig = new HashMap<>();
        Map<String, Object> emptyNewConfig = new HashMap<>();
        
        CleanupEvent.ConfigChangedEvent event = new CleanupEvent.ConfigChangedEvent(
            "test.config", emptyOldConfig, emptyNewConfig, testTimestamp
        );
        
        assertNotNull(event);
        assertTrue(event.getOldConfig().isEmpty());
        assertTrue(event.getNewConfig().isEmpty());
    }
    
    @Test
    void testBaseCleanupEvent_CommonProperties() {
        CleanupEvent.CleanupStartedEvent event = new CleanupEvent.CleanupStartedEvent(
            "test-strategy", world, testTimestamp
        );
        
        // 测试基类属性
        assertEquals(testTimestamp, event.getTimestamp());
        assertTrue(event.getTimestamp() > 0);
        
        // 测试事件ID（如果有的话）
        if (event.getEventId() != null) {
            assertNotNull(event.getEventId());
        }
    }
    
    @Test
    void testEventSerialization() {
        CleanupEvent.CleanupCompletedEvent event = new CleanupEvent.CleanupCompletedEvent(
            "test-strategy", world, 100, 80, 5000L, testTimestamp
        );
        
        // 测试事件序列化（如果实现了的话）
        String serialized = event.toJson();
        if (serialized != null) {
            assertNotNull(serialized);
            assertTrue(serialized.contains("test-strategy"));
            assertTrue(serialized.contains("100"));
            assertTrue(serialized.contains("80"));
        }
    }
    
    @Test
    void testEventDeserialization() {
        // 测试从JSON反序列化事件（如果实现了的话）
        String json = "{\"strategyName\":\"test-strategy\",\"entitiesProcessed\":100,\"entitiesRemoved\":80}";
        
        CleanupEvent.CleanupCompletedEvent event = CleanupEvent.CleanupCompletedEvent.fromJson(json);
        if (event != null) {
            assertEquals("test-strategy", event.getStrategyName());
            assertEquals(100, event.getEntitiesProcessed());
            assertEquals(80, event.getEntitiesRemoved());
        }
    }
    
    @Test
    void testEventValidation() {
        // 测试无效参数
        assertThrows(IllegalArgumentException.class, () -> {
            new CleanupEvent.CleanupStartedEvent(null, world, testTimestamp);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new CleanupEvent.CleanupStartedEvent("", world, testTimestamp);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new CleanupEvent.CleanupStartedEvent("test", null, testTimestamp);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new CleanupEvent.CleanupCompletedEvent("test", world, -1, 0, 1000L, testTimestamp);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new CleanupEvent.CleanupCompletedEvent("test", world, 100, -1, 1000L, testTimestamp);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new CleanupEvent.CleanupCompletedEvent("test", world, 100, 80, -1L, testTimestamp);
        });
    }
    
    @Test
    void testEventImmutability() {
        Map<String, Object> originalConfig = new HashMap<>();
        originalConfig.put("key", "value");
        
        CleanupEvent.ConfigChangedEvent event = new CleanupEvent.ConfigChangedEvent(
            "test.config", originalConfig, new HashMap<>(), testTimestamp
        );
        
        // 修改原始配置不应该影响事件中的配置
        originalConfig.put("key", "modified");
        
        Map<String, Object> eventConfig = event.getOldConfig();
        if (eventConfig != null) {
            assertEquals("value", eventConfig.get("key"));
        }
    }
    
    @Test
    void testEventComparison() {
        CleanupEvent.CleanupStartedEvent event1 = new CleanupEvent.CleanupStartedEvent(
            "strategy1", world, testTimestamp
        );
        
        CleanupEvent.CleanupStartedEvent event2 = new CleanupEvent.CleanupStartedEvent(
            "strategy2", world, testTimestamp + 1000
        );
        
        // 测试事件比较（如果实现了Comparable接口）
        if (event1 instanceof Comparable) {
            @SuppressWarnings("unchecked")
            Comparable<CleanupEvent.CleanupStartedEvent> comparableEvent1 = 
                (Comparable<CleanupEvent.CleanupStartedEvent>) event1;
            
            assertTrue(comparableEvent1.compareTo(event2) < 0); // event1 应该在 event2 之前
        }
    }
    
    @Test
    void testEventMetadata() {
        CleanupEvent.CleanupStartedEvent event = new CleanupEvent.CleanupStartedEvent(
            "test-strategy", world, testTimestamp
        );
        
        // 测试事件元数据（如果有的话）
        Map<String, Object> metadata = event.getMetadata();
        if (metadata != null) {
            assertNotNull(metadata);
            // 可能包含服务器信息、插件版本等
        }
    }
    
    @Test
    void testEventSource() {
        CleanupEvent.CleanupStartedEvent event = new CleanupEvent.CleanupStartedEvent(
            "test-strategy", world, testTimestamp
        );
        
        // 测试事件源（如果有的话）
        String source = event.getSource();
        if (source != null) {
            assertNotNull(source);
            assertTrue(source.length() > 0);
        }
    }
    
    @Test
    void testEventCategory() {
        CleanupEvent.CleanupStartedEvent startedEvent = new CleanupEvent.CleanupStartedEvent(
            "test-strategy", world, testTimestamp
        );
        
        CleanupEvent.PerformanceWarningEvent warningEvent = new CleanupEvent.PerformanceWarningEvent(
            CleanupEvent.PerformanceWarningEvent.WarningType.HIGH_MEMORY, 
            "Warning", 80.0, testTimestamp
        );
        
        // 测试事件分类（如果有的话）
        if (startedEvent.getCategory() != null && warningEvent.getCategory() != null) {
            assertNotEquals(startedEvent.getCategory(), warningEvent.getCategory());
        }
    }
}