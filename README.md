# 🧹 Aris-Sweeping - 爱丽丝清理系统

一个功能强大的Minecraft服务器实体清理和管理插件，由可爱的爱丽丝为您提供服务！

## ✨ 主要功能

### 🎯 智能实体清理
- **掉落物清理**: 自动清理服务器中的掉落物品，防止物品堆积影响性能
- **凋落物清理**: 清理掉落的方块实体（如沙子、砂砾等），避免卡顿
- **箭矢清理**: 移除射出的箭矢，减少实体数量
- **敌对生物清理**: 智能清理50%的敌对生物，保持生态平衡
- **经验球清理**: 清理多余的经验球实体

### 🐄 畜牧业管理系统
- **密度检测**: 实时监控每个区块的动物数量
- **智能预警**: 超标前5分钟全屏提醒玩家
- **保护机制**: 自动保护有名字的宠物和重要动物
- **区块限制**: 可配置每个区块的最大动物数量

### 🛡️ 特殊实体保护
- **矿车保护**: 不清理各种类型的矿车（包括箱子矿车）
- **船只保护**: 保护所有类型的船只
- **装饰保护**: 保护盔甲架、物品展示框、画等装饰实体
- **动物保护**: 默认不清理被动生物，保护玩家的牲畜

### ⚙️ 完整配置系统
- **全局开关**: 一键启用/禁用所有自动清理功能
- **细粒度控制**: 独立控制每种实体类型的清理
- **实时配置**: 无需重启服务器即可修改配置
- **配置查看**: 随时查看当前所有配置状态

## 🚀 快速开始

### 安装
1. 下载最新版本的 `aris-sweeping-1.0.0.jar`
2. 将文件放入服务器的 `plugins` 文件夹
3. 重启服务器
4. 使用 `/arissweeping toggle` 启用插件

### 基础使用
```
# 启用插件（默认为关闭状态）
/arissweeping toggle

# 查看帮助
/arissweeping help

# 立即执行清理
/arissweeping clean

# 查看统计信息
/arissweeping stats

# 查看当前配置
/arissweeping config list
```

## 📋 指令大全

### 基础指令
- `/arissweeping help` - 显示帮助信息
- `/arissweeping toggle` - 快速切换插件开关
- `/arissweeping clean` - 立即执行标准清理
- `/arissweeping stats` - 查看详细统计信息

### 清理指令
- `/arissweeping cleanup` - 执行标准清理
- `/arissweeping cleanup items` - 仅清理掉落物
- `/arissweeping cleanup mobs` - 仅清理敌对生物
- `/arissweeping cleanup all` - 强制清理所有实体

### 配置指令
- `/arissweeping config help` - 配置指令帮助
- `/arissweeping config list` - 查看当前配置
- `/arissweeping config enable <true|false>` - 插件总开关
- `/arissweeping config interval <秒>` - 设置清理间隔
- `/arissweeping config reload` - 重新加载配置

### 清理功能配置
- `/arissweeping config cleanup-items <true|false>` - 掉落物清理
- `/arissweeping config cleanup-mobs <true|false>` - 敌对生物清理
- `/arissweeping config cleanup-animals <true|false>` - 被动生物清理
- `/arissweeping config cleanup-arrows <true|false>` - 箭矢清理
- `/arissweeping config cleanup-falling <true|false>` - 凋落物清理

### 畜牧业管理配置
- `/arissweeping config livestock-check <true|false>` - 密度检测开关
- `/arissweeping config livestock-limit <数量>` - 每区块最大动物数
- `/arissweeping config warning-time <分钟>` - 预警时间设置

### 其他配置
- `/arissweeping config broadcast <true|false>` - 清理消息广播
- `/arissweeping config show-stats <true|false>` - 详细统计显示

## 🔧 配置文件说明

### 重要概念区分

**掉落物 (Items)** vs **凋落物 (Falling Blocks)**:
- **掉落物**: 玩家丢弃或生物死亡掉落的物品实体，如钻石、食物、工具等
- **凋落物**: 受重力影响掉落的方块实体，如沙子、砂砾、铁砧等

两者是完全不同的实体类型，需要分别配置清理规则。

### 默认配置
```yaml
# 全局设置
global:
  enabled: false  # 默认关闭，需要手动启用
  debug: false

# 实体清理设置
entity_cleanup:
  cleanupItems: true              # 清理掉落物
  cleanupExperienceOrbs: true     # 清理经验球
  cleanupArrows: true             # 清理箭矢
  cleanupFallingBlocks: true      # 清理凋落物
  cleanupHostileMobs: false       # 敌对生物清理（50%概率）
  cleanupPassiveMobs: false       # 被动生物清理（建议保持关闭）

# 畜牧业管理
livestock:
  enableDensityCheck: true        # 启用密度检测
  maxAnimalsPerChunk: 20          # 每区块最大动物数
  warningTime: 5                  # 预警时间（分钟）
  enableWarning: true             # 启用预警广播
```

## 🎯 性能优化

- **异步处理**: 支持异步清理，不影响主线程性能
- **批量处理**: 分批处理大量实体，避免服务器卡顿
- **智能检测**: 只在必要时执行清理，减少资源消耗
- **区块优化**: 按区块分组处理，提高清理效率

## 🛠️ 开发信息

- **版本**: 1.0.0
- **兼容性**: Minecraft 1.16+
- **依赖**: Bukkit/Spigot/Paper
- **语言**: Java 8+

## 📝 更新日志

### v1.0.0
- ✅ 基础实体清理功能
- ✅ 智能实体保护机制
- ✅ 畜牧业密度管理系统
- ✅ 完整的配置指令系统
- ✅ 美化的用户界面
- ✅ 全局开关控制
- ✅ 凋落物清理功能
- ✅ 性能优化和异步处理

## 💝 特别感谢

感谢所有为这个项目提供建议和反馈的服务器管理员和玩家们！

---

**由爱丽丝为您提供清理服务 💖**

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