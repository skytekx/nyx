package de.ellpeck.nyx.items;

import net.minecraft.world.item.Item;

/**
 * Compass-like "meteor location device". The needle ("angle" item property) and
 * tint are registered client-side in
 * {@code de.ellpeck.nyx.events.ClientRegistration}.
 */
public class MeteorFinder extends Item {

    public MeteorFinder() {
        super(new Item.Properties());
    }
}
