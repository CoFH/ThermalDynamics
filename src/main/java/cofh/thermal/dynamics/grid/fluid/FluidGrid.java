package cofh.thermal.dynamics.grid.fluid;

import cofh.thermal.dynamics.api.grid.energy.IEnergyGridNode;
import cofh.thermal.dynamics.api.grid.fluid.IFluidGrid;
import cofh.thermal.dynamics.api.grid.fluid.IFluidGridNode;
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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;

/**
 * @author King Lemming
 */
public class FluidGrid extends AbstractGrid<IFluidGrid, IFluidGridNode> implements IFluidGrid {

    protected final int NODE_CAPACITY = 4000;

    protected final GridFluidStorage storage = new GridFluidStorage(NODE_CAPACITY);
    protected LazyOptional<?> fluidCap = LazyOptional.empty();

    protected IFluidGridNode[] distArray = new IFluidGridNode[0];
    protected int distIndex = 0;

    public FluidGrid(UUID id, World world) {

        super(TDynReferences.FLUID_GRID, id, world);
    }

    @Override
    public AbstractGridNode<IFluidGrid> newNode() {

        return new FluidGridNode(this);
    }

    @Override
    public void tick() {

        // storage.tick();

        if (distArray.length != getNodes().size()) {
            distArray = getNodes().values().toArray(new IFluidGridNode[0]);
        }
        int curIndex = distIndex;

        if (distIndex >= distArray.length) {
            distIndex = 0;
        }
        for (int i = distIndex; i < distArray.length; ++i) {
            if (rrNodeTick(curIndex, i)) return;
        }
        for (int i = 0; i < distIndex; ++i) {
            if (rrNodeTick(curIndex, i)) return;
        }
        ++distIndex;
    }

    private boolean rrNodeTick(int curIndex, int i) {

        if (!distArray[i].isLoaded()) {
            return false;
        }
        distArray[i].tick();
        if (getFluid().isEmpty()) {
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

        distArray = new IFluidGridNode[0];
        setCapacity(getNodes().size() * NODE_CAPACITY);
        super.onModified();
    }

    @Override
    public void onMerge(IFluidGrid from) {

        storage.setCapacity(NODE_CAPACITY * getNodes().size());
        storage.fill(from.getFluid(), EXECUTE);
    }

    @Override
    public void onSplit(List<IFluidGrid> others) {

        int totalNodes = 0;
        for (IFluidGrid grid : others) {
            int gridNodes = grid.getNodes().size();
            totalNodes += grid.getNodes().size();
            grid.setCapacity(NODE_CAPACITY * gridNodes);
        }
        int fluidPerNode = getFluid().getAmount() / totalNodes;
        int remFluid = getFluid().getAmount() % totalNodes;

        for (IFluidGrid grid : others) {
            int gridNodes = grid.getNodes().size();
            grid.fill(new FluidStack(getFluid(), (fluidPerNode * gridNodes)), EXECUTE);
        }
        // First grid gets the extra. Why? Because there's always a first grid.
        others.get(0).fill(new FluidStack(getFluid(), remFluid), EXECUTE);
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
            return tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir).isPresent();
        }
        return false;
        // return tile.getCapability(ThermalFluidHelper.getBaseFluidSystem()).isPresent();
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap) {

        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            if (!fluidCap.isPresent()) {
                fluidCap = LazyOptional.of(() -> storage);
            }
            return fluidCap.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public void refreshCapabilities() {

        fluidCap.invalidate();
    }

    // region IFluidGrid
    public int getCapacity() {

        return storage.getCapacity();
    }

    public FluidStack getFluid() {

        return storage.getFluid();
    }

    public void setCapacity(int capacity) {

        storage.setCapacity(capacity);
    }

    public void setFluid(FluidStack fluid) {

        storage.setFluid(fluid);
    }
    // endregion

    //@formatter:off
    @Override public int getTanks() { return storage.getTanks(); }
    @Override public FluidStack getFluidInTank(int tank) { return storage.getFluidInTank(tank); }
    @Override public int fill(FluidStack resource, FluidAction action) { return storage.fill(resource, action); }
    @Override public FluidStack drain(FluidStack resource, FluidAction action) { return storage.drain(resource, action); }
    @Override public FluidStack drain(int maxDrain, FluidAction action) { return storage.drain(maxDrain, action); }
    @Override public int getTankCapacity(int tank) { return storage.getTankCapacity(tank); }
    @Override public boolean isFluidValid(int tank, @Nonnull FluidStack stack) { return storage.isFluidValid(tank, stack); }
    //@formatter:on
}
