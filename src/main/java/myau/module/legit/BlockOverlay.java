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
        // 填充略微外扩，避免和方块表面 z-fighting
        AxisAlignedBB fillBox = new AxisAlignedBB(
                x - 0.002, y - 0.002, z - 0.002,
                x + 1.002, y + 1.002, z + 1.002);
        // 描边再多外扩一点：填充面/方块表面几乎共面，共面时线会被深度测试整段吃掉
        AxisAlignedBB outlineBox = new AxisAlignedBB(
                x - 0.02, y - 0.02, z - 0.02,
                x + 1.02, y + 1.02, z + 1.02);

        RenderUtil.enableRenderState();
        if (!this.throughBlock.getValue()) {
            GlStateManager.enableDepth();
        }
        if (this.fill.getValue()) {
            this.drawFill(fillBox, this.fillColor.getValue());
        }
        if (this.outline.getValue()) {
            int color = this.outlineColor.getValue();
            // 画线不写深度（仍然参与深度测试），否则线的光栅化位置容易被同深度像素覆盖
            GlStateManager.depthMask(false);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            this.drawOutlineBars(outlineBox, color, this.width.getValue());
            GlStateManager.depthMask(true);
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

    /** 12 条棱画成细长方体（复用 quad 路径，绕开 GL_LINES） */
    private void drawOutlineBars(AxisAlignedBB box, int argb, float lineWidth) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        double t = Math.max(0.012D, (double) lineWidth * 0.01D);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        double minX = box.minX, minY = box.minY, minZ = box.minZ;
        double maxX = box.maxX, maxY = box.maxY, maxZ = box.maxZ;
        // 底面
        this.edge(wr, minX, minY, minZ, maxX, minY, minZ, t, r, g, b, a);
        this.edge(wr, maxX, minY, minZ, maxX, minY, maxZ, t, r, g, b, a);
        this.edge(wr, maxX, minY, maxZ, minX, minY, maxZ, t, r, g, b, a);
        this.edge(wr, minX, minY, maxZ, minX, minY, minZ, t, r, g, b, a);
        // 顶面
        this.edge(wr, minX, maxY, minZ, maxX, maxY, minZ, t, r, g, b, a);
        this.edge(wr, maxX, maxY, minZ, maxX, maxY, maxZ, t, r, g, b, a);
        this.edge(wr, maxX, maxY, maxZ, minX, maxY, maxZ, t, r, g, b, a);
        this.edge(wr, minX, maxY, maxZ, minX, maxY, minZ, t, r, g, b, a);
        // 竖棱
        this.edge(wr, minX, minY, minZ, minX, maxY, minZ, t, r, g, b, a);
        this.edge(wr, maxX, minY, minZ, maxX, maxY, minZ, t, r, g, b, a);
        this.edge(wr, maxX, minY, maxZ, maxX, maxY, maxZ, t, r, g, b, a);
        this.edge(wr, minX, minY, maxZ, minX, maxY, maxZ, t, r, g, b, a);
        tessellator.draw();
    }

    /** 一条棱：沿轴方向取全长，另外两轴各加 t 的厚度，再按长方体出 6 个面 */
    private void edge(WorldRenderer wr, double x1, double y1, double z1, double x2, double y2, double z2,
                      double t, int r, int g, int b, int a) {
        double hx = Math.abs(x2 - x1) > 1.0E-6D ? 0.0D : t;
        double hy = Math.abs(y2 - y1) > 1.0E-6D ? 0.0D : t;
        double hz = Math.abs(z2 - z1) > 1.0E-6D ? 0.0D : t;
        double minX = Math.min(x1, x2) - hx, maxX = Math.max(x1, x2) + hx;
        double minY = Math.min(y1, y2) - hy, maxY = Math.max(y1, y2) + hy;
        double minZ = Math.min(z1, z2) - hz, maxZ = Math.max(z1, z2) + hz;
        // 上
        wr.pos(minX, maxY, minZ).color(r, g, b, a).endVertex();
        wr.pos(minX, maxY, maxZ).color(r, g, b, a).endVertex();
        wr.pos(maxX, maxY, maxZ).color(r, g, b, a).endVertex();
        wr.pos(maxX, maxY, minZ).color(r, g, b, a).endVertex();
        // 下
        wr.pos(minX, minY, minZ).color(r, g, b, a).endVertex();
        wr.pos(maxX, minY, minZ).color(r, g, b, a).endVertex();
        wr.pos(maxX, minY, maxZ).color(r, g, b, a).endVertex();
        wr.pos(minX, minY, maxZ).color(r, g, b, a).endVertex();
        // 北
        wr.pos(minX, minY, minZ).color(r, g, b, a).endVertex();
        wr.pos(minX, maxY, minZ).color(r, g, b, a).endVertex();
        wr.pos(maxX, maxY, minZ).color(r, g, b, a).endVertex();
        wr.pos(maxX, minY, minZ).color(r, g, b, a).endVertex();
        // 南
        wr.pos(minX, minY, maxZ).color(r, g, b, a).endVertex();
        wr.pos(maxX, minY, maxZ).color(r, g, b, a).endVertex();
        wr.pos(maxX, maxY, maxZ).color(r, g, b, a).endVertex();
        wr.pos(minX, maxY, maxZ).color(r, g, b, a).endVertex();
        // 西
        wr.pos(minX, minY, minZ).color(r, g, b, a).endVertex();
        wr.pos(minX, minY, maxZ).color(r, g, b, a).endVertex();
        wr.pos(minX, maxY, maxZ).color(r, g, b, a).endVertex();
        wr.pos(minX, maxY, minZ).color(r, g, b, a).endVertex();
        // 东
        wr.pos(maxX, minY, minZ).color(r, g, b, a).endVertex();
        wr.pos(maxX, maxY, minZ).color(r, g, b, a).endVertex();
        wr.pos(maxX, maxY, maxZ).color(r, g, b, a).endVertex();
        wr.pos(maxX, minY, maxZ).color(r, g, b, a).endVertex();
    }
}
