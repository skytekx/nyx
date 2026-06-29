package de.ellpeck.nyx.mixin;

import de.ellpeck.nyx.NightAtmosphere;
import de.ellpeck.nyx.NyxConfig;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The heart of the 1.20.1 Nyx port.
 *
 * <p>{@code ClientLevel#getSkyDarken(float)} returns the interpolated sky
 * brightness used to build the light texture every frame (≈1.0 at noon, floored
 * at 0.2 at deep night). {@link net.minecraft.client.renderer.LightTexture#updateLightTexture}
 * multiplies the sky light contribution by this value, so scaling it down here
 * darkens everything that is lit by the sky — i.e. the open world at night,
 * while caves (block-lit) stay untouched.
 *
 * <p>The reduction is driven by the moon phase: a full moon barely darkens the
 * night, a new moon makes it pitch black. This reproduces Ellpeck's original
 * "the moon governs the night" behaviour on the modern client pipeline.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @Inject(method = "getSkyDarken(F)F", at = @At("RETURN"), cancellable = true)
    private void nyx$darkenNight(float partialTick, CallbackInfoReturnable<Float> cir) {
        Level self = (Level) (Object) this;
        float darken = NightAtmosphere.darkenAmount(self, partialTick);
        if (darken <= 0.0F)
            return;
        float original = cir.getReturnValueF();
        float min = (float) NyxConfig.minBrightness();
        float scaled = Math.max(original * (1.0F - darken), min);
        if (scaled < original)
            cir.setReturnValue(scaled);
    }
}
