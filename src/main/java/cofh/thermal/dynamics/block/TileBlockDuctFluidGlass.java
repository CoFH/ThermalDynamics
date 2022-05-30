package cofh.thermal.dynamics.block;

import cofh.thermal.dynamics.tileentity.FluidDuctGlassTile;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;

import javax.annotation.Nullable;

public class TileBlockDuctFluidGlass extends TileBlockDuctBase {

    public TileBlockDuctFluidGlass(Properties properties) {

        super(properties);
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {

        return new FluidDuctGlassTile();
    }

}
