package de.ellpeck.nyx.events;

import de.ellpeck.nyx.Config;
import de.ellpeck.nyx.Nyx;
import de.ellpeck.nyx.capabilities.NyxWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-only, forge-bus hooks for the server-driven lunar events: the F3 debug
 * line, the per-tick client {@link NyxWorld} update (sky-fade animation) and the
 * lunar-event sky tint. The moon-texture swap lives in
 * {@link de.ellpeck.nyx.mixin.LevelRendererMixin}; the moon-phase night
 * darkening/tint lives in {@link de.ellpeck.nyx.ClientHandler}.
 */
@Mod.EventBusSubscriber(modid = Nyx.ID, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onDebug(CustomizeGuiOverlayEvent.DebugText event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return;
        NyxWorld world = NyxWorld.get(mc.level);
        if (world == null)
            return;
        String pre = ChatFormatting.GREEN + "[" + Nyx.NAME + "]" + ChatFormatting.RESET;
        String name = world.currentEvent == null ? "None" : world.currentEvent.name;
        event.getLeft().add("");
        event.getLeft().add(pre + " CurrEvent: " + name);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START)
            return;
        Level world = Minecraft.getInstance().level;
        if (world == null)
            return;
        NyxWorld nyx = NyxWorld.get(world);
        if (nyx != null)
            nyx.update();
    }

    @SubscribeEvent
    public static void onFogRender(ViewportEvent.ComputeFogColor event) {
        if (!Config.moonEventTint)
            return;
        Level level = Minecraft.getInstance().level;
        if (level == null)
            return;
        NyxWorld world = NyxWorld.get(level);
        if (world == null || world.currentSkyColor == 0)
            return;
        float mod = world.eventSkyModifier;
        event.setRed(lerp(event.getRed(), (world.currentSkyColor >> 16 & 255) / 255F, mod));
        event.setGreen(lerp(event.getGreen(), (world.currentSkyColor >> 8 & 255) / 255F, mod));
        event.setBlue(lerp(event.getBlue(), (world.currentSkyColor & 255) / 255F, mod));
    }

    private static float lerp(float a, float b, float f) {
        return a + f * (b - a);
    }
}
