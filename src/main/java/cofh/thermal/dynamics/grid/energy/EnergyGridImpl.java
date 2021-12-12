package cofh.thermal.dynamics.grid.energy;

import cofh.thermal.dynamics.api.grid.energy.EnergyGrid;
import cofh.thermal.dynamics.api.grid.energy.EnergyGridNode;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.grid.AbstractGrid;
import cofh.thermal.dynamics.grid.AbstractGridNode;
import cofh.thermal.dynamics.init.TDynReferences;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * @author covers1624
 */
public class EnergyGridImpl extends AbstractGrid<EnergyGrid, EnergyGridNode> implements EnergyGrid {

    private final EnergyStorage storage = new EnergyStorage(10000); // TODO needs proper value, likely a function of the 'total distance' in the grid.

    public EnergyGridImpl(UUID id, World world) {

        super(TDynReferences.ENERGY_GRID, id, world);
    }

    @Override
    public AbstractGridNode<EnergyGrid> newNode() {

        return new EnergyGridNodeImpl(this);
    }

    @Override
    public void onMerge(EnergyGrid from) {
        // TODO resize storage.

        // TODO properly merge energy over.
        storage.receiveEnergy(from.getEnergyStored(), false);
    }

    @Override
    public void onSplit(List<EnergyGrid> others) {
        // TODO split energy evenly.
    }

    @Override
    public CompoundNBT serializeNBT() {
        // TODO save storage
        return super.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {

        super.deserializeNBT(nbt);
        // TODO load storage
    }

    @Override
    public boolean canConnectExternally(TileEntity tile, @Nullable Direction dir) {

        if (GridHelper.getGridHost(tile).isPresent()) return false; // We cannot externally connect to other grids.
        if (dir != null) {
            return tile.getCapability(CapabilityEnergy.ENERGY, dir.getOpposite()).isPresent();
        }
        return tile.getCapability(CapabilityEnergy.ENERGY).isPresent();
    }

    //@formatter:off
    @Override public int receiveEnergy(int maxReceive, boolean simulate) { return storage.receiveEnergy(maxReceive, simulate); }
    @Override public int extractEnergy(int maxExtract, boolean simulate) { return storage.extractEnergy(maxExtract, simulate); }
    @Override public int getEnergyStored() { return storage.getEnergyStored(); }
    @Override public int getMaxEnergyStored() { return storage.getMaxEnergyStored(); }
    @Override public boolean canExtract() { return storage.canExtract(); }
    @Override public boolean canReceive() { return storage.canReceive(); }
    //@formatter:on
}
