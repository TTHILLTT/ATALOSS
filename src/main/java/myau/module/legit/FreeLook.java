package myau.module.legit;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.Render3DEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.IntProperty;
import myau.util.KeyBindUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

/**
 * 自由视角（对应 FPSMaster 的 FreeLook）：按住/开启时鼠标只转相机，不动角色朝向。
 *
 * <p>实现要点：
 * <ol>
 *   <li>{@code MixinEntityRenderer} 在 {@code updateCameraAndRender} HEAD 调 {@link #overrideMouse()}，
 *       先 {@code mouseHelper.mouseXYChange()} 把本帧鼠标位移“吃掉”，后面原版就拿不到位移，角色不会转；</li>
 *   <li>{@code orientCamera} 里 {@code Entity.rotationYaw/prevRotationYaw/rotationPitch/prevRotationPitch}
 *       四个字段读取被重定向到相机角度；</li>
 *   <li>进入自由视角时强制第三人称（否则看不到自己的身位）。</li>
 * </ol>
 */
public class FreeLook extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean using = false;
    public static boolean active = false;
    public static float cameraYaw = 0.0F;
    public static float cameraPitch = 0.0F;
    public static float prevCameraYaw = 0.0F;
    public static float prevCameraPitch = 0.0F;

    /** 0 = 模块开启时一直生效；否则按住该键才生效（默认 0） */
    public final IntProperty holdKey = new IntProperty("hold-key", Keyboard.KEY_NONE, 0, 255);

    private static int previousView = -1;

    public FreeLook() {
        super("FreeLook", false, true, true);
    }

    @Override
    public void onEnabled() {
        using = true;
    }

    @Override
    public void onDisabled() {
        using = false;
        this.release();
    }

    private void release() {
        active = false;
        if (mc.gameSettings != null && previousView >= 0) {
            mc.gameSettings.thirdPersonView = previousView;
        }
        previousView = -1;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE) {
            return;
        }
        prevCameraYaw = cameraYaw;
        prevCameraPitch = cameraPitch;
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (mc.thePlayer == null || mc.currentScreen != null || mc.gameSettings == null) {
            this.release();
            return;
        }
        int key = this.holdKey.getValue();
        boolean want = key == Keyboard.KEY_NONE || KeyBindUtil.isKeyDown(key);
        if (want == active) {
            return;
        }
        active = want;
        if (active) {
            cameraYaw = mc.thePlayer.rotationYaw;
            cameraPitch = mc.thePlayer.rotationPitch;
            prevCameraYaw = cameraYaw;
            prevCameraPitch = cameraPitch;
            previousView = mc.gameSettings.thirdPersonView;
            mc.gameSettings.thirdPersonView = 1;
        } else {
            this.release();
        }
    }

    /** @return 是否允许原版继续处理鼠标（true = 照常转角色） */
    public static boolean overrideMouse() {
        if (!using || !active || !mc.inGameHasFocus || !Display.isActive()) {
            return true;
        }
        // 先消费掉本帧位移，原版后面就读不到，角色朝向保持不变
        mc.mouseHelper.mouseXYChange();
        float sensitivity = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float multiplier = sensitivity * sensitivity * sensitivity * 8.0F;
        cameraYaw += mc.mouseHelper.deltaX * multiplier * 0.15F;
        cameraPitch += mc.mouseHelper.deltaY * multiplier * 0.15F;
        cameraPitch = MathHelper.clamp_float(cameraPitch, -90.0F, 90.0F);
        return false;
    }
}
