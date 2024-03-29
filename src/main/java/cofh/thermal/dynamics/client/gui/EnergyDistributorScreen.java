//package cofh.thermal.dynamics.client.gui;
//
//import cofh.lib.util.helpers.StringHelper;
//import cofh.thermal.dynamics.block.entity.EnergyDistributorTile;
//import cofh.thermal.dynamics.inventory.container.EnergyDistributorContainer;
//import cofh.thermal.lib.client.gui.ThermalTileScreenBase;
//import net.minecraft.network.chat.Component;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.world.entity.player.Inventory;
//
//import static cofh.core.util.helpers.GuiHelper.generatePanelInfo;
//import static cofh.lib.util.constants.ModIds.ID_COFH_CORE;
//import static cofh.lib.util.constants.ModIds.ID_THERMAL;
//
//public class EnergyDistributorScreen extends ThermalTileScreenBase<EnergyDistributorContainer> {
//
//    public static final String TEX_PATH = ID_THERMAL + ":textures/gui/container/energy_distributor.png";
//    public static final ResourceLocation TEXTURE = new ResourceLocation(TEX_PATH);
//
//    public static final String TEX_MODE_LATCH_OFF = ID_THERMAL + ":textures/gui/container/item_buffer_mode_normal.png";
//    public static final String TEX_MODE_LATCH_ON = ID_THERMAL + ":textures/gui/container/item_buffer_mode_latch.png";
//
//    public static final String TEX_IGNORE_NBT = ID_COFH_CORE + ":textures/gui/filters/filter_ignore_nbt.png";
//    public static final String TEX_USE_NBT = ID_COFH_CORE + ":textures/gui/filters/filter_use_nbt.png";
//
//    protected EnergyDistributorTile tile;
//
//    public EnergyDistributorScreen(EnergyDistributorContainer container, Inventory inv, Component titleIn) {
//
//        super(container, inv, container.tile, StringHelper.getTextComponent("block.thermal.energy_distributor"));
//        tile = container.tile;
//        texture = TEXTURE;
//        info = generatePanelInfo("info.thermal.energy_distributor");
//    }
//
//    @Override
//    public void init() {
//
//        super.init();
//
//        addButtons();
//    }
//
//    // region ELEMENTS
//    protected void addButtons() {
//
//        //        addElement(new ElementButton(this, 78, 27) {
//        //
//        //            @Override
//        //            public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
//        //
//        //                tile.setLatchMode(true);
//        //                playClickSound(0.7F);
//        //                return true;
//        //            }
//        //        }
//        //                .setSize(20, 20)
//        //                .setTexture(TEX_MODE_LATCH_OFF, 40, 20)
//        //                .setTooltipFactory(new SimpleTooltip(new TranslationTextComponent("info.thermal.item_buffer.mode.0")))
//        //                .setVisible(() -> !tile.getLatchMode()));
//        //
//        //        addElement(new ElementButton(this, 78, 27) {
//        //
//        //            @Override
//        //            public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
//        //
//        //                tile.setLatchMode(false);
//        //                playClickSound(0.4F);
//        //                return true;
//        //            }
//        //        }
//        //                .setSize(20, 20)
//        //                .setTexture(TEX_MODE_LATCH_ON, 40, 20)
//        //                .setTooltipFactory(new SimpleTooltip(new TranslationTextComponent("info.thermal.item_buffer.mode.1")))
//        //                .setVisible(() -> tile.getLatchMode()));
//        //
//        //        addElement(new ElementButton(this, 78, 51) {
//        //
//        //            @Override
//        //            public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
//        //
//        //                tile.setCheckNBT(true);
//        //                playClickSound(0.7F);
//        //                return true;
//        //            }
//        //        }
//        //                .setSize(20, 20)
//        //                .setTexture(TEX_IGNORE_NBT, 40, 20)
//        //                .setTooltipFactory(new SimpleTooltip(new TranslationTextComponent("info.cofh.filter.checkNBT.0")))
//        //                .setVisible(() -> !tile.getCheckNBT()));
//        //
//        //        addElement(new ElementButton(this, 78, 51) {
//        //
//        //            @Override
//        //            public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
//        //
//        //                tile.setCheckNBT(false);
//        //                playClickSound(0.4F);
//        //                return true;
//        //            }
//        //        }
//        //                .setSize(20, 20)
//        //                .setTexture(TEX_USE_NBT, 40, 20)
//        //                .setTooltipFactory(new SimpleTooltip(new TranslationTextComponent("info.cofh.filter.checkNBT.1")))
//        //                .setVisible(() -> tile.getCheckNBT()));
//    }
//    // endregion
//}