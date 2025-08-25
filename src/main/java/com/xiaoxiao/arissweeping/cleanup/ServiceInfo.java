package com.xiaoxiao.arissweeping.cleanup;

import com.xiaoxiao.arissweeping.util.CleanupStateManager.CleanupType;

/**
 * 服务信息接口
 * 负责提供清理服务的基本信息
 */
public interface ServiceInfo {
    
    /**
     * 获取清理服务的类型
     * @return 清理类型
     */
    CleanupType getCleanupType();
    
    /**
     * 获取清理服务的名称
     * @return 服务名称
     */
    String getServiceName();
    
    /**
     * 获取清理服务的描述
     * @return 服务描述
     */
    String getServiceDescription();
    
    /**
     * 获取清理服务的状态信息
     * @return 状态信息字符串
     */
    String getStatusInfo();
}