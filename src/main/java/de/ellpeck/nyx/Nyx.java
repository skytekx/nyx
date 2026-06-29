package de.ellpeck.nyx;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Nyx, ported to Minecraft 1.20.1 (Forge).
 *
 * <p>Original mod and concept: Ellpeck (https://github.com/Ellpeck/Nyx, MIT).
 * This client-side port reimplements Nyx's core night-atmosphere idea on the
 * 1.20.1 client render pipeline: the night gets darker and is tinted, with the
 * intensity driven by the moon phase.
 */
@Mod(Nyx.ID)
public final class Nyx {

    public static final String ID = "nyx";
    public static final String NAME = "Nyx";

    public Nyx() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, NyxConfig.CLIENT_SPEC);
        // Purely a client visual mod; the dedicated server has nothing to do here.
        if (FMLEnvironment.dist != Dist.CLIENT)
            return;
    }
}
