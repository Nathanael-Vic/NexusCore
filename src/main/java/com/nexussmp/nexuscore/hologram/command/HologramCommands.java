package com.nexussmp.nexuscore.hologram.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nexussmp.nexuscore.hologram.HologramPlayback;
import com.nexussmp.nexuscore.hologram.HologramRecorder;
import com.nexussmp.nexuscore.hologram.HologramRecording;
import com.nexussmp.nexuscore.hologram.HologramSettings;
import com.nexussmp.nexuscore.hologram.HologramStorage;
import com.nexussmp.nexuscore.hologram.entity.HologramEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/** {@code /nexus hologram ...}: gravar, parar, reproduzir. */
public final class HologramCommands {
    private static final int OPERATOR_LEVEL = 2;
    /** Mesma faixa do atributo de tamanho do jogo, com folga: de bibelo a gigante. */
    private static final float MIN_SCALE = 0.1F;
    private static final float MAX_SCALE = 10.0F;
    private static final String ALL = "todos";

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("holograma")
                .requires(source -> source.hasPermission(OPERATOR_LEVEL))
                .then(Commands.literal("gravar")
                        .then(Commands.literal("parar").executes(context -> stopRecording(context.getSource())))
                        .then(Commands.literal("cancelar").executes(context -> cancelRecording(context.getSource())))
                        .then(Commands.argument("nome", StringArgumentType.word())
                                .executes(context -> startRecording(context.getSource(),
                                        StringArgumentType.getString(context, "nome")))))
                // As opcoes vem uma depois da outra, sem palavra na frente: o proprio jogo mostra
                // <tamanho>, <rotacao> e companhia acima do campo enquanto se digita.
                .then(Commands.literal("tocar")
                        .then(gravacao()
                                .executes(context -> play(context, false))
                                .then(Commands.argument("tamanho", FloatArgumentType.floatArg(MIN_SCALE, MAX_SCALE))
                                        .executes(context -> play(context, false))
                                        .then(Commands.argument("rotacao", FloatArgumentType.floatArg(-360.0F, 360.0F))
                                                .executes(context -> play(context, false))
                                                .then(Commands.argument("posicao", Vec3Argument.vec3())
                                                        .executes(context -> play(context, false))
                                                        .then(Commands.argument("repetir", BoolArgumentType.bool())
                                                                .executes(context -> play(context,
                                                                        BoolArgumentType.getBool(context, "repetir")))))))))
                .then(Commands.literal("parar")
                        .executes(context -> stop(context.getSource(), null))
                        .then(Commands.argument("nome", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        java.util.stream.Stream.concat(java.util.stream.Stream.of(ALL),
                                                HologramStorage.list().stream()).toList(), builder))
                                .executes(context -> stop(context.getSource(),
                                        StringArgumentType.getString(context, "nome")))))
                .then(Commands.literal("lista").executes(context -> list(context.getSource())))
                .then(Commands.literal("info")
                        .then(gravacao()
                                .executes(context -> info(context.getSource(),
                                        StringArgumentType.getString(context, "nome")))))
                .then(Commands.literal("audio")
                        .then(gravacao()
                                .then(Commands.argument("som", ResourceLocationArgument.id())
                                        .executes(context -> audio(context.getSource(),
                                                StringArgumentType.getString(context, "nome"),
                                                ResourceLocationArgument.getId(context, "som"))))))
                .then(Commands.literal("apagar")
                        .then(gravacao()
                                .executes(context -> delete(context.getSource(),
                                        StringArgumentType.getString(context, "nome")))));
    }

    /** O nome da gravacao, ja com a lista do que existe como sugestao. */
    private static RequiredArgumentBuilder<CommandSourceStack, String> gravacao() {
        return Commands.argument("nome", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(HologramStorage.list(), builder));
    }

    // ---- gravacao ----

    private static int startRecording(CommandSourceStack source, String rawName) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String name = rawName.toLowerCase(Locale.ROOT);
        if (!HologramStorage.isValidName(name)) {
            source.sendFailure(Component.translatable("nexuscore.hologram.invalid_name"));
            return 0;
        }
        if (HologramRecorder.isRecording(player)) {
            source.sendFailure(Component.translatable("nexuscore.hologram.record.already"));
            return 0;
        }
        HologramRecorder.start(player, name);
        source.sendSuccess(() -> Component.translatable("nexuscore.hologram.record.started", name), true);
        return 1;
    }

    private static int stopRecording(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        HologramRecording recording = HologramRecorder.stop(player);
        if (recording == null) {
            source.sendFailure(Component.translatable("nexuscore.hologram.record.none"));
            return 0;
        }
        if (!HologramStorage.save(source.getServer(), recording)) {
            source.sendFailure(Component.translatable("nexuscore.hologram.record.save_failed", recording.name()));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("nexuscore.hologram.record.saved",
                recording.name(), seconds(recording.seconds()), recording.tickCount()), true);
        return 1;
    }

    private static int cancelRecording(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!HologramRecorder.cancel(player)) {
            source.sendFailure(Component.translatable("nexuscore.hologram.record.none"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("nexuscore.hologram.record.cancelled"), true);
        return 1;
    }

    // ---- reproducao ----

    /** {@code null} quando a opcao nao foi escrita: o Brigadier reclama de argumento que nao existe. */
    @Nullable
    private static <T> T optional(CommandContext<CommandSourceStack> context, String name, Class<T> type) {
        try {
            return context.getArgument(name, type);
        } catch (IllegalArgumentException absent) {
            return null;
        }
    }

    private static int play(CommandContext<CommandSourceStack> context, boolean loop) {
        Vec3 position = optional(context, "posicao", Coordinates.class) == null
                ? null
                : Vec3Argument.getVec3(context, "posicao");
        Float scale = optional(context, "tamanho", Float.class);
        Float rotation = optional(context, "rotacao", Float.class);
        CommandSourceStack source = context.getSource();
        String name = StringArgumentType.getString(context, "nome").toLowerCase(Locale.ROOT);
        HologramRecording recording = HologramStorage.load(source.getServer(), name);
        if (recording == null) {
            source.sendFailure(Component.translatable("nexuscore.hologram.not_found", name));
            return 0;
        }
        ServerLevel level = source.getLevel();
        HologramSettings settings = HologramSettings.current();
        if (!settings.enabled()) {
            source.sendFailure(Component.translatable("nexuscore.hologram.disabled"));
            return 0;
        }
        if (HologramPlayback.activeCount(level) >= settings.maxActive()) {
            source.sendFailure(Component.translatable("nexuscore.hologram.too_many", settings.maxActive()));
            return 0;
        }

        Vec3 where = position != null ? position : source.getPosition();
        float finalScale = scale != null ? scale : settings.defaultScale();
        float finalRotation = rotation != null ? rotation : 0.0F;
        HologramEntity hologram = HologramPlayback.start(level, recording, where, finalRotation, finalScale, loop);
        if (hologram == null) {
            source.sendFailure(Component.translatable("nexuscore.hologram.spawn_failed"));
            return 0;
        }
        // A posicao vai na resposta de proposito: e o ponto onde os pes ficam, e o holograma cresce
        // para cima a partir dele. Com isso da para conferir na hora se ele nasceu onde se pediu.
        source.sendSuccess(() -> Component.translatable("nexuscore.hologram.playing",
                name, seconds(recording.seconds()), position(hologram.position()), seconds(finalScale)), true);
        return 1;
    }

    private static int stop(CommandSourceStack source, @Nullable String rawName) {
        String name = rawName == null || rawName.equalsIgnoreCase(ALL) ? null : rawName.toLowerCase(Locale.ROOT);
        int stopped = HologramPlayback.stop(source.getLevel(), name);
        source.sendSuccess(() -> Component.translatable("nexuscore.hologram.stopped", stopped), true);
        return stopped;
    }

    private static int list(CommandSourceStack source) {
        List<String> names = HologramStorage.list();
        if (names.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("nexuscore.hologram.list.empty"), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("nexuscore.hologram.list", names.size(),
                String.join(", ", names)), false);
        return names.size();
    }

    private static int info(CommandSourceStack source, String rawName) {
        String name = rawName.toLowerCase(Locale.ROOT);
        HologramRecording recording = HologramStorage.load(source.getServer(), name);
        if (recording == null) {
            source.sendFailure(Component.translatable("nexuscore.hologram.not_found", name));
            return 0;
        }
        ResourceLocation sound = recording.sound();
        source.sendSuccess(() -> Component.translatable("nexuscore.hologram.info", name,
                seconds(recording.seconds()), recording.tickCount(), recording.authorName(),
                sound == null ? "-" : sound.toString()), false);
        return 1;
    }

    private static int audio(CommandSourceStack source, String rawName, ResourceLocation sound) {
        String name = rawName.toLowerCase(Locale.ROOT);
        HologramRecording recording = HologramStorage.load(source.getServer(), name);
        if (recording == null) {
            source.sendFailure(Component.translatable("nexuscore.hologram.not_found", name));
            return 0;
        }
        recording.setSound(sound);
        if (!HologramStorage.save(source.getServer(), recording)) {
            source.sendFailure(Component.translatable("nexuscore.hologram.record.save_failed", name));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("nexuscore.hologram.audio_set", name, sound.toString()), true);
        return 1;
    }

    private static int delete(CommandSourceStack source, String rawName) {
        String name = rawName.toLowerCase(Locale.ROOT);
        if (!HologramStorage.delete(name)) {
            source.sendFailure(Component.translatable("nexuscore.hologram.not_found", name));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("nexuscore.hologram.deleted", name), true);
        return 1;
    }

    private static String position(Vec3 where) {
        return String.format(Locale.ROOT, "%.1f, %.1f, %.1f", where.x, where.y, where.z);
    }

    private static String seconds(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private HologramCommands() {}
}
