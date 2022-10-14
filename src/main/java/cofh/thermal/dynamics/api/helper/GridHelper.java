package cofh.thermal.dynamics.api.helper;

import cofh.lib.util.helpers.BlockHelper;
import cofh.thermal.dynamics.api.TDynApi;
import cofh.thermal.dynamics.api.grid.IGrid;
import cofh.thermal.dynamics.api.grid.IGridHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Optional;

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
     * Returns an exclusive iterator which iterates all positions between {@code a} and {@code b}.
     */
    public static Iterable<BlockPos> positionsBetween(BlockPos a, BlockPos b) {

        assert isOnSameAxis(a, b);
        Direction dir = BlockHelper.getSide(b.subtract(a));
        assert dir != null : "Not on the same axis??";

        return () -> new Iterator<>() {
            BlockPos curr = a.relative(dir);

            @Override
            public boolean hasNext() {

                return !curr.equals(b);
            }

            @Override
            public BlockPos next() {

                BlockPos ret = curr;
                curr = curr.relative(dir);
                return ret;
            }
        };
    }

    /**
     * @return The number of blocks between the 2 positions. If a and b are adjacent, 0
     */
    public static int numBetween(BlockPos a, BlockPos b) {

        assert isOnSameAxis(a, b);

        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY()) + Math.abs(a.getZ() - b.getZ()) - 1;
    }

    /**
     * @return If {@code middle} is between {@code a} and {@code b} on the same axis.
     */
    public static boolean isOnEdge(BlockPos middle, BlockPos a, BlockPos b) {

        // Check if middle is between a and b.
        assert isOnSameAxis(a, b);
        if (a.getX() != b.getX())
            return middle.getY() == a.getY() && middle.getZ() == a.getZ() && betweenExclusive(middle.getX(), a.getX(), b.getX());
        if (a.getY() != b.getY())
            return middle.getZ() == a.getZ() && middle.getX() == a.getX() && betweenExclusive(middle.getY(), a.getY(), b.getY());
        if (a.getZ() != b.getZ())
            return middle.getX() == a.getX() && middle.getY() == a.getY() && betweenExclusive(middle.getZ(), a.getZ(), b.getZ());

        throw new IllegalArgumentException("A == B");
    }

    private static boolean betweenExclusive(int v, int a, int b) {

        return a < b ? a < v && v < b : b < v && v < a;
    }

    public static boolean isOnSameAxis(BlockPos a, BlockPos b) {

        boolean x = a.getX() == b.getX();
        boolean y = a.getY() == b.getY();
        boolean z = a.getZ() == b.getZ();
        if (x && y) return true; // Z axis or same block
        if (x && z) return true; // Y axis
        if (y && z) return true; // X axis
        return false; // no axis
    }

    public static BlockPos stepTowards(BlockPos from, BlockPos towards) {

        assert isOnSameAxis(from, towards) : "Not on the same axis";
        Direction dir = BlockHelper.getSide(towards.subtract(from));
        assert dir != null : "Not on the same axis??";

        return from.relative(dir);
    }

}
