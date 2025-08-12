# 🧹 Aris-Sweeping - 爱丽丝清理系统

一个功能强大的Minecraft服务器实体清理和管理插件，由可爱的爱丽丝为您提供服务！

## ✨ 主要功能

### 🎯 智能实体清理
- **掉落物清理**: 自动清理服务器中的掉落物品，防止物品堆积影响性能
- **掉落物清理**: 清理掉落的方块实体（如沙子、砂砾等），避免卡顿
- **箭矢清理**: 移除射出的箭矢，减少实体数量
- **敌对生物清理**: 智能清理50%的敌对生物，保持生态平衡
- **经验球清理**: 清理多余的经验球实体

### 🐄 畜牧业管理系统
- **密度检测**: 实时监控每个区块的动物数量
- **智能预警**: 超标前5分钟全屏提醒玩家
- **保护机制**: 自动保护有名字的宠物和重要动物
- **区块限制**: 可配置每个区块的最大动物数量
- **统计查看**: 查看世界畜牧业详细统计信息
- **随机清理**: 随机清理各种动物，避免偏向性
- **🆕 区域解析**: 支持对角点区域定义，美化公告显示
- **🆕 中文显示**: 动物名称自动转换为中文显示
- **🆕 美化公告**: 精美的畜牧业超标警告公告

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

### 畜牧业统计
- `/arissweeping livestock-stats` - 查看世界畜牧业统计
- `/arissweeping livestock` - 查看世界畜牧业统计（简写）

### 配置指令
- `/arissweeping config help` - 配置指令帮助
- `/arissweeping config list` - 查看当前配置
- `/arissweeping config enable <true|false>` - 插件总开关
- `/arissweeping config interval <秒>` - 设置清理间隔
- `/arissweeping config item-age <秒>` - 设置掉落物年龄阈值（30-600秒）
- `/arissweeping config reload` - 重新加载配置

### 清理功能配置
- `/arissweeping config cleanup-items <true|false>` - 掉落物清理开关
- `/arissweeping config cleanup-mobs <true|false>` - 敌对生物清理开关
- `/arissweeping config cleanup-animals <true|false>` - 被动生物清理开关
- `/arissweeping config cleanup-arrows <true|false>` - 箭矢清理开关
- `/arissweeping config cleanup-falling <true|false>` - 掉落方块清理开关

### 阈值设置
- `/arissweeping config item-age <秒>` - 掉落物年龄阈值（30-600秒）
  - 只有存在时间超过此阈值的掉落物才会被清理
  - 建议值：小型服务器120秒，中型服务器90秒，大型服务器60秒

### 畜牧业管理配置
- 畜牧业配置现在通过YAML配置文件管理，支持Spark API智能监控
- `/arissweeping config warning-time <分钟>` - 预警时间设置
- `/arissweeping livestock-stats` - 查看世界畜牧业详细统计

### 其他配置
- `/arissweeping config broadcast <true|false>` - 清理消息广播
- `/arissweeping config show-stats <true|false>` - 详细统计显示

### 🔐 权限管理指令
- `/arissweeping permission give <玩家> <权限>` - 授予玩家权限
- `/arissweeping permission remove <玩家> <权限>` - 移除玩家权限
- `/arissweeping permission list [玩家]` - 查看权限列表
- `/arissweeping permission reload` - 重新加载权限配置

**可用权限节点：**
- `arissweeping.admin` - 管理员权限（包含所有功能）
- `arissweeping.cleanup` - 清理权限
- `arissweeping.stats` - 统计查看权限
- `arissweeping.config` - 配置修改权限

## 🔧 配置文件说明

### 重要概念区分

**掉落物 (Items)** vs **掉落物 (Falling Blocks)**:
- **掉落物**: 玩家丢弃或生物死亡掉落的物品
- **掉落物**: 受重力影响掉落的方块实体，如沙子、砂砾、铁砧等

两者是完全不同的实体类型，需要分别配置清理规则。

## 📊 畜牧业统计功能

### 功能特性

**世界畜牧业统计** 提供了全面的服务器动物管理信息：

- **总体统计**: 显示所有世界的动物总数、已加载区块数和超标区块数
- **世界详情**: 按世界分别显示动物数量和分布情况
- **类型分布**: 在调试模式下显示各种动物类型的详细数量
- **实时数据**: 基于当前已加载区块的实时统计
- **性能友好**: 异步处理，不影响服务器性能

### 使用方法

```bash
# 查看世界畜牧业统计
/arissweeping livestock-stats

# 简写命令
/arissweeping livestock
```

### 统计信息说明

**总体统计包含：**
- 总动物数量：所有世界中的动物总数
- 已加载区块：当前服务器已加载的区块数量
- 超标区块：动物数量超过配置限制的区块数
- 密度阈值：当前配置的每区块最大动物数量

**各世界详情包含：**
- 世界名称和该世界的动物总数
- 调试模式下显示动物类型分布（如牛、羊、猪等的具体数量）

### 权限要求

使用畜牧业统计功能需要 `arissweeping.stats` 权限。

### 配置建议

- 启用调试模式可查看更详细的动物类型分布
- 建议定期查看统计信息以优化服务器性能
- 根据统计结果调整畜牧业密度限制

## 🔐 权限管理系统

### 权限节点说明

本插件内置了简单的权限管理系统，无需额外的权限插件即可使用：

| 权限节点 | 说明 | 包含功能 |
|---------|------|----------|
| `arissweeping.admin` | 管理员权限 | 所有功能 + 权限管理 |
| `arissweeping.cleanup` | 清理权限 | 执行各种清理操作 |
| `arissweeping.stats` | 统计权限 | 查看统计信息和TPS |
| `arissweeping.config` | 配置权限 | 修改配置和切换开关 |

### 权限管理指令

```bash
# 授予权限
/arissweeping permission give <玩家名> <权限节点>

# 移除权限
/arissweeping permission remove <玩家名> <权限节点>

# 查看所有玩家权限
/arissweeping permission list

# 查看指定玩家权限
/arissweeping permission list <玩家名>

# 重新加载权限配置
/arissweeping permission reload
```

### 使用示例

```bash
# 给玩家Steve授予清理权限
/arissweeping permission give Steve arissweeping.cleanup

# 给管理员Alice授予完整管理权限
/arissweeping permission give Alice arissweeping.admin

# 移除玩家Bob的配置权限
/arissweeping permission remove Bob arissweeping.config

# 查看玩家Steve的权限
/arissweeping permission list Steve
```

### 权限特性

- **持久化存储**: 权限数据保存在 `permissions.yml` 文件中
- **实时生效**: 权限变更立即生效，无需重启
- **通知系统**: 权限变更时自动通知所有OP和管理员
- **配置通知**: 配置修改时向有权限的用户发送通知
- **Tab补全**: 支持玩家名和权限节点的智能补全

### 默认权限

- **OP用户**: 自动拥有所有权限
- **普通玩家**: 默认无任何权限，需要手动授予
- **权限继承**: `arissweeping.admin` 权限包含所有其他权限

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
  cleanupFallingBlocks: true      # 清理掉落物
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

### v1.0.3
- ✨ 新增世界畜牧业统计功能 `/arissweeping livestock-stats`
- ✨ 支持查看各世界动物数量、区块统计和类型分布
- 🔧 优化畜牧业清理机制，实现随机清理各种动物
- 🔧 规范化警告消息称谓，统一为"老师们"
- 🔧 添加清理完成后的"光呀！"广播消息
- 📊 异步统计处理，不影响服务器性能
- 📚 完善Tab补全和帮助文档

### v1.0.2
- ✨ 新增掉落物年龄阈值指令配置功能 `/arissweeping config item-age <秒>`
- ✨ 支持通过指令动态调整掉落物年龄阈值（30-600秒）
- 🔧 完善了Tab补全功能，支持掉落物年龄阈值配置
- 📚 更新了帮助文档和配置说明

### v1.0.1
- 🔧 修复清理间隔时间计算错误
- 🔧 修复异步清理完成消息显示问题
- 🔧 优化清理警告时间逻辑
- 🔧 修正术语显示错误（凋落物→掉落物）

### v1.0.0
- ✅ 基础实体清理功能
- ✅ 智能实体保护机制
- ✅ 畜牧业密度管理系统
- ✅ 完整的配置指令系统
- ✅ 美化的用户界面
- ✅ 全局开关控制
- ✅ 掉落物清理功能
- ✅ 性能优化和异步处理
- ✅ 内置权限管理系统
- ✅ 配置变更实时通知
- ✅ 权限分级管理

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
itemAgeThreshold = 60

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

## 🎨 区域解析功能

### 功能介绍
区域解析功能允许您为服务器定义特定区域，当这些区域内发生畜牧业超标时，系统会显示美化的中文公告，包含区域名称、坐标信息和详细的动物清理列表。

### 配置示例
在 `config.yml` 中添加以下配置：

```yaml
livestock:
  regions:
    # 启用区域解析功能
    enabled: true
    # 玩家称呼
    playerTitle: "老师"
    
    # 区域定义（对角点坐标）
    areas:
      # 示例区域1：计算机学院自管区
      computer_college:
        name: "计算机学院自管区"
        world: "world"
        x1: -1000
        z1: -1000
        x2: -500
        z2: -500
      
      # 示例区域2：商学院自管区
      business_college:
        name: "商学院自管区"
        world: "world"
        x1: 500
        z1: 500
        x2: 1000
        z2: 1000
  
  # 动物中文名称映射
  animalNames:
    cow: "牛"
    pig: "猪"
    chicken: "鸡"
    sheep: "羊"
    horse: "马"
    rabbit: "兔子"
    # ... 更多动物名称
```

### 美化公告效果
当区域内畜牧业超标时，系统会显示如下格式的美化公告：

```
═══════════════════════════════════════
🚨 畜牧业超标警告 🚨
═══════════════════════════════════════
📍 区域位置: 计算机学院自管区中的区块
📍 坐标: (-800, -750)

⚠️ 畜牧业超标
详细信息:
• 总生物: 45 只，超出 15 只
• 鸡: 共 20 只
• 牛: 共 15 只
• 猪: 共 10 只

⏰ 在 5 分钟后，将会清理超出的生物
请 老师 们及时清理

📋 清理列表:
• 鸡: 8 只
• 牛: 5 只
• 猪: 2 只

═══════════════════════════════════════
```

### 配置说明

#### 区域定义
- `name`: 区域显示名称
- `world`: 世界名称
- `x1, z1`: 第一个对角点坐标
- `x2, z2`: 第二个对角点坐标

#### 动物名称映射
支持将英文动物名称映射为中文显示，让公告更加友好。

#### 玩家称呼
可自定义对玩家的称呼，如"老师"、"同学"、"玩家"等。

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
cleanupInterval = 600
maxItemsPerChunk = 60
maxEntitiesPerChunk = 120
itemAgeThreshold = 120
maxChunksPerTick = 3
```

### 推荐配置（中型服务器 20-50人）
```toml
cleanupInterval = 300
maxItemsPerChunk = 40
maxEntitiesPerChunk = 80
itemAgeThreshold = 90
maxChunksPerTick = 5
```

### 推荐配置（大型服务器 > 50人）
```toml
cleanupInterval = 120
maxItemsPerChunk = 20
maxEntitiesPerChunk = 60
itemAgeThreshold = 60
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