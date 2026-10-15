package com.nexussmp.nexuscore.parrot;

import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import com.nexussmp.nexuscore.parrot.voice.ParrotBrain;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Papagaio de ombro: um papagaio que um operador poe no ombro de alguem e que repete, de vez em
 * quando, o que os outros falaram perto dele no chat de voz.
 *
 * <p>Tres partes, cada uma no seu canto: o papagaio ({@link ShoulderParrot}) e quem o mantem no
 * ombro ({@link ParrotKeeper}); a memoria e a vontade de falar ({@code parrot.voice}); o desenho,
 * no cliente ({@code parrot.client}). A entidade e registrada sempre, em {@code NexusRegistries};
 * com o modulo desligado, os papagaios vao embora e o comando recusa. Detalhes em
 * {@code docs/PAPAGAIO.md}.
 *
 * <p>O Simple Voice Chat e opcional: sem ele, o papagaio fica no ombro e pia, so nao repete ninguem.
 */
public final class ParrotModule implements NexusModule {

    @Override
    public String id() {
        return ModuleIds.PARROT;
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener(ServerTickEvent.Post.class, event -> ParrotKeeper.tick(event.getServer()));
        // Na frente dos outros: o anel da metamorfose, por exemplo, copiaria a forma do papagaio.
        game.addListener(EventPriority.HIGHEST, PlayerInteractEvent.EntityInteract.class, ParrotKeeper::onInteract);
        game.addListener(EventPriority.HIGHEST, PlayerInteractEvent.EntityInteractSpecific.class,
                ParrotKeeper::onInteractSpecific);
        game.addListener(EventPriority.HIGHEST, AttackEntityEvent.class, ParrotKeeper::onAttack);
        game.addListener(ServerStoppedEvent.class, event -> {
            ParrotKeeper.clear();
            ParrotBrain.clear();
        });
    }
}
