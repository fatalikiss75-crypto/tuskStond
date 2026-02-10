package com.example.tushpStones.listeners;

import com.example.tushpStones.TushpStones;
import com.example.tushpStones.models.ProtectionBlock;
import com.example.tushpStones.models.ProtectedRegion;
import com.example.tushpStones.utils.ParticleVisualizer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Обработчик событий установки и разрушения блоков
 */
public class BlockListener implements Listener {

    private final TushpStones plugin;
    private final ParticleVisualizer visualizer;

    public BlockListener(TushpStones plugin) {
        this.plugin = plugin;
        this.visualizer = new ParticleVisualizer(plugin);
    }

    /**
     * Обработка установки блока
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        Material material = block.getType();

        // Проверяем, является ли блок защитным
        if (!plugin.getConfigManager().isProtectionBlock(material)) {
            return;
        }

        // Проверка прав на создание
        if (!player.hasPermission("tushpstones.create")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "У вас нет прав на создание приватов!");
            return;
        }

        ProtectionBlock protectionBlock = plugin.getConfigManager().getProtectionBlock(material);

        // Создание региона
        boolean success = plugin.getRegionManager().createRegion(
            player,
            block.getLocation(),
            protectionBlock.getId(),
            protectionBlock.getRadius(),
            protectionBlock.getPriority()
        );

        if (!success) {
            event.setCancelled(true);
            
            int current = plugin.getRegionManager().getPlayerRegionsCount(player);
            player.sendMessage(ChatColor.RED + "Не удалось создать регион!");
            player.sendMessage(ChatColor.RED + "Возможно, вы достигли лимита регионов: " + current);
            return;
        }

        // Успешное создание
        player.sendMessage(ChatColor.GREEN + "╔════════════════════════════╗");
        player.sendMessage(ChatColor.GREEN + "║ " + ChatColor.GOLD + "Регион успешно создан!" + ChatColor.GREEN + "     ║");
        player.sendMessage(ChatColor.GREEN + "╠════════════════════════════╣");
        player.sendMessage(ChatColor.GREEN + "║ " + ChatColor.WHITE + "Радиус: " + ChatColor.YELLOW + protectionBlock.getRadius() + " блоков" + ChatColor.GREEN + "    ║");
        player.sendMessage(ChatColor.GREEN + "║ " + ChatColor.WHITE + "Приоритет: " + ChatColor.YELLOW + protectionBlock.getPriority() + ChatColor.GREEN + "              ║");
        
        if (protectionBlock.canBeDestroyed()) {
            player.sendMessage(ChatColor.GREEN + "║ " + ChatColor.RED + "⚠ Может быть взорван!" + ChatColor.GREEN + "      ║");
        } else {
            player.sendMessage(ChatColor.GREEN + "║ " + ChatColor.AQUA + "✓ Защищен от взрывов" + ChatColor.GREEN + "      ║");
        }
        
        player.sendMessage(ChatColor.GREEN + "╚════════════════════════════╝");
        
        // 🎨 ВИЗУАЛИЗАЦИЯ ГРАНИЦ ЧАСТИЦАМИ!
        if (plugin.getConfig().getBoolean("show-particles-on-creation", true)) {
            player.sendMessage(ChatColor.GRAY + "» " + ChatColor.YELLOW + "Границы региона отображаются частицами...");
            visualizer.showCreationAnimation(block.getLocation(), protectionBlock.getRadius(), player);
        }
    }

    /**
     * Обработка разрушения блока
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Получаем регион в этой локации
        ProtectedRegion region = plugin.getRegionManager().getRegionAtLocation(block.getLocation());
        if (region == null) {
            return;
        }

        // Проверяем, является ли сломанный блок блоком привата
        Material material = block.getType();
        if (!plugin.getConfigManager().isProtectionBlock(material)) {
            return;
        }

        // Проверка, центр ли это региона
        if (!isCenterBlock(block.getLocation(), region.getLocation())) {
            return;
        }

        // Проверка прав на удаление
        if (!player.hasPermission("tushpstones.destroy")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "У вас нет прав на удаление приватов!");
            return;
        }

        // Проверка владения
        if (!region.isOwner(player.getUniqueId()) && !player.hasPermission("tushpstones.admin")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Это не ваш регион!");
            return;
        }

        // Удаление региона
        boolean removed = plugin.getRegionManager().removeRegion(region.getId());
        
        if (removed) {
            player.sendMessage(ChatColor.YELLOW + "Регион " + ChatColor.GOLD + region.getId() + ChatColor.YELLOW + " удален!");
        } else {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Ошибка при удалении региона!");
        }
    }

    /**
     * Проверка, является ли блок центром региона
     */
    private boolean isCenterBlock(org.bukkit.Location blockLoc, org.bukkit.Location regionLoc) {
        return blockLoc.getBlockX() == regionLoc.getBlockX() &&
               blockLoc.getBlockY() == regionLoc.getBlockY() &&
               blockLoc.getBlockZ() == regionLoc.getBlockZ();
    }
}
