package de.ellpeck.nyx.capabilities;

import de.ellpeck.nyx.Nyx;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Holds the {@link Capability} token for {@link NyxWorld} and wires up its
 * registration ({@link RegisterCapabilitiesEvent}, mod bus) and attachment to
 * every {@link Level} ({@link AttachCapabilitiesEvent}, forge bus).
 */
@Mod.EventBusSubscriber(modid = Nyx.ID)
public final class NyxCapabilities {

    public static final Capability<NyxWorld> WORLD = CapabilityManager.get(new CapabilityToken<>() {
    });

    @Mod.EventBusSubscriber(modid = Nyx.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent
        public static void register(RegisterCapabilitiesEvent event) {
            event.register(NyxWorld.class);
        }

        private ModBus() {
        }
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Level> event) {
        event.addCapability(new ResourceLocation(Nyx.ID, "world_cap"), new NyxWorld(event.getObject()));
    }

    private NyxCapabilities() {
    }
}
