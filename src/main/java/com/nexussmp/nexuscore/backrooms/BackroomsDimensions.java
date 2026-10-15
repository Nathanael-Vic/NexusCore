package com.nexussmp.nexuscore.backrooms;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * A dimensao das backrooms.
 *
 * <p>Existe por dados ({@code data/nexuscore/dimension/backrooms.json}), como a Space. E um vazio
 * plano: quem constroi os corredores e o gerador, por comando — nao a geracao de mundo.
 *
 * <p>Ela e baixa de proposito: {@code min_y 0} e {@code height 16}, uma unica secao de chunk por
 * chunk. Um andar de 700x700 sao mais de dois mil chunks; com uma secao so cada, salvar e carregar
 * isso fica barato.
 */
public final class BackroomsDimensions {
    public static final ResourceLocation BACKROOMS_ID = NexusCore.id("backrooms");
    public static final ResourceKey<Level> BACKROOMS = ResourceKey.create(Registries.DIMENSION, BACKROOMS_ID);

    /** Chao do andar. As paredes sobem dali; o teto fecha em cima. */
    public static final int FLOOR_Y = 0;
    /** Altura livre entre o chao e o teto, em blocos. */
    public static final int ROOM_HEIGHT = 4;
    public static final int CEILING_Y = FLOOR_Y + ROOM_HEIGHT + 1;

    private BackroomsDimensions() {}
}
