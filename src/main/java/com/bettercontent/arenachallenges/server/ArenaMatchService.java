package com.bettercontent.arenachallenges.server;

import com.bettercontent.arenachallenges.block.ArenaTotemBlockEntity;
import com.bettercontent.arenachallenges.registry.ArenaItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import net.minecraft.commands.Commands;

@Mod.EventBusSubscriber(modid = "arena_challenges")
public final class ArenaMatchService {
    private static final String SNAPSHOT_KEY = "arena_challenges:match_snapshot";
    private static final String NAME_CONSENT_KEY = "arena_challenges:share_replay_name";
    private static final Map<UUID, Invitation> INVITES = new HashMap<>();
    private static final Map<UUID, Match> MATCHES = new HashMap<>();
    private static final Map<UUID, BlockPos> RETURN_POSITIONS = new HashMap<>();
    private static final Map<UUID, BlockPos> ELIGIBLE_REWARD_SITES = new HashMap<>();
    private static final Map<UUID, Match> TRIAL_MOBS = new HashMap<>();

    private ArenaMatchService() {}

    public static boolean handleButton(net.minecraft.world.entity.player.Player player, ArenaTotemBlockEntity site, int button) {
        if (!(player instanceof ServerPlayer serverPlayer) || site.getLevel() == null) return false;
        return switch (button) {
            case 0 -> inviteNearest(serverPlayer, site);
            case 1, 2, 3 -> startTrial(serverPlayer, site, button);
            case 4 -> acceptInvite(serverPlayer, site);
            case 5 -> showReplayStatus(serverPlayer, site);
            case 6, 7, 8 -> claimReward(serverPlayer, site, button - 6);
            default -> false;
        };
    }

    private static boolean inviteNearest(ServerPlayer challenger, ArenaTotemBlockEntity site) {
        if (MATCHES.containsKey(challenger.getUUID()) || !insideArena(challenger, site.getBlockPos())) return message(challenger, "Stand inside the arena and finish your current challenge first.");
        ServerPlayer target = challenger.server.getPlayerList().getPlayers().stream()
                .filter(p -> !p.getUUID().equals(challenger.getUUID()))
                .filter(p -> p.level() == challenger.level() && !MATCHES.containsKey(p.getUUID()))
                .filter(p -> insideArena(p, site.getBlockPos()))
                .min(Comparator.comparingDouble(challenger::distanceToSqr)).orElse(null);
        if (target == null) return message(challenger, "No available opponent is inside this arena.");
        INVITES.put(target.getUUID(), new Invitation(challenger.getUUID(), site.getBlockPos(), challenger.level().dimension()));
        challenger.sendSystemMessage(net.minecraft.network.chat.Component.literal("Duel invitation sent to " + target.getGameProfile().getName() + "."));
        target.sendSystemMessage(net.minecraft.network.chat.Component.literal(challenger.getGameProfile().getName() + " challenged you. Open this totem and choose Accept duel."));
        return true;
    }

    private static boolean acceptInvite(ServerPlayer target, ArenaTotemBlockEntity site) {
        Invitation invite = INVITES.remove(target.getUUID());
        if (invite == null || !invite.site.equals(site.getBlockPos()) || !invite.dimension.equals(target.level().dimension())) {
            return message(target, "There is no duel invitation for this arena.");
        }
        ServerPlayer challenger = target.server.getPlayerList().getPlayer(invite.challenger);
        if (challenger == null || MATCHES.containsKey(challenger.getUUID()) || !insideArena(target, site.getBlockPos()) || !insideArena(challenger, site.getBlockPos())) {
            return message(target, "The duel invitation expired.");
        }
        Match match = Match.duel(site.getBlockPos(), target.level().dimension(), challenger.getUUID(), target.getUUID());
        beginMatch(match, challenger, target);
        challenger.sendSystemMessage(net.minecraft.network.chat.Component.literal("The duel has begun."));
        target.sendSystemMessage(net.minecraft.network.chat.Component.literal("The duel has begun."));
        return true;
    }

    private static boolean startTrial(ServerPlayer player, ArenaTotemBlockEntity site, int tier) {
        if (MATCHES.containsKey(player.getUUID()) || !insideArena(player, site.getBlockPos())) return message(player, "Stand inside the arena and finish your current challenge first.");
        if (site.rewardCount() == 0) return message(player, "This arena's reward stock is depleted. Recorded duels are still available to view.");
        Match match = Match.trial(site.getBlockPos(), player.level().dimension(), player.getUUID(), tier);
        MATCHES.put(player.getUUID(), match);
        stashAndKit(player, match, tier);
        spawnTrialWave(player.serverLevel(), match, site.getBlockPos(), tier);
        return message(player, "Trial " + tier + " begins. Defeat the encounter to claim one remaining unique reward.");
    }

    private static void beginMatch(Match match, ServerPlayer first, ServerPlayer second) {
        match.startedAt = first.server.overworld().getGameTime();
        MATCHES.put(first.getUUID(), match);
        MATCHES.put(second.getUUID(), match);
        stashAndKit(first, match, 2);
        stashAndKit(second, match, 2);
    }

    private static void stashAndKit(ServerPlayer player, Match match, int tier) {
        CompoundTag snapshot = new CompoundTag();
        snapshot.put("inventory", player.getInventory().save(new ListTag()));
        snapshot.putInt("level", player.experienceLevel);
        snapshot.putInt("total", player.totalExperience);
        snapshot.putFloat("progress", player.experienceProgress);
        snapshot.putFloat("health", player.getHealth());
        player.getPersistentData().put(SNAPSHOT_KEY, snapshot);
        player.getInventory().clearContent();
        player.experienceLevel = 0;
        player.totalExperience = 0;
        player.experienceProgress = 0;
        player.setHealth(player.getMaxHealth());
        ItemStack trialBlade = ArenaItems.CHAMPION_BLADE.get().getDefaultInstance();
        trialBlade.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, tier >= 3 ? 4 : 2);
        trialBlade.enchant(net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT, 1);
        player.getInventory().add(trialBlade);
        player.getInventory().add(new ItemStack(Items.SHIELD));
        player.getInventory().add(new ItemStack(Items.COOKED_BEEF, 16));
        player.getInventory().add(new ItemStack(Items.GOLDEN_APPLE, 2));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(tier >= 3 ? Items.DIAMOND_HELMET : Items.IRON_HELMET));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(tier >= 3 ? Items.DIAMOND_CHESTPLATE : Items.IRON_CHESTPLATE));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(tier >= 3 ? Items.DIAMOND_LEGGINGS : Items.IRON_LEGGINGS));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(tier >= 3 ? Items.DIAMOND_BOOTS : Items.IRON_BOOTS));
        player.getInventory().setChanged();
    }

    private static void restore(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(SNAPSHOT_KEY, 10)) return;
        CompoundTag snapshot = data.getCompound(SNAPSHOT_KEY);
        player.getInventory().clearContent();
        player.getInventory().load(snapshot.getList("inventory", 10));
        player.experienceLevel = snapshot.getInt("level");
        player.totalExperience = snapshot.getInt("total");
        player.experienceProgress = snapshot.getFloat("progress");
        player.setHealth(Math.min(snapshot.getFloat("health"), player.getMaxHealth()));
        player.getPersistentData().remove(SNAPSHOT_KEY);
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
    }

    private static void spawnTrialWave(ServerLevel level, Match match, BlockPos center, int tier) {
        List<EntityType<? extends LivingEntity>> roster = switch (tier) {
            case 1 -> List.of(EntityType.ZOMBIE, EntityType.ZOMBIE, EntityType.SKELETON);
            case 2 -> List.of(EntityType.PILLAGER, EntityType.SKELETON, EntityType.VINDICATOR, EntityType.PILLAGER);
            default -> List.of(EntityType.VINDICATOR, EntityType.VINDICATOR, EntityType.PILLAGER, EntityType.PILLAGER, EntityType.EVOKER);
        };
        for (int i = 0; i < roster.size(); i++) {
            LivingEntity mob = roster.get(i).create(level);
            if (mob == null) continue;
            double angle = (Math.PI * 2.0 * i) / roster.size();
            mob.moveTo(center.getX() + Math.cos(angle) * 8.0 + .5, center.getY() + 1,
                    center.getZ() + Math.sin(angle) * 8.0 + .5, (float) (angle * 180.0 / Math.PI), 0);
            mob.getPersistentData().putUUID("arena_challenges:trial_owner", match.first);
            if (level.addFreshEntity(mob)) {
                match.trialMobs.add(mob.getUUID());
                TRIAL_MOBS.put(mob.getUUID(), match);
            }
        }
        match.remainingMobs = match.trialMobs.size();
    }

    private static boolean showReplayStatus(ServerPlayer player, ArenaTotemBlockEntity site) {
        return message(player, "Duel replay archive: " + site.recordCount() + " recorded matches. Select a recording from the arena gallery.");
    }

    private static boolean claimReward(ServerPlayer player, ArenaTotemBlockEntity site, int index) {
        BlockPos eligible = ELIGIBLE_REWARD_SITES.get(player.getUUID());
        if (eligible == null || !eligible.equals(site.getBlockPos())) return message(player, "Complete a duel or trial here before claiming a reward.");
        List<String> choices = List.of("arena_challenges:champion_blade", "arena_challenges:warden_axe", "arena_challenges:duelist_sigil");
        if (index < 0 || index >= choices.size()) return false;
        String reward = choices.get(index);
        if (!site.remainingRewards().contains(reward)) return message(player, "That unique reward has already been claimed.");
        if (!site.claimReward(reward, player)) return message(player, "The reward could not be claimed.");
        ELIGIBLE_REWARD_SITES.remove(player.getUUID());
        return message(player, "Claimed " + reward + ".");
    }

    private static void completeTrial(Match match, ServerPlayer player) {
        MATCHES.remove(player.getUUID());
        restore(player);
        ELIGIBLE_REWARD_SITES.put(player.getUUID(), match.site);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Trial complete. Open the totem to choose one unique reward."));
        player.openMenu((net.minecraft.world.MenuProvider) player.serverLevel().getBlockEntity(match.site));
    }

    private static void forfeit(Match match, UUID loser, MinecraftServer server) {
        ServerPlayer quitter = server.getPlayerList().getPlayer(loser);
        UUID winnerId = match.other(loser);
        ServerPlayer winner = winnerId == null ? null : server.getPlayerList().getPlayer(winnerId);
        if (quitter != null) restore(quitter);
        if (winner != null) {
            restore(winner);
            ELIGIBLE_REWARD_SITES.put(winner.getUUID(), match.site);
            winner.sendSystemMessage(net.minecraft.network.chat.Component.literal("Your opponent forfeited. Open the totem to claim a unique reward."));
        }
        recordDuel(match, server, winnerId, "forfeit");
        removeMatch(match);
    }

    private static void removeMatch(Match match) {
        MATCHES.remove(match.first, match);
        if (match.second != null) MATCHES.remove(match.second, match);
        for (UUID mob : match.trialMobs) TRIAL_MOBS.remove(mob);
    }

    private static void finishDuel(Match match, UUID loser, MinecraftServer server) {
        UUID winnerId = match.other(loser);
        ServerPlayer winner = winnerId == null ? null : server.getPlayerList().getPlayer(winnerId);
        ServerPlayer defeated = server.getPlayerList().getPlayer(loser);
        if (winner != null) {
            restore(winner);
            ELIGIBLE_REWARD_SITES.put(winnerId, match.site);
            winner.sendSystemMessage(net.minecraft.network.chat.Component.literal("You won. Open the totem to choose one unique reward."));
        }
        if (defeated != null) RETURN_POSITIONS.put(loser, match.site);
        recordDuel(match, server, winnerId, "final_death");
        removeMatch(match);
    }

    private static void recordDuel(Match match, MinecraftServer server, UUID winnerId, String result) {
        ServerLevel level = server.getLevel(match.dimension);
        if (level == null) return;
        CompoundTag record = new CompoundTag();
        record.putLong("StartedAt", match.startedAt);
        record.putLong("EndedAt", level.getGameTime());
        record.putString("Result", result);
        record.putInt("Frames", match.frames.size());
        record.put("Motion", match.frames.copy());
        ServerPlayer first = server.getPlayerList().getPlayer(match.first);
        ServerPlayer second = match.second == null ? null : server.getPlayerList().getPlayer(match.second);
        boolean named = first != null && second != null && first.getPersistentData().getBoolean(NAME_CONSENT_KEY)
                && second.getPersistentData().getBoolean(NAME_CONSENT_KEY);
        record.putBoolean("Named", named);
        if (named) {
            record.putString("DuelistA", first.getGameProfile().getName());
            record.putString("DuelistB", second.getGameProfile().getName());
            record.putString("Winner", winnerId != null && winnerId.equals(match.first) ? "A" : "B");
        }
        ArenaTraceBridge.append(level, archiveId(level, match.site), record);
    }

    private static String archiveId(ServerLevel level, BlockPos pos) {
        return level.dimension().location() + ":" + pos.asLong();
    }

    private static void captureFrame(Match match, MinecraftServer server) {
        if (match.kind != Match.Kind.DUEL) return;
        ServerPlayer a = server.getPlayerList().getPlayer(match.first);
        ServerPlayer b = match.second == null ? null : server.getPlayerList().getPlayer(match.second);
        if (a == null || b == null || !a.level().dimension().equals(match.dimension) || !b.level().dimension().equals(match.dimension)) return;
        CompoundTag frame = new CompoundTag();
        frame.putInt("t", match.frames.size());
        putPose(frame, "a", a, match.site);
        putPose(frame, "b", b, match.site);
        match.frames.add(frame);
    }

    private static void putPose(CompoundTag frame, String prefix, ServerPlayer player, BlockPos origin) {
        frame.putShort(prefix + "x", (short) Math.round((player.getX() - origin.getX()) * 16.0));
        frame.putShort(prefix + "y", (short) Math.round((player.getY() - origin.getY()) * 16.0));
        frame.putShort(prefix + "z", (short) Math.round((player.getZ() - origin.getZ()) * 16.0));
        frame.putByte(prefix + "r", (byte) Math.round(player.getYRot() * 256.0F / 360.0F));
        frame.putByte(prefix + "p", (byte) Math.round(player.getXRot()));
    }

    private static boolean insideArena(ServerPlayer player, BlockPos center) {
        return Math.abs(player.getX() - center.getX()) <= 11.5 && Math.abs(player.getZ() - center.getZ()) <= 11.5
                && Math.abs(player.getY() - center.getY()) <= 6.0;
    }

    public static boolean nearArena(ServerPlayer player, BlockPos center) { return insideArena(player, center); }

    private static boolean message(ServerPlayer player, String text) {
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(text));
        return true;
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        Entity entity = event.getEntity();
        Match mobMatch = TRIAL_MOBS.remove(entity.getUUID());
        if (mobMatch != null) {
            mobMatch.trialMobs.remove(entity.getUUID());
            mobMatch.remainingMobs--;
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(mobMatch.first);
            if (mobMatch.remainingMobs <= 0 && player != null) completeTrial(mobMatch, player);
            return;
        }
        if (entity instanceof ServerPlayer loser) {
            Match match = MATCHES.get(loser.getUUID());
            if (match != null && match.kind == Match.Kind.DUEL) finishDuel(match, loser.getUUID(), level.getServer());
            else if (match != null && match.kind == Match.Kind.TRIAL) {
                removeMatch(match);
                RETURN_POSITIONS.put(loser.getUUID(), match.site);
            }
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.getPersistentData().contains(SNAPSHOT_KEY, 10)) return;
        BlockPos returnPos = RETURN_POSITIONS.remove(player.getUUID());
        if (returnPos != null) {
            player.teleportTo(returnPos.getX() + .5, returnPos.getY() + 1, returnPos.getZ() + .5);
        }
        restore(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Match match = MATCHES.get(player.getUUID());
        if (match != null) {
            if (match.kind == Match.Kind.DUEL) forfeit(match, player.getUUID(), player.server);
            else { restore(player); removeMatch(match); }
        }
        restore(player);
        INVITES.remove(player.getUUID());
        INVITES.values().removeIf(invite -> invite.challenger.equals(player.getUUID()));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) restore(player);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        MinecraftServer server = event.getServer();
        if (event.phase != TickEvent.Phase.END) return;
        if (server.getTickCount() % 2 == 0) new HashSet<>(MATCHES.values()).forEach(match -> captureFrame(match, server));
        if (server.getTickCount() % 10 != 0) return;
        for (Match match : new HashSet<>(MATCHES.values())) {
            for (UUID participant : match.participants()) {
                ServerPlayer player = server.getPlayerList().getPlayer(participant);
                if (player != null && !insideArena(player, match.site)) {
                    if (match.kind == Match.Kind.DUEL) forfeit(match, participant, server);
                    else {
                        restore(player);
                        removeMatch(match);
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Trial cancelled when you left the arena."));
                    }
                    break;
                }
            }
        }
        for (var entry : new HashMap<>(RETURN_POSITIONS).entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                BlockPos pos = entry.getValue();
                player.teleportTo(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
                restore(player);
                RETURN_POSITIONS.remove(entry.getKey());
            }
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("arena")
                .then(Commands.literal("consent-names").executes(context -> {
                    if (!(context.getSource().getEntity() instanceof ServerPlayer player)) return 0;
                    boolean enabled = !player.getPersistentData().getBoolean(NAME_CONSENT_KEY);
                    player.getPersistentData().putBoolean(NAME_CONSENT_KEY, enabled);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Arena replay names " + (enabled ? "enabled" : "disabled") + ". A duel is named only when both players opt in."));
                    return 1;
                })));
        if (Boolean.getBoolean("arena.visualValidation")) {
            event.getDispatcher().register(Commands.literal("arena")
                    .then(Commands.literal("visual-open").executes(context -> {
                        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) return 0;
                        ServerLevel level = player.serverLevel();
                        BlockPos center = player.blockPosition().relative(player.getDirection(), 5).below();
                        for (int dx = -12; dx <= 12; dx++) {
                            for (int dz = -12; dz <= 12; dz++) {
                                BlockPos floor = center.offset(dx, 0, dz);
                                level.setBlock(floor, net.minecraft.world.level.block.Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 3);
                                if (Math.abs(dx) == 12 || Math.abs(dz) == 12) {
                                    for (int dy = 1; dy <= 3; dy++) level.setBlock(floor.above(dy), net.minecraft.world.level.block.Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState(), 3);
                                } else {
                                    for (int dy = 1; dy <= 5; dy++) level.setBlock(floor.above(dy), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                                }
                            }
                        }
                        for (int dx : new int[]{-8, 8}) for (int dz : new int[]{-8, 8}) {
                            BlockPos pillar = center.offset(dx, 0, dz);
                            level.setBlock(pillar, net.minecraft.world.level.block.Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 3);
                            for (int dy = 1; dy <= 3; dy++) level.setBlock(pillar.above(dy), net.minecraft.world.level.block.Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
                        }
                        level.setBlock(center, com.bettercontent.arenachallenges.registry.ArenaBlocks.ARENA_TOTEM.get().defaultBlockState(), 3);
                        if (!(level.getBlockEntity(center) instanceof ArenaTotemBlockEntity site)) return 0;
                        site.emptyStockForVisualValidation();
                        if (site.recordCount() == 0) {
                            net.minecraft.nbt.ListTag frames = new net.minecraft.nbt.ListTag();
                            for (int tick = 0; tick < 12; tick++) {
                                CompoundTag frame = new CompoundTag();
                                frame.putShort("ax", (short) (-32 + tick * 3)); frame.putShort("ay", (short) 16); frame.putShort("az", (short) -24);
                                frame.putShort("bx", (short) (32 - tick * 2)); frame.putShort("by", (short) 16); frame.putShort("bz", (short) 24);
                                frame.putByte("ar", (byte) 32); frame.putByte("ap", (byte) 0);
                                frame.putByte("br", (byte) 160); frame.putByte("bp", (byte) 0);
                                frames.add(frame);
                            }
                            CompoundTag record = new CompoundTag();
                            record.putBoolean("Named", false);
                            record.put("Motion", frames);
                            record.putInt("Frames", frames.size());
                            record.putString("Result", "visual_fixture");
                            ArenaTraceBridge.append(level, site.archiveId(), record);
                        }
                        net.minecraftforge.network.NetworkHooks.openScreen(player, site, buffer -> buffer.writeBlockPos(center));
                        return 1;
                    })));
        }
    }

    private record Invitation(UUID challenger, BlockPos site, net.minecraft.resources.ResourceKey<Level> dimension) {}

    private static final class Match {
        enum Kind { DUEL, TRIAL }
        final Kind kind;
        final BlockPos site;
        final net.minecraft.resources.ResourceKey<Level> dimension;
        final UUID first;
        final UUID second;
        final Set<UUID> trialMobs = new HashSet<>();
        final ListTag frames = new ListTag();
        int remainingMobs;
        long startedAt;

        private Match(Kind kind, BlockPos site, net.minecraft.resources.ResourceKey<Level> dimension, UUID first, UUID second) {
            this.kind = kind; this.site = site.immutable(); this.dimension = dimension; this.first = first; this.second = second;
        }
        static Match duel(BlockPos site, net.minecraft.resources.ResourceKey<Level> dim, UUID a, UUID b) { return new Match(Kind.DUEL, site, dim, a, b); }
        static Match trial(BlockPos site, net.minecraft.resources.ResourceKey<Level> dim, UUID player, int tier) { return new Match(Kind.TRIAL, site, dim, player, null); }
        Set<UUID> participants() { return second == null ? Set.of(first) : Set.of(first, second); }
        UUID other(UUID player) { return first.equals(player) ? second : second != null && second.equals(player) ? first : null; }
    }
}
