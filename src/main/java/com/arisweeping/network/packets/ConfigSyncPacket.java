package com.arisweeping.network.packets;

import com.arisweeping.core.ArisSweepingMod;
import com.arisweeping.data.ConfigData;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;
import java.util.ArrayList;

/**
 * 配置同步数据包
 * 用于服务器向客户端同步配置数据
 */
public class ConfigSyncPacket {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private final ConfigData configData;
    
    public ConfigSyncPacket(ConfigData configData) {
        this.configData = configData;
    }
    
    /**
     * 编码数据包内容到缓冲区
     */
    public static void encode(ConfigSyncPacket packet, FriendlyByteBuf buffer) {
        try {
            // 编码物品清理配置
            ConfigData.ItemCleaningConfig itemConfig = packet.configData.getItemCleaningConfig();
            buffer.writeBoolean(itemConfig.isEnabled());
            buffer.writeInt(itemConfig.getItemLifetimeSeconds());
            buffer.writeInt(itemConfig.getChunkRange());
            buffer.writeInt(itemConfig.getMinItemCount());
            
            // 编码物品白名单
            buffer.writeInt(itemConfig.getItemWhitelist().size());
            for (String item : itemConfig.getItemWhitelist()) {
                buffer.writeUtf(item);
            }
            
            // 编码物品黑名单
            buffer.writeInt(itemConfig.getItemBlacklist().size());
            for (String item : itemConfig.getItemBlacklist()) {
                buffer.writeUtf(item);
            }
            
            // 编码动物清理配置
            ConfigData.AnimalCleaningConfig animalConfig = packet.configData.getAnimalCleaningConfig();
            buffer.writeBoolean(animalConfig.isEnabled());
            buffer.writeInt(animalConfig.getMaxAnimalsPerChunk());
            buffer.writeInt(animalConfig.getCheckRadius());
            buffer.writeBoolean(animalConfig.isProtectBreeding());
            buffer.writeBoolean(animalConfig.isProtectBabies());
            
            LOGGER.debug("Encoded ConfigSyncPacket successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to encode ConfigSyncPacket", e);
            throw e;
        }
    }
    
    /**
     * 从缓冲区解码数据包内容
     */
    public static ConfigSyncPacket decode(FriendlyByteBuf buffer) {
        try {
            ConfigData configData = new ConfigData();
            
            // 解码物品清理配置
            ConfigData.ItemCleaningConfig itemConfig = configData.getItemCleaningConfig();
            itemConfig.setEnabled(buffer.readBoolean());
            itemConfig.setItemLifetimeSeconds(buffer.readInt());
            itemConfig.setChunkRange(buffer.readInt());
            itemConfig.setMinItemCount(buffer.readInt());
            
            // 解码物品白名单
            int whitelistSize = buffer.readInt();
            ArrayList<String> whitelist = new ArrayList<>();
            for (int i = 0; i < whitelistSize; i++) {
                whitelist.add(buffer.readUtf());
            }
            itemConfig.setItemWhitelist(whitelist);
            
            // 解码物品黑名单
            int blacklistSize = buffer.readInt();
            ArrayList<String> blacklist = new ArrayList<>();
            for (int i = 0; i < blacklistSize; i++) {
                blacklist.add(buffer.readUtf());
            }
            itemConfig.setItemBlacklist(blacklist);
            
            // 解码动物清理配置
            ConfigData.AnimalCleaningConfig animalConfig = configData.getAnimalCleaningConfig();
            animalConfig.setEnabled(buffer.readBoolean());
            animalConfig.setMaxAnimalsPerChunk(buffer.readInt());
            animalConfig.setCheckRadius(buffer.readInt());
            animalConfig.setProtectBreeding(buffer.readBoolean());
            animalConfig.setProtectBabies(buffer.readBoolean());
            
            LOGGER.debug("Decoded ConfigSyncPacket successfully");
            return new ConfigSyncPacket(configData);
        } catch (Exception e) {
            LOGGER.error("Failed to decode ConfigSyncPacket", e);
            throw e;
        }
    }
    
    /**
     * 处理数据包
     */
    public static void handle(ConfigSyncPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            try {
                LOGGER.debug("Processing ConfigSyncPacket on client");
                
                // 更新客户端配置数据
                ArisSweepingMod.updateConfigData(packet.configData);
                
                LOGGER.info("Client configuration synchronized successfully");
            } catch (Exception e) {
                LOGGER.error("Failed to process ConfigSyncPacket", e);
            }
        });
        ctx.setPacketHandled(true);
    }
}