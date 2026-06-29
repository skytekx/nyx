package de.ellpeck.nyx;

import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Shared night-atmosphere maths for the 1.20.1 Nyx port.
 *
 * <p>The whole mod boils down to two moon-driven values that both the light
 * mixin ({@link de.ellpeck.nyx.mixin.ClientLevelMixin}) and the fog handler
 * ({@link ClientHandler}) consume:
 * <ul>
 *   <li>a {@link #nightFactor night factor} (0 by day, 1 at midnight), and</li>
 *   <li>a {@link #moonBrightness moon brightness} (1.0 at full moon, 0.0 at new moon).</li>
 * </ul>
 * The {@link #darkenAmount darken amount} combines them with the config so a
 * full moon keeps the night bright and a new moon makes it pitch black, exactly
 * mirroring how Ellpeck's original tied night effects to
 * {@code World#getCurrentMoonPhaseFactor()}.
 */
public final class NightAtmosphere {

    private NightAtmosphere() {
    }

    /**
     * @return 0 in full daylight, rising to 1 at the deepest point of the night,
     * derived from the sun's height ({@code cos(sunAngle)}).
     */
    public static float nightFactor(Level level, float partialTick) {
        // getSunAngle == getTimeOfDay * 2PI; cos is +1 at noon, -1 at midnight.
        float sunHeight = Mth.cos(level.getSunAngle(partialTick));
        return Mth.clamp(-sunHeight, 0.0F, 1.0F);
    }

    /**
     * @return the vanilla per-phase moon brightness: 1.0 at full moon, 0.0 at new moon.
     */
    public static float moonBrightness(Level level) {
        int phase = level.dimensionType().moonPhase(level.getDayTime());
        return DimensionType.MOON_BRIGHTNESS_PER_PHASE[phase];
    }

    /**
     * The moon-phase-weighted "nightness", in {@code [0, 1]}: how deep into the night
     * we are, reduced by how full the moon is. This is the shared driver for both the
     * light darkening and the colour tint. Returns 0 when disabled, during the day, or
     * before the config has loaded.
     */
    public static float nightPhase(Level level, float partialTick) {
        if (level == null || !NyxConfig.isLoaded() || !NyxConfig.darkerNights())
            return 0.0F;
        float night = nightFactor(level, partialTick);
        if (night <= 0.0F)
            return 0.0F;
        float moon = moonBrightness(level);
        float influence = (float) NyxConfig.moonPhaseInfluence();
        // Fuller moons (moon -> 1) shrink the effect; new moons (moon -> 0) keep it at full strength.
        return Mth.clamp(night * (1.0F - moon * influence), 0.0F, 1.0F);
    }

    /**
     * The fraction by which night sky brightness should be reduced, in {@code [0, 1]}.
     * This is {@link #nightPhase} scaled by the configured darkness strength.
     */
    public static float darkenAmount(Level level, float partialTick) {
        return Mth.clamp(nightPhase(level, partialTick) * (float) NyxConfig.nightDarkness(), 0.0F, 1.0F);
    }
}
