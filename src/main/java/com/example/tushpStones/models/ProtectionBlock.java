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

    // Настройки прочности
    private final boolean healthEnabled;
    private final int defaultHealth;
    private final int maxHealth;
    private final Material upgradeItem;
    private final int upgradeAmount;
    private final int costPerUpgrade;

    public ProtectionBlock(String id, Material material, int radius, int priority,
                           boolean canBeDestroyed, Set<Material> allowedExplosives,
                           Map<String, Object> flags) {
        this(id, material, radius, priority, canBeDestroyed, allowedExplosives, flags,
                false, 0, 0, null, 0, 0);
    }

    public ProtectionBlock(String id, Material material, int radius, int priority,
                           boolean canBeDestroyed, Set<Material> allowedExplosives,
                           Map<String, Object> flags,
                           boolean healthEnabled, int defaultHealth, int maxHealth,
                           Material upgradeItem, int upgradeAmount, int costPerUpgrade) {
        this.id = id;
        this.material = material;
        this.radius = radius;
        this.priority = priority;
        this.canBeDestroyed = canBeDestroyed;
        this.allowedExplosives = allowedExplosives;
        this.flags = flags;

        // Настройки прочности
        this.healthEnabled = healthEnabled;
        this.defaultHealth = defaultHealth;
        this.maxHealth = maxHealth;
        this.upgradeItem = upgradeItem;
        this.upgradeAmount = upgradeAmount;
        this.costPerUpgrade = costPerUpgrade;
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

    /**
     * Получить урон от взрыва определенного типа
     * @param explosive Тип взрывчатки
     * @return Урон региону (0 если не может нанести урон)
     */
    public int getExplosionDamage(Material explosive) {
        if (!canBeDestroyedBy(explosive)) {
            return 0;
        }

        // Базовый урон в зависимости от типа взрыва
        switch (explosive.name()) {
            case "TNT":
                return 25;
            case "CREEPER_HEAD":
                return 20;
            case "TNT_MINECART":
                return 35;
            case "END_CRYSTAL":
                return 50;
            case "WITHER_SKELETON_SKULL":
                return 45;
            case "FIRE_CHARGE":
                return 15;
            default:
                return 20;
        }
    }

    /**
     * Проверить, можно ли использовать предмет для улучшения
     */
    public boolean isValidUpgradeItem(Material material) {
        return healthEnabled && upgradeItem != null && upgradeItem == material;
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

    // Геттеры для прочности
    public boolean isHealthEnabled() {
        return healthEnabled;
    }

    public int getDefaultHealth() {
        return defaultHealth;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public Material getUpgradeItem() {
        return upgradeItem;
    }

    public int getUpgradeAmount() {
        return upgradeAmount;
    }

    public int getCostPerUpgrade() {
        return costPerUpgrade;
    }
}