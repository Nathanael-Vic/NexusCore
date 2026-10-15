/*
 * Parte do NexusCore.
 * Derivado de Xander's Sodium Options: Continued 3.4.2
 * (isXander, IMB11, soradotwav) - https://github.com/soradotwav/xanders-sodium-options
 * Licenciado sob a GNU Lesser General Public License v3.0 ou posterior (LGPL-3.0-or-later).
 * Alteracoes: portado para Minecraft 1.21.1 / NeoForge e reorganizado nos pacotes do NexusCore.
 */
package com.nexussmp.nexuscore.compat.sodium;

import dev.isxander.yacl3.api.Binding;
import net.caffeinemc.mods.sodium.client.config.ConfigManager;
import net.caffeinemc.mods.sodium.client.config.structure.StatefulOption;

public class SodiumBinding<T> implements Binding<T> {
    private final StatefulOption<T> sodiumOption;

    public SodiumBinding(StatefulOption<T> sodiumOption) {
        this.sodiumOption = sodiumOption;
    }

    @Override
    public void setValue(T value) {
        sodiumOption.modifyValue(value);
    }

    @Override
    public T getValue() {
        return sodiumOption.getValidatedValue();
    }

    @Override
    public T defaultValue() {
        return sodiumOption.getDefaultValue().get(ConfigManager.CONFIG);
    }
}
