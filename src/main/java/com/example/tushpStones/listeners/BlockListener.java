package com.example.tushpStones.listeners;

import com.example.tushpStones.TushpStones;
import com.example.tushpStones.models.ProtectionBlock;
import com.example.tushpStones.models.ProtectedRegion;
import com.example.tushpStones.utils.ParticleVisualizer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Обработчик событий установки и разрушения блоков
 * Добавлена система улучшения прочности на Shift+ПКМ
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

        // Информация о прочности если включена
        if (protectionBlock.isHealthEnabled()) {
            player.sendMessage(ChatColor.GREEN + "╠════════════════════════════╣");
            player.sendMessage(ChatColor.GREEN + "║ " + ChatColor.WHITE + "Прочность: " + ChatColor.YELLOW +
                    protectionBlock.getDefaultHealth() + "/" + protectionBlock.getMaxHealth() + ChatColor.GREEN + "      ║");
            player.sendMessage(ChatColor.GREEN + "║ " + ChatColor.GRAY + "Улучшение: " +
                    protectionBlock.getUpgradeItem().name().toLowerCase() + ChatColor.GREEN + "     ║");
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
     * Обработка улучшения прочности региона (Shift + ПКМ)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Проверяем что это ПКМ по блоку
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        Player player = event.getPlayer();

        // Проверяем что игрок приседает
        if (!player.isSneaking()) {
            return;
        }

        // Проверяем что это блок привата
        if (!plugin.getConfigManager().isProtectionBlock(block.getType())) {
            return;
        }

        // Получаем регион
        ProtectedRegion region = plugin.getRegionManager().getRegionAtLocation(block.getLocation());
        if (region == null) {
            return;
        }

        // Проверяем что кликнули по центральному блоку
        if (!isCenterBlock(block.getLocation(), region.getLocation())) {
            return;
        }

        // Проверка прав на улучшение
        if (!region.isOwner(player.getUniqueId()) && !player.hasPermission("tushpstones.admin")) {
            player.sendMessage(ChatColor.RED + "Только владелец может улучшать регион!");
            event.setCancelled(true);
            return;
        }

        // Проверяем что система прочности включена
        if (!region.isHealthEnabled()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.not-upgradeable", "&cЭтот регион нельзя улучшить!")));
            event.setCancelled(true);
            return;
        }

        // Проверяем что прочность не максимальна
        if (!region.canUpgrade()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.health-max", "&cПрочность уже максимальная!")));
            event.setCancelled(true);
            return;
        }

        // Получаем конфиг блока
        ProtectionBlock protectionBlock = plugin.getConfigManager().getProtectionBlock(block.getType());
        if (protectionBlock == null) {
            return;
        }

        // Получаем предмет в руке
        ItemStack itemInHand = event.getItem();
        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            return;
        }

        // Проверяем что это правильный предмет для улучшения
        if (!protectionBlock.isValidUpgradeItem(itemInHand.getType())) {
            String upgradeItemName = protectionBlock.getUpgradeItem().name().toLowerCase().replace("_", " ");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.wrong-upgrade-item", "&cДля улучшения нужен: &6{item}")
                            .replace("{item}", upgradeItemName)));
            event.setCancelled(true);
            return;
        }

        // Проверяем количество предметов
        int cost = protectionBlock.getCostPerUpgrade();
        if (itemInHand.getAmount() < cost) {
            player.sendMessage(ChatColor.RED + "Недостаточно предметов! Нужно: " + cost);
            event.setCancelled(true);
            return;
        }

        // Улучшаем прочность
        int upgradeAmount = protectionBlock.getUpgradeAmount();
        int actualUpgraded = region.upgrade(upgradeAmount);

        if (actualUpgraded > 0) {
            // Удаляем предметы
            itemInHand.setAmount(itemInHand.getAmount() - cost);

            // Обновляем голограмму
            plugin.getRegionManager().updateHologram(region.getId());

            // Сохраняем изменения
            plugin.getRegionManager().saveRegions();

            // Эффекты улучшения
            player.spawnParticle(Particle.HAPPY_VILLAGER, block.getLocation().add(0.5, 1.5, 0.5), 10, 0.3, 0.3, 0.3, 0);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

            // Сообщение об успехе
            String message = ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.health-upgraded", "&a✓ Прочность улучшена! &7[&6{current}&7/&6{max}&7]")
                            .replace("{current}", String.valueOf(region.getCurrentHealth()))
                            .replace("{max}", String.valueOf(region.getMaxHealth())));
            player.sendMessage(message);
        }

        event.setCancelled(true);
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