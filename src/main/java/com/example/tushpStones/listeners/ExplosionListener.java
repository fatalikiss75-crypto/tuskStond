package com.example.tushpStones.listeners;

import com.example.tushpStones.TushpStones;
import com.example.tushpStones.models.ProtectionBlock;
import com.example.tushpStones.models.ProtectedRegion;
import com.example.tushpStones.utils.ParticleVisualizer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Iterator;

/**
 * Обработчик взрывов - управляет разрушением приватов динамитом
 */
public class ExplosionListener implements Listener {

    private final TushpStones plugin;
    private final ParticleVisualizer visualizer;

    public ExplosionListener(TushpStones plugin) {
        this.plugin = plugin;
        this.visualizer = new ParticleVisualizer(plugin);
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
            if (!protectionBlock.canBeDestroyedBy(explosiveType)) {
                // Удаляем блок из списка разрушаемых (защищаем)
                blockIterator.remove();
                
                // Опциональное сообщение (если взорвал игрок)
                Player bomber = getTNTPlacer(event);
                if (bomber != null && plugin.getConfig().getBoolean("explosion-messages", true)) {
                    bomber.sendMessage(ChatColor.RED + "⚠ Этот приват защищен от " + 
                        explosiveType.name().toLowerCase().replace("_", " ") + "!");
                }
                
                continue;
            }

            // Блок МОЖЕТ быть разрушен этим типом взрывчатки
            boolean removed = plugin.getRegionManager().removeRegion(region.getId());
            
            if (removed) {
                // 🎨 АНИМАЦИЯ РАЗРУШЕНИЯ
                Player bomber = getTNTPlacer(event);

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
                    owner.sendMessage(ChatColor.RED + "═══════════════════════════════");
                    owner.sendMessage(ChatColor.DARK_RED + "⚠ ВАШ РЕГИОН БЫЛ ВЗОРВАН!");
                    owner.sendMessage(ChatColor.RED + "Регион: " + ChatColor.YELLOW + region.getId());
                    owner.sendMessage(ChatColor.RED + "Тип взрыва: " + ChatColor.YELLOW + 
                        explosiveType.name().toLowerCase().replace("_", " "));
                    owner.sendMessage(ChatColor.RED + "═══════════════════════════════");
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
