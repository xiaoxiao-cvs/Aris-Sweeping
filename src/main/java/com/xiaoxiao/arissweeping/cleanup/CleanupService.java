package com.xiaoxiao.arissweeping.cleanup;

/**
 * 简化的清理服务接口
 * 通过组合三个专门的接口来遵循接口隔离原则
 */
public interface CleanupService extends CleanupExecutor, EntityFilter, ServiceInfo {
    // 这个接口现在只是一个组合接口，不包含任何额外的方法
    // 所有功能都通过继承的三个专门接口提供
}