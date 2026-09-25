package com.bettercontent.arenachallenges.worldgen;

import com.bettercontent.arenachallenges.registry.ArenaBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public final class ArenaStructurePiece extends StructurePiece {
    private static final int RADIUS = 12;
    private final BlockPos center;

    public ArenaStructurePiece(BlockPos center) {
        super(ArenaStructures.ARENA_PIECE.get(), 0, new BoundingBox(center.getX() - RADIUS, -64,
                center.getZ() - RADIUS, center.getX() + RADIUS, 320, center.getZ() + RADIUS));
        this.center = center.immutable();
    }

    public ArenaStructurePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        this(new BlockPos(tag.getInt("cx"), tag.getInt("cy"), tag.getInt("cz")));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("cx", center.getX()); tag.putInt("cy", center.getY()); tag.putInt("cz", center.getZ());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator chunkGenerator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        BlockState floor = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState boundary = Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState();
        BlockState pillar = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = center.getX() - RADIUS; x <= center.getX() + RADIUS; x++) {
            for (int z = center.getZ() - RADIUS; z <= center.getZ() + RADIUS; z++) {
                if (!box.isInside(x, center.getY(), z)) continue;
                int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                for (int foundationY = ground + 1; foundationY < center.getY(); foundationY++) {
                    if (box.isInside(x, foundationY, z)) {
                        pos.set(x, foundationY, z);
                        level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
                    }
                }
                pos.set(x, center.getY(), z);
                level.setBlock(pos, floor, 2);
                for (int dy = 1; dy <= 5; dy++) {
                    pos.set(x, center.getY() + dy, z);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                }
                if (Math.abs(x - center.getX()) == RADIUS || Math.abs(z - center.getZ()) == RADIUS) {
                    for (int dy = 1; dy <= 3; dy++) {
                        pos.set(x, center.getY() + dy, z);
                        level.setBlock(pos, boundary, 2);
                    }
                }
            }
        }
        for (int dx : new int[]{-8, 8}) {
            for (int dz : new int[]{-8, 8}) {
                pos.set(center.getX() + dx, center.getY(), center.getZ() + dz);
                level.setBlock(pos, pillar, 2);
                for (int dy = 1; dy <= 3; dy++) {
                    pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    level.setBlock(pos, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 2);
                }
            }
        }
        pos.set(center.getX(), center.getY(), center.getZ());
        level.setBlock(pos, ArenaBlocks.ARENA_TOTEM.get().defaultBlockState(), 2);
        for (int dx : new int[]{-6, 6}) {
            pos.set(center.getX() + dx, center.getY(), center.getZ());
            level.setBlock(pos, Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 2);
            pos.set(center.getX() + dx, center.getY() + 1, center.getZ());
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
    }
}
