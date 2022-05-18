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

    protected final long NODE_CAPACITY = 400;

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

        storage.tick();

        if (distArray.length != getNodes().size()) {
            distArray = getNodes().values().toArray(new IEnergyGridNode[0]);
        }
        int curIndex = distIndex;

        if (distIndex >= distArray.length) {
            distIndex = 0;
        }
        for (int i = distIndex; i < distArray.length; ++i) {
            if (rrNodeTick(curIndex, i)) {
                storage.postTick();
                return;
            }
        }
        for (int i = 0; i < distIndex; ++i) {
            if (rrNodeTick(curIndex, i)) {
                storage.postTick();
                return;
            }
        }
        ++distIndex;
        storage.postTick();
    }

    private boolean rrNodeTick(int curIndex, int i) {

        if (!distArray[i].isLoaded()) {
            return false;
        }
        distArray[i].tick();
        if (getEnergy() <= 0) {
            distIndex = i + 1;
            if (curIndex == distIndex) {
                --distIndex;
            }
            return true;
        }
        return false;
    }

    @Override
    public void onModified() {

        distArray = new IEnergyGridNode[0];
        setBaseCapacity(getNodes().size() * NODE_CAPACITY);
        super.onModified();
    }

    @Override
    public void onMerge(IEnergyGrid from) {

        storage.setBaseCapacity(NODE_CAPACITY * getNodes().size());
        storage.setCapacity(this.getCapacity() + from.getCapacity());
        storage.setEnergy(storage.getEnergy() + from.getEnergy());
    }

    @Override
    public void onSplit(List<IEnergyGrid> others) {

        int totalNodes = 0;
        for (IEnergyGrid grid : others) {
            int gridNodes = grid.getNodes().size();
            totalNodes += grid.getNodes().size();
            grid.setBaseCapacity(NODE_CAPACITY * gridNodes);
            grid.setCapacity(this.getCapacity());
        }
        if (getEnergy() <= 0) {
            return;
        }
        long energyPerNode = getEnergy() / totalNodes;
        long remEnergy = getEnergy() % totalNodes;

        for (IEnergyGrid grid : others) {
            int gridNodes = grid.getNodes().size();
            grid.setEnergy(energyPerNode * gridNodes);
        }
        // First grid gets the extra. Why? Because there's always a first grid.
        others.get(0).setEnergy(others.get(0).getEnergy() + remEnergy);
    }

    @Override
    public CompoundNBT serializeNBT() {

        CompoundNBT tag = super.serializeNBT();
        storage.write(tag);
        return tag;
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

    //@formatter:off
    @Override public long getCapacity() { return storage.getBaseCapacity(); }
    @Override public long getEnergy() { return storage.getEnergy(); }
    @Override public void setBaseCapacity(long capacity) { storage.setBaseCapacity(capacity); }
    @Override public void setCapacity(long capacity) { storage.setCapacity(capacity); }
    @Override public void setEnergy(long energy) { storage.setEnergy(energy); }

    @Override public int receiveEnergy(int maxReceive, boolean simulate) { return storage.receiveEnergy(maxReceive, simulate); }
    @Override public int extractEnergy(int maxExtract, boolean simulate) { return storage.extractEnergy(maxExtract, simulate); }
    @Override public int getEnergyStored() { return storage.getEnergyStored(); }
    @Override public int getMaxEnergyStored() { return storage.getMaxEnergyStored(); }
    @Override public boolean canExtract() { return storage.canExtract(); }
    @Override public boolean canReceive() { return storage.canReceive(); }
    //@formatter:on
}
