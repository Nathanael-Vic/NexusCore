package com.nexussmp.nexuscore.core.config;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * {@code <mundo>/serverconfig/nexuscore-server.toml}: regras do servidor.
 * O NeoForge manda esta config para os clientes ao entrar, entao o cliente tambem enxerga os valores.
 */
public final class ServerConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<List<? extends String>> TRACKED_DIMENSIONS;
    public static final ModConfigSpec.IntValue AREA_CENTER_X;
    public static final ModConfigSpec.IntValue AREA_CENTER_Z;
    public static final ModConfigSpec.IntValue AREA_RADIUS;
    public static final ModConfigSpec.IntValue CHECK_INTERVAL_TICKS;
    public static final ModConfigSpec.BooleanValue COUNT_CREATIVE;
    public static final ModConfigSpec.BooleanValue COUNT_SPECTATOR;
    public static final ModConfigSpec.IntValue SYNC_DELAY_TICKS;

    public static final ModConfigSpec.IntValue HOLOGRAM_MAX_RECORDING_SECONDS;
    public static final ModConfigSpec.DoubleValue HOLOGRAM_DEFAULT_SCALE;
    public static final ModConfigSpec.IntValue HOLOGRAM_AUDIO_JOIN_WINDOW_SECONDS;
    public static final ModConfigSpec.IntValue HOLOGRAM_AUDIO_RANGE_BLOCKS;
    public static final ModConfigSpec.IntValue HOLOGRAM_MAX_ACTIVE;

    public static final ModConfigSpec.IntValue BACKROOMS_DEFAULT_SIZE;
    public static final ModConfigSpec.IntValue BACKROOMS_DEFAULT_DOORS;
    public static final ModConfigSpec.BooleanValue BACKROOMS_DEFAULT_CEILING;
    public static final ModConfigSpec.IntValue BACKROOMS_LAMP_SPACING;
    public static final ModConfigSpec.IntValue BACKROOMS_LAMP_CHANCE;
    public static final ModConfigSpec.IntValue BACKROOMS_CELL_SIZE;
    public static final ModConfigSpec.IntValue BACKROOMS_CHUNKS_PER_TICK;
    public static final ModConfigSpec.IntValue BACKROOMS_EVENT_ODDS;
    public static final ModConfigSpec.IntValue BACKROOMS_EVENT_COOLDOWN_MINUTES;
    public static final ModConfigSpec.BooleanValue BACKROOMS_SKINWALKER;
    public static final ModConfigSpec.IntValue BACKROOMS_SKINWALKER_DELAY_SECONDS;
    public static final ModConfigSpec.IntValue BACKROOMS_SKINWALKER_STALK_SECONDS;
    public static final ModConfigSpec.IntValue BACKROOMS_SKINWALKER_REVEAL_SECONDS;

    public static final ModConfigSpec.IntValue QUEST_MAX_OPEN_PER_PLAYER;
    public static final ModConfigSpec.IntValue QUEST_MAX_SLOTS;
    public static final ModConfigSpec.IntValue QUEST_BOARD_RANGE;
    public static final ModConfigSpec.IntValue QUEST_HISTORY_LIMIT;

    public static final ModConfigSpec.ConfigValue<List<? extends String>> MORPH_BLOCKED_ENTITIES;
    public static final ModConfigSpec.BooleanValue MORPH_ALLOW_PLAYERS;
    public static final ModConfigSpec.BooleanValue MORPH_FLIGHT;
    public static final ModConfigSpec.BooleanValue MORPH_SWIM;
    public static final ModConfigSpec.BooleanValue MORPH_FIRE_IMMUNITY;

    public static final ModConfigSpec.BooleanValue ENVELOPE_SUMMON_COURIERS;
    public static final ModConfigSpec.IntValue ENVELOPE_COURIER_WAIT_SECONDS;
    public static final ModConfigSpec.IntValue ENVELOPE_ASCEND_DISTANCE;
    public static final ModConfigSpec.BooleanValue ENVELOPE_LORD_BATS;

    public static final ModConfigSpec.BooleanValue PANEL_REQUIRE_OPERATOR;
    public static final ModConfigSpec.IntValue PANEL_MAX_WIDTH;
    public static final ModConfigSpec.IntValue PANEL_MAX_HEIGHT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Estatisticas de exploracao: o que os jogadores descobrem, por quem e quando.").push("stats");
        TRACKED_DIMENSIONS = builder
                .comment("Dimensoes rastreadas (ids). Descobertas fora delas nao contam.")
                .defineListAllowEmpty("trackedDimensions", List.of("minecraft:overworld"),
                        () -> "minecraft:overworld", ServerConfig::isResourceLocation);

        builder.comment("Area rastreada: um quadrado centrado em (centerX, centerZ).",
                        "So conta o que for descoberto dentro dele. Nada e escaneado: as contagens sobem",
                        "conforme os jogadores exploram. Vale para cada dimensao rastreada, nas coordenadas dela.")
                .push("area");
        AREA_CENTER_X = builder.defineInRange("centerX", 0, -30_000_000, 30_000_000);
        AREA_CENTER_Z = builder.defineInRange("centerZ", 0, -30_000_000, 30_000_000);
        AREA_RADIUS = builder
                .comment("Metade do lado do quadrado, em blocos. 5000 = de -5000 a 5000 em X e Z. 0 = sem limite.")
                .defineInRange("radius", 5000, 0, 30_000_000);
        builder.pop();

        CHECK_INTERVAL_TICKS = builder
                .comment("De quantos em quantos ticks cada jogador e verificado (20 = 1 segundo).")
                .defineInRange("checkIntervalTicks", 20, 1, 200);
        COUNT_CREATIVE = builder
                .comment("Jogadores no criativo contam?")
                .define("countCreative", false);
        COUNT_SPECTATOR = builder
                .comment("Jogadores no espectador contam?")
                .define("countSpectator", false);
        SYNC_DELAY_TICKS = builder
                .comment("Espera antes de mandar descobertas novas aos clientes; varias no mesmo intervalo viram um pacote.")
                .defineInRange("syncDelayTicks", 20, 1, 200);
        builder.pop();

        builder.comment("Hologramas: gravacao de movimento de jogador e reproducao no mundo.").push("hologram");
        HOLOGRAM_MAX_RECORDING_SECONDS = builder
                .comment("Tempo maximo de uma gravacao. Ao chegar no limite, ela e salva sozinha.")
                .defineInRange("maxRecordingSeconds", 600, 10, 3600);
        HOLOGRAM_DEFAULT_SCALE = builder
                .comment("Tamanho padrao do holograma (1.0 = tamanho de jogador).")
                .defineInRange("defaultScale", 1.0, 0.1, 10.0);
        HOLOGRAM_AUDIO_JOIN_WINDOW_SECONDS = builder
                .comment("Quem chega perto depois deste tempo do inicio ve o holograma, mas nao ouve o audio",
                         "(o jogo nao sabe comecar um som no meio).")
                .defineInRange("audioJoinWindowSeconds", 3, 0, 60);
        HOLOGRAM_AUDIO_RANGE_BLOCKS = builder
                .comment("Distancia, em blocos, em que a fala ainda e ouvida.",
                         "O jogo so trabalha de 16 em 16: valores viram o multiplo mais proximo.")
                .defineInRange("audioRangeBlocks", 48, 16, 256);
        HOLOGRAM_MAX_ACTIVE = builder
                .comment("Quantos hologramas podem estar tocando ao mesmo tempo.")
                .defineInRange("maxActive", 8, 1, 64);
        builder.pop();

        builder.comment("Backrooms: o andar gerado e o evento raro de cair nele.").push("backrooms");
        BACKROOMS_DEFAULT_SIZE = builder
                .comment("Lado do andar, em blocos, quando o comando nao diz outro.")
                .defineInRange("defaultSize", 400, 100, 700);
        BACKROOMS_DEFAULT_DOORS = builder
                .comment("Portas de saida por andar. Caem em qualquer chao, quarto ou corredor, o mais",
                         "longe umas das outras que o tamanho do andar permite.")
                .defineInRange("defaultDoors", 3, 0, 32);
        BACKROOMS_DEFAULT_CEILING = builder
                .comment("Gerar o teto (com os paineis de luz) por padrao.")
                .define("defaultCeiling", true);
        BACKROOMS_LAMP_SPACING = builder
                .comment("Blocos de forro entre um painel de luz e o seguinte.",
                         "3 = luz, tres blocos de forro, luz. Valor baixo enche o teto de luz e deixa",
                         "a geracao mais pesada.")
                .defineInRange("lampSpacing", 3, 1, 16);
        BACKROOMS_LAMP_CHANCE = builder
                .comment("Chance, em porcento, de um lugar de painel realmente ganhar luz.",
                         "Abaixo de 100 o teto fica irregular: cantos escuros no meio do corredor aceso.")
                .defineInRange("lampChance", 75, 1, 100);
        BACKROOMS_CELL_SIZE = builder
                .comment("Largura do corredor, em blocos. E a escala da planta: 1 da corredor de um",
                         "bloco (labirinto apertado), 3 da o corredor largo das backrooms.")
                .defineInRange("cellSize", 3, 1, 5);
        BACKROOMS_CHUNKS_PER_TICK = builder
                .comment("Chunks construidos por tick. Mais e mais rapido e mais pesado.")
                .defineInRange("chunksPerTick", 8, 1, 64);
        BACKROOMS_EVENT_ODDS = builder
                .comment("Chance do evento: 1 em N por minuto de jogo, por pessoa.",
                         "20160 = uma vez a cada duas semanas de jogo sem parar. 0 desliga o evento",
                         "(o comando continua funcionando).")
                .defineInRange("eventOddsPerMinute", 20160, 0, 10000000);
        BACKROOMS_EVENT_COOLDOWN_MINUTES = builder
                .comment("Tempo minimo, em minutos, antes de a mesma pessoa poder cair de novo.")
                .defineInRange("eventCooldownMinutes", 1440, 0, 100000);
        BACKROOMS_SKINWALKER = builder
                .comment("Ligar o skinwalker. Desligado, o andar fica so vazio e zumbindo.")
                .define("skinwalker", true);
        BACKROOMS_SKINWALKER_DELAY_SECONDS = builder
                .comment("Quanto tempo de solidao antes de ele aparecer, em segundos.")
                .defineInRange("skinwalkerDelaySeconds", 45, 5, 3600);
        BACKROOMS_SKINWALKER_STALK_SECONDS = builder
                .comment("Quanto tempo ele ronda fingindo ser gente antes de se revelar.")
                .defineInRange("skinwalkerStalkSeconds", 75, 5, 600);
        BACKROOMS_SKINWALKER_REVEAL_SECONDS = builder
                .comment("Quanto dura a revelacao (osso estalando e luz falhando) antes da cacada.")
                .defineInRange("skinwalkerRevealSeconds", 6, 1, 30);
        builder.pop();

        builder.comment("Mural de quests: lista unica do servidor, com recompensa combinada entre as pessoas.")
                .push("quest");
        QUEST_MAX_OPEN_PER_PLAYER = builder
                .comment("Quantas quests abertas cada jogador pode ter no mural.")
                .defineInRange("maxOpenPerPlayer", 3, 1, 50);
        QUEST_MAX_SLOTS = builder
                .comment("Maximo de pessoas que podem assumir a mesma quest.")
                .defineInRange("maxSlots", 10, 1, 64);
        QUEST_BOARD_RANGE = builder
                .comment("Distancia maxima do quest board para assumir ou criar uma quest.",
                         "O resto (desistir, dizer que fez, conferir, apagar) funciona em qualquer lugar,",
                         "pelo livro do inventario.")
                .defineInRange("boardRange", 6, 1, 32);
        QUEST_HISTORY_LIMIT = builder
                .comment("Quantas quests concluidas ficam guardadas no historico.")
                .defineInRange("historyLimit", 50, 0, 500);
        builder.pop();

        builder.comment("Correio do Envelope: o pombo que vem de longe buscar a carta, e o morcego do lorde.")
                .push("envelope");
        ENVELOPE_SUMMON_COURIERS = builder
                .comment("Carta esperando na caixa e nenhum pombo por perto: um pombo carteiro vem de longe,",
                         "de um ponto que ninguem esta olhando, buscar a carta.")
                .define("summonCouriers", true);
        ENVELOPE_COURIER_WAIT_SECONDS = builder
                .comment("Quanto a carta espera na caixa antes de chamar o pombo carteiro. Nesse tempo, um",
                         "pombo que ja esteja por perto tem a chance de buscar ele mesmo.")
                .defineInRange("courierWaitSeconds", 5, 0, 300);
        ENVELOPE_ASCEND_DISTANCE = builder
                .comment("A que distancia da caixa o pombo some ao partir e surge ao chegar (para fora e",
                         "para cima). O Envelope usa 16, perto o bastante para ver o pombo sumir no ar.")
                .defineInRange("ascendDistance", 32, 16, 64);
        ENVELOPE_LORD_BATS = builder
                .comment("Carta da caixa de um lorde vampiro vai por morcego, e so a noite.")
                .define("lordBats", true);
        builder.pop();

        builder.comment("Painel de exploracao (bloco).").push("panel");
        PANEL_REQUIRE_OPERATOR = builder
                .comment("So operadores (nivel 2) ou jogadores no criativo colocam e configuram paineis.")
                .define("requireOperator", true);
        PANEL_MAX_WIDTH = builder
                .comment("Largura maxima de uma tela formada por varios paineis, em blocos.")
                .defineInRange("maxWidth", 8, 1, 16);
        PANEL_MAX_HEIGHT = builder
                .comment("Altura maxima de uma tela formada por varios paineis, em blocos.")
                .defineInRange("maxHeight", 5, 1, 16);
        builder.pop();

        builder.comment("Anel da metamorfose: copiar a forma de uma criatura ou de um jogador e virar ela.")
                .push("morph");
        MORPH_BLOCKED_ENTITIES = builder
                .comment("Criaturas que o anel nao copia (ids de tipo de entidade).",
                         "Os chefes grandes vem bloqueados: a hitbox do dragao tem 16x8 blocos e prenderia",
                         "a pessoa em qualquer lugar. Vale tambem para quem ja tinha a forma no anel.")
                .defineListAllowEmpty("blockedEntities", List.of("minecraft:ender_dragon", "minecraft:wither"),
                        () -> "minecraft:ender_dragon", ServerConfig::isResourceLocation);
        MORPH_ALLOW_PLAYERS = builder
                .comment("Deixar o anel copiar outros jogadores: skin e nome em cima da cabeca.")
                .define("allowPlayers", true);
        MORPH_FLIGHT = builder
                .comment("Quem vira bicho que voa pode voar (o voo do criativo, com dois pulos).")
                .define("flight", true);
        MORPH_SWIM = builder
                .comment("Quem vira bicho da agua nada com o dobro da velocidade, e respira debaixo",
                         "d'agua se o bicho respira.")
                .define("swim", true);
        MORPH_FIRE_IMMUNITY = builder
                .comment("Quem vira bicho que nao queima (blaze, ghast, cubo de magma) nao se machuca",
                         "com fogo nem com lava.")
                .define("fireImmunity", true);
        builder.pop();

        SPEC = builder.build();
    }

    private static boolean isResourceLocation(Object value) {
        return value instanceof String text && ResourceLocation.tryParse(text) != null;
    }

    /** Leitura segura antes da config carregar (tela de titulo, cliente sem servidor): devolve o padrao. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    private ServerConfig() {}
}
