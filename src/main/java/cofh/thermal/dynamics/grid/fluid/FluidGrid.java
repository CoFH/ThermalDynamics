package cofh.thermal.dynamics.grid.fluid;

import cofh.core.util.helpers.FluidHelper;
import cofh.lib.util.TimeTracker;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.grid.Grid;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

import static cofh.lib.util.Constants.BUCKET_VOLUME;
import static cofh.lib.util.Constants.TANK_MEDIUM;
import static cofh.thermal.dynamics.init.TDynGrids.FLUID_GRID;

/**
 * @author King Lemming
 */
public class FluidGrid extends Grid<FluidGrid, FluidGridNode> implements IFluidHandler {

    protected static final int NODE_CAPACITY = 100;

    protected final GridFluidStorage storage = new GridFluidStorage(NODE_CAPACITY);
    protected LazyOptional<?> fluidCap = LazyOptional.empty();

    protected FluidStack renderFluid = FluidStack.EMPTY;
    protected FluidStack prevRenderFluid = FluidStack.EMPTY;
    protected TimeTracker timeTracker = new TimeTracker();
    protected boolean wasFilled;
    protected boolean needsUpdate;

    protected FluidGridNode[] distArray = new FluidGridNode[0];
    protected int distIndex = 0;

    public FluidGrid(UUID id, Level world) {

        super(FLUID_GRID.get(), id, world);
    }

    @Override
    public FluidGridNode newNode() {

        return new FluidGridNode(this);
    }

    @Override
    public void tick() {

        storage.tick();

        if (distArray.length != getNodes().size()) {
            distArray = getNodes().values().toArray(new FluidGridNode[0]);
        }
        int curIndex = distIndex;

        if (distIndex >= distArray.length) {
            distIndex = 0;
        }
        for (int i = distIndex; i < distArray.length; ++i) {
            rrPreNodeTick(i);
        }
        for (int i = 0; i < distIndex; ++i) {
            rrPreNodeTick(i);
        }
        renderUpdate();

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

    private void rrPreNodeTick(int i) {

        if (distArray[i].isLoaded()) {
            distArray[i].attachmentTick();
        }
    }

    private boolean rrNodeTick(int curIndex, int i) {

        if (!distArray[i].isLoaded()) {
            return false;
        }
        distArray[i].distributionTick();
        if (getFluid().isEmpty()) {
            distIndex = i + 1;
            if (curIndex == distIndex) {
                --distIndex;
            }
            return true;
        }
        return false;
    }

    private void renderUpdate() {

        prevRenderFluid = renderFluid;
        renderFluid = new FluidStack(getFluid(), BUCKET_VOLUME);

        if (!FluidHelper.fluidsEqual(prevRenderFluid, renderFluid) || wasFilled && timeTracker.hasDelayPassed(world, 40) || needsUpdate) {
            if (!wasFilled && renderFluid.isEmpty()) {
                timeTracker.markTime(world);
                wasFilled = true;
                return;
            }
            updateHosts();
            wasFilled = false;
            needsUpdate = false;
        }
    }

    @Override
    public void onModified() {

        distArray = new FluidGridNode[0];
        storage.setBaseCapacity(Math.max(TANK_MEDIUM, getNodes().size() * NODE_CAPACITY));
        super.onModified();
    }

    @Override
    public void onMerge(FluidGrid from) {

        storage.setBaseCapacity(Math.max(TANK_MEDIUM, getNodes().size() * NODE_CAPACITY));
        storage.setCapacity(this.getCapacity() + from.getCapacity());
        storage.setFluid(new FluidStack(storage.getFluid(), this.getFluidAmount() + from.getFluidAmount()));

        needsUpdate = true;

        refreshCapabilities();
        from.refreshCapabilities();
    }

    @Override
    public void onSplit(List<FluidGrid> others) {

        int totalNodes = 0;
        for (FluidGrid grid : others) {
            int gridNodes = grid.getNodes().size();
            totalNodes += grid.getNodes().size();
            grid.setBaseCapacity(Math.max(TANK_MEDIUM, gridNodes * NODE_CAPACITY));
            grid.setCapacity(this.getCapacity());
            if (!this.renderFluid.isEmpty()) {
                grid.needsUpdate = true;
            }
            grid.refreshCapabilities();
        }
        this.refreshCapabilities();
        if (getFluid().isEmpty()) {
            return;
        }
        int fluidPerNode = getFluid().getAmount() / totalNodes;
        int remFluid = getFluid().getAmount() % totalNodes;

        for (FluidGrid grid : others) {
            int gridNodes = grid.getNodes().size();
            grid.setFluid(new FluidStack(getFluid(), (fluidPerNode * gridNodes)));
        }
        // First grid gets the extra. Why? Because there's always a first grid.
        others.get(0).setFluid(new FluidStack(getFluid(), others.get(0).getFluid().getAmount() + remFluid));
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
            return tile.getCapability(ForgeCapabilities.FLUID_HANDLER, dir).isPresent();
        }
        return false;
        // return tile.getCapability(ForgeCapabilities.FLUID_HANDLER).isPresent();
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap) {

        if (cap == ForgeCapabilities.FLUID_HANDLER) {
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

    //@formatter:off
    public int getCapacity() { return storage.getCapacity(); }
    public FluidStack getFluid() { return storage.getFluid(); }
    public FluidStack getRenderFluid() { return renderFluid; }
    public int getFluidAmount() { return storage.getFluid().getAmount(); }
    public void setBaseCapacity(int baseCapacity) { storage.setBaseCapacity(baseCapacity); }
    public void setCapacity(int capacity) { storage.setCapacity(capacity); }
    public void setFluid(FluidStack fluid) { storage.setFluid(fluid); }

    @Override public int getTanks() { return storage.getTanks(); }
    @Override public FluidStack getFluidInTank(int tank) { return storage.getFluidInTank(tank); }
    @Override public int fill(FluidStack resource, FluidAction action) { return storage.fill(resource, action); }
    @Override public FluidStack drain(FluidStack resource, FluidAction action) { return storage.drain(resource, action); }
    @Override public FluidStack drain(int maxDrain, FluidAction action) { return storage.drain(maxDrain, action); }
    @Override public int getTankCapacity(int tank) { return storage.getTankCapacity(tank); }
    @Override public boolean isFluidValid(int tank, @Nonnull FluidStack stack) { return storage.isFluidValid(tank, stack); }
    //@formatter:on
}
