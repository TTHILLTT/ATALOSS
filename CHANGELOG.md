# 更新日志

## 1.2.2 — 2026-10-24

### 新增
- **自定义披风**（Legit Util → `CustomCape`）：本地 `config/ATALOSS/capes/*.png` 或 http(s) URL（自动缓存）；
  任意 **2:1** 尺寸按**原生分辨率**渲染（128x64 / 256x128 都是真 HD，不再压到 64x32）
- **自定义皮肤**（Legit Util → `CustomSkin`）：64x64 或等比 HD；`model` 为 `AUTO` 时自动识别
  Steve（4px 手臂）/ Alex（3px 手臂），也可强制 `DEFAULT` / `SLIM`；旧版 64x32 会提示先转换
- **衣柜**（顶栏 `Wardrobe` 按钮，Liquid Glass 方块墙）：列出 `capes/` 与 `skins/` 下所有 png，
  每个方块带常驻预览（皮肤=正面小人拼贴，披风=有图案的外侧面）+ 文件名，点一下即套用，`None` 关闭，
  `ESC` 返回；轻量 GUI 使用同功能的独立界面
- 新增 `TextureLoader` 公共加载器（本地文件 / URL 下载缓存 / 上传纹理 / 双三次缩放）

### 修复
- 披风/皮肤预览的朝向、左右手性、绕序与深度/剔除组合问题（最终改为平面贴图预览，稳定可读）
- 衣柜页面此前是纯色独立界面，现改为 Liquid GUI 内页，复用模糊玻璃背景
- 预览贴图按 `路径+修改时间` 缓存，改文件后自动重新加载
## 1.2.1 — 2026-10-24

修复"jar 直接放进 mods 跑不起来"的一系列问题，并补充按键与自由视角体验。

### 修复
- **jar 放进 mods 后完全不被加载**：客户端没有任何 `@Mod`，仅靠清单的 `TweakClass` 让 FML 顺带注入 Mixin，FML 1.8.9 对这种普通 jar 的扫描不可靠且静默失败。新增 `myau.init.ATALOSSCorePlugin`（`IFMLLoadingPlugin`）并在清单声明 `FMLCorePlugin`
- **进世界即崩（`IncompatibleClassChangeError`）**：同一份 mixin 配置被 core plugin 与 `TweakClass` 注册两次，`EntityLivingBase` 在配置生效前加载，接口型 Mixin 未应用。core plugin 改为只做 `MixinBootstrap.init()`；`checkPotion` 等 3 处对 `activePotionsMap` 的接口强转改为遍历 `getActivePotionEffects()`
- **生产环境大量 `Mixin apply failed ... could not find any targets`**：Mixin 注解处理器每次编译都会重写 `mixins.ataloss.refmap.json` 且只写本次参与编译的类，增量编译把它截成 1 条（完整为 46 条），导致所有 `@Inject/@Redirect/@Accessor` 退化为按 MCP 原名查找。已关闭增量编译（`options.isIncremental = false`），保证 refmap 始终完整

### 新增
- **鼠标 4 / 5（侧键）绑定**：`ModuleManager` 轮询中键/侧键并做边沿检测（左右键刻意不绑）；`KeyBindUtil` 统一 1-based 命名（`MOUSE4`/`MOUSE5`）；`.bind <module> MOUSE4` 可用；Liquid GUI 的 Bind 控件可直接捕获鼠标
- **FreeLook**：新增 `HOLD` / `TOGGLE` 模式；触发键直接复用模块自己的 Bind Key；Y 轴方向与原版一致

## 1.2 — 2026-10-24

视觉功能补齐（全部为只读/纯客户端表现，不改变任何判定）：

### 新增视觉模块（6）
- **Hitboxes**：显示所有实体碰撞箱轮廓，可扩边、可调颜色/透明度/线宽
- **CleanView**：隐藏药水漩涡粒子（只拦截粒子，不影响药水计时与到期）
- **Crosshair**：自定义准星，间隙/长度/粗细/中心点/描边/颜色可调，冲刺时动态张开，开启时隐藏原版准星
- **CustomFog**：线性或指数雾，颜色与起止距离可调，可分别作用于水下/岩浆；失明药水状态一律不接管
- **HitColor**：受伤闪烁颜色与透明度
- **FreeLook**：鼠标只转相机、不改变角色朝向，可设按住触发；进入时临时切第三人称

### 其他
- 新增 `BoxBarRenderer` 工具类：用 quad 拼立方体棱条，规避 `GL_LINES` 与共面深度问题
- 模块总数 93 → 99

## 1.1 — 2026-10-24

首个以 **ATALOSS** 名义发布的版本。相对 1.0.2（原 OpenMyau 分支）的主要变化：

### 品牌与兼容
- 客户端更名为 **ATALOSS**：主类 `ATALOSS`、modid `ataloss`、jar `ATALOSS-1.1.jar`、配置目录 `./config/ATALOSS/`、账号文件 `ataloss.accounts.json`
- 旧路径自动回退：仍会读取 `./config/OpenMyau/` 与 `openmyau.accounts.json`，写入走新路径
- Java 包名 `myau`、作者署名 `TTHILLTT` 保持不变

### 界面
- **双 GUI 体系**：Liquid Glass（全屏 FBO + 折射着色器）与轻量 2D GUI 二选一，`ClickGui.style` 切换，三层互斥保证轻量模式下玻璃管线零绘制
- **Cheat / Legit 菜单切换**：两套 GUI 顶端都有 `Cheat | Legit` 双段按钮，状态存 `ClickGui.menu` 随配置持久化
- **Legit 菜单**（24 个非作弊模块，见下）
- **HUD 拖拽编辑器**：`HudEditor` 模块打开，左键拖动、右键关闭元素、ESC 保存；位置写回当前配置档案

### 新增 Legit 模块（24）
- HUD 信息（12）：Fps、Cps、Clock、Coords、Direction、Ping、PlayTime、Potions、Server、Reach、Combo、Keystrokes
- 工具（12）：CustomFov、TimeChanger、AutoGG、AutoText、SoundModifier、ToggleSneak、OldAnimations、HudEditor、BlockOverlay、DamageIndicator、MoreParticles、MinimizedBobbing

### 视觉
- **OldAnimations（1.7 动画）**：11 种格挡风格（Lunar / 1.7 / Swang / Sigma / Swank / Swong / Debug / Luna / Jigsaw / Jello / Push）、old-swing / old-using / old-bow / old-rod / block-swing / old-third-person / old-damage / 蹲下视角插值、物品 XYZ/scale 微调
- **BlockOverlay**：准星方块填充 + 描边，RGB 颜色与透明度独立配置，可选穿透显示，开启时自动隐藏原版黑色选择框
- **DamageIndicator**：命中伤害数字上浮淡出
- **MoreParticles**：命中暴击/锋利粒子、心/火/血特效、击杀闪电/爆炸
- **MinimizedBobbing**：关闭走动视角晃动

### 核心
- `FastPlace` 新增 **KEEPY** 模式：长按右键期间只在起手方块所在 Y 层放置；`keep-y-redirect` 用视线面重定向替代丢弃点击，速度与 Normal 一致且不会拓宽桥面
- `Scaffold` 恢复为上游实现
- 属性收集改为沿继承链扫描，修复基类属性在 GUI 不可见、配置不持久化的问题

### 修复
- 点击 Legit 按钮后画面仍显示 Cheat 面板（`drawScreen` 未使用 `activePanels()`）
- OldAnimations 格挡时 `NoSuchFieldError: $SwitchMap$net$minecraft$item$EnumAction`（mixin 内枚举 switch 生成合成字段）
- BlockOverlay 完全不可见（世界坐标未转摄像机相对坐标、未 `enableRenderState`）
- BlockOverlay 描边不可见（`ColorProperty` 只存 RGB，ARGB 落盘后 alpha 被截断为 0）
