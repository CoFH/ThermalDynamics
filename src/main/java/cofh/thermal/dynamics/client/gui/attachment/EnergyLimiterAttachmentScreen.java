package cofh.thermal.dynamics.client.gui.attachment;

import cofh.core.client.gui.ContainerScreenCoFH;
import cofh.core.client.gui.element.ElementBase;
import cofh.core.client.gui.element.ElementButton;
import cofh.core.client.gui.element.ElementTexture;
import cofh.core.client.gui.element.panel.RSControlPanel;
import cofh.core.util.helpers.GuiHelper;
import cofh.thermal.dynamics.attachment.EnergyLimiterAttachment;
import cofh.thermal.dynamics.inventory.container.attachment.EnergyLimiterAttachmentContainer;
import cofh.thermal.dynamics.network.packet.server.AttachmentConfigPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import static cofh.core.util.helpers.GuiHelper.*;
import static cofh.lib.util.constants.ModIds.ID_COFH_CORE;
import static cofh.lib.util.constants.ModIds.ID_THERMAL;
import static cofh.lib.util.helpers.SoundHelper.playClickSound;
import static cofh.lib.util.helpers.StringHelper.format;

public class EnergyLimiterAttachmentScreen extends ContainerScreenCoFH<EnergyLimiterAttachmentContainer> {

    public static final String TEX_PATH = ID_THERMAL + ":textures/gui/container/energy_limiter_attachment.png";
    public static final ResourceLocation TEXTURE = new ResourceLocation(TEX_PATH);

    public static final String TEX_INPUT = ID_THERMAL + ":textures/gui/elements/info_to_energy_grid.png";
    public static final String TEX_OUTPUT = ID_THERMAL + ":textures/gui/elements/info_from_energy_grid.png";

    public static final String TEX_INCREMENT = ID_COFH_CORE + ":textures/gui/elements/button_increment.png";
    public static final String TEX_DECREMENT = ID_COFH_CORE + ":textures/gui/elements/button_decrement.png";

    protected final EnergyLimiterAttachment attachment;

    public EnergyLimiterAttachmentScreen(EnergyLimiterAttachmentContainer container, Inventory inv, Component titleIn) {

        super(container, inv, titleIn);
        texture = TEXTURE;
        attachment = container.attachment;
        info = generatePanelInfo("info.thermal.energy_limiter_attachment");
    }

    @Override
    public void init() {

        super.init();

        addPanel(new RSControlPanel(this, attachment));

        addElement(new ElementTexture(this, 40, 16)
                .setSize(20, 20)
                .setTexture(TEX_INPUT, 20, 20));
        addElement(new ElementTexture(this, 116, 16)
                .setSize(20, 20)
                .setTexture(TEX_OUTPUT, 20, 20));

        addButtons();
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {

        String input = format(attachment.amountInput);
        String output = format(attachment.amountOutput);

        getFontRenderer().draw(matrixStack, input, getCenteredOffset(input, 50), 42, 0x404040);
        getFontRenderer().draw(matrixStack, output, getCenteredOffset(output, 126), 42, 0x404040);

        super.renderLabels(matrixStack, mouseX, mouseY);
    }

    // region ELEMENTS
    protected void addButtons() {

        ElementBase decInput = new ElementButton(this, 35, 56) {

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {

                int change = getChangeAmount(mouseButton);
                float pitch = getPitch(mouseButton);
                pitch -= 0.1F;
                playClickSound(pitch);

                int curInput = attachment.amountInput;
                attachment.amountInput -= change;
                AttachmentConfigPacket.sendToServer(attachment);
                attachment.amountInput = curInput;
                return true;
            }
        }
                .setTooltipFactory(GuiHelper::createDecControlTooltip)
                .setSize(14, 14)
                .setTexture(TEX_DECREMENT, 42, 14)
                .setEnabled(() -> attachment.amountInput > 0);

        ElementBase incInput = new ElementButton(this, 51, 56) {

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {

                int change = getChangeAmount(mouseButton);
                float pitch = getPitch(mouseButton);
                pitch += 0.1F;
                playClickSound(pitch);

                int curInput = attachment.amountInput;
                attachment.amountInput += change;
                AttachmentConfigPacket.sendToServer(attachment);
                attachment.amountInput = curInput;
                return true;
            }
        }
                .setTooltipFactory(GuiHelper::createIncControlTooltip)
                .setSize(14, 14)
                .setTexture(TEX_INCREMENT, 42, 14)
                .setEnabled(() -> attachment.amountInput < attachment.getMaxTransfer());

        ElementBase decOutput = new ElementButton(this, 111, 56) {

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {

                int change = getChangeAmount(mouseButton);
                float pitch = getPitch(mouseButton);
                pitch -= 0.1F;
                playClickSound(pitch);

                int curInput = attachment.amountOutput;
                attachment.amountOutput -= change;
                AttachmentConfigPacket.sendToServer(attachment);
                attachment.amountOutput = curInput;
                return true;
            }
        }
                .setTooltipFactory(GuiHelper::createDecControlTooltip)
                .setSize(14, 14)
                .setTexture(TEX_DECREMENT, 42, 14)
                .setEnabled(() -> attachment.amountOutput > 0);

        ElementBase incOutput = new ElementButton(this, 127, 56) {

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {

                int change = getChangeAmount(mouseButton);
                float pitch = getPitch(mouseButton);
                pitch += 0.1F;
                playClickSound(pitch);

                int curInput = attachment.amountOutput;
                attachment.amountOutput += change;
                AttachmentConfigPacket.sendToServer(attachment);
                attachment.amountOutput = curInput;
                return true;
            }
        }
                .setTooltipFactory(GuiHelper::createIncControlTooltip)
                .setSize(14, 14)
                .setTexture(TEX_INCREMENT, 42, 14)
                .setEnabled(() -> attachment.amountOutput < attachment.getMaxTransfer());

        addElement(decInput);
        addElement(incInput);
        addElement(decOutput);
        addElement(incOutput);
    }
    // endregion
}
