package net.ashpapi.papiai;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

// главный класс мода.
// аннотация @Mod говорит Forge, какой modid у этого мода.
@Mod(PapiAIMod.MODID)
public class PapiAIMod {

    // идентификатор мода. Должен совпадать с modId в mods.toml
    public static final String MODID = "papiai";

    // конструктор вызывается при загрузке мода.
    public PapiAIMod() {
        // регистрируем наш класс CommonEvents на шине событий.
        // он будет слушать события, связанные с игроком.
        MinecraftForge.EVENT_BUS.register(new CommonEvents());

        // регистрируем контроллер активности мобов.
        // этот класс будет управлять частотой тиков AI.
        MinecraftForge.EVENT_BUS.register(new MobActivityController());
    }

    // внутренний класс для общих событий
    public static class CommonEvents {

        // событие входа игрока на сервер / в мир (в одиночке — в интегрированный сервер).
        @SubscribeEvent
        public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            // проверяем, что это именно серверный игрок
            if (event.getEntity() instanceof ServerPlayer player) {
                // отправляем системное сообщение в чат игроку.
                player.sendSystemMessage(
                        Component.literal("PapiAI: мод загружен и работает.")
                );
            }
        }
    }
}
