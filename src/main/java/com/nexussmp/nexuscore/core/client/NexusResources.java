package com.nexussmp.nexuscore.core.client;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/**
 * O Nexus Resources: as texturas do SMP que sobrescrevem as de outros mods (hoje, a moldura de
 * madeira do WaterFrames). Vem dentro do jar, em {@code resourcepacks/nexus_resources}.
 *
 * <p>Fica no topo da lista e e obrigatorio: ja vem ativado e a tela de pacotes nao deixa tirar nem
 * mover. No topo porque precisa ganhar das texturas dos proprios mods, que vem no "Mod Resources".
 */
public final class NexusResources {

    public static void onAddPackFinders(AddPackFindersEvent event) {
        event.addPackFinders(NexusCore.id("resourcepacks/nexus_resources"), PackType.CLIENT_RESOURCES,
                Component.literal("Nexus Resources"), PackSource.BUILT_IN, true, Pack.Position.TOP);
    }

    private NexusResources() {}
}
