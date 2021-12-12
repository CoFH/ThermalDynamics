package cofh.thermal.dynamics.grid;

import cofh.thermal.dynamics.api.grid.Grid;
import cofh.thermal.dynamics.api.grid.GridNode;
import cofh.thermal.dynamics.api.grid.node.NodeAttachment;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.EnumMap;
import java.util.EnumSet;

/**
 * @author covers1624
 */
public abstract class AbstractGridNode<G extends Grid<?, ?>> implements GridNode<G>, INBTSerializable<CompoundNBT> {

    private final EnumMap<Direction, NodeAttachment> attachments = new EnumMap<>(Direction.class);
    private final EnumSet<Direction> internalConnections = EnumSet.noneOf(Direction.class);
    private final EnumSet<Direction> externalConnections = EnumSet.noneOf(Direction.class);
    private G grid;
    private BlockPos pos = BlockPos.ZERO;
    private boolean loaded;

    protected AbstractGridNode(G grid) {
        this.grid = grid;
    }

    @Deprecated
    protected abstract boolean isExternallyConnectable(Direction side);

    @Override
    public CompoundNBT serializeNBT() {
        return new CompoundNBT();
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
    }

    public void onGridChange(G oldGrid) {
    }

    //@formatter:off
    @Override public final G getGrid() { return grid; }
    public final World getWorld() { return grid.getWorld(); }
    @Override public final BlockPos getPos() { return pos; }
    public boolean isLoaded() { return loaded; }
    @Override public EnumSet<Direction> getInternalConnections() { return internalConnections; }
    @Override public EnumSet<Direction> getExternalConnections() { return externalConnections; }
    @Override public EnumMap<Direction, NodeAttachment> getAttachments() { return attachments; }
    public void setPos(BlockPos pos) { this.pos = pos; }
    public void setGrid(G grid) { this.grid = grid; }
    public void setLoaded(boolean loaded) { this.loaded = loaded; }
    //@formatter:on
}
