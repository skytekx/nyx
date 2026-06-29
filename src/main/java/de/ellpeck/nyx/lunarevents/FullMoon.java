package de.ellpeck.nyx.lunarevents;

import de.ellpeck.nyx.Config;
import de.ellpeck.nyx.Nyx;
import de.ellpeck.nyx.capabilities.NyxWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public class FullMoon extends LunarEvent {

    public FullMoon(NyxWorld nyxWorld) {
        super("full_moon", nyxWorld);
    }

    @Override
    public Component getStartMessage() {
        return Component.translatable("info." + Nyx.ID + ".full_moon")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true));
    }

    @Override
    public boolean shouldStart(boolean lastDaytime) {
        if (!Config.fullMoon)
            return false;
        if (!lastDaytime || NyxWorld.isDaytime(this.world))
            return false;
        return NyxWorld.getMoonPhaseFactor(this.world) >= 1;
    }

    @Override
    public boolean shouldStop(boolean lastDaytime) {
        return NyxWorld.isDaytime(this.world);
    }
}
