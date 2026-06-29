package de.ellpeck.nyx.mixin;

import de.ellpeck.nyx.Nyx;
import de.ellpeck.nyx.capabilities.NyxWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Swaps the vanilla moon-phases texture for a tinted one (blood / harvest moon)
 * while the corresponding lunar event is active. Replaces the 1.12 reflection
 * hack that mutated {@code RenderGlobal}'s moon {@code ResourceLocation}.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @ModifyArg(
            method = "renderSky",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V"),
            index = 1,
            require = 0, expect = 0) // cosmetic: degrade gracefully if another rendering mod changes renderSky
    private ResourceLocation nyx$swapMoonTexture(ResourceLocation texture) {
        if (!"textures/environment/moon_phases.png".equals(texture.getPath()))
            return texture;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return texture;
        NyxWorld nyx = NyxWorld.get(mc.level);
        if (nyx == null || nyx.currentEvent == null)
            return texture;
        String moonTex = nyx.currentEvent.getMoonTexture();
        if (moonTex == null)
            return texture;
        return new ResourceLocation(Nyx.ID, "textures/moon/" + moonTex + ".png");
    }
}
