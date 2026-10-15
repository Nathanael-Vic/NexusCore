package com.nexussmp.nexuscore.relic.bag;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * A bolsa aberta: a tela de um bau duplo, com o que esta dentro do item.
 *
 * <p>O cliente desenha um bau duplo comum (e o mesmo tipo de tela); quem guarda as regras e o
 * servidor. Tres travas, todas contra perder ou duplicar coisa:
 * <ul>
 *   <li>a propria bolsa nao se mexe enquanto esta aberta — nem clique, nem tecla de numero, nem
 *       jogar fora, nem trocar de mao;</li>
 *   <li>bolsa nao entra em bolsa, nem caixa de shulker (o que o jogo ja diz que nao cabe em item
 *       com inventario, aqui tambem nao cabe);</li>
 *   <li>tudo o que muda la dentro vai na hora para o item — nao existe "salvar ao fechar" que uma
 *       queda do servidor pudesse pular.</li>
 * </ul>
 */
final class BagMenu extends ChestMenu {
    static final int ROWS = 6;
    static final int SIZE = ROWS * 9;
    /** Onde comecam as vagas da barra de atalho, depois da bolsa e das 27 do inventario. */
    private static final int HOTBAR_START = SIZE + 27;
    /** O botao de "trocar com a outra mao" (tecla F) chega como este numero. */
    private static final int OFFHAND_BUTTON = 40;

    private final ItemStack bag;
    private final InteractionHand hand;
    private final int lockedSlot;

    private BagMenu(int id, Inventory inventory, Contents contents, ItemStack bag, InteractionHand hand) {
        super(MenuType.GENERIC_9x6, id, inventory, contents, ROWS);
        this.bag = bag;
        this.hand = hand;
        this.lockedSlot = hand == InteractionHand.MAIN_HAND ? HOTBAR_START + inventory.selected : -1;
        // As vagas da bolsa trocadas por umas que recusam o que nao cabe em item com inventario.
        for (int i = 0; i < SIZE; i++) {
            Slot old = slots.get(i);
            Slot guarded = new BagSlot(contents, old.getContainerSlot(), old.x, old.y);
            guarded.index = i;
            slots.set(i, guarded);
        }
    }

    static BagMenu open(int id, Inventory inventory, ItemStack bag, InteractionHand hand) {
        return new BagMenu(id, inventory, new Contents(bag), bag, hand);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId == lockedSlot && lockedSlot >= 0) {
            return;
        }
        if (clickType == ClickType.SWAP) {
            boolean hotbarIsBag = hand == InteractionHand.MAIN_HAND && button == player.getInventory().selected;
            boolean offhandIsBag = hand == InteractionHand.OFF_HAND && button == OFFHAND_BUTTON;
            if (hotbarIsBag || offhandIsBag) {
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    /** Aberta so enquanto a mesma bolsa continua na mesma mao. */
    @Override
    public boolean stillValid(Player player) {
        return player.isAlive() && player.getItemInHand(hand) == bag;
    }

    /** As vagas de dentro: so aceitam o que pode morar dentro de um item. */
    private static final class BagSlot extends Slot {
        BagSlot(SimpleContainer container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem().canFitInsideContainerItems();
        }
    }

    /** O conteudo da bolsa: le do item ao abrir, e escreve no item a cada mudanca. */
    private static final class Contents extends SimpleContainer {
        private final ItemStack bag;

        Contents(ItemStack bag) {
            super(SIZE);
            this.bag = bag;
            ItemContainerContents stored = bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
            NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
            stored.copyInto(items);
            for (int i = 0; i < SIZE; i++) {
                setItem(i, items.get(i));
            }
        }

        @Override
        public void setChanged() {
            super.setChanged();
            bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(getItems()));
        }
    }
}
