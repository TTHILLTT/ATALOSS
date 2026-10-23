package myau.module.legit;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.potion.Potion;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.WorldServer;

/**
 * 1.7 动画（移植自 FPSMaster Edge 的 OldAnimations）。
 *
 * <p>本类只管状态与设置；真正的绘制在 {@code MixinItemRenderer}（第一人称）、
 * {@code MixinLayerHeldItem}（第三人称格挡姿态）、{@code MixinLayerArmorBase}（受伤闪烁）、
 * 以及 {@code MixinEntityRenderer}（蹲下视角高度）里。
 */
public class OldAnimations extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static OldAnimations instance;

    public static final int MODE_LUNAR = 0;
    public static final int MODE_17 = 1;
    public static final int MODE_SWANG = 2;
    public static final int MODE_SIGMA = 3;
    public static final int MODE_SWANK = 4;
    public static final int MODE_SWONG = 5;
    public static final int MODE_DEBUG = 6;
    public static final int MODE_LUNA = 7;
    public static final int MODE_JIGSAW = 8;
    public static final int MODE_JELLO = 9;
    public static final int MODE_PUSH = 10;

    public final BooleanProperty animationSneak = new BooleanProperty("sneak-animation", true);
    public final BooleanProperty oldBlock = new BooleanProperty("old-block", true);
    public final ModeProperty animationMode = new ModeProperty("block-style", MODE_17,
            new String[]{"LUNAR", "1.7", "SWANG", "SIGMA", "SWANK", "SWONG", "DEBUG", "LUNA", "JIGSAW", "JELLO", "PUSH"});
    public final BooleanProperty oldRod = new BooleanProperty("old-rod", true);
    public final BooleanProperty oldBow = new BooleanProperty("old-bow", true);
    public final BooleanProperty oldSwing = new BooleanProperty("old-swing", true);
    public final BooleanProperty oldUsing = new BooleanProperty("old-using", true);
    public final BooleanProperty blockSwing = new BooleanProperty("block-swing", true);
    public final BooleanProperty oldDamage = new BooleanProperty("old-damage", true);
    public final BooleanProperty oldThirdPerson = new BooleanProperty("old-third-person", true);
    public final FloatProperty offsetX = new FloatProperty("x", 0.0F, -1.0F, 1.0F);
    public final FloatProperty offsetY = new FloatProperty("y", 0.0F, -1.0F, 1.0F);
    public final FloatProperty offsetZ = new FloatProperty("z", 0.0F, -1.0F, 1.0F);
    public final FloatProperty itemScale = new FloatProperty("scale", 1.0F, 0.0F, 3.0F);

    /** Mixin 里用这个快速判断"模块是否开着" */
    public static boolean using = false;

    private static float eyeHeight = 0.0F;
    private static float lastEyeHeight = 0.0F;
    private static final float START_HEIGHT = 1.62F;
    private static final float END_HEIGHT = 1.54F;

    public OldAnimations() {
        super("OldAnimations", false, true, true);
        instance = this;
    }

    public static OldAnimations get() {
        return instance;
    }

    @Override
    public void onEnabled() {
        using = true;
        eyeHeight = START_HEIGHT;
        lastEyeHeight = START_HEIGHT;
    }

    @Override
    public void onDisabled() {
        using = false;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) {
            return;
        }
        lastEyeHeight = eyeHeight;
        if (mc.thePlayer == null) {
            return;
        }
        if (mc.thePlayer.isSneaking()) {
            eyeHeight = END_HEIGHT;
        } else if (!this.animationSneak.getValue()) {
            eyeHeight = START_HEIGHT;
        } else if (eyeHeight < START_HEIGHT) {
            eyeHeight = START_HEIGHT - (START_HEIGHT - eyeHeight) * 0.4F;
        }
        // 1.7 的"边格挡边挥剑"
        if (this.blockSwing.getValue()
                && mc.gameSettings.keyBindAttack.isKeyDown()
                && mc.thePlayer.isUsingItem()
                && mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            this.swingItem();
        }
    }

    /** 蹲下时的相机眼睛高度（供 MixinEntityRenderer 重定向） */
    public static float getClientEyeHeight(float partialTicks) {
        if (instance == null || !instance.animationSneak.getValue()) {
            return eyeHeight;
        }
        return lastEyeHeight + (eyeHeight - lastEyeHeight) * partialTicks;
    }

    private void swingItem() {
        ItemStack stack = mc.thePlayer.getHeldItem();
        if (stack == null || stack.getItem() == null || !stack.getItem().onEntitySwing(mc.thePlayer, stack)) {
            if (!mc.thePlayer.isSwingInProgress
                    || mc.thePlayer.swingProgressInt >= this.armSwingAnimationEnd() / 2
                    || mc.thePlayer.swingProgressInt < 0) {
                mc.thePlayer.swingProgressInt = -1;
                mc.thePlayer.isSwingInProgress = true;
                if (mc.thePlayer.worldObj instanceof WorldServer) {
                    ((WorldServer) mc.thePlayer.worldObj).getEntityTracker()
                            .sendToAllTrackingEntity(mc.thePlayer, new S0BPacketAnimation(mc.thePlayer, 0));
                }
            }
        }
    }

    private int armSwingAnimationEnd() {
        if (mc.thePlayer.isPotionActive(Potion.digSpeed)) {
            return 6 - (1 + mc.thePlayer.getActivePotionEffect(Potion.digSpeed).getAmplifier());
        }
        if (mc.thePlayer.isPotionActive(Potion.digSlowdown)) {
            return 6 + (1 + mc.thePlayer.getActivePotionEffect(Potion.digSlowdown).getAmplifier()) * 2;
        }
        return 6;
    }
}
