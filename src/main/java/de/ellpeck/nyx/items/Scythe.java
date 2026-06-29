package de.ellpeck.nyx.items;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import de.ellpeck.nyx.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.IPlantable;

import java.util.List;

/**
 * The gleaning scythe: sweeps a 9x9x9 area, harvesting plants and leaves with a
 * chance for bonus drops. Repaired with gleaning crystals. Ported from the 1.12
 * {@code onBlockStartBreak} mass-harvest item.
 */
public class Scythe extends Item {

    private final Multimap<Attribute, AttributeModifier> attributes;

    public Scythe() {
        super(new Item.Properties().durability(3450));
        HashMultimap<Attribute, AttributeModifier> map = HashMultimap.create();
        map.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 10, AttributeModifier.Operation.ADDITION));
        map.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -2.8, AttributeModifier.Operation.ADDITION));
        this.attributes = map;
    }

    @Override
    public boolean onBlockStartBreak(ItemStack itemstack, BlockPos pos, Player player) {
        Level level = player.level();
        ItemStack stack = player.getMainHandItem();

        BlockState state = level.getBlockState(pos);
        if (!isHarvestable(state, level, pos))
            return false;

        if (level.isClientSide)
            return true;

        int range = 4;
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos offset = pos.offset(x, y, z);
                    BlockState offState = level.getBlockState(offset);
                    if (!isHarvestable(offState, level, offset))
                        continue;

                    if (level instanceof ServerLevel server) {
                        List<ItemStack> drops = Block.getDrops(offState, server, offset, server.getBlockEntity(offset), player, stack);
                        for (ItemStack drop : drops) {
                            // chance to increase the drop amount, like the original scythe
                            if (level.random.nextFloat() <= 0.6F) {
                                multCount(drop, 2);
                            } else if (level.random.nextFloat() <= 0.4F) {
                                multCount(drop, 3);
                            } else if (level.random.nextFloat() <= 0.2F) {
                                multCount(drop, 4);
                            }
                            Block.popResource(level, offset, drop);
                        }
                    }

                    level.destroyBlock(offset, false);
                    stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(net.minecraft.world.InteractionHand.MAIN_HAND));
                }
            }
        }
        return true;
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.getItem() == Registration.CRYSTAL_ITEM.get();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        return slot == EquipmentSlot.MAINHAND ? this.attributes : super.getAttributeModifiers(slot, stack);
    }

    private static boolean isHarvestable(BlockState state, Level level, BlockPos pos) {
        return state.getBlock() instanceof IPlantable || state.is(BlockTags.LEAVES);
    }

    private static void multCount(ItemStack stack, int mult) {
        stack.setCount(Math.min(stack.getMaxStackSize(), stack.getCount() * mult));
    }
}
