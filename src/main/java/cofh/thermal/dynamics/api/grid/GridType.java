package cofh.thermal.dynamics.api.grid;

import cofh.thermal.dynamics.api.grid.item.ItemGrid;
import net.minecraft.world.World;
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
public interface GridType<G extends Grid<?, ?>> extends IForgeRegistryEntry<GridType<?>> {

    /**
     * The {@link Grid} class that represents this {@link GridType}.
     *
     * @return The grid class.
     */
    Class<G> getGridType();

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
    G createGrid(UUID id, World world);

    /**
     * Static factory for creating simple Implementations of {@link GridType}.
     *
     * @param clazz       The High level interface that represents this grid. Such as {@link ItemGrid}.
     * @param gridFactory The Factory used to create new instances of this {@link Grid}.
     * @return The new {@link GridType}.
     */
    static <G extends Grid<?, ?>> GridType<G> of(Class<G> clazz, BiFunction<UUID, World, G> gridFactory) {

        abstract class GridTypeImpl<G2 extends Grid<?, ?>> extends ForgeRegistryEntry<GridType<?>> implements GridType<G2> {}
        return new GridTypeImpl<G>() {
            //@formatter:off
            @Override public Class<G> getGridType() { return clazz; }
            @Override public G createGrid(UUID id, World world) { return gridFactory.apply(id, world); }
            //@formatter:on
        };
    }

}
