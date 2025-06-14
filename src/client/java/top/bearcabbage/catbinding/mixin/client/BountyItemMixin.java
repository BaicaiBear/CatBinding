package top.bearcabbage.catbinding.mixin.client;

import io.ejekta.bountiful.content.BountyItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(BountyItem.class)
public class BountyItemMixin extends Item {
    public BountyItemMixin(Settings settings) {
        super(settings);
    }

    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void modifyGetName(ItemStack stack, CallbackInfoReturnable<Text> cir) {
        if (stack.getSubNbt("CBOwner")!=null)
            cir.setReturnValue(cir.getReturnValue().copy().setStyle(this.getName().getStyle()).append(Text.literal("§a属于§6"+ Objects.requireNonNull(stack.getSubNbt("CBOwner")).getString("OwnerName")+"§r")));
    }
}
