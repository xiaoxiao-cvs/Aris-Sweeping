# Aris Sweeping (爱丽丝扫地)

一个高性能的Minecraft 1.20.1 Forge服务端实体清理插件，以《蔚蓝档案》中的爱丽丝角色命名。

## 功能特性

### 🧹 智能清理
- **自动清理掉落物**：根据配置的年龄阈值清理老旧掉落物
- **密集实体优化**：监控区块实体密度，自动清理过密区域
- **多类型实体支持**：支持清理掉落物、经验球、箭矢、敌对生物等
- **智能保护**：自动保护有自定义名称的实体和玩家

### ⚡ 性能优化
- **异步处理**：使用多线程异步清理，避免服务器掉刻
- **分批处理**：每tick限制处理区块数量，平滑性能负载
- **实时监控**：每秒检查实体密度，及时响应
- **内存友好**：优化的数据结构和垃圾回收策略

### 📊 监控统计
- **详细统计**：分类统计清理的实体数量
- **公屏广播**：向所有玩家广播清理结果
- **日志记录**：完整的清理日志记录
- **实时查询**：命令查看当前服务器实体状态

## 安装方法

1. 确保服务器运行 Minecraft 1.20.1 和 Forge 47.2.0+
2. 将编译好的 `.jar` 文件放入服务器的 `mods` 文件夹
3. 重启服务器
4. 插件将自动生成配置文件 `config/arissweeping-server.toml`

## 配置说明

### 基础设置
```toml
[general]
# 清理间隔（秒）
cleanupInterval = 300

[entity_cleanup]
# 是否清理掉落物
cleanupItems = true
# 是否清理经验球
cleanupExperienceOrbs = true
# 是否清理箭矢
cleanupArrows = true
# 是否清理敌对生物
cleanupHostileMobs = false
# 是否清理被动生物
cleanupPassiveMobs = false

[thresholds]
# 每个区块最大物品数量
maxItemsPerChunk = 50
# 每个区块最大实体数量
maxEntitiesPerChunk = 100
# 物品存在时间阈值（秒）
itemAgeThreshold = 300

[performance]
# 是否使用异步清理
asyncCleanup = true
# 每tick处理的最大区块数
maxChunksPerTick = 5

[messages]
# 是否广播清理消息
broadcastCleanup = true
# 是否显示清理统计
showCleanupStats = true
```

## 命令使用

所有命令需要OP权限（权限等级2）：

### 基础命令
```
/arissweeping cleanup          # 执行标准清理
/arissweeping cleanup items    # 仅清理掉落物
/arissweeping cleanup mobs     # 仅清理敌对生物
/arissweeping cleanup all      # 强制清理所有实体（危险）
/arissweeping stats            # 显示实体统计信息
/arissweeping help             # 显示帮助信息
```

### 配置命令
```
/arissweeping config interval <秒数>  # 设置清理间隔
/arissweeping config reload           # 重新加载配置
```

## 清理策略

### 自动清理触发条件
1. **定时清理**：根据配置的间隔时间自动执行
2. **密度清理**：当区块实体数量超过阈值时触发
3. **年龄清理**：当掉落物存在时间超过阈值时清理

### 保护机制
- 永不清理玩家实体
- 保护有自定义名称的实体
- 保护重要的功能性实体
- 可配置的实体类型过滤

## 性能建议

### 推荐配置（小型服务器 < 20人）
```toml
cleanupInterval = 300
maxItemsPerChunk = 30
maxEntitiesPerChunk = 80
itemAgeThreshold = 240
maxChunksPerTick = 3
```

### 推荐配置（中型服务器 20-50人）
```toml
cleanupInterval = 180
maxItemsPerChunk = 40
maxEntitiesPerChunk = 100
itemAgeThreshold = 300
maxChunksPerTick = 5
```

### 推荐配置（大型服务器 > 50人）
```toml
cleanupInterval = 120
maxItemsPerChunk = 50
maxEntitiesPerChunk = 120
itemAgeThreshold = 180
maxChunksPerTick = 8
```

## 开发构建

### 环境要求
- Java 17+
- Gradle 7.6+
- Minecraft 1.20.1
- Forge 47.2.0+

### 构建步骤
```bash
# 克隆项目
git clone https://github.com/xiaoxiao-cvs/Aris-Sweeping.git
cd Aris-Sweeping

# 构建项目
./gradlew build

# 生成的jar文件位于 build/libs/ 目录
```

### 开发环境
```bash
# 运行开发服务器
./gradlew runServer

# 运行开发客户端
./gradlew runClient
```

## 兼容性

- ✅ Minecraft 1.20.1
- ✅ Forge 47.2.0+
- ✅ 支持多世界
- ✅ 支持插件服务器
- ✅ 兼容大部分模组

## 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 贡献

欢迎提交 Issue 和 Pull Request！

## 致谢

- 感谢《蔚蓝档案》为我们带来可爱的爱丽丝角色
- 感谢 Minecraft Forge 团队提供的优秀框架
- 感谢所有测试和反馈的服务器管理员们

---

**注意**：本插件专为服务端性能优化设计，请根据服务器实际情况调整配置参数。