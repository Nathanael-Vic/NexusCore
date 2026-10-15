package com.nexussmp.nexuscore.magic.client;

import com.nexussmp.nexuscore.magic.Mages;
import com.nexussmp.nexuscore.magic.client.jei.MagicJeiPlugin;
import com.nexussmp.nexuscore.magic.network.MagicPayloads;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;
import java.util.Set;

/**
 * O que este cliente sabe sobre si: se le magia — e o que muda na tela de quem nao le.
 *
 * <p>Os nomes das magias ja saem embaralhados do proprio Iron's Spells, que pergunta ao
 * {@link Mages} (pelo mixin). Aqui fica o resto:
 *
 * <ul>
 *   <li><b>O HUD da magia.</b> Barra de mana, barra de magias e barras de lancamento nao existem
 *       para quem nao e mago. Aparecem no instante em que a pessoa vira maga, e somem quando deixa
 *       de ser.</li>
 *   <li><b>Toda linha de tooltip que fala de magia.</b> O nome de uma magia no anel de afinidade, a
 *       mana maxima e o poder de magia de uma armadura: a linha inteira vira os mesmos rabiscos das
 *       magias desconhecidas. Vale para item de qualquer mod, porque o que decide e o texto.</li>
 *   <li><b>As paginas das magias no JEI</b> ({@link MagicJeiPlugin}), quando o JEI esta instalado.</li>
 * </ul>
 */
public final class MagicClient {
    private static final String IRONS = "irons_spellbooks";
    private static final Set<String> HIDDEN_LAYERS = Set.of("mana_overlay", "spell_bar", "cast_bar", "recast_bar");
    /** Nome e descricao de magia ({@code spell.<mod>.<magia>}), e os atributos magicos do Iron's Spells. */
    private static final String SPELL_KEY = "spell.";
    private static final String MAGIC_ATTRIBUTE_KEY = "attribute." + IRONS + ".";

    /** Chega do servidor: sabe ou nao sabe magia. */
    public static void apply(MagicPayloads.Mage payload) {
        Mages.setClientSelf(payload.mage());
        refresh();
    }

    /** A roda de magias e o JEI se refazem na hora, sem esperar a pessoa trocar de item. */
    private static void refresh() {
        if (Minecraft.getInstance().player != null) {
            ClientMagicData.updateSpellSelectionManager();
        }
        if (ModList.get().isLoaded("jei")) {
            MagicJeiPlugin.refresh();
        }
    }

    static void onRenderLayer(RenderGuiLayerEvent.Pre event) {
        ResourceLocation layer = event.getName();
        if (!IRONS.equals(layer.getNamespace()) || !HIDDEN_LAYERS.contains(layer.getPath())) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && !Mages.isMage(player)) {
            event.setCanceled(true);
        }
    }

    static void onTooltip(ItemTooltipEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Mages.isMage(player)) {
            return;
        }
        List<Component> lines = event.getToolTip();
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            if (mentionsMagic(line)) {
                // O estilo vai na raiz e desce para tudo que esta dentro: a linha inteira embaralha.
                lines.set(i, Component.empty().withStyle(AbstractSpell.ELDRITCH_OBFUSCATED_STYLE).append(line));
            }
        }
    }

    /** Procura nome de magia ou atributo magico em qualquer pedaco da linha, inclusive nos argumentos. */
    private static boolean mentionsMagic(Component component) {
        if (component.getContents() instanceof TranslatableContents translatable) {
            String key = translatable.getKey();
            if (key.startsWith(SPELL_KEY) || key.startsWith(MAGIC_ATTRIBUTE_KEY)) {
                return true;
            }
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component inner && mentionsMagic(inner)) {
                    return true;
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            if (mentionsMagic(sibling)) {
                return true;
            }
        }
        return false;
    }

    /** Saiu do servidor: a proxima entrada diz de novo. */
    static void clear() {
        Mages.setClientSelf(false);
    }

    private MagicClient() {}
}
