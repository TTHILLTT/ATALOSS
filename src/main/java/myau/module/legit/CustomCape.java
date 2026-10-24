package myau.module.legit;

import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.TextProperty;
import myau.util.CapeTexture;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

/**
 * 自定义披风（只影响本地渲染，不改任何皮肤/账号数据）。
 *
 * <p>贴图来源支持本地文件或 http(s) URL：
 * <ul>
 *   <li>本地：如 {@code config/ATALOSS/capes/cape.png}（相对路径按游戏目录解析）；</li>
 *   <li>URL：如 {@code https://example.com/cape.png}，首次会下载并缓存到 {@code config/ATALOSS/capes/}。</li>
 * </ul>
 *
 * <p>渲染入口是 {@code MixinAbstractClientPlayer.getLocationCape()}：原版 {@code LayerCape}
 * 会拿这个贴图去画披风，所以不用自己写渲染代码。默认只给自己加，`all-players` 可以让所有玩家显示。
 */
public class CustomCape extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean using = false;

    public final TextProperty path = new TextProperty("cape", "config/ATALOSS/capes/cape.png");
    public final BooleanProperty allPlayers = new BooleanProperty("all-players", false);

    public CustomCape() {
        super("CustomCape", false, true, true);
    }

    public static boolean hasCape() {
        return CapeTexture.isReady();
    }

    public static ResourceLocation getCape() {
        return CapeTexture.LOCATION;
    }

    /** @param player 被渲染的玩家 */
    public static boolean appliesTo(Entity player) {
        if (!using || !CapeTexture.isReady()) {
            return false;
        }
        return player != null && (player == mc.thePlayer || isAllPlayers());
    }

    private static boolean isAllPlayers() {
        CustomCape module = instance();
        return module != null && module.allPlayers.getValue();
    }

    private static CustomCape instance() {
        return myau.ATALOSS.moduleManager == null ? null
                : (CustomCape) myau.ATALOSS.moduleManager.modules.get(CustomCape.class);
    }

    public void reload() {
        // 顺手把目录建好，方便玩家直接丢 png 进去
        try {
            new java.io.File(mc.mcDataDir, "config/ATALOSS/capes").mkdirs();
        } catch (Throwable ignored) {
        }
        CapeTexture.clear();
        String error = CapeTexture.load(this.path.getValue());
        if (error == null) {
            ChatUtil.sendFormatted(String.format("%sCape loaded &7(%dx%d)&r", myau.ATALOSS.clientName,
                    CapeTexture.getWidth(), CapeTexture.getHeight()));
        } else {
            ChatUtil.sendFormatted(String.format("%sCape failed: &c%s&r", myau.ATALOSS.clientName, error));
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
        if (!"cape".equals(string) || !this.isEnabled()) {
            return;
        }
        this.reload();
    }
}
