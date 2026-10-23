package myau.module.legit;

import myau.event.EventTarget;
import myau.events.Render2DEvent;

/** FPS 显示（对应 FPSMaster 的 FPSDisplay） */
public class FpsDisplay extends LegitHudModule {

    public FpsDisplay() {
        super("Fps", 4, 4);
    }

    @Override
    protected String[] getLines() {
        return new String[]{"FPS " + getFps()};
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        this.renderLines();
    }
}
