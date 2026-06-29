package de.ellpeck.nyx.lunarevents;

import de.ellpeck.nyx.Config;
import de.ellpeck.nyx.capabilities.NyxWorld;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.function.Supplier;

public abstract class LunarEvent implements INBTSerializable<CompoundTag> {

    public final String name;
    protected final NyxWorld nyxWorld;
    protected final Level world;

    public LunarEvent(String name, NyxWorld nyxWorld) {
        this.name = name;
        this.nyxWorld = nyxWorld;
        this.world = nyxWorld.world;
    }

    public abstract Component getStartMessage();

    public abstract boolean shouldStart(boolean lastDaytime);

    public abstract boolean shouldStop(boolean lastDaytime);

    public int getSkyColor() {
        return 0;
    }

    public String getMoonTexture() {
        return null;
    }

    public void update(boolean lastDaytime) {
    }

    @Override
    public CompoundTag serializeNBT() {
        return new CompoundTag();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
    }

    public class ConfigImpl implements INBTSerializable<CompoundTag> {

        public final Supplier<Config.LunarEventConfig> config;
        public int daysSinceLast;
        public int startDays;
        public int graceDays;

        public ConfigImpl(Supplier<Config.LunarEventConfig> config) {
            this.config = config;
        }

        public void update(boolean lastDaytime) {
            if (LunarEvent.this.nyxWorld.currentEvent == LunarEvent.this) {
                this.daysSinceLast = 0;
                this.graceDays = 0;
            }

            if (!lastDaytime && NyxWorld.isDaytime(LunarEvent.this.world)) {
                this.daysSinceLast++;
                if (this.startDays < this.config.get().startNight)
                    this.startDays++;
                if (this.graceDays < this.config.get().graceDays)
                    this.graceDays++;
            }
        }

        public boolean canStart() {
            if (LunarEvent.this.nyxWorld.forcedEvent == LunarEvent.this)
                return true;
            if (!this.config.get().enabled)
                return false;
            if (this.startDays < this.config.get().startNight)
                return false;
            if (this.graceDays < this.config.get().graceDays)
                return false;
            if (this.config.get().nightInterval > 0) {
                return this.daysSinceLast >= this.config.get().nightInterval;
            } else {
                return LunarEvent.this.world.random.nextDouble() <= this.config.get().chance;
            }
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag compound = new CompoundTag();
            compound.putInt("days_since_last", this.daysSinceLast);
            compound.putInt("start_days", this.startDays);
            compound.putInt("grace_days", this.graceDays);
            return compound;
        }

        @Override
        public void deserializeNBT(CompoundTag compound) {
            this.daysSinceLast = compound.getInt("days_since_last");
            this.startDays = compound.getInt("start_days");
            this.graceDays = compound.getInt("grace_days");
        }
    }
}
