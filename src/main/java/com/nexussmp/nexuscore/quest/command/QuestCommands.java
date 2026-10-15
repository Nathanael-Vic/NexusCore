package com.nexussmp.nexuscore.quest.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.nexussmp.nexuscore.quest.Quest;
import com.nexussmp.nexuscore.quest.QuestData;
import com.nexussmp.nexuscore.quest.QuestSync;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * {@code /nexus quests ...}: administracao do mural e as <b>missoes do servidor</b>.
 *
 * <p>O uso normal do mural e pelo bloco. O que so existe aqui e a missao entregue por comando: ela
 * cai direto no livro da pessoa, sem autor visivel e sem passar pelo mural, e serve para o que o
 * servidor pede a alguem — a prova para voltar a ser humano, por exemplo.
 *
 * <p>O texto vem num campo so, separado por barras:
 *
 * <pre>/nexus quests dar Fulano Cura do vampirismo | Traga 10 lingotes de ouro | Volta a ser humano</pre>
 *
 * <p>Titulo, descricao e recompensa, nessa ordem; os dois ultimos sao opcionais. E uma barra so por
 * campo, o que deixa escrever a frase inteira de uma vez em vez de encher a tela de argumentos.
 */
public final class QuestCommands {
    private static final int ADMIN_LEVEL = 2;

    /**
     * O completar do comando oferece os ids que estao esperando conferencia.
     *
     * <p>Ninguem digita um UUID inteiro a mao. Com isto, {@code /nexus quests concluir} + Tab ja
     * oferece o que ha para fechar, com o titulo e o nome de quem entregou ao lado.
     *
     * <p>Mesmo assim, o caminho bom para o dia a dia e a aba de administracao no inventario: la a
     * quest se fecha com um clique no nome de quem cumpriu.
     */
    private static final SuggestionProvider<CommandSourceStack> PENDING_IDS = (context, builder) -> {
        for (Quest quest : QuestData.get(context.getSource().getServer()).awaitingCheck()) {
            builder.suggest(quest.id().toString(),
                    Component.translatable("nexuscore.quest.command.pending_hint",
                            quest.title(), names(quest)));
        }
        return builder.buildFuture();
    };

    /** Quem levantou a mao, para caber na dica do completar. */
    private static String names(Quest quest) {
        StringBuilder names = new StringBuilder();
        for (java.util.UUID id : quest.submissions()) {
            if (!names.isEmpty()) {
                names.append(", ");
            }
            names.append(quest.accepted().getOrDefault(id, "?"));
        }
        return names.isEmpty() ? quest.ownerName() : names.toString();
    }
    private static final String SEPARATOR = "\\|";

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("quests")
                .requires(source -> source.hasPermission(ADMIN_LEVEL))
                .then(Commands.literal("lista").executes(QuestCommands::list))
                .then(Commands.literal("dar")
                        .then(Commands.argument("jogadores", EntityArgument.players())
                                .then(Commands.argument("texto", StringArgumentType.greedyString())
                                        .executes(context -> give(context.getSource(),
                                                EntityArgument.getPlayers(context, "jogadores"),
                                                StringArgumentType.getString(context, "texto"))))))
                .then(Commands.literal("pendentes").executes(QuestCommands::pending))
                .then(Commands.literal("dadas")
                        .then(Commands.argument("jogador", EntityArgument.player())
                                .executes(context -> given(context.getSource(),
                                        EntityArgument.getPlayer(context, "jogador")))))
                .then(Commands.literal("concluir")
                        .then(Commands.argument("id", UuidArgument.uuid())
                                .suggests(PENDING_IDS)
                                .executes(context -> close(context, UuidArgument.getUuid(context, "id")))))
                .then(Commands.literal("apagar")
                        .then(Commands.argument("id", UuidArgument.uuid())
                                .executes(context -> delete(context, UuidArgument.getUuid(context, "id")))));
    }

    // ---- missoes do servidor ----

    private static int give(CommandSourceStack source, Collection<ServerPlayer> targets, String text) {
        String[] parts = text.split(SEPARATOR, 3);
        String title = parts[0].trim();
        String description = parts.length > 1 ? parts[1].trim() : "";
        String reward = parts.length > 2 ? parts[2].trim() : "";
        if (title.isEmpty()) {
            source.sendFailure(Component.translatable("nexuscore.quest.command.no_title"));
            return 0;
        }

        QuestData data = QuestData.get(source.getServer());
        int sent = 0;
        for (ServerPlayer target : targets) {
            if (data.give(target, title, description, reward) == QuestData.Result.OK) {
                sent++;
                // O aviso no chat e o par do selo no botao: um para quem esta olhando o jogo, outro
                // para quem so vai abrir o inventario depois.
                target.sendSystemMessage(Component.translatable("nexuscore.quest.given_to_you", title));
            }
        }
        if (sent == 0) {
            source.sendFailure(Component.translatable("nexuscore.quest.command.no_title"));
            return 0;
        }
        QuestSync.markDirty();
        int total = sent;
        source.sendSuccess(() -> Component.translatable("nexuscore.quest.command.given", total, title), true);
        return sent;
    }

    private static int pending(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        List<Quest> waiting = QuestData.get(source.getServer()).awaitingCheck();
        if (waiting.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("nexuscore.quest.command.no_pending"), false);
            return 0;
        }
        for (Quest quest : waiting) {
            source.sendSuccess(() -> Component.translatable("nexuscore.quest.command.pending_entry",
                    names(quest), quest.title(), quest.id().toString()), false);
        }
        return waiting.size();
    }

    private static int given(CommandSourceStack source, ServerPlayer player) {
        List<Quest> given = QuestData.get(source.getServer()).givenTo(player.getUUID());
        if (given.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("nexuscore.quest.command.no_given",
                    player.getGameProfile().getName()), false);
            return 0;
        }
        for (Quest quest : given) {
            source.sendSuccess(() -> Component.translatable("nexuscore.quest.command.given_entry",
                    quest.title(), Component.translatable(stateKey(quest)), quest.id().toString()), false);
        }
        return given.size();
    }

    /** Tres estados, e so isso: pedida, entregue para conferir, conferida. */
    private static String stateKey(Quest quest) {
        if (!quest.isOpen()) {
            return "nexuscore.quest.state.closed";
        }
        return quest.pending() ? "nexuscore.quest.state.waiting" : "nexuscore.quest.state.doing";
    }

    private static int close(CommandContext<CommandSourceStack> context, UUID id) {
        CommandSourceStack source = context.getSource();
        QuestData.Result result = QuestData.get(source.getServer()).closeGiven(id);
        if (result != QuestData.Result.OK) {
            source.sendFailure(Component.translatable("nexuscore.quest.command.not_found"));
            return 0;
        }
        QuestSync.markDirty();
        source.sendSuccess(() -> Component.translatable("nexuscore.quest.command.closed"), true);
        return 1;
    }

    // ---- mural ----

    private static int list(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        List<Quest> quests = QuestData.get(source.getServer()).all();
        if (quests.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("nexuscore.quest.command.empty"), false);
            return 0;
        }
        for (Quest quest : quests) {
            source.sendSuccess(() -> Component.translatable("nexuscore.quest.command.entry",
                    quest.title(), quest.fromServer() ? quest.ownerName() : quest.authorName(),
                    quest.acceptedCount(), quest.maxSlots(),
                    quest.isOpen() ? "aberta" : "concluida", quest.id().toString()), false);
        }
        return quests.size();
    }

    private static int delete(CommandContext<CommandSourceStack> context, UUID id) {
        CommandSourceStack source = context.getSource();
        QuestData.Result result = QuestData.get(source.getServer()).delete(id, null, true);
        if (result != QuestData.Result.OK) {
            source.sendFailure(Component.translatable("nexuscore.quest.command.not_found"));
            return 0;
        }
        QuestSync.markDirty();
        source.sendSuccess(() -> Component.translatable("nexuscore.quest.command.deleted"), true);
        return 1;
    }

    private QuestCommands() {}
}
