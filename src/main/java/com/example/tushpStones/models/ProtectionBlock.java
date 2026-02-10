package com.example.tushpStones.models;

import org.bukkit.Material;

import java.util.Map;
import java.util.Set;

/**
 * Модель конфигурации блока привата
 */
public class ProtectionBlock {

    private final String id;
    private final Material material;
    private final int radius;
    private final int priority;
    private final boolean canBeDestroyed;
    private final Set<Material> allowedExplosives;
    private final Map<String, Object> flags;

    public ProtectionBlock(String id, Material material, int radius, int priority, 
                          boolean canBeDestroyed, Set<Material> allowedExplosives,
                          Map<String, Object> flags) {
        this.id = id;
        this.material = material;
        this.radius = radius;
        this.priority = priority;
        this.canBeDestroyed = canBeDestroyed;
        this.allowedExplosives = allowedExplosives;
        this.flags = flags;
    }

    /**
     * Проверка, может ли взрывчатка разрушить этот блок
     */
    public boolean canBeDestroyedBy(Material explosive) {
        if (!canBeDestroyed) {
            return false;
        }
        
        // Если список пуст, значит любой динамит может разрушить
        if (allowedExplosives.isEmpty()) {
            return true;
        }
        
        return allowedExplosives.contains(explosive);
    }

    // Геттеры
    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public int getRadius() {
        return radius;
    }

    public int getPriority() {
        return priority;
    }

    public boolean canBeDestroyed() {
        return canBeDestroyed;
    }

    public Set<Material> getAllowedExplosives() {
        return allowedExplosives;
    }

    public Map<String, Object> getFlags() {
        return flags;
    }

    public Object getFlag(String key) {
        return flags.get(key);
    }
}
