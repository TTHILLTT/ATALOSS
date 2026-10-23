package myau.module.legit;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.PacketEvent;
import myau.module.Module;
import myau.property.properties.IntProperty;
import myau.property.properties.TextProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.S02PacketChat;

import java.util.Locale;

/** 对局结束时自动在聊天里发一句（对应 FPSMaster 的 AutoGG） */
public class AutoGG extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    /** 小写匹配：覆盖常见服务端（Hypixel / BedWars 系列）的结束文案 */
    private static final String[] TRIGGERS = {
            "1st killer", "1st place", "winning team", "you won", "victory",
            "game over", "you died", "defeat", "match ended", "game ended"
    };

    public final TextProperty message = new TextProperty("message", "gg");
    public final IntProperty cooldown = new IntProperty("cooldown-ms", 5000, 1000, 30000);
    private long lastSent;

    public AutoGG() {
        super("AutoGG", false, true, true);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.RECEIVE || !(event.getPacket() instanceof S02PacketChat)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastSent < (long) this.cooldown.getValue()) {
            return;
        }
        String text = ((S02PacketChat) event.getPacket()).getChatComponent().getUnformattedText().toLowerCase(Locale.ROOT);
        for (String trigger : TRIGGERS) {
            if (!text.contains(trigger)) {
                continue;
            }
            String out = this.message.getValue();
            if (out == null || out.trim().isEmpty()) {
                return;
            }
            this.lastSent = now;
            if (mc.thePlayer != null) {
                mc.thePlayer.sendChatMessage(out.trim());
            }
            return;
        }
    }
}
