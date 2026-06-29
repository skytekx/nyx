package de.ellpeck.nyx.lunarevents;

import de.ellpeck.nyx.Config;
import de.ellpeck.nyx.Nyx;
import de.ellpeck.nyx.capabilities.NyxWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public class StarShower extends LunarEvent {

    private final ConfigImpl config = new ConfigImpl(() -> Config.starShowers);

    public StarShower(NyxWorld nyxWorld) {
        super("star_shower", nyxWorld);
    }

    @Override
    public Component getStartMessage() {
        return Component.translatable("info." + Nyx.ID + ".star_shower")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withItalic(true));
    }

    @Override
    public boolean shouldStart(boolean lastDaytime) {
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
        return 0xdec25f;
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
