package com.nexussmp.nexuscore.display.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nexussmp.nexuscore.display.PanelPage;
import com.nexussmp.nexuscore.display.blockentity.PanelBlockEntity;
import com.nexussmp.nexuscore.stats.ExplorationNames;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/** {@code /nexus panel ...}: pagina inicial, paginas habilitadas e dimensao exibida. */
public final class PanelCommands {
    private static final int OPERATOR_LEVEL = 2;
    private static final String ALL = "all";

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("painel")
                .requires(source -> source.hasPermission(OPERATOR_LEVEL))
                .then(Commands.literal("pagina")
                        .then(Commands.argument("posicao", BlockPosArgument.blockPos())
                                .then(Commands.argument("pagina", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(pageIds(), builder))
                                        .executes(PanelCommands::setPage))))
                .then(Commands.literal("paginas")
                        .then(Commands.argument("posicao", BlockPosArgument.blockPos())
                                .then(Commands.argument("paginas", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                Stream.concat(Stream.of(ALL), pageIds()), builder))
                                        .executes(PanelCommands::setEnabledPages))))
                .then(Commands.literal("dimensao")
                        .then(Commands.argument("posicao", BlockPosArgument.blockPos())
                                .then(Commands.argument("dimensao", DimensionArgument.dimension())
                                        .executes(PanelCommands::setDimension))));
    }

    private static Stream<String> pageIds() {
        return Arrays.stream(PanelPage.all()).map(PanelPage::id);
    }

    private static int setPage(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        PanelBlockEntity panel = panelAt(context);
        if (panel == null) {
            return 0;
        }
        PanelPage page = PanelPage.byId(StringArgumentType.getString(context, "pagina"));
        if (page == null) {
            context.getSource().sendFailure(Component.translatable("nexuscore.command.panel.unknown_page"));
            return 0;
        }
        panel.setPage(page);
        context.getSource().sendSuccess(() -> Component.translatable("nexuscore.panel.page_changed", page.displayName()), true);
        return 1;
    }

    private static int setEnabledPages(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        PanelBlockEntity panel = panelAt(context);
        if (panel == null) {
            return 0;
        }
        String argument = StringArgumentType.getString(context, "paginas").trim();
        int mask = 0;
        List<Component> names = new ArrayList<>();
        if (argument.equalsIgnoreCase(ALL)) {
            mask = PanelPage.ALL_MASK;
            for (PanelPage page : PanelPage.all()) {
                names.add(page.displayName());
            }
        } else {
            for (String id : argument.split("[,\\s]+")) {
                PanelPage page = PanelPage.byId(id.toLowerCase(java.util.Locale.ROOT));
                if (page == null) {
                    context.getSource().sendFailure(Component.translatable("nexuscore.command.panel.unknown_page"));
                    return 0;
                }
                if ((mask & page.bit()) == 0) {
                    names.add(page.displayName());
                }
                mask |= page.bit();
            }
        }
        panel.setEnabledPages(mask);
        Component list = Component.empty();
        for (int i = 0; i < names.size(); i++) {
            list = i == 0 ? names.get(i).copy() : list.copy().append(", ").append(names.get(i));
        }
        Component message = list;
        context.getSource().sendSuccess(() -> Component.translatable("nexuscore.command.panel.pages_changed", message), true);
        return names.size();
    }

    private static int setDimension(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        PanelBlockEntity panel = panelAt(context);
        if (panel == null) {
            return 0;
        }
        ResourceLocation dimension = DimensionArgument.getDimension(context, "dimensao").dimension().location();
        panel.setDimension(dimension);
        context.getSource().sendSuccess(() -> Component.translatable("nexuscore.panel.dimension_changed",
                ExplorationNames.dimension(dimension)), true);
        return 1;
    }

    private static PanelBlockEntity panelAt(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "posicao");
        PanelBlockEntity panel = PanelBlockEntity.controllerOf(context.getSource().getLevel(), pos);
        if (panel == null) {
            context.getSource().sendFailure(Component.translatable("nexuscore.command.panel.not_panel"));
        }
        return panel;
    }

    private PanelCommands() {}
}
