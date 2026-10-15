package com.nexussmp.nexuscore.quest.client;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * O Apothic Attributes (o botao de espada no inventario) abre um painel do mesmo lado que o nosso.
 *
 * <p>Os dois nao cabem juntos, entao quem abre fecha o outro. Aqui so perguntamos se o painel dele
 * esta aberto e, quando preciso, pedimos para ele fechar — por reflexao, em um campo publico e um
 * metodo publico. Sem o mod instalado, nada disso existe e tudo continua funcionando.
 */
final class AttributesPanelCompat {
    private static final String CLASS = "dev.shadowsoffire.apothic_attributes.client.AttributesGui";

    private static boolean checked;
    private static Class<?> guiClass;
    /** {@code wasOpen}: o mod escreve nele a cada quadro e o le no init para reabrir o painel. */
    private static Field openField;
    private static Method toggleMethod;

    static boolean isOpen() {
        load();
        if (openField == null) {
            return false;
        }
        try {
            return openField.getBoolean(null);
        } catch (IllegalAccessException e) {
            openField = null;
            return false;
        }
    }

    /** Fecha o painel de atributos, se ele estiver aberto nesta tela. */
    static void close(Screen screen) {
        if (!isOpen()) {
            return;
        }
        if (toggleMethod != null) {
            for (GuiEventListener listener : screen.children()) {
                if (guiClass.isInstance(listener)) {
                    try {
                        toggleMethod.invoke(listener);
                    } catch (ReflectiveOperationException e) {
                        toggleMethod = null;
                    }
                    break;
                }
            }
        }
        // Sem isso o painel volta sozinho: o proximo init da tela le wasOpen e reabre.
        try {
            openField.setBoolean(null, false);
        } catch (IllegalAccessException e) {
            openField = null;
        }
    }

    private static void load() {
        if (checked) {
            return;
        }
        checked = true;
        try {
            guiClass = Class.forName(CLASS);
            openField = guiClass.getField("wasOpen");
            toggleMethod = guiClass.getMethod("toggleVisibility");
        } catch (ReflectiveOperationException | LinkageError e) {
            guiClass = null;
            openField = null;
            toggleMethod = null;
        }
    }

    private AttributesPanelCompat() {}
}
