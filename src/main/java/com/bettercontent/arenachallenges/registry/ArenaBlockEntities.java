package com.bettercontent.arenachallenges.registry;

import com.bettercontent.arenachallenges.ArenaChallenges;
import com.bettercontent.arenachallenges.block.ArenaTotemBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ArenaBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ArenaChallenges.MOD_ID);
    public static final RegistryObject<BlockEntityType<ArenaTotemBlockEntity>> ARENA_TOTEM = TYPES.register("arena_totem",
            () -> BlockEntityType.Builder.of(ArenaTotemBlockEntity::new, ArenaBlocks.ARENA_TOTEM.get()).build(null));

    private ArenaBlockEntities() {}
}
