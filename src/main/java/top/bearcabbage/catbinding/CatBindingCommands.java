package top.bearcabbage.catbinding;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static net.minecraft.server.command.CommandManager.argument;
import static top.bearcabbage.catbinding.CatBinding.*;

public class CatBindingCommands {
    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LiteralArgumentBuilder<ServerCommandSource> cbroot = LiteralArgumentBuilder.<ServerCommandSource>literal("xmcb")
                    .requires(source -> source.hasPermissionLevel(2));

            cbroot.then(LiteralArgumentBuilder.<ServerCommandSource>literal("add-hand")
                            .executes(context -> {
                                ServerCommandSource source = (ServerCommandSource) context.getSource();
                                if (source.getEntity()!=null && source.getEntity() instanceof ServerPlayerEntity player) {
                                    return add(player.getMainHandStack().getItem(), player.getMainHandStack().getSubNbt("display")==null ? EMPTY_NBT : player.getMainHandStack().getSubNbt("display"), source);
                                } else {
                                    source.sendError(Text.literal("Command can only be used by players."));
                                    return 0;
                                }
                            }))
                    .then(LiteralArgumentBuilder.<ServerCommandSource>literal("remove-hand")
                            .executes(context -> {
                                ServerCommandSource source = (ServerCommandSource) context.getSource();
                                if (source.getEntity()!=null && source.getEntity() instanceof ServerPlayerEntity player) {
                                    return remove(player.getMainHandStack().getItem(), player.getMainHandStack().getSubNbt("display")==null ? EMPTY_NBT : player.getMainHandStack().getSubNbt("display"), source);
                                } else {
                                    source.sendError(Text.literal("Command can only be used by players."));
                                    return 0;
                                }
                            }))
                    .then(LiteralArgumentBuilder.<ServerCommandSource>literal("add")
                            .then(argument("item", ItemStackArgumentType.itemStack(registryAccess))
                                    .executes(context -> add(ItemStackArgumentType.getItemStackArgument(context, "item").getItem(), ALL_NBT, (ServerCommandSource) context.getSource()))))
                    .then(LiteralArgumentBuilder.<ServerCommandSource>literal("remove")
                            .then(argument("item", ItemStackArgumentType.itemStack(registryAccess))
                                    .executes(context -> remove(ItemStackArgumentType.getItemStackArgument(context, "item").getItem(), ALL_NBT, (ServerCommandSource) context.getSource()))))
                    .then(LiteralArgumentBuilder.<ServerCommandSource>literal("reload")
                            .executes(context -> {
                                CatBinding.BINDING_DATA.clear();
                                int loadBindingData = CatBinding.loadBindingData();
                                if (loadBindingData == 0) {
                                    ((ServerCommandSource) context.getSource()).sendError(Text.literal("[CatBinding] No binding data found."));
                                } else {
                                    ((ServerCommandSource) context.getSource()).sendFeedback(() -> Text.literal("[CatBinding] "+loadBindingData+" items has been loaded."), true);
                                } return 1;}));
            dispatcher.register(cbroot);
        });
    }

    public static int add(Item item, NbtCompound nbtCompound, ServerCommandSource source) {
        if (item == null || item == Items.AIR) {
            source.sendError(Text.literal("[CatBinding] Invalid item specified."));
            return 0;
        }
        if (CatBinding.BINDING_DATA.containsKey(item)) {
            if (BINDING_DATA.get(item).contains(ALL_NBT)) {
                source.sendError(Text.literal("[CatBinding] Item " + item.getName().getString() + " is already bound to all nbt compounds."));
                return 0;
            }
            else if (CatBinding.BINDING_DATA.get(item).contains(nbtCompound)){
                source.sendError(Text.literal("[CatBinding] Item " + item.getName().getString() + " with same nbt compound is already bound."));
                return 0;
            }
        }
        Set<NbtCompound> itemNbtList = CatBinding.BINDING_DATA.getOrDefault(item, new HashSet<>());
        if (nbtCompound.equals(ALL_NBT)) {
            itemNbtList.clear();
        }
        itemNbtList.add(nbtCompound);
        CatBinding.BINDING_DATA.put(item, itemNbtList);
        CatBinding.saveBindingData();
        source.sendFeedback(() -> Text.literal("[CatBinding] Item " + item.getName().getString() + " with nbt compound " + nbtCompound.toString() + " has been added."),true );
        return 1;
    }

    public static int remove(Item item, NbtCompound nbtCompound, ServerCommandSource source) {
        if (item == null || item == Items.AIR) {
            source.sendError(Text.literal("[CatBinding] Invalid item specified."));
            return 0;
        }
        if (!CatBinding.BINDING_DATA.containsKey(item)) {
                source.sendError(Text.literal("[CatBinding] Item " + item.getName().getString() + " is not bound."));
                return 0;
        }
        if (nbtCompound == null || nbtCompound.equals(ALL_NBT)) {
                BINDING_DATA.remove(item);
                CatBinding.saveBindingData();
                source.sendFeedback(() ->  Text.literal("[CatBinding] Item " + item.getName().getString() + " with all nbt compounds has been removed from binding."), true);
                return 1;
        }
        Set<NbtCompound> itemNbtList = CatBinding.BINDING_DATA.get(item);
        if (itemNbtList == null || !itemNbtList.contains(nbtCompound)) {
            source.sendError(Text.literal("[CatBinding] Item " + item.getName().getString() + " with nbt compound " + nbtCompound.toString() + " is not bound."));
            return 0;
        }
        itemNbtList.remove(nbtCompound);
        if (itemNbtList.isEmpty()) {
            BINDING_DATA.remove(item);
        } else {
            BINDING_DATA.put(item, itemNbtList);
        }
        CatBinding.saveBindingData();
        source.sendFeedback(() -> Text.literal("[CatBinding] Item " + item.getName().getString() + " with nbt compound " + nbtCompound.toString() + " has been removed from binding."), true);
        return 1;
    }
}
