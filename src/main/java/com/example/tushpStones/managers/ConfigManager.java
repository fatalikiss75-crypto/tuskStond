package com.example.tushpStones.managers;

import com.example.tushpStones.TushpStones;
import com.example.tushpStones.models.ProtectionBlock;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ConfigManager {

    private final TushpStones plugin;
    private FileConfiguration config;
    private FileConfiguration blocksConfig;
    private File blocksFile;

    private final Map<Material, ProtectionBlock> protectionBlocks = new HashMap<>();

    public ConfigManager(TushpStones plugin) {
        this.plugin = plugin;
    }

    /**
     * Загрузка всех конфигов
     */
    public void loadConfigs() {
        // Создаем config.yml
        plugin.saveDefaultConfig();
        config = plugin.getConfig();

        // Создаем blocks.yml
        createBlocksConfig();
        loadProtectionBlocks();

        plugin.getLogger().info("Конфигурация загружена! Блоков приватов: " + protectionBlocks.size());
    }

    /**
     * Создание blocks.yml
     */
    private void createBlocksConfig() {
        blocksFile = new File(plugin.getDataFolder(), "blocks.yml");

        if (!blocksFile.exists()) {
            plugin.saveResource("blocks.yml", false);
        }

        blocksConfig = YamlConfiguration.loadConfiguration(blocksFile);
    }

    /**
     * Загрузка блоков приватов из конфига
     */
    private void loadProtectionBlocks() {
        protectionBlocks.clear();

        ConfigurationSection blocksSection = blocksConfig.getConfigurationSection("blocks");
        if (blocksSection == null) {
            plugin.getLogger().warning("Секция 'blocks' не найдена в blocks.yml!");
            return;
        }

        for (String key : blocksSection.getKeys(false)) {
            ConfigurationSection blockSection = blocksSection.getConfigurationSection(key);
            if (blockSection == null) continue;

            try {
                Material material = Material.valueOf(blockSection.getString("material", "SPONGE"));
                int radius = blockSection.getInt("radius", 10);
                int priority = blockSection.getInt("priority", 0);
                boolean canBeDestroyed = blockSection.getBoolean("can-be-destroyed", false);

                // Загрузка динамитов, которые могут разрушить
                List<String> allowedExplosivesRaw = blockSection.getStringList("allowed-explosives");
                Set<Material> allowedExplosives = new HashSet<>();

                for (String explosive : allowedExplosivesRaw) {
                    try {
                        allowedExplosives.add(Material.valueOf(explosive.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Неизвестный материал взрывчатки: " + explosive);
                    }
                }

                // Загрузка флагов
                Map<String, Object> flags = new HashMap<>();
                ConfigurationSection flagsSection = blockSection.getConfigurationSection("flags");
                if (flagsSection != null) {
                    for (String flagKey : flagsSection.getKeys(false)) {
                        flags.put(flagKey, flagsSection.get(flagKey));
                    }
                }

                // Загрузка настроек прочности
                ConfigurationSection healthSection = blockSection.getConfigurationSection("health");
                boolean healthEnabled = false;
                int defaultHealth = 0;
                int maxHealth = 0;
                Material upgradeItem = null;
                int upgradeAmount = 0;
                int costPerUpgrade = 0;

                if (healthSection != null) {
                    healthEnabled = healthSection.getBoolean("enabled", false);
                    defaultHealth = healthSection.getInt("default", 100);
                    maxHealth = healthSection.getInt("max", 1000);

                    String upgradeItemStr = healthSection.getString("upgrade-item", "DIAMOND");
                    try {
                        upgradeItem = Material.valueOf(upgradeItemStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Неизвестный предмет для улучшения: " + upgradeItemStr);
                        upgradeItem = Material.DIAMOND;
                    }

                    upgradeAmount = healthSection.getInt("upgrade-amount", 50);
                    costPerUpgrade = healthSection.getInt("cost-per-upgrade", 1);
                }

                ProtectionBlock protectionBlock = new ProtectionBlock(
                        key, material, radius, priority, canBeDestroyed, allowedExplosives, flags,
                        healthEnabled, defaultHealth, maxHealth, upgradeItem, upgradeAmount, costPerUpgrade
                );

                protectionBlocks.put(material, protectionBlock);

            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка загрузки блока " + key + ": " + e.getMessage());
            }
        }
    }

    /**
     * Сохранение blocks.yml
     */
    public void saveBlocksConfig() {
        try {
            blocksConfig.save(blocksFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить blocks.yml: " + e.getMessage());
        }
    }

    /**
     * Перезагрузка конфигов
     */
    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        blocksConfig = YamlConfiguration.loadConfiguration(blocksFile);
        loadProtectionBlocks();
    }

    // Геттеры
    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getBlocksConfig() {
        return blocksConfig;
    }

    public Map<Material, ProtectionBlock> getProtectionBlocks() {
        return protectionBlocks;
    }

    public ProtectionBlock getProtectionBlock(Material material) {
        return protectionBlocks.get(material);
    }

    public boolean isProtectionBlock(Material material) {
        return protectionBlocks.containsKey(material);
    }
}