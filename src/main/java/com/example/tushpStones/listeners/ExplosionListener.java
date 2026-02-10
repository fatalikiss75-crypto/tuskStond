package com.example.tushpStones.listeners;

import com.example.tushpStones.TushpStones;
import com.example.tushpStones.models.ProtectionBlock;
import com.example.tushpStones.models.ProtectedRegion;
import com.example.tushpStones.utils.ParticleVisualizer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Обработчик взрывов - управляет разрушением приватов динамитом
 * Система прочности: взрывы наносят урон вместо мгновенного удаления
 */
public class ExplosionListener implements Listener {

    private final TushpStones plugin;
    private final ParticleVisualizer visualizer;

    // Хранилище для лора TNT сущностей (UUID TNTPrimed -> тип TNT)
    private final Map<UUID, TNTType> tntTypes = new HashMap<>();

    // Типы TNT на основе лора
    private enum TNTType {
        NORMAL,           // Обычный TNT
        BLACK_TNT         // Особый TNT с лором "Этот динамит способен взрывать обсидиан и регионы"
    }

    public ExplosionListener(TushpStones plugin) {
        this.plugin = plugin;
        this.visualizer = new ParticleVisualizer(plugin);
    }

    /**
     * Перехват установки TNT для сохранения лора
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTNTPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block.getType() != Material.TNT) {
            return;
        }

        ItemStack item = event.getItemInHand();
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return;
        }

        List<String> lore = meta.getLore();
        if (lore == null) {
            return;
        }

        // Проверяем лор на наличие особой фразы
        for (String line : lore) {
            String plainLine = ChatColor.stripColor(line);
            if (plainLine.contains("Этот динамит способен взрывать обсидиан и регионы")) {
                // TNT с особым лором
                plugin.getLogger().info("Обнаружен особый TNT с лором!");
                // Сохраняем информацию в блоке
                block.setMetadata("special_tnt", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                break;
            }
        }
    }

    /**
     * Перехват активации TNT для получения типа TNT
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTNTIgnite(org.bukkit.event.block.BlockIgniteEvent event) {
        if (event.getBlock().getType() != Material.TNT) {
            return;
        }

        // Проверяем, есть ли метаданные особого TNT
        if (event.getBlock().hasMetadata("special_tnt")) {
            // TNT с особым лором
            plugin.getLogger().info("TNT с особым лором активирован");
            // Будем искать TNTPrimed сущность и отмечать её
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                event.getBlock().getLocation().getWorld().getNearbyEntities(
                        event.getBlock().getLocation().add(0.5, 0.5, 0.5), 1, 1, 1
                ).stream()
                        .filter(e -> e.getType() == EntityType.TNT)
                        .findFirst()
                        .ifPresent(entity -> {
                            tntTypes.put(entity.getUniqueId(), TNTType.BLACK_TNT);
                            plugin.getLogger().info("TNTPrimed помечен как BLACK_TNT");
                        });
            }, 1L);
        }
    }

    /**
     * Очистка устаревших записей о TNT
     * Выполняется с LOWEST приоритетом, чтобы очистить после всех обработчиков
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityExplodeCleanup(EntityExplodeEvent event) {
        if (event.getEntity() != null && event.getEntity().getType() == EntityType.TNT) {
            tntTypes.remove(event.getEntity().getUniqueId());
        }
    }

    /**
     * Очистка при удалении сущности (на всякий случай)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityRemove(org.bukkit.event.entity.EntityRemoveEvent event) {
        if (event.getEntity() != null && event.getEntity().getType() == EntityType.TNT) {
            tntTypes.remove(event.getEntity().getUniqueId());
        }
    }

    /**
     * Получить тип TNT по UUID сущности
     */
    private TNTType getTNTType(UUID tntUuid) {
        return tntTypes.getOrDefault(tntUuid, TNTType.NORMAL);
    }

    /**
     * Проверить, является ли TNT особым (с лором)
     */
    private boolean isSpecialTNT(EntityExplodeEvent event) {
        if (event.getEntity() == null || event.getEntity().getType() != EntityType.TNT) {
            return false;
        }

        return getTNTType(event.getEntity().getUniqueId()) == TNTType.BLACK_TNT;
    }

    /**
     * Обработка взрывов
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        // Получаем тип взрыва
        Material explosiveType = getExplosiveType(event.getEntityType());
        if (explosiveType == null) {
            return; // Не взрывчатка
        }

        // Проверяем, что это именно TNT
        if (explosiveType != Material.TNT) {
            return; // Разрешаем разрушать только TNT
        }

        Location explosionLocation = event.getLocation();
        Iterator<Block> blockIterator = event.blockList().iterator();

        while (blockIterator.hasNext()) {
            Block block = blockIterator.next();

            // Проверяем, является ли блок защитным
            if (!plugin.getConfigManager().isProtectionBlock(block.getType())) {
                continue;
            }

            // Получаем регион
            ProtectedRegion region = plugin.getRegionManager().getRegionAtLocation(block.getLocation());
            if (region == null) {
                continue;
            }

            // Проверяем, центральный ли это блок региона
            if (!isCenterBlock(block.getLocation(), region.getLocation())) {
                continue;
            }

            // Получаем конфиг блока привата
            ProtectionBlock protectionBlock = plugin.getConfigManager().getProtectionBlock(block.getType());
            if (protectionBlock == null) {
                continue;
            }

            // КЛЮЧЕВАЯ ЛОГИКА: Проверка, может ли этот тип динамита разрушить этот тип привата
            // Специальный TNT с лором может взрывать все регионы
            boolean isSpecial = isSpecialTNT(event);
            boolean canDestroy = isSpecial || protectionBlock.canBeDestroyedBy(explosiveType);

            if (!canDestroy) {
                // Удаляем блок из списка разрушаемых (защищаем)
                blockIterator.remove();

                // Опциональное сообщение (если взорвал игрок)
                Player bomber = getTNTPlacer(event);
                if (bomber != null && plugin.getConfig().getBoolean("explosion-messages", true)) {
                    if (isSpecial) {
                        bomber.sendMessage(ChatColor.RED + "⚠ Этот приват защищен!");
                    } else {
                        bomber.sendMessage(ChatColor.RED + "⚠ Этот приват требует особый динамит!");
                    }
                }

                continue;
            }

            // Блок МОЖЕТ быть разрушен этим типом взрывчатки
            // ═══════════════════════════════════════════════════════════
            // НОВАЯ СИСТЕМА: Наносим урон вместо мгновенного удаления
            // ═══════════════════════════════════════════════════════════

            Player bomber = getTNTPlacer(event);

            // Если включена система прочности - наносим урон
            if (region.isHealthEnabled()) {
                int explosionDamage = protectionBlock.getExplosionDamage(explosiveType);
                boolean isDestroyed = region.damage(explosionDamage);

                // Обновляем голограмму
                plugin.getRegionManager().updateHologram(region.getId());

                // Сохраняем изменения
                plugin.getRegionManager().saveRegions();

                // Уведомляем о повреждении
                notifyRegionDamaged(region, bomber, explosionDamage);

                // Удаляем блок из списка разрушаемых (мы контролируем разрушение сами)
                blockIterator.remove();

                // Если регион уничтожен
                if (isDestroyed) {
                    destroyRegion(region, bomber, explosiveType, block);
                }
            } else {
                // Старая система: мгновенное удаление
                boolean removed = plugin.getRegionManager().removeRegion(region.getId());

                if (removed) {
                    handleRegionDestruction(region, bomber, explosiveType);
                }
            }
        }
    }

    /**
     * Обработать уничтожение региона (новая система с прочностью)
     */
    private void destroyRegion(ProtectedRegion region, Player bomber, Material explosiveType, Block block) {
        try {
            // Удаляем регион
            plugin.getRegionManager().removeRegion(region.getId());

            // Разрушаем блок
            block.breakNaturally();

            // 🎨 АНИМАЦИЯ РАЗРУШЕНИЯ
            if (plugin.getConfig().getBoolean("show-particles-on-destruction", true)) {
                visualizer.showDestructionAnimation(
                        region.getLocation(),
                        region.getRadius(),
                        bomber
                );
            }

            // Уведомление владельца
            Player owner = plugin.getServer().getPlayer(region.getOwner());
            if (owner != null && owner.isOnline()) {
                sendRegionDestroyedMessage(owner, region, explosiveType);
            }

            // Уведомление взорвавшего
            if (bomber != null) {
                bomber.sendMessage(ChatColor.GREEN + "✓ Вы успешно уничтожили регион " +
                        ChatColor.GOLD + region.getId() + ChatColor.GREEN + "!");
            }

            // Логирование
            plugin.getLogger().info("Регион " + region.getId() + " был уничтожен с помощью " +
                    explosiveType.name());

        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при уничтожении региона: " + e.getMessage());
        }
    }

    /**
     * Обработать разрушение региона (старая система без прочности)
     */
    private void handleRegionDestruction(ProtectedRegion region, Player bomber, Material explosiveType) {
        // 🎨 АНИМАЦИЯ РАЗРУШЕНИЯ
        if (plugin.getConfig().getBoolean("show-particles-on-destruction", true)) {
            visualizer.showDestructionAnimation(
                    region.getLocation(),
                    region.getRadius(),
                    bomber
            );
        }

        // Уведомление владельца
        Player owner = plugin.getServer().getPlayer(region.getOwner());
        if (owner != null && owner.isOnline()) {
            sendRegionDestroyedMessage(owner, region, explosiveType);
        }

        // Уведомление взорвавшего
        if (bomber != null) {
            bomber.sendMessage(ChatColor.GREEN + "✓ Вы успешно взорвали регион " +
                    ChatColor.GOLD + region.getId() + ChatColor.GREEN + "!");
        }

        // Логирование
        plugin.getLogger().info("Регион " + region.getId() + " был взорван с помощью " +
                explosiveType.name());
    }

    /**
     * Отправить сообщение об уничтожении региона
     */
    private void sendRegionDestroyedMessage(Player owner, ProtectedRegion region, Material explosiveType) {
        owner.sendMessage(ChatColor.RED + "═══════════════════════════════");
        owner.sendMessage(ChatColor.DARK_RED + "✖ ВАШ РЕГИОН УНИЧТОЖЕН!");
        owner.sendMessage(ChatColor.RED + "Регион: " + ChatColor.YELLOW + region.getId());
        owner.sendMessage(ChatColor.RED + "Тип взрыва: " + ChatColor.YELLOW +
                explosiveType.name().toLowerCase().replace("_", " "));
        owner.sendMessage(ChatColor.RED + "═══════════════════════════════");

        // Звук тревоги
        owner.playSound(owner.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 0.5f);
    }

    /**
     * Уведомить о повреждении региона
     */
    private void notifyRegionDamaged(ProtectedRegion region, Player bomber, int damage) {
        try {
            // Уведомление владельца
            Player owner = plugin.getServer().getPlayer(region.getOwner());
            if (owner != null && owner.isOnline()) {
                String message = ChatColor.translateAlternateColorCodes('&',
                        "&c⚠ Регион получил урон! &7[&4" + region.getCurrentHealth() +
                                "&c/&6" + region.getMaxHealth() + "&7]"
                );
                owner.sendActionBar(message);

                // Звук предупреждения если здоровье низкое
                if (region.getHealthPercentage() < 30) {
                    owner.playSound(owner.getLocation(), Sound.ENTITY_IRON_GOLEM_DAMAGE, 1.0f, 0.8f);
                }
            }

            // Уведомление взорвавшего
            if (bomber != null) {
                String message = ChatColor.translateAlternateColorCodes('&',
                        "&a✓ Урон нанесен! &7[&6" + region.getCurrentHealth() +
                                "&7/&6" + region.getMaxHealth() + "&7]"
                );
                bomber.sendActionBar(message);
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при уведомлении о повреждении: " + e.getMessage());
        }
    }

    /**
     * Определение типа взрывчатки по типу сущности
     */
    private Material getExplosiveType(EntityType type) {
        switch (type) {
            case TNT:
                return Material.TNT;
            case CREEPER:
                return Material.CREEPER_HEAD; // Используем как идентификатор крипера
            case TNT_MINECART:
                return Material.TNT_MINECART;
            case WITHER_SKULL:
            case WITHER:
                return Material.WITHER_SKELETON_SKULL;
            case FIREBALL:
            case SMALL_FIREBALL:
                return Material.FIRE_CHARGE;
            case END_CRYSTAL:
                return Material.END_CRYSTAL;
            default:
                return null;
        }
    }

    /**
     * Попытка получить игрока, который установил TNT
     */
    private Player getTNTPlacer(EntityExplodeEvent event) {
        if (event.getEntity() instanceof TNTPrimed) {
            TNTPrimed tnt = (TNTPrimed) event.getEntity();
            if (tnt.getSource() instanceof Player) {
                return (Player) tnt.getSource();
            }
        }
        return null;
    }

    /**
     * Проверка, является ли блок центром региона
     */
    private boolean isCenterBlock(Location blockLoc, Location regionLoc) {
        return blockLoc.getBlockX() == regionLoc.getBlockX() &&
                blockLoc.getBlockY() == regionLoc.getBlockY() &&
                blockLoc.getBlockZ() == regionLoc.getBlockZ();
    }
}