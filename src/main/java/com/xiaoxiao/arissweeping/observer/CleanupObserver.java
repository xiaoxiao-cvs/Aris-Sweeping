package com.xiaoxiao.arissweeping.observer;

/**
 * 清理观察者接口
 * 定义了处理清理事件的方法
 */
public interface CleanupObserver {
    
    /**
     * 处理清理事件
     * 
     * @param event 清理事件
     */
    void onCleanupEvent(CleanupEvent event);
    
    /**
     * 获取观察者名称
     * 
     * @return 观察者名称
     */
    String getObserverName();
    
    /**
     * 检查是否对指定类型的事件感兴趣
     * 
     * @param eventType 事件类型
     * @return 如果感兴趣返回true，否则返回false
     */
    default boolean isInterestedIn(CleanupEvent.CleanupEventType eventType) {
        return true; // 默认对所有事件感兴趣
    }
    
    /**
     * 获取观察者优先级
     * 数值越小优先级越高
     * 
     * @return 优先级
     */
    default int getPriority() {
        return 100; // 默认优先级
    }
    
    /**
     * 检查观察者是否启用
     * 
     * @return 如果启用返回true，否则返回false
     */
    default boolean isEnabled() {
        return true; // 默认启用
    }
    
    /**
     * 观察者初始化
     * 在注册到事件管理器时调用
     */
    default void initialize() {
        // 默认空实现
    }
    
    /**
     * 观察者清理
     * 在从事件管理器移除时调用
     */
    default void cleanup() {
        // 默认空实现
    }
    
    /**
     * 处理事件时发生异常的回调
     * 
     * @param event 导致异常的事件
     * @param exception 异常信息
     */
    default void onEventProcessingError(CleanupEvent event, Exception exception) {
        // 默认空实现，子类可以重写来处理异常
    }
}