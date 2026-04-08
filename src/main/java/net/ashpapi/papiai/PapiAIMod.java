    package net.ashpapi.papiai;

    import net.minecraftforge.api.distmarker.Dist;
    import net.minecraftforge.common.MinecraftForge;
    import net.minecraftforge.event.entity.player.PlayerEvent;
    import net.minecraftforge.eventbus.api.SubscribeEvent;
    import net.minecraftforge.fml.common.Mod;
    import net.minecraftforge.fml.loading.FMLEnvironment;
    import net.minecraft.network.chat.Component;
    import net.minecraft.server.level.ServerPlayer;

    @Mod(PapiAIMod.MODID)
    public class PapiAIMod {

        public static final String MODID = "papiai";

        public PapiAIMod() {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                registerClientHandlers();
            } else {
                MinecraftForge.EVENT_BUS.register(
                        new MobActivityController(new NoOpCulling(), new NoOpLod())
                );
            }

            MinecraftForge.EVENT_BUS.register(new CommonEvents());
        }

        private void registerClientHandlers() {
            OptimizationState optimState = new OptimizationState();

            EntityCullingHandler cullingHandler = new EntityCullingHandler(optimState);
            EntityLodHandler     lodHandler     = new EntityLodHandler(optimState);
            ParticleCullingHandler particleHandler = new ParticleCullingHandler();

            MobActivityController mobController = new MobActivityController(cullingHandler, lodHandler);

            MinecraftForge.EVENT_BUS.register(optimState);
            MinecraftForge.EVENT_BUS.register(cullingHandler);
            MinecraftForge.EVENT_BUS.register(lodHandler);
            MinecraftForge.EVENT_BUS.register(particleHandler);
            MinecraftForge.EVENT_BUS.register(mobController);
            MinecraftForge.EVENT_BUS.register(new RenderEventHandler(cullingHandler, lodHandler));
        }

        private static class NoOpCulling extends EntityCullingHandler {
            NoOpCulling() { super(null); }
            @Override public boolean isVisible(net.minecraft.world.entity.Entity e) { return true; }
        }
        private static class NoOpLod extends EntityLodHandler {
            NoOpLod() { super(null); }
            @Override public boolean isHidden(net.minecraft.world.entity.Entity e) { return false; }
        }

        public static class CommonEvents {
            @SubscribeEvent
            public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
                if (event.getEntity() instanceof ServerPlayer player) {
                    player.sendSystemMessage(
                            Component.literal("PapiAI: симуляция и рендер включены")
                    );
                }
            }
        }
    }