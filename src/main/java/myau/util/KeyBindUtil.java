package myau.util;

import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class KeyBindUtil {
    /** 鼠标键在 key 里用负数存：buttonIndex - 100（3 -> -97） */
    public static final int MOUSE_KEY_OFFSET = 100;

    public static String getKeyName(int keyCode) {
        if (keyCode < 0) {
            int button = keyCode + MOUSE_KEY_OFFSET;
            switch (button) {
                case 0:
                    return "LMB";
                case 1:
                    return "RMB";
                case 2:
                    return "MMB";
                default:
                    // 对外按玩家习惯 1-based 显示：LWJGL button 3 = MOUSE4（第一个侧键）
                    String name = Mouse.getButtonName(button);
                    if (button >= 3) {
                        return "MOUSE" + (button + 1);
                    }
                    return name != null ? name : "MOUSE" + (button + 1);
            }
        }
        return Keyboard.getKeyName(keyCode);
    }

    /** 鼠标按键索引 -> 绑定用 key */
    public static int mouseKey(int buttonIndex) {
        return buttonIndex - MOUSE_KEY_OFFSET;
    }

    /** 绑定用 key -> 鼠标按键索引，非鼠标键返回 -1 */
    public static int mouseButton(int keyCode) {
        return keyCode < 0 ? keyCode + MOUSE_KEY_OFFSET : -1;
    }

    public static boolean isKeyDown(int keyCode) {
        return keyCode < 0 ? Mouse.isButtonDown(keyCode + MOUSE_KEY_OFFSET) : Keyboard.isKeyDown(keyCode);
    }

    public static void updateKeyState(int keyCode) {
        KeyBindUtil.setKeyBindState(keyCode, keyCode < 0 ? Mouse.isButtonDown(keyCode + MOUSE_KEY_OFFSET) : Keyboard.isKeyDown(keyCode));
    }

    public static void setKeyBindState(int keyCode, boolean pressed) {
        KeyBinding.setKeyBindState(keyCode, pressed);
    }

    public static void pressKeyOnce(int keyCode) {
        KeyBinding.onTick(keyCode);
    }
}
