package cofh.thermal.dynamics.grid.energy;

import cofh.core.capability.IRedstoneFluxStorage;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.grid.Grid;
import cofh.thermal.dynamics.grid.GridNode;
import cofh.thermal.lib.util.ThermalEnergyHelper;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

import static cofh.thermal.dynamics.init.TDynGrids.GRID_ENERGY;

/**
 * @author covers1624
 */
public class EnergyGrid extends Grid<EnergyGrid, EnergyGridNode> implements IRedstoneFluxStorage {

    protected final long NODE_CAPACITY = 400;

    protected final GridEnergyStorage storage = new GridEnergyStorage(NODE_CAPACITY);
    protected LazyOptional<?> energyCap = LazyOptional.empty();

    protected EnergyGridNode[] distArray = new EnergyGridNode[0];
    protected int distIndex = 0;

    public EnergyGrid(UUID id, Level world) {

        super(GRID_ENERGY.get(), id, world);
    }

    @Override
    public EnergyGridNode newNode() {

        return new EnergyGridNode(this);
    }

    @Override
    public void tick() {

        storage.tick();

        if (distArray.length != getNodes().size()) {
            distArray = getNodes().values().toArray(new EnergyGridNode[0]);
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

        distArray = new EnergyGridNode[0];
        setBaseCapacity(getNodes().size() * NODE_CAPACITY);
        super.onModified();
    }

    @Override
    public void onMerge(EnergyGrid from) {

        storage.setBaseCapacity(NODE_CAPACITY * getNodes().size());
        storage.setCapacity(this.getCapacity() + from.getCapacity());
        storage.setEnergy(storage.getEnergy() + from.getEnergy());

        refreshCapabilities();
        from.refreshCapabilities();
    }

    @Override
    public void onSplit(List<EnergyGrid> others) {

        int totalNodes = 0;
        for (EnergyGrid grid : others) {
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

        for (EnergyGrid grid : others) {
            int gridNodes = grid.getNodes().size();
            grid.setEnergy(energyPerNode * gridNodes);
            grid.refreshCapabilities();
        }
        // First grid gets the extra. Why? Because there's always a first grid.
        others.get(0).setEnergy(others.get(0).getEnergy() + remEnergy);
    }

    @Override
    public CompoundTag serializeNBT() {

        CompoundTag tag = super.serializeNBT();
        storage.write(tag);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {

        super.deserializeNBT(nbt);
        storage.deserializeNBT(nbt);
    }

    @Override
    public boolean canConnectOnSide(BlockEntity tile, @Nullable Direction dir) {

        if (GridHelper.getGridHost(tile) != null) {
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
    public long getCapacity() { return storage.getBaseCapacity(); }
    public long getEnergy() { return storage.getEnergy(); }
    public void setBaseCapacity(long capacity) { storage.setBaseCapacity(capacity); }
    public void setCapacity(long capacity) { storage.setCapacity(capacity); }
    public void setEnergy(long energy) { storage.setEnergy(energy); }

    @Override public int receiveEnergy(int maxReceive, boolean simulate) { return storage.receiveEnergy(maxReceive, simulate); }
    @Override public int extractEnergy(int maxExtract, boolean simulate) { return storage.extractEnergy(maxExtract, simulate); }
    @Override public int getEnergyStored() { return storage.getEnergyStored(); }
    @Override public int getMaxEnergyStored() { return storage.getMaxEnergyStored(); }
    @Override public boolean canExtract() { return storage.canExtract(); }
    @Override public boolean canReceive() { return storage.canReceive(); }
    //@formatter:on
}
