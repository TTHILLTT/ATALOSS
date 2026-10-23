package myau.module.legit;

import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.property.properties.BooleanProperty;

import java.text.SimpleDateFormat;
import java.util.Date;

/** 本地时间显示（对应 FPSMaster 的 ClockDisplay） */
public class ClockDisplay extends LegitHudModule {
    private final SimpleDateFormat format24 = new SimpleDateFormat("HH:mm:ss");
    private final SimpleDateFormat format12 = new SimpleDateFormat("hh:mm:ss a");
    public final BooleanProperty twentyFourHour = new BooleanProperty("24h", true);

    public ClockDisplay() {
        super("Clock", 4, 28);
    }

    @Override
    protected String[] getLines() {
        Date now = new Date();
        return new String[]{this.twentyFourHour.getValue() ? this.format24.format(now) : this.format12.format(now)};
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        this.renderLines();
    }
}
