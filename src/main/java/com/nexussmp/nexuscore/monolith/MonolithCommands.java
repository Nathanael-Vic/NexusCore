package com.nexussmp.nexuscore.monolith;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

/**
 * {@code /nexus monolito ...}: so operador. O {@code /locate} nao acha os monolitos, de proposito.
 *
 * <ul>
 *   <li>{@code onde} — os seis deste mundo, com a distancia; clicar sugere o {@code /tp}.</li>
 *   <li>{@code eco} — o quanto o mundo esta errado onde voce esta (de 0 a 1).</li>
 * </ul>
 */
public final class MonolithCommands {
    private static final int OPERATOR_LEVEL = 2;

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("monolito")
                .requires(source -> source.hasPermission(OPERATOR_LEVEL))
                .then(Commands.literal("onde").executes(context -> where(context.getSource())))
                .then(Commands.literal("eco").executes(context -> echo(context.getSource())));
    }

    private static int where(CommandSourceStack source) {
        ServerLevel overworld = source.getServer().overworld();
        List<MonolithSites.Site> sites = MonolithSites.of(overworld);
        if (sites.isEmpty()) {
            source.sendFailure(Component.literal("Nenhum monolito neste mundo."));
            return 0;
        }
        Vec3 from = source.getPosition();
        source.sendSuccess(() -> Component.literal("Monolitos deste mundo:").withStyle(ChatFormatting.DARK_PURPLE), false);
        for (MonolithSites.Site site : sites) {
            int x = site.center().getX();
            int z = site.center().getZ();
            String tp = "/execute in minecraft:overworld run tp @s " + x + " ~ " + z;
            MutableComponent line = Component.literal(String.format(Locale.ROOT, " %s  %d, %d  (%d blocos)",
                            site.id().getPath(), x, z, (int) site.horizontalDistance(from.x, from.z)))
                    .withStyle(style -> style.withColor(ChatFormatting.LIGHT_PURPLE)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, tp))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tp))));
            source.sendSuccess(() -> line, false);
        }
        return sites.size();
    }

    private static int echo(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Vec3 at = source.getPosition();
        float value = Echo.intensity(MonolithSites.of(level), at.x, at.z);
        String zone = value <= 0.0F ? "nada" : value <= Echo.STRANGE ? "estranho" : value <= Echo.WRONG ? "errado" : "quebrado";
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT, "Eco aqui: %.2f (%s)", value, zone))
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private MonolithCommands() {}
}
