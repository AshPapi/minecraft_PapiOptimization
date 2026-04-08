package net.ashpapi.papiai;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;


@OnlyIn(Dist.CLIENT)
public class RenderEventHandler {

    private final EntityCullingHandler cullingHandler;
    private final EntityLodHandler lodHandler;

    public RenderEventHandler(EntityCullingHandler cullingHandler, EntityLodHandler lodHandler) {
        this.cullingHandler = cullingHandler;
        this.lodHandler = lodHandler;
    }


    @SubscribeEvent
    public void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        Entity entity = event.getEntity();

        if (entity instanceof Player) return;

        if (!(entity instanceof Mob)) return;

        if (!cullingHandler.isVisible(entity)) {
            event.setCanceled(true);
            return;
        }

        if (!lodHandler.shouldRender(entity)) {
            event.setCanceled(true);
        }
    }
}