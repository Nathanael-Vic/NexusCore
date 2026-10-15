package com.nexussmp.nexuscore.compat.waterframes;

import com.nexussmp.nexuscore.compat.waterframes.client.GrayscaleAtlasReloadListener;
import com.nexussmp.nexuscore.compat.waterframes.client.WaterFramesSettings;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import com.nexussmp.nexuscore.core.util.CompatRequirements;
import com.nexussmp.nexuscore.core.util.ModRequirement;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

import java.util.List;

/**
 * Interface do WaterFrames no estilo das telas do Minecraft.
 *
 * <p>Nada de reescrever telas: os mixins em {@code mixin.waterframes} trocam so o que e desenhado
 * (estilo, sprites, cores de texto) e mantem layout, ordem e comportamento do WaterFrames.
 * Esta classe nao pode referenciar tipos do CreativeCore: ela e instanciada mesmo sem o mod.
 */
public final class WaterFramesCompatModule implements NexusModule {

    @Override
    public String id() {
        return ModuleIds.COMPAT_WATERFRAMES;
    }

    @Override
    public List<ModRequirement> requirements() {
        return List.of(CompatRequirements.WATERFRAMES, CompatRequirements.CREATIVECORE);
    }

    @Override
    public boolean clientOnly() {
        return true;
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        modBus.addListener(ModConfigEvent.Loading.class, event -> WaterFramesSettings.onConfigChanged(event.getConfig()));
        modBus.addListener(ModConfigEvent.Reloading.class, event -> WaterFramesSettings.onConfigChanged(event.getConfig()));
        modBus.addListener(RegisterClientReloadListenersEvent.class,
                event -> event.registerReloadListener(new GrayscaleAtlasReloadListener()));
    }
}
