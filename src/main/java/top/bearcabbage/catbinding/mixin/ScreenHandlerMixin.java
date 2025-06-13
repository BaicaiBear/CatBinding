package top.bearcabbage.catbinding.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin {

    @Shadow public abstract Slot getSlot(int index);

    /**
     * 检查玩家是否有权限操作指定物品
     * @param player 玩家
     * @param itemStack 物品堆
     * @return 是否有权限
     */
    @Unique
    private boolean hasPermissionToOperateItem(PlayerEntity player, ItemStack itemStack) {
        // 空物品堆无需检查
        if (itemStack == null || itemStack.isEmpty()) {
            return true;
        }

        // 检查玩家是否有2级权限
        if (player instanceof ServerPlayerEntity serverPlayer) {
            if (serverPlayer.hasPermissionLevel(2)) {
                return true;
            }
        }

        // 检查物品NBT中的所有者信息
        NbtCompound nbt = itemStack.getNbt();
        if (nbt == null) {
            return true; // 没有NBT标签的物品允许操作
        }

        NbtCompound cbOwner = nbt.getCompound("CBOwner");
        if (cbOwner == null || cbOwner.isEmpty()) {
            return true; // 没有CBOwner标签的物品允许操作
        }

        String ownerUuid = cbOwner.getString("Owner");
        if (ownerUuid == null || ownerUuid.isEmpty()) {
            return true; // 没有Owner字段的物品允许操作
        }

        // 检查物品所有者是否为当前玩家
        String playerUuid = player.getUuidAsString();
        return ownerUuid.equals(playerUuid);
    }

    /**
     * 检查操作类型是否需要权限验证
     * @param actionType 操作类型
     * @return 是否需要验证
     */
    @Unique
    private boolean shouldCheckPermission(SlotActionType actionType) {
        return actionType == SlotActionType.PICKUP ||
                actionType == SlotActionType.QUICK_MOVE ||
                actionType == SlotActionType.SWAP ||
                actionType == SlotActionType.CLONE ||
                actionType == SlotActionType.THROW ||
                actionType == SlotActionType.PICKUP_ALL;
    }

    /**
     * 拦截internalOnSlotClick方法，在执行前进行权限检查
     */
    @Inject(method = "internalOnSlotClick", at = @At("HEAD"), cancellable = true)
    private void onInternalOnSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        // 只对需要权限验证的操作类型进行检查
        if (!shouldCheckPermission(actionType)) {
            return;
        }

        // 处理不同的操作类型
        switch (actionType) {
            case PICKUP:
            case QUICK_MOVE:
                // 检查槽位中的物品
                if (slotIndex >= 0 && slotIndex < ((ScreenHandler)(Object)this).slots.size()) {
                    Slot slot = ((ScreenHandler)(Object)this).getSlot(slotIndex);
                    if (slot != null && slot.hasStack()) {
                        ItemStack slotStack = slot.getStack();
                        if (!hasPermissionToOperateItem(player, slotStack)) {
                            ci.cancel(); // 取消操作
                            return;
                        }
                    }
                }
                break;

            case SWAP:
                // 检查槽位中的物品
                if (slotIndex >= 0 && slotIndex < ((ScreenHandler)(Object)this).slots.size()) {
                    Slot slot = getSlot(slotIndex);
                    if (slot != null && slot.hasStack()) {
                        ItemStack slotStack = slot.getStack();
                        if (!hasPermissionToOperateItem(player, slotStack)) {
                            ci.cancel(); // 取消操作
                            return;
                        }
                    }
                }
                break;

            case CLONE:
                // 检查槽位中的物品（创造模式克隆）
                if (slotIndex >= 0 && slotIndex < ((ScreenHandler)(Object)this).slots.size()) {
                    Slot slot = getSlot(slotIndex);
                    if (slot != null && slot.hasStack()) {
                        ItemStack slotStack = slot.getStack();
                        if (!hasPermissionToOperateItem(player, slotStack)) {
                            ci.cancel(); // 取消操作
                            return;
                        }
                    }
                }
                break;

            case THROW:
                // 检查槽位中的物品（丢弃物品）
                if (slotIndex >= 0 && slotIndex < ((ScreenHandler)(Object)this).slots.size()) {
                    Slot slot = getSlot(slotIndex);
                    if (slot != null && slot.hasStack()) {
                        ItemStack slotStack = slot.getStack();
                        if (!hasPermissionToOperateItem(player, slotStack)) {
                            ci.cancel(); // 取消操作
                            return;
                        }
                    }
                }
                break;

            case PICKUP_ALL:
                // 检查光标中的物品和目标槽位的物品
                ItemStack cursorStack = ((ScreenHandler)(Object)this).getCursorStack();
                if (cursorStack != null && !cursorStack.isEmpty()) {
                    // 检查所有可能被收集的物品
                    for (int i = 0; i < ((ScreenHandler)(Object)this).slots.size(); i++) {
                        Slot slot = getSlot(i);
                        if (slot != null && slot.hasStack()) {
                            ItemStack slotStack = slot.getStack();
                            // 如果物品可以合并，检查权限
                            if (ItemStack.canCombine(cursorStack, slotStack)) {
                                if (!hasPermissionToOperateItem(player, slotStack)) {
                                    ci.cancel(); // 取消操作
                                    return;
                                }
                            }
                        }
                    }
                }
                break;

            default:
                break;
        }
    }
}