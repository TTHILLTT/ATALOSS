package myau.module.legit;

import myau.ATALOSS;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.TextProperty;
import myau.util.ChatUtil;
import myau.util.SkinTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

/**
 * 自定义皮肤（只影响本地渲染，不改账号皮肤数据）。
 *
 * <p>贴图来源与披风一致：本地文件（默认 {@code config/ATALOSS/skins/skin.png}）或 http(s) URL。
 * 需要 64x64（或等比例高清，如 128x128）的 1.8 皮肤布局；旧版 64x32 会提示先转换。
 *
 * <p>渲染入口是 {@code MixinAbstractClientPlayer.getLocationSkin()}；
 * {@code model} 开关可以顺便把模型切成 SLIM（Alex 细手臂）。
 */
public class CustomSkin extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean using = false;

    public final TextProperty path = new TextProperty("skin", "config/ATALOSS/skins/skin.png");
    public final ModeProperty model = new ModeProperty("model", 0, new String[]{"DEFAULT", "SLIM"});
    public final BooleanProperty allPlayers = new BooleanProperty("all-players", false);

    public CustomSkin() {
        super("CustomSkin", false, true, true);
    }

    public static boolean hasSkin() {
        return SkinTexture.isReady();
    }

    public static ResourceLocation getSkin() {
        return SkinTexture.LOCATION;
    }

    public static boolean isSlim() {
        CustomSkin module = instance();
        return module != null && module.model.getValue() == 1;
    }

    public static boolean appliesTo(Entity player) {
        if (!using || !SkinTexture.isReady() || player == null) {
            return false;
        }
        CustomSkin module = instance();
        boolean all = module != null && module.allPlayers.getValue();
        return player == mc.thePlayer || all;
    }

    private static CustomSkin instance() {
        return ATALOSS.moduleManager == null ? null
                : (CustomSkin) ATALOSS.moduleManager.modules.get(CustomSkin.class);
    }

    public void reload() {
        SkinTexture.clear();
        String error = SkinTexture.load(this.path.getValue());
        if (error == null) {
            ChatUtil.sendFormatted(String.format("%sSkin loaded &7(%dx%d)&r", ATALOSS.clientName,
                    SkinTexture.getWidth(), SkinTexture.getHeight()));
        } else {
            ChatUtil.sendFormatted(String.format("%sSkin failed: &c%s&r", ATALOSS.clientName, error));
        }
    }

    @Override
    public void onEnabled() {
        using = true;
        this.reload();
    }

    @Override
    public void onDisabled() {
        using = false;
    }

    @Override
    public void verifyValue(String string) {
        if ("skin".equals(string) && this.isEnabled()) {
            this.reload();
        }
    }
}
