package de.ellpeck.nyx.blocks;

import de.ellpeck.nyx.Nyx;
import de.ellpeck.nyx.Registration;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;

import java.util.function.Consumer;

/**
 * Lunar water as a 1.20.1 {@link ForgeFlowingFluid} (source + flowing) backed by
 * a {@link FluidType}. Replaces the old single {@code Fluid}/{@code BlockFluidClassic}.
 */
public final class LunarWaterFluid {

    private LunarWaterFluid() {
    }

    public static class Type extends FluidType {

        private static final ResourceLocation STILL = new ResourceLocation(Nyx.ID, "block/lunar_water_still");
        private static final ResourceLocation FLOW = new ResourceLocation(Nyx.ID, "block/lunar_water_flowing");

        public Type() {
            super(FluidType.Properties.create()
                    .descriptionId("fluid." + Nyx.ID + ".lunar_water")
                    .canConvertToSource(false)
                    .lightLevel(15)
                    .density(1000)
                    .viscosity(1000));
        }

        @Override
        public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
            consumer.accept(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return STILL;
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return FLOW;
                }
            });
        }
    }

    public static class Source extends ForgeFlowingFluid.Source {
        public Source(Properties properties) {
            super(properties);
        }
    }

    public static class Flowing extends ForgeFlowingFluid.Flowing {
        public Flowing(Properties properties) {
            super(properties);
        }
    }
}
