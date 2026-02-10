package com.example.tushpStones.utils;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Визуализация границ региона частицами
 */
public class ParticleVisualizer {

    private final Plugin plugin;

    public ParticleVisualizer(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Показать границы куба частицами на 5 секунд
     * 
     * @param center Центр региона
     * @param radius Радиус региона
     * @param player Игрок, который видит частицы
     */
    public void showRegionBorders(Location center, int radius, Player player) {
        showRegionBorders(center, radius, player, 5); // 5 секунд по умолчанию
    }

    /**
     * Показать границы куба частицами
     * 
     * @param center Центр региона
     * @param radius Радиус региона
     * @param player Игрок, который видит частицы
     * @param duration Длительность в секундах
     */
    public void showRegionBorders(Location center, int radius, Player player, int duration) {
        World world = center.getWorld();
        if (world == null) return;

        // Цвет частиц (зеленый для нового региона)
        Particle.DustOptions dustOptions = new Particle.DustOptions(
            Color.fromRGB(0, 255, 0), // Зеленый
            0.75f // Размер частицы
        );

        // Создаем анимированную задачу
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = duration * 20; // Конвертируем секунды в тики (20 тиков = 1 сек)
            
            @Override
            public void run() {
                if (ticks >= maxTicks || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                // Рисуем куб из частиц
                drawCubeEdges(center, radius, dustOptions, player);
                
                ticks += 10; // Обновляем каждые 10 тиков (0.5 сек)
            }
        }.runTaskTimer(plugin, 0L, 10L); // Запуск каждые 10 тиков
    }

    /**
     * Показать границы с кастомным цветом
     */
    public void showRegionBordersWithColor(Location center, int radius, Player player, 
                                          int duration, Color color) {
        World world = center.getWorld();
        if (world == null) return;

        Particle.DustOptions dustOptions = new Particle.DustOptions(color, 0.75f);

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = duration * 20;
            
            @Override
            public void run() {
                if (ticks >= maxTicks || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                drawCubeEdges(center, radius, dustOptions, player);
                ticks += 10;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    /**
     * Рисует рёбра куба частицами
     */
    private void drawCubeEdges(Location center, int radius, Particle.DustOptions dustOptions, Player player) {
        double x = center.getX();
        double y = center.getY();
        double z = center.getZ();

        // 8 вершин куба
        Location[] corners = {
            new Location(center.getWorld(), x - radius, y - radius, z - radius),
            new Location(center.getWorld(), x + radius, y - radius, z - radius),
            new Location(center.getWorld(), x + radius, y - radius, z + radius),
            new Location(center.getWorld(), x - radius, y - radius, z + radius),
            new Location(center.getWorld(), x - radius, y + radius, z - radius),
            new Location(center.getWorld(), x + radius, y + radius, z - radius),
            new Location(center.getWorld(), x + radius, y + radius, z + radius),
            new Location(center.getWorld(), x - radius, y + radius, z + radius)
        };

        // Рёбра куба (соединения между вершинами)
        int[][] edges = {
            // Нижнее основание
            {0, 1}, {1, 2}, {2, 3}, {3, 0},
            // Верхнее основание
            {4, 5}, {5, 6}, {6, 7}, {7, 4},
            // Вертикальные рёбра
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };

        // Рисуем каждое ребро
        for (int[] edge : edges) {
            drawLine(corners[edge[0]], corners[edge[1]], dustOptions, player, 0.5);
        }

        // Добавляем угловые акценты (более яркие частицы в углах)
        for (Location corner : corners) {
            player.spawnParticle(
                Particle.FIREWORK,
                corner, 
                3, // Больше частиц в углах
                0.1, 0.1, 0.1, // Небольшой разброс
                dustOptions
            );
        }
    }

    /**
     * Рисует линию частицами между двумя точками
     */
    private void drawLine(Location start, Location end, Particle.DustOptions dustOptions, 
                         Player player, double spacing) {
        double distance = start.distance(end);
        int particles = (int) (distance / spacing);

        double deltaX = (end.getX() - start.getX()) / particles;
        double deltaY = (end.getY() - start.getY()) / particles;
        double deltaZ = (end.getZ() - start.getZ()) / particles;

        for (int i = 0; i <= particles; i++) {
            Location point = new Location(
                start.getWorld(),
                start.getX() + (deltaX * i),
                start.getY() + (deltaY * i),
                start.getZ() + (deltaZ * i)
            );

            player.spawnParticle(Particle.FIREWORK, point, 1, 0, 0, 0, dustOptions);
        }
    }

    /**
     * Показать границы региона всем игрокам в радиусе
     */
    public void showRegionBordersToNearby(Location center, int radius, int viewDistance) {
        World world = center.getWorld();
        if (world == null) return;

        world.getPlayers().forEach(player -> {
            if (player.getLocation().distance(center) <= viewDistance) {
                showRegionBorders(center, radius, player, 5);
            }
        });
    }

    /**
     * Красивая анимация "пульсации" при создании региона
     */
    public void showCreationAnimation(Location center, int radius, Player player) {
        World world = center.getWorld();
        if (world == null) return;

        new BukkitRunnable() {
            int tick = 0;
            final int maxTicks = 60; // 3 секунды
            
            @Override
            public void run() {
                if (tick >= maxTicks || !player.isOnline()) {
                    // После анимации показываем обычные границы
                    showRegionBorders(center, radius, player, 2);
                    this.cancel();
                    return;
                }

                // Эффект "волны" - расширяющийся круг частиц
                double progress = (double) tick / maxTicks;
                int currentRadius = (int) (radius * progress);
                
                // Меняем цвет от синего к зеленому
                int red = (int) (0 * (1 - progress) + 0 * progress);
                int green = (int) (150 * (1 - progress) + 255 * progress);
                int blue = (int) (255 * (1 - progress) + 0 * progress);
                
                Color color = Color.fromRGB(red, green, blue);
                Particle.DustOptions dustOptions = new Particle.DustOptions(color, 1.0f);

                // Рисуем горизонтальный круг на уровне центра
                drawHorizontalCircle(center, currentRadius, dustOptions, player);
                
                // Дополнительные эффекты
                if (tick % 5 == 0) {
                    // Вспышки в центре
                    player.spawnParticle(
                        Particle.HAPPY_VILLAGER,
                        center,
                        5,
                        0.5, 0.5, 0.5,
                        0
                    );
                }

                tick += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /**
     * Рисует горизонтальный круг частицами
     */
    private void drawHorizontalCircle(Location center, int radius, 
                                     Particle.DustOptions dustOptions, Player player) {
        int points = radius * 8; // Больше точек для больших кругов
        
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            
            Location point = new Location(center.getWorld(), x, center.getY(), z);
            player.spawnParticle(Particle.FIREWORK, point, 1, 0, 0, 0, dustOptions);
            
            // Дополнительные круги выше и ниже
            Location pointUp = point.clone().add(0, radius, 0);
            Location pointDown = point.clone().subtract(0, radius, 0);
            
            player.spawnParticle(Particle.FIREWORK, pointUp, 1, 0, 0, 0, dustOptions);
            player.spawnParticle(Particle.FIREWORK, pointDown, 1, 0, 0, 0, dustOptions);
        }
    }

    /**
     * Показать границы при взрыве региона (красным цветом)
     */
    public void showDestructionAnimation(Location center, int radius, Player player) {
        World world = center.getWorld();
        if (world == null) return;

        // Красный цвет для разрушения
        Color redColor = Color.fromRGB(255, 0, 0);
        Particle.DustOptions dustOptions = new Particle.DustOptions(redColor, 1.0f);

        new BukkitRunnable() {
            int tick = 0;
            final int maxTicks = 40; // 2 секунды
            
            @Override
            public void run() {
                if (tick >= maxTicks || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                // Эффект "разрушения" - сжимающийся куб
                double progress = 1.0 - ((double) tick / maxTicks);
                int currentRadius = (int) (radius * progress);
                
                if (currentRadius > 0) {
                    drawCubeEdges(center, currentRadius, dustOptions, player);
                }
                
                // Дополнительные эффекты взрыва
                if (tick % 3 == 0) {
                    player.spawnParticle(
                        Particle.LARGE_SMOKE,
                        center,
                        10,
                        radius / 2.0, radius / 2.0, radius / 2.0,
                        0.02
                    );
                }

                tick += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}
