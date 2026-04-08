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

import java.util.*;

@OnlyIn(Dist.CLIENT)
public class EntityLodHandler {

    private static final double NEAR_DIST = 20.0;
    private static final double MID_DIST = 35.0;
    private static final double FAR_DIST = 50.0;

    private static final int CHECK_INTERVAL = 10;

    private final OptimizationState state;
    private final Set<Integer> hiddenEntities = new HashSet<>();
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

        // лимиты зависят от текущего уровня оптимизации
        int maxMid  = switch (state.getLevel()) {
            case NORMAL -> 24;
            case MEDIUM -> 16;
            case AGGRESSIVE -> 8;
        };
        int maxFar  = switch (state.getLevel()) {
            case NORMAL -> 8;
            case MEDIUM -> 6;
            case AGGRESSIVE -> 3;
        };
        int maxVFar = switch (state.getLevel()) {
            case NORMAL -> 4;
            case MEDIUM -> 2;
            case AGGRESSIVE -> 1;
        };

        hiddenEntities.clear();

        List<MobWithDist> mobs = new ArrayList<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof Mob)) continue;
            if (!entity.isAlive()) continue;
            mobs.add(new MobWithDist(entity.getId(), entity.position().distanceTo(playerPos)));
        }

        mobs.sort(Comparator.comparingDouble(MobWithDist::dist));

        int midCount = 0;
        int farCount = 0;
        int vfarCount = 0;

        for (MobWithDist mob : mobs) {
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

    private record MobWithDist(int id, double dist) {}
}