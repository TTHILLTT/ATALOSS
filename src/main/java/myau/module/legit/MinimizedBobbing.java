package myau.module.legit;

import myau.module.Module;

/**
 * 关闭原版走动视角晃动（对应 FPSMaster 的 MinimizedBobbing）。
 *
 * <p>实现只有一行：{@code MixinEntityRenderer} 里把 {@code setupCameraTransform} 读取
 * {@code gameSettings.viewBobbing} 的那次 FIELD 访问重定向为 {@code viewBobbing && !using}，
 * 不动玩家设置，关掉模块立即恢复原样。
 */
public class MinimizedBobbing extends Module {
    public static boolean using = false;

    public MinimizedBobbing() {
        super("MinimizedBobbing", false, true, true);
    }

    @Override
    public void onEnabled() {
        using = true;
    }

    @Override
    public void onDisabled() {
        using = false;
    }
}
