package cofh.thermal.dynamics.item;

import cofh.core.item.ItemCoFH;
import net.minecraft.world.item.ItemStack;

public class AttachmentItem extends ItemCoFH {

    private String type;

    public AttachmentItem(Properties builder, String type) {

        super(builder);
        this.type = type;
    }

    public String getAttachmentType(ItemStack stack) {

        return this.type;
    }

}
