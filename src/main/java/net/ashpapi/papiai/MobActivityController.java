package net.ashpapi.papiai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MobActivityController {

    // радиус, в пределах которого моб тикается как в обычной игре
    private static final double NEAR_RADIUS = 13.0;

    // радиус, дальше которого используется минимальный процент тиков
    private static final double FAR_RADIUS  = 45.0;

    // минимальный процент тиков AI относительно обычной игры
    // 5% примерно 1 тик из 20
    private static final int MIN_AI_PERCENT = 5;

    // обработчик события "тик живой сущности"
    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        // берём сущность, которая сейчас тикается
        LivingEntity entity = event.getEntity();

        // проверяем, что это серверный мир (на клиенте нас не интересует)
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        // нас интересуют только мобы (зомби, скелеты и т.п.)
        // игроки, стойки для брони и прочие сюда не попадают
        if (!(entity instanceof Mob mob)) {
            return;
        }

        // если моб мёртв или удалён, ничего не делаем
        if (!mob.isAlive() || mob.isRemoved()) {
            return;
        }

        // текущее игровое время в тиках (используем для "разрежения" тиков)
        long gameTime = level.getGameTime();

        // радиус поиска ближайшего игрока (чуть больше дальнего порога)
        double searchRadius = FAR_RADIUS + 16.0;

        // ищем ближайшего игрока вокруг моба
        Player nearest = level.getNearestPlayer(mob, searchRadius);

        // если рядом вообще нет игроков
        // считаем моба "дальним" и даём ему минимальный процент тиков
        if (nearest == null) {
            if (!shouldTick(gameTime, MIN_AI_PERCENT)) {
                // отменяем тик: AI моба в этот тик не выполняется
                event.setCanceled(true);
            }
            return;
        }

        // считаем фактическое расстояние до ближайшего игрока
        double dist = mob.distanceTo(nearest);

        // по расстоянию определяем, какой процент тиков оставить
        int aiPercent = computeAiPercent(dist);

        // если в этот тик моб "не должен думать" — отменяем его тик
        if (!shouldTick(gameTime, aiPercent)) {
            event.setCanceled(true);
        }
    }

    // возвращает процент тиков AI (0..100) в зависимости от расстояния до игрока
    // ближе NEAR_RADIUS  -> 100% тиков
    // дальше FAR_RADIUS  -> MIN_AI_PERCENT
    // между ними         -> плавное уменьшение от 100 до MIN_AI_PERCENT
    private static int computeAiPercent(double dist) {
        // очень близко к игроку, то тикаем каждый тик
        if (dist <= NEAR_RADIUS) {
            return 100;
        }

        // сильно далеко, то тикаем с минимальной частотой
        if (dist >= FAR_RADIUS) {
            return MIN_AI_PERCENT;
        }

        // t от 0 до 1:
        // 0 = на границе ближнего радиуса
        // 1 = на границе дальнего радиуса
        double t = (dist - NEAR_RADIUS) / (FAR_RADIUS - NEAR_RADIUS);

        // линейно уменьшаем процент:
        // при t=0 будет 100%
        // при t=1 будет MIN_AI_PERCENT
        double p = 100.0 + t * (MIN_AI_PERCENT - 100.0);

        // округляем до целого процента
        return (int) Math.round(p);
    }

    // решаем, должен ли моб тикаться в этот конкретный тик
    // в зависимости от желаемого процента тиков.
    private static boolean shouldTick(long gameTime, int percent) {
        // 100% или больше — всегда тикаем
        if (percent >= 100) {
            return true;
        }

        // 0% или меньше — никогда не тикаем
        if (percent <= 0) {
            return false;
        }

        // определяем интервал между тиками
        int interval = Math.round(100.0f / percent);

        // Тикаем только когда номер тика делится на интервал без остатка
        return gameTime % interval == 0;
    }
}
