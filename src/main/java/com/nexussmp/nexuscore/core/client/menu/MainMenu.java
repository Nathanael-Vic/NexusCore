package com.nexussmp.nexuscore.core.client.menu;

import com.nexussmp.nexuscore.core.config.ClientConfig;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Troca a tela de titulo do jogo pelo menu do NexusSMP ({@link NexusTitleScreen}).
 *
 * <p>So a tela de titulo exata do jogo: telas de outros mods que herdam dela ficam como estao. Com
 * {@code mainMenu.enabled = false} em {@code nexuscore-client.toml}, volta a original.
 */
public final class MainMenu {

    public static void onOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() != null && event.getNewScreen().getClass() == TitleScreen.class
                && ClientConfig.get(ClientConfig.MAIN_MENU_ENABLED)) {
            event.setNewScreen(new NexusTitleScreen());
        }
    }

    private MainMenu() {}
}
