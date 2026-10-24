# ATALOSS（工作目录 `E:\Work\ATALOSS`）项目全解析

> 本文基于对仓库源码、git 历史、构建产物、资源文件与 188 份社区配置的实际阅读整理。
> 代码规模：**247 个 Java 文件 / 26,713 行**（含资源 252 个文件）。

---

## 0. 一句话概括

这是一个 **Minecraft 1.8.9 Forge 客户端 Mod（作弊客户端 / hacked client）**，名为 **ATALOSS**，
是开源客户端 **ATALOSS**（上游作者 `60124808866` / TTHILLTT）的**二次开发分支**，
面向 **BedWars / 竞技 PvP** 场景，核心价值是 ~69 个战斗、移动、世界感知、背包自动化模块，
外加 **20 个从 FPSMaster Edge 移植的 Legit 功能**（HUD 信息 / 视觉辅助 / 聊天与输入类，放在 Legit 菜单里），
外加一套**自绘的 "Liquid Glass" ClickGUI**（FBO + GLSL 折射）和一个从 Raven/Flux 移植过来的
**脚本化 Telly 自动化模块（3344 行，全项目最大文件）**。

它不是服务端插件、不是独立程序：产物是一个 Forge Mod JAR，塞进 `.minecraft/mods` 后随游戏启动。

---

## 1. 身份与谱系（全部有 git 证据）

| 项 | 值 |
|---|---|
| 仓库根 | `E:\Work\ATALOSS` |
| origin | `https://github.com/TTHILLTT/OpenMyauPP.git`（GitHub 上的仓库名尚未同步改成 ATALOSS） |
| 当前分支 | `main`，HEAD = `772fbfa fix: fixed some visual problems`（2026-08-07） |
| 提交总数 | 137（`49aa1e3 Initial commit` 2025-10-02 → 2026-08-07） |
| 跟踪文件 | 648（其中 376 个是 `configs/` 下的社区预设） |
| mod id / jar | `ataloss` / `ATALOSS-1.2.2.jar` |
| 遥测/更新检查 | **无**。全仓库除账号登录外没有任何外部 HTTP 调用 |

### 血统链

1. **Myau Client**（闭源商业客户端，README 里给出 `myau.sell.app`）—— 本项目自称 "for those who can't afford it"。
2. **Open Myau**（`60124808866`，MIT/GPL 的开源重实现）。git 历史里能看到它的作者与多位贡献者：
   `60124808866`、`mKHuYHQ74K`、`ididitforthefun`、`YuiYuK1`、`fyxar`、`shirakawa`、`shae`、`MatrixU5er`、`DeletedUser3001`。
   上游的模块骨架（KillAura / Scaffold / Velocity / AutoBlockIn / Radar / AutoAnduril …）几乎全部来自这一层。
3. **ATALOSS**（本仓库）—— 最近 6 次提交（2026-08-05 ~ 08-07）由 `考锡琪` 完成，是这个分支真正的增量：

| commit | 日期 | 内容 |
|---|---|---|
| `f1536a3` | 2026-08-05 | **Add Legit Telly** —— 新增 `Telly.java`（+3794 行） |
| `d9e89e6` | 2026-08-05 | Add TTHILLTT's Watermark（改的是旧 `ClickGui`） |
| `c4ce839` | 2026-08-05 | README + ClickGui 小修 |
| `659fea1` | 2026-08-06 | **Rebuild**：`Myau.java` → `OpenMyau.java`（并在本次改名中进一步变成 `ATALOSS.java`）、包名/类名重构、modid `openmyaupp`（本次改成 `ataloss`）、`./config/Myau/` → `./config/OpenMyau/`（本次改成 `./config/ATALOSS/`）、**GUI 入口切到 `LiquidClickGui`** |
| `1a06d9c` | 2026-08-06 | Visual optimization（Liquid Glass 打磨） |
| `772fbfa` | 2026-08-07 | fix: fixed some visual problems |

> 结论：**上游主体 = 功能模块 + 事件总线 + Mixin；本分支增量 = 品牌重构 + Liquid Glass GUI + Telly**。
> 但注意 `AutoBlockIn`(632 行) 是**上游**加的（`6b51e31 feat: AutoBlockIn (#34)`），README 把它算作自己的成果是宣传口径。

### 1.1 改名：`OpenMyau++` → `ATALOSS`（本文档记录的是改名后的状态）

| 项 | 改名后 | 之前 |
|---|---|---|
| 主类 / 文件 | `myau.ATALOSS`（`src/main/java/myau/ATALOSS.java`） | `myau.OpenMyau` |
| modid | `ataloss`（→ `mixins.ataloss.json`、refmap、`ataloss_at.cfg`） | `openmyaupp` |
| jar 名 | `ATALOSS-<version>.jar` | `OpenMyau++-<version>.jar` |
| 工程名 | `ataloss`（`settings.gradle.kts`） | `openmyauplusplus` |
| 品牌串 | `ATALOSS`（窗口标题、`mcmod.info`、聊天前缀 `[ATALOSS]`） | `OpenMyau++` |
| 配置目录 | `./config/ATALOSS/` | `./config/OpenMyau/` |
| 账号文件 | `<.minecraft>/ataloss.accounts.json` | `openmyau.accounts.json` |

**保持不变**（按"Myau 不要改"）：Java 包名 `myau`、`baseGroup = myau`、上游沿革提法
（`Myau Client`、`Myau-250910`、`myau.sell.app`）、`me.ksyz` 账号管理器包名与作者署名（`TTHILLTT`）。

**旧路径兼容（自动迁移）**：新路径不存在而旧路径存在时，回退读取旧文件，此后保存一律写入新路径 ——
即"读旧、写新"，不会丢数据。涉及的四个读取点：

- `Config`：`CONFIG_DIR = ./config/ATALOSS/`，`LEGACY_CONFIG_DIR = ./config/OpenMyau/`；加载成功会提示 `migrated from ./config/OpenMyau/`；`.config list` / `.config folder` 用 `Config.activeDir()`（新目录优先，其次旧目录）。
- `PlayerFileManager`（`friends.txt` / `enemies.txt`）：新增 `legacyFile` 参数与控制台 `Migrating ... -> ...` 提示。
- `AccountManager`：`ataloss.accounts.json` 缺失而 `openmyau.accounts.json` 存在时不新建空文件，改从旧文件读取，首次 `save()` 时写入新文件并打印迁移提示。
- `ui/ClickGui`（现作为 `LIGHTWEIGHT` 轻量 GUI 重新启用）里的 `clickgui.txt` 路径同步改名。

---

## 2. 构建系统与技术栈

`build.gradle.kts` + `settings.gradle.kts` + `gradle.properties`：

- **Gradle + Kotlin DSL**，`gg.essential.loom` 0.10.0（architectury-loom，Fabric 系工具链跑 Forge 项目）
- 目标：**Minecraft 1.8.9**，映射 `de.oceanlabs.mcp:mcp_stable:22-1.8.9`，Forge `11.15.1.2318-1.8.9`
- **Java 8 toolchain**（`JavaLanguageVersion.of(8)`），UTF-8 编译
- Mixin：`org.spongepowered:mixin:0.7.11-SNAPSHOT` 以 shadow 方式内嵌；`annotationProcessor` 用 0.8.5
- `architectury-pack200` 做 JAR 压缩；`shadowJar` + `remapJar` 产出最终包
- 开发运行：`me.djtheredstoner:DevAuth-forge-legacy:1.2.1`（`runClient` 时用真账号登录，免正版启动器）
- 三个产物（改名后已用 `gradlew.bat --offline clean build` 重新构建并验证，**BUILD SUCCESSFUL**）：
  - `build/intermediates/ATALOSS-1.2.2-without-deps.jar`（≈812 KB，未 remap）
  - `build/intermediates/ATALOSS-1.2.2-non-obfuscated-with-deps.jar`（≈1.77 MB）
  - `build/libs/ATALOSS-1.2.2.jar`（≈1.88 MB，**分发用 remap 成品**；manifest 里 `MixinConfigs: mixins.ataloss.json`，jar 内含 `myau/ATALOSS.class` 与 `mixins.ataloss.refmap.json`，无任何 `OpenMyau` 残留条目）
- 构建命令：`./gradlew build`（在 Windows 上即 `gradlew.bat build`）

`gradle.properties`：
```
loom.platform=forge   baseGroup=myau   mcVersion=1.8.9
modid=ataloss      version=1.2.2     jarName=ATALOSS
```

---

## 3. 代码规模与目录地图

按包统计（Java 源码）：

| 包 | 行数 | 文件 | 内容 |
|---|---:|---:|---|
| `myau.module` | 16,561 | 91 | `Module` 基类 + `ModuleManager` + **69 个作弊模块** + `legit/` 下 **19 个 Legit 模块**（含 Telly 3344 行） |
| `myau.ui` | 3,568 | 22 | 新 `ui/liquid/*`（约 2,000 行）+ 旧 `ClickGui`/`components`/`dataset`（约 1,500 行，**已成死代码**） |
| `myau.mixin` | 2,032 | 42 | 29 个 `Mixin*` 注入 + 13 个 `IAccessor*` 接口 |
| `myau.util` | 2,002 | 18 | 工具类 + `util/shader/{Shader,GlowShader,OutlineShader}` |
| `me.ksyz` | 1,783 | 14 | 第三方账号管理器（ksyzov/AccountManager，LGPL→GPLv3 改写） |
| `myau.command` | 862 | 17 | 指令框架 + 15 条指令 |
| `myau.management` | 604 | 10 | 旋转/目标/延迟/Blink/Lag/朋友等管理器 |
| `myau.event` | 528 | 11 | DarkMagician6 EventAPI 副本（事件总线） |
| `myau.property` | 364 | 9 | 属性系统（7 种属性类型） |
| `myau.events` | 330 | 24 | 24 个具体事件类 |
| `myau.*` 其余 | ~500 | 9 | `ATALOSS` 入口、`Config`、`init`、`enums`、`data.Box` |

根目录：`build/`（产物+缓存）、`configs/`（188 份社区预设，见 §8）、`images/`（README 截图 8 张，单张最大 4 MB）、
`run/`（空）、`.vscode/`、`gradle/`、`log4j2.xml`、`LICENSE`（GPLv3，35 KB）。

---

## 4. 启动链（从游戏启动到模块就绪）

```
Forge 加载 → MixinTweaker → mixins.ataloss.json
    ├─ package: myau.mixin
    ├─ plugin: myau.init.FMLLoadingPlugin     ← 关键：自动发现
    └─ refmap / minVersion 0.7 / JAVA_8
        → MixinMinecraft.startGame(HEAD)  → new Initializer()   // 仅打印 "Meow!"
        → MixinMinecraft.startGame(RETURN) → new ATALOSS()     // 真正的初始化
```

**Mixin 配置里没有 `mixins` 列表**：`FMLLoadingPlugin.getMixins()` 在运行期扫描
`myau.mixin` 包（目录或 JAR 两种模式：`walkDir` / `walkJar`），把包内所有类都注册为 Mixin。
好处：加文件即生效，不需要改 JSON；代价：无法按平台/版本条件裁剪，且启动时多一次全包扫描。

`ATALOSS.init()` 顺序（`ATALOSS.java:42-183`）：

1. new 出 8 个管理器：`RotationManager / FloatManager / BlinkManager / DelayManager / LagManager / PlayerStateManager / FriendManager / TargetManager`
2. new `PropertyManager`、`ModuleManager`、`CommandManager`
3. `EventManager.register(...)` 注册 `rotationManager / floatManager / blinkManager / delayManager / lagManager / moduleManager / commandManager`（**注意：`friendManager / targetManager / playerStateManager` 不注册事件**）
4. **硬编码 new 出 69 个模块**并塞进 `LinkedHashMap<Class<?>, Module>`（顺序 = GUI 里的顺序）
5. `commandManager.commands.add(...)` 15 条指令
6. **反射扫描每个模块的 `getDeclaredFields()`**，把 `instanceof Property` 的字段收集成
   `propertyManager.properties.put(模块 Class, List<Property>)`，并 `setOwner(module)`；同时 `EventManager.register(module)`
7. `new Config("default", true)` 并 `load()`；`friendManager.load()`、`targetManager.load()`
8. 注册 JVM shutdown hook → 退出时 `config.save()`
9. 从 classpath `/version.json` 读 `version`（build 时由 `${version}` 展开），失败则 `"dev"`
10. `AccountManager.init()`（账号管理器，见 §9）

> 重要副作用：**模块的 Property 是模块构造时才赋值的实例字段**，所以属性集合在启动时一次性冻结；
> 运行期动态增删属性不会被 GUI/配置感知。另外反射只扫本类字段（不含父类），所有模块都直接继承 `Module`，故无碍。

---

## 5. 核心架构

### 5.1 事件总线（`myau.event.*`）

直接抄自 **DarkMagician6 EventAPI**（注释里保留了 2013-2014 年的原始 javadoc 与 bitbucket 链接）：

- `EventManager` 全静态，`HashMap<Class<? extends Event>, List<MethodData>>`，值用 `CopyOnWriteArrayList`
- `register(obj)` 遍历 `getDeclaredMethods()`，命中「参数个数 == 1 且带 `@EventTarget`」即注册；
  参数类型即索引键，`@EventTarget.value()` 即优先级
- 优先级只有 5 档（`Priority`：HIGHEST=0 … LOWEST=4），按档位分桶排序
- `call(event)` 用**反射 `Method.invoke`**（每次事件派发都有反射开销，这是性能热点）
- `Event` 体系：`Event`（空接口）→ `EventCancellable`（可取消）→ `EventStoppable`（可中断传播）；
  `Typed` 抽象出 `EventType`（`PRE / ON / POST / SEND / RECEIVE`）
- 24 个具体事件，**谁 fire 的（全部由 Mixin 触发）**：

| 事件 | 触发点 | 语义 |
|---|---|---|
| `TickEvent(PRE/POST)` | `Minecraft.runTick` HEAD / RETURN | 客户端 tick 前后 |
| `UpdateEvent(PRE/POST)` | `EntityPlayerSP.onUpdate` HEAD / RETURN | **核心**：携带 lastReportedYaw/Pitch 与当前 yaw/pitch，可覆盖旋转（见 §6.3） |
| `PlayerUpdateEvent` | `onUpdate` 中调用 `onUpdateWalkingPlayer()` 之前 | 发包前的最后修改点 |
| `LivingUpdateEvent` | `onLivingUpdate` 调用 super 之前 | 活体更新前（跳跃/输入改写常用） |
| `MoveInputEvent` | `MovementInput.updatePlayerMoveState()` 之后 | 移动输入已算好，可改 `movementInput` |
| `PacketEvent(SEND/RECEIVE)` | `NetworkManager.sendPacket` / `channelRead0` HEAD | 可取消 = 丢包 / 拦包 |
| `Render2DEvent` | `GuiIngameForge`（HUD 渲染） | 2D HUD 绘制 |
| `Render3DEvent` | `EntityRenderer`（renderWorldPass 后） | 3D 世界绘制 |
| `RenderLivingEvent(PRE/POST)` | `RendererLivingEntity` | 实体渲染前后（Chams/NameTags） |
| `KeyEvent(int)` | `Minecraft.runTick` 里 `KeyBinding.setKeyBindState` 的 Redirect | 物理按键按下（模块开关） |
| `LeftClickMouseEvent` / `RightClickMouseEvent` | `Minecraft.clickMouse` / `rightClickMouse` HEAD，可取消 | 左右键动作拦截（Telly 用） |
| `HitBlockEvent` | `Minecraft.sendClickBlockToController` HEAD，可取消 | 挖方块（并 resetBlockRemoving） |
| `SwapItemEvent` | `runTick` 中 `changeCurrentItem` Redirect + `MixinKeyBinding` | 切物品/滚轮 |
| `AttackEvent(Entity)` | `PlayerControllerMP.attackEntity` 中 `syncCurrentPlayItem()` 处 | 攻击发出（KillAura/Velocity 统计） |
| `KnockbackEvent(x,y,z)` | `Entity.setVelocity`（仅自己） | **可改写击退向量**（Velocity 模块） |
| `SafeWalkEvent(boolean)` | `Entity.isOnLadder`/边缘判定 | 可强制 safewalk |
| `StrafeEvent(f,f,f)` | `EntityLivingBase.moveEntityWithHeading` | 改移动方向/速度 |
| `MoveInputEvent` | 同上 | — |
| `CancelUseEvent` | `PlayerControllerMP.onStoppedUsingItem` HEAD，可取消 | 阻止停止使用物品（AutoBlock 相关） |
| `WindowClickEvent` | `PlayerControllerMP.windowClick` HEAD，可取消 | 拦背包点击（InvManager 用） |
| `PickEvent` / `RaytraceEvent` | `EntityRenderer`（getMouseOver 系列） | 改射线距离（Reach 用） |
| `ResizeEvent` | `Minecraft.updateFramebufferSize` RETURN | 窗口尺寸变化 |
| `LoadWorldEvent` | `Minecraft.loadWorld` HEAD | 切世界（重置模块状态） |

### 5.2 模块与属性系统

`Module`（96 行基类）：

- 字段：`name / defaultEnabled / defaultKey / defaultHidden / enabled / key / hidden`
- 生命周期钩子：`onEnabled()`、`onDisabled()`、`verifyValue(String)`（属性变更回调）、`getSuffix()`（HUD 后缀）、`formatModule()`（聊天格式化）
- `setEnabled()` 只在状态**变化**时调用钩子；`toggle()` 播放音效（读 `HUD.toggleSound`）并返回「是否提示」

`Property<T>`（抽象，70 行）：`name + validator(Predicate) + visibleChecker(BooleanSupplier) + value + owner`；
`setValue()` 校验通过后回调 `owner.verifyValue(name)`。
7 个子类：`BooleanProperty / FloatProperty / IntProperty / ModeProperty / TextProperty / ColorProperty / PercentProperty`，
每个都实现 `formatValue()`（GUI 显示）、`parseString()`（聊天指令赋值）、`read/write(JsonObject)`（配置持久化）。
`visibleChecker` 就是 GUI 里的「条件显示」（例如 `allow-tools` 仅当 `weapons-only` 为真时可见）。

### 5.3 管理器（`myau.management`，10 个）

| 管理器 | 职责 |
|---|---|
| `RotationManager` | 简单旋转覆盖：`setRotation(yaw,pitch,priority,force)` 记 delta，在 `TickEvent(PRE, HIGHEST)` 应用并按 `Render3DEvent` 插值；`isRotated()` 供 `MixinEntity` 使用 |
| `RotationState` | 纯静态：给渲染层提供 renderYawOffset/rotationYawHead/pitch 的状态机（`applyState`），含一套 body-yaw 平滑算法（±75° 限制、0.3 插值） |
| `TargetManager` / `FriendManager` | 继承 `PlayerFileManager`，分别读写 `./config/ATALOSS/enemies.txt`（深红）与 `friends.txt`；抽象基类提供 add/remove/isFriend/load/save |
| `BlinkManager` | 按模块维度（`BlinkModules` 枚举）吞包缓存，`isBlinking()/offerPacket()`；由 `MixinNetworkManager` 在所有 send 前拦截 |
| `LagManager` | 假延迟：`handlePacket()` 缓存出包、`isFlushing()` 时放行（配合 `DelayManager`） |
| `DelayManager` | 入包延迟：`shouldDelay(packet)` 在 RECEIVE 处以取消 + 缓存实现（`DelayModules` 枚举标记来源） |
| `FloatManager` | 「浮空/位置欺骗」状态（`FloatModules`） |
| `PlayerStateManager` | 记录本 tick 是否 attack/dig/place/swap/swing；**遇到 `C03PacketPlayer` 就清零** → 等价于「自上次位置包以来做过什么」 |
| `TargetManager` | 由 KillAura 写入当前目标，供 HUD/TargetHUD/其它模块读取 |

---

## 6. 运行期数据流

### 6.1 一个 tick 里的顺序（客户端线程，单线程无锁）

```
Minecraft.runTick HEAD
  └─ TickEvent(PRE)                    ← RotationManager 应用并重置旋转
      ... 输入处理 / KeyBinding.setKeyBindState → KeyEvent → ModuleManager 开关模块
      ... EntityPlayerSP.onUpdate HEAD
          └─ UpdateEvent(PRE)          ← 模块在这里 setRotation(yaw,pitch,priority)
                                        （KillAura=1、Scaffold=3、LongJump=4、Telly=5、
                                          BedNuker=5、AutoBlockIn=6、AntiFireball=0、
                                          TargetStrafe 只用 setPervRotation=10）
          → RotationState.applyState(...)   // 供渲染层
          → 若被旋转：记 pendingYaw/Pitch，Redirect isRiding 时直接改写 rotationYaw/Pitch
      ... onUpdateWalkingPlayer() 之前 → PlayerUpdateEvent
      ... onLivingUpdate → LivingUpdateEvent / MoveInputEvent
      ... onUpdate RETURN
          └─ UpdateEvent(POST)         ← 恢复客户端真实旋转（silent rotation 的关键）
Minecraft.runTick RETURN
  └─ TickEvent(POST)
渲染阶段：Render3DEvent（世界）、Render2DEvent（HUD）
```

### 6.2 网络分包流水线（`MixinNetworkManager`，priority 9999）

**出包 `sendPacket`**：
```
PacketEvent(SEND) → 可取消
  ├─ playerStateManager.handlePacket(packet)      // 记录动作
  ├─ blinkManager.isBlinking() → offerPacket()    // 缓存 + cancel
  └─ lagManager.handlePacket(packet)              // 缓存 + cancel
```
外加延迟管理器分流。**入包 `channelRead0`**：先问 `delayManager.shouldDelay()`（是则直接 cancel），
再 `PacketEvent(RECEIVE)` → 可取消。注意：两个 `sendPacket` 重载都打了补丁（一个带 listener），
而 `channelRead0*` 用通配匹配。**加解密/压缩在更下层，这里已是解好的 Packet 对象。**

### 6.3 两套并存的旋转机制（理解本项目最容易踩的坑）

1. **`UpdateEvent` 路径（主流）**：模块在 PRE 阶段调 `event.setRotation(yaw, pitch, priority)`；
   `MixinEntityPlayerSP` 记录 pending 值，在 `onUpdate` RETURN 时把 `rotationYaw/Pitch` 设回真实值，
   于是**只有 `onUpdateWalkingPlayer` 期间服务器收到的是假朝向**（silent rotation）。
   优先级是个 int，越大越晚覆盖（`lastPriority <= priority`）。
2. **`RotationManager` 路径（旧）**：`TickEvent(PRE, HIGHEST)` 按 delta × ticks 施加旋转，
   只在 `KillAura`（`rotate-3.0.0`/SILENT 分支）与 `MixinEntity` 中还有用。
   它直接改 `mc.thePlayer.rotationYaw`，语义上是「持续旋转」而不是「单 tick 欺骗」。

两者都改同一批字段，**优先级与调用顺序耦合**，是修改旋转相关功能时的首要风险点。

---

## 7. UI 子系统：Liquid Glass ClickGUI

### 7.1 两套 GUI 的关系（重要）

现在有**两套 GUI，二选一**，由 `GuiModule.style`（`LIGHTWEIGHT` / `LIQUID`，默认 `LIQUID`）决定：

| 样式 | 类 | 特点 |
|---|---|---|
| `LIGHTWEIGHT` | `myau.ui.ClickGui`（OpenMyau 原版，419 行 + `components/`/`dataset/`/`callback/`） | 纯 2D 立即模式绘制，无 FBO / 无着色器，开销低；面板位置存 `./config/ATALOSS/clickgui.txt` |
| `LIQUID` | `myau.ui.liquid.LiquidClickGui`（534 行 + 玻璃管线） | 全屏 FBO 折射 + 着色器，视觉最好但开销高（弱机改用 `LIGHTWEIGHT`） |

- **顶部菜单切换（两套 GUI 都有）**：窗口最顶端右侧是一枚 `Cheat | Legit` 双段胶囊/按钮，左半 Cheat、右半 Legit，
  点哪半就切到哪个菜单。状态存在 `GuiModule.menu`（`CHEAT`/`LEGIT`，随配置持久化，两套 GUI 共用），
  也可用 `.clickgui menu legit` 切换。
  - `CHEAT` = 69 个作弊模块按 Combat / Movement / Render / Player / Misc 分成 5 个面板；
  - `LEGIT` = **20 个从 FPSMaster Edge 移植的 Legit 功能**，分成 `Legit HUD`（HUD 信息类）与 `Legit Util`
    两个面板，搜索、滚动、拖拽、属性编辑与 Cheat 页完全一致（面板集合由 `activePanels()` 切换）；
    若没有任何 Legit 模块则退回一张占位卡片。

- **入口**：`GuiModule`（模块名 `ClickGui`，`setKey(KEY_RSHIFT)`，默认关闭）→ `onEnabled()` 里先 `setEnabled(false)`
  （"动作假模块"），再按 `style` 打开对应 GUI；`.module clickgui style LIGHTWEIGHT` 也能切。
- **互斥保证（三层，确保轻量 GUI 渲染时玻璃 GUI 完全不渲染）**：
  1. `GuiModule` 每次只实例化/打开一套；切到 `LIGHTWEIGHT` 时先调用 `LiquidClickGui.shutdown()` 释放全屏 FBO，
     并关掉可能残留的玻璃界面。
  2. `LiquidClickGui.initGui()` / `drawScreen()` 开头都检查 `GuiModule.useLiquidGui()`，为假时**直接 return**
     —— 即使被强行显示，也不会分配 FBO、不会画一个像素（连整帧背景拷贝都不做）。
  3. `LiquidClickGui.getBlurTexture()` 在轻量模式下返回 `-1`，`GlassPanel`/`GlassRenderer` 自动走纯色回退分支，
     不会发生任何玻璃纹理采样。
- 切换 `style` 时（`verifyValue`）会关掉当前界面，下次打开即用新样式，避免旧界面按旧样式继续渲染。
- 轻量 GUI 打开后，`InvWalk` / `AutoAnduril` 里的 `instanceof myau.ui.ClickGui` 重新为真，
  「GUI 内允许走动 / 开 GUI 时允许换盾」在 `LIGHTWEIGHT` 下恢复；`LIQUID` 模式下仍不生效。

### 7.2 渲染管线（文字版）

```
drawScreen(mouseX, mouseY, partialTicks)
├─ renderBlurBackdrop()
│   ├─ GL30.glBindFramebuffer(blurA) + glViewport(全分辨率) + drawTextureQuad(主 FBO 纹理)  // 整帧拷贝
│   ├─ 绑回主 FBO，恢复 viewport / Blend / Alpha / Texture2D
│   └─ 失败时降级为纯色背景 drawRect(0x99050508)，并置 blurFailed 不再重试
├─ 2 个环境光斑（drawRoundedRect，绿/蓝低 alpha）
├─ 标题胶囊（"ATALOSS" / "Dev TTHILLTT"）
├─ 顶部菜单切换胶囊（最右端：`Cheat | Legit`，点击哪半切到哪个菜单，状态存 `GuiModule.menu`）
├─ 搜索胶囊（同一折射 shader，uCapsule=1）+ 3 层阴影 + 发丝线 + 光标闪烁
└─ Cheat 页：5 个 GlassPanel（Combat / Movement / Render / Player / Misc）
   Legit 页（占位壳）：中央玻璃卡片 "Legit Menu / No features yet"，不画搜索栏与面板
    ├─ 面板背景 = LiquidGlassShader 采样 blurA（SDF 圆角盒 + 边缘折射）
    ├─ 3 层阴影 + 圆角外框（arcSq 超椭圆四角，13 段/角）
    ├─ scissor 裁剪 → GlassModuleEntry 列表（开关 / 展开箭头 / hover 弹簧动画）
    └─ 展开后 GlassControls 控件：Boolean→开关胶囊(KnobShader)、Float/Int/Percent→滑条、
        Mode→分段/下拉、Color→R/G/B 子滑条、Text→输入、Bind→绑定按钮
```

**GLSL 资产（全部内联在 `GlassRenderer`，`#version 120`）**：

| Shader | 作用 |
|---|---|
| `LiquidGlassShader` | SDF 圆角盒（n=4 超椭圆角范数，与绘制用 arcSq 完全对齐）／真胶囊分支；`smoothstep` 软边遮罩；**全屏折射**：`disp→scaled=mix(0.2,1.0,…)`，越靠边采样越向中心（透镜感）；不做事后调色 |
| `KnobShader` | 开关圆钮版本：圆形 SDF + 同样的折射曲线 |
| `BlurPassShader` | 9-tap 可分离高斯 —— **定义了但零调用点（死代码）** |
| `SaturationShader` | saturate(180%) —— **同样零调用点（死代码）** |
| `Vertex`（`Shader` 基类） | 固定管线 `gl_ModelViewProjectionMatrix` 直通 |

> **文档与实现已经漂移**：`LiquidClickGui` 类注释、`GlassRenderer` 注释都还写着
> "two-stage downsample + separable gaussian + saturate(180%)"，但 `renderBlurBackdrop()`
> 实际只做「整帧原样拷贝」（代码注释自己也承认 `NO gaussian blur`），
> 且 `blurB` FBO 分配后从未被写入 —— 每开一次 GUI 白占 2 个全分辨率 FBO（含 depth 附件）。
> 另外 `blurB` 只在 `onGuiClosed()` 释放。

### 7.3 交互

- 面板：鼠标左键拖标题（**不做屏幕边界约束**）、滚轮滚动（重写 `handleMouseInput` 直接读 `Mouse.getEventDWheel()`，
  因为 `GuiScreen.handleInput()` 已消费过事件；**方向与旧 GUI 相反**）
- 滑条：按住左键时把每个鼠标移动事件转发到所有面板（`mouseDragged`），实时更新
- 搜索：`searchFocused` 命中检测 + 退格 + 可打印字符过滤，5 个面板按 `trim().toLowerCase()` 过滤
- 键位绑定：`GlassControls.BindButton`（左键进入绑定态，ESC/DELETE/右键清 0）；**不区分鼠标键**（旧 GUI 用 `button-100` 编码鼠标键）
- 命中测试用的是「动画前坐标」，绘制用「动画后坐标」→ 入场动画/滚动期间有数 px 错位
- 视觉动画：`animate/easeOutCubic/spring`（200 ms，overshoot 1.70158）、展开进度指数追踪 `1-exp(-8t)`
- `doesGuiPauseGame() = false`（单人游戏不暂停）

### 7.4 状态与可调项

- **Liquid GUI 无任何持久化**：面板位置与搜索词每次重开归零；模块开关的两种样式（switch / accent）由
  `GlassModuleEntry.switchStyle` 决定，右上角的样式切换按钮已移除，因此固定使用默认的 switch 样式
  （该字段保留，便于以后再加回切换入口）；
  轻量 `ClickGui` 有 `./config/ATALOSS/clickgui.txt` 记录面板位置（`LiquidClickGui` 则完全无持久化）
- 主题全部是 `GlassRenderer` 里的 `static final` 常量（12 个颜色 + 圆角 24F + 面板不透明度 0.72F + 200 ms 时长）
- `GlassControls.buildFor` **忽略 `Property.isVisible()`** → 条件属性无条件渲染（与旧 GUI 行为不一致）

---

## 8. 配置持久化与预设

### 8.1 运行期配置（`myau.config.Config`）

- 路径：`./config/ATALOSS/<name>.json`（`name=!` 或 `default` 归一为 `default`），Gson `setPrettyPrinting`
- 结构：`{ "<模块名>": { "toggled": bool, "key": int, "hidden": bool, "<属性名>": 值 … } }`
  （**用模块名而非类名做键**，改名会丢配置）
- 保存时机：`.config save`、退出时的 shutdown hook；加载时机：启动时 `default`、`.config load <name>`
- 改名兼容：新目录 `./config/ATALOSS/` 里没有该配置、而旧目录 `./config/OpenMyau/` 有时，会**读旧写新**并在聊天里提示迁移（见 §1.1）
- 相关文件：`friends.txt` / `enemies.txt`（每行一个名字，同样读旧写新）、`<mcDataDir>/ataloss.accounts.json`（账号，见 §9）

### 8.2 仓库里的 `configs/`（188 份社区预设）

- 每个预设是一个目录：`2025-01-01T00_31_45.981+08_00-<name>.json-by-<author>/`，
  内含 `Description.txt`（英文说明、绑键提示、配置哲学）+ `<name>.json`
- 全部被 git 跟踪（376 个文件），是**上游的社区配置分发**，**代码完全不读这个目录**
  （客户端只读 `./config/ATALOSS/`，迁移期回退读 `./config/OpenMyau/`）
- 内容面向 BedWars：`KillAura / Scaffold / Velocity / AimAssist / Speed / SafeWalk …`；
  **不含 `Telly`、`AutoBlockIn` 键**（它们晚于这批预设），所以配置能加载但新模块不会被配置驱动

---

## 9. 账号管理器与安全边界（`me.ksyz.accountmanager`）

来源：`github.com/ksyzov/AccountManager`（注释保留 "derived from"，LGPL → 本项目 GPLv3）。

- **登录取令牌流程（`MicrosoftAuth`）**：`login.live.com/oauth20_authorize.srf` →
  本地回环 HTTP 服务器 `http://localhost:25575/callback`（页面即 `src/main/resources/callback.html`，
  47 KB，内嵌 Minecraftia 字体与 base64 图标，提示 "Close this window and return to Minecraft!"）→
  `oauth20_token.srf` 换 refresh/access token → `user.auth.xboxlive.com` → `xsts.auth.xboxlive.com` →
  `api.minecraftservices.com/authentication/login_with_xbox` → `/minecraft/profile`（`GuiSessionLogin` 里还有一处
  `HttpURLConnection` 直连 profile 校验）
- **令牌存储：明文 JSON**，路径 `<mcDataDir>/ataloss.accounts.json`（旧名 `openmyau.accounts.json` 在迁移期仍会被读取，见 §1.1），字段 `refreshToken / accessToken` 等。
  **任何能读到 .minecraft 目录的进程都能直接接管这些账号** —— 这是本仓库最实际的资产风险点。
- **`SSLUtils` 的真实作用（不是"绕过证书校验"）**：从 classpath 加载 `ssl.jks`（176 KB，
  `keytool -list` 显示 **152 条 CA trustedCertEntry**，口令 `changeit`，就是一份被冻结的 JDK 8 `cacerts` 副本），
  用它构造 `TrustManagerFactory` 并 `HttpsURLConnection.setDefaultSSLSocketFactory()`。
  也就是**把信任根换成随包分发的固定 CA 列表**（为了在老 JRE/启动器环境里避免 PKIX 报错）。
  ⇒ 风险是「CA 列表过期/被随包替换」而不是「明文中间人」，但**换掉全局默认 TrustManager 依然是敏感操作**：
  同一 JVM 内其它 HTTPS 连接都会受它影响。
- `GuiAccountManager / GuiAddToken / GuiMicrosoftAuth / GuiSessionLogin` 是独立的 GUI，
  `Events.java` 在特定 GUI 打开事件时接管（`mc.displayGuiScreen(new GuiAccountManager(...))`）。
- 全项目**没有遥测、没有更新检查、没有二次 HTTP 上报**（除上述登录与版本读本地资源）。

---

## 10. 指令系统（`myau.command`）

- 分发：`CommandManager.onPacket(@EventTarget(Priority.HIGHEST))` 拦 `PacketEvent(SEND)` 且包为
  `C01PacketChatMessage`；`isTypingCommand()` 要求首字符 `.` 且第二个字符是字母/数字；
  命中即 `event.setCancelled(true)`（**消息不会发到服务器**）→ `handleCommand()`
- **没有 tab 补全**，参数按 `\\s+` 切分；前缀固定 `.`
- 15 条指令：

| 指令（别名） | 作用 |
|---|---|
| `.bind` | 查看/设置模块绑键，可批量 bind all / unbind |
| `.config` (`cfg`,`c`) | `load / save <name> / list / folder`，列表项带 `ClickEvent` 可点击加载 |
| `.denick` | 反混淆/皮肤名查询 |
| `.friend` (`f`) | 好友增删清列（支持一次多个名字） |
| `.help` | 指令列表 |
| `.hide` / `.show` | 在 HUD 中隐藏/显示模块（写 `hidden` 字段） |
| `.ign` | 复制自己用户名到剪贴板 |
| `.item` | 打印手持物品的 registry name / NBT |
| `.list` | 模块列表 |
| `.module` (`m`) | 读写模块属性（`parseString` 路径，非法值给 `getValuePrompt()` 提示） |
| `.player` | 列出附近玩家（可点击） |
| `.target` | 敌人列表增删清列 |
| `.toggle` | 开关模块 |
| `.vclip` | 垂直坐标瞬移（客户端侧，`setPosition` + 距离格式化） |

---

## 11. 模块目录（89 个 = 69 个作弊模块 + 20 个 Legit 模块，全部注册）

**统计**：默认开启 4 个（`HUD`、`Fullbright`、`NoHitDelay`、`Sprint`）；默认 `hidden` 13 个
（`AntiObfuscate`、`BedTracker`、`Fullbright`、`HUD`、`Indicators`、`LightningTracker`、`MCF`、`NickHider`、
`NoHitDelay`、`NoHurtCam`、`Sprint`、`TargetHUD`、`Trajectories`）。
**架构模式**：多数模块是「事件钩子 + Property」；有一批极简模块（`AntiDebuff`、`GhostHand`、`AntiObbyTrap`、
`MCF`、`NoHitDelay`、`KeepSprint`、`NoJumpDelay`、`Sprint`）**自身几乎无逻辑**，只提供开关与判定谓词，
真正工作在对应的 Mixin 里（flag + predicate 模式）。

### 11.1 Combat（15）

| 模块 | 行数 | 作用 | 关键机制 / 主要配置 |
|---|---:|---|---|
| `KillAura` | 1026 | 全自动攻击：目标选择 + APS + AutoBlock + 旋转 | `mode` SINGLE/SWITCH、`sort`(DISTANCE/HEALTH/HURT_TIME/FOV)、`min/max-aps`(14)、`attack/swing-range`、`fov`、`rotations`(NONE/LEGIT/SILENT/LOCK_VIEW，默认 SILENT)、`move-fix`、**`auto-block` 9 种**（NONE/VANILLA/**SPOOF 默认**/HYPIXEL/BLINK/INTERACT/SWAP/LEGIT/FAKE）、`smoothing`、`through-walls`、目标类型 8 个开关 + `teams`/`bot-check`；`onUpdate(Priority.LOW)` 是 300+ 行的巨型分支 |
| `AimAssist` | 127 | 视线内敌人视角平滑吸附 | `horizontal/vertical-speed`(3/0)、`smoothing`(50)、`range`(4.5)、`fov`(90)、`weapons-only`；`TickEvent(POST)` + `KeyEvent`；走 **`RotationManager.setRotation(...,0,false)`**（旧旋转链路、最低优先级，KillAura 会覆盖它） |
| `AutoClicker` | 176 | 按住左键按 CPS 自动补点 + BlockHit（右键） | `min/max-cps`(8/12)、`block-hit`(+`block-hit-ticks`)、`weapons-only`、`break-blocks`(+`range`/`hit-box-vertical`/`hit-box-horizontal`，防误挖)；`TickEvent(PRE)` 里 `while` 补点，`LeftClickMouseEvent(LOWEST)` 把玩家自己的点击计入节奏 |
| `Wtap` | 79 | 攻击后短暂松开前进，打断自身疾跑（重置击退） | `delay`(5.5 tick)、`duration`(1.5)；`MoveInputEvent(LOWEST)` 状态机；**自身不发包**，靠疾跑条件破裂让原版自发 STOP_SPRINTING |
| `Velocity` | 242 | 反击退（5 种模式） | `mode` VANILLA/JUMP/**DELAY**/**REVERSE**/LEGIT_TEST、`horizontal`(0)/`vertical`(100)、爆炸单独两组百分比、`chance`、`fake-check`、`delay-ticks`/`delay-chance`；`KnockbackEvent` 改 `motionX/Y/Z`；收包侧拦 `S12`(自身)/`S27`(爆炸)/`S19 op=2`(真伤害标记)，DELAY 走 `DelayManager(VELOCITY)` 队列 |
| `Freeze` | 198 | VeloBuffer：把击退"冻结"缓存后重放 | `render-timer`、`max-timeout`(300 tick，超时聊天提示 `Freeze timed out`)；后加（`9b8e581`） |
| `Reach` | 53 | 扩大拾取/射线距离 | `range`(3.1)、`chance`(100)；`PickEvent`(ModifyConstant 3.0) + `RaytraceEvent`(改 `d0`)，每 tick 重掷概率 |
| `TargetStrafe` | 219 | 绕 KillAura 目标做圆周走位 | `radius`、`points`(圆采样点)、`require-press`、`speed-only`；`UpdateEvent(HIGHEST)` 里 **`setPervRotation(yaw, 10)`**（压过 KillAura 的 1） |
| `NoHitDelay` | 9 | 清除 `leftClickCounter`（无攻击冷却） | 默认**开启且隐藏**；无事件，消费点在 `MixinMinecraft.clickMouse` |
| `AntiFireball` | 151 | 自动转身攻击飞来的火球 | `range`(5.0，远大于原版 3.0)、`fov`(360)、`rotations`、`swing`、`move-fix`；`UpdateEvent(LOWEST)` 里 `setRotation(r,0)` 打火球 |
| `LagRange` | 216 | 超出原版距离攻击时人为制造延迟 | `delay`(150ms)、`range`(10)、`weapons-only`、`bot-check`、`teams`；配合 `LagManager` 挡出包 |
| `HitBox` | 235 | 放大命中箱 + 覆盖 `mc.objectMouseOver` | `multiplier`(1.2)、`show-hitbox`、`color`、`teams`/`bot-check`；`LeftClickMouseEvent(HIGH)` 直接改写 `objectMouseOver`；后加（`301968c`） |
| `MoreKB` | 110 | 放大击退（在 hurtTime==10 时补发疾跑包） | `mode` LEGIT/LEGIT_FAST/LESS_PACKET/PACKET/DOUBLE_PACKET、`intelligent`、`only-ground`；后加（`e8a82cc`） |
| `Refill` | 68 | 自动补汤/药水 | `mode` SOUP/POT、`delay`；后加（`e7501ea`） |
| `HitSelect` | 284 | 有选择地取消 ATTACK 包（抢二刀/暴击/W-tap） | `mode` SECOND/CRITICALS/W_TAP；`PacketEvent SEND(HIGHEST)` 取消 C02；**会运行时 `KeepSprint.toggle()`** 并在结束时关掉它；后加（`c640019`/`9b8e581`） |

### 11.2 Movement（14）

| 模块 | 行数 | 作用 | 关键机制 / 主要配置 |
|---|---:|---|---|
| `Fly` | 56 | 飞行 | `horizontal-speed`/`vertical-speed`；`StrafeEvent` 设 friction + 按住跳跃/潜行改 `motionY` |
| `Speed` | 66 | 跳跃加速 | `multiplier`、`friction`、`strafe`；`StrafeEvent(LOW)`：地面 `motionY=0.42` + `setSpeed(jumpMotion*multiplier)`；空中改 friction/做 strafe 分解；`Scaffold` 开启时禁用 |
| `LongJump` | 259 | 火球跳 | `mode` FIREBALL/FIREBALL_MANUAL/FIREBALL_HIGH/FIREBALL_FLAT、`motion`、`speed-motion`、`strafe`；`setRotation(yaw,pitch,4)`；无火球时聊天报错 |
| `SafeWalk` | 69 | 边缘防掉 | `motion`/`speed-motion`、`air`、`direction-check`、`pitch-check`、`require-press`、`blocks-only`；消费点 `SafeWalkEvent`（`MixinEntity.moveEntity` 的 ModifyVariable） |
| `Jesus` | 25 | 水面/岩浆上行走 | `speed`(2.5)、`no-push`(`MixinWorld.handleMaterialAcceleration` redirect)、`ground-only` |
| `Blink` | 57 | 吞包瞬移 | `mode` DEFAULT(达 ticks 自动关)/PULSE(周期性 flush)、`ticks`(20，上限 1200)；`BlinkManager.setBlinkState(BLINK)` |
| `NoFall` | 146 | 4 种免摔 | `mode` PACKET(把 `timer.timerSpeed` 降 0.5 做"慢下落"再补 C03 onGround)/BLINK(用 BlinkManager 缓存)/NO_GROUND(强制 `onGround=false`)/SPOOF(包内改 onGround+清零 fallDistance)、`distance`、`delay`；收到 `S08PlayerPosLook` 自动关闭；`ServerUtil.hasPlayerCountInfo()` 用于重置计时 |
| `NoSlow` | 131 | 使用物品不减速（剑/食物/弓三组独立） | `sword-mode`/`food-mode`/`bow-mode`(NONE/VANILLA/FLOAT)、各自的 `*-motion` 百分比与 `*-sprint`；剑的路径由 `MixinEntityPlayerSP` redirect `isUsingItem()` 实现 |
| `KeepSprint` | 25 | 攻击后不减速 | `slowdown`、`ground-only`、`reach-only`；**无事件**，逻辑在 `MixinEntityPlayer`（ModifyConstant 0.6 + redirect setSprinting）与 `PlayerUtil.attackEntity()` 里各有一份 |
| `Eagle` | 112 | 搭桥到边缘自动潜行 | `min/max-delay`、`direction-check`、`jump-check`、`pitch-check`、`blocks-only`、`sneaking-only`；Telly 会临时禁用它 |
| `NoJumpDelay` | 32 | 清除跳跃冷却 | `delay`(3)；`TickEvent(HIGHEST)` 改 `jumpTicks` |
| `AntiVoid` | 123 | 掉入虚空时用 Blink 回拉 | `mode` BLINK、`distance`(5.0) |
| `AntiAFK` | 45 | 防挂机踢出 | 后加（`17f35eb`） |

### 11.3 Render（17）

| 模块 | 行数 | 作用 | 关键机制 / 主要配置 |
|---|---:|---|---|
| `ESP` | 230 | 实体透视框 | `mode` NONE/**2D**/3D/OUTLINE/FAKECORNER/FAKE2D、`color` DEFAULT/TEAMS/HUD、`health-bar` NONE/2D/RAVEN、目标类型开关；用 `GlowShader`/`OutlineShader`（`util/shader`，仅此模块使用） |
| `Chams` | 87 | 穿墙显示实体本体 | 目标类型 11 个开关；靠 `MixinVisGraph` 取消遮挡剔除 |
| `ChestESP` | 169 | 箱子/陷阱箱/末影箱透视 | 三个独立 `ColorProperty` + `tracers`；大箱子合并（`b5e9295`） |
| `ItemESP` | 212 | 掉落物透视 | `opacity`、`outline`、`item-count`、`auto-scale` + 钻石/金/铁/绿宝石筛选 |
| `BedESP` | 151 | 床透视 + 黑曜石保护层 | `mode` DEFAULT(0.5625 高)/FULL、`color` CUSTOM/HUD、`custom-color`、`opacity`(**零引用死配置**)、`outline`、`obsidian`；数据不来自扫世界，而是 `MixinBlockRendererDispatcher` 渲染时登记 |
| `Radar` | 243 | 2D 雷达 | `position`(5 档)、`radar-radius`(55)、`dot-radius`、玩家/朋友/敌人/机器人/**PVP 点** 开关、3 个颜色 |
| `Tracers` | 200 | 指到玩家的线/箭头 | `color`、`lines`/`arrows`、`opacity`、`distance`(512)、目标类型开关；后加可配置距离（`95b8284`） |
| `NameTags` | 257 | 自定义名牌 | `scale`/`auto-scale`、`background`、`distance`(NONE/DEFAULT/**VAPE**)、`health`(NONE/HP/HEARTS/TAB)、`armor`、`effects` + 目标类型 |
| `TargetHUD` | 232 | 目标信息面板 | `position-x/y`、`scale`、`offset-x/y`(可负)、`background`、`head`、`indicator`、`outline`、`animations`、`ka-only`、`chat-preview` |
| `Indicators` | 129 | 投射物来袭指示 | `scale`、`offset`、`direction-check` + 火球/珍珠/箭/蛋/雪球 开关（蛋与雪球是后加 `5fc8f58`） |
| `Xray` | 361 | 矿物透视 | `mode` SOFT(邻面剔除)/FULL、`opacity`、`range`(64)、`caves-only`+`caves-radius`、11 种方块 × (显示 + tracers)；靠 6 个 `MixinBlock*` 的 `getBlockLayer` 重定向 + `MixinBlockModelRenderer` 跳过 AO + `MixinVisGraph` |
| `Trajectories` | 208 | 投掷物轨迹预测 | `opacity`、`bow`、`projectiles`、`pearls` |
| `Fullbright` | 67 | 全亮度 | `mode` GAMMA/EFFECT；默认**开启且隐藏** |
| `NoHurtCam` | 12 | 减弱受伤镜头抖动 | `multiplier`(0)；`MixinEntityRenderer` ModifyConstant 14.0F |
| `ViewClip` | 26 | 第三人称穿墙 + 去雾 | `MixinEntityRenderer` redirect `Vec3.distanceTo`(orientCamera) 与 `Block.getMaterial`(setupFog) |
| `HUD` | 293 | Arraylist 模块列表 + 主题 + 计时 | `color` RAINBOW/CHROMA/ASTOLFO/CUSTOM1/2/3、`color-speed/saturation/brightness`、`position-x/y`、`scale`、`background`、`bar`、`shadow`、`suffixes`、`lower-case`、`chat-outline`(给 `.` 指令输入框描边)、`blink-timer`(显示 blink 包数)、`toggle-sounds`、`toggle-alerts` |
| `GuiModule` | 94 | ClickGui 开关 + GUI 样式 + 菜单页（模块名显示为 `ClickGui`） | `setKey(KEY_RSHIFT)`、`style`(LIGHTWEIGHT/**LIQUID**)、`menu`(**CHEAT**/LEGIT)；`onEnabled()` 立即 `setEnabled(false)` 后按样式打开 `myau.ui.ClickGui` 或 `LiquidClickGui`（"动作假模块"，`ModuleManager` 专门屏蔽它的聊天提示）；静态 `useLiquidGui()` / `isLegitMenu()` / `setMenu()` 是两套 GUI 共用的状态入口 |

### 11.4 Player（13）

| 模块 | 行数 | 作用 | 关键机制 / 主要配置 |
|---|---:|---|---|
| `AutoHeal` | 149 | 低血自动使用治疗品 | `health`(35%)、`delay`(4000ms)、`regen-check`、`hurt-check`+`hurt-time`；识别金头/玩家头/汤（Hypixel UHC 物品名硬编码）；**静默改 `inventory.currentItem`（不发 C09）** |
| `AutoTool` | 77 | 挖方块自动换最佳工具 | `delay`、`switch-back`、`sneak-only`；考虑效率附魔（`1fb2269`） |
| `ChestStealer` | 267 | 自动拿箱子里的东西 | `min/max-delay`、`open-delay`、`auto-close`、`name-check`、`skip-trash`、`more-armor`、`more-sword` |
| `InvManager` | 272 | 背包整理 | `auto-armor`(+`interval`)、`drop-trash`、`check-durability`、7 个固定槽位（剑/镐/铲/斧/方块/投射物/弓）+ 方块/投射物/箭数量阈值、`gold-apple-slot` |
| `InvWalk` | 302 | 容器/GUI 内可移动 | `mode` VANILLA/LEGIT/HYPIXEL/**LEGIT+**(带开/关延迟)、`click-gui`、`lock-move-dey`；**`click-gui` 已失效**（见 §7.1） |
| `Scaffold` | 767 | 自动搭桥 + tower + keep-y | 15 项配置；`rotations` NONE/DEFAULT/BACKWARDS/SIDEWAYS、`move-fix`、`sprint`、`ground/air/speed-motion`、`tower`/`keep-y`(NONE/VANILLA/EXTRA/TELLY)、`multi-place`、`safe-walk`、`item-spoof`(**零引用**)、`block-counter`；`getBlockData()` 每 tick 扫 405 格 → 最多 256 次 raycast（已从 `E:\Work\OpenMyau` 原版恢复，撤销了本地对旋转/移动的全部改动） |
| `Telly` | 3344 | 全自动 Telly Bridge（跳桥+空中转身放块） | 21 相位曲线状态机、车道约束、抗侧漂 PD、静默俯仰、C08 白名单；见 `module-deep-dive.md` |
| `AutoBlockIn` | 675 | 被围时自动封盒（头顶+四周 8 格） | `range`/`speed`/`place-delay`/`rotation-tolerance`/`item-spoof`(**零引用**)/`show-progress`/`move-fix`；三重扫描 + 36 采样点 + **BFS 兜底**（节点上限 8964）；`setRotation(...,6)` |
| `SpeedMine` | 45 | 加快挖掘 | `speed`(15%)、`delay`；直接抬 `curBlockDamageMP` 与 `blockHitDelay` |
| `FastPlace` | 280 | 放置无冷却（NORMAL）/ 同 Y 层限定快速放置（KEEPY） | `mode` NORMAL/**KEEPY**、`delay`、`blocks-only`、`place-fix`、`skip-obsidian`、`skip-interactable`、`keep-y-redirect`（仅 KEEPY 可见）；KEEPY 按 **vanilla 真实落点**（`ItemBlock.canPlaceBlockOnSide` 口径：瞄到的方块可替换就原地放，否则沿 `sideHit` 偏移一格）判定 Y 层：**锁定层 = 本次长按第一次放置的真实落点 Y**（平地上瞄地面顶面 → 就锁在地面上一层）；落点就在锁定层 → 交给 vanilla；落点跑到别层（典型是瞄同层方块顶面会往上放一格）→ 拦下 vanilla，若 `keep-y-redirect` 打开则改为贴同层支撑方块**正前方（视线水平方向）**的侧面自己放一份，落点仍在锁定层，因此出块速度与 NORMAL 相同而不是"丢点击"；**只试正前方一个面，不做左右回退**（否则会把桥撑成 3 格宽），正前方已占用 / 超出 reach / 不可放 → 放弃这一下；松开右键重新取样，HUD 后缀显示 `KeepY <层>`（本地新增，非上游） |
| `GhostHand` | 24 | 准星穿透队友实体 | `team-only`、`ignore-weapons`；无事件，`MixinEntityRenderer.getMouseOver` 里 `list.removeIf` |
| `MCF` | 34 | 鼠标中键加/删好友 | 无配置；`KeyEvent` 里判断键码 `-98`（中键） |
| `AntiDebuff` | 13 | 屏蔽失明/反胃视觉 | `blindness`、`nausea`；消费点在 4 处 `isPotionActive` 重定向 |

### 11.5 Misc（10）

| 模块 | 行数 | 作用 | 关键机制 / 主要配置 |
|---|---:|---|---|
| `BedNuker` | 638 | 自动拆床（含保护层） | `mode` LEGIT/SWAP、`range`、`speed`、`ground-spoof`、`ignore-velocity`(NONE/CANCEL/DELAY)、`surroundings`、`tool-check`、`whitelist`、`swing`、`move-fix`、`show-target/progress`；3 阶段状态机 + 复刻 1.8 挖掘公式；`DelayManager(BED_NUKER)` |
| `BedTracker` | 341 | 敌方逼近己方床的预警 + `/lobby` 宏 + HUD | 17 项配置；靠 S02 聊天文本 + S08 定位床，3 秒后异步扫 51³；`alerts-sound` MEOW/ANVIL |
| `LightningTracker` | 67 | 雷电预警 | 监听雷声/闪电实体并提示 |
| `NoRotate` | 95 | 阻止服务端强制复位视角 | 拦截 `S08PacketPlayerPosLook`（可选静默/发包伪装） |
| `NickHider` | 33 | 隐藏自己名字与等级 | `name`(默认 "You")、`scoreboard`、`level`；`MixinFontRenderer` 替换 + `MixinGuiIngameForge` 改经验显示 |
| `AntiObbyTrap` | 30 | 反黑曜石陷阱 | `set-air`；`MixinWorld.rayTraceBlocks` 里把包住头部的方块当空气/客户端清除 |
| `AntiObfuscate` | 16 | 去掉 `§k` 混淆字符 | 无配置，默认隐藏；`MixinFontRenderer` |
| `AutoAnduril` | 112 | 周期切 Anduril 剑拿速度 | `interval`、`hold`、`speed-check`、`debug`(0–9)；按 NBT lore `§9Justice` 找剑；后加（`a3f5862`） |
| `InventoryClicker` | 46 | 容器里按住左键连点 | `ticks`；调 `IAccessorGuiScreen.callMouseClicked`；后加（`ca7cb05`） |
| `Spammer` | 44 | 定时刷屏 | `text`("meow")、`delay`(秒)、`random`；走真实聊天包 |

### 11.6 跨模块审计结论（分级）

**确认的功能性缺陷（含我方独立复核）**

1. **`InvWalk.click-gui` 与 `AutoAnduril.canSwap()` 在 `LIQUID` 样式下失效** —— 它们判断的是 `instanceof myau.ui.ClickGui`（轻量 GUI 类）；只有在 `LIGHTWEIGHT` 样式下才为真（见 §7.1）。
2. **`AttackEvent` 的取消语义是死的**：`MixinPlayerControllerMP.attackEntity` 与 `KillAura.performAttack` 都只 `EventManager.call(event)`，**没有任何人读 `isCancelled()`**（已复核）→ 模块无法用它拦截攻击。
3. **`AutoHeal` 的 `hurtTick` 在同一 tick 被 PRE 与 POST 各递减一次** → 有效 `hurt-time` 窗口减半。
4. **`HitSelect.resetMotion()` 会关掉用户原本开着的 `KeepSprint`**（`toggle()` 还会放提示音、写配置）。
5. **`MoreKB.getSuffix()` 返回 `"0".."4"`** 而不是模式名（HUD 上显示数字）。
6. **`MoreKB` 的 PACKET 系列模式发包顺序**在 `setSprinting(false)` 之后、`setSprinting(true)` 之前，而后者自身也会发包 → 重复/顺序错乱的 START_SPRINTING。
7. **`KeepSprint.shouldKeepSprint()`** 在 `reach-only` 时直接解引用 `mc.objectMouseOver.hitVec`，无 null 检查 → 潜在 NPE。
8. **`KillAura.performAttack` 先 `swingItem()` 再做射线校验** → 校验失败也会发出空挥手（且 `getAttackDelay` 已被扣减）。
9. **`KillAura` 的实际 APS ≠ 属性值**：`attackDelayMS` 每 tick 减 50 → 有效 APS = `20 / ceil(delay/50)`。
10. **`Velocity` 命名与语义互换**（`reverseFlag` 服务于 DELAY 模式、`delayActive` 服务于 REVERSE 模式），且 `onDisabled` 未清 `chanceCounter/delayChanceCounter`。
11. **`Velocity.mode` 里混进了实验项 `LEGIT_TEST`**；`AutoAnduril.debug`(0–9) 同为调试残留。
12. **`Script/Telly`：7 个方法（含 `onKey`）因移植改名没有调用点** → "手动按键即脱手"检测实际失效，只能靠相机接管检测（需累计 25°）；`rotationGcd()` 在鼠标灵敏度为 0 时除零。
13. **`BedNuker.onRender3D` 颜色通道写错**（`g=getBlue(); b=getGreen()`）；`getHitFacing` 硬编码 reach 8.0 无视 `range`。
14. **异步扫世界**：`BedNuker`/`BedTracker` 用 `ScheduledExecutorService` 在主线程外跑 51³=132,651 次方块查询并读 `mc.theWorld/thePlayer`，且两个 executor **从不 shutdown**。
15. **多处静默换槽不发 `C09PacketHeldItemChange`**（Telly / Scaffold / AutoBlockIn / AutoHeal / AutoAnduril）→ 服务端槽位视角不同步，是最现实的检测点。
16. **`VclipCommand` 的 `try/finally`**：`NumberFormatException` 被吞掉但 `finally` 仍执行 → 非法输入照样传送 0 格并提示 "Clipped (0 blocks)"（已复核）。
17. **`AntiObbyTrap` 用 `world.setBlockToAir`** 只改本地世界，与服务端不同步（视觉假象）。

**死代码 / 零引用配置**

- `BlurPassShader`、`SaturationShader`、`drawRoundedTextureQuad`、`drawRoundedGradient`、`drawGlassPanel`、`GlassRenderer.drawCapsule` 5 参重载、`GL11Helper.drawCircle`、`GlassControls.Slider.isTyping/getTyped`（注意：`myau.ui.ClickGui` 及其 `components/`/`dataset/`/`callback/` 已作为 `LIGHTWEIGHT` GUI 重新启用，**不再是死代码**）。
- `LiquidClickGui.blurB`（FBO 分配后从未写入，全分辨率含 depth）。
- 零引用 Property：`Scaffold.item-spoof`、`AutoBlockIn.item-spoof`、`BedESP.opacity`。
- 死字段/死分支：`MoreKB.shouldSprintReset`、`HitSelect.blockedHits/allowedHits`、`AntiFireball.nearList`、`HitBox.getExpansion(Entity)`、`AutoBlockIn.placedThis`（恒 false → `if (placedThis) break;` 永不执行）、`GlassModuleEntry` 之外的 `Typed/EventTyped` 体系、`EventStoppable`（全项目零 `stop()` 调用）。
- `PostStartGame` 之外：`init/Initializer` 只打印 "Meow!"；`FMLLoadingPlugin.mixinPlugins` 列表无人读。
- `GlowShader.onUse()` 把 `color` 当 sampler 用（`glUniform4f(texLoc, 1,1,1,1)`）→ 每次都产生 GL 错误，所幸随后的 `W(Color)` 覆盖了真正的 color uniform。

**一处需要纠正的误判（我方复核）**

- `BedESP` 的 `custom-color` 默认值写成 `(int) 8085714755840333141L`，看着像乱码，但按 Java 的 long→int 截断后是 **`0x00FF5555`（红色）**：
  `8085714755840333141 = 0x70363ABB_00FF5555`，低 32 位落在 `[0, 16777215]` 内，验证器通过，
  `formatValue()` 的 `substring(0,6)` 也不会越界（`%06X` 至少 6 位）。
  结论：**写法是手误/混淆残留，但不产生异常**，只是默认颜色恰好是红色。

**移动 / 渲染 / 玩家辅助类的补充发现（第二轮审计）**

- **5 个模块零事件钩子**：`Jesus`、`ViewClip`、`NoHurtCam`、`NickHider`、`AntiObfuscate`（外加 `GuiModule`）—— 逻辑全在 Mixin 里，只读模块文件会误判"未实现"；`Freeze`/`Blink`/`LagRange`/`AntiVoid` 的真实拦包点在 `MixinNetworkManager` + 对应 Manager。
- **实体筛选判定重复 3 份**（`ESP`/`Chams`/`NameTags` 逐行同构），`Tracers`/`Radar` 是裁剪版 → 加一类实体要改 5 个地方。
- **每帧全量扫描/排序**：`TeamUtil.getLoadedEntitiesSorted()` 被 ESP(×2)/ItemESP/NameTags/Tracers(×2)/Radar/Indicators 每帧调用；`ChestESP` 每帧遍历全部 tile entity 且无距离/视锥剔除；`LagRange` 每 tick 全量实体；`Xray` 每帧遍历两个 `CopyOnWriteArraySet`。
- **服务器特化硬编码**：`NoFall`（记分板出现 `Players` 即封锁触发）、`NoRotate`/`NameTags`（BedWars 文案与 `displaySlot2`）、`TeamUtil.isShop`（商店盔甲架文案）、`ServerUtil.isHypixel`（含 emoji 的 `www.hypixel.ne🎂t`）、`ItemUtil.hasRawUnbreakingEnchant`（UHC id 50006/50009）。
- **配置项与行为不一致**：`ItemESP.opacity` 实为开关、`Radar.position` 是位标志当枚举用、`SafeWalk.direction-check` 命名反向（为真＝按 W 就失效）、`Eagle.sneak-only` 默认 true 反直觉、拼写错误 `lock-move-dey` / `enemiess` / `bossees`。
- **资源与副作用**：`ESP` 的 Framebuffer 只在 `ResizeEvent` 重建、**`onDisabled` 不释放**（显存泄漏）；`NameTags` 的 `renderItemInGUI/renderPotionEffect` 内部 `GlStateManager.clear(256)` **在世界渲染期清深度**；`Xray` 开启时全局关 `forgeLightPipelineEnabled` 并全量 `loadRenderers()`；`FullBright` 常驻改 `gammaSetting=1000`（默认开启）。
- 新增确认 bug：`ChestStealer` 的鱼竿分支查成了 `ItemType.Projectile`（复制粘贴）；`Refill.isHotbarFull` 上界写成 `i<=36`（含靴子护甲槽）导致判定错误；`InvManager.arrow` 属性**从未被使用**、`usedHotbarSlots` 在物品不存在时也占槽；`Fly` 的 `posY%1.0!=0` 使**整数高度完全无法升降**；`LongJump.readyToUseFireball` 不复位（每次启用只发一弹）；`Freeze` 用 `ArrayList.remove(0)`（O(n²)）并且 `packet.processPacket` 在主线程重放；`Trajectories` 的预测 `while` 循环**无步数上限**；`Spammer` 用 `Render2DEvent`（渲染帧）驱动网络发包。

**性能热点（按模块）**

- `AutoBlockIn.findBestPlacement`：每 tick 扫 ~900 格（range=6 时 ~2000）并对每格 raycast，最多 216 次 `Math.random()`。
- `Scaffold.getBlockData`：405 格 × `multi-place` 1–3 遍 + 16×16 采样。
- `Telly.findPitchPlacementForTarget`：最坏 5 面 × 81 点 × 27 目标 = 上万次 raycast（**只有它做了 `deadlineMs` 时间预算**，其 L959-962 的中文注释记录了"高 FPS 下按帧预搜索导致窗口未响应"的修复）。
- `GL11` 层：Liquid GUI 每帧上千个立即模式批次 + 每模块一次 `new ScaledResolution` + 多次 `new ArrayList`。

### 11.7 Legit 菜单模块（20 个，移植自 FPSMaster Edge）

`myau/module/legit/`（20 文件 / 905 行）+ `myau/ui/HudEditorScreen.java`（141 行）。标记方式：`Module` 新增
`legit` 字段与 `isLegit()`，两套 GUI 据此把模块分流到 Legit 页（`LiquidClickGui.legitPanels` /
`ClickGui.legitCategoryList`）。所有 Legit 模块默认 `hidden = true`（不重复出现在 HUD 的模块列表里）。

`LegitHudModule` 是文本型 HUD 的基类（`offset-x/offset-y/scale/shadow/background` + `renderLines()` +
`getBounds()` / `setPosition()` 供拖拽编辑器使用）；
**注意**：事件系统只扫「本类声明」的方法，所以每个子类都必须自己写一个 `@EventTarget onRender2D` 调 `renderLines()`。
另外属性收集已改为**沿继承链扫描**（§12），否则基类上的 5 个属性不会出现在 GUI、也存不进配置。

| 模块 | 对应 FPSMaster | 作用 | 关键配置 |
|---|---|---|---|
| `FpsDisplay` | FPSDisplay | FPS 数字 | 位置/缩放/阴影/背景 |
| `CpsDisplay` | CPSDisplay | 左右键 CPS（1 秒滑动窗口） | 同上 |
| `ClockDisplay` | ClockDisplay | 本地时钟 | `24h` |
| `CoordsDisplay` | CoordsDisplay | 坐标，可显示下界/主世界换算 | `nether-coords` |
| `DirectionDisplay` | DirectionDisplay | 朝向 + 轴（0=南/+Z…） | — |
| `PingDisplay` | PingDisplay | 延迟 ms（`NetworkPlayerInfo`） | — |
| `PlayTimeDisplay` | PlayTime | 本次会话时长 hh:mm:ss | — |
| `PotionDisplay` | PotionDisplay | 药水效果列表（名称+等级+剩余） | `duration` |
| `ServerDisplay` | ServerAddressDisplay | 当前服务器 IP / Singleplayer | — |
| `ReachDisplay` | ReachDisplay | 最近一次命中的距离（只读） | `reset-ms` |
| `ComboDisplay` | ComboDisplay | 连击计数，超时清零 | `timeout-ms` |
| `Keystrokes` | Keystrokes | WASD + LMB/RMB 按键高亮（自定义 `getBounds`） | `key-size`/`gap`/`mouse`/两个颜色 |
| `CustomFov` | CustomFOV | 开启时锁定 FOV，关闭还原 | `fov` |
| `TimeChanger` | TimeChanger | 客户端锁定世界时间 | `time` |
| `AutoGG` | AutoGG | 对局结束（10 条例句关键词）自动发一句 | `message`/`cooldown-ms` |
| `AutoText` | AutoText | 绑定键按下即发送预设文本（动作模块），可当指令 | `message`/`as-command` |
| `SoundModifier` | SoundModifier | 主音量调节，关闭还原 | `volume` |
| `ToggleSneak` | ToggleSneak | 按一次 Shift 持续潜行 | — |
| `HudEditor` | （HudEditorScreen） | 打开 HUD 拖拽编辑器（动作模块，可绑键） | — |
| `OldAnimations` | OldAnimations | 1.7 动画：第一人称格挡 11 种风格 + 挥击/吃用/拉弓/鱼竿/第三人称格挡/受伤闪烁 + 蹲下视角 | `block-style`、8 个开关、`x`/`y`/`z`/`scale` |

**HUD 拖拽编辑器（`myau/ui/HudEditorScreen.java`）**

- 入口：Legit Util 里的 `HudEditor`（开启即弹界面并立刻回到 OFF，可在面板里绑键，或用 `.bind HudEditor h`）；
- 只列出**已启用**的 `LegitHudModule`；左键拖动改 `offset-x/offset-y`（所见即所得，HUD 本身在 GUI 之下照常渲染），
  右键把该元素关掉，ESC 退出；
- 关闭时 `new Config("default", false).save()` 落盘，所以拖完即持久化；
- 坐标系说明：`offset-x/offset-y` 是**缩放后**的 GUI 像素（模块内部 `scale` 只影响文字大小，不影响定位），
  因此编辑器里的盒子与真实位置一一对应。
- 依赖事实：`Render2DEvent` 由 `MixinGuiIngameForge.renderGameOverlay` 触发，**GUI 打开时 HUD 仍会渲染**，
  所以编辑器能直接看到元素本体。

**OldAnimations 的 Mixin 层（本地新增）**：`MixinItemRenderer`（`@Inject(HEAD,cancellable)` 接管
`renderItemInFirstPerson`，只在模块开启时生效，不用 `@Overwrite`；`NoShield` 在上游是声明未用的死开关，未移植）、
`MixinLayerHeldItem`（第三人称格挡姿态）、`MixinLayerArmorBase`（`shouldCombineTextures` → 1.7 受伤闪烁）、
`MixinEntityRenderer` 增补（`orientCamera` 内 `getEyeHeight` 重定向 → 蹲下视角插值）。

**没有移植的（说明）**：`optimizes/*`（性能优化，按要求排除）、`PerformanceHud`（依赖性能监控）、`MotionBlur`（FBO 累加）、
`Replay`/`Director`（整套录像系统）、`MiniMap`（自绘 FBO 小地图）、`cosmetic/*`、`ChatAvatars`（需要网络+纹理缓存）、
`LyricsDisplay`（依赖音乐模块）、`FreeLook`/`OldAnimations`/`ItemPhysics`/`CustomFog`/`MoreParticles`/`HideIndicator`/
`BlockIndicator`/`FireModifier`/`BetterFishingRod`/`FixedInventory`（都要动渲染或 Mixin，属于"难以融合"）、
`BetterChat`/`BetterScreen`/`CustomTitles`/`TabOverlay`/`Scoreboard`/`ModsList`（需要自己的 UI/字体/i18n 栈）、
`RawInput`（原生输入）、`Crosshair`（要先 Mixin 取消原版准星，否则叠两层）。
> 另外 `Fullbright` / `NoHurtCam` / `NickHider` / `Sprint` 在 ATALOSS 里本就有（属作弊菜单），未重复成 Legit 模块。


## 12. 与上游的差异（fork 增量清单）

**新增/替换**
- `myau.ui.liquid.*`（6 文件 ≈ 2,000 行）：Liquid Glass GUI，`GuiModule` 的唯一入口
- `myau.module.legit.*`（20 文件 ≈ 905 行）+ `myau/ui/HudEditorScreen`（141 行）：从 **FPSMaster Edge** 移植的 19 个
  Legit 功能与 HUD 拖拽编辑器，见 §11.7
- `myau.module.modules.Telly`（3,344 行）：Raven/Flux 风格的脚本化自动化模块
- `FastPlace` 增加 `KEEPY` 模式（本地新增，非上游）：长按右键期间限定在同一 Y 层放置，见 §11.4
- 品牌重构（两步：`659fea1` 把 `Myau` 改成 `OpenMyau++`，本次再把 `OpenMyau++/OpenMyauPP` 改成 `ATALOSS`）：
  主类 `ATALOSS`、`clientName` 前缀、modid `ataloss`、jarName `ATALOSS`、
  配置目录 `./config/ATALOSS/`、账号文件 `ataloss.accounts.json`、`mcmod.info` 名称、README 与 `images/` 截图
  （作者署名 `TTHILLTT` 与 Java 包名 `myau` 保持不动）
- `Mixin` 与各指令的包名/引用批量改写（`659fea1 Rebuild` 一次提交改了 40+ 文件）

**继承自上游**：69 个作弊模块、事件总线、Mixin 体系、属性系统、配置系统、账号管理器、188 份预设；
外加本次本地新增的 20 个 Legit 模块（`myau/module/legit/`）、HUD 拖拽编辑器（`myau/ui/HudEditorScreen`）
与 3 个新 Mixin（`MixinItemRenderer` / `MixinLayerHeldItem` / `MixinLayerArmorBase`）。

**模块级溯源（git 验证，基线 = `d4e3ea6` "init" 2025-10-02，即 README 所说的 `Myau-250910`）**
- 战斗类 23 个中 **18 个来自基线**（AimAssist、AntiDebuff、AntiFireball、AntiObbyTrap、AutoClicker、AutoHeal、
  GhostHand、KeepSprint、KillAura、MCF、NoHitDelay、NoJumpDelay、Reach、SpeedMine、Sprint、TargetStrafe、Velocity、Wtap），
  **5 个是上游后续加入**：`HitBox`(`301968c`)、`MoreKB`(`e8a82cc`)、`AutoAnduril`(`a3f5862`)、
  `InventoryClicker`(`ca7cb05`)、`HitSelect`(`c640019`)。
- 其它上游后加：`AutoBlockIn`(`164a777`+`464aa80`)、`Radar`(`c51fb6e`)、`Refill`(`e7501ea`)、`AntiAFK`(`17f35eb`)、
  `Freeze`(`9b8e581`)、`ChestESP` 增强(`b5e9295`)。
- **只有 `Telly`(3,344) 与 `ui/liquid/`(≈2,000) 是 2026-08 本分支新增**；`Telly` 还是全项目**唯一含中文注释**的文件
  （12 处，记录了 silent-rotation、按帧 raycast 卡窗口等移植坑），是识别"外来血统"的指纹。

**宣传与事实的偏差**：README 把 `AutoBlockIn`、Radar、Refill、AntiAFK、Freeze/HitSelect 等算作"本项目新增"，
但它们在 git 历史里属于上游或第三方 PR（`6b51e31`、`c51fb6e`、`e7501ea`、`17f35eb`、`9b8e581`）。

---

## 13. 死代码与遗留资产（可安全清理）

| 项 | 规模 | 说明 |
|---|---:|---|
| `myau.ui.ClickGui` + `ui/components/*` + `ui/dataset/*` + `ui/callback/GuiInput` + `ui/Component` | ≈1,500 行 | 无人构造；两个 `instanceof` 判断导致 InvWalk / AutoAnduril 功能回归（见 §7.1） |
| `GlassRenderer.BlurPassShader` / `SaturationShader` / `drawRoundedTextureQuad` / `drawRoundedGradient` | ~120 行 | 零调用点 |
| `LiquidClickGui.blurB` FBO | 每帧全分辨率显存 | 分配后从未写入 |
| `ClickGui` 里的水印（`ATALOSS <version>` / `TTHILLTT Forked`） | 3 行 | 随旧 GUI 一起失效（Liquid GUI 里是 "Dev TTHILLTT"） |
| `myau/data/Box.java`、`enums/FloatModules` 等 | 少量 | 使用面很窄 |
| 大段 `[DEBUG]` 注释（`LiquidClickGui` ≈40 行、`Telly` 等） | — | 调试残留 |

---

## 14. 风险与质量清单

**功能正确性**
1. `InvWalk`「GUI 内走动」与 `AutoAnduril` 的 GUI 判断因 `instanceof` 目标类过期而恒 false（**真 bug，易修**）。
2. Liquid GUI 窗口缩放后 FBO 不重建（`initGui` 只跑一次，`ResizeEvent` 没接）→ 拉伸/裁切。
3. Liquid GUI 命中测试用动画前坐标 → 滚动/入场动画期间点击错位。
4. Silently-rotation 双系统（`UpdateEvent` vs `RotationManager`）优先级耦合，改旋转逻辑容易互相打断。
5. `Module.toggle()` 直接强转 `HUD` 读 `toggleSound`，`KillAura.isAttackAllowed()` 直接强转 `Scaffold`
   —— 单例硬耦合，模块顺序/存在性变化即 NPE。

**性能**
6. 事件派发全反射（每 tick 每模块若干次 `Method.invoke`），26k 行规模下是主要 CPU 开销来源。
7. Liquid GUI：每帧整帧 FBO blit；`GlassRenderer.drawCapsule` 里**每个模块每帧 `new ScaledResolution`**；
   `visibleEntries()` 每次调用 `new ArrayList`（每帧 4~6 次）；大量 `glBegin/glEnd` 立即模式与状态乒乓；
   单帧 GL 批次轻松过千（改 `Tessellator` 批处理可降 1~2 个数量级）。
8. `drawScreen` 每帧对 5 个面板 `setFilter(trim().toLowerCase())`，持续产生字符串垃圾。

**状态/GL 卫生**
9. `renderBlurBackdrop()` 里 `glDisable(GL_DEPTH_TEST)` 后**从未恢复**；`GlStateManager.disableCull()` 多处无配对恢复
   —— 依赖后续帧的状态缓存自愈，属潜在渲染鬼影来源。
10. `Shader` 编译/链接失败**完全静默**（返回 -1 / `programId=-1`，无日志），排查困难。

**安全/隐私**
11. 微软账号 refresh/access token **明文**存于 `<.minecraft>/ataloss.accounts.json`。
12. `SSLUtils` 用随包 `ssl.jks`（冻结的 152 CA cacerts，口令 `changeit`）替换 JVM 全局默认 TrustManager。
13. 项目性质本身：可用于多人在线游戏的作弊，上游 LICENSE 为 GPLv3，README 明确面向 BedWars 竞技场景。

---

## 15. 修改入口速查

| 想做的事 | 改哪里 |
|---|---|
| 加/删模块 | 写 `myau/module/modules/Xxx.java` → `ATALOSS.init()` 里 `modules.put` + `new` → （可选）`LiquidClickGui` 构造里的分类列表 |
| 加事件 | `myau/events/XxxEvent.java` + 在对应 Mixin 里 `EventManager.call(new XxxEvent(...))` |
| 加 Mixin | 直接在 `myau/mixin` 下加类（`FMLLoadingPlugin` 自动发现，无需改 JSON），建议 `priority = 9999` 保持一致 |
| 加指令 | `myau/command/commands/XxxCommand.java`（继承 `Command`，构造传别名）→ `ATALOSS.init()` 里 `commands.add` |
| 改 GUI 主题 | `myau/ui/liquid/GlassRenderer.java` 顶部常量 + GLSL 字符串 |
| 改 HUD/水印 | `myau/module/modules/HUD.java`（Render2DEvent 里绘制） |
| 改旋转/欺骗逻辑 | `UpdateEvent.setRotation` 调用点 + `MixinEntityPlayerSP`（silent）+ `RotationManager`/`RotationState` |
| 改分包拦截顺序 | `MixinNetworkManager`（Blink → Lag 的顺序就在这 20 行里） |
| 配置结构 | `Config.load/save` + 各 `Property.read/write` |

> 验证方式：`gradlew.bat build`（当前工作树在 2026-08-07 已成功产出 `build/libs/ATALOSS-1.2.2.jar`）；
> 运行调试：`gradlew.bat runClient`（DevAuth 会拉起真账号登录）。

---

## 16. 建议的优先修复顺序（按"收益/风险"排序）

| 优先级 | 事项 | 位置 | 成本 |
|---|---|---|---|
| P0 | 账号 token 明文落盘：至少加文件权限收紧/加密，或提示用户 | `AccountManager` / `ataloss.accounts.json` | 中 |
| P0 | `AttackEvent` 无人读 `isCancelled` —— 补上判断，恢复事件语义 | `MixinPlayerControllerMP.attackEntity`、`KillAura.performAttack` | 极小 |
| P1 | 让 `InvWalk`/`AutoAnduril` 的 GUI 判断同时接受 `LiquidClickGui`（轻量模式下 `myau.ui.ClickGui` 已可用，`LIQUID` 模式仍缺） | `InvWalk:155`、`AutoAnduril:45` | 极小 |
| P1 | Liquid GUI：`ScaledResolution` 提到每帧一次、`visibleEntries()` 结果缓存、`GL_DEPTH_TEST` 恢复、监听 `ResizeEvent` 重建 FBO | `GlassRenderer`、`GlassPanel`、`LiquidClickGui` | 小 |
| P1 | 删除闲置 `blurB` FBO 与 `BlurPassShader`/`SaturationShader` 死代码，或把模糊真正接上（二选一，别留半成品注释） | `LiquidClickGui`、`GlassRenderer` | 小 |
| P2 | `AutoHeal` 的 `hurtTick` 双减、`HitSelect` 关掉用户 KeepSprint、`MoreKB` 后缀/发包顺序、`Velocity` 命名互换 | 对应模块 | 小 |
| P2 | 静默换槽统一补发 `C09PacketHeldItemChange` | Telly/Scaffold/AutoBlockIn/AutoHeal/AutoAnduril | 中 |
| P2 | `BedNuker`/`BedTracker` 的异步 51³ 扫描改主线程分帧 + executor `shutdown` | 两个模块 | 中 |
| P3 | 事件派发去反射（注册期生成 MethodHandle/Lambda），或至少缓存 `MethodData` 数组 | `EventManager` | 中 |
| P3 | Liquid GUI 用 `Tessellator` 批处理替换上千次 `glBegin/glEnd` | `GlassRenderer` | 大 |
| P3 | 清理 `ClickGui` 里 70+ 未用 import（旧 UI 包已启用，不再算死代码） | `myau.ui.ClickGui` | 小 |

> 附：更细的逐模块深挖见 `docs/module-deep-dive.md`（Telly / AutoBlockIn / Scaffold / BedNuker / BedTracker / BedESP 六个大模块的逐节分析）。
