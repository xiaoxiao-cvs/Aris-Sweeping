package com.arisweeping.cleaning;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.Animal;

/**
 * 实体移除信息
 * 
 * 记录被移除实体的详细信息，用于撤销操作
 */
public class EntityRemovalInfo {
    private final String entityType;
    private final String entityId;
    private final double x, y, z;
    private final String dimensionKey;
    private final CompoundTag entityNBT;
    private final long removalTime;
    
    private EntityRemovalInfo(String entityType, String entityId, double x, double y, double z, 
                             String dimensionKey, CompoundTag entityNBT) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimensionKey = dimensionKey;
        this.entityNBT = entityNBT;
        this.removalTime = System.currentTimeMillis();
    }
    
    /**
     * 为物品实体创建移除信息
     */
    public static EntityRemovalInfo forItem(ItemEntity item) {
        CompoundTag nbt = new CompoundTag();
        item.saveWithoutId(nbt);
        
        return new EntityRemovalInfo(
            "item",
            item.getUUID().toString(),
            item.getX(),
            item.getY(), 
            item.getZ(),
            item.level().dimension().toString(),
            nbt
        );
    }
    
    /**
     * 为动物实体创建移除信息
     */
    public static EntityRemovalInfo forAnimal(Animal animal) {
        CompoundTag nbt = new CompoundTag();
        animal.saveWithoutId(nbt);
        
        return new EntityRemovalInfo(
            "animal",
            animal.getUUID().toString(),
            animal.getX(),
            animal.getY(),
            animal.getZ(),
            animal.level().dimension().toString(),
            nbt
        );
    }
    
    /**
     * 通用实体移除信息创建
     */
    public static EntityRemovalInfo forEntity(Entity entity) {
        CompoundTag nbt = new CompoundTag();
        entity.saveWithoutId(nbt);
        
        String type = "unknown";
        if (entity instanceof ItemEntity) {
            type = "item";
        } else if (entity instanceof Animal) {
            type = "animal";
        }
        
        return new EntityRemovalInfo(
            type,
            entity.getUUID().toString(),
            entity.getX(),
            entity.getY(),
            entity.getZ(),
            entity.level().dimension().toString(),
            nbt
        );
    }
    
    // Getters
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public String getDimensionKey() { return dimensionKey; }
    public CompoundTag getEntityNBT() { return entityNBT; }
    public long getRemovalTime() { return removalTime; }
    
    @Override
    public String toString() {
        return String.format("EntityRemovalInfo{type=%s, id=%s, pos=(%.1f,%.1f,%.1f), dimension=%s}",
            entityType, entityId, x, y, z, dimensionKey);
    }
}