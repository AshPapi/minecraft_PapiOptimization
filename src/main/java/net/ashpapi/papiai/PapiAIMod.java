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
        MinecraftForge.EVENT_BUS.register(new MobActivityController());

        if (FMLEnvironment.dist == Dist.CLIENT) {
            registerClientHandlers();
        }

        MinecraftForge.EVENT_BUS.register(new CommonEvents());
    }

    private void registerClientHandlers() {
        OptimizationState optimState = new OptimizationState();

        EntityCullingHandler cullingHandler = new EntityCullingHandler(optimState);
        EntityLodHandler     lodHandler     = new EntityLodHandler(optimState);

        MinecraftForge.EVENT_BUS.register(optimState);
        MinecraftForge.EVENT_BUS.register(cullingHandler);
        MinecraftForge.EVENT_BUS.register(lodHandler);
        MinecraftForge.EVENT_BUS.register(BlockEntityCullingHandler.getInstance());
        MinecraftForge.EVENT_BUS.register(new DynamicRenderDistanceHandler(optimState));
        MinecraftForge.EVENT_BUS.register(new SoundCullingHandler(optimState));
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
