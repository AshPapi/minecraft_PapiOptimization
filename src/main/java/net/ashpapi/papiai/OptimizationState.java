package net.ashpapi.papiai;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public class OptimizationState {

    public enum Level { NORMAL, MEDIUM, AGGRESSIVE }

    private static final int HIGH_FPS = 60;
    private static final int LOW_FPS = 30;
    private static final int UPDATE_INTERVAL = 20;

    private double smoothedFps = 60.0;
    private Level currentLevel = Level.NORMAL;
    private int tickCounter = 0;

    private ParticleStatus userParticleSetting = null;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tickCounter++;
        if (tickCounter % UPDATE_INTERVAL != 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        if (userParticleSetting == null) {
            userParticleSetting = mc.options.particles().get();
        }

        smoothedFps = 0.7 * smoothedFps + 0.3 * mc.getFps();

        Level previous = currentLevel;

        if (smoothedFps >= HIGH_FPS + 5) {
            currentLevel = Level.NORMAL;
        } else if (smoothedFps <= LOW_FPS - 5) {
            currentLevel = Level.AGGRESSIVE;
        } else if (smoothedFps < HIGH_FPS && smoothedFps > LOW_FPS) {
            currentLevel = Level.MEDIUM;
        }

        if (previous != currentLevel) {
            applyParticleSettings(mc);
        }
    }

    private void applyParticleSettings(Minecraft mc) {
        switch (currentLevel) {
            case NORMAL     -> mc.options.particles().set(userParticleSetting);
            case MEDIUM     -> mc.options.particles().set(ParticleStatus.DECREASED);
            case AGGRESSIVE -> mc.options.particles().set(ParticleStatus.MINIMAL);
        }
    }

    public Level getLevel() {
        return currentLevel;
    }

}