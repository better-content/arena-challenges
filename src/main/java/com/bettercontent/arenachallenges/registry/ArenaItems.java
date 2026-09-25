package com.bettercontent.arenachallenges.registry;

import com.bettercontent.arenachallenges.ArenaChallenges;
import com.bettercontent.arenachallenges.item.DuelistSigilItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ArenaItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ArenaChallenges.MOD_ID);
    public static final RegistryObject<Item> CHAMPION_BLADE = ITEMS.register("champion_blade",
            () -> new SwordItem(Tiers.NETHERITE, 5, -2.1F, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> WARDEN_AXE = ITEMS.register("warden_axe",
            () -> new AxeItem(Tiers.NETHERITE, 7, -2.8F, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> DUELIST_SIGIL = ITEMS.register("duelist_sigil",
            () -> new DuelistSigilItem(new Item.Properties().stacksTo(1)));
    private ArenaItems() {}
}
