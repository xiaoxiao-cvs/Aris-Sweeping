package com.xiaoxiao.arissweeping.util;

import org.bukkit.entity.*;
import com.xiaoxiao.arissweeping.util.EntityTypeUtils.CleanupEntityType;

import java.util.concurrent.atomic.AtomicInteger;

public class CleanupStats {
    private final AtomicInteger itemsCleaned = new AtomicInteger(0);
    private final AtomicInteger experienceOrbsCleaned = new AtomicInteger(0);
    private final AtomicInteger arrowsCleaned = new AtomicInteger(0);
    private final AtomicInteger fallingBlocksCleaned = new AtomicInteger(0);
    private final AtomicInteger mobsCleaned = new AtomicInteger(0);
    private final AtomicInteger otherEntitiesCleaned = new AtomicInteger(0);
    
    public void incrementType(Entity entity) {
        CleanupEntityType type = EntityTypeUtils.getCleanupType(entity);
        switch (type) {
            case ITEM:
                itemsCleaned.incrementAndGet();
                break;
            case EXPERIENCE_ORB:
                experienceOrbsCleaned.incrementAndGet();
                break;
            case ARROW:
                arrowsCleaned.incrementAndGet();
                break;
            case FALLING_BLOCK:
                fallingBlocksCleaned.incrementAndGet();
                break;
            case MOB:
                mobsCleaned.incrementAndGet();
                break;
            default:
                otherEntitiesCleaned.incrementAndGet();
                break;
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
    
    // 兼容性方法 - 带Cleaned后缀的别名
    public void incrementArrowsCleaned() {
        incrementArrows();
    }
    
    public void incrementExperienceOrbsCleaned() {
        incrementExperienceOrbs();
    }
    
    // 冗余的'Removed'后缀方法已移除，请使用对应的标准方法
    
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
    
    // 冗余的'Removed'后缀getter方法已移除，请使用对应的标准getter方法
    
    public int getTotalCleaned() {
        return itemsCleaned.get() + experienceOrbsCleaned.get() + 
               arrowsCleaned.get() + fallingBlocksCleaned.get() + mobsCleaned.get() + otherEntitiesCleaned.get();
    }
    
    /**
     * 合并另一个CleanupStats的统计信息
     * @param other 要合并的统计信息
     */
    public void merge(CleanupStats other) {
        if (other != null) {
            itemsCleaned.addAndGet(other.getItemsCleaned());
            experienceOrbsCleaned.addAndGet(other.getExperienceOrbsCleaned());
            arrowsCleaned.addAndGet(other.getArrowsCleaned());
            fallingBlocksCleaned.addAndGet(other.getFallingBlocksCleaned());
            mobsCleaned.addAndGet(other.getMobsCleaned());
            otherEntitiesCleaned.addAndGet(other.getOtherEntitiesCleaned());
        }
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