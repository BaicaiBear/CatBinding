package top.bearcabbage.catbinding.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.Ownable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;
import java.util.UUID;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity implements Ownable {
    @Shadow
    private UUID owner;
    @Shadow
    private int pickupDelay;

    @Shadow public abstract ItemStack getStack();

    public ItemEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/World;DDDLnet/minecraft/item/ItemStack;DDD)V", at = @At("TAIL"))
    public void init(World world, double x, double y, double z, ItemStack stack, double velocityX, double velocityY, double velocityZ, CallbackInfo ci) {
        if (stack.getSubNbt("CBOwner") != null) {
            String ownerUuid = Objects.requireNonNull(stack.getSubNbt("CBOwner")).getString("Owner");
            if (!ownerUuid.isEmpty()) {
                this.owner = UUID.fromString(ownerUuid);
            }
        }
    }

    @Inject(method = "onPlayerCollision", at = @At("TAIL"))
    public void onPlayerCollision(PlayerEntity player, CallbackInfo ci) {
        if (!this.getWorld().isClient) {
            ItemStack itemStack = this.getStack();
            Item item = itemStack.getItem();
            int i = itemStack.getCount();
            if (this.pickupDelay == 0 && (this.owner != null && !this.owner.equals(player.getUuid())) && player.hasPermissionLevel(2) && player.getInventory().insertStack(itemStack)) {
                player.sendPickup(this, i);
                if (itemStack.isEmpty()) {
                    this.discard();
                    itemStack.setCount(i);
                }
                player.increaseStat(Stats.PICKED_UP.getOrCreateStat(item), i);
                player.triggerItemPickedUpByEntityCriteria((ItemEntity) (Object) this);
            }

        }
    }

}
