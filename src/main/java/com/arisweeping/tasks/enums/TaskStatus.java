package com.arisweeping.tasks.enums;

/**
 * 任务状态枚举
 * 
 * 定义任务在执行过程中的各种状态
 */
public enum TaskStatus {
    /** 待处理 - 任务已提交但尚未开始执行 */
    PENDING("待处理"),
    
    /** 执行中 - 任务正在执行 */
    RUNNING("执行中"),
    
    /** 已完成 - 任务成功完成 */
    COMPLETED("已完成"),
    
    /** 已失败 - 任务执行失败 */
    FAILED("已失败"),
    
    /** 已取消 - 任务被用户或系统取消 */
    CANCELLED("已取消"),
    
    /** 已暂停 - 任务暂时暂停执行 */
    PAUSED("已暂停"),
    
    /** 超时 - 任务执行超时 */
    TIMEOUT("超时"),
    
    /** 被跳过 - 任务由于某种原因被跳过 */
    SKIPPED("被跳过");
    
    private final String description;
    
    TaskStatus(String description) {
        this.description = description;
    }
    
    /**
     * 获取状态的中文描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 检查任务是否已结束（完成、失败、取消、超时、跳过）
     */
    public boolean isFinished() {
        return this == COMPLETED || this == FAILED || this == CANCELLED || 
               this == TIMEOUT || this == SKIPPED;
    }
    
    /**
     * 检查任务是否正在活跃执行（执行中或暂停）
     */
    public boolean isActive() {
        return this == RUNNING || this == PAUSED;
    }
    
    /**
     * 检查任务是否成功完成
     */
    public boolean isSuccessful() {
        return this == COMPLETED;
    }
    
    /**
     * 检查任务是否失败
     */
    public boolean hasFailed() {
        return this == FAILED || this == TIMEOUT;
    }
    
    /**
     * 检查任务是否可以被取消
     */
    public boolean isCancellable() {
        return this == PENDING || this == RUNNING || this == PAUSED;
    }
    
    /**
     * 检查任务是否可以被恢复
     */
    public boolean isResumable() {
        return this == PAUSED;
    }
    
    @Override
    public String toString() {
        return description;
    }
}