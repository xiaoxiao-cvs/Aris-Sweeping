package com.xiaoxiao.arissweeping.observer;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * 清理事件管理器
 * 负责管理观察者的注册、注销和事件分发
 */
public class CleanupEventManager {
    
    private static final Logger logger = Logger.getLogger(CleanupEventManager.class.getName());
    private static CleanupEventManager instance;
    
    private final Map<CleanupEvent.CleanupEventType, List<CleanupObserver>> observers;
    private final Map<String, CleanupObserver> observersByName;
    private final ExecutorService eventExecutor;
    private final boolean asyncProcessing;
    private final int maxQueueSize;
    private final BlockingQueue<EventTask> eventQueue;
    private volatile boolean running;
    
    // 事件统计
    private final Map<CleanupEvent.CleanupEventType, Long> eventCounts;
    private final Map<String, Long> observerProcessingTimes;
    private final Map<String, Long> observerErrorCounts;
    
    private CleanupEventManager(boolean asyncProcessing, int maxQueueSize) {
        this.observers = new ConcurrentHashMap<>();
        this.observersByName = new ConcurrentHashMap<>();
        this.asyncProcessing = asyncProcessing;
        this.maxQueueSize = maxQueueSize;
        this.eventCounts = new ConcurrentHashMap<>();
        this.observerProcessingTimes = new ConcurrentHashMap<>();
        this.observerErrorCounts = new ConcurrentHashMap<>();
        this.running = true;
        
        if (asyncProcessing) {
            this.eventQueue = new LinkedBlockingQueue<>(maxQueueSize);
            this.eventExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "CleanupEventProcessor");
                t.setDaemon(true);
                return t;
            });
            startEventProcessor();
        } else {
            this.eventQueue = null;
            this.eventExecutor = null;
        }
        
        // 初始化事件类型计数器
        for (CleanupEvent.CleanupEventType type : CleanupEvent.CleanupEventType.values()) {
            eventCounts.put(type, 0L);
        }
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized CleanupEventManager getInstance() {
        if (instance == null) {
            instance = new CleanupEventManager(true, 1000);
        }
        return instance;
    }
    
    /**
     * 创建自定义配置的实例
     */
    public static CleanupEventManager create(boolean asyncProcessing, int maxQueueSize) {
        return new CleanupEventManager(asyncProcessing, maxQueueSize);
    }
    
    /**
     * 注册观察者
     */
    public synchronized void registerObserver(CleanupObserver observer) {
        if (observer == null) {
            throw new IllegalArgumentException("Observer cannot be null");
        }
        
        String name = observer.getObserverName();
        if (observersByName.containsKey(name)) {
            logger.warning("Observer with name '" + name + "' already exists. Replacing...");
        }
        
        observersByName.put(name, observer);
        
        // 为每种事件类型注册观察者
        for (CleanupEvent.CleanupEventType eventType : CleanupEvent.CleanupEventType.values()) {
            if (observer.isInterestedIn(eventType)) {
                observers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(observer);
            }
        }
        
        // 按优先级排序
        for (List<CleanupObserver> observerList : observers.values()) {
            observerList.sort(Comparator.comparingInt(CleanupObserver::getPriority));
        }
        
        // 初始化观察者
        try {
            observer.initialize();
            logger.info("Registered observer: " + name);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize observer: " + name, e);
        }
    }
    
    /**
     * 注销观察者
     */
    public synchronized void unregisterObserver(String observerName) {
        CleanupObserver observer = observersByName.remove(observerName);
        if (observer != null) {
            // 从所有事件类型列表中移除
            for (List<CleanupObserver> observerList : observers.values()) {
                observerList.remove(observer);
            }
            
            // 清理观察者
            try {
                observer.cleanup();
                logger.info("Unregistered observer: " + observerName);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to cleanup observer: " + observerName, e);
            }
        }
    }
    
    /**
     * 注销观察者
     */
    public void unregisterObserver(CleanupObserver observer) {
        if (observer != null) {
            unregisterObserver(observer.getObserverName());
        }
    }
    
    /**
     * 发布事件
     */
    public void publishEvent(CleanupEvent event) {
        if (event == null || !running) {
            return;
        }
        
        // 更新事件计数
        eventCounts.merge(event.getEventType(), 1L, Long::sum);
        
        if (asyncProcessing) {
            // 异步处理
            EventTask task = new EventTask(event);
            if (!eventQueue.offer(task)) {
                logger.warning("Event queue is full, dropping event: " + event.getEventType());
            }
        } else {
            // 同步处理
            processEvent(event);
        }
    }
    
    /**
     * 处理事件
     */
    private void processEvent(CleanupEvent event) {
        List<CleanupObserver> eventObservers = observers.get(event.getEventType());
        if (eventObservers == null || eventObservers.isEmpty()) {
            return;
        }
        
        for (CleanupObserver observer : eventObservers) {
            if (!observer.isEnabled()) {
                continue;
            }
            
            long startTime = System.nanoTime();
            try {
                observer.onCleanupEvent(event);
                
                // 记录处理时间
                long processingTime = System.nanoTime() - startTime;
                observerProcessingTimes.merge(observer.getObserverName(), 
                    processingTime / 1_000_000, Long::sum); // 转换为毫秒
                    
            } catch (Exception e) {
                // 记录错误
                observerErrorCounts.merge(observer.getObserverName(), 1L, Long::sum);
                
                logger.log(Level.SEVERE, 
                    "Error processing event " + event.getEventType() + 
                    " in observer " + observer.getObserverName(), e);
                
                // 通知观察者处理错误
                try {
                    observer.onEventProcessingError(event, e);
                } catch (Exception errorHandlingException) {
                    logger.log(Level.SEVERE, 
                        "Error in error handling for observer " + observer.getObserverName(), 
                        errorHandlingException);
                }
            }
        }
    }
    
    /**
     * 启动事件处理器
     */
    private void startEventProcessor() {
        eventExecutor.submit(() -> {
            while (running) {
                try {
                    EventTask task = eventQueue.take();
                    processEvent(task.event);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error in event processor", e);
                }
            }
        });
    }
    
    /**
     * 获取已注册的观察者
     */
    public Set<String> getRegisteredObservers() {
        return new HashSet<>(observersByName.keySet());
    }
    
    /**
     * 获取观察者
     */
    public CleanupObserver getObserver(String name) {
        return observersByName.get(name);
    }
    
    /**
     * 检查观察者是否已注册
     */
    public boolean isObserverRegistered(String name) {
        return observersByName.containsKey(name);
    }
    
    /**
     * 获取事件统计信息
     */
    public Map<CleanupEvent.CleanupEventType, Long> getEventStatistics() {
        return new HashMap<>(eventCounts);
    }
    
    /**
     * 获取观察者处理时间统计
     */
    public Map<String, Long> getObserverProcessingTimes() {
        return new HashMap<>(observerProcessingTimes);
    }
    
    /**
     * 获取观察者错误统计
     */
    public Map<String, Long> getObserverErrorCounts() {
        return new HashMap<>(observerErrorCounts);
    }
    
    /**
     * 清除统计信息
     */
    public void clearStatistics() {
        eventCounts.replaceAll((k, v) -> 0L);
        observerProcessingTimes.clear();
        observerErrorCounts.clear();
    }
    
    /**
     * 获取队列状态（仅异步模式）
     */
    public QueueStatus getQueueStatus() {
        if (!asyncProcessing || eventQueue == null) {
            return new QueueStatus(0, 0, false);
        }
        
        return new QueueStatus(
            eventQueue.size(),
            maxQueueSize,
            true
        );
    }
    
    /**
     * 关闭事件管理器
     */
    public void shutdown() {
        running = false;
        
        if (eventExecutor != null) {
            eventExecutor.shutdown();
            try {
                if (!eventExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    eventExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                eventExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // 清理所有观察者
        for (CleanupObserver observer : observersByName.values()) {
            try {
                observer.cleanup();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error cleaning up observer: " + observer.getObserverName(), e);
            }
        }
        
        observers.clear();
        observersByName.clear();
        
        logger.info("CleanupEventManager shutdown completed");
    }
    
    /**
     * 事件任务
     */
    private static class EventTask {
        final CleanupEvent event;
        final long timestamp;
        
        EventTask(CleanupEvent event) {
            this.event = event;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * 队列状态
     */
    public static class QueueStatus {
        private final int currentSize;
        private final int maxSize;
        private final boolean asyncMode;
        
        public QueueStatus(int currentSize, int maxSize, boolean asyncMode) {
            this.currentSize = currentSize;
            this.maxSize = maxSize;
            this.asyncMode = asyncMode;
        }
        
        public int getCurrentSize() { return currentSize; }
        public int getMaxSize() { return maxSize; }
        public boolean isAsyncMode() { return asyncMode; }
        public double getUsagePercentage() { 
            return maxSize > 0 ? (double) currentSize / maxSize * 100 : 0; 
        }
        
        @Override
        public String toString() {
            if (!asyncMode) {
                return "Sync mode";
            }
            return String.format("Queue: %d/%d (%.1f%%)", 
                currentSize, maxSize, getUsagePercentage());
        }
    }
}