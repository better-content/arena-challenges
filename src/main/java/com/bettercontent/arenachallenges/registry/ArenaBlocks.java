package com.bettercontent.arenachallenges.registry;

import com.bettercontent.arenachallenges.ArenaChallenges;
import com.bettercontent.arenachallenges.block.ArenaTotemBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ArenaBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ArenaChallenges.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ArenaChallenges.MOD_ID);

    public static final RegistryObject<Block> ARENA_TOTEM = BLOCKS.register("arena_totem",
            () -> new ArenaTotemBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(4.0F).noOcclusion()));
    public static final RegistryObject<Item> ARENA_TOTEM_ITEM = ITEMS.register("arena_totem",
            () -> new BlockItem(ARENA_TOTEM.get(), new Item.Properties()));

    private ArenaBlocks() {}
}
