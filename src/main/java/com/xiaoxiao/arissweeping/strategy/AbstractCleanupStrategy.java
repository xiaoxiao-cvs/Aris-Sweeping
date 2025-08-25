package com.xiaoxiao.arissweeping.strategy;

import org.bukkit.entity.Entity;
import org.bukkit.World;
import com.xiaoxiao.arissweeping.exception.ExceptionUtils;
import com.xiaoxiao.arissweeping.exception.CleanupException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 清理策略抽象基类
 * 提供策略的通用实现和模板方法
 */
public abstract class AbstractCleanupStrategy implements CleanupStrategy {
    
    protected final Logger logger;
    protected final StrategyStatistics statistics;
    protected final Map<String, Object> configuration;
    protected volatile boolean enabled;
    
    public AbstractCleanupStrategy() {
        this.logger = Logger.getLogger(getClass().getName());
        this.statistics = new StrategyStatistics();
        this.configuration = new ConcurrentHashMap<>();
        this.enabled = true;
        initializeDefaultConfiguration();
    }
    
    /**
     * 初始化默认配置
     */
    protected abstract void initializeDefaultConfiguration();
    
    /**
     * 执行具体的清理逻辑
     */
    protected abstract CleanupResult doExecute(World world, List<Entity> entities);
    
    @Override
    public final CleanupResult execute(World world, List<Entity> entities) {
        if (!enabled) {
            return new CleanupResult(0, 0, 0, false, "策略已禁用", Collections.emptyMap());
        }
        
        long startTime = System.currentTimeMillis();
        CleanupResult result;
        
        try {
            // 前置检查
            if (!preExecutionCheck(world, entities)) {
                return new CleanupResult(0, 0, 0, false, "前置检查失败", Collections.emptyMap());
            }
            
            // 执行清理
            result = doExecute(world, entities);
            
            // 后置处理
            postExecutionProcess(result);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            String errorMsg = "策略执行异常: " + e.getMessage();
            logger.severe(errorMsg);
            
            CleanupException exception = ExceptionUtils.wrapException(
                getName(), "策略执行", e
            );
            ExceptionUtils.getGlobalHandler().handleException(exception);
            
            result = new CleanupResult(0, 0, executionTime, false, errorMsg, Collections.emptyMap());
        }
        
        // 记录统计信息
        statistics.recordExecution(result);
        
        return result;
    }
    
    /**
     * 执行前检查
     */
    protected boolean preExecutionCheck(World world, List<Entity> entities) {
        if (world == null) {
            logger.warning("世界对象为空");
            return false;
        }
        
        if (entities == null || entities.isEmpty()) {
            logger.fine("实体列表为空");
            return false;
        }
        
        return true;
    }
    
    /**
     * 执行后处理
     */
    protected void postExecutionProcess(CleanupResult result) {
        if (result.isSuccess()) {
            logger.fine(String.format("策略 %s 执行成功: %s", getName(), result.toString()));
        } else {
            logger.warning(String.format("策略 %s 执行失败: %s", getName(), result.getErrorMessage()));
        }
    }
    
    @Override
    public Map<String, Object> getConfiguration() {
        return new HashMap<>(configuration);
    }
    
    @Override
    public void updateConfiguration(Map<String, Object> config) {
        if (config == null) {
            return;
        }
        
        if (validateConfiguration(config)) {
            configuration.putAll(config);
            onConfigurationUpdated();
            logger.info(String.format("策略 %s 配置已更新", getName()));
        } else {
            logger.warning(String.format("策略 %s 配置验证失败", getName()));
        }
    }
    
    /**
     * 配置更新后的回调
     */
    protected void onConfigurationUpdated() {
        // 子类可以重写此方法来处理配置更新
    }
    
    @Override
    public boolean validateConfiguration(Map<String, Object> config) {
        if (config == null) {
            return false;
        }
        
        // 基础验证逻辑
        return doValidateConfiguration(config);
    }
    
    /**
     * 具体的配置验证逻辑
     */
    protected abstract boolean doValidateConfiguration(Map<String, Object> config);
    
    @Override
    public StrategyStatistics getStatistics() {
        return statistics;
    }
    
    @Override
    public void resetStatistics() {
        statistics.reset();
        logger.info(String.format("策略 %s 统计信息已重置", getName()));
    }
    
    /**
     * 启用策略
     */
    public void enable() {
        this.enabled = true;
        logger.info(String.format("策略 %s 已启用", getName()));
    }
    
    /**
     * 禁用策略
     */
    public void disable() {
        this.enabled = false;
        logger.info(String.format("策略 %s 已禁用", getName()));
    }
    
    /**
     * 检查策略是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 获取配置值
     */
    @SuppressWarnings("unchecked")
    protected <T> T getConfigValue(String key, T defaultValue) {
        Object value = configuration.get(key);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return (T) value;
        } catch (ClassCastException e) {
            logger.warning(String.format("配置项 %s 类型转换失败，使用默认值", key));
            return defaultValue;
        }
    }
    
    /**
     * 设置配置值
     */
    protected void setConfigValue(String key, Object value) {
        configuration.put(key, value);
    }
    
    /**
     * 过滤实体列表
     */
    protected List<Entity> filterEntities(List<Entity> entities, java.util.function.Predicate<Entity> filter) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        
        return entities.stream()
                .filter(Objects::nonNull)
                .filter(filter)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 统计实体类型
     */
    protected Map<String, Integer> countEntityTypes(List<Entity> entities) {
        Map<String, Integer> counts = new HashMap<>();
        
        for (Entity entity : entities) {
            if (entity != null) {
                String type = entity.getType().name();
                counts.merge(type, 1, Integer::sum);
            }
        }
        
        return counts;
    }
    
    /**
     * 安全移除实体
     */
    protected boolean safeRemoveEntity(Entity entity) {
        try {
            if (entity != null && entity.isValid()) {
                entity.remove();
                return true;
            }
        } catch (Exception e) {
            logger.warning(String.format("移除实体失败: %s - %s", 
                entity != null ? entity.getType().name() : "null", e.getMessage()));
        }
        return false;
    }
    
    /**
     * 批量移除实体
     */
    protected int batchRemoveEntities(List<Entity> entities) {
        int removedCount = 0;
        
        for (Entity entity : entities) {
            if (safeRemoveEntity(entity)) {
                removedCount++;
            }
        }
        
        return removedCount;
    }
    
    @Override
    public String toString() {
        return String.format("%s{name='%s', priority=%d, enabled=%s}", 
            getClass().getSimpleName(), getName(), getPriority(), enabled);
    }
}