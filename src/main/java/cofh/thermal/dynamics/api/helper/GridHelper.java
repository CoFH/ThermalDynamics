package cofh.thermal.dynamics.api.helper;

import cofh.thermal.dynamics.api.TDynApi;
import cofh.thermal.dynamics.api.grid.IGrid;
import cofh.thermal.dynamics.api.grid.IGridHost;
import cofh.thermal.dynamics.api.grid.IGridNode;
import com.google.common.collect.ImmutableList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Contains helper methods for interacting with {@link IGrid Grids} in world.
 *
 * @author covers1624
 */
public class GridHelper {

    private GridHelper() {

    }

    /**
     * Optionally get a {@link IGridHost} at the given {@link BlockPos} within the
     * given {@link IBlockReader}.
     *
     * @param reader The level.
     * @param pos    The {@link BlockPos}.
     * @return Optionally the {@link IGridHost}.
     */
    public static Optional<IGridHost> getGridHost(IBlockReader reader, BlockPos pos) {

        if (reader instanceof IWorldReader) {
            IWorldReader worldReader = (IWorldReader) reader;
            if (!worldReader.hasChunkAt(pos)) return Optional.empty();
        }
        return getGridHost(reader.getBlockEntity(pos));
    }

    /**
     * Optionally get a {@link IGridHost} from the given {@link TileEntity}.
     *
     * @param tile The {@link TileEntity}.
     * @return Optionally the {@link IGridHost}.
     */
    public static Optional<IGridHost> getGridHost(@Nullable TileEntity tile) {

        if (tile == null) return Optional.empty();

        if (tile instanceof IGridHost) {
            return Optional.of((IGridHost) tile);
        }
        return tile.getCapability(TDynApi.GRID_HOST_CAPABILITY).resolve();
    }

    /**
     * Locate all {@link IGridNode GridNodes} attached to the given {@link BlockPos}.
     *
     * @param world The {@link World} to search in.
     * @param start The {@link BlockPos} to start scanning from.
     * @param from  The {@link BlockPos} to ignore. Usually the adjacent block which is performing this check.
     * @return The attached {@link IGridNode GridNodes} and the {@link BlockPos positions} between <code>start</code>
     * and the found {@link IGridNode}.
     */
    public static List<Pair<IGridNode<?>, Set<BlockPos>>> locateAttachedNodes(World world, BlockPos start, BlockPos from) {

        Set<BlockPos> visited = new HashSet<>();
        LinkedList<IGridHost> candidates = new LinkedList<>();
        visited.add(start);
        visited.add(from);
        addCandidates(world, start, visited, candidates);
        ImmutableList.Builder<Pair<IGridNode<?>, Set<BlockPos>>> builder = ImmutableList.builder();
        while (!candidates.isEmpty()) {
            IGridHost host = candidates.pop();
            Optional<IGridNode<?>> nodeOpt = host.getNode();
            if (nodeOpt.isPresent()) {
                builder.add(Pair.of(nodeOpt.get(), getPositionsBetween(start, host.getHostPos())));
            } else {
                addCandidates(world, host.getHostPos(), visited, candidates);
            }
        }
        return builder.build();
    }

    private static void addCandidates(World world, BlockPos pos, Set<BlockPos> visited, LinkedList<IGridHost> candidates) {

        for (Direction dir : Direction.values()) {
            BlockPos adj = pos.relative(dir);
            if (!visited.add(adj)) continue;
            GridHelper.getGridHost(world, adj)
                    .ifPresent(candidates::add);
        }
    }

    /**
     * Gets all BlockPositions between <code>a</code> and <code>b</code>
     * excluding <code>a</code> and <code>b</code>.
     *
     * @param a The first position.
     * @param b The second position.
     * @return The contained blocks.
     */
    // TODO More to Core, somewhere.
    public static Set<BlockPos> getPositionsBetween(BlockPos a, BlockPos b) {

        Set<BlockPos> positions = new HashSet<>();
        for (BlockPos pos : BlockPos.betweenClosed(a, b)) {
            positions.add(pos.immutable());
        }
        positions.remove(a);
        positions.remove(b);

        return positions;
    }
}
