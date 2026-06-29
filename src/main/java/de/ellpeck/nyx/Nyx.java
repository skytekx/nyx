package de.ellpeck.nyx;

import de.ellpeck.nyx.network.PacketHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Nyx, ported from Minecraft 1.12.2 to 1.20.1 (Forge).
 *
 * <p>Original mod and concept: Ellpeck (https://github.com/Ellpeck/Nyx, MIT).
 *
 * <p>This is the FULL content mod (blocks, items, tools/armor, the falling
 * meteor and star entities, the server-side lunar events - full/blood/harvest
 * moon and star showers - moon-scaled enchantments, lunar water and crystals),
 * plus the already-ported client night-atmosphere effect on the 1.20.1 render
 * pipeline.
 */
@Mod(Nyx.ID)
public final class Nyx {

    public static final String ID = "nyx";
    public static final String NAME = "Nyx";

    public Nyx() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Content registries (blocks, items, entities, enchantments, sounds, fluids, ...).
        Registration.register(modBus);

        // Server/common gameplay config + the client atmosphere config.
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, NyxConfig.CLIENT_SPEC);
        modBus.addListener(this::onConfig);

        // Networking for the NyxWorld capability sync.
        PacketHandler.init();
    }

    private void onConfig(ModConfigEvent event) {
        if (event.getConfig().getSpec() == Config.COMMON_SPEC)
            Config.bake();
    }
}
