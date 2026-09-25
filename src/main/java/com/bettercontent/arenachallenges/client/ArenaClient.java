package com.bettercontent.arenachallenges.client;

import com.bettercontent.arenachallenges.ArenaChallenges;
import com.bettercontent.arenachallenges.registry.ArenaMenus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ArenaChallenges.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ArenaClient {
    private ArenaClient() {}
    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(ArenaMenus.ARENA.get(), ArenaScreen::new));
    }
}
