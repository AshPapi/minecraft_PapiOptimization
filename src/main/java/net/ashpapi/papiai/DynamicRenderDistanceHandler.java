package net.ashpapi.papiai;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public class DynamicRenderDistanceHandler {

    private static final int MIN_RENDER_DISTANCE = 4;
    private static final int ADJUST_INTERVAL     = 100;

    private final OptimizationState state;
    private int userRenderDistance = -1;
    private int tickCounter = 0;

    public DynamicRenderDistanceHandler(OptimizationState state) {
        this.state = state;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (++tickCounter % ADJUST_INTERVAL != 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        int current = mc.options.renderDistance().get();

        if (userRenderDistance == -1) {
            userRenderDistance = current;
        }

        switch (state.getLevel()) {
            case NORMAL -> {
                if (current < userRenderDistance) {
                    applyRenderDistance(mc, current + 1);
                }
            }
            case AGGRESSIVE -> {
                if (current > MIN_RENDER_DISTANCE) {
                    applyRenderDistance(mc, current - 1);
                }
            }
            case MEDIUM -> {}
        }
    }

    private void applyRenderDistance(Minecraft mc, int distance) {
        mc.options.renderDistance().set(distance);
    }
}
