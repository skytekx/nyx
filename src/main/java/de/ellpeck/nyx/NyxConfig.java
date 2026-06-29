package de.ellpeck.nyx;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Client-side configuration for the 1.20.1 port of Nyx.
 *
 * <p>Mirrors the spirit of Ellpeck's original {@code Config} (a mod "about the
 * effects of the moon"): the night sky is darkened, and how dark it gets is
 * governed by the moon phase. A full moon keeps the night relatively bright; a
 * new moon makes it pitch black.
 */
public final class NyxConfig {

    public static final ForgeConfigSpec CLIENT_SPEC;

    private static final ForgeConfigSpec.BooleanValue DARKER_NIGHTS;
    private static final ForgeConfigSpec.DoubleValue NIGHT_DARKNESS;
    private static final ForgeConfigSpec.DoubleValue MOON_PHASE_INFLUENCE;
    private static final ForgeConfigSpec.DoubleValue MIN_BRIGHTNESS;

    private static final ForgeConfigSpec.BooleanValue DARKEN_FOG;
    private static final ForgeConfigSpec.DoubleValue FOG_DARKNESS;
    private static final ForgeConfigSpec.BooleanValue NIGHT_TINT;
    private static final ForgeConfigSpec.IntValue TINT_RED;
    private static final ForgeConfigSpec.IntValue TINT_GREEN;
    private static final ForgeConfigSpec.IntValue TINT_BLUE;
    private static final ForgeConfigSpec.DoubleValue TINT_STRENGTH;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Darkness of the night sky, scaled by the moon phase").push("darkness");
        DARKER_NIGHTS = b
                .comment("If nights should be darkened at all")
                .define("darkerNights", true);
        NIGHT_DARKNESS = b
                .comment("How much the sky light is reduced at the darkest point of a new-moon night (1 = 100% darker)")
                .defineInRange("nightDarkness", 0.6D, 0.0D, 1.0D);
        MOON_PHASE_INFLUENCE = b
                .comment("How strongly a fuller moon counteracts the darkening (1 = a full moon removes all extra darkness)")
                .defineInRange("moonPhaseInfluence", 0.75D, 0.0D, 1.0D);
        MIN_BRIGHTNESS = b
                .comment("Lower bound for the resulting sky brightness so the night never goes fully black (vanilla night floor is 0.2)")
                .defineInRange("minBrightness", 0.0D, 0.0D, 1.0D);
        b.pop();

        b.comment("Colour of the night sky / fog, scaled by the moon phase").push("color");
        DARKEN_FOG = b
                .comment("If the fog and sky colour should be darkened together with the light, so the horizon matches the darker world")
                .define("darkenFog", true);
        FOG_DARKNESS = b
                .comment("How strongly the fog/sky colour darkens relative to the world light (1 = same amount)")
                .defineInRange("fogDarkness", 0.5D, 0.0D, 1.0D);
        NIGHT_TINT = b
                .comment("If the night fog/sky should be tinted towards the tint colour (deeper on darker moon phases)")
                .define("nightTint", true);
        TINT_RED = b.defineInRange("tintRed", 12, 0, 255);
        TINT_GREEN = b.defineInRange("tintGreen", 16, 0, 255);
        TINT_BLUE = b.defineInRange("tintBlue", 40, 0, 255);
        TINT_STRENGTH = b
                .comment("How strongly the tint colour is mixed into the night fog/sky at a new moon")
                .defineInRange("tintStrength", 0.35D, 0.0D, 1.0D);
        b.pop();

        CLIENT_SPEC = b.build();
    }

    private NyxConfig() {
    }

    public static boolean isLoaded() {
        return CLIENT_SPEC.isLoaded();
    }

    public static boolean darkerNights() {
        return DARKER_NIGHTS.get();
    }

    public static double nightDarkness() {
        return NIGHT_DARKNESS.get();
    }

    public static double moonPhaseInfluence() {
        return MOON_PHASE_INFLUENCE.get();
    }

    public static double minBrightness() {
        return MIN_BRIGHTNESS.get();
    }

    public static boolean darkenFog() {
        return DARKEN_FOG.get();
    }

    public static double fogDarkness() {
        return FOG_DARKNESS.get();
    }

    public static boolean nightTint() {
        return NIGHT_TINT.get();
    }

    public static int tintRed() {
        return TINT_RED.get();
    }

    public static int tintGreen() {
        return TINT_GREEN.get();
    }

    public static int tintBlue() {
        return TINT_BLUE.get();
    }

    public static double tintStrength() {
        return TINT_STRENGTH.get();
    }
}
