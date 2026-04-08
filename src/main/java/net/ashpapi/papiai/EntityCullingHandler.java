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

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class EntityCullingHandler {

    private static final double OCCLUSION_MIN_DIST = 6.0;
    private static final double FRUSTUM_DOT_THRESHOLD = -0.3;

    private final OptimizationState state;
    private final Map<Integer, Boolean> visibilityCache = new HashMap<>();
    private int tickCounter = 0;

    public EntityCullingHandler(OptimizationState state) {
        this.state = state;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        tickCounter++;

        // интервал проверки зависит от уровня оптимизации
        int interval = switch (state.getLevel()) {
            case NORMAL -> 6;
            case MEDIUM -> 4;
            case AGGRESSIVE -> 2;
        };

        if (tickCounter % interval != 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        ClientLevel level = mc.level;
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        Vec3 lookVec = Vec3.directionFromRotation(camera.getXRot(), camera.getYRot());

        // дистанция occlusion зависит от уровня оптимизации
        double occlusionMaxDist = switch (state.getLevel()) {
            case NORMAL -> 48.0;
            case MEDIUM -> 40.0;
            case AGGRESSIVE -> 32.0;
        };

        visibilityCache.clear();

        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof Player) continue;
            if (!entity.isAlive()) continue;

            Vec3 entityCenter = entity.getBoundingBox().getCenter();
            Vec3 toEntity = entityCenter.subtract(camPos);
            double dist = toEntity.length();

            // frustum culling
            if (dist > 2.0) {
                double dot = toEntity.normalize().dot(lookVec);
                if (dot < FRUSTUM_DOT_THRESHOLD) {
                    visibilityCache.put(entity.getId(), false);
                    continue;
                }
            }

            // occlusion culling
            if (dist >= OCCLUSION_MIN_DIST && dist <= occlusionMaxDist) {
                if (isOccluded(level, camPos, entityCenter, entity.getBoundingBox())) {
                    visibilityCache.put(entity.getId(), false);
                    continue;
                }
            }

            visibilityCache.put(entity.getId(), true);
        }
    }

    private boolean isOccluded(ClientLevel level, Vec3 from, Vec3 entityCenter, AABB box) {
        Vec3[] checkPoints = {
                entityCenter,
                new Vec3(box.minX, entityCenter.y, box.minZ),
                new Vec3(box.maxX, entityCenter.y, box.minZ),
                new Vec3(box.minX, entityCenter.y, box.maxZ),
                new Vec3(box.maxX, entityCenter.y, box.maxZ),
        };

        for (Vec3 target : checkPoints) {
            HitResult hit = level.clip(new ClipContext(
                    from, target,
                    ClipContext.Block.VISUAL,
                    ClipContext.Fluid.NONE,
                    null
            ));
            if (hit.getType() == HitResult.Type.MISS) return false;
        }
        return true;
    }

    public boolean isVisible(Entity entity) {
        return visibilityCache.getOrDefault(entity.getId(), true);
    }
}