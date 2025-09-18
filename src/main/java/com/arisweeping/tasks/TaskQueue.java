package com.arisweeping.tasks;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.arisweeping.core.Constants;
import com.arisweeping.tasks.enums.TaskPriority;
import com.arisweeping.tasks.enums.TaskStatus;
import com.arisweeping.tasks.models.TaskExecution;

/**
 * 任务队列管理器
 * 
 * 负责管理优先级任务队列，提供线程安全的任务排队和出队操作
 */
public class TaskQueue {
    
    private final PriorityBlockingQueue<TaskExecution> queue;
    private final AtomicInteger queueSize = new AtomicInteger(0);
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    // 队列状态
    private volatile boolean isPaused = false;
    private volatile boolean isShutdown = false;
    
    public TaskQueue() {
        // 使用自定义比较器创建优先级队列
        this.queue = new PriorityBlockingQueue<>(
            Constants.TaskManagement.MAX_TASK_QUEUE_SIZE,
            this::compareTaskPriority
        );
    }
    
    /**
     * 任务优先级比较器
     * 按照优先级和提交时间排序
     */
    private int compareTaskPriority(TaskExecution t1, TaskExecution t2) {
        // 首先按优先级排序（高优先级在前）
        int priorityCompare = t2.getPriority().compareLevel(t1.getPriority());
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        
        // 优先级相同时按提交时间排序（早提交的在前）
        return Long.compare(t1.getSequence(), t2.getSequence());
    }
    
    /**
     * 将任务加入队列
     */
    public boolean enqueue(TaskExecution execution) {
        if (isShutdown) {
            return false;
        }
        
        if (execution == null || execution.getStatus() != TaskStatus.PENDING) {
            return false;
        }
        
        lock.writeLock().lock();
        try {
            // 检查队列容量
            if (queueSize.get() >= Constants.TaskManagement.MAX_TASK_QUEUE_SIZE) {
                System.err.println("Task queue is full, rejecting task: " + execution.getTaskId());
                return false;
            }
            
            boolean added = queue.offer(execution);
            if (added) {
                queueSize.incrementAndGet();
                System.out.println("Enqueued task: " + execution.getTaskId() + 
                                 " with priority: " + execution.getPriority() + 
                                 ", queue size: " + queueSize.get());
            }
            return added;
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 从队列中取出下一个任务
     */
    public TaskExecution dequeue() {
        if (isShutdown || isPaused) {
            return null;
        }
        
        lock.readLock().lock();
        try {
            TaskExecution execution = queue.poll();
            if (execution != null) {
                queueSize.decrementAndGet();
                
                // 验证任务状态
                if (execution.getStatus() != TaskStatus.PENDING) {
                    System.out.println("Skipping task with invalid status: " + execution.getTaskId() + 
                                     ", status: " + execution.getStatus());
                    return dequeue(); // 递归查找下一个有效任务
                }
                
                System.out.println("Dequeued task: " + execution.getTaskId() + 
                                 ", remaining queue size: " + queueSize.get());
            }
            return execution;
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 查看队列头部任务（不移除）
     */
    public TaskExecution peek() {
        lock.readLock().lock();
        try {
            return queue.peek();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 移除指定任务
     */
    public boolean removeTask(TaskExecution execution) {
        if (isShutdown) {
            return false;
        }
        
        lock.writeLock().lock();
        try {
            boolean removed = queue.remove(execution);
            if (removed) {
                queueSize.decrementAndGet();
                System.out.println("Removed task from queue: " + execution.getTaskId());
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 暂停队列处理
     */
    public void pause() {
        isPaused = true;
        System.out.println("Task queue paused");
    }
    
    /**
     * 恢复队列处理
     */
    public void resume() {
        isPaused = false;
        System.out.println("Task queue resumed");
    }
    
    /**
     * 清空队列
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            int clearedCount = queue.size();
            queue.clear();
            queueSize.set(0);
            System.out.println("Cleared " + clearedCount + " tasks from queue");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 关闭队列
     */
    public void shutdown() {
        isShutdown = true;
        clear();
        System.out.println("Task queue shutdown");
    }
    
    /**
     * 获取队列大小
     */
    public int size() {
        return queueSize.get();
    }
    
    /**
     * 检查队列是否为空
     */
    public boolean isEmpty() {
        return queueSize.get() == 0;
    }
    
    /**
     * 检查队列是否已暂停
     */
    public boolean isPaused() {
        return isPaused;
    }
    
    /**
     * 检查队列是否已关闭
     */
    public boolean isShutdown() {
        return isShutdown;
    }
    
    /**
     * 获取按优先级分组的统计信息
     */
    public String getPriorityStats() {
        lock.readLock().lock();
        try {
            int[] counts = new int[TaskPriority.values().length];
            
            for (TaskExecution execution : queue) {
                TaskPriority priority = execution.getPriority();
                counts[priority.ordinal()]++;
            }
            
            StringBuilder stats = new StringBuilder();
            stats.append("Queue priority stats: ");
            for (TaskPriority priority : TaskPriority.values()) {
                int count = counts[priority.ordinal()];
                if (count > 0) {
                    stats.append(priority.name()).append("=").append(count).append(" ");
                }
            }
            
            return stats.toString().trim();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 获取队列状态信息
     */
    public String getStatus() {
        return String.format("TaskQueue[size=%d, paused=%s, shutdown=%s, capacity=%d]",
                           queueSize.get(), isPaused, isShutdown, 
                           Constants.TaskManagement.MAX_TASK_QUEUE_SIZE);
    }
    
    @Override
    public String toString() {
        return getStatus() + " - " + getPriorityStats();
    }
}