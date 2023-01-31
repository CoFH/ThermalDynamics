package cofh.thermal.dynamics.client.gui.attachment;

import cofh.core.client.gui.ContainerScreenCoFH;
import cofh.core.client.gui.element.*;
import cofh.core.client.gui.element.panel.RSControlPanel;
import cofh.core.util.helpers.GuiHelper;
import cofh.thermal.dynamics.attachment.FluidTurboServoAttachment;
import cofh.thermal.dynamics.inventory.container.attachment.FluidTurboServoAttachmentContainer;
import cofh.thermal.dynamics.network.packet.server.AttachmentConfigPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import static cofh.core.util.helpers.GuiHelper.*;
import static cofh.lib.util.Constants.PATH_GUI;
import static cofh.lib.util.constants.ModIds.ID_COFH_CORE;
import static cofh.lib.util.constants.ModIds.ID_THERMAL;
import static cofh.lib.util.helpers.SoundHelper.playClickSound;
import static cofh.lib.util.helpers.StringHelper.format;

public class FluidTurboServoAttachmentScreen extends ContainerScreenCoFH<FluidTurboServoAttachmentContainer> {

    public static final ResourceLocation TEXTURE = new ResourceLocation(PATH_GUI + "generic.png");

    public static final String TEX_EXTRACT = ID_THERMAL + ":textures/gui/elements/info_extract_to_fluid_grid.png";

    public static final String TEX_INCREMENT = ID_COFH_CORE + ":textures/gui/elements/button_increment.png";
    public static final String TEX_DECREMENT = ID_COFH_CORE + ":textures/gui/elements/button_decrement.png";

    public static final String TEX_DENY_LIST = PATH_GUI + "filters/filter_deny_list.png";
    public static final String TEX_ALLOW_LIST = PATH_GUI + "filters/filter_allow_list.png";
    public static final String TEX_IGNORE_NBT = PATH_GUI + "filters/filter_ignore_nbt.png";
    public static final String TEX_USE_NBT = PATH_GUI + "filters/filter_use_nbt.png";

    protected final FluidTurboServoAttachment attachment;

    public FluidTurboServoAttachmentScreen(FluidTurboServoAttachmentContainer container, Inventory inv, Component titleIn) {

        super(container, inv, titleIn);

        texture = TEXTURE;
        attachment = container.attachment;
        info = generatePanelInfo("info.thermal.fluid_turbo_servo_attachment");
    }

    @Override
    public void init() {

        super.init();

        addPanel(new RSControlPanel(this, attachment));

        for (int i = 0; i < menu.getFilterSize(); ++i) {
            Slot slot = menu.slots.get(i);
            addElement(createSlot(this, slot.x, slot.y));

            final int j = i;
            addElement(new ElementFluid(this, slot.x, slot.y).setFluid(() -> menu.getFilterStacks().get(j)).setSize(16, 16));
        }
        addElement(new ElementTexture(this, 141, 16)
                .setSize(20, 20)
                .setTexture(TEX_EXTRACT, 20, 20));

        addButtons();
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {

        String output = format(attachment.amountTransfer);

        fontRenderer().draw(matrixStack, output, getCenteredOffset(output, 151), 42, 0x404040);

        super.renderLabels(matrixStack, mouseX, mouseY);
    }

    // region ELEMENTS
    protected void addButtons() {

        addElement(new ElementButton(this, 105, 22) {

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {

                menu.setAllowList(true);
                playClickSound(0.7F);
                return true;
            }
        }
                .setSize(20, 20)
                .setTexture(TEX_DENY_LIST, 40, 20)
                .setTooltipFactory(new SimpleTooltip(Component.translatable("info.cofh.filter.allowlist.0")))
                .setVisible(() -> !menu.getAllowList()));

        addElement(new ElementButton(this, 105, 22) {

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {

                menu.setAllowList(false);
                playClickSound(0.4F);
                return true;
            }
        }
                .setSize(20, 20)
                .setTexture(TEX_ALLOW_LIST, 40, 20)
                .setTooltipFactory(new SimpleTooltip(Component.translatable("info.cofh.filter.allowlist.1")))
                .setVisible(() -> menu.getAllowList()));

        addElement(new ElementButton(this, 105, 44) {

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {

                menu.setCheckNBT(true);
                playClickSound(0.7F);
                return true;
            }
        }
                .setSize(20, 20)
                .setTexture(TEX_IGNORE_NBT, 40, 20)
                .setTooltipFactory(new SimpleTooltip(Component.translatable("info.cofh.filter.checkNBT.0")))
                .setVisible(() -> !menu.getCheckNBT()));

        addElement(new ElementButton(this, 105, 44) {

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {

                menu.setCheckNBT(false);
                playClickSound(0.4F);
                return true;
            }
        }
                .setSize(20, 20)
                .setTexture(TEX_USE_NBT, 40, 20)
                .setTooltipFactory(new SimpleTooltip(Component.translatable("info.cofh.filter.checkNBT.1")))
                .setVisible(() -> menu.getCheckNBT()));

        ElementBase decTransfer = new ElementButton(this, 136, 56) {

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {

                int change = getChangeAmount(mouseButton);
                float pitch = getPitch(mouseButton);
                pitch -= 0.1F;
                playClickSound(pitch);

                int curTransfer = attachment.amountTransfer;
                attachment.amountTransfer -= change;
                AttachmentConfigPacket.sendToServer(attachment);
                attachment.amountTransfer = curTransfer;
                return true;
            }
        }
                .setTooltipFactory(GuiHelper::createDecControlTooltip)
                .setSize(14, 14)
                .setTexture(TEX_DECREMENT, 42, 14)
                .setEnabled(() -> attachment.amountTransfer > 0);

        ElementBase incTransfer = new ElementButton(this, 152, 56) {

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {

                int change = getChangeAmount(mouseButton);
                float pitch = getPitch(mouseButton);
                pitch += 0.1F;
                playClickSound(pitch);

                int curTransfer = attachment.amountTransfer;
                attachment.amountTransfer += change;
                AttachmentConfigPacket.sendToServer(attachment);
                attachment.amountTransfer = curTransfer;
                return true;
            }
        }
                .setTooltipFactory(GuiHelper::createIncControlTooltip)
                .setSize(14, 14)
                .setTexture(TEX_INCREMENT, 42, 14)
                .setEnabled(() -> attachment.amountTransfer < attachment.getMaxTransfer());

        addElement(decTransfer);
        addElement(incTransfer);
    }
    // endregion
}