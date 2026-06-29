package de.ellpeck.nyx.items;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;

public class Axe extends AxeItem {

    public Axe(Tier material, float damage, float speed) {
        super(material, damage, speed, new Item.Properties());
    }
}
