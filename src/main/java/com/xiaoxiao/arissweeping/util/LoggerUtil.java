package com.xiaoxiao.arissweeping.util;

import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

/**
 * 统一的日志管理工具类
 * 提供标准化的日志打印方法，消除重复的日志实现
 * 遵循"好品味"原则：简洁、直观、避免重复
 */
public class LoggerUtil {
    private static Logger logger;
    private static String pluginPrefix;
    
    /**
     * 初始化日志工具
     */
    public static void init(Plugin plugin) {
        logger = plugin.getLogger();
        pluginPrefix = "[" + plugin.getName() + "]";
    }
    
    /**
     * 记录信息日志
     */
    public static void info(String message) {
        if (logger != null) {
            logger.info(message);
        }
    }
    
    /**
     * 记录带组件标识的信息日志
     */
    public static void info(String component, String message) {
        info(String.format("[%s] %s", component, message));
    }
    
    /**
     * 记录格式化信息日志
     */
    public static void info(String format, Object... args) {
        info(String.format(format, args));
    }
    
    /**
     * 记录带组件标识的格式化信息日志
     */
    public static void info(String component, String format, Object... args) {
        info(component, String.format(format, args));
    }
    
    /**
     * 记录FINE级别日志
     */
    public static void fine(String message) {
        if (logger != null) {
            logger.fine(message);
        }
    }
    
    /**
     * 记录带组件标识的FINE级别日志
     */
    public static void fine(String component, String message) {
        fine(String.format("[%s] %s", component, message));
    }
    
    /**
     * 记录格式化FINE级别日志
     */
    public static void fine(String format, Object... args) {
        fine(String.format(format, args));
    }
    
    /**
     * 记录带组件标识的格式化FINE级别日志
     */
    public static void fine(String component, String format, Object... args) {
        fine(component, String.format(format, args));
    }
    
    /**
     * 记录警告日志
     */
    public static void warning(String message) {
        if (logger != null) {
            logger.warning(message);
        }
    }
    
    /**
     * 记录带组件标识的警告日志
     */
    public static void warning(String component, String message) {
        warning(String.format("[%s] %s", component, message));
    }
    
    /**
     * 记录格式化警告日志
     */
    public static void warning(String format, Object... args) {
        warning(String.format(format, args));
    }
    
    /**
     * 记录带组件标识的格式化警告日志
     */
    public static void warning(String component, String format, Object... args) {
        warning(component, String.format(format, args));
    }
    
    /**
     * 记录警告日志 (warn方法别名)
     */
    public static void warn(String message) {
        warning(message);
    }
    
    /**
     * 记录带组件标识的警告日志 (warn方法别名)
     */
    public static void warn(String component, String message) {
        warning(component, message);
    }
    
    /**
     * 记录格式化警告日志 (warn方法别名)
     */
    public static void warn(String format, Object... args) {
        warning(format, args);
    }
    
    /**
     * 记录带组件标识的格式化警告日志 (warn方法别名)
     */
    public static void warn(String component, String format, Object... args) {
        warning(component, format, args);
    }
    
    /**
     * 记录错误日志 (error方法别名)
     */
    public static void error(String message) {
        severe(message);
    }
    
    /**
     * 记录带组件标识的错误日志 (error方法别名)
     */
    public static void error(String component, String message) {
        severe(component, message);
    }
    
    /**
     * 记录格式化错误日志 (error方法别名)
     */
    public static void error(String format, Object... args) {
        severe(format, args);
    }
    
    /**
     * 记录带组件标识的格式化错误日志 (error方法别名)
     */
    public static void error(String component, String format, Object... args) {
        severe(component, format, args);
    }
    
    /**
     * 记录异常错误日志 (error方法别名)
     */
    public static void error(String message, Throwable throwable) {
        severe(message, throwable);
    }
    
    /**
     * 记录带组件标识的异常错误日志 (error方法别名)
     */
    public static void error(String component, String message, Throwable throwable) {
        severe(component, message, throwable);
    }
    
    /**
     * 记录严重错误日志
     */
    public static void severe(String message) {
        if (logger != null) {
            logger.severe(message);
        }
    }
    
    /**
     * 记录带组件标识的严重错误日志
     */
    public static void severe(String component, String message) {
        severe(String.format("[%s] %s", component, message));
    }
    
    /**
     * 记录格式化严重错误日志
     */
    public static void severe(String format, Object... args) {
        severe(String.format(format, args));
    }
    
    /**
     * 记录带组件标识的格式化严重错误日志
     */
    public static void severe(String component, String format, Object... args) {
        severe(component, String.format(format, args));
    }
    
    /**
     * 记录异常信息
     */
    public static void severe(String message, Throwable throwable) {
        severe(message);
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }
    
    /**
     * 记录带组件标识的异常信息
     */
    public static void severe(String component, String message, Throwable throwable) {
        severe(component, message);
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }
    
    /**
     * 调试模式日志（仅在调试模式下输出）
     */
    public static void debug(boolean debugMode, String message) {
        if (debugMode) {
            info("[DEBUG] " + message);
        }
    }
    
    /**
     * 带组件标识的调试模式日志
     */
    public static void debug(boolean debugMode, String component, String message) {
        if (debugMode) {
            info(component, "[DEBUG] " + message);
        }
    }
    
    /**
     * 格式化调试模式日志
     */
    public static void debug(boolean debugMode, String format, Object... args) {
        if (debugMode) {
            info("[DEBUG] " + String.format(format, args));
        }
    }
    
    /**
     * 带组件标识的格式化调试模式日志
     */
    public static void debug(boolean debugMode, String component, String format, Object... args) {
        if (debugMode) {
            info(component, "[DEBUG] " + String.format(format, args));
        }
    }
    
    /**
     * 记录任务状态日志
     */
    public static void taskStatus(String taskName, String status) {
        info("Task", "%s: %s", taskName, status);
    }
    
    /**
     * 记录清理统计日志
     */
    public static void cleanupStats(String component, int processed, int removed, long duration) {
        info(component, "清理完成 - 处理: %d, 移除: %d, 耗时: %dms", processed, removed, duration);
    }
    
    /**
     * 记录性能监控日志
     */
    public static void performance(String component, String metric, double value) {
        info(component, "性能指标 - %s: %.2f", metric, value);
    }
    
    /**
     * 记录违规检测日志
     */
    public static void violation(String component, String location, int count, String action) {
        warning(component, "违规检测 - 位置: %s, 数量: %d, 操作: %s", location, count, action);
    }
}