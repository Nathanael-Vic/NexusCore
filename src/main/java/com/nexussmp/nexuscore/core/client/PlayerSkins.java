package com.nexussmp.nexuscore.core.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A skin de um jogador, para desenhar coisas que nao sao ele: hologramas e o skinwalker.
 *
 * <p>A ordem importa e e sempre a mesma:
 *
 * <ol>
 *   <li><b>A pessoa esta online</b> — o cliente ja tem a skin dela, e ela e a mais atual. Nada e
 *       guardado aqui nesse caso: quem troca de skin no meio da sessao passa a aparecer trocado, em
 *       vez de congelado na skin de quando foi visto pela primeira vez.
 *   <li><b>O perfil veio com textura</b> — e o caso normal do que o servidor manda. Resolve na hora,
 *       sem perguntar nada a ninguem: e assim que funciona em servidor offline ou com mod de skin
 *       (SkinRestorer e parecidos), onde perguntar a Mojang traria a skin errada ou nenhuma.
 *   <li><b>Sobrou so o id</b> — ai sim busca uma vez e guarda. Ate a busca voltar, a skin padrao;
 *       desenhar nunca espera rede.
 * </ol>
 */
public final class PlayerSkins {
    private static final Map<UUID, PlayerSkin> CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> REQUESTED = new ConcurrentHashMap<>();

    /** A skin do perfil que o servidor mandou. */
    public static PlayerSkin of(@Nullable GameProfile profile, UUID fallback) {
        if (profile == null || profile.getId() == null) {
            return DefaultPlayerSkin.get(fallback);
        }
        UUID id = profile.getId();

        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        PlayerInfo online = connection == null ? null : connection.getPlayerInfo(id);
        if (online != null) {
            return online.getSkin();
        }

        PlayerSkin cached = CACHE.get(id);
        if (cached != null) {
            return cached;
        }
        if (REQUESTED.putIfAbsent(id, Boolean.TRUE) == null) {
            Minecraft.getInstance().getSkinManager().getOrLoad(profile)
                    .thenAccept(skin -> CACHE.put(id, skin));
        }
        return DefaultPlayerSkin.get(id);
    }

    /** A skin de quem so se conhece por id e nome. */
    public static PlayerSkin of(@Nullable UUID player, String name, UUID fallback) {
        return player == null
                ? DefaultPlayerSkin.get(fallback)
                : of(new GameProfile(player, name), fallback);
    }

    public static void clear() {
        CACHE.clear();
        REQUESTED.clear();
    }

    private PlayerSkins() {}
}
