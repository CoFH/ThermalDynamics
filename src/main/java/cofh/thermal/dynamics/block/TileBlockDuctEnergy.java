package cofh.thermal.dynamics.block;

import cofh.thermal.dynamics.tileentity.DuctTileEnergy;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;

import javax.annotation.Nullable;

public class TileBlockDuctEnergy extends TileBlockDuctBase {

    public TileBlockDuctEnergy(Properties properties) {

        super(properties);
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {

        return new DuctTileEnergy();
    }

}
