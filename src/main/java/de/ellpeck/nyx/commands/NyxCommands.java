package de.ellpeck.nyx.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import de.ellpeck.nyx.Config;
import de.ellpeck.nyx.Nyx;
import de.ellpeck.nyx.capabilities.NyxWorld;
import de.ellpeck.nyx.entities.FallingMeteor;
import de.ellpeck.nyx.lunarevents.LunarEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * The {@code /nyxforce} and {@code /nyxmeteor} commands, ported from the 1.12
 * {@code CommandBase} pair to Brigadier.
 */
@Mod.EventBusSubscriber(modid = Nyx.ID)
public final class NyxCommands {

    private NyxCommands() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("nyxforce")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("event", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            NyxWorld world = NyxWorld.get(ctx.getSource().getLevel());
                            Stream<String> names = world == null ? Stream.of("clear")
                                    : Stream.concat(world.lunarEvents.stream().map(e -> e.name), Stream.of("clear"));
                            return SharedSuggestionProvider.suggest(names, builder);
                        })
                        .executes(NyxCommands::force)));

        dispatcher.register(Commands.literal("nyxmeteor")
                .requires(s -> s.hasPermission(2))
                .executes(ctx -> meteor(ctx, null, null, null, false))
                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                .executes(ctx -> meteor(ctx, DoubleArgumentType.getDouble(ctx, "x"), DoubleArgumentType.getDouble(ctx, "z"), null, false))
                                .then(Commands.argument("size", IntegerArgumentType.integer(1))
                                        .executes(ctx -> meteor(ctx, DoubleArgumentType.getDouble(ctx, "x"), DoubleArgumentType.getDouble(ctx, "z"), IntegerArgumentType.getInteger(ctx, "size"), false))
                                        .then(Commands.argument("homing", BoolArgumentType.bool())
                                                .executes(ctx -> meteor(ctx, DoubleArgumentType.getDouble(ctx, "x"), DoubleArgumentType.getDouble(ctx, "z"), IntegerArgumentType.getInteger(ctx, "size"), BoolArgumentType.getBool(ctx, "homing"))))))));
    }

    private static int force(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String arg = StringArgumentType.getString(ctx, "event");
        NyxWorld world = NyxWorld.get(source.getLevel());
        if (world == null)
            return 0;
        if ("clear".equals(arg)) {
            world.forcedEvent = null;
            source.sendSuccess(() -> Component.translatable("command.nyx.force.clear"), true);
            return 1;
        }
        Optional<LunarEvent> ev = world.lunarEvents.stream().filter(e -> e.name.equals(arg)).findFirst();
        if (ev.isEmpty()) {
            source.sendFailure(Component.translatable("command.nyx.force.invalid", arg));
            return 0;
        }
        world.forcedEvent = ev.get();
        source.sendSuccess(() -> Component.translatable("command.nyx.force.success", arg), true);
        return 1;
    }

    private static int meteor(CommandContext<CommandSourceStack> ctx, Double x, Double z, Integer size, boolean homing) {
        CommandSourceStack source = ctx.getSource();
        if (!Config.meteors) {
            source.sendFailure(Component.literal("Meteor content is disabled in the config"));
            return 0;
        }
        Vec3 at = source.getPosition();
        double px = x != null ? x : at.x;
        double pz = z != null ? z : at.z;
        BlockPos pos = new BlockPos((int) px, 0, (int) pz);
        FallingMeteor meteor = FallingMeteor.spawn(source.getLevel(), pos);
        if (size != null)
            meteor.getEntityData().set(FallingMeteor.SIZE, size);
        meteor.homing = homing;
        BlockPos result = meteor.blockPosition();
        source.sendSuccess(() -> Component.translatable("command.nyx.meteor.success", result.getX(), result.getY(), result.getZ()), true);
        return 1;
    }
}
