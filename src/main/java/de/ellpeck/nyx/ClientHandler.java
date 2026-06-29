package de.ellpeck.nyx;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-only handler that darkens and tints the fog/sky colour at night.
 *
 * <p>The {@link de.ellpeck.nyx.mixin.ClientLevelMixin light mixin} already
 * darkens the world light map via {@code ClientLevel#getSkyDarken}, but the fog
 * and sky colour are computed separately, so without this the horizon would stay
 * bright while the ground went dark. This mirrors Ellpeck's original
 * {@code onFogRender} sky-tint handler, but drives it off the moon phase instead
 * of off scripted lunar events.
 */
@Mod.EventBusSubscriber(modid = Nyx.ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientHandler {

    private ClientHandler() {
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Level level = Minecraft.getInstance().level;
        if (level == null || !NyxConfig.isLoaded())
            return;

        float partialTick = (float) event.getPartialTick();
        float phase = NightAtmosphere.nightPhase(level, partialTick);
        if (phase <= 0.0F)
            return;

        float r = event.getRed();
        float g = event.getGreen();
        float b = event.getBlue();

        // Darken the fog/sky colour so the horizon matches the darker world light.
        if (NyxConfig.darkenFog()) {
            float fogDarken = phase * (float) NyxConfig.nightDarkness() * (float) NyxConfig.fogDarkness();
            float keep = Mth.clamp(1.0F - fogDarken, 0.0F, 1.0F);
            r *= keep;
            g *= keep;
            b *= keep;
        }

        // Tint the night towards the configured colour; deeper on darker moon phases.
        if (NyxConfig.nightTint()) {
            float amount = Mth.clamp(phase * (float) NyxConfig.tintStrength(), 0.0F, 1.0F);
            if (amount > 0.0F) {
                r = Mth.lerp(amount, r, NyxConfig.tintRed() / 255.0F);
                g = Mth.lerp(amount, g, NyxConfig.tintGreen() / 255.0F);
                b = Mth.lerp(amount, b, NyxConfig.tintBlue() / 255.0F);
            }
        }

        event.setRed(r);
        event.setGreen(g);
        event.setBlue(b);
    }
}
