    package net.ashpapi.papiai;

    import net.minecraft.client.Minecraft;
    import net.minecraft.client.ParticleStatus;
    import net.minecraftforge.api.distmarker.Dist;
    import net.minecraftforge.api.distmarker.OnlyIn;
    import net.minecraftforge.event.TickEvent;
    import net.minecraftforge.eventbus.api.SubscribeEvent;


    @OnlyIn(Dist.CLIENT)
    public class ParticleCullingHandler {

        private static final int LOW_FPS_THRESHOLD = 30;
        private static final int FPS_UPDATE_INTERVAL = 20;

        private double smoothedFps = 60.0;
        private boolean lowFpsMode = false;
        private int tickCounter = 0;

        private ParticleStatus userParticleSetting = null;

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            tickCounter++;
            if (tickCounter % FPS_UPDATE_INTERVAL != 0) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            if (userParticleSetting == null) {
                userParticleSetting = mc.options.particles().get();
            }

            smoothedFps = 0.7 * smoothedFps + 0.3 * mc.getFps();

            boolean wasLow = lowFpsMode;

            if (smoothedFps < LOW_FPS_THRESHOLD) {
                lowFpsMode = true;
            } else if (smoothedFps > LOW_FPS_THRESHOLD + 5) {
                lowFpsMode = false;
            }

            if (wasLow == lowFpsMode) return;

            if (lowFpsMode) {
                mc.options.particles().set(ParticleStatus.MINIMAL);
            } else {
                mc.options.particles().set(userParticleSetting);
            }
        }
    }