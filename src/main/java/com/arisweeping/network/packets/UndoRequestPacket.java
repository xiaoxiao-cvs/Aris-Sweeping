package com.arisweeping.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UndoRequestPacket {
    public static void encode(UndoRequestPacket packet, FriendlyByteBuf buffer) {}
    public static UndoRequestPacket decode(FriendlyByteBuf buffer) { return new UndoRequestPacket(); }
    public static void handle(UndoRequestPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().setPacketHandled(true);
    }
}