package cofh.thermal.dynamics.client.gui.attachment;

import cofh.core.client.gui.ContainerScreenCoFH;
import cofh.core.client.gui.element.ElementButton;
import cofh.core.client.gui.element.ElementFluid;
import cofh.core.client.gui.element.SimpleTooltip;
import cofh.core.client.gui.element.panel.RSControlPanel;
import cofh.thermal.dynamics.attachment.FluidFilterAttachment;
import cofh.thermal.dynamics.attachment.FluidFilterAttachment.FilterMode;
import cofh.thermal.dynamics.inventory.container.attachment.FluidFilterAttachmentContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import static cofh.core.util.helpers.GuiHelper.createSlot;
import static cofh.core.util.helpers.GuiHelper.generatePanelInfo;
import static cofh.lib.util.Constants.PATH_GUI;
import static cofh.lib.util.constants.ModIds.ID_THERMAL;
import static cofh.lib.util.helpers.SoundHelper.playClickSound;

public class FluidFilterAttachmentScreen extends ContainerScreenCoFH<FluidFilterAttachmentContainer> {

    public static final ResourceLocation TEXTURE = new ResourceLocation(PATH_GUI + "generic.png");

    public static final String TEX_DENY_LIST = PATH_GUI + "filters/filter_deny_list.png";
    public static final String TEX_ALLOW_LIST = PATH_GUI + "filters/filter_allow_list.png";
    public static final String TEX_IGNORE_NBT = PATH_GUI + "filters/filter_ignore_nbt.png";
    public static final String TEX_USE_NBT = PATH_GUI + "filters/filter_use_nbt.png";

    public static final String TEX_BIDIRECTIONAL = ID_THERMAL + ":textures/gui/container/filter_attachment_mode_bidirectional.png";
    public static final String TEX_INPUT = ID_THERMAL + ":textures/gui/container/filter_attachment_mode_inbound.png";
    public static final String TEX_OUTPUT = ID_THERMAL + ":textures/gui/container/filter_attachment_mode_outbound.png";

    protected final FluidFilterAttachment attachment;

    public FluidFilterAttachmentScreen(FluidFilterAttachmentContainer container, Inventory inv, Component titleIn) {

        super(container, inv, titleIn);

        texture = TEXTURE;
        attachment = container.attachment;
        info = generatePanelInfo("info.thermal.fluid_filter_attachment");
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
        addButtons();
    }

    // region ELEMENTS
    protected void addButtons() {

        addElement(new ElementButton(this, 121, 22) {

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {

                menu.setAllowList(true);
                playClickSound(0.7F);
                return true;
            }
        }
                .setSize(20, 20)
                .setTexture(TEX_DENY_LIST, 40, 20)
                .setTooltipFactory(new SimpleTooltip(new TranslatableComponent("info.cofh.filter.allowlist.0")))
                .setVisible(() -> !menu.getAllowList()));

        addElement(new ElementButton(this, 121, 22) {

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {

                menu.setAllowList(false);
                playClickSound(0.4F);
                return true;
            }
        }
                .setSize(20, 20)
                .setTexture(TEX_ALLOW_LIST, 40, 20)
                .setTooltipFactory(new SimpleTooltip(new TranslatableComponent("info.cofh.filter.allowlist.1")))
                .setVisible(() -> menu.getAllowList()));

        addElement(new ElementButton(this, 121, 44) {

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {

                menu.setCheckNBT(true);
                playClickSound(0.7F);
                return true;
            }
        }
                .setSize(20, 20)
                .setTexture(TEX_IGNORE_NBT, 40, 20)
                .setTooltipFactory(new SimpleTooltip(new TranslatableComponent("info.cofh.filter.checkNBT.0")))
                .setVisible(() -> !menu.getCheckNBT()));

        addElement(new ElementButton(this, 121, 44) {

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {

                menu.setCheckNBT(false);
                playClickSound(0.4F);
                return true;
            }
        }
                .setSize(20, 20)
                .setTexture(TEX_USE_NBT, 40, 20)
                .setTooltipFactory(new SimpleTooltip(new TranslatableComponent("info.cofh.filter.checkNBT.1")))
                .setVisible(() -> menu.getCheckNBT()));

        addElement(new ElementButton(this, 143, 33) {

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {

                attachment.setFilterMode(FilterMode.TO_EXTERNAL_ONLY);
                playClickSound(0.8F);
                return true;
            }
        }
                .setSize(20, 20)
                .setTexture(TEX_BIDIRECTIONAL, 40, 20)
                .setTooltipFactory(new SimpleTooltip(new TranslatableComponent("info.thermal.filter_attachment.mode.0")))
                .setVisible(() -> attachment.getFilterMode() == FilterMode.BIDIRECTIONAL));

        addElement(new ElementButton(this, 143, 33) {

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {

                attachment.setFilterMode(FilterMode.TO_GRID_ONLY);
                playClickSound(0.7F);
                return true;
            }
        }
                .setSize(20, 20)
                .setTexture(TEX_INPUT, 40, 20)
                .setTooltipFactory(new SimpleTooltip(new TranslatableComponent("info.thermal.filter_attachment.mode.1")))
                .setVisible(() -> attachment.getFilterMode() == FilterMode.TO_EXTERNAL_ONLY));

        addElement(new ElementButton(this, 143, 33) {

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {

                attachment.setFilterMode(FilterMode.BIDIRECTIONAL);
                playClickSound(0.6F);
                return true;
            }
        }
                .setSize(20, 20)
                .setTexture(TEX_OUTPUT, 40, 20)
                .setTooltipFactory(new SimpleTooltip(new TranslatableComponent("info.thermal.filter_attachment.mode.2")))
                .setVisible(() -> attachment.getFilterMode() == FilterMode.TO_GRID_ONLY));
    }
    // endregion
}
