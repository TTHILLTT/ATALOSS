package myau.ui;

/**
 * 衣柜 3D 预览的调试参数（临时，不写配置）。
 *
 * <p>由衣柜页右侧的 Tuning 面板实时修改，{@link WardrobeRenderer} 每帧读取。
 */
public final class WardrobeTuning {
    /** 皮肤预览 */
    public static float skinYaw = 180.0F;
    public static float skinPitch = 0.0F;
    public static float skinScale = 150.0F;
    public static float skinOffsetX = 0.0F;
    public static float skinOffsetY = -26.0F;

    /** 披风预览 */
    public static float capeYaw = 0.0F;
    public static float capePitch = 8.0F;
    public static float capeScale = 100.0F;
    public static float capeOffsetX = 0.0F;
    public static float capeOffsetY = 0.0F;

    /** 渲染开关 */
    public static boolean cull = false;
    public static boolean depth = true;
    public static boolean lighting = false;
    /** 在 3D 下面再叠一层平铺贴图，方便对比 */
    public static boolean flatUnderlay = false;

    private WardrobeTuning() {
    }

    public static void reset() {
        skinYaw = 180.0F;
        skinPitch = 0.0F;
        skinScale = 150.0F;
        skinOffsetX = 0.0F;
        skinOffsetY = 0.0F;
        capeYaw = 0.0F;
        capePitch = 8.0F;
        capeScale = 100.0F;
        capeOffsetX = 0.0F;
        capeOffsetY = 0.0F;
        cull = false;
        depth = true;
        lighting = false;
        flatUnderlay = false;
    }
}
