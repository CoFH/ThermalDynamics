package cofh.thermal.dynamics.block;

import cofh.thermal.dynamics.tileentity.FluidDuctTile;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;

import javax.annotation.Nullable;

public class TileBlockDuctFluid extends TileBlockDuctBase {

    public TileBlockDuctFluid(Properties properties) {

        super(properties);
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {

        return new FluidDuctTile();
    }

}
