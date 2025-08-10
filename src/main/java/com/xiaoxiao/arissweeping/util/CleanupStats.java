package com.xiaoxiao.arissweeping.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.AbstractArrow;

import java.util.concurrent.atomic.AtomicInteger;

public class CleanupStats {
    private final AtomicInteger itemsCleaned = new AtomicInteger(0);
    private final AtomicInteger experienceOrbsCleaned = new AtomicInteger(0);
    private final AtomicInteger arrowsCleaned = new AtomicInteger(0);
    private final AtomicInteger mobsCleaned = new AtomicInteger(0);
    private final AtomicInteger otherEntitiesCleaned = new AtomicInteger(0);
    
    public void incrementType(Entity entity) {
        if (entity instanceof ItemEntity) {
            itemsCleaned.incrementAndGet();
        } else if (entity instanceof ExperienceOrb) {
            experienceOrbsCleaned.incrementAndGet();
        } else if (entity instanceof AbstractArrow) {
            arrowsCleaned.incrementAndGet();
        } else if (entity instanceof Monster) {
            mobsCleaned.incrementAndGet();
        } else {
            otherEntitiesCleaned.incrementAndGet();
        }
    }
    
    public int getItemsCleaned() {
        return itemsCleaned.get();
    }
    
    public int getExperienceOrbsCleaned() {
        return experienceOrbsCleaned.get();
    }
    
    public int getArrowsCleaned() {
        return arrowsCleaned.get();
    }
    
    public int getMobsCleaned() {
        return mobsCleaned.get();
    }
    
    public int getOtherEntitiesCleaned() {
        return otherEntitiesCleaned.get();
    }
    
    public int getTotalCleaned() {
        return itemsCleaned.get() + experienceOrbsCleaned.get() + 
               arrowsCleaned.get() + mobsCleaned.get() + otherEntitiesCleaned.get();
    }
    
    @Override
    public String toString() {
        return String.format(
            "CleanupStats{items=%d, xpOrbs=%d, arrows=%d, mobs=%d, others=%d, total=%d}",
            getItemsCleaned(), getExperienceOrbsCleaned(), getArrowsCleaned(),
            getMobsCleaned(), getOtherEntitiesCleaned(), getTotalCleaned()
        );
    }
    
    public void reset() {
        itemsCleaned.set(0);
        experienceOrbsCleaned.set(0);
        arrowsCleaned.set(0);
        mobsCleaned.set(0);
        otherEntitiesCleaned.set(0);
    }
}