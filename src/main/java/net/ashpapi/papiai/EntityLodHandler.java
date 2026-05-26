package net.ashpapi.papiai;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public class EntityLodHandler {

    private static final double NEAR_DIST = 20.0;
    private static final double MID_DIST = 35.0;
    private static final double FAR_DIST = 50.0;

    private static final int CHECK_INTERVAL = 10;

    private final OptimizationState state;
    private final IntSet hiddenEntities = new IntOpenHashSet();
    private final List<MobWithDist> mobPool = new ArrayList<>();
    private int tickCounter = 0;

    public EntityLodHandler(OptimizationState state) {
        this.state = state;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        tickCounter++;
        if (tickCounter % CHECK_INTERVAL != 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        ClientLevel level = mc.level;
        Vec3 playerPos = mc.player.position();

        hiddenEntities.clear();

        int activeMobCount = 0;
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof Mob)) continue;
            if (!entity.isAlive()) continue;

            MobWithDist mob;
            if (activeMobCount < mobPool.size()) {
                mob = mobPool.get(activeMobCount);
            } else {
                mob = new MobWithDist();
                mobPool.add(mob);
            }
            mob.set(entity.getId(), entity.position().distanceTo(playerPos));
            activeMobCount++;
        }

        if (activeMobCount < 15) {
            return;
        }

        int maxMid  = switch (state.getLevel()) {
            case NORMAL -> 200;
            case MEDIUM -> 48;
            case AGGRESSIVE -> 16;
        };
        int maxFar  = switch (state.getLevel()) {
            case NORMAL -> 80;
            case MEDIUM -> 30;
            case AGGRESSIVE -> 10;
        };
        int maxVFar = switch (state.getLevel()) {
            case NORMAL -> 40;
            case MEDIUM -> 15;
            case AGGRESSIVE -> 5;
        };

        mobPool.subList(0, activeMobCount).sort(Comparator.comparingDouble(m -> m.dist));

        int midCount = 0;
        int farCount = 0;
        int vfarCount = 0;

        for (int i = 0; i < activeMobCount; i++) {
            MobWithDist mob = mobPool.get(i);
            if (mob.dist <= MID_DIST) {
                if (mob.dist > NEAR_DIST) {
                    midCount++;
                    if (midCount > maxMid) hiddenEntities.add(mob.id);
                }
            } else if (mob.dist <= FAR_DIST) {
                farCount++;
                if (farCount > maxFar) hiddenEntities.add(mob.id);
            } else {
                vfarCount++;
                if (vfarCount > maxVFar) hiddenEntities.add(mob.id);
            }
        }
    }

    public boolean shouldRender(Entity entity) {
        return !hiddenEntities.contains(entity.getId());
    }

    public boolean isHidden(Entity entity) {
        return hiddenEntities.contains(entity.getId());
    }

    private static class MobWithDist {
        int id;
        double dist;

        void set(int id, double dist) {
            this.id = id;
            this.dist = dist;
        }
    }
}