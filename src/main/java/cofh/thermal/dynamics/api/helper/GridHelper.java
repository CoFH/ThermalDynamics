package cofh.thermal.dynamics.api.helper;

import cofh.thermal.dynamics.api.TDynApi;
import cofh.thermal.dynamics.api.grid.IGrid;
import cofh.thermal.dynamics.api.grid.IGridHost;
import cofh.thermal.dynamics.api.grid.IGridNode;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
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
     * given {@link BlockGetter}.
     *
     * @param reader The level.
     * @param pos    The {@link BlockPos}.
     * @return Optionally the {@link IGridHost}.
     */
    public static Optional<IGridHost> getGridHost(BlockGetter reader, BlockPos pos) {

        if (reader instanceof LevelReader worldReader) {
            if (!worldReader.hasChunkAt(pos)) return Optional.empty();
        }
        return getGridHost(reader.getBlockEntity(pos));
    }

    /**
     * Optionally get a {@link IGridHost} from the given {@link BlockEntity}.
     *
     * @param tile The {@link BlockEntity}.
     * @return Optionally the {@link IGridHost}.
     */
    public static Optional<IGridHost> getGridHost(@Nullable BlockEntity tile) {

        if (tile == null) {
            return Optional.empty();
        }
        if (tile instanceof IGridHost) {
            return Optional.of((IGridHost) tile);
        }
        return tile.getCapability(TDynApi.GRID_HOST_CAPABILITY).resolve();
    }

    /**
     * Locate all {@link IGridNode GridNodes} attached to the given {@link BlockPos}.
     *
     * @param world  The {@link Level} to search in.
     * @param start  The {@link BlockPos} to start scanning from.
     * @param from   The {@link BlockPos} to ignore. Usually the adjacent block which is performing this check.
     * @param origin Found grid hosts must be connectable to this host.
     * @return The attached {@link IGridNode GridNodes} and the {@link BlockPos positions} between <code>start</code>
     * and the found {@link IGridNode}.
     */
    public static List<Pair<IGridNode<?>, Set<BlockPos>>> locateAttachedNodes(Level world, BlockPos start, BlockPos from, IGridHost origin) {

        Set<BlockPos> visited = new HashSet<>();
        LinkedList<IGridHost> candidates = new LinkedList<>();
        visited.add(start);
        visited.add(from);
        addCandidates(world, start, origin, visited, candidates);
        ImmutableList.Builder<Pair<IGridNode<?>, Set<BlockPos>>> builder = ImmutableList.builder();
        while (!candidates.isEmpty()) {
            IGridHost host = candidates.pop();

            IGridNode<?> node = host.getNode();
            if (node != null) {
                builder.add(Pair.of(node, getPositionsBetween(start, host.getHostPos())));
            } else {
                addCandidates(world, host.getHostPos(), host, visited, candidates);
            }
        }
        return builder.build();
    }

    private static void addCandidates(Level world, BlockPos pos, IGridHost origin, Set<BlockPos> visited, LinkedList<IGridHost> candidates) {

        for (Direction dir : Direction.values()) {
            BlockPos adj = pos.relative(dir);
            if (!visited.add(adj)) continue;
            GridHelper.getGridHost(world, adj)
                    .filter(other -> origin.canConnectTo(other, dir) && other.canConnectTo(origin, dir.getOpposite()))
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
    // TODO Move to Core, somewhere.
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
