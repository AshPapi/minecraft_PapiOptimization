package net.ashpapi.papiai;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;

@OnlyIn(Dist.CLIENT)
public class BlockEntityCullingHandler {

    private static final BlockEntityCullingHandler INSTANCE = new BlockEntityCullingHandler();

    public static BlockEntityCullingHandler getInstance() {
        return INSTANCE;
    }

    private static final double MIN_OCCLUSION_DIST = 6.0;
    private static final int MAX_RAYCASTS_PER_TICK = 10;

    private final Long2BooleanMap visibilityCache = new Long2BooleanOpenHashMap();
    private final Long2LongMap lastCheckTicks = new Long2LongOpenHashMap();
    private int raycastsThisTick = 0;
    private long tickCounter = 0;

    private BlockEntityCullingHandler() {
        visibilityCache.defaultReturnValue(true);
        lastCheckTicks.defaultReturnValue(0L);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;
            raycastsThisTick = 0;

            if (tickCounter % 200 == 0) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && mc.level != null) {
                    Vec3 playerPos = mc.player.position();
                    
                    LongIterator it = visibilityCache.keySet().iterator();
                    while (it.hasNext()) {
                        long packedPos = it.nextLong();
                        int x = BlockPos.getX(packedPos);
                        int y = BlockPos.getY(packedPos);
                        int z = BlockPos.getZ(packedPos);
                        double dx = x + 0.5 - playerPos.x;
                        double dy = y + 0.5 - playerPos.y;
                        double dz = z + 0.5 - playerPos.z;
                        if (dx * dx + dy * dy + dz * dz > 4096.0) {
                            it.remove();
                            lastCheckTicks.remove(packedPos);
                        }
                    }
                } else {
                    visibilityCache.clear();
                    lastCheckTicks.clear();
                }
            }
        }
    }

    public boolean isBlockEntityVisible(BlockEntity be, OptimizationState state) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return true;

        BlockPos pos = be.getBlockPos();
        long packedPos = pos.asLong();
        Vec3 playerPos = mc.player.position();
        double distSqr = pos.distToCenterSqr(playerPos);

        if (distSqr < MIN_OCCLUSION_DIST * MIN_OCCLUSION_DIST) {
            return true;
        }

        double maxDist = switch (state.getLevel()) {
            case NORMAL -> 64.0;
            case MEDIUM -> 48.0;
            case AGGRESSIVE -> 32.0;
        };

        if (distSqr > maxDist * maxDist) {
            return false;
        }

        boolean hasCached = visibilityCache.containsKey(packedPos);
        boolean cached = visibilityCache.get(packedPos);
        long lastCheck = lastCheckTicks.get(packedPos);
        long elapsed = tickCounter - lastCheck;

        long interval = (distSqr > 24.0 * 24.0) ? 30 : 15;

        if (!hasCached || elapsed >= interval) {
            if (raycastsThisTick < MAX_RAYCASTS_PER_TICK) {
                boolean visible = checkVisibility(mc.level, mc.gameRenderer.getMainCamera().getPosition(), pos);
                visibilityCache.put(packedPos, visible);
                lastCheckTicks.put(packedPos, tickCounter);
                raycastsThisTick++;
                return visible;
            }
        }

        return !hasCached || cached;
    }

    private boolean checkVisibility(ClientLevel level, Vec3 from, BlockPos pos) {
        Vec3 target = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        HitResult hit = level.clip(new ClipContext(
                from, target,
                ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE,
                null
        ));
        return hit.getType() == HitResult.Type.MISS;
    }
}
