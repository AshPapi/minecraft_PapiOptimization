package net.ashpapi.papiai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

import java.util.*;

public class PathfindingOptimizer {

    private static final int MAX_CACHE_AGE_TICKS = 40;
    private static final double MAX_DEST_DIST_SQR = 4.0;
    private static final double MAX_START_DIST_SQR = 100.0;

    private static final Map<EntityType<?>, List<CachedPath>> pathCache = new HashMap<>();

    public static Path getSharedPath(Mob mob, Set<BlockPos> targets, long gameTime) {
        if (targets == null || targets.isEmpty()) return null;

        BlockPos targetPos = targets.iterator().next();

        if (mob.getNavigation() != null) {
            Path currentPath = mob.getNavigation().getPath();
            if (currentPath != null && !currentPath.isDone()) {
                BlockPos currentDest = currentPath.getTarget();
                if (currentDest.distSqr(targetPos) <= 2.25) {
                    return clonePath(currentPath);
                }
            }
        }

        EntityType<?> type = mob.getType();
        double mobX = mob.getX();
        double mobY = mob.getY();
        double mobZ = mob.getZ();

        List<CachedPath> list = pathCache.get(type);
        if (list == null) return null;

        list.removeIf(cached -> gameTime - cached.timeCreated > MAX_CACHE_AGE_TICKS);

        for (CachedPath cached : list) {
            if (getDistSqr(cached.destinationPacked, targetPos) <= MAX_DEST_DIST_SQR) {
                if (getDistSqr(cached.startPacked, mobX, mobY, mobZ) <= MAX_START_DIST_SQR) {
                    if (cached.path != null) {
                        return clonePath(cached.path);
                    }
                }
            }
        }

        return null;
    }

    public static void cachePath(Mob mob, Set<BlockPos> targets, Path path, long gameTime) {
        if (path == null || targets == null || targets.isEmpty()) return;

        EntityType<?> type = mob.getType();
        long mobPosPacked = BlockPos.asLong((int) mob.getX(), (int) mob.getY(), (int) mob.getZ());
        long targetPosPacked = targets.iterator().next().asLong();

        List<CachedPath> list = pathCache.computeIfAbsent(type, k -> new ArrayList<>());

        if (list.size() >= 5) {
            list.remove(0);
        }

        list.add(new CachedPath(targetPosPacked, mobPosPacked, path, gameTime));
    }

    private static double getDistSqr(long packed, BlockPos pos) {
        double dx = BlockPos.getX(packed) - pos.getX();
        double dy = BlockPos.getY(packed) - pos.getY();
        double dz = BlockPos.getZ(packed) - pos.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private static double getDistSqr(long packed, double x, double y, double z) {
        double dx = BlockPos.getX(packed) - x;
        double dy = BlockPos.getY(packed) - y;
        double dz = BlockPos.getZ(packed) - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public static Path clonePath(Path path) {
        if (path == null) return null;

        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < path.getNodeCount(); i++) {
            nodes.add(path.getNode(i));
        }

        Path cloned = new Path(nodes, path.getTarget(), path.canReach());
        cloned.setNextNodeIndex(path.getNextNodeIndex());
        return cloned;
    }

    private static class CachedPath {
        final long destinationPacked;
        final long startPacked;
        final Path path;
        final long timeCreated;

        CachedPath(long destinationPacked, long startPacked, Path path, long timeCreated) {
            this.destinationPacked = destinationPacked;
            this.startPacked = startPacked;
            this.path = path;
            this.timeCreated = timeCreated;
        }
    }
}
