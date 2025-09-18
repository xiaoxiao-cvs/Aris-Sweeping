package com.arisweeping.core;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ArisSweeping 统一日志系统
 * 
 * 提供美观的彩色终端输出、精确时间戳和详细的启动日志
 */
public class ArisLogger {
    
    // ANSI 颜色代码
    private static final String RESET = "\u001B[0m";
    private static final String BLACK = "\u001B[30m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";
    
    // 背景色
    private static final String BG_RED = "\u001B[41m";
    private static final String BG_GREEN = "\u001B[42m";
    private static final String BG_YELLOW = "\u001B[43m";
    private static final String BG_BLUE = "\u001B[44m";
    
    // 字体样式
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";
    private static final String UNDERLINE = "\u001B[4m";
    
    // 时间格式化器
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    // 模组名称标识
    private static final String MOD_TAG = BOLD + CYAN + "[ArisSweeping]" + RESET;
    
    // 日志级别枚举
    public enum Level {
        DEBUG(CYAN + "DEBUG" + RESET, CYAN),
        INFO(GREEN + "INFO " + RESET, GREEN),
        WARN(YELLOW + "WARN " + RESET, YELLOW),
        ERROR(RED + "ERROR" + RESET, RED),
        FATAL(BG_RED + WHITE + "FATAL" + RESET, RED);
        
        public final String coloredName;
        public final String color;
        
        Level(String coloredName, String color) {
            this.coloredName = coloredName;
            this.color = color;
        }
    }
    
    /**
     * 输出调试信息
     */
    public static void debug(String message) {
        log(Level.DEBUG, message, null);
    }
    
    /**
     * 输出调试信息（带格式化）
     */
    public static void debug(String format, Object... args) {
        log(Level.DEBUG, String.format(format, args), null);
    }
    
    /**
     * 输出普通信息
     */
    public static void info(String message) {
        log(Level.INFO, message, null);
    }
    
    /**
     * 输出普通信息（带格式化）
     */
    public static void info(String format, Object... args) {
        log(Level.INFO, String.format(format, args), null);
    }
    
    /**
     * 输出警告信息
     */
    public static void warn(String message) {
        log(Level.WARN, message, null);
    }
    
    /**
     * 输出警告信息（带格式化）
     */
    public static void warn(String format, Object... args) {
        log(Level.WARN, String.format(format, args), null);
    }
    
    /**
     * 输出错误信息
     */
    public static void error(String message) {
        log(Level.ERROR, message, null);
    }
    
    /**
     * 输出错误信息（带异常）
     */
    public static void error(String message, Throwable throwable) {
        log(Level.ERROR, message, throwable);
    }
    
    /**
     * 输出错误信息（带格式化）
     */
    public static void error(String format, Object... args) {
        log(Level.ERROR, String.format(format, args), null);
    }
    
    /**
     * 输出严重错误信息
     */
    public static void fatal(String message) {
        log(Level.FATAL, message, null);
    }
    
    /**
     * 输出严重错误信息（带异常）
     */
    public static void fatal(String message, Throwable throwable) {
        log(Level.FATAL, message, throwable);
    }
    
    /**
     * 核心日志输出方法
     */
    private static void log(Level level, String message, Throwable throwable) {
        String timestamp = DIM + LocalDateTime.now().format(TIME_FORMATTER) + RESET;
        String levelTag = "[" + level.coloredName + "]";
        String threadName = DIM + "[" + Thread.currentThread().getName() + "]" + RESET;
        
        StringBuilder logLine = new StringBuilder();
        logLine.append(timestamp).append(" ");
        logLine.append(levelTag).append(" ");
        logLine.append(MOD_TAG).append(" ");
        logLine.append(threadName).append(" ");
        logLine.append(level.color).append(message).append(RESET);
        
        // 根据日志级别选择输出流
        if (level == Level.ERROR || level == Level.FATAL) {
            System.err.println(logLine.toString());
        } else {
            System.out.println(logLine.toString());
        }
        
        // 输出异常堆栈
        if (throwable != null) {
            String exceptionColor = level == Level.FATAL ? RED : YELLOW;
            System.err.println(exceptionColor + "Exception: " + throwable.getClass().getSimpleName() + 
                             ": " + throwable.getMessage() + RESET);
            
            StackTraceElement[] stackTrace = throwable.getStackTrace();
            for (int i = 0; i < Math.min(stackTrace.length, 10); i++) {
                StackTraceElement element = stackTrace[i];
                System.err.println(DIM + "  at " + element.toString() + RESET);
            }
            
            if (stackTrace.length > 10) {
                System.err.println(DIM + "  ... " + (stackTrace.length - 10) + " more" + RESET);
            }
        }
    }
    
    /**
     * 输出美观的启动横幅
     */
    public static void printStartupBanner() {
        String[] banner = {
            "",
            BOLD + CYAN + "╔══════════════════════════════════════════════════════════════╗" + RESET,
            BOLD + CYAN + "║" + RESET + BOLD + "                    ArisSweeping v1.0.0-alpha                " + BOLD + CYAN + "║" + RESET,
            BOLD + CYAN + "║" + RESET + "                  智能实体清理与管理模组                        " + BOLD + CYAN + "║" + RESET,
            BOLD + CYAN + "╠══════════════════════════════════════════════════════════════╣" + RESET,
            BOLD + CYAN + "║" + RESET + GREEN + " ✓ " + RESET + "异步清理系统                                               " + BOLD + CYAN + "║" + RESET,
            BOLD + CYAN + "║" + RESET + GREEN + " ✓ " + RESET + "智能任务管理                                               " + BOLD + CYAN + "║" + RESET,
            BOLD + CYAN + "║" + RESET + GREEN + " ✓ " + RESET + "撤销操作支持                                               " + BOLD + CYAN + "║" + RESET,
            BOLD + CYAN + "║" + RESET + GREEN + " ✓ " + RESET + "可视化配置界面                                             " + BOLD + CYAN + "║" + RESET,
            BOLD + CYAN + "║" + RESET + GREEN + " ✓ " + RESET + "高性能多线程架构                                           " + BOLD + CYAN + "║" + RESET,
            BOLD + CYAN + "╠══════════════════════════════════════════════════════════════╣" + RESET,
            BOLD + CYAN + "║" + RESET + "  Minecraft: " + YELLOW + "1.20.1" + RESET + "  │  Forge: " + YELLOW + "47.2.x" + RESET + "  │  Java: " + YELLOW + "17+" + RESET + "    " + BOLD + CYAN + "║" + RESET,
            BOLD + CYAN + "╚══════════════════════════════════════════════════════════════╝" + RESET,
            ""
        };
        
        for (String line : banner) {
            System.out.println(line);
        }
    }
    
    /**
     * 输出详细的启动阶段信息
     */
    public static void logStartupPhase(String phase, String description) {
        String phaseTag = BOLD + BG_BLUE + WHITE + " " + phase + " " + RESET;
        String arrow = BOLD + BLUE + " ➤ " + RESET;
        info("%s%s%s", phaseTag, arrow, description);
    }
    
    /**
     * 输出启动成功信息
     */
    public static void logStartupSuccess(String component, long timeMs) {
        String checkMark = GREEN + "✓" + RESET;
        String time = DIM + "(" + timeMs + "ms)" + RESET;
        info("%s %s initialized successfully %s", checkMark, component, time);
    }
    
    /**
     * 输出启动失败信息
     */
    public static void logStartupFailure(String component, Throwable throwable) {
        String crossMark = RED + "✗" + RESET;
        error("%s %s initialization failed", crossMark, component);
        if (throwable != null) {
            error("Cause: %s", throwable.getMessage());
        }
    }
    
    /**
     * 输出进度条样式的日志
     */
    public static void logProgress(String operation, int current, int total) {
        int barLength = 30;
        int filled = (int) ((double) current / total * barLength);
        
        StringBuilder bar = new StringBuilder();
        bar.append(GREEN + "[" + RESET);
        
        for (int i = 0; i < barLength; i++) {
            if (i < filled) {
                bar.append(GREEN + "█" + RESET);
            } else {
                bar.append(DIM + "░" + RESET);
            }
        }
        
        bar.append(GREEN + "]" + RESET);
        
        String percentage = String.format("%3d%%", (current * 100) / total);
        info("%s %s %s (%d/%d)", operation, bar.toString(), 
             BOLD + percentage + RESET, current, total);
    }
    
    /**
     * 输出配置加载状态
     */
    public static void logConfigStatus(String configName, boolean success, String details) {
        String status = success ? GREEN + "✓ LOADED" + RESET : RED + "✗ FAILED" + RESET;
        String configTag = BOLD + PURPLE + "[CONFIG]" + RESET;
        info("%s %s: %s %s", configTag, configName, status, 
             DIM + "(" + details + ")" + RESET);
    }
    
    /**
     * 输出分割线
     */
    public static void logSeparator() {
        System.out.println(DIM + "════════════════════════════════════════════════════════════════" + RESET);
    }
    
    /**
     * 检查是否支持彩色输出
     */
    private static boolean supportsColor() {
        String os = System.getProperty("os.name").toLowerCase();
        String term = System.getenv("TERM");
        return !os.contains("win") || (term != null && term.contains("color"));
    }
}