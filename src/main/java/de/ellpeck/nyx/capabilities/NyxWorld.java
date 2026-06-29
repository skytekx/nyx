package de.ellpeck.nyx.capabilities;

import de.ellpeck.nyx.Config;
import de.ellpeck.nyx.lunarevents.BloodMoon;
import de.ellpeck.nyx.lunarevents.FullMoon;
import de.ellpeck.nyx.lunarevents.HarvestMoon;
import de.ellpeck.nyx.lunarevents.LunarEvent;
import de.ellpeck.nyx.lunarevents.StarShower;
import de.ellpeck.nyx.network.PacketHandler;
import de.ellpeck.nyx.network.PacketNyxWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The central per-{@link Level} state of the mod, attached as a Forge
 * capability. It tracks the currently running lunar event, cached meteor spawn
 * positions, meteor landing sites, how long players have been present in chunks
 * (for meteor rate-limiting) and which dimensions have been visited (gate).
 *
 * <p>Ported from the 1.12 {@code ICapabilityProvider}/{@code INBTSerializable}
 * version to 1.20.1's {@link ICapabilitySerializable} + {@link LazyOptional}.
 */
public class NyxWorld implements ICapabilitySerializable<CompoundTag> {

    /** Vanilla per-phase moon brightness used by the moon-scaled enchantments (1.0 at full moon). */
    public static float moonPhase;

    public final Level world;
    public final List<LunarEvent> lunarEvents = new ArrayList<>();
    public final Set<BlockPos> cachedMeteorPositions = new HashSet<>();
    public final Map<ChunkPos, MutableInt> playersPresentTicks = new HashMap<>();
    public final Set<BlockPos> meteorLandingSites = new HashSet<>();
    public final Set<String> visitedDimensions = new HashSet<>();
    public float eventSkyModifier;
    public int currentSkyColor;
    public LunarEvent currentEvent;
    public LunarEvent forcedEvent;

    private final LazyOptional<NyxWorld> holder = LazyOptional.of(() -> this);
    private boolean wasDaytime;

    public NyxWorld(Level world) {
        this.world = world;
        this.lunarEvents.add(new HarvestMoon(this));
        this.lunarEvents.add(new StarShower(this));
        this.lunarEvents.add(new BloodMoon(this));
        // this needs to stay at the end to prioritize random events
        this.lunarEvents.add(new FullMoon(this));
    }

    public void update() {
        if (!Config.isLoaded())
            return;
        if (!this.world.isClientSide) {
            // add to visited dimensions list
            if (this.world.getGameTime() % 200 == 0 && this.world.getServer() != null) {
                for (Player player : this.world.getServer().getPlayerList().getPlayers())
                    this.visitedDimensions.add(Config.dimensionName(player.level()));
            }

            // calculate which chunks have players close to them for meteor spawning
            int interval = 100;
            if (Config.meteors && this.world.getGameTime() % interval == 0) {
                Set<ChunkPos> remaining = new HashSet<>(this.playersPresentTicks.keySet());
                for (Player player : this.world.players()) {
                    for (int x = -Config.meteorDisallowRadius; x <= Config.meteorDisallowRadius; x++) {
                        for (int z = -Config.meteorDisallowRadius; z <= Config.meteorDisallowRadius; z++) {
                            ChunkPos pos = new ChunkPos(Mth.floor(player.getX() / 16) + x, Mth.floor(player.getZ() / 16) + z);
                            MutableInt time = this.playersPresentTicks.computeIfAbsent(pos, p -> new MutableInt());
                            time.add(interval);
                            remaining.remove(pos);
                        }
                    }
                }
                if (!remaining.isEmpty()) {
                    for (ChunkPos pos : remaining) {
                        MutableInt time = this.playersPresentTicks.get(pos);
                        time.subtract(interval);
                        if (time.intValue() <= 0)
                            this.playersPresentTicks.remove(pos);
                    }
                }
            }
        }

        // moon event stuff
        if (Config.isDimensionAllowed(this.world)) {
            moonPhase = getMoonPhaseFactor(this.world);

            for (LunarEvent event : this.lunarEvents)
                event.update(this.wasDaytime);

            if (!this.world.isClientSide) {
                boolean isDirty = false;

                if (this.currentEvent == null) {
                    if (this.forcedEvent != null && this.forcedEvent.shouldStart(this.wasDaytime)) {
                        this.currentEvent = this.forcedEvent;
                        this.forcedEvent = null;
                    } else {
                        for (LunarEvent event : this.lunarEvents) {
                            if (event.shouldStart(this.wasDaytime)) {
                                this.currentEvent = event;
                                break;
                            }
                        }
                    }
                    if (this.currentEvent != null) {
                        isDirty = true;
                        if (Config.eventNotifications) {
                            Component text = this.currentEvent.getStartMessage();
                            for (Player player : this.world.players())
                                player.sendSystemMessage(text);
                        }
                    }
                }

                if (this.currentEvent != null && this.currentEvent.shouldStop(this.wasDaytime)) {
                    this.currentEvent = null;
                    isDirty = true;
                }

                if (isDirty)
                    this.sendToClients();

                this.wasDaytime = isDaytime(this.world);
            } else {
                if (this.currentEvent != null && this.currentSkyColor != 0) {
                    if (this.eventSkyModifier < 1)
                        this.eventSkyModifier += 0.01F;
                } else {
                    if (this.eventSkyModifier > 0)
                        this.eventSkyModifier -= 0.01F;
                }
            }
        }
    }

    public void sendToClients() {
        for (Player player : this.world.players()) {
            if (player instanceof ServerPlayer sp)
                PacketHandler.sendTo(sp, new PacketNyxWorld(this));
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        return this.serializeNBT(false);
    }

    public CompoundTag serializeNBT(boolean client) {
        CompoundTag compound = new CompoundTag();
        if (this.currentEvent != null)
            compound.putString("event", this.currentEvent.name);
        compound.putBoolean("was_daytime", this.wasDaytime);
        for (LunarEvent event : this.lunarEvents)
            compound.put(event.name, event.serializeNBT());
        ListTag landings = new ListTag();
        for (BlockPos pos : this.meteorLandingSites)
            landings.add(LongTag.valueOf(pos.asLong()));
        compound.put("meteor_landings", landings);
        ListTag meteors = new ListTag();
        for (BlockPos pos : this.cachedMeteorPositions)
            meteors.add(LongTag.valueOf(pos.asLong()));
        compound.put("cached_meteors", meteors);
        if (!client) {
            ListTag ticks = new ListTag();
            for (Map.Entry<ChunkPos, MutableInt> e : this.playersPresentTicks.entrySet()) {
                CompoundTag comp = new CompoundTag();
                comp.putInt("x", e.getKey().x);
                comp.putInt("z", e.getKey().z);
                comp.putInt("ticks", e.getValue().intValue());
                ticks.add(comp);
            }
            compound.put("players_present_ticks", ticks);
            ListTag dimensions = new ListTag();
            for (String dim : this.visitedDimensions)
                dimensions.add(StringTag.valueOf(dim));
            compound.put("visited_dims", dimensions);
        }
        return compound;
    }

    @Override
    public void deserializeNBT(CompoundTag compound) {
        this.deserializeNBT(compound, false);
    }

    public void deserializeNBT(CompoundTag compound, boolean client) {
        String name = compound.getString("event");
        this.currentEvent = this.lunarEvents.stream().filter(e -> e.name.equals(name)).findFirst().orElse(null);
        if (this.currentEvent != null)
            this.currentSkyColor = this.currentEvent.getSkyColor();
        this.wasDaytime = compound.getBoolean("was_daytime");
        for (LunarEvent event : this.lunarEvents)
            event.deserializeNBT(compound.getCompound(event.name));
        this.meteorLandingSites.clear();
        ListTag landings = compound.getList("meteor_landings", Tag.TAG_LONG);
        for (int i = 0; i < landings.size(); i++)
            this.meteorLandingSites.add(BlockPos.of(((LongTag) landings.get(i)).getAsLong()));
        this.cachedMeteorPositions.clear();
        ListTag meteors = compound.getList("cached_meteors", Tag.TAG_LONG);
        for (int i = 0; i < meteors.size(); i++)
            this.cachedMeteorPositions.add(BlockPos.of(((LongTag) meteors.get(i)).getAsLong()));
        if (!client) {
            this.playersPresentTicks.clear();
            ListTag ticks = compound.getList("players_present_ticks", Tag.TAG_COMPOUND);
            for (int i = 0; i < ticks.size(); i++) {
                CompoundTag comp = ticks.getCompound(i);
                this.playersPresentTicks.put(new ChunkPos(comp.getInt("x"), comp.getInt("z")), new MutableInt(comp.getInt("ticks")));
            }
            this.visitedDimensions.clear();
            ListTag dimensions = compound.getList("visited_dims", Tag.TAG_STRING);
            for (int i = 0; i < dimensions.size(); i++)
                this.visitedDimensions.add(dimensions.getString(i));
        }
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == NyxCapabilities.WORLD ? this.holder.cast() : LazyOptional.empty();
    }

    public static float getMoonPhaseFactor(Level level) {
        int phase = level.dimensionType().moonPhase(level.getDayTime());
        return DimensionType.MOON_BRIGHTNESS_PER_PHASE[phase];
    }

    public static boolean isDaytime(Level world) {
        // night is between 12541 and 23458 ticks, when stars appear in the sky
        long time = world.getDayTime() % 24000;
        return !(time >= 12541 && time < 23458);
    }

    public static NyxWorld get(Level world) {
        if (world == null)
            return null;
        return world.getCapability(NyxCapabilities.WORLD).orElse(null);
    }
}
