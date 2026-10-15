package com.nexussmp.nexuscore.display;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** O que a tela do painel mostra. O OP escolhe a pagina inicial e quais ficam habilitadas. */
public enum PanelPage {
    SUMMARY,
    BIOMES,
    STRUCTURES,
    RECENT;

    private static final PanelPage[] VALUES = values();
    public static final int ALL_MASK = (1 << VALUES.length) - 1;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public int bit() {
        return 1 << ordinal();
    }

    public Component displayName() {
        return Component.translatable("nexuscore.panel.page." + id());
    }

    public static PanelPage[] all() {
        return VALUES;
    }

    /** {@code null} quando o texto nao e uma pagina conhecida. */
    @Nullable
    public static PanelPage byId(String id) {
        for (PanelPage page : VALUES) {
            if (page.id().equals(id)) {
                return page;
            }
        }
        return null;
    }

    public static PanelPage byIdOrSummary(String id) {
        PanelPage page = byId(id);
        return page == null ? SUMMARY : page;
    }

    /** Proxima pagina habilitada na mascara, andando em circulo. */
    public static PanelPage next(PanelPage from, int mask, int direction) {
        int safeMask = mask == 0 ? ALL_MASK : mask;
        int index = from.ordinal();
        for (int step = 0; step < VALUES.length; step++) {
            index = Math.floorMod(index + direction, VALUES.length);
            if ((safeMask & VALUES[index].bit()) != 0) {
                return VALUES[index];
            }
        }
        return from;
    }

    /** Quantas paginas a mascara habilita (nunca zero). */
    public static int count(int mask) {
        int safeMask = mask == 0 ? ALL_MASK : mask;
        return Integer.bitCount(safeMask);
    }

    /** Posicao da pagina entre as habilitadas, para o indicador de bolinhas. */
    public static int indexIn(PanelPage page, int mask) {
        int safeMask = mask == 0 ? ALL_MASK : mask;
        int index = 0;
        for (PanelPage other : VALUES) {
            if (other == page) {
                return index;
            }
            if ((safeMask & other.bit()) != 0) {
                index++;
            }
        }
        return 0;
    }
}
