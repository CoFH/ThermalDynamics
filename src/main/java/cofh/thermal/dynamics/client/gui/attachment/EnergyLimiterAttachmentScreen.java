package cofh.thermal.dynamics.client.gui.attachment;

import cofh.core.client.gui.ContainerScreenCoFH;
import cofh.core.client.gui.element.ElementBase;
import cofh.core.client.gui.element.ElementButton;
import cofh.core.client.gui.element.ElementTexture;
import cofh.core.client.gui.element.panel.RSControlPanel;
import cofh.thermal.dynamics.attachment.EnergyLimiterAttachment;
import cofh.thermal.dynamics.inventory.container.attachment.EnergyLimiterAttachmentContainer;
import cofh.thermal.dynamics.network.packet.server.AttachmentConfigPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Collections;

import static cofh.core.util.helpers.GuiHelper.generatePanelInfo;
import static cofh.lib.util.constants.ModIds.ID_COFH_CORE;
import static cofh.lib.util.constants.ModIds.ID_THERMAL;
import static cofh.lib.util.helpers.SoundHelper.playClickSound;
import static cofh.lib.util.helpers.StringHelper.format;
import static cofh.lib.util.helpers.StringHelper.localize;

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
    @Override
    public boolean handleElementButtonClick(String buttonName, int mouseButton) {

        int change = 1000;
        float pitch = 0.7F;

        if (hasShiftDown()) {
            change *= 10;
            pitch += 0.1F;
        }
        if (hasControlDown()) {
            change /= 100;
            pitch -= 0.2F;
        }
        if (mouseButton == 1) {
            change /= 10;
            pitch -= 0.1F;
        }
        int curInput = attachment.amountInput;
        int curOutput = attachment.amountOutput;

        switch (buttonName) {
            case "DecInput" -> {
                attachment.amountInput -= change;
                pitch -= 0.1F;
            }
            case "IncInput" -> {
                attachment.amountInput += change;
                pitch += 0.1F;
            }
            case "DecOutput" -> {
                attachment.amountOutput -= change;
                pitch -= 0.1F;
            }
            case "IncOutput" -> {
                attachment.amountOutput += change;
                pitch += 0.1F;
            }
        }
        playClickSound(pitch);

        AttachmentConfigPacket.sendToServer(attachment);

        attachment.amountInput = curInput;
        attachment.amountOutput = curOutput;
        return true;
    }

    protected void addButtons() {

        ElementBase decInput = new ElementButton(this, 35, 56)
                .setTooltipFactory((element, mouseX, mouseY) -> {

                    if (element.enabled()) {
                        int change = 1000;

                        if (hasShiftDown()) {
                            change *= 10;
                        }
                        if (hasControlDown()) {
                            change /= 100;
                        }
                        return Collections.singletonList(new TextComponent(
                                localize("info.cofh.decrease_by")
                                        + " " + format(change)
                                        + "/" + format(change / 10)));
                    }
                    return Collections.emptyList();
                })
                .setName("DecInput")
                .setSize(14, 14)
                .setTexture(TEX_DECREMENT, 42, 14)
                .setEnabled(() -> attachment.amountInput > 0);

        ElementBase incInput = new ElementButton(this, 51, 56)
                .setTooltipFactory((element, mouseX, mouseY) -> {

                    if (element.enabled()) {
                        int change = 1000;

                        if (hasShiftDown()) {
                            change *= 10;
                        }
                        if (hasControlDown()) {
                            change /= 100;
                        }
                        return Collections.singletonList(new TextComponent(
                                localize("info.cofh.increase_by")
                                        + " " + format(change)
                                        + "/" + format(change / 10)));
                    }
                    return Collections.emptyList();
                })
                .setName("IncInput")
                .setSize(14, 14)
                .setTexture(TEX_INCREMENT, 42, 14)
                .setEnabled(() -> attachment.amountInput < attachment.getMaxTransfer());

        ElementBase decOutput = new ElementButton(this, 111, 56)
                .setTooltipFactory((element, mouseX, mouseY) -> {

                    if (element.enabled()) {
                        int change = 1000;

                        if (hasShiftDown()) {
                            change *= 10;
                        }
                        if (hasControlDown()) {
                            change /= 100;
                        }
                        return Collections.singletonList(new TextComponent(
                                localize("info.cofh.decrease_by")
                                        + " " + format(change)
                                        + "/" + format(change / 10)));
                    }
                    return Collections.emptyList();
                })
                .setName("DecOutput")
                .setSize(14, 14)
                .setTexture(TEX_DECREMENT, 42, 14)
                .setEnabled(() -> attachment.amountOutput > 0);

        ElementBase incOutput = new ElementButton(this, 127, 56)
                .setTooltipFactory((element, mouseX, mouseY) -> {

                    if (element.enabled()) {
                        int change = 1000;

                        if (hasShiftDown()) {
                            change *= 10;
                        }
                        if (hasControlDown()) {
                            change /= 100;
                        }
                        return Collections.singletonList(new TextComponent(
                                localize("info.cofh.increase_by")
                                        + " " + format(change)
                                        + "/" + format(change / 10)));
                    }
                    return Collections.emptyList();
                })
                .setName("IncOutput")
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
