package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.LoadWorldEvent;
import myau.events.RightClickMouseEvent;
import myau.events.TickEvent;
import myau.mixin.IAccessorMinecraft;
import myau.module.Module;
import myau.util.BlockUtil;
import myau.util.PlayerUtil;
import myau.util.RotationUtil;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.ModeProperty;
import net.minecraft.block.Block;
import net.minecraft.block.BlockObsidian;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class FastPlace extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat df = new DecimalFormat("0.0#", new DecimalFormatSymbols(Locale.US));
    /** keepY 未锁定时的哨兵值 */
    private static final int KEEP_Y_UNSET = Integer.MIN_VALUE;
    private long delayMS = 0L;
    /** KeepY 模式锁定的 Y 层：本次长按右键期间有效，松开后重新取样 */
    private int keepY = KEEP_Y_UNSET;
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"NORMAL", "KEEPY"});
    public final FloatProperty delay = new FloatProperty("delay", 1.0F, 1.0F, 3.0F);
    public final BooleanProperty blocksOnly = new BooleanProperty("blocks-only", true);
    public final BooleanProperty placeFix = new BooleanProperty("place-fix", true);
    public final BooleanProperty skipObsidian = new BooleanProperty("skip-obsidian", true);
    public final BooleanProperty skipInteractable = new BooleanProperty("skip-interactable", true);
    /** KEEPY：把"瞄偏导致落点跑到别层"的点击改到锁定层去放，而不是丢掉（丢点击会明显比 NORMAL 慢） */
    public final BooleanProperty keepYRedirect = new BooleanProperty("keep-y-redirect", true, () -> this.mode.getValue() == 1);

    private boolean canPlace() {
        ItemStack stack = mc.thePlayer.getHeldItem();
        if (stack != null) {
            Item item = stack.getItem();
            if (item instanceof ItemFishingRod) {
                return false;
            }
            if (item instanceof ItemBlock) {
                Block block = ((ItemBlock) item).getBlock();
                if (skipObsidian.getValue() && block instanceof BlockObsidian) {
                    return false;
                }
                if (skipInteractable.getValue() && BlockUtil.isInteractable(block)) {
                    return false;
                }
                if (!(Boolean) this.placeFix.getValue()) {
                    return true;
                }
                MovingObjectPosition mop = RotationUtil.rayTrace(
                        mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, mc.playerController.getBlockReachDistance(), 1.0F
                );
                return mop != null
                        && mop.typeOfHit == MovingObjectType.BLOCK
                        && ((ItemBlock) item).canPlaceBlockOnSide(mc.theWorld, mop.getBlockPos(), mop.sideHit, mc.thePlayer, stack);
            }
        }
        return !(Boolean) this.blocksOnly.getValue();
    }

    public FastPlace() {
        super("FastPlace", false);
    }

    private boolean isKeepY() {
        return this.mode.getValue() == 1;
    }

    /** 本次长按右键是否已经锁定 Y 层 */
    public boolean hasKeepY() {
        return this.keepY != KEEP_Y_UNSET;
    }

    /** 已锁定的 Y 层，未锁定时返回 Integer.MIN_VALUE */
    public int getKeepY() {
        return this.keepY;
    }

    /** 右键是否处于按下状态（与 vanilla 触发 rightClickMouse 的条件一致） */
    private boolean isUseKeyDown() {
        return mc.gameSettings.keyBindUseItem.isKeyDown();
    }

    /**
     * 这次右键是否真的会"放置方块"：瞄到方块 + 手持方块 + 该面可放。
     * KeepY 只拦截放置，不会影响开箱子 / 吃东西 / 与实体交互等其它右键行为。
     */
    private boolean isBlockPlacement(MovingObjectPosition mop) {
        if (mop == null || mop.typeOfHit != MovingObjectType.BLOCK) {
            return false;
        }
        ItemStack stack = mc.thePlayer.getHeldItem();
        if (stack == null || !(stack.getItem() instanceof ItemBlock)) {
            return false;
        }
        return ((ItemBlock) stack.getItem()).canPlaceBlockOnSide(mc.theWorld, mop.getBlockPos(), mop.sideHit, mc.thePlayer, stack);
    }

    private void resetKeepY() {
        this.keepY = KEEP_Y_UNSET;
    }

    /**
     * 复刻 vanilla / {@link ItemBlock#canPlaceBlockOnSide} 的落点算法：
     * 瞄到的方块可替换就落在原格，否则沿命中面（sideHit）偏移一格。
     * KeepY 必须按"真实落点"判断，只判断瞄准方块的话，瞄顶面时会往上放一格。
     */
    private int getPlacementY(MovingObjectPosition mop) {
        BlockPos pos = mop.getBlockPos();
        if (!mc.theWorld.getBlockState(pos).getBlock().isReplaceable(mc.theWorld, pos)) {
            pos = pos.offset(mop.sideHit);
        }
        return pos.getY();
    }

    /**
     * KeepY 模式：长按右键期间，所有方块都必须落在同一个 Y 层上。
     *
     * <p>锁定层 = 本次长按里**第一次会放置成功的那一下的真实落点 Y**
     * （落点算法同 vanilla：瞄到的方块可替换就原地放，否则沿命中面偏移一格）。
     * 例如平地上瞄地面方块的顶面 → 落点是地面上一层，锁定层就是那一层，
     * 之后继续铺都停在这一层，不会再往上/往下漂；松开右键后重新取样。
     *
     * <p>落点本来就在锁定层的点击交给 vanilla 放；落点跑到别层的点击会被拦下，
     * 若开启 {@code keep-y-redirect}，则改为贴同层支撑方块的侧面自己放一份，
     * 避免"丢点击"导致出块速度比 NORMAL 慢。
     */
    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (!this.isEnabled() || !this.isKeepY() || event.isCancelled()) {
            return;
        }
        if (!this.isUseKeyDown()) {
            this.resetKeepY();
            return;
        }
        MovingObjectPosition mop = mc.objectMouseOver;
        if (!this.isBlockPlacement(mop)) {
            return;
        }
        int placementY = this.getPlacementY(mop);
        if (!this.hasKeepY()) {
            // 起手：用这一次的真实落点锁定本层，本次放行
            this.keepY = placementY;
            return;
        }
        if (placementY == this.keepY) {
            return;
        }
        // 落点不在锁定层（典型：瞄同层方块的顶面 → vanilla 会往上放一格）。
        // 先把 vanilla 那一下拦掉，避免放到别层；开启重定向时改为贴同层支撑方块的侧面自己放，
        // 这样每一 tick 都能出块，速度与 NORMAL 一致。
        if (this.keepYRedirect.getValue()) {
            this.placeAtKeepY(mop);
        }
        event.setCancelled(true);
    }

    /** 玩家视线水平方向对应的朝向（yaw 0 = +Z/South，-90 = +X/East） */
    private EnumFacing getLookFacing() {
        double yaw = Math.toRadians(mc.thePlayer.rotationYaw);
        double dx = -Math.sin(yaw);
        double dz = Math.cos(yaw);
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0.0 ? EnumFacing.EAST : EnumFacing.WEST;
        }
        return dz >= 0.0 ? EnumFacing.SOUTH : EnumFacing.NORTH;
    }

    /**
     * 重定向放置：支撑方块本身就在锁定层，只是当前瞄准面会让落点跑到别层
     * （瞄顶面会高一层、瞄底面会低一层）。这里改成贴该方块的**正前方**水平侧面放，
     * 落点仍留在锁定层。
     *
     * <p>只考虑"视线水平方向"那一个面，**不做左右/朝向自己的回退** —— 否则正面放不下时
     * 会往两侧补块，把桥撑成 3 格宽。正面不行就直接放弃这一下（丢掉点击），保持桥的走向。
     *
     * @return 是否成功发放置
     */
    private boolean placeAtKeepY(MovingObjectPosition mop) {
        BlockPos support = mop.getBlockPos();
        if (support.getY() != this.keepY) {
            return false;   // 支撑方块都不在锁定层，无从改起
        }
        ItemStack stack = mc.thePlayer.getHeldItem();
        if (stack == null || !(stack.getItem() instanceof ItemBlock)) {
            return false;
        }
        ItemBlock itemBlock = (ItemBlock) stack.getItem();
        Block block = itemBlock.getBlock();
        if (skipObsidian.getValue() && block instanceof BlockObsidian) {
            return false;
        }
        if (skipInteractable.getValue() && BlockUtil.isInteractable(block)) {
            return false;
        }
        EnumFacing facing = this.getLookFacing();
        BlockPos target = support.offset(facing);
        if (!BlockUtil.isReplaceable(target)) {
            return false;   // 正前方已经被占了（比如这一格刚放过），不往两侧挪
        }
        if (!PlayerUtil.canReach(target, mc.playerController.getBlockReachDistance())) {
            return false;
        }
        if (!itemBlock.canPlaceBlockOnSide(mc.theWorld, support, facing, mc.thePlayer, stack)) {
            return false;
        }
        Vec3 hitVec = BlockUtil.getHitVec(support, facing, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, stack, support, facing, hitVec)) {
            mc.thePlayer.swingItem();
            // 与 vanilla rightClickMouse 保持一致：放完把冷却置 4，让上面的 delay 记账逻辑照常生效
            ((IAccessorMinecraft) mc).setRightClickDelayTimer(4);
            return true;
        }
        return false;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (!this.isUseKeyDown()) {
                // 松开右键 → 结束本次取样
                this.resetKeepY();
            }
            int rightClickDelayTimer = ((IAccessorMinecraft) mc).getRightClickDelayTimer();
            if (rightClickDelayTimer == 4) {
                this.delayMS = this.delayMS + (long) (50.0F * this.delay.getValue());
            }
            if (this.delayMS > 0L) {
                this.delayMS = this.delayMS - 50;
            }
            if (this.delayMS <= 0L && rightClickDelayTimer > 1 && this.canPlace()) {
                ((IAccessorMinecraft) mc).setRightClickDelayTimer(0);
            }
        }
    }

    @EventTarget
    public void onWorldLoad(LoadWorldEvent event) {
        this.resetKeepY();
    }

    @Override
    public void onDisabled() {
        this.delayMS = 0L;
        this.resetKeepY();
    }

    @Override
    public void verifyValue(String string) {
        // 切换模式 / 改动属性时丢弃旧的锁定层，避免沿用上一次的 Y
        this.resetKeepY();
    }

    @Override
    public String[] getSuffix() {
        if (this.isKeepY()) {
            return new String[]{"KeepY " + (this.hasKeepY() ? this.keepY : "-")};
        }
        return new String[]{df.format(this.delay.getValue())};
    }
}
