package myau.module.legit;

import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.ModeProperty;

/**
 * 自定义雾（对应 FPSMaster 的 CustomFog）。
 *
 * <p>实现在 {@code MixinEntityRenderer}：{@code setupFog} RETURN 处设 GL 雾参数，
 * {@code updateFogColor} RETURN 处同步天空/清屏色（否则会出现"方块被雾吃掉、天空还是原版蓝"）。
 * 失明药水、未开启时的水下/岩浆一律不接管 —— 那些属于视野限制，覆盖等于给优势。
 */
public class CustomFog extends Module {
    public static boolean using = false;

    public final ColorProperty color = new ColorProperty("color", 0x00C8FF);
    public final FloatProperty startDistance = new FloatProperty("start", 32.0F, 0.0F, 200.0F);
    public final FloatProperty endDistance = new FloatProperty("end", 64.0F, 1.0F, 200.0F);
    public final ModeProperty fogMode = new ModeProperty("mode", 0, new String[]{"LINEAR", "EXPONENTIAL"});
    public final BooleanProperty affectWater = new BooleanProperty("affect-water", false);
    public final BooleanProperty affectLava = new BooleanProperty("affect-lava", false);

    public CustomFog() {
        super("CustomFog", false, true, true);
    }

    @Override
    public void onEnabled() {
        using = true;
    }

    @Override
    public void onDisabled() {
        using = false;
    }

    public boolean isLinear() {
        return this.fogMode.getValue() == 0;
    }
}
