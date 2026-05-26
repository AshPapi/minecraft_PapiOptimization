package net.ashpapi.papiai.mixin;

import net.ashpapi.papiai.PathfindingOptimizer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(PathFinder.class)
public class PathFinderMixin {

    @Inject(method = "findPath", at = @At("HEAD"), cancellable = true)
    private void onFindPath(PathNavigationRegion region, Mob mob, Set<BlockPos> targets, float maxRange, int accuracy, float redirectionFactor, CallbackInfoReturnable<Path> cir) {
        long gameTime = mob.level().getGameTime();
        Path shared = PathfindingOptimizer.getSharedPath(mob, targets, gameTime);
        if (shared != null) {
            cir.setReturnValue(shared);
        }
    }

    @Inject(method = "findPath", at = @At("RETURN"))
    private void onFindPathReturn(PathNavigationRegion region, Mob mob, Set<BlockPos> targets, float maxRange, int accuracy, float redirectionFactor, CallbackInfoReturnable<Path> cir) {
        Path result = cir.getReturnValue();
        if (result != null) {
            long gameTime = mob.level().getGameTime();
            PathfindingOptimizer.cachePath(mob, targets, result, gameTime);
        }
    }
}
