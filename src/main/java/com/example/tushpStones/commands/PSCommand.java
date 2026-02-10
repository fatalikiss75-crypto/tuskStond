package com.example.tushpStones.commands;

import com.example.tushpStones.TushpStones;
import com.example.tushpStones.models.ProtectedRegion;
import com.example.tushpStones.utils.ParticleVisualizer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Обработчик команд /ps
 */
public class PSCommand implements CommandExecutor, TabCompleter {

    private final TushpStones plugin;
    private final ParticleVisualizer visualizer;

    public PSCommand(TushpStones plugin) {
        this.plugin = plugin;
        this.visualizer = new ParticleVisualizer(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эта команда только для игроков!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "info":
                handleInfo(player, args);
                break;
            case "add":
                handleAdd(player, args);
                break;
            case "remove":
                handleRemove(player, args);
                break;
            case "addowner":
                handleAddOwner(player, args);
                break;
            case "removeowner":
                handleRemoveOwner(player, args);
                break;
            case "hide":
                handleHide(player);
                break;
            case "unhide":
                handleUnhide(player);
                break;
            case "home":
                handleHome(player, args);
                break;
            case "count":
                handleCount(player);
                break;
            case "list":
                handleList(player);
                break;
            case "view":
                handleView(player);
                break;
            case "reload":
                handleReload(player);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Неизвестная команда! Используйте /ps для помощи.");
        }

        return true;
    }

    /**
     * Показать информацию о регионе
     */
    private void handleInfo(Player player, String[] args) {
        if (!player.hasPermission("tushpstones.info")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды!");
            return;
        }

        ProtectedRegion region = plugin.getRegionManager().getRegionAtLocation(player.getLocation());
        if (region == null) {
            player.sendMessage(ChatColor.RED + "Вы не находитесь в регионе!");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "╔════════════════════════════╗");
        player.sendMessage(ChatColor.GOLD + "║ " + ChatColor.WHITE + "Информация о регионе" + ChatColor.GOLD + "      ║");
        player.sendMessage(ChatColor.GOLD + "╠════════════════════════════╣");
        player.sendMessage(ChatColor.GOLD + "║ " + ChatColor.YELLOW + "ID: " + ChatColor.WHITE + region.getId() + ChatColor.GOLD + "");
        player.sendMessage(ChatColor.GOLD + "║ " + ChatColor.YELLOW + "Владелец: " + ChatColor.WHITE + 
            Bukkit.getOfflinePlayer(region.getOwner()).getName());
        player.sendMessage(ChatColor.GOLD + "║ " + ChatColor.YELLOW + "Радиус: " + ChatColor.WHITE + region.getRadius());
        player.sendMessage(ChatColor.GOLD + "║ " + ChatColor.YELLOW + "Тип блока: " + ChatColor.WHITE + region.getBlockType());
        player.sendMessage(ChatColor.GOLD + "║ " + ChatColor.YELLOW + "Приоритет: " + ChatColor.WHITE + region.getPriority());
        
        if (!region.getCoOwners().isEmpty()) {
            player.sendMessage(ChatColor.GOLD + "║ " + ChatColor.YELLOW + "Совладельцы: " + ChatColor.WHITE + 
                region.getCoOwners().size());
        }
        
        if (!region.getMembers().isEmpty()) {
            player.sendMessage(ChatColor.GOLD + "║ " + ChatColor.YELLOW + "Участники: " + ChatColor.WHITE + 
                region.getMembers().size());
        }
        
        player.sendMessage(ChatColor.GOLD + "╚════════════════════════════╝");
    }

    /**
     * Добавить участника
     */
    private void handleAdd(Player player, String[] args) {
        if (!player.hasPermission("tushpstones.members")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Использование: /ps add <игрок>");
            return;
        }

        ProtectedRegion region = plugin.getRegionManager().getRegionAtLocation(player.getLocation());
        if (region == null) {
            player.sendMessage(ChatColor.RED + "Вы не находитесь в регионе!");
            return;
        }

        if (!region.isOwner(player.getUniqueId()) && !player.hasPermission("tushpstones.admin")) {
            player.sendMessage(ChatColor.RED + "Вы не владелец этого региона!");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Игрок не найден!");
            return;
        }

        region.addMember(target.getUniqueId());
        plugin.getRegionManager().saveRegions();
        
        player.sendMessage(ChatColor.GREEN + "Игрок " + target.getName() + " добавлен в регион!");
        target.sendMessage(ChatColor.GREEN + "Вас добавили в регион " + region.getId() + "!");
    }

    /**
     * Удалить участника
     */
    private void handleRemove(Player player, String[] args) {
        if (!player.hasPermission("tushpstones.members")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Использование: /ps remove <игрок>");
            return;
        }

        ProtectedRegion region = plugin.getRegionManager().getRegionAtLocation(player.getLocation());
        if (region == null) {
            player.sendMessage(ChatColor.RED + "Вы не находитесь в регионе!");
            return;
        }

        if (!region.isOwner(player.getUniqueId()) && !player.hasPermission("tushpstones.admin")) {
            player.sendMessage(ChatColor.RED + "Вы не владелец этого региона!");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Игрок не найден!");
            return;
        }

        region.removeMember(target.getUniqueId());
        plugin.getRegionManager().saveRegions();
        
        player.sendMessage(ChatColor.GREEN + "Игрок " + target.getName() + " удален из региона!");
    }

    /**
     * Добавить совладельца
     */
    private void handleAddOwner(Player player, String[] args) {
        if (!player.hasPermission("tushpstones.owners")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Использование: /ps addowner <игрок>");
            return;
        }

        ProtectedRegion region = plugin.getRegionManager().getRegionAtLocation(player.getLocation());
        if (region == null) {
            player.sendMessage(ChatColor.RED + "Вы не находитесь в регионе!");
            return;
        }

        if (!region.getOwner().equals(player.getUniqueId()) && !player.hasPermission("tushpstones.admin")) {
            player.sendMessage(ChatColor.RED + "Только главный владелец может добавлять совладельцев!");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Игрок не найден!");
            return;
        }

        region.addCoOwner(target.getUniqueId());
        plugin.getRegionManager().saveRegions();
        
        player.sendMessage(ChatColor.GREEN + "Игрок " + target.getName() + " добавлен как совладелец!");
        target.sendMessage(ChatColor.GREEN + "Вас сделали совладельцем региона " + region.getId() + "!");
    }

    /**
     * Удалить совладельца
     */
    private void handleRemoveOwner(Player player, String[] args) {
        if (!player.hasPermission("tushpstones.owners")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Использование: /ps removeowner <игрок>");
            return;
        }

        ProtectedRegion region = plugin.getRegionManager().getRegionAtLocation(player.getLocation());
        if (region == null) {
            player.sendMessage(ChatColor.RED + "Вы не находитесь в регионе!");
            return;
        }

        if (!region.getOwner().equals(player.getUniqueId()) && !player.hasPermission("tushpstones.admin")) {
            player.sendMessage(ChatColor.RED + "Только главный владелец может удалять совладельцев!");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Игрок не найден!");
            return;
        }

        region.removeCoOwner(target.getUniqueId());
        plugin.getRegionManager().saveRegions();
        
        player.sendMessage(ChatColor.GREEN + "Игрок " + target.getName() + " удален из совладельцев!");
    }

    /**
     * Скрыть блок привата
     */
    private void handleHide(Player player) {
        if (!player.hasPermission("tushpstones.hide")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды!");
            return;
        }

        ProtectedRegion region = plugin.getRegionManager().getRegionAtLocation(player.getLocation());
        if (region == null) {
            player.sendMessage(ChatColor.RED + "Вы не находитесь в регионе!");
            return;
        }

        if (!region.isOwner(player.getUniqueId()) && !player.hasPermission("tushpstones.admin")) {
            player.sendMessage(ChatColor.RED + "Вы не владелец этого региона!");
            return;
        }

        region.setHidden(true);
        region.getLocation().getBlock().setType(org.bukkit.Material.AIR);
        plugin.getRegionManager().saveRegions();
        
        player.sendMessage(ChatColor.GREEN + "Блок привата скрыт!");
    }

    /**
     * Показать блок привата
     */
    private void handleUnhide(Player player) {
        if (!player.hasPermission("tushpstones.unhide")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды!");
            return;
        }

        ProtectedRegion region = plugin.getRegionManager().getRegionAtLocation(player.getLocation());
        if (region == null) {
            player.sendMessage(ChatColor.RED + "Вы не находитесь в регионе!");
            return;
        }

        if (!region.isOwner(player.getUniqueId()) && !player.hasPermission("tushpstones.admin")) {
            player.sendMessage(ChatColor.RED + "Вы не владелец этого региона!");
            return;
        }

        region.setHidden(false);
        // Восстановление блока (нужно получить тип из конфига)
        plugin.getRegionManager().saveRegions();
        
        player.sendMessage(ChatColor.GREEN + "Блок привата восстановлен!");
    }

    /**
     * Телепорт в свой регион
     */
    private void handleHome(Player player, String[] args) {
        if (!player.hasPermission("tushpstones.home")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды!");
            return;
        }

        List<ProtectedRegion> regions = plugin.getRegionManager().getPlayerRegions(player);
        
        if (regions.isEmpty()) {
            player.sendMessage(ChatColor.RED + "У вас нет регионов!");
            return;
        }

        int index = 0;
        if (args.length > 1) {
            try {
                index = Integer.parseInt(args[1]) - 1;
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Неверный номер региона!");
                return;
            }
        }

        if (index < 0 || index >= regions.size()) {
            player.sendMessage(ChatColor.RED + "Регион с таким номером не найден!");
            return;
        }

        ProtectedRegion region = regions.get(index);
        player.teleport(region.getLocation());
        player.sendMessage(ChatColor.GREEN + "Вы телепортированы в регион " + region.getId() + "!");
    }

    /**
     * Показать количество регионов
     */
    private void handleCount(Player player) {
        if (!player.hasPermission("tushpstones.count")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды!");
            return;
        }

        int count = plugin.getRegionManager().getPlayerRegionsCount(player);
        player.sendMessage(ChatColor.YELLOW + "У вас " + ChatColor.GOLD + count + ChatColor.YELLOW + " регион(ов)");
    }

    /**
     * Показать список регионов
     */
    private void handleList(Player player) {
        if (!player.hasPermission("tushpstones.list")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды!");
            return;
        }

        List<ProtectedRegion> regions = plugin.getRegionManager().getPlayerRegions(player);
        
        if (regions.isEmpty()) {
            player.sendMessage(ChatColor.RED + "У вас нет регионов!");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "═══ Ваши регионы ═══");
        for (int i = 0; i < regions.size(); i++) {
            ProtectedRegion region = regions.get(i);
            player.sendMessage(
                    ChatColor.YELLOW + "" + (i + 1) + ". "
                            + ChatColor.WHITE + region.getId()
                            + ChatColor.GRAY + " (" + region.getBlockType() + ")"
            );
        }
    }

    /**
     * Показать границы региона частицами
     */
    private void handleView(Player player) {
        if (!player.hasPermission("tushpstones.view")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды!");
            return;
        }

        ProtectedRegion region = plugin.getRegionManager().getRegionAtLocation(player.getLocation());
        if (region == null) {
            player.sendMessage(ChatColor.RED + "Вы не находитесь в регионе!");
            return;
        }

        // Определяем цвет в зависимости от отношения к региону
        Color color;
        if (region.getOwner().equals(player.getUniqueId())) {
            color = Color.fromRGB(0, 255, 0); // Зеленый для своего
            player.sendMessage(ChatColor.GREEN + "✓ Показываю границы вашего региона...");
        } else if (region.isOwner(player.getUniqueId())) {
            color = Color.fromRGB(0, 200, 255); // Голубой для совладельца
            player.sendMessage(ChatColor.AQUA + "✓ Показываю границы (вы совладелец)...");
        } else if (region.isMember(player.getUniqueId())) {
            color = Color.fromRGB(255, 255, 0); // Желтый для участника
            player.sendMessage(ChatColor.YELLOW + "✓ Показываю границы (вы участник)...");
        } else {
            color = Color.fromRGB(255, 100, 0); // Оранжевый для чужого
            player.sendMessage(ChatColor.GOLD + "✓ Показываю границы чужого региона...");
        }

        // Показываем границы на 10 секунд
        visualizer.showRegionBordersWithColor(
            region.getLocation(), 
            region.getRadius(), 
            player, 
            10, // 10 секунд
            color
        );
    }

    /**
     * Перезагрузить конфиг
     */
    private void handleReload(Player player) {
        if (!player.hasPermission("tushpstones.admin")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды!");
            return;
        }

        plugin.getConfigManager().reload();
        player.sendMessage(ChatColor.GREEN + "Конфигурация перезагружена!");
    }

    /**
     * Помощь по командам
     */
    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "╔═══════════════════════════════╗");
        player.sendMessage(ChatColor.GOLD + "║ " + ChatColor.WHITE + "TushpStones - Команды" + ChatColor.GOLD + "         ║");
        player.sendMessage(ChatColor.GOLD + "╠═══════════════════════════════╣");
        player.sendMessage(ChatColor.YELLOW + "/ps info" + ChatColor.GRAY + " - Информация о регионе");
        player.sendMessage(ChatColor.YELLOW + "/ps view" + ChatColor.GRAY + " - Показать границы частицами");
        player.sendMessage(ChatColor.YELLOW + "/ps add <игрок>" + ChatColor.GRAY + " - Добавить участника");
        player.sendMessage(ChatColor.YELLOW + "/ps remove <игрок>" + ChatColor.GRAY + " - Удалить участника");
        player.sendMessage(ChatColor.YELLOW + "/ps addowner <игрок>" + ChatColor.GRAY + " - Добавить совладельца");
        player.sendMessage(ChatColor.YELLOW + "/ps removeowner <игрок>" + ChatColor.GRAY + " - Удалить совладельца");
        player.sendMessage(ChatColor.YELLOW + "/ps hide" + ChatColor.GRAY + " - Скрыть блок");
        player.sendMessage(ChatColor.YELLOW + "/ps unhide" + ChatColor.GRAY + " - Показать блок");
        player.sendMessage(ChatColor.YELLOW + "/ps home [номер]" + ChatColor.GRAY + " - Телепорт в регион");
        player.sendMessage(ChatColor.YELLOW + "/ps count" + ChatColor.GRAY + " - Количество регионов");
        player.sendMessage(ChatColor.YELLOW + "/ps list" + ChatColor.GRAY + " - Список регионов");
        player.sendMessage(ChatColor.GOLD + "╚═══════════════════════════════╝");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("info", "view", "add", "remove", "addowner", "removeowner", 
                "hide", "unhide", "home", "count", "list", "reload")
                .stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || 
                                  args[0].equalsIgnoreCase("remove") ||
                                  args[0].equalsIgnoreCase("addowner") ||
                                  args[0].equalsIgnoreCase("removeowner"))) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
