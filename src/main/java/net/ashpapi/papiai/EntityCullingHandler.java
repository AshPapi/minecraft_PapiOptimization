package net.ashpapi.papiai;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

@OnlyIn(Dist.CLIENT)
public class EntityCullingHandler {

    private static EntityCullingHandler INSTANCE;

    public static EntityCullingHandler getInstance() {
        return INSTANCE;
    }

    private static final double OCCLUSION_MIN_DIST = 6.0;
    private static final double FRUSTUM_DOT_THRESHOLD = -0.3;
    private static final int MAX_RAYCASTS_PER_TICK = 12;

    private final OptimizationState state;
    private final Int2BooleanMap visibilityCache = new Int2BooleanOpenHashMap();
    private final Int2LongMap lastCheckTicks = new Int2LongOpenHashMap();
    private long tickCounter = 0;

    public EntityCullingHandler(OptimizationState state) {
        this.state = state;
        INSTANCE = this;
        visibilityCache.defaultReturnValue(true);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        tickCounter++;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        ClientLevel level = mc.level;
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        Vec3 lookVec = Vec3.directionFromRotation(camera.getXRot(), camera.getYRot());

        double occlusionMaxDist = switch (state.getLevel()) {
            case NORMAL -> 48.0;
            case MEDIUM -> 40.0;
            case AGGRESSIVE -> 32.0;
        };

        if (tickCounter % 100 == 0) {
            IntSet activeIds = new IntOpenHashSet();
            for (Entity entity : level.entitiesForRendering()) {
                activeIds.add(entity.getId());
            }
            visibilityCache.keySet().retainAll(activeIds);
            lastCheckTicks.keySet().retainAll(activeIds);
        }

        int raycastsThisTick = 0;

        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof Player) continue;
            if (!entity.isAlive()) continue;

            int id = entity.getId();
            Vec3 entityCenter = entity.getBoundingBox().getCenter();
            Vec3 toEntity = entityCenter.subtract(camPos);
            double dist = toEntity.length();

            if (dist > 2.0) {
                double dot = toEntity.normalize().dot(lookVec);
                if (dot < FRUSTUM_DOT_THRESHOLD) {
                    visibilityCache.put(id, false);
                    continue;
                }
            }

            if (dist >= OCCLUSION_MIN_DIST && dist <= occlusionMaxDist) {
                long lastCheck = lastCheckTicks.getOrDefault(id, 0L);
                long elapsed = tickCounter - lastCheck;

                long interval = getCheckInterval(dist, state.getLevel());

                if (elapsed >= interval) {
                    if (raycastsThisTick < MAX_RAYCASTS_PER_TICK) {
                        boolean occluded = isOccluded(level, camPos, entity, entity.getBoundingBox());
                        visibilityCache.put(id, !occluded);
                        lastCheckTicks.put(id, tickCounter);
                        raycastsThisTick++;
                    }
                }
            } else {
                visibilityCache.put(id, true);
                lastCheckTicks.put(id, tickCounter);
            }
        }
    }

    private long getCheckInterval(double dist, OptimizationState.Level optimLevel) {
        long base;
        if (dist < 15.0) {
            base = 4;
        } else if (dist < 30.0) {
            base = 8;
        } else {
            base = 16;
        }

        return switch (optimLevel) {
            case NORMAL -> base;
            case MEDIUM -> base + 2;
            case AGGRESSIVE -> base * 2;
        };
    }

    private boolean isOccluded(ClientLevel level, Vec3 from, Entity entity, AABB box) {
        Vec3 center = box.getCenter();
        if (clipPoint(level, from, center.x, center.y, center.z)) {
            return false;
        }

        double sizeX = box.getXsize();
        double sizeY = box.getYsize();
        double sizeZ = box.getZsize();
        if (sizeX < 0.8 && sizeY < 0.8 && sizeZ < 0.8) {
            return true;
        }

        if (clipPoint(level, from, box.minX, center.y, box.minZ)) return false;
        if (clipPoint(level, from, box.maxX, center.y, box.minZ)) return false;
        if (clipPoint(level, from, box.minX, center.y, box.maxZ)) return false;
        if (clipPoint(level, from, box.maxX, center.y, box.maxZ)) return false;

        return true;
    }

    private boolean clipPoint(ClientLevel level, Vec3 from, double x, double y, double z) {
        Vec3 target = new Vec3(x, y, z);
        HitResult hit = level.clip(new ClipContext(
                from, target,
                ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE,
                null
        ));
        return hit.getType() == HitResult.Type.MISS;
    }

    @SubscribeEvent
    public void onEntityJoin(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            int id = event.getEntity().getId();
            visibilityCache.remove(id);
            lastCheckTicks.remove(id);
        }
    }

    @SubscribeEvent
    public void onEntityLeave(net.minecraftforge.event.entity.EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            int id = event.getEntity().getId();
            visibilityCache.remove(id);
            lastCheckTicks.remove(id);
        }
    }

    public boolean isVisible(Entity entity) {
        return visibilityCache.getOrDefault(entity.getId(), true);
    }
}