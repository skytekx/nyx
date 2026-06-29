package de.ellpeck.nyx.network;

import de.ellpeck.nyx.capabilities.NyxWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> client sync of the client-relevant {@link NyxWorld} state (current
 * event, cached/landed meteor positions). Replaces the old
 * {@code SimpleNetworkWrapper}/{@code IMessage} pair.
 */
public class PacketNyxWorld {

    private final CompoundTag info;

    public PacketNyxWorld(NyxWorld world) {
        this.info = world.serializeNBT(true);
    }

    private PacketNyxWorld(CompoundTag info) {
        this.info = info;
    }

    public static void encode(PacketNyxWorld msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.info);
    }

    public static PacketNyxWorld decode(FriendlyByteBuf buf) {
        return new PacketNyxWorld(buf.readNbt());
    }

    public static void handle(PacketNyxWorld msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.apply(msg.info)));
        ctx.get().setPacketHandled(true);
    }

    private static final class ClientHandler {
        static void apply(CompoundTag info) {
            Level world = Minecraft.getInstance().level;
            if (world != null) {
                NyxWorld nyx = NyxWorld.get(world);
                if (nyx != null)
                    nyx.deserializeNBT(info, true);
            }
        }
    }
}
