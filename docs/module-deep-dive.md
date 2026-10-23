# ATALOSS (ATALOSS) 大型模块源码技术分析

> 分析目标：`E:\Work\ATALOSS\src\main\java\myau\module\modules\`
> 读取方式：全部文件**完整逐行读完**（Telly 3794 行分 10 次读完）
> 物理行数 vs 非空行数：任务书给的行数是「非空行数」，本报告表格两个都列。

---

## 0. 框架约定（读代码前必须知道的 12 条）

| 机制 | 实现 | 关键点 |
|---|---|---|
| 模块基类 | `Module(name, enabled, hidden)` | `setEnabled` 触发 `onEnabled/onDisabled`；`toggle()` 会播 HUD 音 |
| 事件注册 | `ATALOSS.init()` 反射扫描所有 `Property` 字段并 `EventManager.register(module)` | 只扫描 `getDeclaredMethods()`，**不包含父类方法**；同优先级按**注册先后**顺序执行 |
| 优先级 | `Priority.HIGHEST=0 … LOWEST=4`（值越小越先跑） | `Telly` 全用 `HIGHEST`；`Scaffold/AutoBlockIn/BedNuker` 用 `HIGH` |
| 旋转主通道 | `UpdateEvent.setRotation(yaw,pitch,priority)` | 仅 PRE 生效，`lastPriority <= priority` 才覆盖 → 同 tick 里**高 priority 数字赢**，平局后者赢 |
| 静默旋转 | `UpdateEvent.setPervRotation(yaw,priority)` → `RotationState.applyState(...)` | 只有调用过它，`RotationState.isActived()/getPriority()/getSmoothedYaw()` 才有效，`MoveUtil.fixStrafe` 才能用 |
| RotationManager | `ATALOSS.rotationManager.setRotation(yaw,pitch,priority,force)` | **Telly 完全没用它**（见 §1.7） |
| 移动输入 | `MoveInputEvent`（`MixinEntityPlayerSP` 在 `updatePlayerMoveState()` 之后 fire） | 模块直接改 `mc.thePlayer.movementInput.moveForward/moveStrafe/jump/sneak` |
| `MovementInput` 陷阱 | MC 每 tick 会 `movementInput.updatePlayerMoveState()` 重置 forward/strafe | 所以**只有 `MoveInputEvent`（或其后）写入才有效**；Mixin 已把 `moveForward/Strafe` 字段设为 public |
| 分包/延迟 | `LagManager`（出包队列）、`BlinkManager`（出包队列）、`DelayManager`（**入包**队列） | Lag/Blink 走 `PacketUtil.sendPacketNoEvent`（绕过事件）；Delay 用 `packet.processPacket(handler)` 回放 |
| 玩家状态 | `PlayerStateManager.handlePacket` | `attacking/digging/placing/swapping/swinging` 任一 C03 出包即全部清零（即"本 tick 尚未动作"） |
| 属性可见性 | `Property(name, default, BooleanSupplier visibleChecker)` | 四参构造 `IntProperty(name,v,min,max,check)` 的第 5 参是可见性 |
| 配置持久化 | `ATALOSS.init()` 反射取 `getDeclaredFields()` 中 `instanceof Property` | **私有 property 也会被注册**（顺序 = 声明顺序，决定 GUI 顺序） |

**事件流（每 tick 顺序）**
```
runTick HEAD  → TickEvent(PRE)
  onLivingUpdate → MoveInputEvent
  onUpdate HEAD → UpdateEvent(PRE) ──┬─ Telly.running 的准备阶段
                                     └─ 所有模块抢旋转
  onUpdateWalkingPlayer 前 → PlayerUpdateEvent
  onLivingUpdate 内 → LivingUpdateEvent
  onUpdate RETURN → UpdateEvent(POST)
runTick RETURN → TickEvent(POST)
```

---

## 1. `Telly.java` —— 3794 行 / 3344 非空（全项目最大）

### 1.1 功能与使用场景

**Legit Telly（正统 Telly Bridge）全自动脚本**。Telly bridge 是 BedWars/竞技 PvP 里的高难操作：**背对着桥向后跳、在空中转身 180°、低头 90° 往脚下丢方块**，用一条 21 tick 的固定节奏实现"一跳一块"的极限搭桥速度（比 Scaffold 快近一倍，但极容易被 anticheat 抓）。

本文件不是原生 Java 模块，而是 **从 Raven/BSP/Flux 的 JS 脚本（`scriptName = "Telly"`）机械移植过来的**：
- 保留完整脚本 API 层：`ClientApi / ModulesApi / KeybindsApi / InventoryApi / WorldApi / RenderApi / GlApi / UtilApi / BridgeApi`
- 保留完整脚本类型系统：`Entity / ItemStack / Block / Vec3 / CPacket(C02/C03/C07/C08/C0B) / SPacket(S23) / PlayerState`
- 中文注释直接暴露移植痕迹（第 959-962、1186、1442、1815、2755 行）

**玩法**：开模块 → 站方块边缘潜行低头（pitch ≥ 75°、"对角线 ±2°"朝向）→ 出现红框"Activate?"→ 1 秒后变绿 → 按住右键 + 松开潜行 → 全自动起飞，直到松手/被传送/被击落。

### 1.2 全部配置项（Property）

| Property | 类型 | 默认 | 可见性 | 说明 |
|---|---|---|---|---|
| `auto-swap` | Boolean | `true` | 恒显 | 手上方块 ≤5 时自动切到背包里最多的一组（**纯客户端切槽，不发 C09**） |
| `disable-safewalk` | Boolean | `true` | 恒显 | 启动时关掉 `SafeWalk` 模块，结束时恢复原状态 |
| `show-activation-hitbox` | Boolean | `false` | 恒显 | 3D 画激活面（绿/红边框 + 半透明填充） |
| `print` | Boolean | `false` | 恒显 | 聊天栏状态输出 |

**注意**：`onLoad()` 里 `modules.registerDescription/registerButton` 全是**空实现**（第 3171-3176 行），`getButton` 直接映射到 4 个 Property；`Eagle` 的开关只通过 `eagleDisabledForActivation / eagleWasDisabledByTelly` 两个 boolean 记账。

### 1.3 事件钩子清单

| 方法 | 事件 | 优先级 | PRE 做什么 | POST 做什么 |
|---|---|---|---|---|
| `handleUpdate` | `UpdateEvent` | HIGHEST | `onPreUpdate()` → `advanceTellyCycle()` → `onPreMotion(state)` → `event.setRotation(state.yaw,state.pitch,5)` + `setPervRotation(...,5)` | `onPostMotion()`：`c08CounterAtTickBoundary = totalC08Counter; manualC08InWindow=false` |
| `handleMoveInput` | `MoveInputEvent` | HIGHEST | 恒执行 `applyTellyMovementInput()`（suppress sneak → enforce safewalk → holdScriptedRotation → `applyMovement(staged*)`） | — |
| `handleLivingUpdate` | `LivingUpdateEvent` | HIGHEST | `client.setSneak(false)` + `enforceSafeWalkDisabledForRun()`（只在 running 时） | — |
| `handleRender2D` | `Render2DEvent` | HIGHEST | `onRenderTick()`：prompt 淡入淡出、画"Activate?"文字、相机接管检测、`applySmoothedRotation()` | — |
| `handleRender3D` | `Render3DEvent` | HIGHEST | `onRenderWorld()` → `drawActivationFaceRegion()` | — |
| `handlePacket` | `PacketEvent` | HIGHEST | SEND：`CPacket.from()` → `onPacketSent()`，返回 false 即 `setCancelled(true)`；RECEIVE：`SPacket.from()` → `onPacketReceived()` | — |
| `handleWorldLoad` | `LoadWorldEvent` | HIGHEST | `stopAutomation(false)` + `autoPlaceOnWorldJoin()` | — |
| `handleLeftClick` | `LeftClickMouseEvent` | HIGHEST | `onMouse(0,true,0,0)`，返回 false → cancel | — |
| `handleRightClick` | `RightClickMouseEvent` | HIGHEST | `onMouse(1,true,0,0)` | — |
| `handleHitBlock` | `HitBlockEvent` | HIGHEST | 复用 `onMouse(0,...)` | — |
| `handleSafeWalk` | `SafeWalkEvent` | HIGHEST | running 且 `disable-safewalk` → `setSafeWalk(false)` | — |

**⚠️ 死代码（脚本接口残留，Java 端从未被调用）**：
`onLoad()`、`onWorldJoin()`、`onKey()`、`onPostPlayerInput()`、`autoPlaceOnRenderTick()`、`autoPlaceOnWorldJoin()`（除 LoadWorld 外无调用点）、`CPacket.C03.yaw/pitch/ground`、`applyGCD()`、`swingReset()`、`printStatus` 的部分分支。
其中 `onKey()` 承担了**丢失的手动脱手检测**（`stopAutomation` on manual movement key）。因为没人调它，实际脱手只能靠 `detectManualCameraTakeover()`（相机累积偏差 ≥25°）。

### 1.4 核心算法

#### (A) 状态机：`armed`（待激活）→ `running`（脚本运行）

```
armAutomation()   : armed=true, running=false, setupTick=0, cyclePhase=19
   ↓ 需要: 潜行 + pitch>=75 + raycast 命中"前脚下方块的外侧下半面中央"
updateActivationPrompt() 每 tick 检测
   ├─ activatePromptAt==0 时开始计时；>=850ms 抑制 use 键；>=1000ms 变绿 + 关 Eagle
   └─ 绿色 + 松开潜行 + 按住右键 + 朝向在对角线 ±2° 内 → beginAutomation()
beginAutomation() : 吸附 baseYaw 到最近对角线, 计算 travel 方向, 锁 antiSwayLane,
                    setupTick=0, cyclePhase=19, running=true, armed=false
   ↓
setupTick 0..11 (12 tick 前摇): forward=-1, strafe=-1, jump = (tick>=6), use=按住
                                rotation target = baseYaw, pitch=74.52
setupTick==11 → 切到 tellyCycle；takeoverDetectionAt = now+125ms
   ↓
advanceTellyCycle() 每 tick: phase = cyclePhase → 应用 phase 的 fwd/strafe/jump/sprint/use
                            → 同时把 rotation 目标设为 **phase+1** 的曲线值 (前瞻 1 tick)
```

**21 相位曲线表**（`yawCurve / pitchCurve / forwardCurve / strafeCurve`，索引 0..20；`cyclePhase` 从 19 起步，所以首个执行相位是 0）

| ph | yaw(相对 baseYaw) | pitch | fwd | strafe | jump | sprint | use | 阶段含义 |
|---|---|---|---|---|---|---|---|---|
| 0 | +91.68 | 64.31 | +1 | -1 | 否 | ✔ | 否 | 前摇/前进 |
| 1 | +98.88 | 59.95 | +1 | -1 | ✔ | ✔ | 否 | 起跳 |
| 2 | +78.94 | 60.57 | 0 | -1 | ✔ | ✘ | 否 | 滞留 |
| 3 | +37.45 | 61.46 | 0 | -1 | ✔ | ✘ | 否 | 开始转身 |
| 4 | +1.61 | 60.64 | -1 | 0 | ✔ | ✘ | 否 | 转身完成(面朝前方) |
| 5 | -21.69 | 58.89 | -1 | 0 | ✔ | ✘ | 否 | |
| 6 | -33.98 | 56.91 | -1 | 0 | ✔ | ✘ | 否 | |
| 7 | -35.80 | **56.63** | -1 | 0 | ✔ | ✘ | **✔** | 最低点，开始放块 |
| 8 | -34.64 | 58.65 | -1 | 0 | ✔ | ✘ | ✔ | |
| 9 | -33.85 | 61.63 | -1 | 0 | ✔ | ✘ | ✔ | |
| 10 | -33.06 | 64.20 | -1 | 0 | ✔ | ✘ | ✔ | |
| 11 | -31.55 | 66.74 | -1 | 0 | ✔ | ✘ | ✔ | |
| 12 | -29.26 | 68.69 | -1 | 0 | ✔ | ✘ | ✔ | |
| 13 | -26.65 | 70.64 | -1 | 0 | ✔ | ✘ | ✔ | |
| 14 | -24.19 | 73.01 | -1 | 0 | ✔ | ✘ | ✔ | |
| 15 | -21.07 | 75.37 | -1 | 0 | ✔ | ✘ | ✔ | |
| 16 | -18.84 | 77.46 | -1 | 0 | ✔ | ✘ | ✔ | |
| 17 | -17.06 | **78.90** | -1 | -1 | ✔ | ✘ | ✔ | 峰值低头 |
| 18 | -8.87 | 78.56 | -1 | -1 | ✔ | ✘ | ✔ | |
| 19 | +2.61 | 77.22 | +1 | -1 | ✔ | ✘ | ✔ | 落回，准备循环 |
| 20 | +41.94 | 72.25 | +1 | -1 | ✔ | ✘ | ✔ | 回摆到下一周期 |

**每 tick 决策树（PRE）**
```
onPreUpdate():
  1. enforceSafeWalkDisabledForRun(); if running → 松 attack 键, applySmoothedRotation(), holdScriptedRotation()
  2. if armed && !running → updateActivationPrompt()
  3. if !running → return
  4. 冻结检测: freezeLastTickAt != 0 && now-last > 300ms → stopAutomation(true)   ← 游戏卡顿兜底
  5. player == null || player.isDead() || fallDistance > 7.0 → stopAutomation(true)
  6. handleAutoSwap(player)                       ← 手持方块 <=5 → 换最多的一组
  7. !player.isHoldingBlock() → stopAutomation(true)
  8. if firstTellyPlacementPending → updateAdaptivePlacementAim(player)
  9. autoPlaceOnPreUpdate() → syncPlacementTick() + 每 tick 一次 processAutoPlaceTick()
 10. if firstTellyPlacementPending → 再来一次 updateAdaptivePlacementAim()
```
```
processAutoPlaceTick(player):   ← 真正的分包决策
  pruneRejectedTargets()
  lastPlacedPos 支撑消失 → 全部清空
  !isAutoPlaceActiveWindow() → 清候选 + bridge.remove("AutoPlacePlacing") + 恢复 use → return
  heldStack 不是可用方块 → 同上
  脚下块不可替换 → 同上
  yaw = running ? scriptedRotationYaw : player.getYaw()   ← 必须脚本视角, 相机视角会偏到"后左"
  pitch = sanitizePitch(running ? scriptedRotationPitch : player.getPitch())
  candidate = resolveCandidateWithOffCursorSilentPitch(...)
      → 命中: 记 silentPitch, suppressUse()
      → 未命中且本窗口还没放过: restoreUseToPhysicalState()
  placedInCurrentWindow() || lastPlacementAttemptTick == tick → suppressUse(); return
  candidate == null → clearCachedCandidate(); return
  !claimExperimentalPlacementTick() → return      ← 与其他模块抢"本 tick 唯一放置权"
  bridge.add("AutoPlacePlacing"); lastPlacementAttemptTick = tick
  attemptPlacement(...) 成功 → return
  仍未放置 → 用 findBelowPlacement(deadline = now + 4ms) 重试一次 → attemptPlacement
  都不行 → releaseExperimentalPlacementClaim()
```
```
attemptPlacement(player, candidate, held):
  八重前置校验: placedPos/supportPos/face 非空; isStraightTellyTarget(placedPos)  ← 必须在桥的车道上
               脚下可替换; 手持可用方块; !placedInCurrentWindow()
  prePlaceHit = resolveVerifiedHit(yaw, pitch, supportPos, face, placedPos)   ← raycast 必须精确命中支撑面
  !cancelledGhostBlocks.contains(supportPos); isReplaceable(placedPos);
  isSupportAvailable(supportPos); !doesPlacementIntersectPlayer(placedPos)
  counterBefore = totalC08Counter; placingViaModule = true
  client.placeBlock(supportPos, faceName(face), hitAbs) → mc.playerController.onPlayerRightClick(...)
  placingViaModule = false
  packetSent = totalC08Counter > counterBefore
  成功 → 记录 lastPlacedPos/lastSupportPos/lastSupportFace/lastSuccessfulPlaceTick/forceSuppressTick
        + client.swing()（swingItem + C0A + resetEquippedProgress）
  发了包但返回 false → markRejectedTarget(placedPos)（4 tick 内不再尝试）
```

#### (B) 分包 / 位置欺骗策略

| 手法 | 实现 | 作用 |
|---|---|---|
| **C08 白名单** | `onPacketSent`：C08 的 `direction != 255` 时算出 `placedTarget`，若 `!isStraightTellyTarget()` → **cancel 包** + 记入 `cancelledGhostBlocks` | 把"偏到侧向"的放置全部拦在本地，只让车道上的包出去 |
| **Ghost block 恢复** | 收到 `S23PacketBlockChange` 时从 `cancelledGhostBlocks` 移除该坐标 | 服务端回正后重新允许该格子 |
| **静默俯仰 (silent pitch)** | `autoPlaceOnPreMotion(state)`：`silentPitchActive && !manualC08InWindow` → 覆盖 `state.pitch` 为"指向支撑面命中点"的 pitch | 脚本视角 74.52° 与放置所需 pitch 不一致时，只让**这一个包**用真实 pitch，其余包保持脚本值 |
| **use 键抑制** | `suppressUse()` / `restoreUseToPhysicalState()` + 取消 `direction == 255` 的 C08（右键空挥） | 防止本地右键导致多余的 `C08(direction=255)` 到服务端 |
| **手动点击封锁** | `shouldSuppressManualClicksThisTick()`：`lastSuccessfulPlaceTick == currentClientTick \|\| forceSuppressTick == currentClientTick`；`onMouse` 直接 cancel 事件 | 脚本自己放完块的 tick 里，玩家的鼠标点不会重复触发 |
| **放置仲裁** | `BridgeApi`：`PlacementArbiterTick/Owner` + `AutoPlacePlacing` + 检查 `bridge.has("ScaffoldRunning")` | 与 Scaffold 互斥，避免同 tick 双放 |
| **位置碰撞历史** | `doesPlacementIntersectPlayer` 同时检查**当前**位置、**lastTick** 位置、**最后发给服务端的 C03 位置**（`lastSentServerPos`） | 防止放在"服务端还没收到你已经离开"的格子里 → 卡方块/被回弹 |
| **旋转 GCD 量化** | `rotationGcd() = (sens*0.6+0.2)^3 * 8 * 0.15`；`quantizeFrom(origin, value)` 把 rotate 增量对齐到 gcd 的整数倍 | 让脚本旋转看起来像真实鼠标输入（反 `aim` 检测） |
| **yaw 抖动** | `YAW_NUDGE_PATTERN = {0,1,-1,2,-2}`，每 tick 取 `rotationGcd() * pattern[counter%5]` 加到目标 yaw | 消除完美等差数列的机器特征 |
| **相机接管检测** | `detectManualCameraTakeover()`：`takeoverAccumulated -= elapsed*0.045`，偏差超噪声底线时累加，≥25 就 `stopAutomation` | 玩家自己动视角 → 立刻放手 |
| **C0B 潜行包拦截** | `onPacketSent`：`START_SNEAKING` → cancel | 运行中不允许潜行（会破坏 telly 节奏） |
| **攻击/挖掘包拦截** | `C02(ATTACK)` / `C07(DESTROY)` → cancel | 运行中锁攻击 |
| **丢包保护** | `isDropProtected() = running \|\| isActivationInProgress()`，`C07(DROP)` → cancel，`onKey` 里也锁 drop 键 | 防止手滑丢方块毁掉整条桥 |
| **S08 急停** | `onPacketReceived`：`S08PacketPlayerPosLook` → `stopAutomation(true)` | 被服务端拉回 → 说明已被 anticheat 干预，立即停 |

#### (C) 方块选择与放置朝向

候选搜索链（按代价从低到高，全部带 `deadlineMs` 时间预算防卡帧）：

```
findBelowPlacement(deadline)
 ├─ 1. findDirectCursorRayPlacement     : 直接 raycast 当前视角，命中面即支撑面（最理想）
 └─ mode == 1 (直线桥) :
      ├─ isStraightCenterBelowAir ?  findBelowPlayerAirborneFallback
      ├─ tryPreviousVisibleFirst   ?  findStraightPreviousVisibleFaceFallback
      ├─ straightGroundException   ?  findStraightGroundExceptionCandidate (forcedModeCheck=2)
      ├─ findStraightLegacyLaneFallback   (9 个 lane target × 3 个 Y 层)
      ├─ (!tryPreviousVisibleFirst) ? findStraightPreviousVisibleFaceFallback
      └─ findPreviousBlockAirborneFallback
    mode == 2 (斜向) :
      ├─ findBelowPlacementForSupport(player, …, null, -1, deadline)
      ├─ findBelowPlayerAirborneFallback
      ├─ findNearestSupportToBelowPlayerFallback    ← 半径 0..3 环形搜索 + Bresenham 补线
      └─ findLegacyBelowPlacement (含 findLegacyDiagonalPlacement)
```

**面命中点枚举（性能核心）**：
```java
FACE_HIT_OFFSETS          = {0.5, 0.25, 0.75, 0.15, 0.85}            // 5×5 = 25 点/面
EXTENDED_FACE_HIT_OFFSETS = {0.5,0.25,0.75,0.15,0.85,0.35,0.65,0.05,0.95} // 9×9 = 81 点/面
ALLOWED_PLACE_FACES = {2,3,4,5,1}   // N,S,W,E,UP（不含 DOWN）
useExtendedSearch() 恒 true
```
→ `findPitchPlacementForTarget` 最坏 **5 面 × 81 点 = 405 次** `getSupportFaceHitVec + computePitchToHitVec + isPlacementLookAligned(内部再一次 raycast)`，再加上外层最多 27 个 target。

**朝向计算**：`computePitchToHitVec` = `-atan2(dy, sqrt(dx²+dz²))`（夹 ±89°）；yaw 不单独算（由脚本曲线固定为对角线），只用 `isPlacementLookAligned` 校验"这个 pitch 是否真能 raycast 到目标面"。

**候选打分**（`scorePlacementCandidate`，越小越好）：
```
pitchPenalty        = |wrapAngle(candidatePitch - currentPitch)|
centerPenalty * 2.0 = (|primary-0.5| + |secondary-0.5|) * 2
facePenalty         = (placeFace == UP ? 0 : 0.35)
straightSidePenalty = 直线模式下换面且不在边缘 → +0.8
```

**车道约束（`isStraightTellyTarget`）**——这是"桥能连起来"的关键：
```java
lane = travelX != 0 ? position[2] : position[0];   // 横向车道坐标
lane != bridgeLaneBlock            → reject
straightProgress(pos) < bridgeStartProgress → reject
// straightProgress = x*travelX + z*travelZ
```

#### (D) 抗侧漂（Anti-Sway）PD 控制
```java
error        = antiSwayLane - lanePosition            // 车道方向的位置误差
laneVelocity = motion.z 或 motion.x
// 死区
if (|error| < 0.015 && |laneVelocity| < 0.008) { 衰减 yawOffset *= 0.65; return strafe; }
desiredLaneVelocity = clamp(error*0.42 - laneVelocity*0.78, ±0.16)
velocityCorrection  = desiredLaneVelocity - laneVelocity
yawLaneDerivative   = travelX!=0 ? (-fwd*sin + strafe*cos) : (-fwd*cos - strafe*sin)
desiredYawOffset    = clamp(deg(velocityCorrection*0.55 / yawLaneDerivative), ±2.25)   // 分母<0.12 则不用
antiSwayYawOffset   = antiSwayYawOffset*0.60 + desiredYawOffset*0.40                  // 一阶低通
// 离散"拍打"补偿: 条件满足时临时把 strafe 加 1.0（一次）
```
在 `setRotationTarget` 里 `correctedTargetYaw += antiSwayYawOffset`（自适应放置瞄准生效时跳过）。

#### (E) 自适应放置瞄准
`firstTellyPlacementPending` 期间，`updateAdaptivePlacementAim` 用**缓存的候选块命中点**算 yaw/pitch，写入 `adaptiveAimYaw/Pitch`（有效窗口 125ms）。`setRotationTarget` 检测到窗口有效就整段替换目标 yaw/pitch——为的是第一次放块不许失败（第一块失败整条桥就断）。

#### (F) 激活几何（`isLookingAtEdge`）
```java
ACTIVATION_ACROSS_MIN=0.38, MAX=0.65    // 沿面宽度方向 38%~65%（避开两端）
ACTIVATION_HEIGHT_MIN=0.25, MAX=0.75    // 面高度 25%~75%（下半部分）
ACTIVATION_YAW_TOLERANCE=2.0f           // 必须贴合 45°+90k 对角线
lipDistance = 玩家到方块外沿距离, > 0.65 → 不算边缘
isActivationYawAligned: round((yaw-45)/90)*90+45 的 ±2°
```

#### (G) 时间/延迟阈值汇总（硬编码）

| 常量 | 值 | 位置 |
|---|---|---|
| `updateActivationPrompt` 抑制 use | 850 ms | `activationSuppressUse()` |
| 激活转绿 | 1000 ms | `activationPromptReady()` |
| prompt 断开后清除 | 300 ms | L419 |
| 冻结兜底 (掉帧急停) | 300 ms | L311 |
| 摔落急停阈值 | `fallDistance > 7.0` | L318 |
| 相机接管累加阈值 | 25.0 | L811 |
| 相机接管衰减 | `elapsed_ms * 0.045` / 帧 | L802 |
| 接管检测启动延迟 | `+125 ms` | L901 |
| 自适应瞄准有效期 | 125 ms | L1199 |
| 固定旋转时长 | 50 ms（setup 也用 50） | L892/931/1213 |
| 拒绝目标记忆 | 4 tick | `isRejectedTarget` |
| 搜索预算 | 直线 8ms(重试 4ms)；斜向 10ms(6ms) | L1852/1947/2017 |
| 视线 pitch 下限 | 40°（legacy 搜索）；上限 89° | L2323 |
| `reach()` | 创造 5.0 / 生存 4.5 | L3031 |
| raycast 距离（激活） | 4.5 | L454 |
| setup 前摇 tick 数 | 12（其中 jump 从第 6 tick 开始） | L883 |

### 1.5 与其他管理器的耦合

| 目标 | 调用 | 说明 |
|---|---|---|
| `RotationManager` | **零调用** | ⚠️ 反转常识：Telly 不走 `ATALOSS.rotationManager.setRotation()` |
| `RotationState` | 间接：`event.setPervRotation(...)` → `MixinEntityPlayerSP` → `RotationState.applyState` | 副作用是 `RotationState.getPriority()==5`，但 Telly 自己管移动，所以 `MoveUtil.fixStrafe` 不会被它用到 |
| 旋转落地 | `player.setYaw()/setPitch()`（`Entity.setYaw` 直写 `rotationYaw`） | 每 tick 被 `holdScriptedRotation` + `applySmoothedRotation` 反复写 |
| `ModuleManager` | `getModule(String)`（Eagle / SafeWalk / Timer）、`modules.get(...)` 未直接用 | `ModulesApi` 的薄包装 |
| `PropertyManager` | `getProperty(module, name)`（`getButton` 兜底路径） | — |
| `Eagle` | `eagle.toggle()`（关/恢复），`try/catch(Throwable ignored)` | 激活时关、结束恢复 |
| `SafeWalk` | `modules.isEnabled/disable/enable("SafeWalk")` + `SafeWalkEvent.setSafeWalk(false)` | 双保险 |
| `Timer` | `modules.isEnabled("Timer")` → 用 `System.currentTimeMillis()/50` 当 tick 号 | ⚠️ **ATALOSS 没有 Timer 模块**，永远返回 false |
| `Scaffold` | `BridgeApi.has("ScaffoldRunning")` | ⚠️ Scaffold **从不写这个 key**，互斥实际靠 `PlacementArbiter*` |
| `PacketUtil` | 无 | 放置走 `playerController.onPlayerRightClick`，swing 走 `sendQueue.addToSendQueue(C0A)` |
| `ItemUtil.isBlock` | `Entity.isHoldingBlock()` | — |
| `BlockUtil.isInteractable` | `Block.name/interactable` 构造 | — |
| `ChatUtil.sendRaw` | `printStatus` | — |
| `ChatColors.formatColor` | `UtilApi.color` | — |
| **完全不碰** | `LagManager`、`BlinkManager`、`DelayManager`、`FloatManager`、`PlayerStateManager` | 任务书假设的耦合**不存在**；Telly 是"纯本地输入 + 旋转 + 放置"方案 |

### 1.6 可疑点 / Bug / 隐患

**A. 移植产生的死代码（最大问题）**
1. `onKey()` 从未被调用 → **手动按键脱手检测完全失效**。玩家按 WASD 不会停脚本，只能靠 `detectManualCameraTakeover()`（要累计 25°）。
2. `onWorldJoin()`、`onPostPlayerInput()`、`autoPlaceOnRenderTick()`、`applyGCD()`、`swingReset()`、`CPacket.C03` 的 yaw/pitch/ground 全未使用。
3. `rotationTick`、`antiSway` 局部变量、`candidateResolvedThisTick` 的部分赋值路径冗余。

**B. 数值/边界 Bug**
4. `rotationGcd()` 在 `mouseSensitivity == 0` 时为 `0.0f` → `delta % gcd` = **除零（NaN/ArithmeticException 风险）**；`quantizeFrom` 同。
5. `STOP_DESTROY`… 不适用，但 `isInActivationFaceCenter` 与 `drawActivationFaceRegion` 的坐标变换靠手写 4 分支，NORTH/WEST 的 `1.0 -` 反转容易错位（渲染框与实际激活区可能不重合）。
6. `isStraightCenterBelowAir` 用 `isReplaceableName(..., airOnly=true)`，而 `isReplaceableName` 里 `airOnly` 分支**先 return**，`EXPERIMENTAL_REPLACEABLE_BLOCKS` 永远不会在 airOnly 路径生效——两套常量表语义混乱。
7. `UNPLACEABLE_EXACT` 含 `"gravel"`、`"clay"`、`"sand"`、`"soul_sand"` — 这些在 BedWars 里是**完全可放**的方块，被误判为不可用 → `isUsableBlockStack` 返回 false → auto-swap 会跳过它们。
8. `isUsableBlockStack` 用 `stack.name.toLowerCase().contains(...)` 做子串匹配，`"torch"` 会同时命中 `redstone_torch`（意图内），但 `"sign"` 会命中 `"design"` 类命名、`"bed"` 会命中 `"bedrock"`（→ **基岩被当成不可放置**）。
9. `gate: if (score == 0) break;`（AutoBlockIn）同类问题在 Telly 不存在，但 `handleAutoSwap` 的 threshold 逻辑：`heldCount > 5` 才不换，`bestSize` 从 `heldCount` 起步 → 手持 5 个、背包 100 个会换；手持 6 个就永不换（即使背包有 64）——**阈值语义与直觉相反**。

**C. 竞态 / 状态泄漏**
10. `cancelledGhostBlocks` 只在收 S23 时清理。若 `cancelledGhostBlocks.add(placedTarget)` 后**没有** S23（例如服务端接受但没回包），该 key 永久残留 → `attemptPlacement` 里 `cancelledGhostBlocks.contains(supportPos)` 会**永久封锁该支撑面**。
11. `isAutoPlaceActiveWindow` 用 `bridge.has("ScaffoldRunning")`，但 Scaffold 从不写这个键 → 与 Scaffold 同开会双放（靠 `PlacementArbiter` 兜底，而那个只挡"本 tick 第二次"）。
12. `BridgeApi.VALUES` 是 `static HashMap`，`Telly` 每次 new 都共用（`ATALOSS` 只 new 一次，暂无实际危害，但 `bridge.add(null value)` + `has()` 的语义很脆）。
13. `placementTick()` 依赖 `Timer` 模块（不存在）→ 恒走 `player.getTicksExisted()`。若将来加了 Timer，tick 号会跳变，`rejectedTargets`/cache 的"同 tick"判断会瞬间失效。
14. `handleAutoSwap` 直接 `inventory.currentItem = slot`（客户端单边），不发 `C09PacketHeldItemChange` → **服务端手上还是旧物品**，反作弊可检测（同类问题见 Scaffold/AutoBlockIn）。

**D. 性能隐患（非常严重）**
15. `findPitchPlacementForTarget` 最坏 `5 面 × 9 offset × 9 offset = 405` 次 `getSupportFaceHitVec + computePitchToHitVec + isPlacementLookAligned(再 raycast)`，外层 target 最多 27 个 → **单 tick 上万次 raycast**。虽然每层都有 `client.time() >= deadlineMs` 检查，但 `System.currentTimeMillis()` 在 4~10ms 预算内可执行量仍然极大。
16. `getBelowTargets` 有 `cachedBelowTargets + cachedBelowTargetsTick` 缓存（✓），但 `getBelowPlayerFallbackEndpoints` / `findNearestSupportedReplaceableTarget` **无缓存**，每个 target 循环都会重算。
17. `pruneRejectedTargets()` 每 tick 对 `rejectedTargets` 做 `String.split(",")` + `parseInt` ×3，纯字符串哈希（应用 `long` key）。
18. `onRenderTick` 里的原脚本"按帧预搜索候选块"已被**显式删除**（第 959-962 行中文注释：高 FPS 下重复 raycast 导致窗口"未响应"）——说明该模块的性能问题曾被实测踩过坑。

**E. 硬编码常量清单（无 Property 可调）**
`74.52f`（起手 pitch，出现 6 次）、`5`（旋转 priority，与 BedNuker 相同）、`7.0f` fallDistance、`300L` freeze、`25.0f` takeover、`0.045` 衰减、`±2.25°` anti-sway 上限、`0.42/0.78/0.16/0.55/0.60/0.40` PD 系数、`4.5` reach、`40°~89°` pitch 搜索域、`0.38/0.65/0.25/0.75/2.0` 激活几何、`12` setup tick、`21` 相位曲线（3 条 float 数组）。

---

## 2. `AutoBlockIn.java` —— 675 行 / 562 非空

### 2.1 功能与场景
**被围时自动"封盒"（block-in / block-in trap escape）**：用最快的速度把头顶 + 四周 8 格全填满，防止被对手连击/被围杀。BedWars 里被逼到角落或被 2 人夹击时用来自救，也是"AntiObbyTrap"的互补模块。

### 2.2 配置项

| Property | 类型 | 默认 | 范围 | 可见性 |
|---|---|---|---|---|
| `range` | Float | 4.5 | 3.0–6.0 | 恒显 |
| `speed` | Int | 20 | 5–100 | 每 tick 最大转角（度） |
| `place-delay` | Int | 50 | 0–200 | 两次放置的最小间隔 ms |
| `rotation-tolerance` | Int | 25 | 5–100 | 差多少度以内才允许放（度） |
| `item-spoof` | Boolean | true | — | 只做 UI 显示，**无实际逻辑** |
| `show-progress` | Boolean | true | — | 屏幕中上显示 `Blocking: xx%` |
| `move-fix` | Mode | 1 | `NONE/SILENT/STRICT` | 只有 `SILENT(1)` 生效 |

**内部方块优先级表 `BLOCK_SCORE`**（越小越优先，`getUnlocalizedName().replace("tile.","")` 匹配）：
`obsidian 0 → end_stone 1 → planks 2 / log 2 → glass 3 / stained_glass 3 → hardened_clay 4 / stained_hardened_clay 4 → cloth 5`
→ 找到 score 0 立即 `break`（早退优化）。

### 2.3 事件钩子

| 方法 | 事件 | 优先级 | 内容 |
|---|---|---|---|
| `onUpdate` | `UpdateEvent` | HIGH | PRE：读 `event.getYaw/Pitch` 作为 server 视角 → `updateProgress()` → `findBestBlockSlot()` 切槽 → `findBestPlacement()` → 算目标 yaw/pitch → 按 `speed` 线性夹紧 → `event.setRotation(aimYaw,aimPitch,6)`；`moveFix!=0` 时 `setPervRotation(aimYaw,6)` |
| `onTick` | `TickEvent` | HIGH | PRE：`withinRotationTolerance` 通过且距上次放置 ≥ `place-delay` → 用 `aimYaw/aimPitch` 重做 raycast，命中同一 block+face 才 `onPlayerRightClick` + `swingItem` |
| `onMove` | `MoveInputEvent` | 默认(MEDIUM) | `moveFix==1 && RotationState.isActived() && getPriority()==6 && isForwardPressed()` → `MoveUtil.fixStrafe(getSmoothedYaw())` |
| `onSwap` | `SwapItemEvent` | 默认 | `lastSlot = event.setSlot(lastSlot); event.setCancelled(true)` → 锁滚轮换槽 |
| `onRender2D` | `Render2DEvent` | 默认 | 画进度百分比（`(87,85,85)` 自定义色 + `-1090519040` alpha） |

### 2.4 核心算法
```
updateProgress(): filled / 9   （up(2) 1 格 + 4 方向 × (feet, head) 8 格）
   0~33% 红 / 33~66% 黄 / >66% 绿

findBestPlacement():
  eye = getPositionEyes(1.0)
  roofTarget = feetPos.up(2)
  if (!isAir(roofTarget)) → sidesAim()   // 头顶已封 → 只找侧面
  else:
    扫描 [eye±range] × [eye-1 .. eye+range] × [eye±range] 的所有非空气块   ← 三重循环
      粗筛: 中心距 <= (reach+1)^2
      精筛: dist2PointAABB(eye, block) <= reach^2        ← 点到 AABB 的距离
      视线: rayTraceBlocks(eye, blockCenter) 命中该块才收
    supports 按距离升序
    ① 对每个 support 试 tryPlaceOnBlock(support, eye, reach, roofTarget)   // 先封顶
    ② 都不行 → BFS (上限 8964 节点) 从"所有 support 的相邻空气格"扩展到 roofTarget
       回溯路径 → 从终点倒序对路径上每个 air 格找相邻 support 放置
    ③ 再不行 → sidesAim(eye, reach, feetPos)

tryPlaceOnBlock(supportBlock, eye, reach, targetPos):
  for facing in EnumFacing.values():          // 6 面
     if (supportBlock.offset(facing) != targetPos) continue
     n = round(1/STEP) = 5  → (r,c) 6×6 = 36 个采样点
       点 = r*0.2 + rand(±0.02), 夹到 [0,1]
       hitPos = getHitPosOnFace(block, facing, u, v)  // INSET = 0.05
       rot = getRotationsWrapped(eye, hitPos)
       raycast(rot) 命中 (block, facing) → 记录 targetBlock/Facing/HitVec/aimYaw/aimPitch
```

**分包/时序**：`UpdateEvent` 只负责**转视角**；`TickEvent` 再校验 `withinRotationTolerance` 并 `place-delay` 节流后才真正 `onPlayerRightClick`。即 **"先转向 → 下一 tick 才放"**，两个事件天然隔开一帧的缓冲，避免"转身瞬间放置"被服务端判定为异常。

### 2.5 耦合
`RotationState`（isActived/getPriority/getSmoothedYaw）、`MoveUtil.fixStrafe/isForwardPressed`、`UpdateEvent.setRotation/setPervRotation`、`SwapItemEvent.setSlot`、`mc.thePlayer.getPositionVector/getPositionEyes`、`mc.theWorld.rayTraceBlocks`、`ScaledResolution`。**不用任何 manager（Rotation/Lag/Blink/Delay）**。

### 2.6 可疑点
1. **性能爆炸**：`findBestPlacement` 每 `UpdateEvent` 做 `(2*4.5)³ ≈ 700~900` 格扫描，其中每格一次 `rayTraceBlocks` → **每 tick 数百次 raycast**（`range=6` 时约 2000 格）。在 `UpdateEvent`（每 tick 一次）+ `TickEvent`（又一次 raycast）双重负担下，是明显瓶颈。
2. **`Math.random()` 逐点调用**：`tryPlaceOnBlock` 的 `r`/`c` 循环里对每个采样点各调一次 `Math.random()`，`6×6×6 面 = 216` 次 `Math.random()`/tick（全局锁）。应用 `ThreadLocalRandom` 或预生成抖动表。
3. `isAir()` 把 `water/flowing_water/lava/flowing_lava/fire` 视为空气 → **在水里/岩浆里会往水里放方块**，可能自埋。
4. `item-spoof` Property **完全没有任何代码引用**（死配置）。
5. `progress` 的"总格数 = 9"把 `up(2)` 算 1 格、4 方向 × 2 = 8 格，但**不含自己脚下和头顶上方第 3 格**，且 `sidesAim` 的 `DIRS` 是 4 邻域（无对角），所以"100%"其实还没封住对角。
6. `placedThis` 在 `findBestPlacement` 的路径循环里被声明并判断（L387/402），但**永远是 false** → `if (placedThis) break;` 是死分支（复制粘贴残留）。
7. `getSlot()` 返回 `lastSlot` 但没有 `setSlot`，`onDisabled` 恢复槽位用的是**开启时快照**，若期间玩家手动换槽会冲突。
8. `move-fix` 声明 3 个 mode（`NONE/SILENT/STRICT`）但 `STRICT` 与 `SILENT` 走同一分支（`!= 0`），STRICT 无独立语义。
9. `BSF` 注释写 BFS，节点上限硬编码 `8964`（`> 8964` 退出），可疑的"防卡死"魔数。
10. 与 `BedNuker` 互斥：`BedNuker.onTick/onUpdate` 里 `if (autoBlockIn.isEnabled()) return;`（单方向互斥，AutoBlockIn 不知道 BedNuker）。

---

## 3. `Scaffold.java` —— 767 行 / 734 非空

### 3.1 功能与场景
**自动搭桥/搭路（Scaffold）**：边走边往脚下放方块，BedWars 里最常见的位移辅助。带 tower（向上搭柱）、keep-y（保持高度）、multi-place（一次放多块）、safe-walk。

### 3.2 配置项

| Property | 类型 | 默认 | 取值/范围 | 可见性 |
|---|---|---|---|---|
| `rotations` | Mode | 2 | `NONE / DEFAULT / BACKWARDS / SIDEWAYS` | 恒显 |
| `move-fix` | Mode | 1 | `NONE / SILENT` | 恒显 |
| `sprint` | Mode | 0 | `NONE / VANILLA` | 恒显 |
| `ground-motion` | Percent | 100 | 0–100 | 恒显 |
| `air-motion` | Percent | 100 | 0–100 | 恒显 |
| `speed-motion` | Percent | 100 | 0–100 | 恒显 |
| `tower` | Mode | 0 | `NONE / VANILLA / EXTRA / TELLY` | 恒显 |
| `keep-y` | Mode | 0 | `NONE / VANILLA / EXTRA / TELLY` | 恒显 |
| `keep-y-on-press` | Boolean | false | — | `keep-y != 0` |
| `no-keep-y-on-jump-potion` | Boolean | false | — | `keep-y != 0` |
| `multi-place` | Boolean | true | — | 恒显 |
| `safe-walk` | Boolean | true | — | 恒显 |
| `swing` | Boolean | true | — | 恒显 |
| `item-spoof` | Boolean | false | — | 恒显（**无实际逻辑**） |
| `block-counter` | Boolean | true | — | 恒显 |

**静态常量**：`placeOffsets = {0.03125, 0.09375, …, 0.96875}`（16 档，即 `(2k+1)/32`）。

### 3.3 事件钩子

| 方法 | 事件 | 优先级 | 内容 |
|---|---|---|---|
| `onUpdate` | `UpdateEvent` | HIGH | PRE：主逻辑（见 §3.4）；`rotationTick` 递减、stage 计数、切槽、选块、算旋转、`event.setRotation(targetYaw,targetPitch,3)` + `setPervRotation` |
| `onStrafe` | `StrafeEvent` | 默认 | tower 状态机（VANILLA / EXTRA 两套 setMotionY 序列） |
| `onMoveInput` | `MoveInputEvent` | 默认 | `moveFix==1 && RotationState.getPriority()==3 && isForwardPressed()` → `MoveUtil.fixStrafe`；`onGround && stage>0 && forward` → 强制 `jump = true` |
| `onLivingUpdate` | `LivingUpdateEvent` | 默认 | 按 `ground-motion/air-motion/speed-motion` 缩放 `moveForward/moveStrafe`（先做 1/√2 归一）；`shouldStopSprint()` 时 `setSprinting(false)` |
| `onSafeWalk` | `SafeWalkEvent` | 默认 | onGround && motionY<=0 && `PlayerUtil.canMove(motionX,motionZ,-1)` → `setSafeWalk(true)` |
| `onRender` | `Render2DEvent` | 默认 | 屏幕中心右侧画 `N blocks left` |
| `onLeftClick` | `LeftClickMouseEvent` | 默认 | **无条件 cancel** |
| `onRightClick` | `RightClickMouseEvent` | 默认 | **无条件 cancel** |
| `onHitBlock` | `HitBlockEvent` | 默认 | **无条件 cancel** |
| `onSwap` | `SwapItemEvent` | 默认 | 锁槽（同 AutoBlockIn） |

### 3.4 核心算法

**选块（`getBlockData`）**
```java
targetPos = (floor(x), stage!=0&&!shouldKeepY ? min(startY, floor(y)) : floor(y)) - 1, floor(z))
if (!isReplaceable(targetPos)) return null
// 三重扫描 x∈[-4,4], y∈[-4,0], z∈[-4,4] = 9*5*9 = 405 格
  条件: !isReplaceable(pos) && !isInteractable(pos)
        && 玩家到 pos 中心距离 <= blockReachDistance
        && (stage==0 || shouldKeepY || pos.y < startY)
  对每个合法支撑块的 6 个方向（非 DOWN）：邻居 replaceable → 收集支撑块
positions 按"到 targetPos 中心距离²"升序 → 取最近
facing = getBestFacing(blockPos, targetPos)
   遍历 6 面（非 DOWN），只保留 offset 后 pos.y <= targetPos.y 的
   取 offset 后离 targetPos 中心最近的面（平局优先 UP）
```

**放置点/朝向选择**
```java
for (dx in x[]) for (dy in y[]) for (dz in z[])      // x/y/z 默认 16 档
   // 命中面决定哪一维只取单值
   NORTH → z={0.0}; EAST → x={1.0}; SOUTH → z={1.0}; WEST → x={0.0}; DOWN → y={0.0}; UP → y={1.0}
   rotations = RotationUtil.getRotationsTo(relX, relY, relZ, baseYaw, pitch)
   raycast 命中同一 (blockPos, facing) → 记录 |Δyaw|+|Δpitch| 最小的一组
bestYaw/bestPitch → canRotate = true
```
最坏：`16 × 16 × 16 = 4096` 次（实际上只有两维 16 档，第三维固定 1 → **256 次** raycast/tick）。`multiplace` 时 `getBlockData()` 再调 1~3 次 + 每次 raycast。

**Tower 状态机（`onStrafe`）**
- `tower == VANILLA(1)`：`towerTick 0→1→2→3→1`，用 `motionY = -0.0784000015258789` / `0.42` / `0.75 - posY%1` / `1.0 - posY%1` 精确对齐方块边界。
- `tower == EXTRA(2)`：`towerTick 0..5`，额外做"往朝向前方微移 `getRandomOffset()` 到方块中心"的位置修正（`getRandomOffset() = 0.2155 - rand(1e-4, 9e-4)`），并使用 `towerDelay >= 4` 循环。
- `keep-y == TELLY(3)` 或 `tower == TELLY(3)` 时 `isTowering()` 为真 → 在 `onUpdate` 里做旋转抖动（`tolerance = rotationTick>=2 ? rand(90,95) : rand(30,35)`；`targetPitch = rand(30,80)`）。

**`shouldStopSprint()`**：非 towering 且 (`keepY` 不是 1/2 或 stage<=0) 且 `sprint == NONE` → 强制停冲刺。

**互斥**：`canPlace()` — `BedNuker.isEnabled() && BedNuker.isReady()` → 不放；`LongJump.isEnabled() && isAutoMode() && !isJumping()` → 不放。

### 3.5 耦合
`RotationState`、`MoveUtil`（adjustYaw/fixStrafe/getSpeedLevel/setSpeed/getMoveYaw/isForwardPressed）、`RotationUtil`（quantizeAngle/wrapAngleDiff/getRotationsTo/rayTrace/clampAngle）、`RandomUtil`、`BlockUtil`（isReplaceable/isInteractable/isSolid/getClickVec/getHitVec）、`ItemUtil`、`PlayerUtil`、`PacketUtil`（C0A）、`BedNuker`、`LongJump`、`HUD`、`Potion.jump`。

### 3.6 可疑点
1. `item-spoof` Property **零引用**（死配置，同 AutoBlockIn）。
2. `placeOffsets` 是 `private static final`，但内层循环**每 tick 新建 `double[] x/y/z` 再赋值**（`z = new double[]{0.0}` 等）——本可直接引用数组，白造对象。
3. `getBlockData()` 的三重扫描 405 格 × 每格最多 5 次 `isReplaceable` + `getDistance`，且 `multiplace` 时**每 tick 再跑 1~3 遍** → 最多 ~1600 格判定/tick。
4. `onLeftClick/onRightClick/onHitBlock` **无条件 cancel**（不管 `canPlace()`），会导致模块开启时完全无法手动放置/挖掘，即使 BedNuker 正忙。
5. `this.stage` 的调整逻辑很怪：`stage--` / `if (stage<0) stage++`，只有 `==0` 时才设 1；`keep-y` 的 `EXTRA(2)` 分支与 `shouldKeepY` 的交互在 `onUpdate` 尾部（L460-470）与 `onStrafe` 的 `getBlockData()` 重复放置。
6. `distanceToEdge()` 用 `Math.ceil(posX) - posX` / `posX - Math.floor(posX)`，在整数边界（posX 恰为整数）时返回 0 → tower EXTRA 的偏移会被 `Math.min(offset, distance-0.05)` 削到负值。
7. 缩放 `moveForward/moveStrafe` 时先做 `1/√2` 归一，但 `Eagle`（LOWEST）在 `MoveInputEvent` 里也乘 `0.3`，`Scaffold`（MEDIUM）与 `Eagle`（LOWEST）叠加顺序会互相影响 —— 两者同开时移动速度不是单模块预期值。
8. `getSlot()` 有 getter 无 setter；`lastSlot` 在 `onEnabled` 快照，`onDisabled` 恢复，与 `AutoBlockIn` 同款单边切槽（不发 C09）。
9. `shouldStopSprint()` 的布尔表达式 `(void)` 写得很别扭：`(!stage || this.stage <= 0) && sprintMode==0`，stage 变量遮蔽了字段语义。
10. `onRender` 每次渲染都遍历 9 个槽算 block count，并对每格调 `BlockUtil.isInteractable` + `isSolid`（可缓存到 tick）。

---

## 4. `BedNuker.java` —— 638 行 / 602 非空

### 4.1 功能与场景
**自动拆床（Bed Nuker）**：BedWars 里自动找到最近的敌方床、算最快破坏路径（含环绕保护方块）、自动切镐、自动 breakout。`LEGIT` 模式模拟手动挖；`SWAP` 模式先做"预破坏"（到 100% 才切工具并 STOP）。

### 4.2 配置项

| Property | 类型 | 默认 | 取值/范围 |
|---|---|---|---|
| `mode` | Mode | 0 | `LEGIT / SWAP` |
| `range` | Float | 4.5 | 3.0–6.0 |
| `speed` | Percent | 0 | 0–100（每 1% 减 0.3% 所需进度） |
| `ground-spoof` | Boolean | false | 用"onGround=true"的挖掘速度算进度（伪造落地加速） |
| `ignore-velocity` | Mode | 0 | `NONE / CANCEL / DELAY` |
| `surroundings` | Boolean | true | 先拆床周围的保护块 |
| `tool-check` | Boolean | true | 没镐就跳过该目标 |
| `whitelist` | Boolean | true | 用 3 秒后扫描的己方床白名单排除 |
| `swing` | Boolean | true | false 时发 `C0APacketAnimation` 代替 `swingItem` |
| `move-fix` | Mode | 1 | `NONE / SILENT / STRICT` |
| `show-target` | Mode | 1 | `NONE / DEFAULT / HUD` |
| `show-progress` | Mode | 1 | `NONE / DEFAULT / HUD` |

### 4.3 事件钩子

| 方法 | 事件 | 优先级 | 内容 |
|---|---|---|---|
| `onTick` | `TickEvent` | HIGH | PRE：主状态机（`breakStage 0→1→2`）；若 `AutoBlockIn` 开启则整体跳过 |
| `onUpdate` | `UpdateEvent` | **LOWEST** | PRE：`isReady()` 时算目标旋转 `event.setRotation(...,5)`（LOWEST 让它**最后**写，覆盖别人） |
| `onPlayerUpdate` | `PlayerUpdateEvent` | 默认 | `isBreaking() && !attacking && !digging && !placing && !swinging` → 补一次 `doSwing()` |
| `onMoveInput` | `MoveInputEvent` | 默认 | `moveFix==1 && RotationState.getPriority()==5 && forward` → `fixStrafe` |
| `onKnockback` | `KnockbackEvent` | HIGH | `ignore-velocity == CANCEL(1)` 且有目标 → `setCancelled(true)` 并把 x/y/z 覆盖为当前 motion |
| `onRender2D` | `Render2DEvent` | 默认 | 画进度 `xx%` |
| `onRender3D` | `Render3DEvent` | LOW | `sendBlockBreakProgress(id, target, progress*10-1)`；`show-target` 时画目标盒（`r/b/g` 顺序写错见 §4.6） |
| `onLoadWorld` | `LoadWorldEvent` | 默认 | `waitingForStart = false` |
| `onPacket` | `PacketEvent` | 默认 | 见下 |
| `onLeftClick` | `LeftClickMouseEvent` | 默认 | `isReady()` 或鼠标指向方块 → cancel |
| `onRightClick` | `RightClickMouseEvent` | 默认 | `isReady()` → cancel |
| `onHitBlock` | `HitBlockEvent` | 默认 | 同 `onLeftClick` |
| `onSwap` | `SwapItemEvent` | 默认 | `savedSlot != -1` → cancel（锁槽） |

**`onPacket` 的三种副作用**
1. `S02PacketChat` 含 `"§e§lProtect your bed and destroy the enemy bed"` 或 `"§e§lDestroy the enemy bed and then eliminate them"` → `waitingForStart = true`
2. `S08PacketPlayerPosLook && waitingForStart` → 清空白名单，**`scheduler.schedule(..., 1s)` 扫描 51×51×51 找自己的床**
3. `ignore-velocity == DELAY(2)`：`S12PacketEntityVelocity`（自己 && motionY>0）或 `S27PacketExplosion`（有位移）→ `delayManager.delay(BED_NUKER)` + 直接 `delayedPacket.offer(packet)` + `event.setCancelled(true)`

### 4.4 核心算法

**挖掘进度（完全复刻 MC 1.8 `PlayerControllerMP` 公式）**
```java
getDigSpeed(state, slot, onGround):
   base = item.getDigSpeed(state)              // 空手 = 1.0
   if (base > 1.0 && efficiency>0) base += eff² + 1
   Haste: * (1 + (amp+1)*0.2)
   MiningFatigue: *0.3 / *0.09 / *0.0027 / *8.1e-4
   inWater && !AquaAffinity: /= 5
   !onGround: /= 5                              // boolean5 = mc.thePlayer.onGround 或伪造值
getBreakDelta = digSpeed / hardness / (canHarvest ? 30 : 100)
```
`speed` Property 的作用：完成阈值 `1.0 - 0.3 * speed/100`。

**主状态机（`onTick`）**
```
前置: AutoBlockIn 开着 → return
      targetBed 变空气 / 超出 range → restoreSlot + resetBreaking
      targetBed 不是床 && findNearestBed() != null → resetBreaking
      mode==LEGIT 且 savedSlot==-1 → 保存当前槽并切到 findInventorySlot(工具)  + syncHeldItem()
switch (breakStage):
  0: if (!isUsingItem()) → doSwing(); C07(START_DESTROY_BLOCK); doSwing();
                            effectRenderer.addBlockHitEffects(); breakStage=1
  1: 累加 breakProgress += getBreakDelta(...); tickCounter++
     delta = tick * getBreakDelta(state, target, slot, onGround && ground-spoof)
     addBlockHitEffects
     if (breakProgress >= 1 - 0.3*speed%  || delta >= 1 - 0.3*speed%):
        mode==SWAP → readyToBreak = true; 切槽; 若正使用物品再切到下一格
        C07(STOP_DESTROY_BLOCK); doSwing()
        playAuxSFX(2001, target, stateId); world.setBlockToAir(target)   ← 本地预测破坏
        timer.reset(); breakStage = 2
  2: restoreSlot(); resetBreaking()
若 targetBed 仍存在 → return
若 allowEdit && timer.hasTimeElapsed(500) → findNearestBed() 重新选目标，重置进度
```

**目标搜索（`findNearestBed` → `findTargetBed`）**
```java
三重循环 x,y,z ∈ [pos-6, pos+6] = 13³ = 2197 格
  排除白名单里的床；block instanceof BlockBed && isBlockWithinReach(range)
  → targets 按到眼睛距离² 升序
  for 每个 bed:
    if (surroundings) {
        pos = validateBedPlacement(bed)
        if (pos != null) { if (toolCheck && !hasProperTool(block)) continue; return pos; }
    }
    return bedPos
```
**`validateBedPlacement`（包围块选择）**
```java
对床的 HEAD/FOOT 两格 × 5 个方向(UP,N,S,W,E)：
   if (BlockUtil.isReplaceable(neighbor)) return null;   // 有缺口 → 床可直接拆, 不走这条路
   if (!(neighbor instanceof BlockBed)) 收集
按 calcBlockStrength 降序（易拆优先），再按距离升序 → 取第 1 个
```

**`getHitFacing`**：`blockPos + (0.5, 0.25, 0.5)` 到眼睛算角度，`rayTrace(..., 8.0, 1.0f)`（**hardcode 8.0，忽略 `range`**），失败默认 `EnumFacing.UP`。

### 4.5 耦合
`DelayManager`（`setDelayState/delay/delayedPacket/getDelayModule` + `DelayModules.BED_NUKER`）、`PlayerStateManager`（4 个 boolean）、`RotationState`、`RotationUtil`、`MoveUtil.fixStrafe`、`ItemUtil.findInventorySlot`、`BlockUtil`、`PlayerUtil.canReach/isBlockWithinReach`、`PacketUtil.sendPacket`、`RenderUtil`、`ColorUtil.interpolate`、`TimerUtil`、`IAccessorPlayerControllerMP.callSyncCurrentPlayItem`、`BedESP.getHeight()`、`HUD.getColor/scale/shadow`、`AutoBlockIn`、`ChatColors`。

### 4.6 可疑点
1. **`onRender3D` 颜色通道写错**：`int r = color.getRed(); int g = color.getBlue(); int b = color.getGreen();` → **蓝绿互换**，进度色偏色（红→黄→绿 会显示成红→青→绿）。
2. **`getHitFacing` 硬编码 reach = 8.0**，与 `range`（3–6）不一致；`EnumFacing.UP` 兜底可能对着无效面发 C07。
3. **`scheduler` 泄漏**：`Executors.newScheduledThreadPool(1)` 在构造函数创建，**全类无 `shutdown()`**；`onDisabled` 也不取消已排队任务。同类问题在 `BedTracker` 也有。
4. **跨线程访问 `mc.theWorld`/`mc.thePlayer`**：`scheduler.schedule(() -> { for 51³ 扫 mc.theWorld.getBlockState ... })` 在**非主线程**读世界和玩家位置 → 数据竞争（Minecraft 世界不是线程安全的）。
5. **`51³ = 132,651` 次 `getBlockState`** 在 1 秒延迟后一次性跑完（BedTracker 是 3000ms 后），会明显掉帧。
6. `delayManager.delay(BED_NUKER)` 只设 `delayModule` 字段，**不重置 `delay` 计时器**；而 `setDelayState(true, ...)` 才 `delay=0`。两条路径混用同一状态机 → 回放时机不明确。
7. `resetBreaking()` 在 `targetBed == null` 时会**跳过** `sendBlockBreakProgress(-1)`（有 null 检查，正确），但 `breakProgress` 的 `ground-spoof` 分支用 `tickCounter * getBreakDelta(..., true)`，另一个分支用累加 → **两条进度计算口径不一致**，`calcProgress()` 显示会跳变。
8. `onTick` 里 `restoreSlot()` 在 `breakStage==2` 与 L423 各调一次，且 `mode==SWAP` 时在 L383 又改一次 `savedSlot`（两次 `savedSlot = currentItem` 语义不同）→ 槽位恢复可能恢复到错误格子。
9. `getSuffix()` 用 `CaseFormat.UPPER_UNDERSCORE.to(UPPER_CAMEL, mode)` 把 `LEGIT` 变 `Legit` —— 只是显示。
10. `bedWhitelist` 是"自己床的位置"，但扫描是在 `S08` 后 1 秒（BedTracker 是 3 秒）—— 1 秒内可能还没加载完区块 → 白名单可能为空 → **可能去拆自己的床**。
11. `onUpdate` 优先级 `LOWEST` + `priority=5`，与 `Telly` 的 `priority=5` 平局时**后注册者赢**（Telly 在 `ATALOSS` 里注册顺序晚于 BedNuker）→ 两者同开会互相抢旋转。

---

## 5. `BedTracker.java` —— 341 行 / 329 非空

### 5.1 功能与场景
**敌方逼近己方床的预警 + 自动 /lobby 宏**。开局保护自己床：有人靠近（默认 48 格内）/ 有人掏出末影珍珠 → 聊天栏告警 + 播声音；可选"macro"在有人靠近时自动执行 `/lobby` 逃跑（BedWars 常见保命脚本）。带 HUD 显示床坐标状态与距离。

### 5.2 配置项（全部）

| Property | 类型 | 默认 | 范围/取值 | 可见性条件 |
|---|---|---|---|---|
| `alerts` | Boolean | true | — | 恒显 |
| `alerts-range` | Int | 48 | 8–128 | `alerts` |
| `alerts-on-pearl` | Boolean | true | — | 恒显 |
| `alerts-sound` | Mode | 1 | `NONE / MEOW / ANVIL` | `alerts \|\| alerts-on-pearl` |
| `alerts-frequency` | Int | 5 | 1–30（秒） | `alerts \|\| alerts-on-pearl` |
| `macro` | Boolean | false | — | 恒显 |
| `macro-range` | Int | 24 | 8–128 | `macro` |
| `macro-on-pearl` | Boolean | false | — | 恒显 |
| `macro-text` | Text | `/lobby` | — | `macro \|\| macro-on-pearl` |
| `macro-delay` | Int | 1 | 1–10（秒） | `macro \|\| macro-on-pearl` |
| `hud` | Boolean | true | — | 恒显 |
| `hud-position-x` | Mode | 0 | `LEFT / MIDDLE / RIGHT` | `hud` |
| `hud-position-y` | Mode | 0 | `TOP / MIDDLE / BOTTOM` | `hud` |
| `hud-offset-x` | Int | 2 | 0–255 | `hud` |
| `hud-offset-y` | Int | 2 | 0–255 | `hud` |
| `hud-scale` | Float | 1.0 | 0.5–1.5 | `hud` |
| `hud-shadow` | Boolean | true | — | `hud` |

**构造**：`super("BedTracker", false, true)` → `hidden = true`（默认不在 GUI 列出，需 `ShowCommand`）。

### 5.3 事件钩子

| 方法 | 事件 | 优先级 | 内容 |
|---|---|---|---|
| `onTick` | `TickEvent` | 默认 | **仅 POST**（`event.getType() == POST`）才跑主体：探测珍珠实体 + 遍历玩家 + 距离判定 + 告警/宏 |
| `onRender` | `Render2DEvent` | LOW | 画 `Bed: true/false | Distance: N`（距离色带） |
| `onLoadWorld` | `LoadWorldEvent` | 默认 | `waiting = false` |
| `onPacket` | `PacketEvent` | 默认 | `S02PacketChat` 匹配开局提示 → 清状态 + `waiting = true`；`S08PacketPlayerPosLook && waiting` → **3 秒后**异步扫 51³ 定位自己的床 |

### 5.4 核心算法

**床定位（`onPacket` 里的 lambda）**
```java
按开局聊天 "§e§lProtect your bed and destroy the enemy bed"
           或 "§e§lDestroy the enemy bed and then eliminate them" 判定"新局"
   → 清 alertCooldowns / trackedPearls / whitelistedPlayers, bedPos = null, waiting = true
收到 S08（自己已经落到出生点/被传送后）
   → 3 秒后（executor.schedule）以眼睛为中心 51×51×51 扫描
   → 找到第一块 BlockBed 即 bedPos，播 "note.pling"，打印坐标
```

**告警逻辑（每 tick POST，只在 `isBed(bedPos)` 时）**
```
1) 新珍珠检测: mc.theWorld.loadedEntityList 里所有 EntityEnderPearl
   未记录过 → 记录 + 若 alerts-on-pearl: 聊天告警 "Detected Ender Pearl ⚠"
              + 若 macro-on-pearl: 满足 macro-delay → 触发宏
2) 玩家扫描: loadedEntityList.stream().filter(EntityPlayer)
             .filter(!isBot && !whitelistedPlayers.contains(name)).collect(toList())
   - isSameTeam(player) → 加入 whitelistedPlayers（永久白名单）
   - else:
       距离床中心的 distance
       alerts && distance < alerts-range: 冷却(alert-frequency 秒/人) → 聊天 "xx is N blocks away from your bed ⚠"
       alerts-on-pearl && 手持 ItemEnderPearl: 冷却 → 聊天 "xx has Ender Pearl ⚠"
       (macro && distance < macro-range) || (macro-on-pearl && isPearl) → 满足 macro-delay → marco = true
3) pearl 命中 → playAlertSound()（MEOW = "mob.cat.meow" / ANVIL = "random.anvil_land"）
4) marco → 聊天打印 + ChatUtil.sendMessage(macroText) → 真的发 /lobby 指令
```
**HUD 颜色带（`getHudColor`）**：`<0` 白；`<=100` 绿；`100~114` 黄→绿插值；`114~128` 红→黄插值；`>128` 红。

### 5.5 耦合
`TeamUtil.isBot/isSameTeam`、`ChatUtil.sendFormatted/sendRaw/sendMessage`、`SoundUtil.playSound`、`ColorUtil.interpolate`、`ChatColors`、`ATALOSS.clientName`、`ScheduledExecutorService`、`ItemEnderPearl`。

### 5.6 可疑点
1. **`whitelistedPlayers` 在 stream 遍历中被修改**：`onTick` 的 `for (EntityPlayer player : ...collect(toList()))` 循环体里 `this.whitelistedPlayers.add(...)`，而 stream 的 `.filter(!whitelistedPlayers.contains(name))` 已经先求值成 List，所以**本次不会 CME**；但如果换成惰性 stream 就会。同时该 set 只增不减，长时间对局会缓慢增长（同队玩家数量有限，实际低危）。
2. **`trackedPearls` 永不清理实体引用**：只在开局/禁用时 `clear()`。`LinkedHashSet<EntityEnderPearl>` 持有一路所有珍珠的强引用，长局内存泄漏（同款问题在 BeatNuker 的 `bedWhitelist`，但那个是 `ArrayList<BlockPos>` 只增不减）。
3. **`executor` 无 `shutdown()`**，且 `onDisabled` 不取消排队任务 → 禁用后仍会执行扫描并改 `bedPos`。
4. **跨线程读 `mc.thePlayer` / `mc.theWorld`**（3 秒后的 51³ 扫描）→ 数据竞争。
5. **51³ = 132,651 次 `getBlockState`** → 掉帧。
6. `alerts-on-pearl` 的聊天告警（L144）**没有冷却检查**（直接发），而玩家扫描（L173/183）有冷却 → 珍珠实体告警会刷屏（多个珍珠 = 多条）。同时 `pearl = true` → 每次都 `playAlertSound()`。
7. `waiting` 只在 `LoadWorldEvent` 或收到 S08 后置 false；若收不到 S08（例如一直没被传送），`waiting` 会一直为 true，之后每个 S08 都会重新触发扫描。
8. `alertFrequency` 用 `cooldown + freq*1000 <= millis` 判断，但 `alertCooldowns` 的 key 是玩家名 → 同一玩家的"靠近告警"和"珍珠告警"共用一个冷却（可能互相吞掉告警）。
9. `hudOffX` 范围 0–255 却当做像素偏移用，且 `case 2`（RIGHT）里 `scale = (scale + 1.0F) * -1.0F` 这种"先转负再加"的写法容易在 `hudScale != 1` 时错位。
10. `macroText` 默认 `/lobby`，`ChatUtil.sendMessage` 会**真的发出指令**——`macro` 默认 false 所以安全，但 `macro-on-pearl` 默认也 false，一旦玩家误开就是"见珍珠即退房"。
11. `getHudColor` 的 `distance < 0` 分支（白）实际不可达（距离恒 ≥0）。

---

## 6. `BedESP.java` —— 151 行 / 141 非空

### 6.1 功能与场景
**床透视**：把已加载的床用完整包围盒/填充框画出来（配合 `obsidian` 选项把床周围的黑曜石保护层也高亮），BedWars 里用来判断对面床是否上保护、保护了几层。

### 6.2 配置项（全部）

| Property | 类型 | 默认 | 取值 | 可见性 |
|---|---|---|---|---|
| `mode` | Mode | 0 | `DEFAULT(高 0.5625) / FULL(高 1.0)` | 恒显 |
| `color` | Mode | 0 | `CUSTOM / HUD` | 恒显 |
| `custom-color` | Color | `(int) 8085714755840333141L` | RGB ≤ 0xFFFFFF | `color == 0` |
| `opacity` | Percent | 25 | 0–100 | 恒显（**声明了但渲染里没用**，见 §6.6） |
| `outline` | Boolean | false | — | 恒显 |
| `obsidian` | Boolean | true | — | 恒显 |

### 6.3 事件钩子

| 方法 | 事件 | 优先级 | 内容 |
|---|---|---|---|
| `onRender3D` | `Render3DEvent` | 默认 | 遍历 `beds` 集合；校验 HEAD/FOOT 配对；`obsidian` 时画两侧黑曜石；画床本体框 |
| `onEnabled` | — | — | `mc.renderGlobal.loadRenderers()`（强制重载区块以填充 `beds`） |

### 6.4 核心算法
```
beds 集合由 MixinBlockRendererDispatcher 在渲染方块时 add（不是网络包/世界扫描）
onRender3D:
  for (BlockPos p : beds)                       // CopyOnWriteArraySet，渲染线程安全
     state = world.getBlockState(p)
     if (state.getBlock() instanceof BlockBed && PART == HEAD):
        opposite = p.offset(FACING.getOpposite())
        if (对面也是 BlockBed 且 PART == FOOT):   // 只处理"完整的床"，避免重复绘制
           if (obsidian):
              for facing in {UP,N,S,W,E}:
                 offsetX = p.offset(facing); offsetZ = opposite.offset(facing)
                 if (两侧都是黑曜石) → drawObsidianBox(合并 AABB)
                 else if (单侧)      → drawObsidian(那一侧)
           画床本体 AABB（min/max 取 head/foot 两格的并集，Y 高由 getHeight() 决定）
           整体 .offset(-renderPosX, -renderPosY, -renderPosZ)
     else: beds.remove(p)                        // 自清理
  颜色: CUSTOM → customColor；HUD → ((HUD) modules.get(HUD.class)).getColor(now)
```
`getHeight()`：`mode==FULL ? 1.0 : 0.5625`（0.5625 = 床的实际高度）；`BedNuker.onRender3D` 也调它。

### 6.5 耦合
`RenderUtil.enableRenderState/drawBoundingBox/drawFilledBox/drawBlockBoundingBox/drawBlockBox`、`IAccessorRenderManager.getRenderPos*`、`HUD.getColor`、`MixinBlockRendererDispatcher`（数据来源）、`BlockBed/BlockObsidian`。
**不监听任何网络包**，数据完全来自渲染管线的方块渲染回调。

### 6.6 可疑点
1. **`custom-color` 默认值是个 Bug**：`new ColorProperty("custom-color", (int) 8085714755840333141L, ...)` —— `8085714755840333141L` 是**随机的 19 位十进制常量**，`(int)` 强转后只保留低 32 位。`ColorProperty` 的验证器是 `rgb <= 16777215`，而 `setValue` 只在**赋值时**校验，构造时直接塞进 `value` → 初值可能是负数/超大值，`formatValue` 里的 `String.format("%06X", v).substring(0,6)` 可能抛 `StringIndexOutOfBoundsException`（负数的 hex 串更短）。这是明显的复制粘贴/生成器残留。
2. **`opacity` Property 零引用**：声明了 `new PercentProperty("opacity", 25)`，但 `drawFilledBox`/`drawBoundingBox` 全部用**硬编码 alpha 255**（`drawObsidianBox`、本体绘制都是 255）。配置项完全无效。
3. `drawObsidianBox` / `drawObsidian` 里黑曜石颜色硬编码 `(170, 0, 170)`，与 `custom-color` 无关。
4. `obsidian` 分支只检查 `facing ∈ {UP,N,S,W,E}`（**不含 DOWN**），且要求 head 和 foot **同时**有黑曜石才画合并框 → 只保护了一侧时画两个独立框，视觉上容易误判"保护完整"。
5. `else { this.beds.remove(blockPos); }` 在**遍历 `CopyOnWriteArraySet` 时删除**：COW 集合迭代是快照式的，删除不会 CME，但本轮迭代仍在遍历已删元素（可接受）；不过这个 else 分支会把**任何瞬时区块卸载/未加载**的床永久移出集合，需要 `onEnabled` 的 `loadRenderers()` 才能重建。
6. `mode` 只有 `DEFAULT/FULL` 两档，`FULL(1.0)` 会把床周围一格也盖住（看起来像"床变高"），并不是"完整包围盒"的语义。
7. `color == HUD` 时每帧调 `HUD.getColor(System.currentTimeMillis())`，逐床重复计算（`getColor` 内部一般是 HSV 计算），床多时浪费。
8. 遍历中每次 `mc.theWorld.getBlockState(blockPos)` 对同一坐标可能被多次访问（obsidian 循环 5 次 × 2 格 = 10 次 + 本体 2 次），无缓存。

---

## 7. 汇总表

| 模块名 | 物理行 / 非空行 | 核心作用 | 复杂度 | 主要依赖的系统 |
|---|---|---|---|---|
| **Telly** | 3794 / 3344 | 全自动 Legit Telly-Bridge（21 相位状态机 + 曲线旋转 + 静默俯仰放置 + 车道约束 + 抗侧漂 PD + 幽灵方块拦截 + 相机接管检测） | ★★★★★ 极高（脚本移植、10 个内部 API 类、~120 个方法/字段、4 条并行子逻辑） | 自身脚本 API 层；`UpdateEvent`(HIGHEST)/`MoveInputEvent`/`PacketEvent`/`SafeWalkEvent`；`player.setYaw/setPitch` **直写**；`ModuleManager`(Eagle/SafeWalk/Timer)；`ItemUtil`/`BlockUtil`/`RotationUtil`/`ChatUtil`。**不碰** Rotation/Lag/Blink/Delay/PlayerState/Float Manager |
| **Scaffold** | 767 / 734 | 自动搭桥 + tower 搭柱 + keep-y 保高 + multi-place | ★★★★ 高（405 格扫描 ×多遍、16² 采样点射线、tower 双状态机、4 档 rotation mode） | `RotationState`(priority 3)、`RotationUtil`、`MoveUtil.fixStrafe/getSpeedLevel`、`BlockUtil`、`PlayerUtil.canMove`、`BedNuker`/`LongJump`(互斥)、`HUD`、`SwapItemEvent`/`SafeWalkEvent` |
| **BedNuker** | 638 / 602 | 自动拆床（含保护块优先级 + SWAP 预破坏 + 反击退延迟） | ★★★★ 高（复刻 MC 挖掘公式、3 阶段状态机、13³ 目标搜索、51³ 白名单扫描、3 种 velocity 策略） | `DelayManager`(BED_NUKER)、`PlayerStateManager`、`RotationState`(priority 5)、`RotationUtil`、`ItemUtil.findInventorySlot`、`PlayerUtil.canReach`、`BedESP.getHeight`、`HUD`、`AutoBlockIn`(互斥)、`ScheduledExecutorService` |
| **AutoBlockIn** | 675 / 562 | 被围时自动封盒（头顶 + 四周 8 格），带方块优先级与 BFS 补线 | ★★★★ 高（~900 格扫描 + 逐格 raycast、BFS 8964 节点、6 面 × 36 采样点、双事件时序） | `RotationState`(priority 6)、`MoveUtil.fixStrafe`、`UpdateEvent`+`TickEvent`、`SwapItemEvent`、`PlayerStateManager`(被 BedNuker 单向引用) |
| **BedTracker** | 341 / 329 | 敌方逼近己方床预警（距离 + 末影珍珠）+ 自动 `/lobby` 宏 + HUD | ★★★ 中（每 tick 全实体扫描、冷却表、3 秒后 51³ 异步定位、6 段 HUD 布局分支） | `TeamUtil.isBot/isSameTeam`、`ChatUtil`、`SoundUtil`、`ColorUtil`、`HUD` 无依赖；`ScheduledExecutorService`；`PacketEvent`(S02/S08) |
| **BedESP** | 151 / 141 | 床透视（床体 + 可选黑曜石保护层高亮） | ★★ 低（数据来自渲染回调，无算法） | `RenderUtil`、`IAccessorRenderManager`、`HUD.getColor`、`MixinBlockRendererDispatcher`（唯一数据源） |

---

## 8. 横向结论（跨模块）

1. **可调性严重不足**：6 个模块共约 60 个 Property，但 `Telly` 的 40+ 个魔数全部硬编码（曲线数组、PD 系数、时间阈值、几何容差），`BedESP.opacity` / `Scaffold.item-spoof` / `AutoBlockIn.item-spoof` 是**零引用死配置**。
2. **性能反模式集中在"三重循环 + 逐格 raycast"**：`AutoBlockIn.findBestPlacement`（~900 格 × raycast）、`Scaffold.getBlockData`（405 格 × multiplace 多遍）、`Telly.findPitchPlacementForTarget`（405 采样点 × 27 target）。只有 Telly 用 `deadlineMs` 做了时间预算，另两个没有。`Telly` 的注释（L959-962）证明作者已在高 FPS 下踩过"窗口未响应"的坑。
3. **跨线程访问游戏世界**：`BedNuker` 与 `BedTracker` 都在 `ScheduledExecutorService` 里扫 51³=132,651 格并读 `mc.thePlayer`/`mc.theWorld`，既掉帧又有数据竞争；两个 scheduler 都**从不 shutdown**。
4. **本地单边切槽**：`Telly.handleAutoSwap`、`Scaffold`、`AutoBlockIn`、`BedNuker` 全部直接改 `inventory.currentItem` 而不发 `C09PacketHeldItemChange` → 服务端视角与本地不同步，是服务器端反作弊/幽灵物品的常见成因。
5. **`RotationState` 派系 vs `RotationManager` 派系**：`Scaffold/AutoBlockIn/BedNuker` 走 `UpdateEvent.setPervRotation` → `RotationState` + `MoveUtil.fixStrafe`（priority 3/5/6）；`Telly` **完全绕过**，直写 `player.rotationYaw/Pitch` + `priority 5`。两派混用时旋转互相覆盖（`UpdateEvent` 同 tick 里 priority 相同者后者赢，而 `Telly` 注册更晚 → Telly 赢）。任务书假设的"Telly ↔ RotationManager 交互"实际**不存在**，这是一个重要的框架级事实。
6. **`MixinEntityPlayerSP` 的静默旋转机制会把 `rotationYaw` 还原给相机**，所以任何"自己写 `turnTo`"的模块（Telly）必须每 tick 重写，且不能用 `player.getYaw()` 作为判断依据（Telly 第 1442 行中文注释明确记录了这个坑）。
7. **模块间互斥全是单向/隐式的**：`BedNuker → AutoBlockIn`（单向）、`Scaffold.canPlace → BedNuker/LongJump`（单向）、`Telly → "ScaffoldRunning"`（**键从没人写**）。没有统一的"放置仲裁"中心，`Telly` 的 `PlacementArbiter*` 是唯一尝试。
