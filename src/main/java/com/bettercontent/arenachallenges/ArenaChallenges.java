package com.bettercontent.arenachallenges;

import com.bettercontent.arenachallenges.registry.ArenaBlocks;
import com.bettercontent.arenachallenges.registry.ArenaBlockEntities;
import com.bettercontent.arenachallenges.registry.ArenaMenus;
import com.bettercontent.arenachallenges.worldgen.ArenaStructures;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.gametest.ForgeGameTestHooks;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ArenaChallenges.MOD_ID)
public final class ArenaChallenges {
    public static final String MOD_ID = "arena_challenges";

    public ArenaChallenges() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ArenaBlocks.BLOCKS.register(modBus);
        ArenaBlocks.ITEMS.register(modBus);
        ArenaBlockEntities.TYPES.register(modBus);
        ArenaMenus.MENUS.register(modBus);
        ArenaStructures.TYPES.register(modBus);
        ArenaStructures.PIECES.register(modBus);
        modBus.addListener(ArenaChallengesGameTests::register);
        if (ForgeGameTestHooks.isGametestEnabled()) {
            GameTestRegistry.register(ArenaChallengesGameTests.class);
        }
    }
}
