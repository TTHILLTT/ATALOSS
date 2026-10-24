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
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;

import java.awt.Color;

/** 准星指向方块的描边/高亮（对应 FPSMaster 的 BlockOverlay） */
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
        if (state == null || state.getBlock().getMaterial().isReplaceable()) {
            return;
        }
        if (this.fill.getValue()) {
            Color color = new Color(this.fillColor.getValue());
            AxisAlignedBB box = new AxisAlignedBB(
                    pos.getX() - 0.002, pos.getY() - 0.002, pos.getZ() - 0.002,
                    pos.getX() + 1.002, pos.getY() + 1.002, pos.getZ() + 1.002);
            RenderUtil.drawFilledBox(box, color.getRed(), color.getGreen(), color.getBlue());
        }
        if (this.outline.getValue()) {
            Color color = new Color(this.outlineColor.getValue());
            RenderUtil.drawBlockBoundingBox(pos, 1.0, color.getRed(), color.getGreen(),
                    color.getBlue(), color.getAlpha(), this.width.getValue());
        }
    }
}
