package com.arisweeping.core;

/**
 * 模组常量定义
 * 
 * 包含整个模组使用的各种常量和配置值
 */
public final class Constants {
    
    // 模组基本信息
    public static final String MOD_ID = "arisweeping";
    public static final String MOD_NAME = "Aris Sweeping";
    public static final String MOD_VERSION = "1.0.0-alpha";
    
    // 任务管理相关常量
    public static final class TaskManagement {
        /** 最大撤销操作数量 */
        public static final int MAX_UNDO_OPERATIONS = 10;
        
        /** 撤销操作超时时间（分钟） */
        public static final long UNDO_TIMEOUT_MINUTES = 5;
        
        /** 任务队列最大容量 */
        public static final int MAX_TASK_QUEUE_SIZE = 1000;
        
        /** 任务历史最大保存数量 */
        public static final int MAX_TASK_HISTORY_SIZE = 100;
        
        /** 默认任务超时时间（秒） */
        public static final long DEFAULT_TASK_TIMEOUT_SECONDS = 30;
    }
    
    // 清理系统相关常量
    public static final class Cleaning {
        /** 默认清理检查间隔（tick） */
        public static final int DEFAULT_CLEANING_INTERVAL_TICKS = 200; // 10秒
        
        /** 最大清理半径（方块） */
        public static final double MAX_CLEANING_RADIUS = 128.0;
        
        /** 默认物品存活时间（tick） */
        public static final int DEFAULT_ITEM_AGE_THRESHOLD = 6000; // 5分钟
        
        /** 动物密度检查半径（方块） */
        public static final double DEFAULT_ANIMAL_DENSITY_RADIUS = 16.0;
        
        /** 默认动物密度阈值 */
        public static final int DEFAULT_ANIMAL_DENSITY_THRESHOLD = 10;
    }
    
    // 异步处理相关常量
    public static final class AsyncProcessing {
        /** 核心线程池大小 */
        public static final int CORE_THREAD_POOL_SIZE = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        
        /** 最大线程池大小 */
        public static final int MAX_THREAD_POOL_SIZE = Math.max(4, Runtime.getRuntime().availableProcessors());
        
        /** IO线程池大小 */
        public static final int IO_THREAD_POOL_SIZE = 2;
        
        /** 调度线程池大小 */
        public static final int SCHEDULER_THREAD_POOL_SIZE = 1;
        
        /** 线程保活时间（秒） */
        public static final long THREAD_KEEP_ALIVE_SECONDS = 60;
        
        /** 队列容量 */
        public static final int TASK_QUEUE_CAPACITY = 1000;
    }
    
    // 配置文件相关常量
    public static final class Config {
        /** 配置文件名 */
        public static final String CONFIG_FILE_NAME = "arisweeping.json";
        
        /** 统计数据文件名 */
        public static final String STATS_FILE_NAME = "arisweeping_stats.json";
        
        /** 配置版本 */
        public static final int CONFIG_VERSION = 1;
    }
    
    // GUI相关常量
    public static final class GUI {
        /** 默认GUI宽度 */
        public static final int DEFAULT_GUI_WIDTH = 320;
        
        /** 默认GUI高度 */
        public static final int DEFAULT_GUI_HEIGHT = 240;
        
        /** 组件间距 */
        public static final int COMPONENT_SPACING = 5;
        
        /** 按钮高度 */
        public static final int BUTTON_HEIGHT = 20;
        
        /** 滑块高度 */
        public static final int SLIDER_HEIGHT = 20;
    }
    
    // 网络相关常量
    public static final class Network {
        /** 协议版本 */
        public static final String PROTOCOL_VERSION = "1";
        
        /** 最大数据包大小 */
        public static final int MAX_PACKET_SIZE = 32767;
    }
    
    // 私有构造函数防止实例化
    private Constants() {
        throw new AssertionError("Constants class should not be instantiated");
    }
}