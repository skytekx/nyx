package de.ellpeck.nyx.lunarevents;

import de.ellpeck.nyx.Config;
import de.ellpeck.nyx.Nyx;
import de.ellpeck.nyx.capabilities.NyxWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/**
 * The Blood Moon.
 *
 * <p><b>Port note:</b> the 1.12 version reimplemented {@code WorldEntitySpawner}
 * to raise the hostile-mob spawn cap by {@code bloodMoonSpawnMultiplier}. That
 * class no longer exists in a portable form on 1.20.1 (it is now
 * {@code NaturalSpawner} with an entirely different structure), so the density
 * boost is instead applied through the {@code FinalizeSpawn} hook in
 * {@link de.ellpeck.nyx.events.Events}: naturally-spawning monsters during a
 * blood moon are tagged (so they vanish at sunup) and duplicated
 * {@code (multiplier - 1)} times nearby. The visible effect - many more hostile
 * mobs that die at sunrise - is preserved.
 */
public class BloodMoon extends LunarEvent {

    private final ConfigImpl config = new ConfigImpl(() -> Config.bloodMoon);

    public BloodMoon(NyxWorld nyxWorld) {
        super("blood_moon", nyxWorld);
    }

    @Override
    public Component getStartMessage() {
        return Component.translatable("info." + Nyx.ID + ".blood_moon")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withItalic(true));
    }

    @Override
    public boolean shouldStart(boolean lastDaytime) {
        if (Config.bloodMoonOnFull && NyxWorld.getMoonPhaseFactor(this.world) < 1)
            return false;
        if (!lastDaytime || NyxWorld.isDaytime(this.world))
            return false;
        return this.config.canStart();
    }

    @Override
    public boolean shouldStop(boolean lastDaytime) {
        return NyxWorld.isDaytime(this.world);
    }

    @Override
    public int getSkyColor() {
        return 0x420d03;
    }

    @Override
    public String getMoonTexture() {
        return "blood_moon";
    }

    @Override
    public void update(boolean lastDaytime) {
        this.config.update(lastDaytime);
    }

    @Override
    public CompoundTag serializeNBT() {
        return this.config.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.config.deserializeNBT(nbt);
    }
}
