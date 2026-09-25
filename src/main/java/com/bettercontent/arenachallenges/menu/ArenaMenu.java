package com.bettercontent.arenachallenges.menu;

import com.bettercontent.arenachallenges.block.ArenaTotemBlockEntity;
import com.bettercontent.arenachallenges.registry.ArenaMenus;
import com.bettercontent.arenachallenges.server.ArenaMatchService;
import com.bettercontent.arenachallenges.server.ArenaReplayService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public final class ArenaMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final ContainerData data;
    private int replayPage;

    public ArenaMenu(int id, Inventory inventory, ArenaTotemBlockEntity site) {
        super(ArenaMenus.ARENA.get(), id);
        this.pos = site.getBlockPos();
        this.data = new ContainerData() {
            @Override public int get(int index) {
                if (index == 0) return site.rewardCount();
                if (index == 1) return site.recordCount();
                if (index == 2) return replayPage;
                java.util.List<String> stock = site.remainingRewards();
                String id = switch (index) {
                    case 3 -> "arena_challenges:champion_blade";
                    case 4 -> "arena_challenges:warden_axe";
                    case 5 -> "arena_challenges:duelist_sigil";
                    default -> "";
                };
                return stock.contains(id) ? 1 : 0;
            }
            @Override public void set(int index, int value) { if (index == 2) replayPage = value; }
            @Override public int getCount() { return 6; }
        };
        addDataSlots(data);
    }

    public ArenaMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        super(ArenaMenus.ARENA.get(), id);
        this.pos = buffer.readBlockPos();
        this.data = new SimpleContainerData(6);
        addDataSlots(data);
    }

    public BlockPos pos() { return pos; }
    public int rewardCount() { return data.get(0); }
    public int recordCount() { return data.get(1); }
    public int replayPage() { return data.get(2); }
    public boolean rewardAvailable(int index) { return index >= 0 && index < 3 && data.get(3 + index) != 0; }

    public void setReplayPage(int page) { replayPage = Math.max(0, page); broadcastChanges(); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player.level().getBlockEntity(pos) instanceof ArenaTotemBlockEntity site)) return false;
        if (id == 20) { setReplayPage(Math.min(replayPage + 1, Math.max(0, (site.recordCount() - 1) / 3))); return true; }
        if (id == 21) { setReplayPage(Math.max(0, replayPage - 1)); return true; }
        if (id >= 22 && id <= 24) return ArenaReplayService.play(player, site, replayPage, id - 22);
        return ArenaMatchService.handleButton(player, site, id);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) < 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
}
