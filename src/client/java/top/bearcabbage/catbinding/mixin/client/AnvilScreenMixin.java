package top.bearcabbage.catbinding.mixin.client;

import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.ForgingScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

import static top.bearcabbage.catbinding.CatBindingClient.extractBeforeTarget;

@Mixin(AnvilScreen.class)
abstract public class AnvilScreenMixin extends ForgingScreen<AnvilScreenHandler> {
    public AnvilScreenMixin(AnvilScreenHandler handler, PlayerInventory playerInventory, Text title, Identifier texture) {
        super(handler, playerInventory, title, texture);
    }

    @Shadow private TextFieldWidget nameField;

    @Inject(method = "onSlotUpdate", at = @At("TAIL"))
    public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack, CallbackInfo ci) {
        if (slotId == 0 && stack.getSubNbt("CBOwner")!=null)  this.nameField.setText(extractBeforeTarget(stack.getName(), "§a属于§6" + Objects.requireNonNull(Objects.requireNonNull(stack.getSubNbt("CBOwner")).getString("OwnerName")) + "§r").getString());
    }


}
