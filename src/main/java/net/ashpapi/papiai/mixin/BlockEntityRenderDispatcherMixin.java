package net.ashpapi.papiai.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ashpapi.papiai.BlockEntityCullingHandler;
import net.ashpapi.papiai.OptimizationState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <T extends BlockEntity> void onRender(
            T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, CallbackInfo ci) {
        
        OptimizationState state = OptimizationState.getInstance();
        if (state != null) {
            boolean visible = BlockEntityCullingHandler.getInstance().isBlockEntityVisible(blockEntity, state);
            if (!visible) {
                ci.cancel();
            }
        }
    }
}
