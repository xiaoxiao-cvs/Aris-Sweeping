# ArisSweeping

**ArisSweeping** 是一个智能的 Minecraft 1.20.1 实体清理模组，专注于性能优化和用户体验。

## 特性

- **智能任务管理系统** - 优先级队列与多线程异步处理
- **高效实体清理** - 支持掉落物品和过密畜牧实体清理
- **完善撤销系统** - NBT 数据保存，安全可恢复
- **游戏内配置界面** - 基于 malilib 框架的专业 UI
- **实时性能监控** - HUD 显示和统计数据
- **客户端服务端同步** - 实时配置和状态同步

## 核心功能

### 智能任务管理
- **异步处理架构** - 多线程池设计（核心/IO/调度线程）
- **优先级调度系统** - 基于任务重要性的智能调度  
- **完整生命周期管理** - 8 种任务状态的全程追踪
- **自动资源管理** - 线程池优雅关闭和资源清理

### 实体清理引擎
- **多策略支持** - 时间/距离/密度 基础的清理策略
- **智能过滤系统** - 物品类型和动物密度过滤
- **区块感知处理** - 可配置范围的区块级清理
- **性能优先设计** - 异步处理防止服务器卡顿

## 系统要求

- **Minecraft**: 1.20.1
- **Forge**: 47.2.0+
- **Java**: 17+
- **内存**: 开发环境最少分配 3GB

## 快速开始

### 安装
1. 从 [Releases](https://github.com/xiaoxiao-cvs/Aris-Sweeping/releases) 下载最新版本
2. 将 `.jar` 文件放入 `mods/` 文件夹
3. 安装必需依赖（Forge 自动处理）
4. 启动 Minecraft 并通过游戏内 GUI 进行配置

### 配置
- **默认热键**: `K` - 切换主要功能
- **配置界面**: `H+C` - 打开配置界面
- **设置位置**: `.minecraft/config/arisweeping/`

## 开发

### 从源码构建
```bash
# 克隆仓库
git clone https://github.com/xiaoxiao-cvs/Aris-Sweeping.git
cd Aris-Sweeping

# 构建模组
./gradlew build

# 运行开发环境
./gradlew runClient    # 客户端
./gradlew runServer    # 服务端
```

### 开发环境
```bash
# 生成 IDE 配置
./gradlew genEclipseRuns
./gradlew genIntellijRuns
./gradlew genVSCodeRuns
```

## 架构概览

该模组采用模块化架构，职责分离清晰：

- **核心管理层** - 系统初始化和生命周期管理
- **任务执行层** - 智能调度和异步处理  
- **实体清理层** - 清理策略和过滤系统
- **用户界面层** - 基于 malilib 的配置界面
- **网络通信层** - 客户端服务端同步
- **数据持久化层** - 配置和统计管理

详细技术信息请参阅 [开发文档.md](开发文档.md)。

## 性能特性

- **内存高效** - 可配置内存阈值和自动清理
- **线程安全** - 无锁数据结构和原子操作
- **可扩展性** - 处理大量实体而不降低性能
- **容错性强** - 全面的错误处理和恢复机制

## 贡献

欢迎贡献！请阅读我们的 [贡献指南](开发文档.md#贡献指南) 并：

1. Fork 仓库
2. 创建功能分支
3. 遵循编码标准
4. 为新功能添加测试  
5. 提交 Pull Request

### 开发状态
- **核心系统**: 95% 完成
- **实体清理**: 90% 完成  
- **任务管理**: 95% 完成
- **用户界面**: 98% 完成
- **网络层**: 80% 完成
- **数据系统**: 95% 完成

## 支持

- **文档**: 查看 [开发文档.md](开发文档.md) 获取技术细节
- **Bug 报告**: 使用 [GitHub Issues](https://github.com/xiaoxiao-cvs/Aris-Sweeping/issues)
- **功能请求**: 通过 GitHub Issues 提交，标记 enhancement 标签

## 许可证

该项目采用 GNU General Public License v3.0 许可证 - 详情请参阅 [LICENSE](LICENSE) 文件。

## 致谢

- **Minecraft Forge** - 模组开发框架
- **malilib** - UI 框架基础
- **MiniHUD** - UI 设计灵感
- **Minecraft 模组开发社区** - 资源和支持

---

*ArisSweeping 正在积极开发中。API 和功能可能在版本间发生变化。*