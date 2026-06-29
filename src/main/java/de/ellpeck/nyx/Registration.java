package de.ellpeck.nyx;

import de.ellpeck.nyx.blocks.Crystal;
import de.ellpeck.nyx.blocks.Glass;
import de.ellpeck.nyx.blocks.LunarWaterCauldron;
import de.ellpeck.nyx.blocks.LunarWaterFluid;
import de.ellpeck.nyx.blocks.MeteorRock;
import de.ellpeck.nyx.blocks.StarAir;
import de.ellpeck.nyx.enchantments.LunarEdge;
import de.ellpeck.nyx.enchantments.LunarShield;
import de.ellpeck.nyx.entities.CauldronTracker;
import de.ellpeck.nyx.entities.FallingMeteor;
import de.ellpeck.nyx.entities.FallingStar;
import de.ellpeck.nyx.items.Axe;
import de.ellpeck.nyx.items.Bow;
import de.ellpeck.nyx.items.FallenStar;
import de.ellpeck.nyx.items.Hammer;
import de.ellpeck.nyx.items.LunarWaterBottle;
import de.ellpeck.nyx.items.MeteorFinder;
import de.ellpeck.nyx.items.Pickaxe;
import de.ellpeck.nyx.items.Scythe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.function.Supplier;

/**
 * Central registration for the 1.20.1 port: every block, item, entity, block
 * entity, enchantment, sound, fluid and the creative tab live here as
 * {@link RegistryObject}s on {@link DeferredRegister}s, replacing the old
 * {@code GameRegistry}/{@code @ObjectHolder} approach.
 */
public final class Registration {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Nyx.ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Nyx.ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Nyx.ID);
    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, Nyx.ID);
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, Nyx.ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, Nyx.ID);
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Nyx.ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Nyx.ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Nyx.ID);

    // region materials -------------------------------------------------------

    // Original: EnumHelper.addToolMaterial("NYX:METEOR", 3, (int)(1561*1.5F), 7, 7, 18)
    // -> harvest level 3 (diamond), uses 2341, speed 7, attack bonus 7, enchantability 18.
    public static final Tier METEOR_TIER = TierSortingRegistry.registerTier(
            new ForgeTier(3, 2341, 7.0F, 7.0F, 18, BlockTags.NEEDS_DIAMOND_TOOL,
                    () -> Ingredient.of(Registration.METEOR_INGOT.get())),
            new ResourceLocation(Nyx.ID, "meteor"),
            List.<Object>of(net.minecraft.world.item.Tiers.DIAMOND),
            List.<Object>of(net.minecraft.world.item.Tiers.NETHERITE));

    // Original: EnumHelper.addArmorMaterial("NYX:METEOR", "nyx:meteor", (int)(33*1.5F)=49, {4,8,10,4}, 18, ..., 3 toughness).
    // 1.12 damage-reduction array order is {boots, legs, chest, helmet} (matching vanilla diamond {3,6,8,3}).
    private static final java.util.Map<ArmorItem.Type, Integer> ARMOR_DEFENSE = java.util.Map.of(
            ArmorItem.Type.BOOTS, 4, ArmorItem.Type.LEGGINGS, 8, ArmorItem.Type.CHESTPLATE, 10, ArmorItem.Type.HELMET, 4);
    private static final java.util.Map<ArmorItem.Type, Integer> ARMOR_DURABILITY = java.util.Map.of(
            ArmorItem.Type.BOOTS, 13, ArmorItem.Type.LEGGINGS, 15, ArmorItem.Type.CHESTPLATE, 16, ArmorItem.Type.HELMET, 11);

    public static final ArmorMaterial METEOR_ARMOR = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return ARMOR_DURABILITY.get(type) * 49; // base factor 49 = round(33 * 1.5)
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return ARMOR_DEFENSE.get(type);
        }

        @Override
        public int getEnchantmentValue() {
            return 18;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_GENERIC;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(Registration.METEOR_INGOT.get());
        }

        @Override
        public String getName() {
            return Nyx.ID + ":meteor";
        }

        @Override
        public float getToughness() {
            return 3.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.0F;
        }
    };

    // endregion

    // region fluids ----------------------------------------------------------

    public static final RegistryObject<FluidType> LUNAR_WATER_TYPE = FLUID_TYPES.register("lunar_water",
            () -> new LunarWaterFluid.Type());
    public static final RegistryObject<ForgeFlowingFluid> LUNAR_WATER_FLUID = FLUIDS.register("lunar_water",
            () -> new LunarWaterFluid.Source(lunarWaterProps()));
    public static final RegistryObject<ForgeFlowingFluid> LUNAR_WATER_FLOWING = FLUIDS.register("lunar_water_flowing",
            () -> new LunarWaterFluid.Flowing(lunarWaterProps()));

    private static ForgeFlowingFluid.Properties lunarWaterProps() {
        return new ForgeFlowingFluid.Properties(LUNAR_WATER_TYPE, LUNAR_WATER_FLUID, LUNAR_WATER_FLOWING)
                .block(LUNAR_WATER_BLOCK)
                .bucket(LUNAR_WATER_BUCKET)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(1)
                .explosionResistance(100F);
    }

    // endregion

    // region blocks ----------------------------------------------------------

    public static final RegistryObject<LiquidBlock> LUNAR_WATER_BLOCK = BLOCKS.register("lunar_water",
            () -> new LiquidBlock(LUNAR_WATER_FLUID, BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.WATER).lightLevel(s -> 15)));
    public static final RegistryObject<Block> LUNAR_WATER_CAULDRON = BLOCKS.register("lunar_water_cauldron",
            () -> new LunarWaterCauldron(BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.CAULDRON).lightLevel(s -> 13)));

    public static final RegistryObject<Block> STAR_AIR = BLOCKS.register("star_air",
            () -> new StarAir(BlockBehaviour.Properties.of().noCollission().noOcclusion().instabreak()
                    .replaceable().lightLevel(s -> 15).noLootTable().pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> STAR_BLOCK = BLOCKS.register("star_block",
            () -> new Block(stone(MapColor.COLOR_BLACK).strength(2).lightLevel(s -> 6)));
    public static final RegistryObject<Block> CRACKED_STAR_BLOCK = BLOCKS.register("cracked_star_block",
            () -> new Block(stone(MapColor.COLOR_BLACK).strength(2)));
    public static final RegistryObject<Block> CHISELED_STAR_BLOCK = BLOCKS.register("chiseled_star_block",
            () -> new Block(stone(MapColor.COLOR_BLACK).strength(2).lightLevel(s -> 6)));
    public static final RegistryObject<Block> STAR_STAIRS = BLOCKS.register("star_stairs",
            () -> new StairBlock(() -> STAR_BLOCK.get().defaultBlockState(), stone(MapColor.COLOR_BLACK).strength(2).lightLevel(s -> 6)));
    public static final RegistryObject<Block> STAR_SLAB = BLOCKS.register("star_slab",
            () -> new SlabBlock(stone(MapColor.COLOR_BLACK).strength(2).lightLevel(s -> 6)));

    public static final RegistryObject<Block> METEOR_ROCK = BLOCKS.register("meteor_rock", MeteorRock::new);
    public static final RegistryObject<Block> GLEANING_METEOR_ROCK = BLOCKS.register("gleaning_meteor_rock", MeteorRock::new);
    public static final RegistryObject<Block> CRYSTAL = BLOCKS.register("crystal", Crystal::new);
    public static final RegistryObject<Block> METEOR_BLOCK = BLOCKS.register("meteor_block",
            () -> new Block(stone(MapColor.COLOR_GRAY).strength(3)));
    public static final RegistryObject<Block> METEOR_GLASS = BLOCKS.register("meteor_glass",
            () -> new Glass(BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.GLASS).strength(5, 3000)));

    // Plain 1.12 ROCK blocks were hand-mineable (only meteor rock needed a diamond
    // pickaxe), so we don't require the correct tool here.
    private static BlockBehaviour.Properties stone(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).sound(SoundType.STONE);
    }

    // endregion

    // region block entities --------------------------------------------------

    public static final RegistryObject<BlockEntityType<Crystal.Tile>> CRYSTAL_BE = BLOCK_ENTITIES.register("crystal",
            () -> BlockEntityType.Builder.of(Crystal.Tile::new, CRYSTAL.get()).build(null));
    public static final RegistryObject<BlockEntityType<StarAir.Tile>> STAR_AIR_BE = BLOCK_ENTITIES.register("star_air",
            () -> BlockEntityType.Builder.of(StarAir.Tile::new, STAR_AIR.get()).build(null));

    // endregion

    // region items -----------------------------------------------------------

    private static RegistryObject<Item> blockItem(RegistryObject<Block> block) {
        return ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
    }

    // block items (the lunar water cauldron has no item, matching the original -
    // it is created in-world by dyeing an exposed cauldron of water)
    public static final RegistryObject<Item> STAR_BLOCK_ITEM = blockItem(STAR_BLOCK);
    public static final RegistryObject<Item> CRACKED_STAR_BLOCK_ITEM = blockItem(CRACKED_STAR_BLOCK);
    public static final RegistryObject<Item> CHISELED_STAR_BLOCK_ITEM = blockItem(CHISELED_STAR_BLOCK);
    public static final RegistryObject<Item> STAR_STAIRS_ITEM = blockItem(STAR_STAIRS);
    public static final RegistryObject<Item> STAR_SLAB_ITEM = blockItem(STAR_SLAB);
    public static final RegistryObject<Item> METEOR_ROCK_ITEM = blockItem(METEOR_ROCK);
    public static final RegistryObject<Item> GLEANING_METEOR_ROCK_ITEM = blockItem(GLEANING_METEOR_ROCK);
    public static final RegistryObject<Item> CRYSTAL_ITEM = blockItem(CRYSTAL);
    public static final RegistryObject<Item> METEOR_BLOCK_ITEM = blockItem(METEOR_BLOCK);
    public static final RegistryObject<Item> METEOR_GLASS_ITEM = blockItem(METEOR_GLASS);

    // plain & special items
    public static final RegistryObject<Item> LUNAR_WATER_BUCKET = ITEMS.register("lunar_water_bucket",
            () -> new BucketItem(LUNAR_WATER_FLUID, new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1)));
    public static final RegistryObject<Item> LUNAR_WATER_BOTTLE = ITEMS.register("lunar_water_bottle", LunarWaterBottle::new);
    public static final RegistryObject<Item> METEOR_SHARD = ITEMS.register("meteor_shard", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FALLEN_STAR = ITEMS.register("fallen_star", FallenStar::new);
    public static final RegistryObject<Item> METEOR_DUST = ITEMS.register("meteor_dust", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> METEOR_FINDER = ITEMS.register("meteor_finder", MeteorFinder::new);
    public static final RegistryObject<Item> UNREFINED_CRYSTAL = ITEMS.register("unrefined_crystal", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SCYTHE = ITEMS.register("scythe", Scythe::new);
    public static final RegistryObject<Item> METEOR_INGOT = ITEMS.register("meteor_ingot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> METEOR_BOW = ITEMS.register("meteor_bow", Bow::new);
    public static final RegistryObject<Item> METEOR_PICKAXE = ITEMS.register("meteor_pickaxe", () -> new Pickaxe(METEOR_TIER, 1, -2.8F));
    public static final RegistryObject<Item> METEOR_AXE = ITEMS.register("meteor_axe", () -> new Axe(METEOR_TIER, 5.0F, -3.0F));
    public static final RegistryObject<Item> METEOR_SHOVEL = ITEMS.register("meteor_shovel", () -> new ShovelItem(METEOR_TIER, 1.5F, -3.0F, new Item.Properties()));
    public static final RegistryObject<Item> METEOR_HOE = ITEMS.register("meteor_hoe", () -> new HoeItem(METEOR_TIER, -3, 0.0F, new Item.Properties()));
    public static final RegistryObject<Item> METEOR_SWORD = ITEMS.register("meteor_sword", () -> new SwordItem(METEOR_TIER, 3, -2.4F, new Item.Properties()));
    public static final RegistryObject<Item> METEOR_HELM = ITEMS.register("meteor_helm", () -> new ArmorItem(METEOR_ARMOR, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> METEOR_CHEST = ITEMS.register("meteor_chest", () -> new ArmorItem(METEOR_ARMOR, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> METEOR_PANTS = ITEMS.register("meteor_pants", () -> new ArmorItem(METEOR_ARMOR, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> METEOR_BOOTS = ITEMS.register("meteor_boots", () -> new ArmorItem(METEOR_ARMOR, ArmorItem.Type.BOOTS, new Item.Properties()));
    public static final RegistryObject<Item> METEOR_HAMMER = ITEMS.register("meteor_hammer", () -> new Hammer(METEOR_TIER));

    // endregion

    // region enchantments ----------------------------------------------------

    public static final RegistryObject<Enchantment> LUNAR_EDGE = ENCHANTMENTS.register("lunar_edge", LunarEdge::new);
    public static final RegistryObject<Enchantment> LUNAR_SHIELD = ENCHANTMENTS.register("lunar_shield", LunarShield::new);

    // endregion

    // region sounds ----------------------------------------------------------

    public static final RegistryObject<SoundEvent> LUNAR_WATER_SOUND = sound("lunar_water");
    public static final RegistryObject<SoundEvent> FALLING_STAR_SOUND = sound("falling_star");
    public static final RegistryObject<SoundEvent> FALLING_STAR_IMPACT_SOUND = sound("falling_star_impact");
    public static final RegistryObject<SoundEvent> FALLING_METEOR_SOUND = sound("falling_meteor");
    public static final RegistryObject<SoundEvent> FALLING_METEOR_IMPACT_SOUND = sound("falling_meteor_impact");
    public static final RegistryObject<SoundEvent> HAMMER_START_SOUND = sound("hammer_start");
    public static final RegistryObject<SoundEvent> HAMMER_END_SOUND = sound("hammer_end");

    private static RegistryObject<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Nyx.ID, name)));
    }

    // endregion

    // region entities --------------------------------------------------------

    public static final RegistryObject<net.minecraft.world.entity.EntityType<FallingStar>> FALLING_STAR = ENTITIES.register("falling_star",
            () -> net.minecraft.world.entity.EntityType.Builder.<FallingStar>of(FallingStar::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F).clientTrackingRange(8).updateInterval(1).fireImmune()
                    .build(Nyx.ID + ":falling_star"));
    public static final RegistryObject<net.minecraft.world.entity.EntityType<FallingMeteor>> FALLING_METEOR = ENTITIES.register("falling_meteor",
            () -> net.minecraft.world.entity.EntityType.Builder.<FallingMeteor>of(FallingMeteor::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F).clientTrackingRange(16).updateInterval(1).fireImmune()
                    .build(Nyx.ID + ":falling_meteor"));
    public static final RegistryObject<net.minecraft.world.entity.EntityType<CauldronTracker>> CAULDRON_TRACKER = ENTITIES.register("cauldron_tracker",
            () -> net.minecraft.world.entity.EntityType.Builder.<CauldronTracker>of(CauldronTracker::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F).clientTrackingRange(4).updateInterval(20)
                    .build(Nyx.ID + ":cauldron_tracker"));

    // endregion

    // region creative tab ----------------------------------------------------

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("nyx", () -> CreativeModeTab.builder()
            .title(net.minecraft.network.chat.Component.translatable("itemGroup.nyx"))
            .icon(() -> new net.minecraft.world.item.ItemStack(FALLEN_STAR.get()))
            .displayItems((params, output) -> ITEMS.getEntries().stream()
                    .map(RegistryObject::get)
                    .forEach(output::accept))
            .build());

    // endregion

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        SOUNDS.register(bus);
        ENCHANTMENTS.register(bus);
        FLUID_TYPES.register(bus);
        FLUIDS.register(bus);
        ENTITIES.register(bus);
        BLOCK_ENTITIES.register(bus);
        TABS.register(bus);
    }

    private Registration() {
    }
}
