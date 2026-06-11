package net.ashpapi.papiai.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightTexture.class)
public class LightTextureMixin {
    @Shadow @Final private Minecraft minecraft;

    @Unique
    private GameRendererAccessor papiai$gameRendererAccessor = null;
    @Unique
    private double papiai$lastGamma = -1.0;
    @Unique
    private DimensionSpecialEffects papiai$lastDimension = null;
    @Unique
    private boolean papiai$lastNightVision = false;
    @Unique
    private boolean papiai$lastConduitPower = false;
    @Unique
    private float papiai$lastSkyDarkness = -1.0f;
    @Unique
    private long papiai$lastUpdateTime = -1L;
    @Unique
    private float papiai$lastRainLevel = -1.0f;
    @Unique
    private float papiai$lastThunderLevel = -1.0f;

    @Inject(method = "<init>(Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/Minecraft;)V", at = @At("TAIL"))
    private void papiai$onInit(GameRenderer renderer, Minecraft client, CallbackInfo ci) {
        this.papiai$gameRendererAccessor = (GameRendererAccessor) renderer;
    }

    @Inject(method = "tick()V", at = @At("HEAD"), cancellable = true)
    private void papiai$onTick(CallbackInfo ci) {
        if (this.papiai$gameRendererAccessor == null) return;

        LocalPlayer player = this.minecraft.player;
        ClientLevel level = this.minecraft.level;
        if (player == null || level == null) return;

        long currentTime = level.getGameTime();

        // Force update every 20 ticks (1 second) to be absolutely safe
        if (papiai$lastUpdateTime == -1L || currentTime - papiai$lastUpdateTime >= 20L) {
            papiai$updateState(player, level, currentTime);
            return;
        }

        // Under water visibility changes over 600 ticks, we must tick during transition
        if (player.isUnderWater() && ((LocalPlayerAccessor) player).papiai$getWaterVisionTime() < 600) {
            papiai$updateState(player, level, currentTime);
            return;
        }

        // Night vision flashes when duration < 200 ticks, we must tick during flashing
        boolean hasNightVision = player.hasEffect(MobEffects.NIGHT_VISION);
        if (hasNightVision) {
            var effect = player.getEffect(MobEffects.NIGHT_VISION);
            if (effect != null && effect.getDuration() < 200) {
                papiai$updateState(player, level, currentTime);
                return;
            }
        }

        double gamma = this.minecraft.options.gamma().get();
        DimensionSpecialEffects dimension = level.effects();
        boolean hasConduitPower = player.hasEffect(MobEffects.CONDUIT_POWER);
        boolean hasDarkness = player.hasEffect(MobEffects.DARKNESS);
        float rainLevel = level.getRainLevel(1.0f);
        float thunderLevel = level.getThunderLevel(1.0f);
        float skyDarkness = this.papiai$gameRendererAccessor.papiai$getDarkenWorldAmount();

        if (hasDarkness ||
            gamma != papiai$lastGamma ||
            dimension != papiai$lastDimension ||
            hasNightVision != papiai$lastNightVision ||
            hasConduitPower != papiai$lastConduitPower ||
            skyDarkness != papiai$lastSkyDarkness ||
            Math.abs(rainLevel - papiai$lastRainLevel) > 0.01f ||
            Math.abs(thunderLevel - papiai$lastThunderLevel) > 0.01f) {
            
            papiai$updateState(player, level, currentTime);
            return;
        }

        // Nothing changed! Cancel texture update
        ci.cancel();
    }

    @Unique
    private void papiai$updateState(LocalPlayer player, ClientLevel level, long currentTime) {
        papiai$lastGamma = this.minecraft.options.gamma().get();
        papiai$lastDimension = level.effects();
        papiai$lastNightVision = player.hasEffect(MobEffects.NIGHT_VISION);
        papiai$lastConduitPower = player.hasEffect(MobEffects.CONDUIT_POWER);
        papiai$lastSkyDarkness = this.papiai$gameRendererAccessor.papiai$getDarkenWorldAmount();
        papiai$lastRainLevel = level.getRainLevel(1.0f);
        papiai$lastThunderLevel = level.getThunderLevel(1.0f);
        papiai$lastUpdateTime = currentTime;
    }
}
