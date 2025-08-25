package com.xiaoxiao.arissweeping.strategy;

import com.xiaoxiao.arissweeping.strategy.impl.AgeBasedCleanupStrategy;
import com.xiaoxiao.arissweeping.strategy.impl.DensityBasedCleanupStrategy;
import com.xiaoxiao.arissweeping.exception.ExceptionUtils;
import com.xiaoxiao.arissweeping.exception.CleanupException;
import org.bukkit.entity.Entity;
import org.bukkit.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 策略管理器
 * 管理所有清理策略的注册、执行和配置
 */
public class StrategyManager {
    
    private final Logger logger;
    private final Map<String, CleanupStrategy> strategies;
    private final List<CleanupStrategy> enabledStrategies;
    private final Map<String, Object> globalConfiguration;
    private volatile boolean enabled;
    
    public StrategyManager() {
        this.logger = Logger.getLogger(getClass().getName());
        this.strategies = new ConcurrentHashMap<>();
        this.enabledStrategies = new CopyOnWriteArrayList<>();
        this.globalConfiguration = new ConcurrentHashMap<>();
        this.enabled = true;
        
        initializeDefaultStrategies();
        initializeGlobalConfiguration();
    }
    
    /**
     * 初始化默认策略
     */
    private void initializeDefaultStrategies() {
        try {
            // 注册默认策略
            registerStrategy(new DensityBasedCleanupStrategy());
            registerStrategy(new AgeBasedCleanupStrategy());
            
            logger.info("默认清理策略已初始化");
        } catch (Exception e) {
            CleanupException exception = ExceptionUtils.systemError(
                "初始化默认策略失败", e
            );
            ExceptionUtils.getGlobalHandler().handleException(exception);
        }
    }
    
    /**
     * 初始化全局配置
     */
    private void initializeGlobalConfiguration() {
        globalConfiguration.put("max_execution_time_ms", 5000L); // 最大执行时间5秒
        globalConfiguration.put("parallel_execution", false); // 是否并行执行
        globalConfiguration.put("strategy_timeout_ms", 2000L); // 单个策略超时时间
        globalConfiguration.put("fail_fast", false); // 是否快速失败
        globalConfiguration.put("log_execution_details", true); // 是否记录执行详情
    }
    
    /**
     * 注册策略
     */
    public void registerStrategy(CleanupStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("策略不能为空");
        }
        
        String name = strategy.getName();
        if (strategies.containsKey(name)) {
            logger.warning("策略已存在，将被替换: " + name);
        }
        
        strategies.put(name, strategy);
        updateEnabledStrategies();
        
        logger.info("策略已注册: " + name);
    }
    
    /**
     * 注销策略
     */
    public boolean unregisterStrategy(String strategyName) {
        CleanupStrategy removed = strategies.remove(strategyName);
        if (removed != null) {
            updateEnabledStrategies();
            logger.info("策略已注销: " + strategyName);
            return true;
        }
        return false;
    }
    
    /**
     * 获取策略
     */
    public CleanupStrategy getStrategy(String name) {
        return strategies.get(name);
    }
    
    /**
     * 获取所有策略
     */
    public Collection<CleanupStrategy> getAllStrategies() {
        return new ArrayList<>(strategies.values());
    }
    
    /**
     * 获取启用的策略
     */
    public List<CleanupStrategy> getEnabledStrategies() {
        return new ArrayList<>(enabledStrategies);
    }
    
    /**
     * 更新启用的策略列表
     */
    private void updateEnabledStrategies() {
        List<CleanupStrategy> enabled = strategies.values().stream()
                .filter(strategy -> strategy instanceof AbstractCleanupStrategy ? 
                       ((AbstractCleanupStrategy) strategy).isEnabled() : true)
                .sorted(Comparator.comparingInt(CleanupStrategy::getPriority))
                .collect(Collectors.toList());
        
        enabledStrategies.clear();
        enabledStrategies.addAll(enabled);
        
        logger.fine("启用的策略已更新，数量: " + enabled.size());
    }
    
    /**
     * 执行所有适用的策略
     */
    public StrategyExecutionResult executeStrategies(World world, List<Entity> entities) {
        if (!enabled) {
            return new StrategyExecutionResult(false, "策略管理器已禁用", Collections.emptyList());
        }
        
        if (world == null || entities == null || entities.isEmpty()) {
            return new StrategyExecutionResult(false, "无效的输入参数", Collections.emptyList());
        }
        
        long startTime = System.currentTimeMillis();
        List<StrategyResult> results = new ArrayList<>();
        boolean overallSuccess = true;
        String errorMessage = null;
        
        long maxExecutionTime = (Long) globalConfiguration.get("max_execution_time_ms");
        boolean failFast = (Boolean) globalConfiguration.get("fail_fast");
        boolean logDetails = (Boolean) globalConfiguration.get("log_execution_details");
        
        try {
            for (CleanupStrategy strategy : enabledStrategies) {
                // 检查总执行时间
                if (System.currentTimeMillis() - startTime > maxExecutionTime) {
                    logger.warning("策略执行超时，停止后续策略");
                    break;
                }
                
                // 检查策略是否适用
                if (!strategy.isApplicable(world, entities)) {
                    if (logDetails) {
                        logger.fine("策略不适用: " + strategy.getName());
                    }
                    continue;
                }
                
                // 执行策略
                StrategyResult result = executeStrategy(strategy, world, entities);
                results.add(result);
                
                if (logDetails) {
                    logger.info(String.format("策略 %s 执行完成: %s", 
                        strategy.getName(), result.getCleanupResult().toString()));
                }
                
                // 检查是否需要快速失败
                if (!result.isSuccess() && failFast) {
                    overallSuccess = false;
                    errorMessage = "策略执行失败: " + strategy.getName();
                    break;
                }
                
                // 更新实体列表（移除已清理的实体）
                entities = updateEntityList(entities, result.getCleanupResult());
            }
            
        } catch (Exception e) {
            overallSuccess = false;
            errorMessage = "策略执行异常: " + e.getMessage();
            
            CleanupException exception = ExceptionUtils.wrapException(
                "StrategyManager", "执行策略", e
            );
            ExceptionUtils.getGlobalHandler().handleException(exception);
        }
        
        long totalExecutionTime = System.currentTimeMillis() - startTime;
        
        return new StrategyExecutionResult(
            overallSuccess && !results.isEmpty(),
            errorMessage,
            results,
            totalExecutionTime
        );
    }
    
    /**
     * 执行单个策略
     */
    private StrategyResult executeStrategy(CleanupStrategy strategy, World world, List<Entity> entities) {
        long startTime = System.currentTimeMillis();
        
        try {
            CleanupStrategy.CleanupResult result = strategy.execute(world, entities);
            long executionTime = System.currentTimeMillis() - startTime;
            
            return new StrategyResult(
                strategy.getName(),
                result,
                executionTime,
                result.isSuccess(),
                result.getErrorMessage()
            );
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            String errorMsg = "策略执行异常: " + e.getMessage();
            
            CleanupStrategy.CleanupResult failedResult = new CleanupStrategy.CleanupResult(
                0, 0, executionTime, false, errorMsg, Collections.emptyMap()
            );
            
            return new StrategyResult(
                strategy.getName(),
                failedResult,
                executionTime,
                false,
                errorMsg
            );
        }
    }
    
    /**
     * 更新实体列表，移除已清理的实体
     */
    private List<Entity> updateEntityList(List<Entity> entities, CleanupStrategy.CleanupResult result) {
        // 简单实现：返回仍然有效的实体
        return entities.stream()
                .filter(entity -> entity != null && entity.isValid())
                .collect(Collectors.toList());
    }
    
    /**
     * 启用策略
     */
    public void enableStrategy(String strategyName) {
        CleanupStrategy strategy = strategies.get(strategyName);
        if (strategy instanceof AbstractCleanupStrategy) {
            ((AbstractCleanupStrategy) strategy).enable();
            updateEnabledStrategies();
        }
    }
    
    /**
     * 禁用策略
     */
    public void disableStrategy(String strategyName) {
        CleanupStrategy strategy = strategies.get(strategyName);
        if (strategy instanceof AbstractCleanupStrategy) {
            ((AbstractCleanupStrategy) strategy).disable();
            updateEnabledStrategies();
        }
    }
    
    /**
     * 更新策略配置
     */
    public boolean updateStrategyConfiguration(String strategyName, Map<String, Object> config) {
        CleanupStrategy strategy = strategies.get(strategyName);
        if (strategy != null) {
            strategy.updateConfiguration(config);
            return true;
        }
        return false;
    }
    
    /**
     * 获取策略配置
     */
    public Map<String, Object> getStrategyConfiguration(String strategyName) {
        CleanupStrategy strategy = strategies.get(strategyName);
        return strategy != null ? strategy.getConfiguration() : Collections.emptyMap();
    }
    
    /**
     * 更新全局配置
     */
    public void updateGlobalConfiguration(Map<String, Object> config) {
        if (config != null) {
            globalConfiguration.putAll(config);
            logger.info("全局配置已更新");
        }
    }
    
    /**
     * 获取全局配置
     */
    public Map<String, Object> getGlobalConfiguration() {
        return new HashMap<>(globalConfiguration);
    }
    
    /**
     * 获取所有策略的统计信息
     */
    public Map<String, CleanupStrategy.StrategyStatistics> getAllStatistics() {
        Map<String, CleanupStrategy.StrategyStatistics> stats = new HashMap<>();
        
        for (Map.Entry<String, CleanupStrategy> entry : strategies.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().getStatistics());
        }
        
        return stats;
    }
    
    /**
     * 重置所有策略的统计信息
     */
    public void resetAllStatistics() {
        for (CleanupStrategy strategy : strategies.values()) {
            strategy.resetStatistics();
        }
        logger.info("所有策略统计信息已重置");
    }
    
    /**
     * 启用管理器
     */
    public void enable() {
        this.enabled = true;
        logger.info("策略管理器已启用");
    }
    
    /**
     * 禁用管理器
     */
    public void disable() {
        this.enabled = false;
        logger.info("策略管理器已禁用");
    }
    
    /**
     * 检查管理器是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 检查实体是否应该被清理
     */
    public boolean shouldCleanupEntity(Entity entity, Set<String> enabledStrategyNames) {
        if (!enabled || entity == null) {
            return false;
        }
        
        List<CleanupStrategy> strategiesToCheck = enabledStrategies.stream()
                .filter(strategy -> enabledStrategyNames == null || enabledStrategyNames.contains(strategy.getName()))
                .collect(Collectors.toList());
        
        for (CleanupStrategy strategy : strategiesToCheck) {
            if (strategy.shouldCleanup(entity)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取策略信息
     */
    public List<StrategyInfo> getStrategyInfos() {
        return strategies.values().stream()
                .map(strategy -> new StrategyInfo(
                    strategy.getName(),
                    strategy.getDescription(),
                    strategy.getPriority(),
                    strategy instanceof AbstractCleanupStrategy ? 
                        ((AbstractCleanupStrategy) strategy).isEnabled() : true,
                    strategy.getStatistics()
                ))
                .sorted(Comparator.comparingInt(StrategyInfo::getPriority))
                .collect(Collectors.toList());
    }
    
    /**
     * 策略执行结果类
     */
    public static class StrategyExecutionResult {
        private final boolean success;
        private final String errorMessage;
        private final List<StrategyResult> strategyResults;
        private final long totalExecutionTime;
        
        public StrategyExecutionResult(boolean success, String errorMessage, 
                                     List<StrategyResult> strategyResults) {
            this(success, errorMessage, strategyResults, 0);
        }
        
        public StrategyExecutionResult(boolean success, String errorMessage, 
                                     List<StrategyResult> strategyResults, long totalExecutionTime) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.strategyResults = strategyResults;
            this.totalExecutionTime = totalExecutionTime;
        }
        
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public List<StrategyResult> getStrategyResults() { return strategyResults; }
        public long getTotalExecutionTime() { return totalExecutionTime; }
        
        public int getTotalProcessedEntities() {
            return strategyResults.stream()
                    .mapToInt(r -> r.getCleanupResult().getProcessedEntities())
                    .sum();
        }
        
        public int getTotalRemovedEntities() {
            return strategyResults.stream()
                    .mapToInt(r -> r.getCleanupResult().getRemovedEntities())
                    .sum();
        }
    }
    
    /**
     * 单个策略结果类
     */
    public static class StrategyResult {
        private final String strategyName;
        private final CleanupStrategy.CleanupResult cleanupResult;
        private final long executionTime;
        private final boolean success;
        private final String errorMessage;
        
        public StrategyResult(String strategyName, CleanupStrategy.CleanupResult cleanupResult,
                            long executionTime, boolean success, String errorMessage) {
            this.strategyName = strategyName;
            this.cleanupResult = cleanupResult;
            this.executionTime = executionTime;
            this.success = success;
            this.errorMessage = errorMessage;
        }
        
        public String getStrategyName() { return strategyName; }
        public CleanupStrategy.CleanupResult getCleanupResult() { return cleanupResult; }
        public long getExecutionTime() { return executionTime; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    /**
     * 策略信息类
     */
    public static class StrategyInfo {
        private final String name;
        private final String description;
        private final int priority;
        private final boolean enabled;
        private final CleanupStrategy.StrategyStatistics statistics;
        
        public StrategyInfo(String name, String description, int priority, 
                          boolean enabled, CleanupStrategy.StrategyStatistics statistics) {
            this.name = name;
            this.description = description;
            this.priority = priority;
            this.enabled = enabled;
            this.statistics = statistics;
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
        public int getPriority() { return priority; }
        public boolean isEnabled() { return enabled; }
        public CleanupStrategy.StrategyStatistics getStatistics() { return statistics; }
    }
}