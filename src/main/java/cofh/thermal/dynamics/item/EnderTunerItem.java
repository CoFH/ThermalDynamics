package cofh.thermal.dynamics.item;

import cofh.core.item.ItemCoFH;
import cofh.core.util.ProxyUtils;
import cofh.lib.item.IPlacementItem;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.item.Rarity;
import net.minecraft.util.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class EnderTunerItem extends ItemCoFH implements IPlacementItem {

    public EnderTunerItem(Properties builder) {

        super(builder);

        ProxyUtils.registerItemModelProperty(this, new ResourceLocation("has_data"), ((stack, world, entity) -> stack.hasTag() ? 1F : 0F));
    }

    @Override
    protected void tooltipDelegate(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {

    }

    @Override
    public Rarity getRarity(ItemStack stack) {

        return stack.hasTag() ? Rarity.UNCOMMON : Rarity.COMMON;
    }

    protected boolean useDelegate(ItemStack stack, ItemUseContext context) {

        return false;
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext context) {

        PlayerEntity player = context.getPlayer();
        if (player == null) {
            return ActionResultType.FAIL;
        }
        return player.canPlayerEdit(context.getPos(), context.getFace(), context.getItem()) ? ActionResultType.SUCCESS : ActionResultType.FAIL;
    }

    @Override
    public ActionResultType onItemUseFirst(ItemStack stack, ItemUseContext context) {

        PlayerEntity player = context.getPlayer();
        if (player == null) {
            return ActionResultType.PASS;
        }
        return player.canPlayerEdit(context.getPos(), context.getFace(), stack) && useDelegate(stack, context) ? ActionResultType.SUCCESS : ActionResultType.PASS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {

        ItemStack stack = player.getHeldItem(hand);
        if (player.isSecondaryUseActive()) {
            if (stack.getTag() != null) {
                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 0.3F);
            }
            stack.setTag(null);
        }
        player.swingArm(hand);
        return ActionResult.resultSuccess(stack);
    }

    // region IPlacementItem
    @Override
    public boolean onBlockPlacement(ItemStack stack, ItemUseContext context) {

        return useDelegate(stack, context);
    }
    // endregion

}
