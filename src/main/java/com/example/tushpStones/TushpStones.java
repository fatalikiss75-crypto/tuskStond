package com.example.tushpStones;

import com.example.tushpStones.commands.PSCommand;
import com.example.tushpStones.listeners.BlockListener;
import com.example.tushpStones.listeners.ExplosionListener;
import com.example.tushpStones.listeners.PlayerMoveListener;
import com.example.tushpStones.managers.ConfigManager;
import com.example.tushpStones.managers.TushpRegionManager;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class TushpStones extends JavaPlugin implements Listener {

    private static TushpStones instance;
    private ConfigManager configManager;
    private TushpRegionManager tushpRegionManager;
    private PlayerMoveListener playerMoveListener;

    @Override
    public void onEnable() {
        instance = this;

        // Проверка WorldGuard
        if (!checkWorldGuard()) {
            getLogger().severe("WorldGuard не найден! Плагин отключается...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Инициализация менеджеров
        this.configManager = new ConfigManager(this);
        this.tushpRegionManager = new TushpRegionManager(this);

        // Загрузка конфигураций
        configManager.loadConfigs();
        tushpRegionManager.loadRegions();

        // Регистрация событий
        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginManager().registerEvents(new ExplosionListener(this), this);

        // Регистрация слушателя движения для уведомлений о входе/выходе
        this.playerMoveListener = new PlayerMoveListener(this);
        getServer().getPluginManager().registerEvents(playerMoveListener, this);

        // Регистрация общих событий плагина
        getServer().getPluginManager().registerEvents(this, this);

        // Регистрация команд
        PSCommand psCommand = new PSCommand(this);
        getCommand("ps").setExecutor(psCommand);
        getCommand("ps").setTabCompleter(psCommand);

        getLogger().info("TushpStones успешно загружен!");
        getLogger().info("Автор: Professional Developer");
        getLogger().info("Версия: " + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        // Сохранение всех данных
        if (tushpRegionManager != null) {
            tushpRegionManager.saveRegions();
            getLogger().info("Все регионы сохранены!");
        }

        getLogger().info("TushpStones отключен!");
    }

    /**
     * Обработка выхода игрока - очистка кэша
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (playerMoveListener != null) {
            playerMoveListener.removePlayer(event.getPlayer().getUniqueId());
        }
    }

    /**
     * Проверка наличия WorldGuard
     */
    private boolean checkWorldGuard() {
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            return getServer().getPluginManager().getPlugin("WorldGuard") != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    // Геттеры
    public static TushpStones getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public TushpRegionManager getRegionManager() {
        return tushpRegionManager;
    }
}