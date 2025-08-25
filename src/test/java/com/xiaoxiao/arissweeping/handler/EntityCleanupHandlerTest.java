package com.xiaoxiao.arissweeping.handler;

import com.xiaoxiao.arissweeping.config.ConfigManager;
import com.xiaoxiao.arissweeping.observer.CleanupEventManager;
import com.xiaoxiao.arissweeping.performance.PerformanceManager;
import com.xiaoxiao.arissweeping.strategy.StrategyManager;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * EntityCleanupHandler 单元测试
 */
@ExtendWith(MockitoExtension.class)
class EntityCleanupHandlerTest {
    
    @Mock
    private ConfigManager configManager;
    
    @Mock
    private StrategyManager strategyManager;
    
    @Mock
    private CleanupEventManager eventManager;
    
    @Mock
    private PerformanceManager performanceManager;
    
    @Mock
    private Server server;
    
    @Mock
    private BukkitScheduler scheduler;
    
    @Mock
    private BukkitTask task;
    
    @Mock
    private World world;
    
    @Mock
    private Entity entity1;
    
    @Mock
    private Entity entity2;
    
    @Mock
    private Entity entity3;
    
    private EntityCleanupHandler handler;
    
    @BeforeEach
    void setUp() {
        // 设置基本的 mock 行为
        when(configManager.getBoolean("cleanup.enabled", true)).thenReturn(true);
        when(configManager.getInt("cleanup.interval", 300)).thenReturn(300);
        when(configManager.getInt("cleanup.batch-size", 100)).thenReturn(100);
        when(configManager.getBoolean("cleanup.async", true)).thenReturn(true);
        when(configManager.getStringList("cleanup.worlds", Arrays.asList("world", "world_nether", "world_the_end")))
            .thenReturn(Arrays.asList("world", "world_nether", "world_the_end"));
        
        when(world.getName()).thenReturn("world");
        when(world.getEntities()).thenReturn(Arrays.asList(entity1, entity2, entity3));
        
        when(entity1.getUniqueId()).thenReturn(UUID.randomUUID());
        when(entity1.getType()).thenReturn(EntityType.ITEM);
        when(entity1.isValid()).thenReturn(true);
        
        when(entity2.getUniqueId()).thenReturn(UUID.randomUUID());
        when(entity2.getType()).thenReturn(EntityType.EXPERIENCE_ORB);
        when(entity2.isValid()).thenReturn(true);
        
        when(entity3.getUniqueId()).thenReturn(UUID.randomUUID());
        when(entity3.getType()).thenReturn(EntityType.ARROW);
        when(entity3.isValid()).thenReturn(true);
        
        when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTaskTimerAsynchronously(any(), any(Runnable.class), anyLong(), anyLong()))
            .thenReturn(task);
        
        handler = new EntityCleanupHandler(configManager, strategyManager, eventManager, performanceManager);
    }
    
    @Test
    void testInitialization() {
        assertNotNull(handler);
        
        // 验证依赖注入
        verify(configManager, atLeastOnce()).getBoolean("cleanup.enabled", true);
        verify(configManager, atLeastOnce()).getInt("cleanup.interval", 300);
    }
    
    @Test
    void testStartCleanup() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            
            handler.startCleanup();
            
            // 验证调度器被调用
            verify(scheduler).runTaskTimerAsynchronously(
                any(), any(Runnable.class), eq(0L), eq(6000L) // 300秒 * 20 ticks
            );
        }
    }
    
    @Test
    void testStopCleanup() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            
            handler.startCleanup();
            handler.stopCleanup();
            
            // 验证任务被取消
            verify(task).cancel();
        }
    }
    
    @Test
    void testCleanupDisabled() {
        when(configManager.getBoolean("cleanup.enabled", true)).thenReturn(false);
        
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            bukkit.when(Bukkit::getWorlds).thenReturn(Arrays.asList(world));
            
            handler.performCleanup();
            
            // 验证没有进行清理
            verify(strategyManager, never()).executeStrategies(any(), any());
        }
    }
    
    @Test
    void testPerformCleanup_SingleWorld() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            bukkit.when(Bukkit::getWorlds).thenReturn(Arrays.asList(world));
            
            // 模拟策略执行结果
            when(strategyManager.executeStrategies(eq(world), any()))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(entity1, entity2)));
            
            handler.performCleanup();
            
            // 验证策略管理器被调用
            verify(strategyManager).executeStrategies(eq(world), any());
            
            // 验证事件发布
            verify(eventManager, atLeastOnce()).publish(any());
        }
    }
    
    @Test
    void testPerformCleanup_MultipleWorlds() {
        World world2 = mock(World.class);
        when(world2.getName()).thenReturn("world_nether");
        when(world2.getEntities()).thenReturn(Arrays.asList(entity3));
        
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            bukkit.when(Bukkit::getWorlds).thenReturn(Arrays.asList(world, world2));
            
            when(strategyManager.executeStrategies(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(entity1)))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(entity3)));
            
            handler.performCleanup();
            
            // 验证两个世界都被处理
            verify(strategyManager).executeStrategies(eq(world), any());
            verify(strategyManager).executeStrategies(eq(world2), any());
        }
    }
    
    @Test
    void testPerformCleanup_WorldFiltering() {
        World excludedWorld = mock(World.class);
        when(excludedWorld.getName()).thenReturn("excluded_world");
        
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            bukkit.when(Bukkit::getWorlds).thenReturn(Arrays.asList(world, excludedWorld));
            
            when(strategyManager.executeStrategies(eq(world), any()))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(entity1)));
            
            handler.performCleanup();
            
            // 验证只有允许的世界被处理
            verify(strategyManager).executeStrategies(eq(world), any());
            verify(strategyManager, never()).executeStrategies(eq(excludedWorld), any());
        }
    }
    
    @Test
    void testPerformCleanup_AsyncExecution() {
        when(configManager.getBoolean("cleanup.async", true)).thenReturn(true);
        
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            bukkit.when(Bukkit::getWorlds).thenReturn(Arrays.asList(world));
            
            CompletableFuture<List<Entity>> future = new CompletableFuture<>();
            when(strategyManager.executeStrategies(eq(world), any())).thenReturn(future);
            
            // 启动异步清理
            handler.performCleanup();
            
            // 完成异步操作
            future.complete(Arrays.asList(entity1, entity2));
            
            // 等待异步操作完成
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            verify(strategyManager).executeStrategies(eq(world), any());
        }
    }
    
    @Test
    void testPerformCleanup_SyncExecution() {
        when(configManager.getBoolean("cleanup.async", true)).thenReturn(false);
        
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            bukkit.when(Bukkit::getWorlds).thenReturn(Arrays.asList(world));
            
            when(strategyManager.executeStrategies(eq(world), any()))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(entity1)));
            
            handler.performCleanup();
            
            verify(strategyManager).executeStrategies(eq(world), any());
        }
    }
    
    @Test
    void testPerformCleanup_ExceptionHandling() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            bukkit.when(Bukkit::getWorlds).thenReturn(Arrays.asList(world));
            
            // 模拟策略执行异常
            CompletableFuture<List<Entity>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException("Test exception"));
            when(strategyManager.executeStrategies(eq(world), any())).thenReturn(failedFuture);
            
            // 清理不应该抛出异常
            assertDoesNotThrow(() -> handler.performCleanup());
            
            verify(strategyManager).executeStrategies(eq(world), any());
        }
    }
    
    @Test
    void testPerformCleanup_BatchProcessing() {
        when(configManager.getInt("cleanup.batch-size", 100)).thenReturn(2);
        
        // 创建更多实体用于测试批处理
        List<Entity> manyEntities = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Entity entity = mock(Entity.class);
            when(entity.getUniqueId()).thenReturn(UUID.randomUUID());
            when(entity.getType()).thenReturn(EntityType.ITEM);
            when(entity.isValid()).thenReturn(true);
            manyEntities.add(entity);
        }
        
        when(world.getEntities()).thenReturn(manyEntities);
        
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            bukkit.when(Bukkit::getWorlds).thenReturn(Arrays.asList(world));
            
            when(strategyManager.executeStrategies(eq(world), any()))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(manyEntities.get(0), manyEntities.get(1))));
            
            handler.performCleanup();
            
            // 验证批处理逻辑
            verify(strategyManager, atLeast(1)).executeStrategies(eq(world), any());
        }
    }
    
    @Test
    void testPerformCleanup_PerformanceMonitoring() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            bukkit.when(Bukkit::getWorlds).thenReturn(Arrays.asList(world));
            
            when(strategyManager.executeStrategies(eq(world), any()))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(entity1, entity2)));
            
            handler.performCleanup();
            
            // 验证性能监控被调用
            verify(performanceManager, atLeastOnce()).recordMetric(anyString(), anyDouble());
        }
    }
    
    @Test
    void testPerformCleanup_EventPublishing() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            bukkit.when(Bukkit::getWorlds).thenReturn(Arrays.asList(world));
            
            when(strategyManager.executeStrategies(eq(world), any()))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(entity1, entity2)));
            
            handler.performCleanup();
            
            // 验证清理开始和完成事件被发布
            verify(eventManager, atLeast(2)).publish(any());
        }
    }
    
    @Test
    void testPerformCleanup_NoEntitiesRemoved() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            bukkit.when(Bukkit::getWorlds).thenReturn(Arrays.asList(world));
            
            when(strategyManager.executeStrategies(eq(world), any()))
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
            
            handler.performCleanup();
            
            verify(strategyManager).executeStrategies(eq(world), any());
            verify(eventManager, atLeastOnce()).publish(any());
        }
    }
    
    @Test
    void testConfigurationUpdate() {
        // 测试配置更新
        when(configManager.getInt("cleanup.interval", 300)).thenReturn(600);
        
        handler.updateConfiguration();
        
        // 验证配置被重新读取
        verify(configManager, atLeastOnce()).getInt("cleanup.interval", 300);
    }
    
    @Test
    void testGetStatistics() {
        Map<String, Object> stats = handler.getStatistics();
        
        assertNotNull(stats);
        assertTrue(stats.containsKey("totalCleanupsPerformed"));
        assertTrue(stats.containsKey("totalEntitiesRemoved"));
        assertTrue(stats.containsKey("averageExecutionTime"));
        assertTrue(stats.containsKey("lastCleanupTime"));
    }
    
    @Test
    void testResetStatistics() {
        // 执行一些清理操作
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            bukkit.when(Bukkit::getWorlds).thenReturn(Arrays.asList(world));
            
            when(strategyManager.executeStrategies(eq(world), any()))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(entity1)));
            
            handler.performCleanup();
        }
        
        // 重置统计
        handler.resetStatistics();
        
        Map<String, Object> stats = handler.getStatistics();
        assertEquals(0, stats.get("totalCleanupsPerformed"));
        assertEquals(0, stats.get("totalEntitiesRemoved"));
    }
    
    @Test
    void testConcurrentCleanup() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        AtomicInteger cleanupCount = new AtomicInteger(0);
        
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            bukkit.when(Bukkit::getWorlds).thenReturn(Arrays.asList(world));
            
            when(strategyManager.executeStrategies(eq(world), any()))
                .thenAnswer(invocation -> {
                    cleanupCount.incrementAndGet();
                    latch.countDown();
                    return CompletableFuture.completedFuture(Arrays.asList(entity1));
                });
            
            // 启动多个并发清理
            for (int i = 0; i < 3; i++) {
                new Thread(() -> handler.performCleanup()).start();
            }
            
            // 等待所有清理完成
            assertTrue(latch.await(5, TimeUnit.SECONDS));
            
            // 验证清理被执行
            assertTrue(cleanupCount.get() > 0);
        }
    }
    
    @Test
    void testMemoryUsageMonitoring() {
        when(configManager.getBoolean("cleanup.monitor-memory", true)).thenReturn(true);
        when(configManager.getDouble("cleanup.memory-threshold", 0.8)).thenReturn(0.8);
        
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            bukkit.when(Bukkit::getWorlds).thenReturn(Arrays.asList(world));
            
            when(strategyManager.executeStrategies(eq(world), any()))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(entity1)));
            
            handler.performCleanup();
            
            // 验证内存监控
            verify(performanceManager, atLeastOnce()).checkMemoryUsage();
        }
    }
    
    @Test
    void testShutdown() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            
            handler.startCleanup();
            handler.shutdown();
            
            // 验证清理被停止
            verify(task).cancel();
            
            // 验证资源被清理
            assertFalse(handler.isRunning());
        }
    }
    
    @Test
    void testIsRunning() {
        assertFalse(handler.isRunning());
        
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            
            handler.startCleanup();
            assertTrue(handler.isRunning());
            
            handler.stopCleanup();
            assertFalse(handler.isRunning());
        }
    }
    
    @Test
    void testCleanupSpecificWorld() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            
            when(strategyManager.executeStrategies(eq(world), any()))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(entity1, entity2)));
            
            CompletableFuture<Integer> result = handler.cleanupWorld(world);
            
            assertNotNull(result);
            assertEquals(2, result.join().intValue());
            
            verify(strategyManager).executeStrategies(eq(world), any());
        }
    }
    
    @Test
    void testCleanupSpecificEntityType() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            bukkit.when(Bukkit::getWorlds).thenReturn(Arrays.asList(world));
            
            when(strategyManager.executeStrategies(eq(world), any()))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(entity1)));
            
            CompletableFuture<Integer> result = handler.cleanupEntityType(EntityType.ITEM);
            
            assertNotNull(result);
            verify(strategyManager).executeStrategies(eq(world), any());
        }
    }
    
    @Test
    void testGetActiveCleanupTasks() {
        Set<String> activeTasks = handler.getActiveCleanupTasks();
        
        assertNotNull(activeTasks);
        // 初始状态应该没有活跃任务
        assertTrue(activeTasks.isEmpty());
    }
    
    @Test
    void testCleanupWithCustomFilter() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            bukkit.when(Bukkit::getWorlds).thenReturn(Arrays.asList(world));
            
            // 自定义过滤器：只清理 ITEM 类型的实体
            when(strategyManager.executeStrategies(eq(world), any()))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(entity1)));
            
            CompletableFuture<Integer> result = handler.cleanupWithFilter(
                entity -> entity.getType() == EntityType.ITEM
            );
            
            assertNotNull(result);
            verify(strategyManager).executeStrategies(eq(world), any());
        }
    }
}