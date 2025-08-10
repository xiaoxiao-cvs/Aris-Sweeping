package com.xiaoxiao.arissweeping.util;

import org.bukkit.entity.*;

import java.util.concurrent.atomic.AtomicInteger;

public class CleanupStats {
    private final AtomicInteger itemsCleaned = new AtomicInteger(0);
    private final AtomicInteger experienceOrbsCleaned = new AtomicInteger(0);
    private final AtomicInteger arrowsCleaned = new AtomicInteger(0);
    private final AtomicInteger fallingBlocksCleaned = new AtomicInteger(0);
    private final AtomicInteger mobsCleaned = new AtomicInteger(0);
    private final AtomicInteger otherEntitiesCleaned = new AtomicInteger(0);
    
    public void incrementType(Entity entity) {
        if (entity instanceof Item) {
            itemsCleaned.incrementAndGet();
        } else if (entity instanceof ExperienceOrb) {
            experienceOrbsCleaned.incrementAndGet();
        } else if (entity instanceof Arrow) {
            arrowsCleaned.incrementAndGet();
        } else if (entity instanceof org.bukkit.entity.FallingBlock) {
            fallingBlocksCleaned.incrementAndGet();
        } else if (entity instanceof Monster || entity instanceof Animals) {
            mobsCleaned.incrementAndGet();
        } else {
            otherEntitiesCleaned.incrementAndGet();
        }
    }
    
    public void incrementItems() {
        itemsCleaned.incrementAndGet();
    }
    
    public void incrementExperienceOrbs() {
        experienceOrbsCleaned.incrementAndGet();
    }
    
    public void incrementArrows() {
        arrowsCleaned.incrementAndGet();
    }
    
    public void incrementFallingBlocks() {
        fallingBlocksCleaned.incrementAndGet();
    }
    
    public void incrementMobs() {
        mobsCleaned.incrementAndGet();
    }
    
    public void incrementOthers() {
        otherEntitiesCleaned.incrementAndGet();
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
    
    public int getFallingBlocksCleaned() {
        return fallingBlocksCleaned.get();
    }
    
    public int getMobsCleaned() {
        return mobsCleaned.get();
    }
    
    public int getOtherEntitiesCleaned() {
        return otherEntitiesCleaned.get();
    }
    
    public int getTotalCleaned() {
        return itemsCleaned.get() + experienceOrbsCleaned.get() + 
               arrowsCleaned.get() + fallingBlocksCleaned.get() + mobsCleaned.get() + otherEntitiesCleaned.get();
    }
    
    @Override
    public String toString() {
        return String.format(
            "CleanupStats{items=%d, xpOrbs=%d, arrows=%d, fallingBlocks=%d, mobs=%d, others=%d, total=%d}",
            getItemsCleaned(), getExperienceOrbsCleaned(), getArrowsCleaned(),
            getFallingBlocksCleaned(), getMobsCleaned(), getOtherEntitiesCleaned(), getTotalCleaned()
        );
    }
    
    public void reset() {
        itemsCleaned.set(0);
        experienceOrbsCleaned.set(0);
        arrowsCleaned.set(0);
        fallingBlocksCleaned.set(0);
        mobsCleaned.set(0);
        otherEntitiesCleaned.set(0);
    }
}