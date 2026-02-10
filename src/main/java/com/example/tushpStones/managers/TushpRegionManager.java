package com.example.tushpStones.managers;

import com.example.tushpStones.TushpStones;
import com.example.tushpStones.models.ProtectedRegion;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TushpRegionManager {

    private final TushpStones plugin;
    private final Map<String, ProtectedRegion> regions = new HashMap<>();
    private File regionsFile;
    private FileConfiguration regionsConfig;
    public TushpRegionManager(TushpStones plugin) {
        this.plugin = plugin;
    }

    /**
     * Загрузка регионов из файла
     */
    public void loadRegions() {
        regionsFile = new File(plugin.getDataFolder(), "regions.yml");
        
        if (!regionsFile.exists()) {
            try {
                regionsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Не удалось создать regions.yml: " + e.getMessage());
                return;
            }
        }

        regionsConfig = YamlConfiguration.loadConfiguration(regionsFile);
        regions.clear();

        for (String key : regionsConfig.getKeys(false)) {
            try {
                ProtectedRegion region = ProtectedRegion.deserialize(regionsConfig.getConfigurationSection(key));
                if (region != null) {
                    regions.put(key, region);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка загрузки региона " + key + ": " + e.getMessage());
            }
        }

        plugin.getLogger().info("Загружено регионов: " + regions.size());
    }

    /**
     * Сохранение регионов
     */
    public void saveRegions() {
        regionsConfig = new YamlConfiguration();

        for (Map.Entry<String, ProtectedRegion> entry : regions.entrySet()) {
            regionsConfig.createSection(entry.getKey(), entry.getValue().serialize());
        }

        try {
            regionsConfig.save(regionsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить regions.yml: " + e.getMessage());
        }
    }

    /**
     * Создание нового региона
     */
    public boolean createRegion(Player player, Location location, String blockType, int radius, int priority) {
        String regionId = generateRegionId(player);
        
        // Проверка лимита регионов
        int currentRegions = getPlayerRegionsCount(player);
        int maxRegions = getMaxRegions(player);
        
        if (currentRegions >= maxRegions && maxRegions > 0) {
            return false;
        }

        World world = location.getWorld();
        if (world == null) return false;

        // Создание WorldGuard региона
        try {
            RegionManager wgManager = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .get(BukkitAdapter.adapt(world));

            if (wgManager == null) return false;

            BlockVector3 min = BlockVector3.at(
                    location.getBlockX() - radius,
                    location.getBlockY() - radius,
                    location.getBlockZ() - radius
            );

            BlockVector3 max = BlockVector3.at(
                    location.getBlockX() + radius,
                    location.getBlockY() + radius,
                    location.getBlockZ() + radius
            );

            ProtectedCuboidRegion wgRegion = new ProtectedCuboidRegion(regionId, min, max);
            wgRegion.setPriority(priority);
            wgRegion.getOwners().addPlayer(player.getUniqueId());


            wgManager.addRegion(wgRegion);

            // Сохранение в нашей системе
            ProtectedRegion region = new ProtectedRegion(
                regionId,
                player.getUniqueId(),
                location,
                radius,
                blockType,
                priority,
                new HashSet<>(),
                new HashSet<>()
            );

            regions.put(regionId, region);
            saveRegions();

            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка создания региона: " + e.getMessage());
            return false;
        }
    }

    /**
     * Удаление региона
     */
    public boolean removeRegion(String regionId) {
        ProtectedRegion region = regions.get(regionId);
        if (region == null) return false;

        World world = region.getLocation().getWorld();
        if (world == null) return false;

        try {
            RegionManager wgManager = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .get(BukkitAdapter.adapt(world));

            if (wgManager != null) {
                wgManager.removeRegion(regionId);
            }

            regions.remove(regionId);
            saveRegions();
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка удаления региона: " + e.getMessage());
            return false;
        }
    }

    /**
     * Получение региона по локации
     */
    public ProtectedRegion getRegionAtLocation(Location location) {
        for (ProtectedRegion region : regions.values()) {
            if (region.contains(location)) {
                return region;
            }
        }
        return null;
    }

    /**
     * Получение всех регионов игрока
     */
    public List<ProtectedRegion> getPlayerRegions(Player player) {
        return regions.values().stream()
            .filter(region -> region.getOwner().equals(player.getUniqueId()))
            .collect(Collectors.toList());
    }

    /**
     * Получение количества регионов игрока
     */
    public int getPlayerRegionsCount(Player player) {
        return (int) regions.values().stream()
            .filter(region -> region.getOwner().equals(player.getUniqueId()))
            .count();
    }

    /**
     * Генерация уникального ID региона
     */
    private String generateRegionId(Player player) {
        int count = getPlayerRegionsCount(player);
        return "ps_" + player.getName().toLowerCase() + "_" + count;
    }

    /**
     * Получение максимального количества регионов для игрока
     */
    private int getMaxRegions(Player player) {
        // Проверка на безлимитное право
        if (player.hasPermission("tushpstones.limit.unlimited")) {
            return -1; // -1 = без лимита
        }

        // Проверка специфичных лимитов
        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("tushpstones.limit." + i)) {
                return i;
            }
        }

        // Лимит по умолчанию из конфига
        return plugin.getConfig().getInt("default-region-limit", 3);
    }

    // Геттеры
    public Map<String, ProtectedRegion> getRegions() {
        return regions;
    }

    public ProtectedRegion getRegion(String id) {
        return regions.get(id);
    }
}
