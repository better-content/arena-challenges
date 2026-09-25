package com.bettercontent.arenachallenges.block;

import com.bettercontent.arenachallenges.menu.ArenaMenu;
import com.bettercontent.arenachallenges.registry.ArenaBlockEntities;
import com.bettercontent.arenachallenges.registry.ArenaItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class ArenaTotemBlockEntity extends BlockEntity implements MenuProvider {
    private static final String STOCK_KEY = "RemainingRewards";
    private final NonNullList<String> remainingRewards = NonNullList.create();
    private boolean stockLoaded;

    public ArenaTotemBlockEntity(BlockPos pos, BlockState state) {
        super(ArenaBlockEntities.ARENA_TOTEM.get(), pos, state);
    }

    public void ensureStock() {
        if (stockLoaded) return;
        stockLoaded = true;
        remainingRewards.clear();
        remainingRewards.add("arena_challenges:champion_blade");
        remainingRewards.add("arena_challenges:warden_axe");
        remainingRewards.add("arena_challenges:duelist_sigil");
        setChanged();
    }

    public int rewardCount() { ensureStock(); return remainingRewards.size(); }
    public void emptyStockForVisualValidation() {
        remainingRewards.clear();
        stockLoaded = true;
        setChanged();
    }
    public String archiveId() {
        return getLevel() == null ? "unknown:" + worldPosition.asLong() : getLevel().dimension().location() + ":" + worldPosition.asLong();
    }
    public int recordCount() { return level instanceof ServerLevel serverLevel ? com.bettercontent.arenachallenges.server.ArenaTraceBridge.count(serverLevel, archiveId()) : 0; }
    public java.util.List<String> remainingRewards() { ensureStock(); return java.util.List.copyOf(remainingRewards); }

    public boolean claimReward(String rewardId, ServerPlayer player) {
        ensureStock();
        if (!remainingRewards.remove(rewardId)) return false;
        var id = net.minecraft.resources.ResourceLocation.tryParse(rewardId);
        var stack = id == null ? net.minecraft.world.item.ItemStack.EMPTY : switch (id.getPath()) {
            case "champion_blade" -> ArenaItems.CHAMPION_BLADE.get().getDefaultInstance();
            case "warden_axe" -> ArenaItems.WARDEN_AXE.get().getDefaultInstance();
            case "duelist_sigil" -> ArenaItems.DUELIST_SIGIL.get().getDefaultInstance();
            default -> net.minecraft.world.item.ItemStack.EMPTY;
        };
        if (stack.isEmpty()) return false;
        if (id.getPath().equals("champion_blade")) {
            stack.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, 4);
            stack.enchant(net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT, 1);
            stack.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 3);
        } else if (id.getPath().equals("warden_axe")) {
            stack.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, 4);
            stack.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 3);
        }
        if (!player.getInventory().add(stack)) player.drop(stack, false);
        player.getInventory().setChanged();
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ensureStock();
        ListTag stock = new ListTag();
        remainingRewards.forEach(id -> stock.add(StringTag.valueOf(id)));
        tag.put(STOCK_KEY, stock);
        tag.putBoolean("StockInitialized", true);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        remainingRewards.clear();
        stockLoaded = tag.getBoolean("StockInitialized");
        if (stockLoaded) {
            ListTag stock = tag.getList(STOCK_KEY, 8);
            stock.forEach(entry -> remainingRewards.add(entry.getAsString()));
        }
    }

    @Override public Component getDisplayName() { return Component.translatable("container.arena_challenges.totem"); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        ensureStock();
        return new ArenaMenu(id, inventory, this);
    }

    @Override public CompoundTag getUpdateTag() { CompoundTag tag = new CompoundTag(); saveAdditional(tag); return tag; }
    @Nullable @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
