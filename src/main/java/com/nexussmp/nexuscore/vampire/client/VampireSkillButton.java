package com.nexussmp.nexuscore.vampire.client;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.quest.client.QuestBookButton;
import de.teamlapen.vampirism.client.gui.screens.EditSelectActionScreen;
import de.teamlapen.vampirism.client.gui.screens.SelectActionRadialScreen;
import de.teamlapen.vampirism.client.gui.screens.skills.SkillsScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;

/**
 * O botao dos dentinhos no inventario: quem nao e lorde nao tem, e quem e tem no lugar certo.
 *
 * <p>Duas coisas erradas de uma vez, e a mesma correcao resolve as duas. A primeira e de regra:
 * aquele botao leva a arvore de habilidades, e habilidade e coisa de lorde — para o vampiro comum
 * ele e uma porta para uma sala vazia. A segunda e de lugar: o Vampirism poe o botao a 125 pixels
 * da borda do inventario, e o livro de quests daqui fica a 126 — os dois no mesmo ponto, um por
 * baixo do outro.
 *
 * <p>Entao o botao dele sai da tela sempre, e para o lorde entra um nosso no lugar, a 148 — o passo
 * seguinte da fileira: receitas, quests, habilidades.
 *
 * <p>A primeira tentativa foi reusar a arte do proprio Vampirism, e foi ela que ficou errada na tela:
 * aquele arquivo nao e um icone, e um <b>botao inteiro</b> desenhado no estilo do mod — cinza chapado,
 * com a moldura dele. Posto ao lado do livro de receitas e do de quests, virava o estranho da fileira.
 * Entao ele e montado como o de quests: a <b>mesma moldura</b> dos vizinhos ({@link QuestBookButton#BUTTON})
 * com o icone por cima, que aqui e a presa de vampiro — item de verdade, desenhado do mesmo jeito que
 * o livro e desenhado no botao do lado.
 *
 * <p>Achar o botao dele e feito pela arte que ele desenha, nao pela posicao: posicao o jogador
 * muda na configuracao, arte nao. Isso custa um campo lido por reflexao no <i>Minecraft</i> (nao no
 * Vampirism), resolvido uma vez. Se um dia esse campo sumir, o botao volta a ser o dele, no lugar
 * de antes — chato, e so.
 */
public final class VampireSkillButton {
    /** O livro de receitas fica em 104 e o de quests em 126: o passo da fileira e 22. */
    private static final int OFFSET_X = 148;
    private static final int WIDTH = 20;
    private static final int HEIGHT = 18;
    private static final String VAMPIRISM = "vampirism";

    /** O icone: a presa, o desenho mais curto de "vampiro" que o proprio mod ja tem pronto. */
    private static final ResourceLocation FANG = ResourceLocation.fromNamespaceAndPath(VAMPIRISM, "vampire_fang");

    private static Field spritesField;
    private static boolean spritesLookedUp;
    private static Field tooltipField;
    private static boolean tooltipLookedUp;
    /**
     * Resolvido na primeira vez que o botao aparece, e nao quando a classe carrega.
     *
     * <p>Esta classe e tocada durante a construcao do mod, quando o registro de itens ainda nem foi
     * preenchido: procurar a presa ali traria o item vazio para sempre.
     */
    private static ItemStack icon = ItemStack.EMPTY;

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        // Modulo desligado: o botao do Vampirism fica como ele pos, e a tela abre para todo mundo.
        if (!(event.getScreen() instanceof InventoryScreen inventory)
                || !CommonConfig.isModuleEnabled(ModuleIds.VAMPIRE)) {
            return;
        }
        ImageButton theirs = findVampirismButton(event.getListenersList());
        if (theirs == null) {
            // Sem botao dele: ou a pessoa nao e de faccao nenhuma, ou ele desligou o botao na
            // configuracao. Nos dois casos nao ha nada a fazer aqui.
            return;
        }
        event.removeListener(theirs);
        if (VampireClient.isLord()) {
            event.addListener(new SkillButton(inventory, theirs));
        }
    }

    /**
     * As telas do Vampirism que sao so do cliente: a arvore de habilidades e a roda de poderes.
     *
     * <p>E o outro caminho para as mesmas coisas — a tecla de atalho. Tirar o botao e deixar a
     * tecla aberta seria esconder a porta sem tranca-la. Nenhuma delas tem bau do lado do servidor,
     * entao recusar aqui nao deixa ponta solta nenhuma.
     */
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (VampireClient.isLord() || !CommonConfig.isModuleEnabled(ModuleIds.VAMPIRE)) {
            return;
        }
        Screen screen = event.getNewScreen();
        if (screen instanceof SkillsScreen
                || screen instanceof SelectActionRadialScreen<?>
                || screen instanceof EditSelectActionScreen<?>) {
            event.setCanceled(true);
        }
    }

    @Nullable
    private static ImageButton findVampirismButton(List<GuiEventListener> widgets) {
        for (GuiEventListener listener : widgets) {
            if (listener instanceof ImageButton button && isVampirism(button)) {
                return button;
            }
        }
        return null;
    }

    /** Se o botao desenha arte do Vampirism. E assim que se reconhece um botao dele. */
    static boolean isVampirism(ImageButton button) {
        ResourceLocation sprite = spriteOf(button);
        return sprite != null && VAMPIRISM.equals(sprite.getNamespace());
    }

    /** A imagem que aquele botao desenha, lida do campo do proprio Minecraft. */
    @Nullable
    static ResourceLocation spriteOf(ImageButton button) {
        Field field = spritesField();
        if (field == null) {
            return null;
        }
        try {
            return field.get(button) instanceof WidgetSprites sprites ? sprites.enabled() : null;
        } catch (IllegalAccessException error) {
            spritesField = null;
            return null;
        }
    }

    /**
     * A dica do botao original, para o substituto nao perder o que ele dizia.
     *
     * <p>O jogo deixa <i>pôr</i> uma dica num widget, mas nao deixa ler a que ja esta la. Sem isto,
     * trocar o botao apagaria o "arvore de habilidades" que aparece ao passar o mouse — e um botao
     * com icone e sem nome e pior do que um botao feio. Se o campo mudar de nome um dia, so a dica
     * se perde.
     */
    @Nullable
    static Tooltip tooltipOf(AbstractWidget widget) {
        if (!tooltipLookedUp) {
            tooltipLookedUp = true;
            try {
                tooltipField = AbstractWidget.class.getDeclaredField("tooltip");
                tooltipField.setAccessible(true);
            } catch (ReflectiveOperationException | RuntimeException error) {
                NexusCore.LOGGER.warn("[NexusCore] vampiro: as dicas dos botoes nao puderam ser copiadas", error);
            }
        }
        if (tooltipField == null) {
            return null;
        }
        try {
            return tooltipField.get(widget) instanceof Tooltip tooltip ? tooltip : null;
        } catch (IllegalAccessException error) {
            tooltipField = null;
            return null;
        }
    }

    @Nullable
    private static Field spritesField() {
        if (!spritesLookedUp) {
            spritesLookedUp = true;
            try {
                spritesField = ImageButton.class.getDeclaredField("sprites");
                spritesField.setAccessible(true);
            } catch (ReflectiveOperationException | RuntimeException error) {
                NexusCore.LOGGER.warn("[NexusCore] vampiro: o botao do Vampirism nao pode ser movido", error);
            }
        }
        return spritesField;
    }

    /**
     * O nosso botao. O clique e o dele: guardamos o botao original fora da tela so para apertar.
     *
     * <p>Ele se reposiciona a cada quadro porque o inventario anda: qualquer painel lateral (o
     * livro de receitas, o de quests, o de atributos) empurra a tela para o lado, e o botao precisa
     * ir junto.
     */
    /** A presa, procurada de uma vez so na primeira aparicao do botao. */
    static ItemStack fang() {
        if (icon.isEmpty()) {
            icon = new ItemStack(BuiltInRegistries.ITEM.get(FANG));
        }
        return icon;
    }

    private static final class SkillButton extends ImageButton {
        /** O {@code ImageButton} exige um conjunto de imagens; quem desenha aqui e o renderWidget. */
        private static final WidgetSprites EMPTY_SPRITES =
                new WidgetSprites(QuestBookButton.BUTTON, QuestBookButton.BUTTON_HIGHLIGHTED);

        private final AbstractContainerScreen<?> screen;

        private SkillButton(AbstractContainerScreen<?> screen, Button original) {
            super(0, 0, WIDTH, HEIGHT, EMPTY_SPRITES, ignored -> original.onPress(),
                    Component.translatable("nexuscore.vampire.skills_button"));
            this.screen = screen;
            setTooltip(Tooltip.create(Component.translatable("nexuscore.vampire.skills_button")));
            follow();
        }

        private void follow() {
            setX(screen.getGuiLeft() + OFFSET_X);
            setY(screen.height / 2 - 22);
        }

        /** Nao chama o {@code super}: a moldura e o icone sao desenhados aqui, como no botao vizinho. */
        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            follow();
            graphics.blitSprite(isHoveredOrFocused() ? QuestBookButton.BUTTON_HIGHLIGHTED : QuestBookButton.BUTTON,
                    getX(), getY(), getWidth(), getHeight());
            graphics.renderFakeItem(fang(), getX() + 2, getY() + 1);
        }
    }

    private VampireSkillButton() {}
}
