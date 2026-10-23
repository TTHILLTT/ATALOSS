package myau.module;

import myau.ATALOSS;
import myau.module.modules.HUD;
import myau.util.KeyBindUtil;

public abstract class Module {
    protected final String name;
    protected final boolean defaultEnabled;
    protected final int defaultKey;
    protected final boolean defaultHidden;
    /** 是否属于 Legit 菜单（HUD 信息 / 视觉辅助等非作弊功能） */
    protected final boolean legit;
    protected boolean enabled;
    protected int key;
    protected boolean hidden;

    public Module(String name, boolean enabled) {
        this(name, enabled, false, false);
    }

    public Module(String name, boolean enabled, boolean hidden) {
        this(name, enabled, hidden, false);
    }

    public Module(String name, boolean enabled, boolean hidden, boolean legit) {
        this.name = name;
        this.enabled = this.defaultEnabled = enabled;
        this.key = this.defaultKey = 0;
        this.hidden = this.defaultHidden = hidden;
        this.legit = legit;
    }

    /** 该模块显示在 Legit 菜单还是 Cheat 菜单 */
    public boolean isLegit() {
        return this.legit;
    }

    public String getName() {
        return this.name;
    }

    public String formatModule() {
        return String.format(
                "%s%s &r(%s&r)",
                this.key == 0 ? "" : String.format("&l[%s] &r", KeyBindUtil.getKeyName(this.key)),
                this.name,
                this.enabled ? "&a&lON" : "&c&lOFF"
        );
    }

    public String[] getSuffix() {
        return new String[0];
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                this.onEnabled();
            } else {
                this.onDisabled();
            }
        }
    }

    public boolean toggle() {
        boolean enabled = !this.enabled;
        this.setEnabled(enabled);
        if (this.enabled == enabled) {
            if (((HUD) ATALOSS.moduleManager.modules.get(HUD.class)).toggleSound.getValue()) {
                ATALOSS.moduleManager.playSound();
            }
            return true;
        } else {
            return false;
        }
    }

    public int getKey() {
        return this.key;
    }

    public void setKey(int integer) {
        this.key = integer;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public void setHidden(boolean boolean1) {
        this.hidden = boolean1;
    }

    public void onEnabled() {
    }

    public void onDisabled() {
    }

    public void verifyValue(String string) {
    }
}
