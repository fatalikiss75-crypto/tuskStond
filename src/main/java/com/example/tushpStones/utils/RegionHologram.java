package com.example.tushpStones.utils;

import com.example.tushpStones.models.ProtectedRegion;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Голограмма над регионом с информацией о прочности
 */
public class RegionHologram {

    private final Plugin plugin;
    private final ProtectedRegion region;
    private ArmorStand hologramStand;
    private BukkitRunnable updateTask;

    // Цвета для прогресс-бара
    private static final String COLOR_FULL = "&a";
    private static final String COLOR_HALF = "&e";
    private static final String COLOR_LOW = "&c";
    private static final String COLOR_EMPTY = "&7";

    public RegionHologram(Plugin plugin, ProtectedRegion region) {
        this.plugin = plugin;
        this.region = region;
    }

    /**
     * Создать голограмму над регионом
     */
    public void create() {
        try {
            World world = region.getLocation().getWorld();
            if (world == null) return;

            // Удаляем старую голограмму если есть
            remove();

            // Позиция голограммы - на 2.5 блока выше центра
            Location hologramLoc = region.getLocation().clone().add(0.5, 2.5, 0.5);

            // Создаем ArmorStand
            hologramStand = (ArmorStand) world.spawnEntity(hologramLoc, EntityType.ARMOR_STAND);
            hologramStand.setVisible(false);
            hologramStand.setCustomNameVisible(true);
            hologramStand.setGravity(false);
            hologramStand.setMarker(true);
            hologramStand.setSmall(true);
            hologramStand.setInvulnerable(true);

            // Обновляем текст
            updateText();

            // Запускаем периодическое обновление
            startUpdateTask();

        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка создания голограммы: " + e.getMessage());
        }
    }

    /**
     * Удалить голограмму
     */
    public void remove() {
        try {
            if (updateTask != null) {
                updateTask.cancel();
                updateTask = null;
            }

            if (hologramStand != null && !hologramStand.isDead()) {
                hologramStand.remove();
                hologramStand = null;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка удаления голограммы: " + e.getMessage());
        }
    }

    /**
     * Обновить текст голограммы
     */
    public void update() {
        updateText();
    }

    /**
     * Обновить текст голограммы
     */
    private void updateText() {
        if (hologramStand == null || hologramStand.isDead()) return;

        String ownerName = getOwnerName();
        String healthBar = createHealthBar();

        String displayText = ChatColor.translateAlternateColorCodes('&',
                "&7[&6Регион: &e" + ownerName + "&7] &r" + healthBar
        );

        hologramStand.setCustomName(displayText);
    }

    /**
     * Запустить задачу периодического обновления
     */
    private void startUpdateTask() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (hologramStand == null || hologramStand.isDead()) {
                    this.cancel();
                    return;
                }
                updateText();
            }
        };
        // Обновляем каждые 20 тиков (1 секунда)
        updateTask.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * Получить имя владельца
     */
    private String getOwnerName() {
        String name = plugin.getServer().getOfflinePlayer(region.getOwner()).getName();
        return name != null ? name : "Неизвестно";
    }

    /**
     * Создать визуальный прогресс-бар здоровья
     */
    private String createHealthBar() {
        int current = region.getCurrentHealth();
        int max = region.getMaxHealth();

        if (max <= 0) return "";

        // Если система прочности отключена для этого региона
        if (!region.isHealthEnabled()) {
            return ChatColor.translateAlternateColorCodes('&', "&a✓ Защищено");
        }

        int percentage = (int) ((double) current / max * 100);
        int bars = 10; // Количество сегментов в полоске
        int filledBars = (int) ((double) current / max * bars);

        StringBuilder bar = new StringBuilder();
        bar.append(getHealthColor(percentage));

        // Символы для прогресс-бара
        for (int i = 0; i < bars; i++) {
            if (i < filledBars) {
                bar.append("█");
            } else {
                bar.append(COLOR_EMPTY).append("░");
            }
        }

        bar.append(" &7[").append(current).append("&c/").append(max).append("&7]");

        return ChatColor.translateAlternateColorCodes('&', bar.toString());
    }

    /**
     * Получить цвет в зависимости от процента здоровья
     */
    private String getHealthColor(int percentage) {
        if (percentage > 60) {
            return COLOR_FULL;
        } else if (percentage > 30) {
            return COLOR_HALF;
        } else {
            return COLOR_LOW;
        }
    }

    /**
     * Получить ArmorStand голограммы
     */
    public ArmorStand getHologramStand() {
        return hologramStand;
    }
}