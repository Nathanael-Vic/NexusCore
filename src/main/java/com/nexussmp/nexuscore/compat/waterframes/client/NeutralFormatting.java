package com.nexussmp.nexuscore.compat.waterframes.client;

import net.minecraft.ChatFormatting;

/**
 * Tira azul/roxo (e o verde/amarelo de estado) dos textos do WaterFrames.
 * Vermelho so continua onde significa erro (decidido), e so se {@code keepErrorRed} estiver ligado.
 */
public final class NeutralFormatting {

    /** Para texto que nao e erro: cor de estado vira a cor padrao do controle. */
    public static ChatFormatting neutral(ChatFormatting original) {
        if (!WaterFramesSettings.vanillaStyle()) {
            return original;
        }
        return switch (original) {
            case AQUA, DARK_AQUA, BLUE, DARK_BLUE, LIGHT_PURPLE, DARK_PURPLE, GREEN, DARK_GREEN, RED, DARK_RED ->
                    ChatFormatting.RESET;
            case YELLOW, GOLD -> ChatFormatting.GRAY;
            default -> original;
        };
    }

    /** Para textos onde vermelho significa erro (status do video, URL invalida). */
    public static ChatFormatting errorAware(ChatFormatting original) {
        if (!WaterFramesSettings.vanillaStyle()) {
            return original;
        }
        if (original == ChatFormatting.RED || original == ChatFormatting.DARK_RED) {
            return WaterFramesSettings.keepErrorRed() ? original : ChatFormatting.GRAY;
        }
        return neutral(original);
    }

    private NeutralFormatting() {}
}
