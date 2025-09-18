package com.arisweeping.tasks.enums;

/**
 * 任务优先级枚举
 * 
 * 定义不同任务的执行优先级
 */
public enum TaskPriority {
    /** 低优先级 - 常规清理任务 */
    LOW(1),
    
    /** 普通优先级 - 用户触发的清理任务 */
    NORMAL(2),
    
    /** 高优先级 - 紧急清理或系统任务 */
    HIGH(3),
    
    /** 关键优先级 - 撤销操作等关键任务 */
    CRITICAL(4);
    
    private final int level;
    
    TaskPriority(int level) {
        this.level = level;
    }
    
    /**
     * 获取优先级数值，数值越大优先级越高
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * 比较两个优先级的高低
     * 
     * @param other 另一个优先级
     * @return 如果当前优先级更高返回正数，相等返回0，更低返回负数
     */
    public int compareLevel(TaskPriority other) {
        return Integer.compare(this.level, other.level);
    }
    
    /**
     * 检查当前优先级是否高于指定优先级
     */
    public boolean isHigherThan(TaskPriority other) {
        return this.level > other.level;
    }
    
    /**
     * 检查当前优先级是否低于指定优先级
     */
    public boolean isLowerThan(TaskPriority other) {
        return this.level < other.level;
    }
}