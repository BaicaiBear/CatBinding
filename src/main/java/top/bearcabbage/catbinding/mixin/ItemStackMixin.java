package top.bearcabbage.catbinding.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static top.bearcabbage.catbinding.CatBinding.isItemBound;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {


	@Shadow public abstract NbtCompound getOrCreateSubNbt(String key);

	@Shadow @Nullable public abstract NbtCompound getSubNbt(String key);


	@Shadow public abstract Item getItem();

	@Shadow public abstract ItemStack setCustomName(@Nullable Text name);

	@Shadow public abstract Text getName();

	@Inject(method = "inventoryTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/Item;inventoryTick(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;IZ)V"))
	private void beforeInventoryTick(World world, Entity entity, int slot, boolean selected, CallbackInfo info) {
		if (isItemBound(this.getItem(),this.getSubNbt("display")) && this.getSubNbt("CBOwner")==null && entity instanceof ServerPlayerEntity player && !player.hasPermissionLevel(2)) {
			this.getOrCreateSubNbt("CBOwner").putString("Owner", player.getUuidAsString());
			this.setCustomName(this.getName().copy().append(Text.literal("§a属于§6").append(player.getName()).append("§r")));
		}
	}
}