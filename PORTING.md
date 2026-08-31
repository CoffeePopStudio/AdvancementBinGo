# bingo-main → Paper 移植进度

> 目标：保留原项目“成就/卡牌/计分核心”，移植到 AdvancementBinGo Paper 插件，生产可用。

## 已完成

- [x] 项目加入 Kotlin 2.4 + kotlinx-serialization 支持
- [x] `PortBingoObjective`：成就目标模型（含队伍完成状态）
- [x] `PortBingoCardEntry` / `PortBingoCard`：5x5 卡牌模型、行列/对角线统计
- [x] `PortCardGenerator`：按 TASK/GOAL/CHALLENGE + 深度自动分级，12/8/5 难度配比，极难成就低权重
- [x] `PortCardMapRenderer` + `PortMapFactory`：Paper 原版地图渲染 5x5 卡片（先试 map）
- [x] `PortTeam` / `PortGameState` / `PortScoreService`
- [x] `PortAdvancementManager`：Bukkit Advancement 查询/清空
- [x] `PortAdvancementObjectiveManager`：按队伍 tick 成就完成状态

## 待办

- [ ] 将 Kotlin 核心接入现有 Java 插件（替换/桥接 `BingoCard`）
- [ ] 游戏模式：Lockout / Hidden Items / Consume Items / Inventory Mode
- [ ] 非成就目标：Item / Stats / Scoreboard / OneOf / SomeOf / AllOf / Inverse / Opponent
- [ ] 统计系统（SQLDelight 或轻量存储）
- [ ] Map 渲染如果效果不好，回退到现有 GUI
- [ ] 生产环境：异步世界重置、自动开局、断线重连等已存在，需与 Kotlin 核心整合
