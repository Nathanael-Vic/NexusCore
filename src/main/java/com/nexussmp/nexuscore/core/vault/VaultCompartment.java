package com.nexussmp.nexuscore.core.vault;

import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Um pedaco de inventario que nao e o inventario do jogo.
 *
 * <p>O {@link PlayerVault} sabe guardar as 36 vagas de sempre e mais nada. Mods de acessorio
 * (Curios, por exemplo) penduram outras vagas no jogador — colar, anel, capa —, e quem entra nas
 * backrooms tem de perder essas tambem, senao "voce entra sem nada" vira "sem quase nada", e quem
 * volta tem de encontrar tudo no lugar.
 *
 * <p>Cada compartimento se identifica por um {@link #id()} curto, que vira a chave dentro do
 * arquivo guardado. O nome e para sempre: mudar o id depois faz o que ja esta guardado nao ser
 * reconhecido na volta.
 */
public interface VaultCompartment {
    /** Nome curto e estavel deste compartimento ({@code curios}...). */
    String id();

    /**
     * Guarda o conteudo e deixa as vagas vazias, como o inventario principal.
     *
     * @return o que foi guardado, ou {@code null} se nao havia nada a guardar.
     */
    @Nullable
    Tag stash(ServerPlayer player);

    /** Devolve o que {@link #stash(ServerPlayer)} tinha guardado. */
    void restore(ServerPlayer player, Tag saved);
}
