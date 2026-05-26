package net.ashpapi.papiai.mixin;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class LeavesBlockMixin {

    @Inject(method = "skipRendering(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z", at = @At("HEAD"), cancellable = true)
    private void onSkipRendering(BlockState adjacentState, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        BlockState self = (BlockState) (Object) this;
        if (self.getBlock() instanceof LeavesBlock && adjacentState.getBlock() instanceof LeavesBlock) {
            cir.setReturnValue(true);
        }
    }
}
