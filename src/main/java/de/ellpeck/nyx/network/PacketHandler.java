package de.ellpeck.nyx.network;

import de.ellpeck.nyx.Nyx;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class PacketHandler {

    private static final String VERSION = "1";
    private static SimpleChannel network;

    public static void init() {
        network = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(Nyx.ID, "main"),
                () -> VERSION, VERSION::equals, VERSION::equals);
        network.registerMessage(0, PacketNyxWorld.class, PacketNyxWorld::encode, PacketNyxWorld::decode, PacketNyxWorld::handle);
    }

    public static void sendTo(ServerPlayer player, PacketNyxWorld message) {
        network.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    private PacketHandler() {
    }
}
