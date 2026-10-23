package myau.module.legit;

import myau.event.EventTarget;
import myau.events.AttackEvent;
import myau.events.Render2DEvent;
import myau.property.properties.IntProperty;

/** 连击计数（对应 FPSMaster 的 ComboDisplay），超过 timeout 自动清零 */
public class ComboDisplay extends LegitHudModule {
    public final IntProperty timeout = new IntProperty("timeout-ms", 1500, 200, 5000);
    private int combo = 0;
    private long lastHit = 0L;

    public ComboDisplay() {
        super("Combo", 4, 124);
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!this.isEnabled() || event.getTarget() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastHit <= (long) this.timeout.getValue()) {
            this.combo++;
        } else {
            this.combo = 1;
        }
        this.lastHit = now;
    }

    @Override
    protected String[] getLines() {
        if (System.currentTimeMillis() - this.lastHit > (long) this.timeout.getValue()) {
            this.combo = 0;
        }
        return this.combo > 0 ? new String[]{"Combo x" + this.combo} : null;
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        this.renderLines();
    }
}
