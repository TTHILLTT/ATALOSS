package myau.module.legit;

import myau.event.EventTarget;
import myau.events.AttackEvent;
import myau.events.Render2DEvent;
import myau.property.properties.IntProperty;
import myau.util.RotationUtil;
import net.minecraft.util.Vec3;

/** 最近一次命中的距离（对应 FPSMaster 的 ReachDisplay），只读显示、不影响攻击判定 */
public class ReachDisplay extends LegitHudModule {
    public final IntProperty resetAfter = new IntProperty("reset-ms", 2500, 200, 10000);
    private double lastReach = -1.0D;
    private long lastHit = 0L;

    public ReachDisplay() {
        super("Reach", 4, 112);
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || event.getTarget() == null) {
            return;
        }
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
        this.lastReach = RotationUtil.distanceToBox(event.getTarget(), eyes);
        this.lastHit = System.currentTimeMillis();
    }

    @Override
    protected String[] getLines() {
        if (this.lastReach < 0.0D || System.currentTimeMillis() - this.lastHit > (long) this.resetAfter.getValue()) {
            return new String[]{"Reach -"};
        }
        return new String[]{String.format("Reach %.2f", this.lastReach)};
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        this.renderLines();
    }
}
