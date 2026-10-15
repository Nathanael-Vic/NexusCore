package com.nexussmp.nexuscore.hologram.client.render;

import com.nexussmp.nexuscore.core.client.PlayerSkins;
import com.nexussmp.nexuscore.hologram.entity.HologramEntity;
import net.minecraft.client.resources.PlayerSkin;

/** A skin do holograma e a skin de quem gravou. O trabalho de achar e do {@link PlayerSkins}. */
public final class HologramSkins {

    public static PlayerSkin skin(HologramEntity hologram) {
        return PlayerSkins.of(hologram.authorProfile(), hologram.getUUID());
    }

    private HologramSkins() {}
}
