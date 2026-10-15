package com.nexussmp.nexuscore.display.client.render;

/**
 * Cores do conteudo da tela (ARGB). O fundo e a moldura nao estao aqui: vem da textura do bloco
 * ({@code textures/block/panel/}), desenhada pelo modelo.
 */
final class PanelColors {
    static final int TITLE = 0xFFFFFFFF;
    static final int LABEL = 0xFFA8A8A8;
    static final int VALUE = 0xFFFFFFFF;
    static final int PIONEER = 0xFFFFFF55;
    static final int MUTED = 0xFF7A7F85;
    static final int SEPARATOR = 0xFF2C3238;

    private PanelColors() {}
}
