package top.bearcabbage.catbinding.mixin.client;

import io.ejekta.bountiful.content.BountyItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void modifyGetName(CallbackInfoReturnable<Text> cir) {
    ItemStack itemStack = (ItemStack) (Object) this;

    // Early return if CBOwner is null
    if (itemStack.getSubNbt("CBOwner") == null) {
        return;
    }

    // Check for BountyItem and display conditions
    if (!(itemStack.getItem() instanceof BountyItem
            && (itemStack.getSubNbt("display") == null
                || !itemStack.getSubNbt("display").contains("Name", 8)))) {
        String ownerName = Objects.requireNonNull(itemStack.getSubNbt("CBOwner")).getString("OwnerName");
        cir.setReturnValue(cir.getReturnValue().copy().append(Text.literal("§a属于§6" + ownerName + "§r")));
    }
}

}
