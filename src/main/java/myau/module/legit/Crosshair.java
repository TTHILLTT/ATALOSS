package myau.module.legit;

import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.FloatProperty;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

/** 自定义准星（对应 FPSMaster 的 Crosshair），开启时原版准星被 MixinGuiIngame 关掉 */
public class Crosshair extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static boolean using = false;

    public final FloatProperty gap = new FloatProperty("gap", 3.5F, 0.0F, 10.0F);
    public final FloatProperty length = new FloatProperty("length", 3.0F, 1.0F, 10.0F);
    public final FloatProperty thickness = new FloatProperty("thickness", 1.0F, 0.5F, 5.0F);
    public final BooleanProperty dot = new BooleanProperty("dot", true);
    public final BooleanProperty outline = new BooleanProperty("outline", true);
    public final FloatProperty outlineWidth = new FloatProperty("outline-width", 0.8F, 0.0F, 5.0F, this.outline::getValue);
    public final FloatProperty dynamic = new FloatProperty("dynamic", 3.0F, 0.0F, 10.0F);
    public final ColorProperty color = new ColorProperty("color", 0xFFFFFF);
    public final ColorProperty outlineColor = new ColorProperty("outline-color", 0x000000, this.outline::getValue);

    private float currentDynamic = 0.0F;

    public Crosshair() {
        super("Crosshair", false, true, true);
    }

    @Override
    public void onEnabled() {
        using = true;
    }

    @Override
    public void onDisabled() {
        using = false;
        this.currentDynamic = 0.0F;
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled() || mc.gameSettings.showDebugInfo || mc.thePlayer == null) {
            return;
        }
        ScaledResolution sr = new ScaledResolution(mc);
        float centerX = (float) sr.getScaledWidth() / 2.0F;
        float centerY = (float) sr.getScaledHeight() / 2.0F;

        // 冲刺时准星张开一点（按 tick 线性插值，不依赖渲染帧率）
        float target = mc.thePlayer.isSprinting() ? this.dynamic.getValue() : 0.0F;
        this.currentDynamic += (target - this.currentDynamic) * 0.2F;

        float gapValue = this.gap.getValue() + this.currentDynamic;
        float thick = this.thickness.getValue();
        float len = this.length.getValue();
        int fill = 0xFF000000 | (this.color.getValue() & 0xFFFFFF);
        int outlineColor = 0xFF000000 | (this.outlineColor.getValue() & 0xFFFFFF);
        boolean withOutline = this.outline.getValue();
        float ow = withOutline ? this.outlineWidth.getValue() : 0.0F;

        RenderUtil.enableRenderState();
        // 上下左右四条
        this.rect(centerX - thick / 2.0F, centerY - len - gapValue, thick, len, fill, outlineColor, ow, withOutline);
        this.rect(centerX - thick / 2.0F, centerY + gapValue, thick, len, fill, outlineColor, ow, withOutline);
        this.rect(centerX - len - gapValue, centerY - thick / 2.0F, len, thick, fill, outlineColor, ow, withOutline);
        this.rect(centerX + gapValue, centerY - thick / 2.0F, len, thick, fill, outlineColor, ow, withOutline);
        if (this.dot.getValue()) {
            this.rect(centerX - 1.0F, centerY - 1.0F, 2.0F, 2.0F, fill, outlineColor, ow, withOutline);
        }
        RenderUtil.disableRenderState();
    }

    private void rect(float x, float y, float w, float h, int color, int outlineColor, float outlineWidth,
                      boolean withOutline) {
        if (withOutline && outlineWidth > 0.0F) {
            RenderUtil.drawRect(x - outlineWidth, y - outlineWidth,
                    x + w + outlineWidth, y + h + outlineWidth, outlineColor);
        }
        RenderUtil.drawRect(x, y, x + w, y + h, color);
    }
}
