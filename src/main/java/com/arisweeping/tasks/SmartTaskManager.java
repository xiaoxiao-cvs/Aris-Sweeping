package com.arisweeping.tasks;

import com.arisweeping.async.AsyncTaskManager;
import com.arisweeping.core.Constants;
import com.arisweeping.tasks.enums.TaskPriority;
import com.arisweeping.tasks.enums.TaskStatus;
import com.arisweeping.tasks.models.TaskExecution;
import com.arisweeping.tasks.models.TaskResult;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 智能任务管理器
 * 
 * 核心任务管理系统，负责任务的提交、执行、监控和协调
 * 提供智能的任务调度和执行机制
 */
public class SmartTaskManager {
    
    // 核心组件
    private final AsyncTaskManager asyncManager;
    private final TaskQueue taskQueue;
    private final UndoManager undoManager;
    private final TaskHistoryManager historyManager;
    
    // 任务状态追踪
    private final Map<UUID, TaskExecution> activeTasks = new ConcurrentHashMap<>();
    private final AtomicLong taskIdGenerator = new AtomicLong(0);
    
    // 执行控制
    private volatile boolean isRunning = false;
    private ScheduledFuture<?> schedulerFuture;
    
    public SmartTaskManager() {
        this.asyncManager = new AsyncTaskManager();
        this.taskQueue = new TaskQueue();
        this.undoManager = new UndoManager(
            Constants.TaskManagement.MAX_UNDO_OPERATIONS,
            Constants.TaskManagement.UNDO_TIMEOUT_MINUTES
        );
        this.historyManager = new TaskHistoryManager();
    }
    
    /**
     * 启动任务管理器
     */
    public void start() {
        if (isRunning) {
            return;
        }
        
        isRunning = true;
        
        // 启动任务处理调度器
        schedulerFuture = asyncManager.scheduleAtFixedRate(
            this::processTaskQueue, 
            0, 
            100, // 100ms间隔
            TimeUnit.MILLISECONDS
        );
        
        System.out.println("SmartTaskManager started");
    }
    
    /**
     * 停止任务管理器
     */
    public void stop() {
        if (!isRunning) {
            return;
        }
        
        isRunning = false;
        
        if (schedulerFuture != null) {
            schedulerFuture.cancel(false);
        }
        
        // 取消所有活跃任务
        for (TaskExecution execution : activeTasks.values()) {
            if (execution.isCancellable()) {
                execution.markCancelled();
                if (execution.getFuture() != null) {
                    execution.getFuture().cancel(true);
                }
            }
        }
        
        // 关闭异步管理器
        asyncManager.shutdown();
        
        System.out.println("SmartTaskManager stopped");
    }
    
    /**
     * 提交清理任务
     * 
     * @param taskType 任务类型
     * @param priority 任务优先级  
     * @param requestData 请求数据
     * @return 任务执行结果的Future
     */
    public CompletableFuture<TaskResult> submitCleaningTask(String taskType, TaskPriority priority, Object requestData) {
        UUID taskId = UUID.randomUUID();
        long sequence = taskIdGenerator.incrementAndGet();
        
        TaskExecution execution = new TaskExecution(taskId, sequence, taskType, priority, requestData);
        
        // 创建任务Future
        CompletableFuture<TaskResult> taskFuture = new CompletableFuture<>();
        execution.setFuture(taskFuture);
        
        // 添加到活跃任务列表
        activeTasks.put(taskId, execution);
        
        // 添加到任务队列
        taskQueue.enqueue(execution);
        
        System.out.println("Submitted cleaning task: " + taskType + " with priority: " + priority);
        
        return taskFuture;
    }
    
    /**
     * 提交撤销任务
     */
    public CompletableFuture<TaskResult> submitUndoTask(UUID originalTaskId) {
        return submitCleaningTask("UNDO_TASK", TaskPriority.CRITICAL, originalTaskId);
    }
    
    /**
     * 取消任务
     */
    public boolean cancelTask(UUID taskId) {
        TaskExecution execution = activeTasks.get(taskId);
        if (execution != null && execution.isCancellable()) {
            execution.markCancelled();
            
            // 取消Future
            if (execution.getFuture() != null) {
                execution.getFuture().complete(TaskResult.cancelled(taskId, execution.getExecutionDurationMs()));
            }
            
            // 从活跃任务中移除
            activeTasks.remove(taskId);
            
            System.out.println("Cancelled task: " + taskId);
            return true;
        }
        return false;
    }
    
    /**
     * 暂停任务
     */
    public boolean pauseTask(UUID taskId) {
        TaskExecution execution = activeTasks.get(taskId);
        if (execution != null && execution.getStatus() == TaskStatus.RUNNING) {
            execution.markPaused();
            System.out.println("Paused task: " + taskId);
            return true;
        }
        return false;
    }
    
    /**
     * 恢复任务
     */
    public boolean resumeTask(UUID taskId) {
        TaskExecution execution = activeTasks.get(taskId);
        if (execution != null && execution.getStatus() == TaskStatus.PAUSED) {
            execution.setStatus(TaskStatus.PENDING);
            taskQueue.enqueue(execution);
            System.out.println("Resumed task: " + taskId);
            return true;
        }
        return false;
    }
    
    /**
     * 获取任务状态
     */
    public TaskExecution getTaskExecution(UUID taskId) {
        return activeTasks.get(taskId);
    }
    
    /**
     * 获取所有活跃任务
     */
    public Map<UUID, TaskExecution> getActiveTasks() {
        return Map.copyOf(activeTasks);
    }
    
    /**
     * 获取任务队列状态
     */
    public String getQueueStatus() {
        return taskQueue.getStatus();
    }
    
    /**
     * 处理任务队列 - 由调度器定期调用
     */
    private void processTaskQueue() {
        if (!isRunning) {
            return;
        }
        
        try {
            TaskExecution nextTask = taskQueue.dequeue();
            if (nextTask != null) {
                executeTask(nextTask);
            }
        } catch (Exception e) {
            System.err.println("Error processing task queue: " + e.getMessage());
        }
    }
    
    /**
     * 执行单个任务
     */
    private void executeTask(TaskExecution execution) {
        if (execution.getStatus() != TaskStatus.PENDING) {
            return;
        }
        
        execution.markStarted();
        
        CompletableFuture<TaskResult> future = asyncManager.submitCoreTask(() -> {
            try {
                // 模拟任务执行
                TaskResult result = performActualTask(execution);
                
                // 执行成功
                execution.markCompleted();
                
                // 记录历史
                historyManager.recordTask(execution, result);
                
                return result;
                
            } catch (Exception e) {
                // 执行失败
                execution.markFailed(e.getMessage());
                TaskResult errorResult = TaskResult.failure(
                    execution.getTaskId(), 
                    e.getMessage(), 
                    e, 
                    execution.getExecutionDurationMs()
                );
                
                // 记录历史
                historyManager.recordTask(execution, errorResult);
                
                return errorResult;
            } finally {
                // 从活跃任务中移除
                activeTasks.remove(execution.getTaskId());
            }
        });
        
        // 将结果传递给原始Future
        future.whenComplete((result, throwable) -> {
            CompletableFuture<TaskResult> originalFuture = execution.getFuture();
            if (originalFuture != null) {
                if (throwable != null) {
                    originalFuture.completeExceptionally(throwable);
                } else {
                    originalFuture.complete(result);
                }
            }
        });
    }
    
    /**
     * 执行实际任务逻辑（占位符方法）
     */
    private TaskResult performActualTask(TaskExecution execution) {
        // 这里是实际任务执行的占位符
        // 在后续开发中会被具体的清理逻辑替代
        
        String taskType = execution.getTaskType();
        System.out.println("Executing task: " + taskType + " (ID: " + execution.getTaskId() + ")");
        
        // 模拟任务处理时间
        try {
            Thread.sleep(100 + (long)(Math.random() * 200)); // 100-300ms随机延迟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Task interrupted", e);
        }
        
        // 模拟处理结果
        long processedItems = (long)(Math.random() * 50) + 1;
        execution.updateProgress(processedItems, processedItems);
        
        return TaskResult.success(
            execution.getTaskId(), 
            execution.getExecutionDurationMs(), 
            processedItems
        );
    }
    
    /**
     * 检查管理器是否正在运行
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * 获取撤销管理器
     */
    public UndoManager getUndoManager() {
        return undoManager;
    }
    
    /**
     * 获取历史管理器
     */
    public TaskHistoryManager getHistoryManager() {
        return historyManager;
    }
    
    /**
     * 获取异步管理器状态
     */
    public String getAsyncManagerStatus() {
        return asyncManager.getPoolStatus();
    }
}