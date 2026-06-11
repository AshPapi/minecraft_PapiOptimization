package net.ashpapi.papiai.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {
    @Unique
    private Vec3 papiai$cachedSkyColor = null;
    @Unique
    private double papiai$lastCamX;
    @Unique
    private double papiai$lastCamY;
    @Unique
    private double papiai$lastCamZ;
    @Unique
    private long papiai$lastTickTime = -1L;
    @Unique
    private float papiai$lastRainLevel = -1.0F;
    @Unique
    private float papiai$lastThunderLevel = -1.0F;

    @Inject(method = "getSkyColor(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void papiai$onGetSkyColor(Vec3 cameraPos, float partialTicks, CallbackInfoReturnable<Vec3> cir) {
        ClientLevel level = (ClientLevel) (Object) this;
        long currentTick = level.getGameTime();
        float rainLevel = level.getRainLevel(partialTicks);
        float thunderLevel = level.getThunderLevel(partialTicks);

        boolean sameTick = currentTick == papiai$lastTickTime;
        boolean standingStill = Math.abs(cameraPos.x - papiai$lastCamX) < 0.05 && 
                                Math.abs(cameraPos.y - papiai$lastCamY) < 0.05 && 
                                Math.abs(cameraPos.z - papiai$lastCamZ) < 0.05;

        if (papiai$cachedSkyColor != null && 
            (sameTick || (standingStill && Math.abs(currentTick - papiai$lastTickTime) < 10L)) &&
            Math.abs(rainLevel - papiai$lastRainLevel) < 0.01F &&
            Math.abs(thunderLevel - papiai$lastThunderLevel) < 0.01F) {
            
            cir.setReturnValue(papiai$cachedSkyColor);
            return;
        }

        if (!sameTick) {
            papiai$lastTickTime = currentTick;
        }
        if (!standingStill) {
            papiai$lastCamX = cameraPos.x;
            papiai$lastCamY = cameraPos.y;
            papiai$lastCamZ = cameraPos.z;
        }
        papiai$lastRainLevel = rainLevel;
        papiai$lastThunderLevel = thunderLevel;
    }

    @Inject(method = "getSkyColor(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;", at = @At("TAIL"))
    private void papiai$afterGetSkyColor(Vec3 cameraPos, float partialTicks, CallbackInfoReturnable<Vec3> cir) {
        papiai$cachedSkyColor = cir.getReturnValue();
    }
}
