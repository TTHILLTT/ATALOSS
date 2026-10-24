package myau.module.legit;

import myau.event.EventTarget;
import myau.events.Render3DEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.FloatProperty;
import myau.util.RenderUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;

/**
 * 准星指向方块的填充/描边（对应 FPSMaster 的 BlockOverlay）。
 *
 * <p>注意 ATALOSS 的绘制约定：{@code Render3DEvent} 里的坐标是**摄像机相对坐标**，
 * 所以世界坐标要自己减去 {@code renderManager.viewerPos*}；绘制前必须先
 * {@code RenderUtil.enableRenderState()}，否则纹理会开着、深度测试也在，什么都看不到。
 */
public class BlockOverlay extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final BooleanProperty fill = new BooleanProperty("fill", true);
    public final BooleanProperty outline = new BooleanProperty("outline", true);
    public final BooleanProperty throughBlock = new BooleanProperty("through-block", false);
    public final FloatProperty width = new FloatProperty("width", 1.5F, 0.1F, 10.0F, this.outline::getValue);
    public final ColorProperty fillColor = new ColorProperty("fill-color", 0x32FFFFFF, this.fill::getValue);
    public final ColorProperty outlineColor = new ColorProperty("outline-color", 0xFFFFFFFF, this.outline::getValue);

    public BlockOverlay() {
        super("BlockOverlay", false, true, true);
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled() || mc.theWorld == null || mc.thePlayer == null || mc.objectMouseOver == null) {
            return;
        }
        if (mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }
        BlockPos pos = mc.objectMouseOver.getBlockPos();
        if (pos == null) {
            return;
        }
        IBlockState state = mc.theWorld.getBlockState(pos);
        if (state == null || state.getBlock() == null || state.getBlock().getMaterial().isReplaceable()) {
            return;
        }
        double x = (double) pos.getX() - mc.getRenderManager().viewerPosX;
        double y = (double) pos.getY() - mc.getRenderManager().viewerPosY;
        double z = (double) pos.getZ() - mc.getRenderManager().viewerPosZ;
        AxisAlignedBB box = new AxisAlignedBB(
                x - 0.002, y - 0.002, z - 0.002,
                x + 1.002, y + 1.002, z + 1.002);

        RenderUtil.enableRenderState();
        if (!this.throughBlock.getValue()) {
            GlStateManager.enableDepth();
        }
        if (this.fill.getValue()) {
            this.drawFill(box, this.fillColor.getValue());
        }
        if (this.outline.getValue()) {
            int color = this.outlineColor.getValue();
            RenderUtil.drawBoundingBox(box,
                    (color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF,
                    color >>> 24, this.width.getValue());
        }
        GlStateManager.enableDepth();
        RenderUtil.disableRenderState();
    }

    /** 6 个面，颜色按 ARGB 解包；填充透明度不超过 120，避免把方块整块糊住 */
    private void drawFill(AxisAlignedBB box, int argb) {
        int a = Math.min((argb >>> 24) & 0xFF, 120);
        if (a <= 0) {
            a = 50;
        }
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        // 上
        wr.pos(box.minX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        wr.pos(box.minX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        wr.pos(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        wr.pos(box.maxX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        // 下
        wr.pos(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        wr.pos(box.maxX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        wr.pos(box.maxX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        wr.pos(box.minX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        // 北
        wr.pos(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        wr.pos(box.minX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        wr.pos(box.maxX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        wr.pos(box.maxX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        // 南
        wr.pos(box.minX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        wr.pos(box.maxX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        wr.pos(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        wr.pos(box.minX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        // 西
        wr.pos(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        wr.pos(box.minX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        wr.pos(box.minX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        wr.pos(box.minX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        // 东
        wr.pos(box.maxX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        wr.pos(box.maxX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        wr.pos(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        wr.pos(box.maxX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        tessellator.draw();
    }
}
