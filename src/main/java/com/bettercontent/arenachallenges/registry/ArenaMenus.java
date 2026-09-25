package com.bettercontent.arenachallenges.registry;

import com.bettercontent.arenachallenges.ArenaChallenges;
import com.bettercontent.arenachallenges.menu.ArenaMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ArenaMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ArenaChallenges.MOD_ID);
    public static final RegistryObject<MenuType<ArenaMenu>> ARENA = MENUS.register("arena", () -> IForgeMenuType.create(ArenaMenu::new));
    private ArenaMenus() {}
}
