package com.nexussmp.nexuscore.vampire;

import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.vampirism.api.event.ActionEvent;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import de.teamlapen.vampirism.blocks.MedChairBlock;
import de.teamlapen.vampirism.inventory.RevertBackMenu;
import de.teamlapen.vampirism.inventory.VampirismMenu;
import de.teamlapen.vampirism.items.InjectionItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * O cerco: o que o Vampirism pode e o que nao pode fazer neste servidor.
 *
 * <p>Sao cinco regras, e todas passam pelos eventos que o proprio Vampirism publica — nenhuma
 * mexe no mod por dentro:
 *
 * <ol>
 *   <li><b>Cacador nao existe.</b> Nem entrar na faccao, nem subir nela. Nao e "desencorajado": o
 *       pedido e negado na origem, entao nenhum caminho (treinador, altar, comando de outro mod)
 *       leva alguem para la.</li>
 *   <li><b>So lorde progride.</b> Quem nao esta na lista para no nivel 1 — vampiro que queima no
 *       sol e precisa de sangue, e nada alem disso. Sem nivel, nao ha ponto de habilidade; sem
 *       ponto, nao ha poder.</li>
 *   <li><b>E nem por outro caminho.</b> Se alguma habilidade sobrar de antes, usar tambem nao
 *       da: quem nao e lorde nao aciona poder nenhum, venha o comando de onde vier.</li>
 *   <li><b>Nada nasce sozinho.</b> Vampiro, cacador, fantasma e o resto dos mobs do Vampirism somem
 *       do nascimento natural, dos geradores, das estruturas e do totem de vila. O que um operador
 *       puser continua funcionando, e a criatura convertida pela mordida de um lorde tambem — essa e
 *       a graca. O que o mundo ja tinha de antes e cuidado por {@link VampireWorld}.</li>
 *   <li><b>A agulha nao decide nada.</b> A cadeira de injecao do Vampirism e uma porta de mao
 *       dupla: com seringa de alho ela devolve a pessoa a condicao humana, e com seringa de
 *       sanguinare ela transforma. Nenhuma das duas cabe aqui — entrar no vampirismo e mordida de
 *       lorde, sair e comando, e as duas coisas sao decididas por gente, nao por item. A seringa
 *       para de funcionar na cadeira, e a tela de reverter nao abre nem por outro caminho.</li>
 * </ol>
 */
public final class VampireRules {
    /** Entre um aviso e outro para a mesma pessoa. Subir de nivel e tentado toda hora. */
    private static final long NOTICE_INTERVAL_MS = 60_000L;

    /**
     * De onde uma criatura <b>nao</b> pode vir.
     *
     * <p>A lista e do que e proibido, e nao do que e permitido, para que um operador com ovo de
     * spawn, um comando ou uma conversao continuem funcionando sem precisar ser previstos aqui.
     *
     * <p>{@code EVENT} e o do totem de vila: e por ele que a vila "dos vampiros" ou "dos cacadores"
     * enche de mob de faccao. Nada mais do Vampirism nasce por esse caminho.
     */
    private static final Set<MobSpawnType> FORBIDDEN = Set.of(
            MobSpawnType.NATURAL, MobSpawnType.CHUNK_GENERATION, MobSpawnType.SPAWNER,
            MobSpawnType.STRUCTURE, MobSpawnType.PATROL, MobSpawnType.JOCKEY,
            MobSpawnType.REINFORCEMENT, MobSpawnType.EVENT);

    private static final Map<UUID, Long> lastNotice = new HashMap<>();

    public static void register(IEventBus game) {
        // HIGHEST para o "nao" sair antes de qualquer outra coisa reagir ao pedido.
        game.addListener(EventPriority.HIGHEST, PlayerFactionEvent.CanJoinFaction.class, VampireRules::canJoin);
        game.addListener(EventPriority.HIGHEST, false, PlayerFactionEvent.FactionLevelChangePre.class,
                VampireRules::levelChange);
        game.addListener(EventPriority.HIGHEST, false, ActionEvent.ActionActivatedEvent.class,
                VampireRules::action);
        game.addListener(FinalizeSpawnEvent.class, VampireRules::spawn);
        game.addListener(PlayerContainerEvent.Open.class, VampireRules::menu);
        game.addListener(PlayerInteractEvent.RightClickBlock.class, VampireRules::injection);
    }

    private static boolean off() {
        return !CommonConfig.isModuleEnabled(ModuleIds.VAMPIRE);
    }

    // ---- 1. cacador nao existe ----

    private static void canJoin(PlayerFactionEvent.CanJoinFaction event) {
        if (!off() && isHunter(event.getFactionToJoin())) {
            event.setBehavior(PlayerFactionEvent.CanJoinFaction.Behavior.DENY);
        }
    }

    // ---- 2. so lorde progride ----

    private static void levelChange(PlayerFactionEvent.FactionLevelChangePre event) {
        if (off()) {
            return;
        }
        IPlayableFaction<?> target = event.getNewFaction();
        if (target == null) {
            // Saindo da faccao: sempre pode. E o caminho da cura.
            return;
        }
        if (isHunter(target)) {
            event.setCanceled(true);
            notice(event.getPlayer().asEntity(), "nexuscore.vampire.no_hunters");
            return;
        }
        if (event.getNewLevel() <= Vampires.CAPPED_LEVEL) {
            return;
        }
        Player player = event.getPlayer().asEntity();
        if (player instanceof ServerPlayer server && !Vampires.isLord(server.server, server)) {
            event.setCanceled(true);
            notice(player, "nexuscore.vampire.capped");
        }
    }

    // ---- 3. e nem por outro caminho ----

    private static void action(ActionEvent.ActionActivatedEvent event) {
        if (off()) {
            return;
        }
        Player player = event.getFactionPlayer().asEntity();
        if (player instanceof ServerPlayer server && !Vampires.isLord(server.server, server)) {
            event.setCanceled(true);
        }
    }

    /**
     * Duas telas do Vampirism que este servidor nao deixa abrir.
     *
     * <p>A de <b>progressao</b>, para quem nao e lorde: nao ha nivel nem habilidade para gastar ali.
     * A de <b>reverter</b>, para todo mundo, inclusive lorde: a cura e so por comando.
     *
     * <p>Fechar do lado do servidor e o unico jeito honesto: recusar so no cliente deixaria a pessoa
     * com um bau aberto que ela nao ve. E uma rede de seguranca — o caminho normal ate a tela de
     * reverter ja morre antes, na seringa.
     */
    private static void menu(PlayerContainerEvent.Open event) {
        boolean revert = event.getContainer() instanceof RevertBackMenu;
        if ((!revert && !(event.getContainer() instanceof VampirismMenu)) || off()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (revert) {
            player.closeContainer();
            tell(player, "nexuscore.vampire.no_cure");
        } else if (!Vampires.isLord(player.server, player)) {
            player.closeContainer();
        }
    }

    /**
     * A seringa nao faz efeito na cadeira de injecao.
     *
     * <p>O corte e no <b>gesto</b>, e nao na tela que viria depois, por um motivo pratico: a cadeira
     * consome a seringa antes de abrir qualquer coisa. Recusar so la na frente devolveria a pessoa
     * com a mesma condicao de antes e um item a menos — uma punicao por ter tentado.
     *
     * <p>Vale para todas as seringas, nos dois sentidos: a de alho, que curava, e a de sanguinare,
     * que transformava. As duas contornavam a regra da casa — quem entra e quem sai do vampirismo e
     * decidido por gente, nao por bancada de crafting.
     *
     * <p>So na cadeira: com a seringa na mao, o resto do mundo continua respondendo normal — bau,
     * porta, bancada. Recusar o clique em qualquer bloco deixava a pessoa sem abrir nada ate guardar
     * a seringa.
     */
    private static void injection(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getItemStack().getItem() instanceof InjectionItem) || off()
                || !(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof MedChairBlock)) {
            return;
        }
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player) {
            tell(player, "nexuscore.vampire.no_needle");
        }
    }

    /** Recusa curta, na barra de acao: e resposta a um gesto, nao conversa de chat. */
    private static void tell(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY), true);
    }

    // ---- 4. nada nasce sozinho ----

    /**
     * Vale para todo mob do lado "jogo" do Vampirism ({@link VampireWorld#isWildMob}): os de faccao
     * e tambem os que nao tem faccao, como o fantasma da floresta. A criatura convertida pela mordida
     * de um lorde e o minion dele ficam de fora — sao do jogador.
     */
    private static void spawn(FinalizeSpawnEvent event) {
        if (FORBIDDEN.contains(event.getSpawnType()) && VampireWorld.isWildMob(event.getEntity()) && !off()) {
            event.setSpawnCancelled(true);
        }
    }

    // ---- avisos ----

    private static boolean isHunter(@Nullable IPlayableFaction<?> faction) {
        return faction != null && VReference.HUNTER_FACTION_ID.equals(faction.getID());
    }

    /**
     * Uma frase explicando o "nao" — no maximo uma por minuto.
     *
     * <p>Tentar subir de nivel acontece a cada gole de sangue depois que a barra enche, entao sem
     * o intervalo isso viraria uma parede de texto no chat de quem ja entendeu.
     */
    private static void notice(@Nullable Player player, String key) {
        if (!(player instanceof ServerPlayer server)) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = lastNotice.get(server.getUUID());
        if (last != null && now - last < NOTICE_INTERVAL_MS) {
            return;
        }
        lastNotice.put(server.getUUID(), now);
        server.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }

    /** Saiu do servidor: o intervalo do aviso recomeca na volta. */
    public static void forget(UUID player) {
        lastNotice.remove(player);
    }

    /** Servidor parando: a memoria dos avisos nao precisa sobreviver a nada. */
    public static void clear() {
        lastNotice.clear();
    }

    private VampireRules() {}
}
