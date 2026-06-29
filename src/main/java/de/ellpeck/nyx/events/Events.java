package de.ellpeck.nyx.events;

import de.ellpeck.nyx.Config;
import de.ellpeck.nyx.Nyx;
import de.ellpeck.nyx.Registration;
import de.ellpeck.nyx.capabilities.NyxWorld;
import de.ellpeck.nyx.entities.CauldronTracker;
import de.ellpeck.nyx.entities.FallingMeteor;
import de.ellpeck.nyx.entities.FallingStar;
import de.ellpeck.nyx.entities.WolfAiSpecialMoon;
import de.ellpeck.nyx.lunarevents.BloodMoon;
import de.ellpeck.nyx.lunarevents.FullMoon;
import de.ellpeck.nyx.lunarevents.HarvestMoon;
import de.ellpeck.nyx.lunarevents.StarShower;
import de.ellpeck.nyx.network.PacketHandler;
import de.ellpeck.nyx.network.PacketNyxWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Nyx.ID)
public final class Events {

    private static final AttributeModifier METEOR_MOVEMENT_MODIFIER = new AttributeModifier(
            UUID.fromString("c1f96acc-e117-4dc1-a351-e295a5de6071"), Nyx.ID + ":meteor_movement_speed", -0.15F, AttributeModifier.Operation.MULTIPLY_TOTAL);

    private Events() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        if (!Config.meteors)
            return;
        Player player = event.player;
        Level level = player.level();

        // meteor armor speed reduction
        if (level.getGameTime() % 20 == 0) {
            AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            String key = Nyx.ID + ":meteor_equipped";
            boolean equipped = countMeteorArmor(player) >= 2;
            if (speed != null) {
                if (equipped && !player.getPersistentData().getBoolean(key)) {
                    player.getPersistentData().putBoolean(key, true);
                    if (speed.getModifier(METEOR_MOVEMENT_MODIFIER.getId()) == null)
                        speed.addTransientModifier(METEOR_MOVEMENT_MODIFIER);
                } else if (!equipped && player.getPersistentData().getBoolean(key)) {
                    player.getPersistentData().putBoolean(key, false);
                    speed.removeModifier(METEOR_MOVEMENT_MODIFIER.getId());
                }
            }
        }

        // meteor hammer slam on landing
        if (player.onGround() && player.fallDistance <= 0 && player.getPersistentData().contains(Nyx.ID + ":leap_start")) {
            if (!level.isClientSide) {
                long leapTime = level.getGameTime() - player.getPersistentData().getLong(Nyx.ID + ":leap_start");
                if (leapTime >= 5) {
                    int r = 3;
                    AABB area = new AABB(player.getX() - r, player.getY() - r, player.getZ() - r, player.getX() + r, player.getY() + r, player.getZ() + r);
                    DamageSource source = level.damageSources().playerAttack(player);
                    float damage = Config.hammerDamage * Math.min((leapTime - 5) / 35F, 1);
                    for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
                        if (entity == player)
                            continue;
                        entity.hurt(source, damage);
                        entity.setDeltaMovement(entity.getDeltaMovement().x, 1, entity.getDeltaMovement().z);
                        entity.hurtMarked = true;
                    }
                    level.playSound(null, player.blockPosition(), Registration.HAMMER_END_SOUND.get(), SoundSource.PLAYERS, 1, 1);
                }
            }
            player.getPersistentData().remove(Nyx.ID + ":leap_start");
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (Config.meteors && event.getEntity().getPersistentData().contains(Nyx.ID + ":leap_start"))
            event.setDamageMultiplier(0);
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (Config.meteors && event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
            int equipped = countMeteorArmor(event.getEntity());
            event.setAmount(event.getAmount() * (1 - 0.1F * equipped));
        }
    }

    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        if (!Config.meteors)
            return;
        ItemStack sword = event.getEntity().getMainHandItem();
        if (sword.getItem() != Registration.METEOR_SWORD.get())
            return;
        if (event.getTarget() instanceof LivingEntity target)
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 10, true, false));
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (!Config.meteors)
            return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker))
            return;
        if (attacker.getMainHandItem().getItem() != Registration.METEOR_AXE.get())
            return;
        if (!(event.getEntity() instanceof Player player) || !player.isUsingItem())
            return;
        ItemStack active = player.getUseItem();
        if (active.getItem() instanceof ShieldItem)
            active.hurtAndBreak(13, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.side != LogicalSide.SERVER)
            return;
        Level world = event.level;
        NyxWorld data = NyxWorld.get(world);
        if (data == null)
            return;
        data.update();

        // Falling stars
        if (Config.fallingStars && !NyxWorld.isDaytime(world) && world.getGameTime() % 20 == 0 && Config.isDimensionAllowed(world)) {
            for (Player player : world.players()) {
                float chanceMult = data.currentEvent instanceof StarShower ? 15 : 1;
                if (world.random.nextFloat() > Config.fallingStarRarity * chanceMult)
                    continue;
                BlockPos startPos = player.blockPosition().offset((int) (world.random.nextGaussian() * 20), 0, (int) (world.random.nextGaussian() * 20));
                startPos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, startPos).above(Mth.nextInt(world.random, 32, 64));

                FallingStar star = Registration.FALLING_STAR.get().create(world);
                if (star != null) {
                    star.setPos(startPos.getX(), startPos.getY(), startPos.getZ());
                    world.addFreshEntity(star);
                }
            }
        }

        // Meteors
        meteors:
        if (Config.meteors && world.getGameTime() % 20 == 0) {
            if (world.players().isEmpty())
                break meteors;
            Player selectedPlayer = world.players().get(world.random.nextInt(world.players().size()));
            if (selectedPlayer == null)
                break meteors;
            double spawnX = selectedPlayer.getX() + Mth.nextDouble(world.random, -Config.meteorSpawnRadius, Config.meteorSpawnRadius);
            double spawnZ = selectedPlayer.getZ() + Mth.nextDouble(world.random, -Config.meteorSpawnRadius, Config.meteorSpawnRadius);
            BlockPos spawnPos = new BlockPos((int) spawnX, 0, (int) spawnZ);
            double chance = Config.getMeteorChance(world, data);
            MutableInt ticksInArea = data.playersPresentTicks.get(new ChunkPos(spawnPos));
            if (ticksInArea != null && ticksInArea.intValue() >= Config.meteorDisallowTime)
                chance /= Math.pow(2, ticksInArea.intValue() / (double) Config.meteorDisallowTime);
            if (chance <= 0 || !(world.random.nextFloat() <= chance))
                break meteors;
            if (!world.isLoaded(spawnPos)) {
                data.cachedMeteorPositions.add(spawnPos);
                data.sendToClients();
            } else {
                FallingMeteor.spawn(world, spawnPos);
            }
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof Level world) || world.isClientSide)
            return;
        NyxWorld data = NyxWorld.get(world);
        if (data == null || !Config.meteors)
            return;
        ChunkPos cp = event.getChunk().getPos();
        List<BlockPos> meteors = data.cachedMeteorPositions.stream()
                .filter(p -> p.getX() >= cp.getMinBlockX() && p.getZ() >= cp.getMinBlockZ() && p.getX() <= cp.getMaxBlockX() && p.getZ() <= cp.getMaxBlockZ())
                .toList();
        for (BlockPos pos : meteors)
            FallingMeteor.spawn(world, pos);
        if (!meteors.isEmpty()) {
            data.cachedMeteorPositions.removeAll(meteors);
            data.sendToClients();
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (Config.bloodMoonVanish && !entity.level().isClientSide && NyxWorld.isDaytime(entity.level())
                && entity.getPersistentData().getBoolean(Nyx.ID + ":blood_moon_spawn")) {
            ((ServerLevel) entity.level()).sendParticles(ParticleTypes.LARGE_SMOKE, entity.getX(), entity.getY(), entity.getZ(), 10, 0.5, 1, 0.5, 0);
            entity.discard();
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        Level world = event.getLevel();
        if (world.isClientSide)
            return;
        NyxWorld nyx = NyxWorld.get(world);
        if (nyx == null)
            return;

        if (entity instanceof ServerPlayer player) {
            PacketHandler.sendTo(player, new PacketNyxWorld(nyx));
        } else if (entity instanceof Wolf wolf) {
            wolf.targetSelector.addGoal(3, new WolfAiSpecialMoon(wolf));
        }
    }

    @SubscribeEvent
    public static void onExpDrop(LivingExperienceDropEvent event) {
        if (!Config.enchantments || !Config.lunarEdgeXp)
            return;
        Player player = event.getAttackingPlayer();
        if (player == null)
            return;
        ItemStack held = player.getMainHandItem();
        int level = EnchantmentHelper.getItemEnchantmentLevel(Registration.LUNAR_EDGE.get(), held);
        if (level <= 0)
            return;
        float exp = event.getDroppedExperience();
        float mod = 2 * (level / (float) Registration.LUNAR_EDGE.get().getMaxLevel());
        event.setDroppedExperience(Mth.floor(exp * mod));
    }

    @SubscribeEvent
    public static void onEntityDrop(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof ElderGuardian && !entity.level().isClientSide && entity.level().random.nextDouble() <= Config.meteorShardGuardianChance) {
            ItemStack stack = new ItemStack(Registration.METEOR_SHARD.get(), (event.getLootingLevel() / 2) + 1);
            event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), stack));
        }
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        Mob entity = event.getEntity();
        if (!(entity instanceof Enemy))
            return;
        NyxWorld nyx = NyxWorld.get(entity.level());
        if (nyx == null)
            return;
        boolean natural = event.getSpawnType() == MobSpawnType.NATURAL;

        // Don't spawn (non-slime) monsters during the harvest moon
        if (natural && !(entity instanceof Slime) && nyx.currentEvent instanceof HarvestMoon) {
            event.setSpawnCancelled(true);
            return;
        }

        // Bigger slimes during full/harvest moon
        if (entity instanceof Slime slime) {
            int size = slime.getSize();
            if (nyx.currentEvent instanceof FullMoon) {
                int i = slime.level().random.nextInt(5);
                if (i <= 1)
                    size += 2;
                if (i <= 2)
                    size += 2;
            } else if (nyx.currentEvent instanceof HarvestMoon) {
                int i = slime.level().random.nextInt(15);
                if (i < 8)
                    size += i * 2;
            }
            if (size != slime.getSize())
                slime.setSize(size, true);
        }

        if (nyx.currentEvent instanceof FullMoon) {
            // random effect
            if (Config.addPotionEffects && !(entity instanceof net.minecraft.world.entity.monster.Creeper)) {
                MobEffect effect = null;
                int i = entity.level().random.nextInt(20);
                if (i <= 2) {
                    effect = MobEffects.MOVEMENT_SPEED;
                } else if (i <= 4) {
                    effect = MobEffects.DAMAGE_BOOST;
                } else if (i <= 6) {
                    effect = MobEffects.REGENERATION;
                } else if (i <= 7) {
                    effect = MobEffects.INVISIBILITY;
                }
                if (effect != null)
                    entity.addEffect(new MobEffectInstance(effect, Integer.MAX_VALUE));
            }
            if (natural && Config.additionalMobsChance > 0 && entity.level().random.nextInt(Config.additionalMobsChance) == 0)
                doExtraSpawn(entity, "full_moon_spawn");
        }

        // Blood moon density boost (port-adapted; see BloodMoon)
        if (natural && nyx.currentEvent instanceof BloodMoon) {
            entity.getPersistentData().putBoolean(Nyx.ID + ":blood_moon_spawn", true);
            int extra = Config.bloodMoonSpawnMultiplier - 1;
            for (int i = 0; i < extra; i++)
                spawnCopy(entity, "blood_moon_spawn");
        }
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level world = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = world.getBlockState(pos);
        NyxWorld nyx = NyxWorld.get(world);

        ench:
        if (Config.disallowDayEnchanting) {
            long time = world.getDayTime() % 24000;
            if (time > 13000 && time < 23000)
                break ench;
            if (!(state.getBlock() instanceof EnchantmentTableBlock))
                break ench;
            if (Config.isEnchantingWhitelisted(world))
                break ench;
            event.setUseBlock(Event.Result.DENY);
            player.displayClientMessage(Component.translatable("info." + Nyx.ID + ".day_enchanting"), true);
        }

        lunar:
        if (Config.lunarWater) {
            if (!state.is(Blocks.CAULDRON))
                break lunar;
            ItemStack holding = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (holding.isEmpty())
                break lunar;
            boolean hasLunarWater = FluidUtil.getFluidContained(holding)
                    .map(fs -> fs.getFluid() == Registration.LUNAR_WATER_FLUID.get() && fs.getAmount() >= 1000)
                    .orElse(false);
            if (!hasLunarWater)
                break lunar;

            if (!world.isClientSide) {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.BUCKET));
                world.setBlockAndUpdate(pos, Registration.LUNAR_WATER_CAULDRON.get().defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
            }
            player.swing(InteractionHand.MAIN_HAND);
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        }

        if (nyx != null && nyx.currentEvent instanceof BloodMoon && !Config.bloodMoonSleeping && state.getBlock() instanceof BedBlock)
            player.displayClientMessage(Component.translatable("info." + Nyx.ID + ".blood_moon_sleeping"), true);
    }

    @SubscribeEvent
    public static void onBlockChanged(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof Level world) || world.isClientSide)
            return;
        BlockPos pos = event.getPos();
        BlockState state = world.getBlockState(pos);
        if (!state.is(Blocks.WATER_CAULDRON))
            return;
        if (!world.getEntitiesOfClass(CauldronTracker.class, new AABB(pos)).isEmpty())
            return;
        CauldronTracker tracker = new CauldronTracker(Registration.CAULDRON_TRACKER.get(), world);
        tracker.setTrackingPos(pos);
        world.addFreshEntity(tracker);
    }

    @SubscribeEvent
    public static void onSleep(PlayerSleepInBedEvent event) {
        Player player = event.getEntity();
        NyxWorld nyx = NyxWorld.get(player.level());
        if (nyx != null && nyx.currentEvent instanceof BloodMoon && !Config.bloodMoonSleeping)
            event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
    }

    private static int countMeteorArmor(LivingEntity entity) {
        int count = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            if (stack.getItem() instanceof ArmorItem armor && armor.getMaterial() == Registration.METEOR_ARMOR)
                count++;
        }
        return count;
    }

    private static void doExtraSpawn(LivingEntity original, String key) {
        if (!original.getPersistentData().getBoolean(Nyx.ID + ":" + key))
            spawnCopy(original, key);
    }

    /** Spawns one tagged duplicate of the given mob nearby (full/blood moon density). */
    private static boolean spawnCopy(LivingEntity original, String key) {
        ResourceLocation name = ForgeRegistries.ENTITY_TYPES.getKey(original.getType());
        if (name == null)
            return false;
        boolean listed = Config.mobDuplicationBlacklist.contains(name.toString());
        if (Config.isMobDuplicationWhitelist != listed)
            return false;
        Level world = original.level();
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (x == 0 && y == 0 && z == 0)
                        continue;
                    BlockPos offset = original.blockPosition().offset(x, y, z);
                    if (!NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND, world, offset, original.getType()))
                        continue;
                    Entity entity = original.getType().create(world);
                    if (!(entity instanceof Mob mob))
                        return false;
                    mob.moveTo(original.getX() + x, original.getY() + y, original.getZ() + z, Mth.wrapDegrees(world.random.nextFloat() * 360), 0);
                    mob.setYHeadRot(mob.getYRot());
                    mob.setYBodyRot(mob.getYRot());
                    mob.getPersistentData().putBoolean(Nyx.ID + ":" + key, true);
                    if (world instanceof ServerLevel sl)
                        mob.finalizeSpawn(sl, world.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
                    world.addFreshEntity(entity);
                    return true;
                }
            }
        }
        return false;
    }
}
