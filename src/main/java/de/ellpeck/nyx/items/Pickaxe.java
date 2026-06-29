package de.ellpeck.nyx.items;

import de.ellpeck.nyx.Registration;
import de.ellpeck.nyx.blocks.MeteorRock;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class Pickaxe extends PickaxeItem {

    public Pickaxe(Tier material, int attackDamage, float attackSpeed) {
        super(material, attackDamage, attackSpeed, new Item.Properties());
    }

    public Pickaxe(Tier material, int attackDamage, float attackSpeed, Properties properties) {
        super(material, attackDamage, attackSpeed, properties);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float speed = super.getDestroySpeed(stack, state);
        // meteor pickaxe mines obsidian and meteors twice as fast
        if (this == Registration.METEOR_PICKAXE.get() && (state.is(Blocks.OBSIDIAN) || state.getBlock() instanceof MeteorRock))
            speed *= 2;
        return speed;
    }
}
