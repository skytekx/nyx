package de.ellpeck.nyx.items;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;

/**
 * The meteorite-reinforced bow: longer draw (30 ticks instead of 20), higher
 * velocity and ~1.75x arrow damage. Ported from the 1.12 {@code ItemBow} subclass.
 */
public class Bow extends BowItem {

    public Bow() {
        super(new Item.Properties().durability(2250));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entityLiving, int timeLeft) {
        if (!(entityLiving instanceof Player player))
            return;
        boolean infinite = player.getAbilities().instabuild || EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, stack) > 0;
        ItemStack ammo = player.getProjectile(stack);

        int charge = this.getUseDuration(stack) - timeLeft;
        charge = ForgeEventFactory.onArrowLoose(stack, level, player, charge, !ammo.isEmpty() || infinite);
        if (charge < 0)
            return;

        if (!ammo.isEmpty() || infinite) {
            if (ammo.isEmpty())
                ammo = new ItemStack(Items.ARROW);

            // edit: longer draw time (30 instead of 20)
            float power = getVelocity(charge);
            if (power >= 0.1D) {
                boolean creativeArrow = player.getAbilities().instabuild
                        || (ammo.getItem() instanceof ArrowItem arrow && arrow.isInfinite(ammo, stack, player));

                if (!level.isClientSide) {
                    ArrowItem arrowitem = (ArrowItem) (ammo.getItem() instanceof ArrowItem ? ammo.getItem() : Items.ARROW);
                    AbstractArrow arrow = arrowitem.createArrow(level, ammo, player);
                    arrow = this.customArrow(arrow);
                    // edit: velocity multiplied by 3 then again by 1.3
                    arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, power * 3.0F * 1.3F, 1.0F);
                    if (power == 1.0F)
                        arrow.setCritArrow(true);

                    int powerEnch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, stack);
                    if (powerEnch > 0)
                        arrow.setBaseDamage(arrow.getBaseDamage() + powerEnch * 0.5D + 0.5D);
                    int punch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, stack);
                    if (punch > 0)
                        arrow.setKnockback(punch);
                    if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, stack) > 0)
                        arrow.setSecondsOnFire(100);

                    stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));

                    if (creativeArrow || player.getAbilities().instabuild && (ammo.is(Items.SPECTRAL_ARROW) || ammo.is(Items.TIPPED_ARROW)))
                        arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;

                    // edit: increase arrow damage
                    arrow.setBaseDamage(arrow.getBaseDamage() * 1.75F);
                    level.addFreshEntity(arrow);
                }

                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                        1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);

                if (!creativeArrow && !player.getAbilities().instabuild) {
                    ammo.shrink(1);
                    if (ammo.isEmpty())
                        player.getInventory().removeItem(ammo);
                }
                player.awardStat(Stats.ITEM_USED.get(this));
            }
        }
    }

    /** Velocity for a given charge time, with the longer 30-tick draw. */
    public static float getVelocity(int charge) {
        float f = charge / 30.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        if (f > 1.0F)
            f = 1.0F;
        return f;
    }

    @Override
    public int getEnchantmentValue() {
        return 18;
    }
}
