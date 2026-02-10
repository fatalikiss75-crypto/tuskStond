package com.example.tushpStones.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/**
 * Модель защищенного региона
 */
public class ProtectedRegion {

    private final String id;
    private final UUID owner;
    private final Location location;
    private final int radius;
    private final String blockType;
    private final int priority;
    private final Set<UUID> members;
    private final Set<UUID> coOwners;
    private boolean hidden;

    public ProtectedRegion(String id, UUID owner, Location location, int radius, 
                          String blockType, int priority, Set<UUID> members, Set<UUID> coOwners) {
        this.id = id;
        this.owner = owner;
        this.location = location;
        this.radius = radius;
        this.blockType = blockType;
        this.priority = priority;
        this.members = members;
        this.coOwners = coOwners;
        this.hidden = false;
    }

    /**
     * Проверка, находится ли локация в регионе
     */
    public boolean contains(Location loc) {
        if (!loc.getWorld().equals(location.getWorld())) {
            return false;
        }

        return loc.distance(location) <= radius;
    }

    /**
     * Проверка, является ли игрок владельцем
     */
    public boolean isOwner(UUID uuid) {
        return owner.equals(uuid) || coOwners.contains(uuid);
    }

    /**
     * Проверка, является ли игрок членом
     */
    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    /**
     * Проверка, имеет ли игрок доступ
     */
    public boolean hasAccess(UUID uuid) {
        return isOwner(uuid) || isMember(uuid);
    }

    /**
     * Добавление члена
     */
    public void addMember(UUID uuid) {
        members.add(uuid);
    }

    /**
     * Удаление члена
     */
    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    /**
     * Добавление совладельца
     */
    public void addCoOwner(UUID uuid) {
        coOwners.add(uuid);
    }

    /**
     * Удаление совладельца
     */
    public void removeCoOwner(UUID uuid) {
        coOwners.remove(uuid);
    }

    /**
     * Сериализация в ConfigurationSection
     */
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        
        map.put("id", id);
        map.put("owner", owner.toString());
        map.put("world", location.getWorld().getName());
        map.put("x", location.getX());
        map.put("y", location.getY());
        map.put("z", location.getZ());
        map.put("radius", radius);
        map.put("blockType", blockType);
        map.put("priority", priority);
        map.put("hidden", hidden);
        
        List<String> membersList = new ArrayList<>();
        for (UUID uuid : members) {
            membersList.add(uuid.toString());
        }
        map.put("members", membersList);
        
        List<String> coOwnersList = new ArrayList<>();
        for (UUID uuid : coOwners) {
            coOwnersList.add(uuid.toString());
        }
        map.put("coOwners", coOwnersList);
        
        return map;
    }

    /**
     * Десериализация из ConfigurationSection
     */
    public static ProtectedRegion deserialize(ConfigurationSection section) {
        if (section == null) return null;

        String id = section.getString("id");
        UUID owner = UUID.fromString(section.getString("owner"));
        
        World world = Bukkit.getWorld(section.getString("world"));
        if (world == null) return null;
        
        Location location = new Location(
            world,
            section.getDouble("x"),
            section.getDouble("y"),
            section.getDouble("z")
        );
        
        int radius = section.getInt("radius");
        String blockType = section.getString("blockType");
        int priority = section.getInt("priority");
        
        Set<UUID> members = new HashSet<>();
        List<String> membersList = section.getStringList("members");
        for (String uuidStr : membersList) {
            try {
                members.add(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException ignored) {}
        }
        
        Set<UUID> coOwners = new HashSet<>();
        List<String> coOwnersList = section.getStringList("coOwners");
        for (String uuidStr : coOwnersList) {
            try {
                coOwners.add(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException ignored) {}
        }
        
        ProtectedRegion region = new ProtectedRegion(
            id, owner, location, radius, blockType, priority, members, coOwners
        );
        
        region.setHidden(section.getBoolean("hidden", false));
        
        return region;
    }

    // Геттеры и сеттеры
    public String getId() {
        return id;
    }

    public UUID getOwner() {
        return owner;
    }

    public Location getLocation() {
        return location;
    }

    public int getRadius() {
        return radius;
    }

    public String getBlockType() {
        return blockType;
    }

    public int getPriority() {
        return priority;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public Set<UUID> getCoOwners() {
        return coOwners;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
}
