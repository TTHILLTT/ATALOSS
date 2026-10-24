package myau.module.legit;

import myau.event.EventTarget;
import myau.events.Render3DEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.PercentProperty;
import myau.util.BoxBarRenderer;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;

/** 显示所有实体的碰撞箱轮廓（对应 FPSMaster 的 Hitboxes，只渲染、不改判定） */
public class Hitboxes extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ColorProperty color = new ColorProperty("color", 0xFF5555);
    public final PercentProperty opacity = new PercentProperty("opacity", 100);
    public final FloatProperty width = new FloatProperty("width", 1.5F, 0.5F, 5.0F);
    public final BooleanProperty expand = new BooleanProperty("expand", false);

    public Hitboxes() {
        super("Hitboxes", false, true, true);
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled() || mc.theWorld == null || mc.thePlayer == null) {
            return;
        }
        int rgb = this.color.getValue();
        int alpha = (int) (255.0F * ((float) this.opacity.getValue() / 100.0F));
        if (alpha <= 0) {
            return;
        }
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        float partialTicks = event.getPartialTicks();
        RenderUtil.enableRenderState();
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity == mc.thePlayer || entity.isInvisibleToPlayer(mc.thePlayer)) {
                continue;
            }
            double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double) partialTicks
                    - mc.getRenderManager().viewerPosX;
            double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) partialTicks
                    - mc.getRenderManager().viewerPosY;
            double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double) partialTicks
                    - mc.getRenderManager().viewerPosZ;
            double expansion = this.expand.getValue() ? (double) entity.getCollisionBorderSize() : 0.0D;
            AxisAlignedBB box = entity.getEntityBoundingBox()
                    .expand(expansion, expansion, expansion)
                    .offset(x - entity.posX, y - entity.posY, z - entity.posZ);
            BoxBarRenderer.drawBars(box, this.width.getValue(), red, green, blue, alpha);
        }
        GlStateManager.enableDepth();
        RenderUtil.disableRenderState();
    }
}
