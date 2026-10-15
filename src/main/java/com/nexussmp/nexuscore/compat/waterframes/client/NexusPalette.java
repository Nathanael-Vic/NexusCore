package com.nexussmp.nexuscore.compat.waterframes.client;

/** Cores das telas do Minecraft usadas na interface do WaterFrames. ARGB. */
public final class NexusPalette {
    /** Rotulo sobre o painel claro, igual ao titulo "Inventario": sem sombra. */
    public static final int LABEL = 0xFF404040;

    /** Texto dentro de campo de texto (vanilla EditBox). */
    public static final int FIELD_TEXT = 0xFFE0E0E0;
    public static final int FIELD_TEXT_DISABLED = 0xFF707070;
    /** Destaque ao passar o mouse (vanilla). */
    public static final int HIGHLIGHT_TEXT = 0xFFFFFFA0;

    /** Veu sobre controles desativados: neutro, para nao "sujar" o painel claro de preto. */
    public static final int DISABLED_OVERLAY = 0x73707070;

    private NexusPalette() {}
}
