package net.ashpapi.papiai.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ashpapi.papiai.EntityCullingHandler;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void onRenderEntity(
            E entity, double x, double y, double z, float rotation, float partialTicks,
            PoseStack poseStack, MultiBufferSource bufferSource, int light, CallbackInfo ci) {
        
        if (entity instanceof Player) return;

        EntityCullingHandler culling = EntityCullingHandler.getInstance();
        if (culling != null && !culling.isVisible(entity)) {
            ci.cancel();
        }
    }
}
