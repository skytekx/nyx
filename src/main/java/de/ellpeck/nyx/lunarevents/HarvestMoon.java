package de.ellpeck.nyx.lunarevents;

import de.ellpeck.nyx.Config;
import de.ellpeck.nyx.Nyx;
import de.ellpeck.nyx.capabilities.NyxWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashSet;
import java.util.Set;

public class HarvestMoon extends LunarEvent {

    private final ConfigImpl config = new ConfigImpl(() -> Config.harvestMoon);

    public HarvestMoon(NyxWorld nyxWorld) {
        super("harvest_moon", nyxWorld);
    }

    @Override
    public Component getStartMessage() {
        return Component.translatable("info." + Nyx.ID + ".harvest_moon")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.BLUE).withItalic(true));
    }

    @Override
    public boolean shouldStart(boolean lastDaytime) {
        if (Config.harvestMoonOnFull && NyxWorld.getMoonPhaseFactor(this.world) < 1)
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
        return 0x3f3fc0;
    }

    @Override
    public String getMoonTexture() {
        return "harvest_moon";
    }

    @Override
    public void update(boolean lastDaytime) {
        this.config.update(lastDaytime);

        if (this.world.isClientSide || this.nyxWorld.currentEvent != this || Config.harvestMoonGrowAmount <= 0)
            return;
        if (this.world.getGameTime() % Config.harvestMoonGrowInterval != 0)
            return;
        if (!(this.world instanceof ServerLevel level))
            return;

        // 1.12 iterated every loaded chunk; 1.20.1 has no clean public "all loaded chunks"
        // accessor, so we grow plants in chunks around each player instead.
        Set<ChunkPos> done = new HashSet<>();
        int radius = 6;
        for (Player player : level.players()) {
            ChunkPos center = player.chunkPosition();
            for (int cx = -radius; cx <= radius; cx++) {
                for (int cz = -radius; cz <= radius; cz++) {
                    ChunkPos cp = new ChunkPos(center.x + cx, center.z + cz);
                    if (!done.add(cp) || !level.hasChunk(cp.x, cp.z))
                        continue;
                    for (int i = 0; i < Config.harvestMoonGrowAmount; i++) {
                        int x = cp.getMinBlockX() + level.random.nextInt(16);
                        int z = cp.getMinBlockZ() + level.random.nextInt(16);
                        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(pos);
                        if (!(state.getBlock() instanceof BonemealableBlock growable)
                                || state.getBlock() instanceof GrassBlock
                                || state.getBlock() instanceof TallGrassBlock
                                || state.getBlock() instanceof DoublePlantBlock)
                            continue;
                        try {
                            if (growable.isValidBonemealTarget(level, pos, state, false))
                                growable.performBonemeal(level, level.random, pos, state);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }
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
