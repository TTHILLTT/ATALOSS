package myau.module.legit;

import myau.event.EventTarget;
import myau.events.Render2DEvent;
import net.minecraft.util.MathHelper;

/** 朝向显示（对应 FPSMaster 的 DirectionDisplay） */
public class DirectionDisplay extends LegitHudModule {

    public DirectionDisplay() {
        super("Direction", 4, 52);
    }

    @Override
    protected String[] getLines() {
        if (mc.thePlayer == null) {
            return null;
        }
        // Minecraft 的 yaw：0 = 南(+Z)，90 = 西(-X)，180 = 北(-Z)，-90 = 东(+X)
        float yaw = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw);
        String direction;
        String axis;
        if (yaw >= -45.0F && yaw < 45.0F) {
            direction = "South";
            axis = "+Z";
        } else if (yaw >= 45.0F && yaw < 135.0F) {
            direction = "West";
            axis = "-X";
        } else if (yaw >= 135.0F || yaw < -135.0F) {
            direction = "North";
            axis = "-Z";
        } else {
            direction = "East";
            axis = "+X";
        }
        return new String[]{direction + " (" + axis + ")"};
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        this.renderLines();
    }
}
