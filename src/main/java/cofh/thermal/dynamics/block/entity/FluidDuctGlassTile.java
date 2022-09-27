package cofh.thermal.dynamics.block.entity;

import cofh.core.network.packet.client.TileStatePacket;
import cofh.core.util.helpers.FluidHelper;
import cofh.core.util.helpers.RenderHelper;
import cofh.lib.api.block.entity.IPacketHandlerTile;
import cofh.thermal.dynamics.api.grid.IGridHostUpdateable;
import cofh.thermal.dynamics.grid.fluid.FluidGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static cofh.lib.util.constants.NBTTags.TAG_RENDER_FLUID;
import static cofh.thermal.dynamics.init.TDynTileEntities.DUCT_FLUID_GLASS_TILE;
import static cofh.thermal.dynamics.util.TDynConstants.BLANK_TEXTURE;

public class FluidDuctGlassTile extends FluidDuctTile implements IGridHostUpdateable, IPacketHandlerTile {

    FluidStack renderFluid = FluidStack.EMPTY;

    public FluidDuctGlassTile(BlockPos pos, BlockState state) {

        super(DUCT_FLUID_GLASS_TILE.get(), pos, state);
    }

    @Override
    public void update() {

        TileStatePacket.sendToClient(this);
    }

    @Nonnull
    @Override
    public IModelData getModelData() {

        if (modelUpdate) {
            modelData.setFill(renderFluid.isEmpty() ? BLANK_TEXTURE : RenderHelper.getFluidTexture(renderFluid).getName());
            modelData.setFillColor(FluidHelper.color(renderFluid));
        }
        return super.getModelData();
    }

    // region NBT
    @Override
    public void saveAdditional(CompoundTag tag) {

        if (!renderFluid.isEmpty()) {
            tag.put(TAG_RENDER_FLUID, renderFluid.writeToNBT(new CompoundTag()));
        }
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {

        super.load(tag);

        renderFluid = FluidStack.loadFluidStackFromNBT(tag.getCompound(TAG_RENDER_FLUID));
    }
    // endregion

    // region NETWORK
    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {

        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {

        return saveWithoutMetadata();
    }

    // STATE
    @Override
    public FriendlyByteBuf getStatePacket(FriendlyByteBuf buffer) {

        renderFluid = ((FluidGrid) getGrid()).getRenderFluid();
        buffer.writeFluidStack(renderFluid);

        return buffer;
    }

    @Override
    public void handleStatePacket(FriendlyByteBuf buffer) {

        renderFluid = buffer.readFluidStack();

        requestModelDataUpdate();
    }
    // endregion
}
