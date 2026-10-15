package com.nexussmp.nexuscore.quest.client;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * O botao do livro de quests, ao lado do livro de receitas.
 *
 * <p>Mesmo botao do jogo (sprite de botao vanilla) com o **item livro** desenhado em cima, no mesmo
 * tamanho do livro de receitas. Ele se reposiciona a cada quadro: qualquer mod que abra um painel
 * lateral empurra o inventario, e o botao precisa ir junto.
 *
 * <p>Quando alguma coisa entra no livro e ainda nao foi lida, o canto de cima ganha um <b>selo de
 * cera</b> — cinco pixels de lacre vermelho, com contorno escuro e um brilho em cima, do mesmo jeito
 * que qualquer outra arte do jogo e desenhada. E um selo, e nao um ponto de alerta, porque o que ele
 * marca e um recado fechado: some no instante em que o livro e aberto.
 */
public final class QuestBookButton extends Button {
    /** Mesma medida do botao do livro de receitas. */
    public static final int WIDTH = 20;
    public static final int HEIGHT = 18;
    /** Deslocamento a partir da borda esquerda do inventario, ao lado do botao de receitas. */
    private static final int OFFSET_X = 126;

    // Mesma anatomia do botao do livro de receitas: contorno com cantos cortados, luz em cima e a
    // esquerda, sombra embaixo e a direita, e a face azulada quando o mouse esta em cima.
    //
    // E publico porque a fileira toda usa: receitas (do jogo), quests e habilidades de vampiro ficam
    // lado a lado ali em cima, e botao que mora na mesma fileira tem de ter a mesma moldura.
    public static final ResourceLocation BUTTON = NexusCore.id("quest/book_button");
    public static final ResourceLocation BUTTON_HIGHLIGHTED = NexusCore.id("quest/book_button_highlighted");
    private static final ItemStack BOOK = new ItemStack(Items.BOOK);

    /** O lacre: contorno quase preto, cera vermelha e um ponto de luz no alto a esquerda. */
    private static final int SEAL_EDGE = 0xFF2B0E0E;
    private static final int SEAL_WAX = 0xFFB4302E;
    private static final int SEAL_SHINE = 0xFFD9605A;
    private static final int SEAL_SIZE = 5;

    private final AbstractContainerScreen<?> screen;
    /** Guardado para so trocar a dica quando o estado muda, e nao a cada quadro. */
    private boolean unread;

    public QuestBookButton(AbstractContainerScreen<?> screen, OnPress onPress) {
        super(0, 0, WIDTH, HEIGHT, Component.translatable("nexuscore.quest.book.button"), onPress, DEFAULT_NARRATION);
        this.screen = screen;
        follow();
    }

    /** Cola o botao na posicao atual do inventario. */
    public void follow() {
        setX(screen.getGuiLeft() + OFFSET_X);
        setY(screen.height / 2 - 22);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        follow();
        graphics.blitSprite(isHoveredOrFocused() ? BUTTON_HIGHLIGHTED : BUTTON, getX(), getY(), getWidth(), getHeight());
        graphics.renderFakeItem(BOOK, getX() + 2, getY() + 1);

        boolean now = QuestClientCache.hasUnread();
        if (now != unread) {
            unread = now;
            setTooltip(Tooltip.create(Component.translatable(now
                    ? "nexuscore.quest.book.unread"
                    : "nexuscore.quest.book.button")));
        }
        if (now) {
            seal(graphics, getX() + getWidth() - SEAL_SIZE, getY() - 1);
        }
    }

    /** Um lacre de cera no canto de cima: cinco por cinco, desenhado pixel a pixel como o resto. */
    private static void seal(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + SEAL_SIZE, y + SEAL_SIZE, SEAL_EDGE);
        graphics.fill(x + 1, y + 1, x + SEAL_SIZE - 1, y + SEAL_SIZE - 1, SEAL_WAX);
        graphics.fill(x + 1, y + 1, x + 2, y + 2, SEAL_SHINE);
    }
}
