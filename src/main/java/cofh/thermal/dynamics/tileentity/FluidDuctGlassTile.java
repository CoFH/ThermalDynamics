package cofh.thermal.dynamics.tileentity;

import cofh.core.network.packet.client.TileStatePacket;
import cofh.core.util.helpers.RenderHelper;
import cofh.lib.tileentity.ITilePacketHandler;
import cofh.thermal.dynamics.api.internal.IUpdateableGridHostInternal;
import cofh.thermal.dynamics.grid.fluid.FluidGrid;
import cofh.thermal.dynamics.init.TDynReferences;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;

import static cofh.lib.util.constants.NBTTags.TAG_RENDER_FLUID;
import static cofh.thermal.dynamics.util.TDynConstants.BLANK_TEXTURE;

public class FluidDuctGlassTile extends FluidDuctTile implements IUpdateableGridHostInternal, ITilePacketHandler {

    FluidStack renderFluid = FluidStack.EMPTY;

    public FluidDuctGlassTile() {

        super(TDynReferences.FLUID_DUCT_GLASS_TILE);
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
        }
        return super.getModelData();
    }

    // region NBT
    @Override
    public CompoundNBT save(CompoundNBT tag) {

        if (!renderFluid.isEmpty()) {
            tag.put(TAG_RENDER_FLUID, renderFluid.writeToNBT(new CompoundNBT()));
        }
        return super.save(tag);
    }

    @Override
    public void load(BlockState state, CompoundNBT tag) {

        super.load(state, tag);

        renderFluid = FluidStack.loadFluidStackFromNBT(tag.getCompound(TAG_RENDER_FLUID));
    }
    // endregion

    // STATE
    @Override
    public PacketBuffer getStatePacket(PacketBuffer buffer) {

        renderFluid = ((FluidGrid) (getGrid().get())).getRenderFluid();
        buffer.writeFluidStack(renderFluid);

        return buffer;
    }

    @Override
    public void handleStatePacket(PacketBuffer buffer) {

        renderFluid = buffer.readFluidStack();

        requestModelDataUpdate();
    }
    // endregion
}
