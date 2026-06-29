package de.ellpeck.nyx.entities;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Renderer that draws nothing, used for the otherwise-invisible falling star and
 * cauldron-tracker entities (their visuals are particles). Ported from
 * {@code EmptyRenderer}.
 */
public class NoopRenderer<T extends Entity> extends EntityRenderer<T> {

    public NoopRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
