package myau.module.legit;

import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.property.properties.BooleanProperty;
import net.minecraft.util.MathHelper;

/** 坐标显示，可选同时显示下界/主世界换算坐标（对应 FPSMaster 的 CoordsDisplay） */
public class CoordsDisplay extends LegitHudModule {
    public final BooleanProperty netherCoords = new BooleanProperty("nether-coords", false);

    public CoordsDisplay() {
        super("Coords", 4, 40);
    }

    @Override
    protected String[] getLines() {
        if (mc.thePlayer == null) {
            return null;
        }
        int x = MathHelper.floor_double(mc.thePlayer.posX);
        int y = MathHelper.floor_double(mc.thePlayer.posY);
        int z = MathHelper.floor_double(mc.thePlayer.posZ);
        String main = "XYZ " + x + " / " + y + " / " + z;
        if (!this.netherCoords.getValue() || mc.thePlayer.dimension == 1) {
            return new String[]{main};
        }
        if (mc.thePlayer.dimension == -1) {
            return new String[]{main, "OW " + (x * 8) + " / " + (z * 8)};
        }
        return new String[]{main, "NE " + (x / 8) + " / " + (z / 8)};
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        this.renderLines();
    }
}
