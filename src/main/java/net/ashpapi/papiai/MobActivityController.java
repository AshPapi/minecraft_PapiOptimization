package net.ashpapi.papiai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class MobActivityController {

    private static final double NEAR_RADIUS    = 13.0;
    private static final double FAR_RADIUS     = 45.0;
    private static final int    MIN_AI_PERCENT = 5;
    private static final int    CACHE_DURATION = 5;
    private static final int    HURT_GRACE_TICKS = 60;

    private final Int2ObjectMap<CachedActivity> activityCache = new Int2ObjectOpenHashMap<>();
    private long lastCleanupTime = 0;

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();

        if (!(entity.level() instanceof ServerLevel level)) return;
        if (!(entity instanceof Mob mob)) return;
        if (!mob.isAlive() || mob.isRemoved()) return;

        // Mobs in combat must keep full AI (e.g. phantoms diving at the player)
        if (mob.getTarget() != null || mob.isAggressive()) return;
        if (mob.getLastHurtByMob() != null
                && mob.tickCount - mob.getLastHurtByMobTimestamp() < HURT_GRACE_TICKS) return;

        // Skipping ticks for airborne mobs freezes them mid-air (no gravity/flight)
        if (!mob.onGround() && !mob.isInWater()) return;

        // Baby growth advances in aiStep(); throttled babies would grow up to 20x slower
        if (mob.isBaby()) return;

        long gameTime = level.getGameTime();
        int entityId = mob.getId();

        if (gameTime - lastCleanupTime > 200) {
            activityCache.keySet().removeIf(id -> level.getEntity(id) == null);
            lastCleanupTime = gameTime;
        }

        CachedActivity cached = activityCache.get(entityId);
        int aiPercent;

        if (cached != null && gameTime - cached.lastCheckTime < CACHE_DURATION) {
            aiPercent = cached.aiPercent;
        } else {
            Player nearest = level.getNearestPlayer(mob, FAR_RADIUS + 16.0);
            if (nearest == null) {
                aiPercent = MIN_AI_PERCENT;
            } else {
                double dist = mob.distanceTo(nearest);
                aiPercent = computeAiPercent(dist);
            }

            if (cached == null) {
                activityCache.put(entityId, new CachedActivity(gameTime, aiPercent));
            } else {
                cached.update(gameTime, aiPercent);
            }
        }

        if (!shouldTick(gameTime, aiPercent, entityId)) {
            event.setCanceled(true);
        }
    }

    private static int computeAiPercent(double dist) {
        if (dist <= NEAR_RADIUS) return 100;
        if (dist >= FAR_RADIUS)  return MIN_AI_PERCENT;
        double t = (dist - NEAR_RADIUS) / (FAR_RADIUS - NEAR_RADIUS);
        return (int) Math.round(100.0 + t * (MIN_AI_PERCENT - 100.0));
    }

    private static boolean shouldTick(long gameTime, int percent, int entityId) {
        if (percent >= 100) return true;
        if (percent <= 0)   return false;
        // Spread allowed ticks evenly instead of one solid burst per 100 ticks,
        // so a mob at 5% ticks every ~20 ticks rather than freezing for ~5 seconds
        return ((gameTime + entityId) * percent) % 100 < percent;
    }

    private static class CachedActivity {
        long lastCheckTime;
        int aiPercent;

        CachedActivity(long lastCheckTime, int aiPercent) {
            this.lastCheckTime = lastCheckTime;
            this.aiPercent = aiPercent;
        }

        void update(long lastCheckTime, int aiPercent) {
            this.lastCheckTime = lastCheckTime;
            this.aiPercent = aiPercent;
        }
    }
}
