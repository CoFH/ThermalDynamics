package cofh.thermal.dynamics.grid.energy;

import cofh.thermal.dynamics.api.grid.energy.IEnergyGrid;
import cofh.thermal.dynamics.api.grid.energy.IEnergyGridNode;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.grid.AbstractGrid;
import cofh.thermal.dynamics.grid.AbstractGridNode;
import cofh.thermal.dynamics.init.TDynReferences;
import cofh.thermal.lib.util.ThermalEnergyHelper;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * @author covers1624
 */
public class EnergyGrid extends AbstractGrid<IEnergyGrid, IEnergyGridNode> implements IEnergyGrid {

    protected final long NODE_CAPACITY = 500000;

    protected final GridEnergyStorage storage = new GridEnergyStorage(NODE_CAPACITY);
    protected LazyOptional<?> energyCap = LazyOptional.empty();

    protected IEnergyGridNode[] distArray = new IEnergyGridNode[0];
    protected int distIndex = 0;

    public EnergyGrid(UUID id, World world) {

        super(TDynReferences.ENERGY_GRID, id, world);
    }

    @Override
    public AbstractGridNode<IEnergyGrid> newNode() {

        return new EnergyGridNode(this);
    }

    @Override
    public void tick() {

        if (distArray.length != getNodes().size()) {
            distArray = getNodes().values().toArray(new IEnergyGridNode[0]);
            if (distIndex >= distArray.length) {
                distIndex = 0;
            }
        }
        int i = distIndex;
        loops:
        {
            for (int c = distIndex, e = distArray.length; c < e; ++c) {
                distArray[i].tick();
                ++i; // don't 'stick' to a sink, increment before break
                if (getEnergy() <= 0) {
                    break loops;
                }
            }
            i = 0;
            for (int c = 0, e = distIndex; (getEnergy() > 0) & (c < e); ++c) {
                distArray[i].tick();
                ++i;
            }
        }
        distIndex = i;
    }

    @Override
    public void onMerge(IEnergyGrid from) {

        //        storage.setCapacity(NODE_CAPACITY * getNodes().size());
        //        storage.setEnergy(storage.getEnergy() + from.getEnergy());
    }

    @Override
    public void onSplit(List<IEnergyGrid> others) {

        //        int totalNodes = 0;
        //        for (IEnergyGrid grid : others) {
        //            int gridNodes = grid.getNodes().size();
        //            totalNodes += grid.getNodes().size();
        //            grid.setCapacity(NODE_CAPACITY * gridNodes);
        //        }
        //        for (int i = others.size() - 1; i > 0; --i) {
        //            int gridNodes = others.get(i).getNodes().size();
        //            setCapacity((gridNodes * getCapacity()) / totalNodes);
        //            setEnergy((gridNodes * getEnergy()) / totalNodes);
        //        }

        //        for (IEnergyGrid grid : others) {
        //            int gridNodes = grid.getNodes().size();
        //            setCapacity((gridNodes * getCapacity()) / totalNodes);
        //            setEnergy((gridNodes * getEnergy()) / totalNodes);
        //        }
    }

    @Override
    public CompoundNBT serializeNBT() {

        storage.serializeNBT();
        return super.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {

        super.deserializeNBT(nbt);
        storage.deserializeNBT(nbt);
    }

    @Override
    public boolean canConnectOnSide(TileEntity tile, @Nullable Direction dir) {

        if (GridHelper.getGridHost(tile).isPresent()) {
            return false; // We cannot externally connect to other grids.
        }
        if (dir != null) {
            return tile.getCapability(ThermalEnergyHelper.getBaseEnergySystem(), dir).isPresent();
        }
        return false;
        // return tile.getCapability(ThermalEnergyHelper.getBaseEnergySystem()).isPresent();
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap) {

        if (cap == ThermalEnergyHelper.getBaseEnergySystem()) {
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

    // region IEnergyGrid
    public long getCapacity() {

        return storage.getCapacity();
    }

    public long getEnergy() {

        return storage.getEnergy();
    }

    public void setCapacity(long capacity) {

        storage.setCapacity(capacity);
    }

    public void setEnergy(long energy) {

        storage.setEnergy(energy);
    }
    // endregion

    //@formatter:off
    @Override public int receiveEnergy(int maxReceive, boolean simulate) { return storage.receiveEnergy(maxReceive, simulate); }
    @Override public int extractEnergy(int maxExtract, boolean simulate) { return storage.extractEnergy(maxExtract, simulate); }
    @Override public int getEnergyStored() { return storage.getEnergyStored(); }
    @Override public int getMaxEnergyStored() { return storage.getMaxEnergyStored(); }
    @Override public boolean canExtract() { return storage.canExtract(); }
    @Override public boolean canReceive() { return storage.canReceive(); }
    //@formatter:on
}
