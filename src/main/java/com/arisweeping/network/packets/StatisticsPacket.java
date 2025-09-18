package com.arisweeping.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StatisticsPacket {
    public static void encode(StatisticsPacket packet, FriendlyByteBuf buffer) {}
    public static StatisticsPacket decode(FriendlyByteBuf buffer) { return new StatisticsPacket(); }
    public static void handle(StatisticsPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().setPacketHandled(true);
    }
}