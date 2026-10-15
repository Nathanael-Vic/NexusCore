package com.nexussmp.nexuscore.parrot;

import net.minecraft.world.entity.animal.Parrot;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * As cinco penas que o jogo tem, com o nome que se digita no comando.
 *
 * <p>A cor de cada uma tambem pinta as particulas de quando o papagaio chega e vai embora: um
 * papagaio verde chega numa nuvem verde.
 */
public enum ParrotColor {
    VERMELHO("vermelho", Parrot.Variant.RED_BLUE, 0xE0301E),
    AZUL("azul", Parrot.Variant.BLUE, 0x2E55D8),
    VERDE("verde", Parrot.Variant.GREEN, 0x6CC51C),
    CIANO("ciano", Parrot.Variant.YELLOW_BLUE, 0x22C6E0),
    CINZA("cinza", Parrot.Variant.GRAY, 0xB8B8B8);

    private final String id;
    private final Parrot.Variant variant;
    private final int rgb;

    ParrotColor(String id, Parrot.Variant variant, int rgb) {
        this.id = id;
        this.variant = variant;
        this.rgb = rgb;
    }

    public String id() {
        return id;
    }

    public Parrot.Variant variant() {
        return variant;
    }

    public Vector3f particleColor() {
        return new Vector3f(((rgb >> 16) & 0xFF) / 255.0F, ((rgb >> 8) & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F);
    }

    @Nullable
    public static ParrotColor byId(String id) {
        for (ParrotColor color : values()) {
            if (color.id.equals(id)) {
                return color;
            }
        }
        return null;
    }

    public static ParrotColor of(Parrot.Variant variant) {
        for (ParrotColor color : values()) {
            if (color.variant == variant) {
                return color;
            }
        }
        return VERMELHO;
    }
}
