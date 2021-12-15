package cofh.thermal.dynamics.grid.energy;

import cofh.lib.energy.EnergyStorageCoFH;
import cofh.thermal.dynamics.api.grid.energy.IEnergyGrid;
import cofh.thermal.dynamics.api.grid.energy.IEnergyGridNode;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.grid.AbstractGrid;
import cofh.thermal.dynamics.grid.AbstractGridNode;
import cofh.thermal.dynamics.init.TDynReferences;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * @author covers1624
 */
public class EnergyGrid extends AbstractGrid<IEnergyGrid, IEnergyGridNode> implements IEnergyGrid {

    protected final EnergyStorageCoFH storage = new EnergyStorageCoFH(100000); // TODO needs proper value, likely a function of the 'total distance' in the grid.
    protected LazyOptional<?> energyCap = LazyOptional.empty();

    public EnergyGrid(UUID id, World world) {

        super(TDynReferences.ENERGY_GRID, id, world);
    }

    @Override
    public AbstractGridNode<IEnergyGrid> newNode() {

        return new EnergyGridNode(this);
    }

    @Override
    public void onMerge(IEnergyGrid from) {
        // TODO resize storage.

        // TODO properly merge energy over.
        storage.receiveEnergy(from.getEnergyStored(), false);
    }

    @Override
    public void onSplit(List<IEnergyGrid> others) {
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

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap) {

        if (cap == CapabilityEnergy.ENERGY) {
            if (!energyCap.isPresent()) {
                energyCap = LazyOptional.of(() -> storage);
            }
            return energyCap.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public void refreshCapabilities() {

        energyCap.invalidate();
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
