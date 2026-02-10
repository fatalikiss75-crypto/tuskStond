package com.example.tushpStones.listeners;

import com.example.tushpStones.TushpStones;
import com.example.tushpStones.models.ProtectedRegion;
import com.example.tushpStones.utils.ParticleVisualizer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Обработчик входа/выхода игроков из регионов
 * Оптимизирован: проверка раз в секунду
 */
public class PlayerMoveListener implements Listener {

    private final TushpStones plugin;
    private final ParticleVisualizer visualizer;

    // Кэш текущих регионов игроков
    private final Map<UUID, ProtectedRegion> playerRegions = new HashMap<>();
    // Кэш времени последней проверки
    private final Map<UUID, Long> lastCheckTime = new HashMap<>();

    // Интервал проверки в миллисекундах (1 секунда)
    private static final long CHECK_INTERVAL = 1000;

    public PlayerMoveListener(TushpStones plugin) {
        this.plugin = plugin;
        this.visualizer = new ParticleVisualizer(plugin);
        startCleanupTask();
    }

    /**
     * Обработка движения игрока
     * Проверка оптимизирована - выполняется раз в секунду
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Проверяем нужна ли проверка (раз в секунду)
        long currentTime = System.currentTimeMillis();
        Long lastCheck = lastCheckTime.get(playerId);

        if (lastCheck != null && (currentTime - lastCheck) < CHECK_INTERVAL) {
            return; // Пропускаем, еще не прошла секунда
        }

        lastCheckTime.put(playerId, currentTime);

        // Получаем текущий регион
        Location to = event.getTo();
        if (to == null) return;

        ProtectedRegion currentRegion = plugin.getRegionManager().getRegionAtLocation(to);
        ProtectedRegion previousRegion = playerRegions.get(playerId);

        // Проверяем изменение региона
        if (currentRegion == null && previousRegion == null) {
            return; // Игрок не в регионе и не был в регионе
        }

        if (currentRegion != null && previousRegion != null &&
                currentRegion.getId().equals(previousRegion.getId())) {
            return; // Игрок в том же регионе
        }

        // Игрок вышел из региона
        if (previousRegion != null && (currentRegion == null ||
                !currentRegion.getId().equals(previousRegion.getId()))) {
            handleRegionExit(player, previousRegion);
        }

        // Игрок вошел в новый регион
        if (currentRegion != null && (previousRegion == null ||
                !currentRegion.getId().equals(previousRegion.getId()))) {
            handleRegionEnter(player, currentRegion);
        }

        // Обновляем кэш
        playerRegions.put(playerId, currentRegion);
    }

    /**
     * Обработка входа в регион
     */
    private void handleRegionEnter(Player player, ProtectedRegion region) {
        try {
            String ownerName = Bukkit.getOfflinePlayer(region.getOwner()).getName();
            if (ownerName == null) ownerName = "Неизвестно";

            // Определяем цвет в зависимости от владельца
            boolean isOwner = region.isOwner(player.getUniqueId());
            boolean isMember = region.isMember(player.getUniqueId());

            ChatColor regionColor;
            if (isOwner) {
                regionColor = ChatColor.GREEN;
            } else if (isMember) {
                regionColor = ChatColor.YELLOW;
            } else {
                regionColor = ChatColor.RED;
            }

            // Отправляем ActionBar
            String actionBarMessage = ChatColor.translateAlternateColorCodes('&',
                    "&7» " + regionColor + "Вы вошли в регион &6" + ownerName
            );
            player.sendActionBar(actionBarMessage);

            // Отправляем Title (короткий)
            String titleMessage = ChatColor.translateAlternateColorCodes('&',
                    regionColor + "✖ " + ownerName + " ✖"
            );
            String subtitleMessage = ChatColor.translateAlternateColorCodes('&',
                    isOwner ? "&aВаш регион" : (isMember ? "&eВы участник" : "&cЧужой регион")
            );

            if (plugin.getConfig().getBoolean("messages.show-titles", true)) {
                player.sendTitle(titleMessage, subtitleMessage, 10, 40, 10);
            }

            // Показываем границы частицами
            if (plugin.getConfig().getBoolean("particles.show-on-enter", true)) {
                Color particleColor = isOwner ? Color.GREEN : (isMember ? Color.YELLOW : Color.RED);
                visualizer.showRegionBordersWithColor(
                        region.getLocation(),
                        region.getRadius(),
                        player,
                        5, // 5 секунд
                        particleColor
                );
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при обработке входа в регион: " + e.getMessage());
        }
    }

    /**
     * Обработка выхода из региона
     */
    private void handleRegionExit(Player player, ProtectedRegion region) {
        try {
            String ownerName = Bukkit.getOfflinePlayer(region.getOwner()).getName();
            if (ownerName == null) ownerName = "Неизвестно";

            // Отправляем ActionBar
            String actionBarMessage = ChatColor.translateAlternateColorCodes('&',
                    "&7» " + ChatColor.RED + "Вы вышли из региона &6" + ownerName
            );
            player.sendActionBar(actionBarMessage);

        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при обработке выхода из региона: " + e.getMessage());
        }
    }

    /**
     * Очистка неактивных игроков из кэша
     */
    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();

                // Очищаем игроков, которые давно не двигались (5 минут)
                lastCheckTime.entrySet().removeIf(entry ->
                        (currentTime - entry.getValue()) > 300000
                );

                // Удаляем из кэша регионов тех, кого нет в кэше времени
                playerRegions.keySet().retainAll(lastCheckTime.keySet());
            }
        }.runTaskTimer(plugin, 6000L, 6000L); // Каждые 5 минут
    }

    /**
     * Удалить игрока из кэша (вызывается при выходе игрока)
     */
    public void removePlayer(UUID playerId) {
        playerRegions.remove(playerId);
        lastCheckTime.remove(playerId);
    }
}