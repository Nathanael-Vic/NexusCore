package com.nexussmp.nexuscore.core.module;

import java.util.List;

/** IDs dos modulos. Fonte unica: a config de liga/desliga e montada a partir de {@link #ALL}. */
public final class ModuleIds {
    public static final String COMPAT_SODIUM = "compat.sodium";
    public static final String COMPAT_WATERFRAMES = "compat.waterframes";
    public static final String COMPAT_OPTIONS = "compat.options";
    public static final String COMPAT_COSMETICARMOR = "compat.cosmeticarmor";
    public static final String COMPAT_ENVELOPE = "compat.envelope";
    public static final String STATS = "stats";
    public static final String DISPLAY = "display";
    public static final String SPACE = "space";
    public static final String HOLOGRAM = "hologram";
    public static final String QUEST = "quest";
    public static final String BACKROOMS = "backrooms";
    public static final String RADIO = "radio";
    public static final String VAMPIRE = "vampire";
    public static final String MORPH = "morph";
    public static final String MAGIC = "magic";
    public static final String PARROT = "parrot";
    public static final String RELIC = "relic";

    public static final List<String> ALL =
            List.of(COMPAT_SODIUM, COMPAT_WATERFRAMES, COMPAT_OPTIONS, COMPAT_COSMETICARMOR, COMPAT_ENVELOPE,
                    STATS, DISPLAY, SPACE, HOLOGRAM, QUEST, BACKROOMS, RADIO, VAMPIRE, MORPH, MAGIC, PARROT, RELIC);

    private ModuleIds() {}
}
