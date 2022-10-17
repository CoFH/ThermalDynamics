package cofh.thermal.dynamics.api.grid;

import cofh.thermal.dynamics.ThermalDynamics;
import cofh.thermal.dynamics.grid.Grid;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistryEntry;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.UUID;
import java.util.function.BiFunction;

/**
 * Represents a grid type.
 * <p>
 * This is a very abstract concept as a Grid type could mean anything
 * from Items to Fluids, Power, Other grids.
 * <p>
 * Grid types are intended to be static and used to infer what kind
 * of data/usages a grid may have. They are also used to detect what
 * blocks a grid is composed of when being built.
 * <p>
 *
 * @author covers1624
 */
public interface IGridType<G extends Grid<G, ?>> extends IForgeRegistryEntry<IGridType<?>> {

    /**
     * <strong>INTERNAL, do not call externally.</strong>
     * <p>
     * Factory method for constructing new instances of this grid type.
     * <p>
     * Called internally during grid construction.
     *
     * @param id    The ID of this grid.
     * @param world The world of this grid.
     * @return The new grid.
     */
    G createGrid(UUID id, Level world);

    /**
     * Static factory for creating simple Implementations of {@link IGridType}.
     *
     * @param gridFactory The Factory used to create new instances of this {@link Grid}.
     * @return The new {@link IGridType}.
     */
    static <G extends Grid<G, ?>> IGridType<G> of(BiFunction<UUID, Level, G> gridFactory) {

        abstract class GridTypeImpl<G2 extends Grid<G2, ?>> extends ForgeRegistryEntry<IGridType<?>> implements IGridType<G2> { }
        return new GridTypeImpl<>() {
            //@formatter:off
            @Override public G createGrid(UUID id, Level world) { return gridFactory.apply(id, world); }
            //@formatter:on

            @Override
            public String toString() {
                return getRegistryName().toString();
//                return ThermalDynamics.GRID_TYPE_REGISTRY.get().getKey(this).toString();
                // 1.19 ^^
            }
        };
    }

}
