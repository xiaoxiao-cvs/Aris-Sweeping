# ArisSweeping 开发文档

## 概述

ArisSweeping 是一个高性能的 Minecraft 1.20.1 Forge 模组，提供智能实体清理和高级任务管理功能。该模组具有异步处理、完善的撤销系统，以及基于 malilib 的专业配置界面。

## 项目架构

### 核心系统设计

```
ArisSweeping 模组架构
├── 核心管理层
│   ├── ArisSweepingMod - 主模组类和生命周期管理
│   ├── ModInitializer - 系统初始化协调器  
│   ├── ModConfig - 配置系统核心
│   └── Constants - 全局常量和配置值
├── 任务执行层
│   ├── SmartTaskManager - 智能任务调度和协调
│   ├── AsyncTaskManager - 多线程异步处理
│   ├── UndoManager - 完善的撤销系统，带 NBT 数据保存
│   └── TaskHistoryManager - 任务执行历史和统计
├── 实体清理层
│   ├── EntityCleaner - 支持策略的核心清理引擎
│   ├── Filters/ - 实体过滤系统（物品/动物/自定义）
│   └── Strategies/ - 清理策略（基于时间/距离/密度）
├── 用户界面层（基于 malilib）
│   ├── GuiConfigsAris - 主配置界面
│   ├── ConfigGuiTab - 配置分类系统
│   ├── HUDManager - 游戏内 HUD 叠加层管理
│   └── ModernConfigScreen - 高级配置组件
├── 网络通信层
│   ├── PacketHandler - 网络数据包管理
│   └── Packets/ - 客户端服务端同步数据包
└── 数据持久化层
    ├── ConfigManager - 配置文件管理
    ├── StatisticsCollector - 性能指标和统计
    └── PerformanceMonitor - 实时性能监控
```

## 主要特性

### 1. 智能任务管理系统
- **基于优先级的调度** 使用 `PriorityBlockingQueue`
- **多线程执行** 为核心/IO/调度任务分离线程池
- **完整的任务生命周期** 追踪，包含 8 种不同状态
- **智能资源管理** 自动清理和超时处理

### 2. 高级实体清理引擎  
- **异步处理** 防止服务器卡顿
- **多种清理策略**：基于时间、距离、密度
- **可配置过滤系统** 针对物品和动物
- **区块感知处理** 可配置范围限制

### 3. 专业配置界面
- **malilib 集成** 遵循 MiniHUD 设计模式
- **分类设置** （通用/物品清理/实体清理/性能/颜色）
- **实时配置** 自动同步
- **热键支持** 快速访问和切换

### 4. 完善撤销系统
- **NBT 数据保存** 完整实体恢复
- **可配置撤销历史** （5-50 次操作）
- **时限撤销** 自动清理
- **安全恢复** 验证检查

## 技术实现

### 线程安全与性能
- **无锁数据结构** 使用 `ConcurrentHashMap` 和 `AtomicLong`
- **异步优先设计** 使用 `CompletableFuture` 链  
- **资源池化** 针对线程和临时对象
- **内存高效** 处理，可配置阈值

### 网络架构
- **客户端服务端同步** 配置和统计数据
- **高效数据包设计** 最小化数据传输
- **可靠传递** 确认系统
- **向后兼容性** 支持

### 数据管理
- **类型安全配置** 验证和默认值
- **持久化统计** 自动保存
- **可配置数据保留** 策略
- **导出/导入功能** 配置备份

## 开发环境设置

### 前置要求
- **Java 开发工具包 17+**
- **Minecraft 1.20.1** 
- **Forge 47.2.0+**
- **malilib 依赖** （自动解析）

### 构建说明

```bash
# 克隆仓库
git clone https://github.com/xiaoxiao-cvs/Aris-Sweeping.git
cd Aris-Sweeping

# 构建模组
./gradlew build

# 运行开发客户端
./gradlew runClient

# 运行开发服务端  
./gradlew runServer

# 生成 IDE 运行配置
./gradlew genEclipseRuns
./gradlew genIntellijRuns
```

## 代码结构与约定

### 包组织
```
com.arisweeping/
├── core/           # 核心系统组件
├── tasks/          # 任务管理系统  
├── cleaning/       # 实体清理逻辑
├── gui/            # 用户界面组件
├── network/        # 网络通信
├── config/         # 配置管理
├── data/           # 数据模型和持久化
├── async/          # 异步处理工具
└── monitoring/     # 性能监控
```

### 编码标准
- **Java 17 特性** 适当使用
- **不可变对象** 用于数据传输（TaskResult、CleaningResult）
- **构建者模式** 用于复杂对象构造
- **策略模式** 用于清理算法
- **工厂方法** 用于对象创建
- **全面错误处理** 具体异常类型

### 关键设计模式
- **单例模式** 管理器类
- **观察者模式** 任务状态通知  
- **状态模式** 任务生命周期管理
- **命令模式** 撤销操作
- **外观模式** 复杂子系统访问

## API 参考

### 核心类

#### SmartTaskManager
```java
public class SmartTaskManager {
    // 提交任务执行
    public CompletableFuture<TaskResult> submitTask(TaskExecution task)
    
    // 取消运行中的任务
    public boolean cancelTask(UUID taskId)
    
    // 获取任务状态
    public TaskStatus getTaskStatus(UUID taskId)
}
```

#### EntityCleaner  
```java
public class EntityCleaner {
    // 根据指定请求清理实体
    public CompletableFuture<CleaningResult> cleanEntities(CleaningRequest request)
    
    // 获取清理统计
    public CleaningStats getStatistics()
}
```

#### UndoManager
```java
public class UndoManager {
    // 撤销上一次操作
    public CompletableFuture<UndoResult> undoLastOperation()
    
    // 检查是否可撤销
    public boolean canUndo()
}
```

### 配置系统

#### 配置分类
- **通用设置**: 主开关、热键、调试模式、HUD 设置
- **物品清理**: 策略、生存时间、半径、保护设置  
- **实体清理**: 密度限制、保护规则、繁殖设置
- **性能设置**: 线程池大小、内存阈值、超时值
- **颜色设置**: HUD 颜色、进度指示器、文本样式

#### 配置访问
```java
// 访问配置值
boolean enabled = Configs.General.ENABLED.getBooleanValue();
int lifetime = Configs.ItemCleaning.ITEM_LIFETIME_SECONDS.getIntegerValue();
double radius = Configs.ItemCleaning.CLEANING_RADIUS.getDoubleValue();

// 注册配置回调
Configs.General.ENABLED.setValueChangeCallback((newValue) -> {
    // 处理配置变更
});
```

## 测试指南

### 单元测试
- **全面覆盖** 核心逻辑组件
- **模拟对象** Minecraft 服务端依赖  
- **并发测试** 线程安全组件
- **性能基准** 关键路径

### 集成测试  
- **多服务器测试** 网络组件
- **配置验证** 测试
- **撤销系统可靠性** 测试
- **内存泄漏检测** 长期运行操作

## 性能考虑

### 优化技术
- **区块批处理** 最小化世界访问
- **实体过滤** 昂贵操作之前
- **异步处理** I/O 绑定任务
- **内存池化** 临时对象
- **懒加载** 昂贵资源

### 监控与分析
- **内置性能指标** 通过 PerformanceMonitor
- **可配置日志级别** 调试用
- **内存使用跟踪** 带警报
- **任务执行计时** 和统计

## 贡献指南

### Pull Request 流程
1. **Fork 仓库** 并创建功能分支
2. **遵循编码标准** 并保持测试覆盖率
3. **更新文档** API 变更
4. **充分测试** 客户端和服务端
5. **提交 PR** 详细描述和测试结果

### 代码审查标准
- **功能性**: 代码是否解决了预期问题？
- **性能**: 无显著性能回归
- **安全性**: 无潜在安全漏洞  
- **可维护性**: 代码可读且文档齐全
- **兼容性**: 尽可能保持向后兼容

### 问题报告
报告 Bug 或请求功能时：
- **包含模组版本** 和 Forge 版本
- **提供复现步骤** 和最小测试用例
- **包含相关日志** （client.log、server.log）
- **描述预期与实际行为**

## 发布流程

### 版本管理
- **语义化版本** （主版本.次版本.修订版本）
- **Alpha/Beta/RC** 测试版本
- **更新日志维护** 注明破坏性变更
- **迁移指南** 主版本更新

### 兼容性矩阵
| ArisSweeping 版本 | Minecraft 版本 | Forge 版本 | Java 版本 |
|------------------|---------------|-----------|----------|
| 1.0.0-alpha      | 1.20.1        | 47.2.0+   | 17+      |

## 许可证

该项目采用 GNU General Public License v3.0 许可证 - 详情请参阅 [LICENSE](LICENSE) 文件。

## 支持

技术支持和问题：
- **GitHub Issues**: Bug 报告和功能请求
- **文档**: 代码内文档和本指南  
- **社区**: Minecraft 模组开发社区和论坛

---

*本文档与代码库同步维护。请在任何重大变更时保持更新。*