package net.ashpapi.papiai;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public class SoundCullingHandler {

    private final OptimizationState state;

    public SoundCullingHandler(OptimizationState state) {
        this.state = state;
    }

    @SubscribeEvent
    public void onPlaySound(PlaySoundEvent event) {
        if (event.getSound() == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        SoundSource source = event.getSound().getSource();
        if (source != SoundSource.HOSTILE && source != SoundSource.NEUTRAL) return;

        if (event.getSound().isRelative()) return;

        Vec3 soundPos = new Vec3(
                event.getSound().getX(),
                event.getSound().getY(),
                event.getSound().getZ()
        );
        double dist = mc.player.position().distanceTo(soundPos);

        double maxDist = switch (state.getLevel()) {
            case NORMAL -> 48.0;
            case MEDIUM -> 32.0;
            case AGGRESSIVE -> 16.0;
        };

        if (dist > maxDist) {
            event.setSound(null);
        }
    }
}
