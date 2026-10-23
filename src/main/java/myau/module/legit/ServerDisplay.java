package myau.module.legit;

import myau.event.EventTarget;
import myau.events.Render2DEvent;
import net.minecraft.client.multiplayer.ServerData;

/** 当前服务器地址（对应 FPSMaster 的 ServerAddressDisplay） */
public class ServerDisplay extends LegitHudModule {

    public ServerDisplay() {
        super("Server", 4, 100);
    }

    @Override
    protected String[] getLines() {
        ServerData data = mc.getCurrentServerData();
        return new String[]{data == null ? "Singleplayer" : data.serverIP};
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        this.renderLines();
    }
}
