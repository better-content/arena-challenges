package com.bettercontent.arenachallenges.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;

/** Optional binary boundary to Player Traces, which owns persistent arena replays. */
public final class ArenaTraceBridge {
    private static final String API = "com.bettercontent.playertraces.TracesMod";
    private static Method append;
    private static Method count;
    private static Method get;

    private ArenaTraceBridge() {}

    private static Method method(String name, Class<?>... args) throws ReflectiveOperationException {
        return Class.forName(API).getMethod(name, args);
    }

    public static boolean available() {
        try { method("arenaDuelCount", ServerLevel.class, String.class); return true; }
        catch (ReflectiveOperationException ignored) { return false; }
    }

    public static int count(ServerLevel level, String arenaId) {
        try {
            if (count == null) count = method("arenaDuelCount", ServerLevel.class, String.class);
            return (int) count.invoke(null, level, arenaId);
        } catch (ReflectiveOperationException error) {
            return 0;
        }
    }

    public static int appendIndex(ServerLevel level, String arenaId) {
        return count(level, arenaId);
    }

    public static void append(ServerLevel level, String arenaId, CompoundTag record) {
        try {
            if (append == null) append = method("appendArenaDuel", ServerLevel.class, String.class, byte[].class);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            NbtIo.writeCompressed(record, bytes);
            append.invoke(null, level, arenaId, bytes.toByteArray());
        } catch (ReflectiveOperationException | java.io.IOException error) {
            throw new IllegalStateException("Player Traces could not archive the arena duel", error);
        }
    }

    public static CompoundTag get(ServerLevel level, String arenaId, int index) {
        try {
            if (get == null) get = method("arenaDuel", ServerLevel.class, String.class, int.class);
            byte[] bytes = (byte[]) get.invoke(null, level, arenaId, index);
            return bytes == null ? null : NbtIo.readCompressed(new ByteArrayInputStream(bytes));
        } catch (ReflectiveOperationException | java.io.IOException error) {
            return null;
        }
    }
}
