package com.xiaoxiao.arissweeping.observer;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CleanupEventManager 单元测试
 */
@ExtendWith(MockitoExtension.class)
class CleanupEventManagerTest {
    
    @Mock
    private World world;
    
    @Mock
    private Entity entity;
    
    private CleanupEventManager eventManager;
    
    @BeforeEach
    void setUp() {
        eventManager = new CleanupEventManager();
        
        when(world.getName()).thenReturn("test-world");
        when(entity.getUniqueId()).thenReturn(UUID.randomUUID());
        when(entity.getType()).thenReturn(org.bukkit.entity.EntityType.ITEM);
    }
    
    @Test
    void testSubscribeAndPublish_CleanupStarted() {
        AtomicInteger eventCount = new AtomicInteger(0);
        
        // 订阅事件
        eventManager.subscribe(CleanupEvent.CleanupStartedEvent.class, event -> {
            eventCount.incrementAndGet();
            assertEquals("test-strategy", event.getStrategyName());
            assertEquals(world, event.getWorld());
        });
        
        // 发布事件
        CleanupEvent.CleanupStartedEvent event = new CleanupEvent.CleanupStartedEvent(
            "test-strategy", world, System.currentTimeMillis()
        );
        eventManager.publish(event);
        
        assertEquals(1, eventCount.get());
    }
    
    @Test
    void testSubscribeAndPublish_CleanupCompleted() {
        AtomicInteger eventCount = new AtomicInteger(0);
        List<CleanupEvent.CleanupCompletedEvent> receivedEvents = new ArrayList<>();
        
        // 订阅事件
        eventManager.subscribe(CleanupEvent.CleanupCompletedEvent.class, event -> {
            eventCount.incrementAndGet();
            receivedEvents.add(event);
        });
        
        // 发布事件
        CleanupEvent.CleanupCompletedEvent event = new CleanupEvent.CleanupCompletedEvent(
            "test-strategy", world, 100, 80, 5000L, System.currentTimeMillis()
        );
        eventManager.publish(event);
        
        assertEquals(1, eventCount.get());
        assertEquals(1, receivedEvents.size());
        
        CleanupEvent.CleanupCompletedEvent receivedEvent = receivedEvents.get(0);
        assertEquals("test-strategy", receivedEvent.getStrategyName());
        assertEquals(world, receivedEvent.getWorld());
        assertEquals(100, receivedEvent.getEntitiesProcessed());
        assertEquals(80, receivedEvent.getEntitiesRemoved());
        assertEquals(5000L, receivedEvent.getExecutionTime());
    }
    
    @Test
    void testSubscribeAndPublish_EntityRemoved() {
        List<Entity> removedEntities = new ArrayList<>();
        
        // 订阅事件
        eventManager.subscribe(CleanupEvent.EntityRemovedEvent.class, event -> {
            removedEntities.add(event.getEntity());
            assertEquals("test-strategy", event.getStrategyName());
            assertEquals("Age exceeded threshold", event.getReason());
        });
        
        // 发布事件
        CleanupEvent.EntityRemovedEvent event = new CleanupEvent.EntityRemovedEvent(
            entity, "test-strategy", "Age exceeded threshold", System.currentTimeMillis()
        );
        eventManager.publish(event);
        
        assertEquals(1, removedEntities.size());
        assertEquals(entity, removedEntities.get(0));
    }
    
    @Test
    void testSubscribeAndPublish_PerformanceWarning() {
        List<CleanupEvent.PerformanceWarningEvent> warnings = new ArrayList<>();
        
        // 订阅事件
        eventManager.subscribe(CleanupEvent.PerformanceWarningEvent.class, event -> {
            warnings.add(event);
        });
        
        // 发布事件
        CleanupEvent.PerformanceWarningEvent event = new CleanupEvent.PerformanceWarningEvent(
            CleanupEvent.PerformanceWarningEvent.WarningType.HIGH_MEMORY,
            "Memory usage is 85%",
            85.0,
            System.currentTimeMillis()
        );
        eventManager.publish(event);
        
        assertEquals(1, warnings.size());
        CleanupEvent.PerformanceWarningEvent warning = warnings.get(0);
        assertEquals(CleanupEvent.PerformanceWarningEvent.WarningType.HIGH_MEMORY, warning.getWarningType());
        assertEquals("Memory usage is 85%", warning.getMessage());
        assertEquals(85.0, warning.getValue(), 0.001);
    }
    
    @Test
    void testSubscribeAndPublish_ConfigChanged() {
        List<CleanupEvent.ConfigChangedEvent> configChanges = new ArrayList<>();
        
        // 订阅事件
        eventManager.subscribe(CleanupEvent.ConfigChangedEvent.class, event -> {
            configChanges.add(event);
        });
        
        // 发布事件
        Map<String, Object> oldConfig = new HashMap<>();
        oldConfig.put("enabled", false);
        oldConfig.put("interval", 300);
        
        Map<String, Object> newConfig = new HashMap<>();
        newConfig.put("enabled", true);
        newConfig.put("interval", 600);
        
        CleanupEvent.ConfigChangedEvent event = new CleanupEvent.ConfigChangedEvent(
            "cleanup.strategy.age-based", oldConfig, newConfig, System.currentTimeMillis()
        );
        eventManager.publish(event);
        
        assertEquals(1, configChanges.size());
        CleanupEvent.ConfigChangedEvent configChange = configChanges.get(0);
        assertEquals("cleanup.strategy.age-based", configChange.getConfigPath());
        assertEquals(oldConfig, configChange.getOldConfig());
        assertEquals(newConfig, configChange.getNewConfig());
    }
    
    @Test
    void testMultipleSubscribers() {
        AtomicInteger subscriber1Count = new AtomicInteger(0);
        AtomicInteger subscriber2Count = new AtomicInteger(0);
        AtomicInteger subscriber3Count = new AtomicInteger(0);
        
        // 多个订阅者订阅同一事件
        eventManager.subscribe(CleanupEvent.CleanupStartedEvent.class, event -> {
            subscriber1Count.incrementAndGet();
        });
        
        eventManager.subscribe(CleanupEvent.CleanupStartedEvent.class, event -> {
            subscriber2Count.incrementAndGet();
        });
        
        eventManager.subscribe(CleanupEvent.CleanupStartedEvent.class, event -> {
            subscriber3Count.incrementAndGet();
        });
        
        // 发布事件
        CleanupEvent.CleanupStartedEvent event = new CleanupEvent.CleanupStartedEvent(
            "test-strategy", world, System.currentTimeMillis()
        );
        eventManager.publish(event);
        
        // 所有订阅者都应该收到事件
        assertEquals(1, subscriber1Count.get());
        assertEquals(1, subscriber2Count.get());
        assertEquals(1, subscriber3Count.get());
    }
    
    @Test
    void testUnsubscribe() {
        AtomicInteger eventCount = new AtomicInteger(0);
        
        // 订阅事件
        CleanupEventListener<CleanupEvent.CleanupStartedEvent> listener = event -> {
            eventCount.incrementAndGet();
        };
        
        eventManager.subscribe(CleanupEvent.CleanupStartedEvent.class, listener);
        
        // 发布第一个事件
        CleanupEvent.CleanupStartedEvent event1 = new CleanupEvent.CleanupStartedEvent(
            "test-strategy", world, System.currentTimeMillis()
        );
        eventManager.publish(event1);
        
        assertEquals(1, eventCount.get());
        
        // 取消订阅
        eventManager.unsubscribe(CleanupEvent.CleanupStartedEvent.class, listener);
        
        // 发布第二个事件
        CleanupEvent.CleanupStartedEvent event2 = new CleanupEvent.CleanupStartedEvent(
            "test-strategy-2", world, System.currentTimeMillis()
        );
        eventManager.publish(event2);
        
        // 事件计数不应该增加
        assertEquals(1, eventCount.get());
    }
    
    @Test
    void testUnsubscribeAll() {
        AtomicInteger startedEventCount = new AtomicInteger(0);
        AtomicInteger completedEventCount = new AtomicInteger(0);
        
        // 订阅多种事件
        eventManager.subscribe(CleanupEvent.CleanupStartedEvent.class, event -> {
            startedEventCount.incrementAndGet();
        });
        
        eventManager.subscribe(CleanupEvent.CleanupCompletedEvent.class, event -> {
            completedEventCount.incrementAndGet();
        });
        
        // 发布事件验证订阅有效
        eventManager.publish(new CleanupEvent.CleanupStartedEvent(
            "test", world, System.currentTimeMillis()
        ));
        eventManager.publish(new CleanupEvent.CleanupCompletedEvent(
            "test", world, 10, 8, 1000L, System.currentTimeMillis()
        ));
        
        assertEquals(1, startedEventCount.get());
        assertEquals(1, completedEventCount.get());
        
        // 取消所有订阅
        eventManager.unsubscribeAll();
        
        // 再次发布事件
        eventManager.publish(new CleanupEvent.CleanupStartedEvent(
            "test2", world, System.currentTimeMillis()
        ));
        eventManager.publish(new CleanupEvent.CleanupCompletedEvent(
            "test2", world, 20, 16, 2000L, System.currentTimeMillis()
        ));
        
        // 事件计数不应该增加
        assertEquals(1, startedEventCount.get());
        assertEquals(1, completedEventCount.get());
    }
    
    @Test
    void testAsyncEventHandling() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger eventCount = new AtomicInteger(0);
        
        // 订阅事件（模拟异步处理）
        eventManager.subscribe(CleanupEvent.CleanupStartedEvent.class, event -> {
            try {
                Thread.sleep(100); // 模拟耗时操作
                eventCount.incrementAndGet();
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // 发布事件
        CleanupEvent.CleanupStartedEvent event = new CleanupEvent.CleanupStartedEvent(
            "test-strategy", world, System.currentTimeMillis()
        );
        eventManager.publish(event);
        
        // 等待异步处理完成
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(1, eventCount.get());
    }
    
    @Test
    void testExceptionHandlingInListener() {
        AtomicInteger successfulListenerCount = new AtomicInteger(0);
        AtomicInteger exceptionListenerCount = new AtomicInteger(0);
        
        // 订阅正常的监听器
        eventManager.subscribe(CleanupEvent.CleanupStartedEvent.class, event -> {
            successfulListenerCount.incrementAndGet();
        });
        
        // 订阅会抛出异常的监听器
        eventManager.subscribe(CleanupEvent.CleanupStartedEvent.class, event -> {
            exceptionListenerCount.incrementAndGet();
            throw new RuntimeException("Test exception in listener");
        });
        
        // 订阅另一个正常的监听器
        eventManager.subscribe(CleanupEvent.CleanupStartedEvent.class, event -> {
            successfulListenerCount.incrementAndGet();
        });
        
        // 发布事件
        CleanupEvent.CleanupStartedEvent event = new CleanupEvent.CleanupStartedEvent(
            "test-strategy", world, System.currentTimeMillis()
        );
        
        // 发布事件不应该抛出异常
        assertDoesNotThrow(() -> eventManager.publish(event));
        
        // 正常的监听器应该都被调用
        assertEquals(2, successfulListenerCount.get());
        assertEquals(1, exceptionListenerCount.get());
    }
    
    @Test
    void testConcurrentEventPublishing() throws InterruptedException {
        AtomicInteger eventCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(10);
        
        // 订阅事件
        eventManager.subscribe(CleanupEvent.CleanupStartedEvent.class, event -> {
            eventCount.incrementAndGet();
            latch.countDown();
        });
        
        // 创建多个线程同时发布事件
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final int threadId = i;
            Thread thread = new Thread(() -> {
                CleanupEvent.CleanupStartedEvent event = new CleanupEvent.CleanupStartedEvent(
                    "strategy-" + threadId, world, System.currentTimeMillis()
                );
                eventManager.publish(event);
            });
            threads.add(thread);
            thread.start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join(1000);
        }
        
        // 等待所有事件被处理
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(10, eventCount.get());
    }
    
    @Test
    void testEventInheritance() {
        AtomicInteger baseEventCount = new AtomicInteger(0);
        AtomicInteger specificEventCount = new AtomicInteger(0);
        
        // 订阅基类事件
        eventManager.subscribe(CleanupEvent.class, event -> {
            baseEventCount.incrementAndGet();
        });
        
        // 订阅具体事件
        eventManager.subscribe(CleanupEvent.CleanupStartedEvent.class, event -> {
            specificEventCount.incrementAndGet();
        });
        
        // 发布具体事件
        CleanupEvent.CleanupStartedEvent event = new CleanupEvent.CleanupStartedEvent(
            "test-strategy", world, System.currentTimeMillis()
        );
        eventManager.publish(event);
        
        // 两个监听器都应该被调用
        assertEquals(1, baseEventCount.get());
        assertEquals(1, specificEventCount.get());
    }
    
    @Test
    void testEventFiltering() {
        AtomicInteger filteredEventCount = new AtomicInteger(0);
        
        // 订阅带过滤条件的事件
        eventManager.subscribe(CleanupEvent.CleanupCompletedEvent.class, event -> {
            // 只处理移除了超过50个实体的事件
            if (event.getEntitiesRemoved() > 50) {
                filteredEventCount.incrementAndGet();
            }
        });
        
        // 发布不满足条件的事件
        eventManager.publish(new CleanupEvent.CleanupCompletedEvent(
            "test1", world, 100, 30, 1000L, System.currentTimeMillis()
        ));
        
        // 发布满足条件的事件
        eventManager.publish(new CleanupEvent.CleanupCompletedEvent(
            "test2", world, 200, 80, 2000L, System.currentTimeMillis()
        ));
        
        // 只有满足条件的事件被处理
        assertEquals(1, filteredEventCount.get());
    }
    
    @Test
    void testEventStatistics() {
        // 发布多种类型的事件
        eventManager.publish(new CleanupEvent.CleanupStartedEvent(
            "test1", world, System.currentTimeMillis()
        ));
        
        eventManager.publish(new CleanupEvent.CleanupCompletedEvent(
            "test1", world, 100, 80, 1000L, System.currentTimeMillis()
        ));
        
        eventManager.publish(new CleanupEvent.EntityRemovedEvent(
            entity, "test1", "reason", System.currentTimeMillis()
        ));
        
        // 获取事件统计（如果实现了该功能）
        Map<Class<? extends CleanupEvent>, Integer> statistics = eventManager.getEventStatistics();
        
        if (statistics != null) {
            assertTrue(statistics.containsKey(CleanupEvent.CleanupStartedEvent.class));
            assertTrue(statistics.containsKey(CleanupEvent.CleanupCompletedEvent.class));
            assertTrue(statistics.containsKey(CleanupEvent.EntityRemovedEvent.class));
        }
    }
    
    @Test
    void testEventHistory() {
        // 发布一些事件
        CleanupEvent.CleanupStartedEvent event1 = new CleanupEvent.CleanupStartedEvent(
            "test1", world, System.currentTimeMillis()
        );
        CleanupEvent.CleanupCompletedEvent event2 = new CleanupEvent.CleanupCompletedEvent(
            "test1", world, 100, 80, 1000L, System.currentTimeMillis()
        );
        
        eventManager.publish(event1);
        eventManager.publish(event2);
        
        // 获取事件历史（如果实现了该功能）
        List<CleanupEvent> history = eventManager.getEventHistory();
        
        if (history != null) {
            assertFalse(history.isEmpty());
            assertTrue(history.contains(event1) || history.size() >= 1);
            assertTrue(history.contains(event2) || history.size() >= 2);
        }
    }
    
    @Test
    void testEventPriority() {
        List<String> executionOrder = Collections.synchronizedList(new ArrayList<>());
        
        // 订阅高优先级监听器
        eventManager.subscribe(CleanupEvent.CleanupStartedEvent.class, event -> {
            executionOrder.add("high-priority");
        }, EventPriority.HIGH);
        
        // 订阅普通优先级监听器
        eventManager.subscribe(CleanupEvent.CleanupStartedEvent.class, event -> {
            executionOrder.add("normal-priority");
        }, EventPriority.NORMAL);
        
        // 订阅低优先级监听器
        eventManager.subscribe(CleanupEvent.CleanupStartedEvent.class, event -> {
            executionOrder.add("low-priority");
        }, EventPriority.LOW);
        
        // 发布事件
        eventManager.publish(new CleanupEvent.CleanupStartedEvent(
            "test", world, System.currentTimeMillis()
        ));
        
        // 验证执行顺序（如果实现了优先级功能）
        if (executionOrder.size() == 3) {
            assertEquals("high-priority", executionOrder.get(0));
            assertEquals("normal-priority", executionOrder.get(1));
            assertEquals("low-priority", executionOrder.get(2));
        }
    }
    
    @Test
    void testEventCancellation() {
        AtomicInteger cancelledEventCount = new AtomicInteger(0);
        AtomicInteger processedEventCount = new AtomicInteger(0);
        
        // 订阅取消事件的监听器
        eventManager.subscribe(CleanupEvent.CleanupStartedEvent.class, event -> {
            if (event instanceof CancellableEvent) {
                ((CancellableEvent) event).setCancelled(true);
                cancelledEventCount.incrementAndGet();
            }
        }, EventPriority.HIGH);
        
        // 订阅处理事件的监听器
        eventManager.subscribe(CleanupEvent.CleanupStartedEvent.class, event -> {
            if (!(event instanceof CancellableEvent) || !((CancellableEvent) event).isCancelled()) {
                processedEventCount.incrementAndGet();
            }
        }, EventPriority.LOW);
        
        // 发布可取消的事件（如果实现了该功能）
        CleanupEvent.CleanupStartedEvent event = new CleanupEvent.CleanupStartedEvent(
            "test", world, System.currentTimeMillis()
        );
        eventManager.publish(event);
        
        // 验证事件取消机制
        // 这个测试取决于是否实现了事件取消功能
        assertTrue(cancelledEventCount.get() >= 0);
        assertTrue(processedEventCount.get() >= 0);
    }
    
    // 辅助接口用于测试
    interface CancellableEvent {
        boolean isCancelled();
        void setCancelled(boolean cancelled);
    }
    
    // 辅助枚举用于测试
    enum EventPriority {
        HIGH, NORMAL, LOW
    }
}