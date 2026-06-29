package de.ellpeck.nyx;

import de.ellpeck.nyx.capabilities.NyxWorld;
import de.ellpeck.nyx.lunarevents.StarShower;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Server/common gameplay configuration, ported from the original
 * {@code net.minecraftforge.common.config.Configuration} to a
 * {@link ForgeConfigSpec}. The original code read plenty of plain static
 * fields ({@code Config.meteors}, {@code Config.fallingStars}, ...); to keep the
 * rest of the port close to the source those cached fields are preserved and
 * {@link #bake()} refreshes them whenever the config (re)loads.
 *
 * <p>Dimension entries are now full resource locations (e.g.
 * {@code minecraft:overworld}); {@link #dimensionMatches} also accepts the bare
 * path the 1.12 config used.
 */
public final class Config {

    public static final ForgeConfigSpec COMMON_SPEC;

    // cached values (refreshed in bake())
    public static Set<String> allowedDimensions;
    public static boolean enchantments;
    public static boolean lunarWater;
    public static boolean addPotionEffects;
    public static int additionalMobsChance;
    public static boolean lunarEdgeXp;
    public static boolean disallowDayEnchanting;
    public static double meteorShardGuardianChance;
    public static boolean fallingStars;
    public static double fallingStarRarity;
    public static boolean fullMoon;
    public static boolean bloodMoonSleeping;
    public static int bloodMoonSpawnMultiplier;
    public static Set<String> mobDuplicationBlacklist;
    public static boolean isMobDuplicationWhitelist;
    public static boolean bloodMoonVanish;
    public static int bloodMoonSpawnRadius;
    public static boolean harvestMoonOnFull;
    public static boolean bloodMoonOnFull;
    public static boolean moonEventTint;
    public static int harvestMoonGrowAmount;
    public static int harvestMoonGrowInterval;
    public static LunarEventConfig harvestMoon;
    public static LunarEventConfig starShowers;
    public static LunarEventConfig bloodMoon;
    public static List<? extends Integer> lunarWaterTicks;
    public static double meteorChance;
    public static double meteorChanceNight;
    public static String meteorGateDimension;
    public static double meteorChanceAfterGate;
    public static double meteorChanceAfterGateNight;
    public static double meteorChanceStarShower;
    public static double meteorChanceEnd;
    public static int meteorSpawnRadius;
    public static boolean meteors;
    public static int meteorDisallowRadius;
    public static int meteorDisallowTime;
    public static Set<String> enchantingWhitelistDimensions;
    public static boolean eventNotifications;
    public static int crystalDurability;
    public static int hammerDamage;

    // raw config values
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ALLOWED_DIMENSIONS;
    private static final ForgeConfigSpec.BooleanValue LUNAR_WATER;
    private static final ForgeConfigSpec.DoubleValue METEOR_SHARD_GUARDIAN_CHANCE;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> MOB_DUPLICATION_BLACKLIST;
    private static final ForgeConfigSpec.BooleanValue IS_MOB_DUPLICATION_WHITELIST;
    private static final ForgeConfigSpec.BooleanValue MOON_EVENT_TINT;
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> LUNAR_WATER_TICKS;
    private static final ForgeConfigSpec.BooleanValue EVENT_NOTIFICATIONS;

    private static final ForgeConfigSpec.BooleanValue FULL_MOON;
    private static final ForgeConfigSpec.BooleanValue ADD_POTION_EFFECTS;
    private static final ForgeConfigSpec.IntValue ADDITIONAL_MOBS_CHANCE;

    private static final ForgeConfigSpec.BooleanValue ENCHANTMENTS;
    private static final ForgeConfigSpec.BooleanValue LUNAR_EDGE_XP;
    private static final ForgeConfigSpec.BooleanValue DISALLOW_DAY_ENCHANTING;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENCHANTING_WHITELIST_DIMENSIONS;

    private static final LunarEventSpec HARVEST_MOON;
    private static final ForgeConfigSpec.BooleanValue HARVEST_MOON_ON_FULL;
    private static final ForgeConfigSpec.IntValue HARVEST_MOON_GROW_AMOUNT;
    private static final ForgeConfigSpec.IntValue HARVEST_MOON_GROW_INTERVAL;

    private static final LunarEventSpec STAR_SHOWERS;
    private static final ForgeConfigSpec.BooleanValue FALLING_STARS;
    private static final ForgeConfigSpec.DoubleValue FALLING_STAR_RARITY;

    private static final LunarEventSpec BLOOD_MOON;
    private static final ForgeConfigSpec.BooleanValue BLOOD_MOON_SLEEPING;
    private static final ForgeConfigSpec.IntValue BLOOD_MOON_SPAWN_MULTIPLIER;
    private static final ForgeConfigSpec.BooleanValue BLOOD_MOON_VANISH;
    private static final ForgeConfigSpec.IntValue BLOOD_MOON_SPAWN_RADIUS;
    private static final ForgeConfigSpec.BooleanValue BLOOD_MOON_ON_FULL;

    private static final ForgeConfigSpec.BooleanValue METEORS;
    private static final ForgeConfigSpec.DoubleValue METEOR_CHANCE;
    private static final ForgeConfigSpec.DoubleValue METEOR_CHANCE_NIGHT;
    private static final ForgeConfigSpec.ConfigValue<String> METEOR_GATE_DIMENSION;
    private static final ForgeConfigSpec.DoubleValue METEOR_CHANCE_AFTER_GATE;
    private static final ForgeConfigSpec.DoubleValue METEOR_CHANCE_AFTER_GATE_NIGHT;
    private static final ForgeConfigSpec.DoubleValue METEOR_CHANCE_STAR_SHOWER;
    private static final ForgeConfigSpec.DoubleValue METEOR_CHANCE_END;
    private static final ForgeConfigSpec.IntValue METEOR_SPAWN_RADIUS;
    private static final ForgeConfigSpec.IntValue METEOR_DISALLOW_RADIUS;
    private static final ForgeConfigSpec.IntValue METEOR_DISALLOW_TIME;
    private static final ForgeConfigSpec.IntValue CRYSTAL_DURABILITY;
    private static final ForgeConfigSpec.IntValue HAMMER_DAMAGE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("general");
        ALLOWED_DIMENSIONS = b.comment("Resource locations of the dimensions that lunar events should occur in")
                .defineList("allowedDimensions", List.of("minecraft:overworld"), o -> o instanceof String);
        LUNAR_WATER = b.comment("If lunar water should be enabled").define("lunarWater", true);
        METEOR_SHARD_GUARDIAN_CHANCE = b.comment("The chance (1 = 100%) for a meteor shard to be dropped from an elder guardian")
                .defineInRange("meteorShardGuardianChance", 0.05, 0, 1);
        MOB_DUPLICATION_BLACKLIST = b.comment("Registry names of entities that should not be spawned during the full and blood moons (or a whitelist if isMobDuplicationWhitelist)")
                .defineList("mobDuplicationBlacklist", List.of(), o -> o instanceof String);
        IS_MOB_DUPLICATION_WHITELIST = b.comment("If the mobDuplicationBlacklist should act as a whitelist instead").define("isMobDuplicationWhitelist", false);
        MOON_EVENT_TINT = b.comment("If moon events should tint the sky").define("moonEventTint", true);
        LUNAR_WATER_TICKS = b.comment("Ticks a cauldron of water must be exposed to the night sky per moon phase: Full, new, waning crescent, waxing crescent, third quarter, first quarter, waning gibbous, waxing gibbous, harvest moon, blood moon. -1 disables that phase.")
                .defineList("lunarWaterTicks", List.of(1200, -1, 4800, 4800, 3600, 3600, 2400, 2400, 600, -1), o -> o instanceof Integer);
        EVENT_NOTIFICATIONS = b.comment("If moon events should be announced in chat when they start").define("eventNotifications", true);
        b.pop();

        b.push("fullMoon");
        FULL_MOON = b.comment("If the vanilla full moon should be considered a proper lunar event").define("fullMoon", true);
        ADD_POTION_EFFECTS = b.comment("If mobs spawned during a full moon should have random potion effects").define("addPotionEffects", true);
        ADDITIONAL_MOBS_CHANCE = b.comment("Chance for an additional mob to spawn during a full moon (higher = less likely, 0 disables)")
                .defineInRange("additionalMobsChance", 5, 0, 1000);
        b.pop();

        b.push("enchantments");
        ENCHANTMENTS = b.comment("If the enchantments should be enabled").define("enchantments", true);
        LUNAR_EDGE_XP = b.comment("If a weapon enchanted with lunar edge should increase experience drops").define("lunarEdgeXp", true);
        DISALLOW_DAY_ENCHANTING = b.comment("If enchanting should be disallowed during the day").define("disallowDayEnchanting", true);
        ENCHANTING_WHITELIST_DIMENSIONS = b.comment("Dimensions where enchanting is always allowed, not just at night")
                .defineList("enchantingWhitelistDimensions", List.of("minecraft:the_nether", "minecraft:the_end"), o -> o instanceof String);
        b.pop();

        b.push("harvestMoon");
        HARVEST_MOON = new LunarEventSpec(b, "harvestMoon", "Harvest Moon", 0.05);
        HARVEST_MOON_ON_FULL = b.comment("If the harvest moon should only occur on full moon nights").define("harvestMoonOnFull", true);
        HARVEST_MOON_GROW_AMOUNT = b.comment("Plants grown per chunk during the harvest moon").defineInRange("harvestMoonGrowAmount", 15, 0, 100);
        HARVEST_MOON_GROW_INTERVAL = b.comment("Ticks between plant growth during the harvest moon").defineInRange("harvestMoonGrowInterval", 10, 1, 100);
        b.pop();

        b.push("fallingStars");
        STAR_SHOWERS = new LunarEventSpec(b, "starShowers", "Star Showers", 0.05);
        FALLING_STARS = b.comment("If falling stars should be enabled").define("fallingStars", true);
        FALLING_STAR_RARITY = b.comment("Chance (1 = 100%) for a falling star to appear at night for each player each second")
                .defineInRange("fallingStarRarity", 0.01, 0, 1);
        b.pop();

        b.push("bloodMoon");
        BLOOD_MOON = new LunarEventSpec(b, "bloodMoon", "Blood Moon", 0.05);
        BLOOD_MOON_SLEEPING = b.comment("If sleeping is allowed during a blood moon").define("bloodMoonSleeping", false);
        BLOOD_MOON_SPAWN_MULTIPLIER = b.comment("Multiplier with which mobs spawn during the blood moon").defineInRange("bloodMoonSpawnMultiplier", 2, 1, 1000);
        BLOOD_MOON_VANISH = b.comment("If mobs spawned by the blood moon should die at sunup").define("bloodMoonVanish", true);
        BLOOD_MOON_SPAWN_RADIUS = b.comment("Closest distance mobs can spawn from a player during the blood moon").defineInRange("bloodMoonSpawnRadius", 20, 0, 256);
        BLOOD_MOON_ON_FULL = b.comment("If the blood moon should only occur on full moon nights").define("bloodMoonOnFull", true);
        b.pop();

        b.push("meteors");
        METEORS = b.comment("If meteor content should be enabled").define("meteors", true);
        METEOR_CHANCE = b.comment("Chance of a meteor spawning every second, during the day").defineInRange("meteorChance", 0.00014, 0, 1);
        METEOR_CHANCE_NIGHT = b.comment("Chance of a meteor spawning every second, during nighttime").defineInRange("meteorChanceNight", 0.0024, 0, 1);
        METEOR_GATE_DIMENSION = b.comment("Dimension that needs to be entered to increase meteor spawning").define("meteorGateDimension", "minecraft:the_nether");
        METEOR_CHANCE_AFTER_GATE = b.comment("Daytime meteor chance after the gate dimension was entered").defineInRange("meteorChanceAfterGate", 0.0002, 0, 1);
        METEOR_CHANCE_AFTER_GATE_NIGHT = b.comment("Nighttime meteor chance after the gate dimension was entered").defineInRange("meteorChanceAfterGateNight", 0.003, 0, 1);
        METEOR_CHANCE_STAR_SHOWER = b.comment("Chance of a meteor spawning every second during a star shower").defineInRange("meteorChanceStarShower", 0.0075, 0, 1);
        METEOR_CHANCE_END = b.comment("Chance of a meteor spawning every second in the end dimension").defineInRange("meteorChanceEnd", 0.003, 0, 1);
        METEOR_SPAWN_RADIUS = b.comment("Blocks a meteor can spawn away from the nearest player").defineInRange("meteorSpawnRadius", 1000, 1, 10000);
        METEOR_DISALLOW_RADIUS = b.comment("Radius in chunks marked invalid for meteor spawning around each player").defineInRange("meteorDisallowRadius", 16, 0, 64);
        METEOR_DISALLOW_TIME = b.comment("Ticks per player until the chance of a meteor spawning in the area is halved").defineInRange("meteorDisallowTime", 12000, 1, 100000);
        CRYSTAL_DURABILITY = b.comment("Uses a gleaning crystal has for bone-mealing").defineInRange("crystalDurability", 1000, 1, 100000);
        HAMMER_DAMAGE = b.comment("Damage the meteor hammer deals at maximum flight time").defineInRange("hammerDamage", 15, 0, 1000);
        b.pop();

        COMMON_SPEC = b.build();
    }

    public static void bake() {
        allowedDimensions = new HashSet<>(ALLOWED_DIMENSIONS.get());
        lunarWater = LUNAR_WATER.get();
        meteorShardGuardianChance = METEOR_SHARD_GUARDIAN_CHANCE.get();
        mobDuplicationBlacklist = new HashSet<>(MOB_DUPLICATION_BLACKLIST.get());
        isMobDuplicationWhitelist = IS_MOB_DUPLICATION_WHITELIST.get();
        moonEventTint = MOON_EVENT_TINT.get();
        lunarWaterTicks = LUNAR_WATER_TICKS.get();
        eventNotifications = EVENT_NOTIFICATIONS.get();

        fullMoon = FULL_MOON.get();
        addPotionEffects = ADD_POTION_EFFECTS.get();
        additionalMobsChance = ADDITIONAL_MOBS_CHANCE.get();

        enchantments = ENCHANTMENTS.get();
        lunarEdgeXp = LUNAR_EDGE_XP.get();
        disallowDayEnchanting = DISALLOW_DAY_ENCHANTING.get();
        enchantingWhitelistDimensions = new HashSet<>(ENCHANTING_WHITELIST_DIMENSIONS.get());

        harvestMoon = HARVEST_MOON.bake();
        harvestMoonOnFull = HARVEST_MOON_ON_FULL.get();
        harvestMoonGrowAmount = HARVEST_MOON_GROW_AMOUNT.get();
        harvestMoonGrowInterval = HARVEST_MOON_GROW_INTERVAL.get();

        starShowers = STAR_SHOWERS.bake();
        fallingStars = FALLING_STARS.get();
        fallingStarRarity = FALLING_STAR_RARITY.get();

        bloodMoon = BLOOD_MOON.bake();
        bloodMoonSleeping = BLOOD_MOON_SLEEPING.get();
        bloodMoonSpawnMultiplier = BLOOD_MOON_SPAWN_MULTIPLIER.get();
        bloodMoonVanish = BLOOD_MOON_VANISH.get();
        bloodMoonSpawnRadius = BLOOD_MOON_SPAWN_RADIUS.get();
        bloodMoonOnFull = BLOOD_MOON_ON_FULL.get();

        meteors = METEORS.get();
        meteorChance = METEOR_CHANCE.get();
        meteorChanceNight = METEOR_CHANCE_NIGHT.get();
        meteorGateDimension = METEOR_GATE_DIMENSION.get();
        meteorChanceAfterGate = METEOR_CHANCE_AFTER_GATE.get();
        meteorChanceAfterGateNight = METEOR_CHANCE_AFTER_GATE_NIGHT.get();
        meteorChanceStarShower = METEOR_CHANCE_STAR_SHOWER.get();
        meteorChanceEnd = METEOR_CHANCE_END.get();
        meteorSpawnRadius = METEOR_SPAWN_RADIUS.get();
        meteorDisallowRadius = METEOR_DISALLOW_RADIUS.get();
        meteorDisallowTime = METEOR_DISALLOW_TIME.get();
        crystalDurability = CRYSTAL_DURABILITY.get();
        hammerDamage = HAMMER_DAMAGE.get();
    }

    public static boolean isLoaded() {
        return allowedDimensions != null;
    }

    /** @return the configured meteor spawn chance for the given level/state, mirroring the original logic. */
    public static double getMeteorChance(Level level, NyxWorld data) {
        if (dimensionMatches(level, "minecraft:the_end"))
            return meteorChanceEnd;
        if (!isDimensionAllowed(level))
            return 0;
        boolean visitedGate = data.visitedDimensions.contains(meteorGateDimension)
                || data.visitedDimensions.contains(stripNamespace(meteorGateDimension));
        if (!NyxWorld.isDaytime(level)) {
            if (data.currentEvent instanceof StarShower)
                return meteorChanceStarShower;
            return visitedGate ? meteorChanceAfterGateNight : meteorChanceNight;
        }
        return visitedGate ? meteorChanceAfterGate : meteorChance;
    }

    public static boolean isDimensionAllowed(Level level) {
        return allowedDimensions.stream().anyMatch(d -> dimensionMatches(level, d));
    }

    public static boolean isEnchantingWhitelisted(Level level) {
        return enchantingWhitelistDimensions.stream().anyMatch(d -> dimensionMatches(level, d));
    }

    /** Accepts either a full resource location ({@code minecraft:overworld}) or the bare path ({@code overworld}). */
    public static boolean dimensionMatches(Level level, String entry) {
        net.minecraft.resources.ResourceLocation id = level.dimension().location();
        return id.toString().equals(entry) || id.getPath().equals(entry) || id.getPath().equals(stripNamespace(entry));
    }

    public static String dimensionName(Level level) {
        return level.dimension().location().toString();
    }

    private static String stripNamespace(String s) {
        int i = s.indexOf(':');
        return i < 0 ? s : s.substring(i + 1);
    }

    public static class LunarEventConfig {
        public boolean enabled;
        public double chance;
        public int startNight;
        public int nightInterval;
        public int graceDays;
    }

    private static final class LunarEventSpec {
        final ForgeConfigSpec.BooleanValue enabled;
        final ForgeConfigSpec.DoubleValue chance;
        final ForgeConfigSpec.IntValue startNight;
        final ForgeConfigSpec.IntValue nightInterval;
        final ForgeConfigSpec.IntValue graceDays;

        LunarEventSpec(ForgeConfigSpec.Builder b, String name, String display, double defaultChance) {
            enabled = b.comment("If the " + display + " should be enabled").define(name, true);
            chance = b.comment("Chance (1 = 100%) of the " + display + " occurring").defineInRange(name + "Chance", defaultChance, 0, 1);
            startNight = b.comment("Nights that should pass before the " + display + " occurs for the first time").defineInRange(name + "StartNight", 0, 0, 1000);
            nightInterval = b.comment("Interval in days at which the " + display + " should occur. Overrides chance if > 0.").defineInRange(name + "Interval", 0, 0, 1000);
            graceDays = b.comment("Days that should pass until the " + display + " happens again").defineInRange(name + "GracePeriod", 0, 0, 1000);
        }

        LunarEventConfig bake() {
            LunarEventConfig c = new LunarEventConfig();
            c.enabled = enabled.get();
            c.chance = chance.get();
            c.startNight = startNight.get();
            c.nightInterval = nightInterval.get();
            c.graceDays = graceDays.get();
            return c;
        }
    }

    private Config() {
    }
}
