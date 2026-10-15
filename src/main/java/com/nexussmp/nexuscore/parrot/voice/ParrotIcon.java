package com.nexussmp.nexuscore.parrot.voice;

/**
 * O iconezinho da categoria "Papagaios" nas configuracoes do chat de voz: uma cabeca de arara.
 *
 * <p>Desenhado aqui mesmo, pixel por pixel, em vez de uma imagem nos recursos: o servidor e quem
 * registra a categoria, e o servidor dedicado nao tem os arquivos de textura do cliente.
 */
final class ParrotIcon {
    private static final String[] ROWS = {
            "................",
            "................",
            ".....RRRR.......",
            "....RRRRRR......",
            "...RRRRRRRR.....",
            "...RRRWWKRRYY...",
            "...RRRWKKRRYYY..",
            "...RRRRRRRRYYKY.",
            "...RRRRRRRR.YK..",
            "...RRRRRRR......",
            "..BRRRRRRR......",
            "..BBRRRRRR......",
            "..BBBRRRRR......",
            "..BBBBRRR.......",
            "...BBBB.........",
            "................",
    };

    /** O formato que o chat de voz pede: 16x16, ARGB, indexado por [x][y]. */
    static int[][] pixels() {
        int[][] icon = new int[16][16];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                icon[x][y] = color(ROWS[y].charAt(x));
            }
        }
        return icon;
    }

    private static int color(char pixel) {
        return switch (pixel) {
            case 'R' -> 0xFFE0301E;
            case 'B' -> 0xFF2E55D8;
            case 'Y' -> 0xFFF2C94C;
            case 'W' -> 0xFFFFFFFF;
            case 'K' -> 0xFF141414;
            default -> 0x00000000;
        };
    }

    private ParrotIcon() {}
}
