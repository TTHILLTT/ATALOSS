package myau.module.legit;

import myau.module.Module;
import myau.property.properties.ColorProperty;
import myau.property.properties.PercentProperty;

/**
 * 受伤闪烁颜色（对应 FPSMaster 的 HitColor）。
 *
 * <p>原版受伤时把护甲/皮肤染成红色（{@code RendererLivingEntity.setBrightness} 里
 * 写 brightness buffer 的颜色）。这里在 {@code MixinRendererLivingEntity} 里接管该方法，
 * 只替换 hurt/death 状态下写进 buffer 的 RGB 与透明度。
 */
public class HitColor extends Module {
    public static boolean using = false;

    public final ColorProperty color = new ColorProperty("color", 0xFF0000);
    public final PercentProperty opacity = new PercentProperty("opacity", 47);

    public HitColor() {
        super("HitColor", false, true, true);
    }

    @Override
    public void onEnabled() {
        using = true;
    }

    @Override
    public void onDisabled() {
        using = false;
    }

    public static float red() {
        return ((instanceColor() >> 16) & 0xFF) / 255.0F;
    }

    public static float green() {
        return ((instanceColor() >> 8) & 0xFF) / 255.0F;
    }

    public static float blue() {
        return (instanceColor() & 0xFF) / 255.0F;
    }

    public static float alpha() {
        HitColor module = instance();
        if (module == null) {
            return 0.3F;
        }
        return (float) module.opacity.getValue() / 100.0F;
    }

    private static int instanceColor() {
        HitColor module = instance();
        return module == null ? 0xFF0000 : (module.color.getValue() & 0xFFFFFF);
    }

    private static HitColor instance() {
        return myau.ATALOSS.moduleManager == null ? null
                : (HitColor) myau.ATALOSS.moduleManager.modules.get(HitColor.class);
    }
}
