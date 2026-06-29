package de.ellpeck.nyx.events;

import de.ellpeck.nyx.Nyx;
import de.ellpeck.nyx.Registration;
import de.ellpeck.nyx.capabilities.NyxWorld;
import de.ellpeck.nyx.entities.MeteorRenderer;
import de.ellpeck.nyx.entities.NoopRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.stream.Stream;

/**
 * Client-only, mod-bus registration: entity renderers, item colours and item
 * model properties (the meteor finder's needle and the bow's draw animation).
 */
@Mod.EventBusSubscriber(modid = Nyx.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientRegistration {

    private ClientRegistration() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Registration.FALLING_METEOR.get(), MeteorRenderer::new);
        event.registerEntityRenderer(Registration.FALLING_STAR.get(), NoopRenderer::new);
        event.registerEntityRenderer(Registration.CAULDRON_TRACKER.get(), NoopRenderer::new);
    }

    @SubscribeEvent
    public static void onItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> 0xd44a13, Registration.METEOR_FINDER.get());
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(Registration.METEOR_FINDER.get(), new ResourceLocation("angle"), new MeteorAngle());

            // bow draw animation with the longer 30-tick draw
            ItemProperties.register(Registration.METEOR_BOW.get(), new ResourceLocation("pull"),
                    (stack, level, entity, seed) -> {
                        if (entity == null)
                            return 0;
                        return entity.getUseItem() != stack ? 0
                                : (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / 30.0F;
                    });
            ItemProperties.register(Registration.METEOR_BOW.get(), new ResourceLocation("pulling"),
                    (stack, level, entity, seed) ->
                            entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1 : 0);
        });
    }

    /** Compass-like needle that points to the nearest known meteor landing site. */
    private static final class MeteorAngle implements ClampedItemPropertyFunction {

        private double rotation;
        private double rota;
        private long lastUpdateTick;
        private BlockPos meteorPos;

        @Override
        public float unclampedCall(net.minecraft.world.item.ItemStack stack, ClientLevel level, net.minecraft.world.entity.LivingEntity entity, int seed) {
            if (entity == null)
                return 0;
            if (level == null && entity.level() instanceof ClientLevel cl)
                level = cl;
            if (level == null)
                return 0;

            double yaw = Mth.positiveModulo(entity.getYRot() / 360.0D, 1.0D);
            double angle = getMeteorAngle(level, entity) / (Math.PI * 2D);
            double rot;
            if (Double.isNaN(angle)) {
                rot = Math.random();
            } else {
                rot = 0.5D - (yaw - 0.25D - angle);
            }
            rot = this.wobble(level, rot);
            return Mth.positiveModulo((float) rot, 1.0F);
        }

        private double wobble(ClientLevel level, double target) {
            if (level.getGameTime() != this.lastUpdateTick) {
                this.lastUpdateTick = level.getGameTime();
                double diff = Mth.positiveModulo(target - this.rotation + 0.5D, 1.0D) - 0.5D;
                this.rota += diff * 0.1D;
                this.rota *= 0.8D;
                this.rotation = Mth.positiveModulo(this.rotation + this.rota, 1.0D);
            }
            return this.rotation;
        }

        private double getMeteorAngle(ClientLevel level, Entity e) {
            if (this.meteorPos == null || level.getGameTime() % 100 == 0) {
                NyxWorld data = NyxWorld.get(level);
                if (data == null || (data.meteorLandingSites.isEmpty() && data.cachedMeteorPositions.isEmpty()))
                    return Double.NaN;
                this.meteorPos = Stream.concat(data.meteorLandingSites.stream(), data.cachedMeteorPositions.stream())
                        .min((p1, p2) -> Double.compare(e.distanceToSqr(p1.getX(), p1.getY(), p1.getZ()), e.distanceToSqr(p2.getX(), p2.getY(), p2.getZ())))
                        .orElse(null);
            }
            if (this.meteorPos == null)
                return Double.NaN;
            return Math.atan2(this.meteorPos.getZ() - e.getZ(), this.meteorPos.getX() - e.getX());
        }
    }
}
