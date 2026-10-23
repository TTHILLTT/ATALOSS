package myau.module.legit;

import myau.event.EventTarget;
import myau.events.Render2DEvent;
import net.minecraft.client.network.NetworkPlayerInfo;

/** 延迟显示（对应 FPSMaster 的 PingDisplay） */
public class PingDisplay extends LegitHudModule {

    public PingDisplay() {
        super("Ping", 4, 64);
    }

    @Override
    protected String[] getLines() {
        if (mc.thePlayer == null || mc.getNetHandler() == null) {
            return new String[]{"Ping -"};
        }
        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        return new String[]{"Ping " + (info == null ? 0 : info.getResponseTime()) + "ms"};
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        this.renderLines();
    }
}
