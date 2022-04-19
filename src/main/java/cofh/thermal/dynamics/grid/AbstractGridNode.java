package cofh.thermal.dynamics.grid;

import cofh.thermal.dynamics.api.grid.IGrid;
import cofh.thermal.dynamics.api.grid.IGridNode;
import cofh.thermal.dynamics.api.grid.node.INodeAttachment;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.EnumMap;
import java.util.EnumSet;

/**
 * Represents the base implementation of a {@link IGridNode}.
 * <p>
 * All {@link IGridNode} instances must extend from this.
 *
 * @author covers1624
 */
public abstract class AbstractGridNode<G extends IGrid<?, ?>> implements IGridNode<G>, INBTSerializable<CompoundNBT> {

    protected final EnumMap<Direction, INodeAttachment> attachments = new EnumMap<>(Direction.class);
    protected final EnumSet<Direction> connections = EnumSet.noneOf(Direction.class);
    protected G grid;
    protected BlockPos pos = BlockPos.ZERO;
    protected boolean loaded;
    protected boolean cached;

    protected AbstractGridNode(G grid) {

        this.grid = grid;
    }

    @Override
    public CompoundNBT serializeNBT() {

        return new CompoundNBT();
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {

    }

    public void onGridChange(G oldGrid) {

    }

    public final void clearConnections() {

        connections.clear();
        cached = false;
    }

    //@formatter:off
    @Override public final G getGrid() { return grid; }
    public final World getWorld() { return grid.getWorld(); }
    @Override public final BlockPos getPos() { return pos; }
    public boolean isLoaded() { return loaded; }
    @Override public EnumSet<Direction> getConnections() { return connections; }
    @Override public EnumMap<Direction, INodeAttachment> getAttachments() { return attachments; }
    public void setPos(BlockPos pos) { this.pos = pos; }
    public void setGrid(G grid) { this.grid = grid; }
    public void setLoaded(boolean loaded) { this.loaded = loaded; }
    //@formatter:on
}
