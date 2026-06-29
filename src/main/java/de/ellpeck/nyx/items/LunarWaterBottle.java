package de.ellpeck.nyx.items;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;

/**
 * Drinkable lunar water: removes harmful effects and grants regeneration.
 *
 * <p><b>Port note:</b> the 1.12 bottle also exposed a {@code FluidBucketWrapper}
 * (1/4-bucket) fluid capability so it could interact with fluid machinery. That
 * fluid-handler-on-a-bottle has not been re-added; the bottle is simply a
 * drinkable item now. Lunar water still exists as a full fluid + bucket, which
 * is what the cauldron interaction uses.
 */
public class LunarWaterBottle extends Item {

    public LunarWaterBottle() {
        super(new Item.Properties().stacksTo(1));
    }

    public static boolean applyLunarWater(LivingEntity entity) {
        boolean did = false;

        Set<net.minecraft.world.effect.MobEffect> effectsToRemove = new HashSet<>();
        for (MobEffectInstance effect : entity.getActiveEffects()) {
            if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                effectsToRemove.add(effect.getEffect());
                did = true;
            }
        }
        effectsToRemove.forEach(entity::removeEffect);

        if (entity.getEffect(MobEffects.REGENERATION) == null) {
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 5 * 20, 1));
            did = true;
        }
        return did;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entityLiving) {
        if (!level.isClientSide)
            applyLunarWater(entityLiving);
        Player player = entityLiving instanceof Player p ? p : null;
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
            if (stack.isEmpty())
                return new ItemStack(Items.GLASS_BOTTLE);
            if (player != null)
                player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
        }
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }
}
