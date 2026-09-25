package com.bettercontent.arenachallenges.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraftforge.common.Tags;

import java.util.ArrayList;
import java.util.Optional;

public final class ArenaStructure extends Structure {
    public static final Codec<ArenaStructure> CODEC = simpleCodec(ArenaStructure::new);
    private static final int RADIUS = 12;

    public ArenaStructure(StructureSettings settings) { super(settings); }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX();
        int z = chunk.getMiddleBlockZ();
        int y = context.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());
        BlockPos center = new BlockPos(x, y - 1, z);
        var biome = context.chunkGenerator().getBiomeSource().getNoiseBiome(
                QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), context.randomState().sampler());
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER) || biome.is(Tags.Biomes.IS_WATER)) return Optional.empty();

        var heights = new ArrayList<Integer>();
        for (int dx = -RADIUS; dx <= RADIUS; dx += 4) {
            for (int dz = -RADIUS; dz <= RADIUS; dz += 4) {
                int height = context.chunkGenerator().getFirstFreeHeight(x + dx, z + dz, Heightmap.Types.WORLD_SURFACE_WG,
                        context.heightAccessor(), context.randomState());
                heights.add(height);
            }
        }
        if (heights.stream().mapToInt(Integer::intValue).max().orElse(y) - heights.stream().mapToInt(Integer::intValue).min().orElse(y) > 4) {
            return Optional.empty();
        }
        return Optional.of(new GenerationStub(center, pieces -> pieces.addPiece(new ArenaStructurePiece(center))));
    }

    @Override
    public StructureType<?> type() { return ArenaStructures.ARENA.get(); }
}
