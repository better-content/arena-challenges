package com.bettercontent.arenachallenges.worldgen;

import com.bettercontent.arenachallenges.ArenaChallenges;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ArenaStructures {
    public static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, ArenaChallenges.MOD_ID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, ArenaChallenges.MOD_ID);
    public static final RegistryObject<StructureType<ArenaStructure>> ARENA = TYPES.register("arena",
            () -> () -> ArenaStructure.CODEC);
    public static final RegistryObject<StructurePieceType> ARENA_PIECE = PIECES.register("arena",
            () -> ArenaStructurePiece::new);

    private ArenaStructures() {}
}
