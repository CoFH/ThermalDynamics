package cofh.thermal.dynamics.api.grid;

import cofh.thermal.dynamics.api.TDynApi;
import cofh.thermal.dynamics.api.grid.multi.IMultiGrid;
import cofh.thermal.dynamics.api.grid.multi.IMultiGridNode;
import cofh.thermal.dynamics.init.TDynReferences;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

import java.util.Optional;
import java.util.Set;

/**
 * Represents an object capable of hosting a {@link IGrid}.
 * <p>
 * These are usually {@link TileEntity} instances.
 * <p>
 * This interface may be implemented directly on the {@link TileEntity},
 * or exposed via a {@link Capability}.
 *
 * @author covers1624
 * @see TDynApi#GRID_HOST_CAPABILITY
 */
public interface IGridHost {

    World getHostWorld();

    BlockPos getHostPos();

    /**
     * Gets the raw {@link IGrid} hosted by this {@link IGridHost}.
     *
     * @return The raw grid.
     */
    Optional<IGrid<?, ?>> getGrid();

    void setGrid(IGrid<?, ?> grid);

    Set<IGridType<?>> getExposedTypes();

    /**
     * Gets the {@link IGridNode} hosted by this grid host.
     *
     * @return The {@link IGridNode}.
     */
    default Optional<IGridNode<?>> getNode() {

        Optional<IGrid<?, ?>> gridOpt = getGrid();
        return gridOpt.map(grid -> grid.getNodes().get(getHostPos()));
    }

    /**
     * Tries to get the {@link IGridNode} hosted by this {@link IGridHost} of the given type.
     * <p>
     * If the hosted grid is a {@link IMultiGrid}, the {@link IMultiGridNode} will be queried
     * for a grid of the given type.
     *
     * @param gridType The {@link IGridType}
     * @return The {@link IGridNode}
     */
    default <G extends IGrid<?, ?>> Optional<IGridNode<G>> getNode(IGridType<G> gridType) {

        Optional<IGridNode<?>> nodeOpt = getNode();
        if (!nodeOpt.isPresent()) return Optional.empty();
        IGridNode<?> node = nodeOpt.get();
        if (node.getGrid().getGridType() == TDynReferences.MULTI_GRID) {
            IMultiGridNode multiGridNode = (IMultiGridNode) node;
            return multiGridNode.getSubGrid(gridType);
        }
        if (node.getGrid().getGridType() != gridType) return Optional.empty();

        //noinspection unchecked
        return Optional.of((IGridNode<G>) node);
    }

}
