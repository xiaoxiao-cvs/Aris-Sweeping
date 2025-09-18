package com.arisweeping.network;
import com.arisweeping.core.ArisLogger;

import com.arisweeping.core.ArisSweepingMod;
import com.arisweeping.network.packets.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 网络数据包处理器
 * 负责客户端-服务器之间的数据同步
 */
public class PacketHandler {
    
    private static final String PROTOCOL_VERSION = "1";
    
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ArisSweepingMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    
    private static int messageId = 0;
    
    /**
     * 注册所有数据包类型
     */
    public static void register() {
        ArisLogger.info("Registering network packets for ArisSweeping...");
        
        // 配置同步数据包
        INSTANCE.messageBuilder(ConfigSyncPacket.class, messageId++)
                .encoder(ConfigSyncPacket::encode)
                .decoder(ConfigSyncPacket::decode)
                .consumerMainThread(ConfigSyncPacket::handle)
                .add();
        
        // 任务状态数据包
        INSTANCE.messageBuilder(TaskStatusPacket.class, messageId++)
                .encoder(TaskStatusPacket::encode)
                .decoder(TaskStatusPacket::decode)
                .consumerMainThread(TaskStatusPacket::handle)
                .add();
        
        // 撤销请求数据包
        INSTANCE.messageBuilder(UndoRequestPacket.class, messageId++)
                .encoder(UndoRequestPacket::encode)
                .decoder(UndoRequestPacket::decode)
                .consumerMainThread(UndoRequestPacket::handle)
                .add();
        
        // 清理状态数据包
        INSTANCE.messageBuilder(CleaningStatusPacket.class, messageId++)
                .encoder(CleaningStatusPacket::encode)
                .decoder(CleaningStatusPacket::decode)
                .consumerMainThread(CleaningStatusPacket::handle)
                .add();
        
        // 统计数据数据包
        INSTANCE.messageBuilder(StatisticsPacket.class, messageId++)
                .encoder(StatisticsPacket::encode)
                .decoder(StatisticsPacket::decode)
                .consumerMainThread(StatisticsPacket::handle)
                .add();
        
        ArisLogger.info("Registered {} network packet types", messageId);
    }
    
    /**
     * 发送数据包到服务器
     */
    public static void sendToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }
    
    /**
     * 发送数据包到指定客户端
     */
    public static void sendToClient(Object packet, net.minecraft.server.level.ServerPlayer player) {
        INSTANCE.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
    
    /**
     * 发送数据包到所有客户端
     */
    public static void sendToAllClients(Object packet) {
        INSTANCE.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(), packet);
    }
    
    /**
     * 发送数据包到指定维度的所有客户端
     */
    public static void sendToDimension(Object packet, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {
        INSTANCE.send(net.minecraftforge.network.PacketDistributor.DIMENSION.with(() -> dimension), packet);
    }
    
    /**
     * 发送数据包到指定位置附近的客户端
     */
    public static void sendToNear(Object packet, net.minecraft.server.level.ServerLevel level, 
                                 double x, double y, double z, double radius) {
        net.minecraftforge.network.PacketDistributor.TargetPoint target = 
                new net.minecraftforge.network.PacketDistributor.TargetPoint(x, y, z, radius, level.dimension());
        
        INSTANCE.send(net.minecraftforge.network.PacketDistributor.NEAR.with(() -> target), packet);
    }
    
    /**
     * 获取协议版本
     */
    public static String getProtocolVersion() {
        return PROTOCOL_VERSION;
    }
    
    /**
     * 检查网络连接是否兼容
     */
    public static boolean isCompatibleVersion(String remoteVersion) {
        return PROTOCOL_VERSION.equals(remoteVersion);
    }
}