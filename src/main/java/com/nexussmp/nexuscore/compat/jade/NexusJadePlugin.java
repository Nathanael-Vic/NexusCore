package com.nexussmp.nexuscore.compat.jade;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.backrooms.entity.SkinwalkerEntity;
import com.nexussmp.nexuscore.compat.envelope.client.BatCourierClient;
import com.nexussmp.nexuscore.morph.MorphForm;
import com.nexussmp.nexuscore.morph.MorphState;
import com.nexussmp.nexuscore.parrot.ShoulderParrot;
import com.nexussmp.nexuscore.relic.astral.AstralBody;
import com.nexussmp.nexuscore.relic.stone.UnyieldingStoneBlock;
import com.nexussmp.nexuscore.relic.stone.UnyieldingStoneBlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.JadeIds;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.overlay.RayTracing;

import java.util.function.Predicate;

/**
 * O que o Jade mostra das coisas do NexusCore.
 *
 * <p><b>O skinwalker nao diz de que mod ele e.</b> O Jade fecha toda plaquinha com o nome do mod de
 * onde a coisa veio. Para um bloco isso e util; para <b>aquilo</b> e o fim do susto: a pessoa passa
 * o mouse no vulto com a cara de um amigo e le "NexusCore" — pronto, virou uma entidade de mod, nao
 * mais alguem estranho no corredor. O nome e a barra de vida continuam; so a assinatura sai.
 *
 * <p><b>O papagaio de ombro mostra so o nome.</b> O nome que o operador deu no comando ja aparece
 * sozinho (e o nome da entidade). Sai a barra de vida — ele nao toma dano, seria uma barra que nunca
 * mexe — e sai o "NexusCore", para ele ser so um papagaio como outro qualquer.
 *
 * <p><b>Quem esta transformado mostra o nome da forma.</b> Virou morcego no anel da metamorfose, a
 * plaquinha diz "Morcego"; copiou outro jogador, diz o nome dele. Sem isto o disfarce caia no
 * primeiro mouse que passasse por cima: o Jade le o nome da entidade de verdade, que e a pessoa.
 *
 * <p><b>O morcego carteiro do lorde vampiro diz "Morcego"</b>, e nao "Pombo" (ver
 * {@link #isBatCourier}).
 *
 * <p>Tudo isso vale <b>mesmo com a opcao ligada</b> nas configuracoes do Jade, de proposito: nao
 * adianta o dono do servidor desligar no cliente dele se cada pessoa que entrar pode ligar de novo.
 *
 * <p>Sem o Jade instalado nada disto existe — quem carrega esta classe e o proprio Jade, quando
 * varre os mods atras da anotacao aqui em cima.
 */
@WailaPlugin
public final class NexusJadePlugin implements IWailaPlugin {
    private static final String ENVELOPE = "envelope";
    private static final String MINECRAFT = "Minecraft";

    /** O lado do servidor: o dono da pedra inabalavel, so para admin ({@link StoneOwnerProvider}). */
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(StoneOwnerProvider.INSTANCE, UnyieldingStoneBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        hideOwnParrot(registration);
        registration.registerBlockComponent(StoneOwnerProvider.INSTANCE, UnyieldingStoneBlock.class);

        // Depois de a plaquinha estar montada: e o unico ponto em que as linhas ja existem e ainda da
        // para tirar ou trocar. Cada linha carrega a marca de quem a colocou, entao mexe-se so nela.
        registration.addTooltipCollectedCallback((root, accessor) -> {
            if (!(accessor instanceof EntityAccessor entity)) {
                return;
            }
            ITooltip tooltip = root.getTooltip();
            if (entity.getEntity() instanceof SkinwalkerEntity) {
                tooltip.remove(JadeIds.CORE_MOD_NAME);
            } else if (entity.getEntity() instanceof ShoulderParrot) {
                tooltip.remove(JadeIds.CORE_MOD_NAME);
                tooltip.remove(JadeIds.MC_ENTITY_HEALTH);
            } else if (entity.getEntity() instanceof AstralBody body) {
                // O corpo deixado pela mascara astral passa pela propria pessoa: so o nome dela.
                if (body.getCustomName() != null) {
                    tooltip.replace(JadeIds.CORE_OBJECT_NAME, IThemeHelper.get().title(body.getCustomName()));
                }
                tooltip.replace(JadeIds.CORE_MOD_NAME, IThemeHelper.get().modName(MINECRAFT));
                tooltip.remove(JadeIds.MC_ENTITY_HEALTH);
            } else if (entity.getEntity() instanceof Player player) {
                MorphForm form = MorphState.form(player);
                if (form != null) {
                    tooltip.replace(JadeIds.CORE_OBJECT_NAME, IThemeHelper.get().title(form.displayName()));
                }
            } else if (isBatCourier(entity.getEntity())) {
                Entity pigeon = entity.getEntity();
                IThemeHelper theme = IThemeHelper.get();
                tooltip.replace(JadeIds.CORE_OBJECT_NAME, theme.title(pigeon.hasCustomName()
                        ? pigeon.getCustomName() : EntityType.BAT.getDescription()));
                tooltip.replace(JadeIds.CORE_MOD_NAME, theme.modName(MINECRAFT));
            }
        });
    }

    /**
     * O pombo carteiro de um lorde vampiro e desenhado como morcego (no {@code BatCourierClient}),
     * mas continua sendo um pombo do Envelope — e o Jade le o nome da entidade de verdade. Aqui a
     * plaquinha passa a dizer "Morcego", e o mod embaixo "Minecraft", que e de onde o morcego vem.
     *
     * <p>A pergunta so vai para o lado do Envelope com ele instalado: a classe de la conhece o pombo,
     * e sem o mod ela nem poderia ser carregada.
     */
    private static boolean isBatCourier(Entity entity) {
        return ModList.get().isLoaded(ENVELOPE) && BatCourierClient.isBat(entity);
    }

    /**
     * Quem carrega o papagaio nao ve a plaquinha dele: olhando um pouco para o lado em primeira
     * pessoa, a mira do Jade batia no papagaio grudado no ombro. Para os outros, nada muda.
     *
     * <p>O Jade nao pergunta ao papagaio se ele pode ser mirado, entao ele sai do filtro de entidades
     * do proprio Jade: a mira passa direto, e o que estiver atras (um bloco, outro bicho) aparece no
     * lugar. Esse filtro nao e parte da API publica do Jade; se uma versao futura mudar, fica a rede
     * de baixo — a plaquinha do proprio papagaio e descartada, e nao aparece nada.
     */
    private static void hideOwnParrot(IWailaClientRegistration registration) {
        try {
            Predicate<Entity> previous = RayTracing.ENTITY_FILTER;
            RayTracing.ENTITY_FILTER = entity -> !isOwnParrot(entity) && previous.test(entity);
        } catch (LinkageError changed) {
            NexusCore.LOGGER.warn("[NexusCore] Jade: filtro de mira mudou; o papagaio do dono so e escondido, sem mostrar o que esta atras");
        }
        registration.addRayTraceCallback((hit, accessor, original) ->
                accessor instanceof EntityAccessor entity && isOwnParrot(entity.getEntity()) ? null : accessor);
    }

    private static boolean isOwnParrot(Entity entity) {
        return entity instanceof ShoulderParrot parrot && parrot.isOnLocalShoulder();
    }
}
