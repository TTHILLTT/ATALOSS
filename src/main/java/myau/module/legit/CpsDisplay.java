package myau.module.legit;

import myau.event.EventTarget;
import myau.events.LeftClickMouseEvent;
import myau.events.Render2DEvent;
import myau.events.RightClickMouseEvent;

import java.util.ArrayDeque;

/** 左右键 CPS 显示（对应 FPSMaster 的 CPSDisplay） */
public class CpsDisplay extends LegitHudModule {
    private final ArrayDeque<Long> leftClicks = new ArrayDeque<>();
    private final ArrayDeque<Long> rightClicks = new ArrayDeque<>();

    public CpsDisplay() {
        super("Cps", 4, 16);
    }

    @Override
    protected String[] getLines() {
        long now = System.currentTimeMillis();
        trim(this.leftClicks, now);
        trim(this.rightClicks, now);
        return new String[]{"CPS " + this.leftClicks.size() + " | " + this.rightClicks.size()};
    }

    private static void trim(ArrayDeque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000L) {
            clicks.pollFirst();
        }
    }

    @EventTarget
    public void onLeftClick(LeftClickMouseEvent event) {
        if (this.isEnabled()) {
            this.leftClicks.add(System.currentTimeMillis());
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled()) {
            this.rightClicks.add(System.currentTimeMillis());
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        this.renderLines();
    }
}
