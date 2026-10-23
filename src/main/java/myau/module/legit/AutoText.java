package myau.module.legit;

import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.TextProperty;
import net.minecraft.client.Minecraft;

/**
 * 按绑定键发送预设文本（对应 FPSMaster 的 AutoText）。
 *
 * <p>做成"动作模块"：用模块自己的绑定键触发（GUI 里的 Bind 控件或 {@code .bind AutoText g}），
 * 按下即发送并立刻回到 OFF —— 比用 0-255 的数字滑块选键好用得多。
 */
public class AutoText extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final TextProperty message = new TextProperty("message", "hi");
    public final BooleanProperty asCommand = new BooleanProperty("as-command", false);

    public AutoText() {
        super("AutoText", false, true, true);
    }

    @Override
    public void onEnabled() {
        setEnabled(false);
        String text = this.message.getValue();
        if (text == null || text.trim().isEmpty() || mc.thePlayer == null) {
            return;
        }
        text = text.trim();
        if (this.asCommand.getValue() && !text.startsWith("/")) {
            text = "/" + text;
        }
        mc.thePlayer.sendChatMessage(text);
    }
}
