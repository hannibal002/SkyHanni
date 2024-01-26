package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockCrops.class)
public class MixinBlockCrops extends BlockBush {

    @Override
    public AxisAlignedBB getSelectedBoundingBox(World worldIn, BlockPos pos) {
        updateCropsMaxY(worldIn, pos, worldIn.getBlockState(pos).getBlock());
        return super.getSelectedBoundingBox(worldIn, pos);
    }

    @Override
    public MovingObjectPosition collisionRayTrace(World worldIn, BlockPos pos, Vec3 start, Vec3 end) {
        updateCropsMaxY(worldIn, pos, worldIn.getBlockState(pos).getBlock());
        return super.collisionRayTrace(worldIn, pos, start, end);
    }

    private void updateCropsMaxY(World world, BlockPos pos, Block block) {
        final IBlockState blockState = world.getBlockState(pos);
        if (Minecraft.getMinecraft().theWorld != null) {
            if(blockState.getValue(BlockCrops.AGE) == 7) {
                ((BlockAccessor)block).setMaxY_skyhanni(1f);
                return;
            }
        }
        ((BlockAccessor)block).setMaxY_skyhanni(0.25f);
    }
}
