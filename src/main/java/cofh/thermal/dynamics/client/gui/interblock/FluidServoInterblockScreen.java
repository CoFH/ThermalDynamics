package cofh.thermal.dynamics.client.gui.interblock;

import cofh.core.client.gui.ContainerScreenCoFH;
import cofh.core.client.gui.element.ElementButton;
import cofh.core.client.gui.element.ElementFluid;
import cofh.core.client.gui.element.SimpleTooltip;
import cofh.thermal.dynamics.common.inventory.interblock.FluidServoInterblockContainer;
import cofh.thermal.dynamics.interblock.FluidServoInterblock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import static cofh.core.util.helpers.GuiHelper.createSlot;
import static cofh.core.util.helpers.GuiHelper.generatePanelInfo;
import static cofh.lib.util.Constants.PATH_GUI;
import static cofh.lib.util.helpers.SoundHelper.playClickSound;

public class FluidServoInterblockScreen extends ContainerScreenCoFH<FluidServoInterblockContainer> {

    public static final ResourceLocation TEXTURE = new ResourceLocation(PATH_GUI + "generic.png");

    public static final String TEX_DENY_LIST = PATH_GUI + "filters/filter_deny_list.png";
    public static final String TEX_ALLOW_LIST = PATH_GUI + "filters/filter_allow_list.png";
    public static final String TEX_IGNORE_NBT = PATH_GUI + "filters/filter_ignore_nbt.png";
    public static final String TEX_USE_NBT = PATH_GUI + "filters/filter_use_nbt.png";

    protected final FluidServoInterblock attachment;

    public FluidServoInterblockScreen(FluidServoInterblockContainer container, Inventory inv, Component titleIn) {

        super(container, inv, titleIn);

        texture = TEXTURE;
        attachment = container.attachment;
        info = generatePanelInfo("info.thermal.fluid_servo_interblock");
    }

    @Override
    public void init() {

        super.init();

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
    }
    // endregion
}
