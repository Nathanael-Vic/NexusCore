package com.nexussmp.nexuscore.core.client.menu;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.core.config.ClientConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommonButtons;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * O menu principal do NexusSMP, no lugar da tela de titulo do jogo.
 *
 * <p>Por enquanto so o essencial, sobre um fundo preto: entrar direto no servidor (com o status ao
 * vivo embaixo: online, quantos jogadores, a latencia), um mundo de teste, opcoes e sair — com os
 * botoes e os icones de idioma e acessibilidade do proprio jogo. O fundo e a arte vem depois.
 *
 * <p>Abrir pela primeira vez na sessao tem uma entrada suave: os botoes aparecem aos poucos.
 */
public final class NexusTitleScreen extends Screen {
    private static final int WIDE = 204;
    private static final int HALF = 100;
    private static final float FADE_MS = 1000.0F;
    private static final long REPING_MS = 20_000L;

    /** Uma thread so, de fundo, para perguntar ao servidor como ele esta — como a lista de servidores faz. */
    private static final ExecutorService PING_THREAD = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "NexusCore menu ping");
        thread.setDaemon(true);
        return thread;
    });

    /** A entrada so acontece a primeira vez que o menu abre na sessao, como no jogo. */
    private static boolean firstOpen = true;

    private final boolean fading;
    private long openedAt;
    private final ServerStatusPinger pinger = new ServerStatusPinger();
    @Nullable
    private ServerData server;
    private String address = "";
    private long nextPing;

    public NexusTitleScreen() {
        super(Component.translatable("nexuscore.menu.title"));
        this.fading = firstOpen;
        firstOpen = false;
    }

    @Override
    protected void init() {
        address = ClientConfig.get(ClientConfig.MAIN_MENU_SERVER_ADDRESS).strip();
        int center = width / 2;
        int top = height / 4 + 48;

        Button join = addRenderableWidget(Button.builder(Component.translatable("nexuscore.menu.join"), button -> join())
                .bounds(center - WIDE / 2, top, WIDE, 20).build());
        boolean valid = !address.isEmpty() && ServerAddress.isValidAddress(address);
        join.active = valid;
        if (!valid) {
            join.setTooltip(Tooltip.create(Component.translatable("nexuscore.menu.no_address")));
        }

        addRenderableWidget(Button.builder(Component.translatable("nexuscore.menu.test_world"),
                        button -> minecraft.setScreen(new SelectWorldScreen(this)))
                .bounds(center - WIDE / 2, top + 36, WIDE, 20).build());

        int row = top + 72;
        addRenderableWidget(Button.builder(Component.translatable("menu.options"),
                        button -> minecraft.setScreen(new OptionsScreen(this, minecraft.options)))
                .bounds(center - WIDE / 2, row, HALF, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("menu.quit"), button -> minecraft.stop())
                .bounds(center + WIDE / 2 - HALF, row, HALF, 20).build());

        // Os dois icones do jogo, nos mesmos lugares de sempre: idioma a esquerda, acessibilidade a direita.
        addRenderableWidget(CommonButtons.language(20,
                        button -> minecraft.setScreen(new LanguageSelectScreen(this, minecraft.options, minecraft.getLanguageManager())), true))
                .setPosition(center - WIDE / 2 - 24, row);
        addRenderableWidget(CommonButtons.accessibility(20,
                        button -> minecraft.setScreen(new AccessibilityOptionsScreen(this, minecraft.options)), true))
                .setPosition(center + WIDE / 2 + 4, row);

        Component credits = Component.translatable("title.credits");
        int creditsWidth = font.width(credits);
        addRenderableWidget(new PlainTextButton(width - creditsWidth - 2, height - 10, creditsWidth, 10, credits,
                button -> minecraft.setScreen(new CreditsAndAttributionScreen(this)), font));

        if (valid && server == null) {
            server = new ServerData("NexusSMP", address, ServerData.Type.OTHER);
            ping();
        }
    }

    // ---- o servidor ----

    private void join() {
        if (server == null) {
            return;
        }
        ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(address), server, false, null);
    }

    /** Pergunta ao servidor como ele esta, numa thread de fundo; a resposta chega sozinha no {@code server}. */
    private void ping() {
        ServerData data = server;
        if (data == null || data.state() == ServerData.State.PINGING) {
            return;
        }
        data.setState(ServerData.State.PINGING);
        nextPing = Util.getMillis() + REPING_MS;
        PING_THREAD.submit(() -> {
            try {
                pinger.pingServer(data, () -> { }, () -> data.setState(
                        data.protocol == SharedConstants.getCurrentVersion().getProtocolVersion()
                                ? ServerData.State.SUCCESSFUL : ServerData.State.INCOMPATIBLE));
            } catch (UnknownHostException unknown) {
                data.setState(ServerData.State.UNREACHABLE);
            } catch (Exception failure) {
                data.setState(ServerData.State.UNREACHABLE);
            }
        });
    }

    @Override
    public void tick() {
        pinger.tick();
        if (server != null && Util.getMillis() > nextPing) {
            ping();
        }
    }

    @Override
    public void removed() {
        pinger.removeAll();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    // ---- desenho ----

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (openedAt == 0L) {
            openedAt = Util.getMillis();
        }
        float uiAlpha = fading ? Mth.clamp((Util.getMillis() - openedAt) / FADE_MS, 0.0F, 1.0F) : 1.0F;
        for (GuiEventListener child : children()) {
            if (child instanceof AbstractWidget widget) {
                widget.setAlpha(uiAlpha);
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        if (uiAlpha <= 0.02F) {
            return;
        }
        int alpha = Mth.ceil(uiAlpha * 255.0F) << 24;
        status(graphics, alpha);
        Component version = Component.translatable("nexuscore.menu.version", modVersion(), SharedConstants.getCurrentVersion().getName());
        graphics.drawString(font, version, 2, height - 10, 0x9A96A8 | alpha);
    }

    /** Fundo preto, por enquanto. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF000000);
    }

    /** A linha embaixo do "Entrar": o servidor esta de pe? quantos jogadores? */
    private void status(GuiGraphics graphics, int alpha) {
        int y = height / 4 + 48 + 24;
        int dotColor;
        Component text;
        if (server == null) {
            dotColor = 0x6A6680;
            text = Component.translatable("nexuscore.menu.status.no_address");
        } else {
            switch (server.state()) {
                case SUCCESSFUL -> {
                    dotColor = 0x55E07A;
                    text = server.players != null
                            ? Component.translatable("nexuscore.menu.status.online", server.players.online(), server.players.max(), server.ping)
                            : Component.translatable("nexuscore.menu.status.online_simple");
                }
                case INCOMPATIBLE -> {
                    dotColor = 0xFFAA33;
                    text = Component.translatable("nexuscore.menu.status.incompatible");
                }
                case UNREACHABLE -> {
                    dotColor = 0xE0485A;
                    text = Component.translatable("nexuscore.menu.status.offline");
                }
                default -> {
                    // Procurando: o ponto pisca devagar enquanto espera a resposta.
                    dotColor = (Util.getMillis() / 500L) % 2 == 0 ? 0xF2D45C : 0x8A7A30;
                    text = Component.translatable("nexuscore.menu.status.searching");
                }
            }
        }
        Component line = Component.literal("● ").withColor(dotColor).append(text.copy().withStyle(ChatFormatting.GRAY));
        graphics.drawCenteredString(font, line, width / 2, y, 0xFFFFFF | alpha);
    }

    private static String modVersion() {
        return ModList.get().getModContainerById(NexusCore.MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("?");
    }
}
