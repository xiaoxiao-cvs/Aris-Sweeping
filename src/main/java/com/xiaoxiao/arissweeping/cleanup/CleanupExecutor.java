package com.xiaoxiao.arissweeping.cleanup;

import com.xiaoxiao.arissweeping.util.CleanupStats;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 清理执行接口
 * 负责实际的清理操作执行
 */
public interface CleanupExecutor {
    
    /**
     * 检查是否可以执行清理
     * @return 如果可以执行清理返回true，否则返回false
     */
    boolean canExecuteCleanup();
    
    /**
     * 同步执行清理
     * @param sender 命令发送者（可选）
     * @return 清理统计信息
     */
    CleanupStats executeCleanup(CommandSender sender);
    
    /**
     * 异步执行清理
     * @param sender 命令发送者（可选）
     * @return 清理统计信息的Future
     */
    CompletableFuture<CleanupStats> executeCleanupAsync(CommandSender sender);
    
    /**
     * 检查清理是否正在运行
     * @return 如果正在运行返回true，否则返回false
     */
    boolean isCleanupRunning();
    
    /**
     * 停止清理服务
     */
    void stopCleanup();
    
    /**
     * 批量清理实体
     * @param entities 要清理的实体列表
     * @param sender 命令发送者（可选）
     * @return 清理统计信息
     */
    CleanupStats batchCleanupEntities(List<Entity> entities, CommandSender sender);
}