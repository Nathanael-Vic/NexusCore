package com.nexussmp.nexuscore.core.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** {@code config/nexuscore-client.toml}: opcoes que so fazem sentido no cliente. */
public final class ClientConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue WATERFRAMES_VANILLA_STYLE;
    public static final ModConfigSpec.BooleanValue WATERFRAMES_GRAYSCALE_ICONS;
    public static final ModConfigSpec.BooleanValue WATERFRAMES_KEEP_ERROR_RED;
    public static final ModConfigSpec.BooleanValue WATERFRAMES_SWAP_RESIZE_ICONS;
    public static final ModConfigSpec.BooleanValue OPTIONS_REMOVE_HEADER_EXTRAS;
    public static final ModConfigSpec.BooleanValue MAIN_MENU_ENABLED;
    public static final ModConfigSpec.ConfigValue<String> MAIN_MENU_SERVER_ADDRESS;
    public static final ModConfigSpec.IntValue PANEL_RENDER_DISTANCE;
    public static final ModConfigSpec.IntValue RADIO_VOLUME;
    public static final ModConfigSpec.ConfigValue<String> HOLOGRAM_COLOR;
    public static final ModConfigSpec.DoubleValue HOLOGRAM_ALPHA;
    public static final ModConfigSpec.DoubleValue HOLOGRAM_SCANLINES;
    public static final ModConfigSpec.DoubleValue HOLOGRAM_FLICKER;
    public static final ModConfigSpec.DoubleValue HOLOGRAM_RIM;
    public static final ModConfigSpec.BooleanValue HOLOGRAM_PARTICLES;
    public static final ModConfigSpec.BooleanValue HOLOGRAM_ITEMS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Interface do WaterFrames no estilo das telas do Minecraft.",
                        "Para voltar ao visual original, desligue modules.compat.waterframes.enabled",
                        "em nexuscore-common.toml.")
                .push("waterframes");
        WATERFRAMES_VANILLA_STYLE = builder
                .comment("Painel, secoes e campos com o visual do inventario.")
                .define("vanillaStyle", true);
        WATERFRAMES_GRAYSCALE_ICONS = builder
                .comment("Icones do WaterFrames em cinza. Aplica ao recarregar recursos (F3+T).")
                .define("grayscaleIcons", true);
        WATERFRAMES_KEEP_ERROR_RED = builder
                .comment("Mantem vermelho so nos icones e cores de erro. false = tudo neutro.")
                .define("keepErrorRed", true);
        WATERFRAMES_SWAP_RESIZE_ICONS = builder
                .comment("Troca os icones de proporcao: seta horizontal ao lado da largura, vertical ao lado da altura.")
                .define("swapResizeIcons", true);
        builder.pop();

        builder.comment("Tela de opcoes do jogo.").push("options");
        OPTIONS_REMOVE_HEADER_EXTRAS = builder
                .comment("Tira do cabecalho os botoes que outros mods penduram ao lado da dificuldade.",
                        "Com eles, o cabecalho estoura a altura fixa e o titulo encosta no topo da tela.")
                .define("removeHeaderExtras", true);
        builder.pop();

        builder.comment("Aparencia dos hologramas. Da para ajustar em jogo (F3+T nao precisa; recarrega na hora).")
                .push("hologram");
        HOLOGRAM_COLOR = builder
                .comment("Cor do holograma em hexadecimal (RRGGBB).")
                .define("color", "74D8FF");
        HOLOGRAM_ALPHA = builder
                .comment("Transparencia geral: 1.0 = solido.")
                .defineInRange("alpha", 0.62, 0.05, 1.0);
        HOLOGRAM_SCANLINES = builder
                .comment("Quantidade de linhas horizontais por bloco de altura. 0 = sem linhas.")
                .defineInRange("scanlines", 12.0, 0.0, 64.0);
        HOLOGRAM_FLICKER = builder
                .comment("Forca da oscilacao (o 'piscar' da projecao). 0 = imagem estavel.")
                .defineInRange("flicker", 0.08, 0.0, 0.5);
        HOLOGRAM_RIM = builder
                .comment("Brilho na silhueta, que da o contorno luminoso.")
                .defineInRange("rimGlow", 0.55, 0.0, 2.0);
        HOLOGRAM_PARTICLES = builder
                .comment("Particulas subindo em volta da projecao.")
                .define("particles", true);
        HOLOGRAM_ITEMS = builder
                .comment("Desenhar o item que a pessoa segurava (tambem em cor de holograma).")
                .define("renderItems", true);
        builder.pop();

        builder.comment("Menu principal do NexusSMP, no lugar da tela de titulo do jogo.").push("mainMenu");
        MAIN_MENU_ENABLED = builder
                .comment("Usar o menu do NexusSMP. false = tela de titulo original do Minecraft.")
                .define("enabled", true);
        MAIN_MENU_SERVER_ADDRESS = builder
                .comment("Endereco do servidor que o botao 'Entrar' abre direto (ex.: play.exemplo.com ou 1.2.3.4:25565).",
                        "Vazio = o botao fica desligado ate alguem preencher.")
                .define("serverAddress", "");
        builder.pop();

        builder.comment("Painel de exploracao.").push("panel");
        PANEL_RENDER_DISTANCE = builder
                .comment("Distancia maxima, em blocos, em que a tela do painel e desenhada.")
                .defineInRange("renderDistance", 48, 8, 128);
        builder.pop();

        builder.comment("Radio ao vivo.").push("radio");
        RADIO_VOLUME = builder
                .comment("Volume da radio para voce, de 0 a 100.",
                        "E o seu volume e so o seu: mexer aqui nao encosta no volume do jogo nem no",
                        "do chat de voz. Da para ajustar pela propria tela da radio.")
                .defineInRange("volume", 70, 0, 100);
        builder.pop();

        SPEC = builder.build();
    }

    /** Leitura segura antes da config carregar: devolve o padrao. */
    public static boolean get(ModConfigSpec.BooleanValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    public static int get(ModConfigSpec.IntValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    public static double get(ModConfigSpec.DoubleValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    public static <T> T get(ModConfigSpec.ConfigValue<T> value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    private ClientConfig() {}
}
