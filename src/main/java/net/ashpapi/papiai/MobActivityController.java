package net.ashpapi.papiai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MobActivityController {

    private static final double NEAR_RADIUS = 13.0;
    private static final double FAR_RADIUS  = 45.0;
    private static final int    MIN_AI_PERCENT       = 5;
    private static final int    INVISIBLE_AI_PERCENT = 1;

    private final EntityCullingHandler cullingHandler;
    private final EntityLodHandler     lodHandler;

    public MobActivityController(EntityCullingHandler cullingHandler, EntityLodHandler lodHandler) {
        this.cullingHandler = cullingHandler;
        this.lodHandler     = lodHandler;
    }

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();

        if (!(entity.level() instanceof ServerLevel level)) return;
        if (!(entity instanceof Mob mob)) return;
        if (!mob.isAlive() || mob.isRemoved()) return;

        long gameTime = level.getGameTime();
        double searchRadius = FAR_RADIUS + 16.0;
        Player nearest = level.getNearestPlayer(mob, searchRadius);

        if (!cullingHandler.isVisible(mob) || lodHandler.isHidden(mob)) {
            if (!shouldTick(gameTime, INVISIBLE_AI_PERCENT)) {
                event.setCanceled(true);
            }
            return;
        }

        if (nearest == null) {
            if (!shouldTick(gameTime, MIN_AI_PERCENT)) {
                event.setCanceled(true);
            }
            return;
        }

        double dist = mob.distanceTo(nearest);
        int aiPercent = computeAiPercent(dist);

        if (!shouldTick(gameTime, aiPercent)) {
            event.setCanceled(true);
        }
    }

    private static int computeAiPercent(double dist) {
        if (dist <= NEAR_RADIUS) return 100;
        if (dist >= FAR_RADIUS)  return MIN_AI_PERCENT;

        double t = (dist - NEAR_RADIUS) / (FAR_RADIUS - NEAR_RADIUS);
        return (int) Math.round(100.0 + t * (MIN_AI_PERCENT - 100.0));
    }

    private static boolean shouldTick(long gameTime, int percent) {
        if (percent >= 100) return true;
        if (percent <= 0)   return false;
        return gameTime % Math.round(100.0f / percent) == 0;
    }
}