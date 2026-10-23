package myau.module.legit;

import myau.event.EventTarget;
import myau.events.Render2DEvent;

/** 本次会话时长（对应 FPSMaster 的 PlayTime） */
public class PlayTimeDisplay extends LegitHudModule {
    private long sessionStart = System.currentTimeMillis();

    public PlayTimeDisplay() {
        super("PlayTime", 4, 76);
    }

    @Override
    public void onEnabled() {
        this.sessionStart = System.currentTimeMillis();
    }

    @Override
    protected String[] getLines() {
        long elapsed = Math.max(0L, System.currentTimeMillis() - this.sessionStart) / 1000L;
        long hours = elapsed / 3600L;
        long minutes = (elapsed % 3600L) / 60L;
        long seconds = elapsed % 60L;
        return new String[]{String.format("%02d:%02d:%02d", hours, minutes, seconds)};
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        this.renderLines();
    }
}
