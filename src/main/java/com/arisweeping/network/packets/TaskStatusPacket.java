package com.arisweeping.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 任务状态数据包
 */
public class TaskStatusPacket {
    
    public static void encode(TaskStatusPacket packet, FriendlyByteBuf buffer) {
        // TODO: 实现编码
    }
    
    public static TaskStatusPacket decode(FriendlyByteBuf buffer) {
        // TODO: 实现解码
        return new TaskStatusPacket();
    }
    
    public static void handle(TaskStatusPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.setPacketHandled(true);
    }
}