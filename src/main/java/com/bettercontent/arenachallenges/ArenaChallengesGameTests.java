package com.bettercontent.arenachallenges;

import com.bettercontent.arenachallenges.registry.ArenaBlocks;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ArenaChallenges.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArenaChallengesGameTests {
    private ArenaChallengesGameTests() {}

    public static void register(RegisterGameTestsEvent event) {
        event.register(ArenaChallengesGameTests.class);
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void arenaTotemIsRegistered(GameTestHelper helper) {
        ResourceLocation id = new ResourceLocation(ArenaChallenges.MOD_ID, "arena_totem");
        helper.assertTrue(ArenaBlocks.ARENA_TOTEM.isPresent(), "Arena Totem block registration is missing");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id), "Arena Totem item registration is missing");
        helper.succeed();
    }
}
