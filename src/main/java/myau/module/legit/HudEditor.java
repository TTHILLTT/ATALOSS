package myau.module.legit;

import myau.module.Module;
import myau.ui.HudEditorScreen;
import net.minecraft.client.Minecraft;

/** 打开 Legit HUD 的拖拽编辑器（动作模块：开启即弹界面，然后立刻回到 OFF，可绑键） */
public class HudEditor extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public HudEditor() {
        super("HudEditor", false, true, true);
    }

    @Override
    public void onEnabled() {
        setEnabled(false);
        mc.displayGuiScreen(new HudEditorScreen());
    }
}
