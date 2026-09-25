package com.bettercontent.arenachallenges.server;

import com.bettercontent.arenachallenges.block.ArenaTotemBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = "arena_challenges")
public final class ArenaReplayService {
    private static final Map<UUID, Playback> PLAYBACKS = new HashMap<>();

    private ArenaReplayService() {}

    public static boolean play(net.minecraft.world.entity.player.Player rawPlayer, ArenaTotemBlockEntity site, int page, int row) {
        if (!(rawPlayer instanceof ServerPlayer player) || site.getLevel() == null) return false;
        if (!ArenaMatchService.nearArena(player, site.getBlockPos())) {
            player.sendSystemMessage(Component.literal("Stand in the arena to view its duel recordings."));
            return true;
        }
        int archiveSize = ArenaTraceBridge.count(player.serverLevel(), site.archiveId());
        int index = archiveSize - 1 - page * 3 - row;
        if (index < 0 || index >= archiveSize) {
            player.sendSystemMessage(Component.literal("There is no recording in that archive slot."));
            return true;
        }
        CompoundTag record = ArenaTraceBridge.get(player.serverLevel(), site.archiveId(), index);
        if (record == null) {
            player.sendSystemMessage(Component.literal("Player Traces could not load that duel recording."));
            return true;
        }
        ListTag frames = record.getList("Motion", 10);
        if (frames.isEmpty()) {
            player.sendSystemMessage(Component.literal("That duel ended before a replay frame was recorded."));
            return true;
        }
        ServerLevel level = player.serverLevel();
        for (Playback active : new ArrayList<>(PLAYBACKS.values())) {
            if (active.level == level && active.origin.equals(site.getBlockPos())) active.remove();
        }
        String labelA = record.getBoolean("Named") ? record.getString("DuelistA") : "Echo A";
        String labelB = record.getBoolean("Named") ? record.getString("DuelistB") : "Echo B";
        ArmorStand a = createEcho(level, site.getBlockPos(), frames.getCompound(0), "a", labelA, Items.IRON_SWORD);
        ArmorStand b = createEcho(level, site.getBlockPos(), frames.getCompound(0), "b", labelB, Items.SHIELD);
        if (!level.addFreshEntity(a) || !level.addFreshEntity(b)) {
            a.discard(); b.discard();
            player.sendSystemMessage(Component.literal("The replay could not be started."));
            return true;
        }
        Playback playback = new Playback(level, site.getBlockPos(), frames.copy(), a, b);
        PLAYBACKS.put(a.getUUID(), playback);
        player.closeContainer();
        player.sendSystemMessage(Component.literal("Playing archived duel " + (index + 1) + ". Echoes are visual only."));
        return true;
    }

    private static ArmorStand createEcho(ServerLevel level, BlockPos origin, CompoundTag frame, String prefix,
                                         String label, net.minecraft.world.item.Item item) {
        ArmorStand stand = EntityType.ARMOR_STAND.create(level);
        if (stand == null) throw new IllegalStateException("Armor stand entity is unavailable");
        move(stand, origin, frame, prefix);
        stand.setInvulnerable(true);
        stand.setNoGravity(true);
        stand.setSilent(true);
        stand.setNoBasePlate(true);
        stand.setShowArms(true);
        stand.setGlowingTag(true);
        stand.setCustomName(Component.literal(label));
        stand.setCustomNameVisible(true);
        stand.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item));
        stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
        stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
        stand.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
        stand.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.CHAINMAIL_BOOTS));
        stand.getPersistentData().putBoolean("arena_challenges:replay_echo", true);
        return stand;
    }

    private static void move(ArmorStand stand, BlockPos origin, CompoundTag frame, String prefix) {
        double x = origin.getX() + frame.getShort(prefix + "x") / 16.0 + .5;
        double y = origin.getY() + frame.getShort(prefix + "y") / 16.0;
        double z = origin.getZ() + frame.getShort(prefix + "z") / 16.0 + .5;
        float yaw = Byte.toUnsignedInt(frame.getByte(prefix + "r")) * 360.0F / 256.0F;
        stand.moveTo(x, y, z, yaw, frame.getByte(prefix + "p"));
        stand.setYHeadRot(yaw);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        for (Playback playback : new ArrayList<>(PLAYBACKS.values())) {
            if (playback.level != level) continue;
            playback.tick();
            if (playback.finished()) {
                playback.remove();
                PLAYBACKS.remove(playback.a.getUUID());
            }
        }
    }

    private static final class Playback {
        final ServerLevel level;
        final BlockPos origin;
        final ListTag frames;
        final ArmorStand a;
        final ArmorStand b;
        int cursor;
        int subTick;

        Playback(ServerLevel level, BlockPos origin, ListTag frames, ArmorStand a, ArmorStand b) {
            this.level = level; this.origin = origin.immutable(); this.frames = frames; this.a = a; this.b = b;
        }

        void tick() {
            if (++subTick % 2 != 0 || cursor >= frames.size()) return;
            CompoundTag frame = frames.getCompound(cursor++);
            move(a, origin, frame, "a");
            move(b, origin, frame, "b");
        }
        boolean finished() { return cursor >= frames.size(); }
        void remove() { a.discard(); b.discard(); }
    }
}
