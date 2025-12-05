package io.github.misode.packtest;

import net.minecraft.server.level.ServerLevel;

import java.util.HashSet;
import java.util.Set;

public class TemporaryForcedChunks {

    private static final Set<TemporaryChunk> temporaryForced = new HashSet<>();

    public static boolean isTemporary(ServerLevel level, int x, int z) {
        return temporaryForced.contains(TemporaryChunk.from(level, x, z));
    }

    public static void markTemporary(ServerLevel level, int x, int z) {
        temporaryForced.add(TemporaryChunk.from(level, x, z));
    }

    public static void unmarkTemporary(ServerLevel level, int x, int z) {
        temporaryForced.remove(TemporaryChunk.from(level, x, z));
    }

    private record TemporaryChunk(String dimension, int x, int z) {

        private static TemporaryChunk from(ServerLevel level, int x, int z) {
            return new TemporaryChunk(level.dimension().identifier().toString(), x, z);
        }
    }
}
