package com.arisweeping.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CleaningStatusPacket {
    public static void encode(CleaningStatusPacket packet, FriendlyByteBuf buffer) {}
    public static CleaningStatusPacket decode(FriendlyByteBuf buffer) { return new CleaningStatusPacket(); }
    public static void handle(CleaningStatusPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().setPacketHandled(true);
    }
}