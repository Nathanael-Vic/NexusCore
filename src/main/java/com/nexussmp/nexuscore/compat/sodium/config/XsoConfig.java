/*
 * Parte do NexusCore.
 * Derivado de Xander's Sodium Options: Continued 3.4.2
 * (isXander, IMB11, soradotwav) - https://github.com/soradotwav/xanders-sodium-options
 * Licenciado sob a GNU Lesser General Public License v3.0 ou posterior (LGPL-3.0-or-later).
 * Alteracoes: portado para Minecraft 1.21.1 / NeoForge e reorganizado nos pacotes do NexusCore.
 */
package com.nexussmp.nexuscore.compat.sodium.config;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class XsoConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("NexusCore/Sodium");
    private static final String FILE_NAME = "nexuscore-sodium-options.json";
    /** Arquivo do jar antigo do XSO; migrado uma vez para {@link #FILE_NAME}. */
    private static final String LEGACY_FILE_NAME = "xanders-sodium-options.json";

    private static boolean dirty = false;

    public static final ConfigClassHandler<XsoConfig> INSTANCE = ConfigClassHandler.createBuilder(XsoConfig.class)
            .id(ResourceLocation.fromNamespaceAndPath("nexuscore", "sodium_options"))
            .serializer(handler -> GsonConfigSerializerBuilder.create(handler)
                    .setPath(FMLPaths.CONFIGDIR.get().resolve(FILE_NAME))
                    .appendGsonBuilder(com.google.gson.GsonBuilder::setPrettyPrinting)
                    .build())
            .build();

    @SerialEntry
    public boolean enabled = true;

    @SerialEntry
    public boolean lenientOptions = true;

    @SerialEntry
    public boolean hardCrash = false;

    @SerialEntry
    public boolean externalMenus = false;

    public static ConfigCategory getConfigCategory() {
        XsoConfig config = INSTANCE.instance();
        XsoConfig defaults = INSTANCE.defaults();

        return ConfigCategory.createBuilder()
                .name(Component.translatable("xso.title"))
                .option(LabelOption.create(Component.empty()))
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("xso.cfg.enabled"))
                        .description(OptionDescription.of(Component.translatable("xso.cfg.enabled.tooltip")))
                        .binding(defaults.enabled, () -> config.enabled, val -> {
                            config.enabled = val;
                            dirty = true;
                        })
                        .controller(BooleanControllerBuilder::create)
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("xso.cfg.lenient_opts"))
                        .description(OptionDescription.of(Component.translatable("xso.cfg.lenient_opts.tooltip")))
                        .binding(defaults.lenientOptions, () -> config.lenientOptions, val -> {
                            config.lenientOptions = val;
                            dirty = true;
                        })
                        .controller(opt -> BooleanControllerBuilder.create(opt)
                                .yesNoFormatter()
                                .coloured(false))
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("xso.cfg.hard_crash"))
                        .description(OptionDescription.of(Component.translatable("xso.cfg.hard_crash.tooltip")))
                        .binding(defaults.hardCrash, () -> config.hardCrash, val -> {
                            config.hardCrash = val;
                            dirty = true;
                        })
                        .controller(opt -> BooleanControllerBuilder.create(opt)
                                .yesNoFormatter()
                                .coloured(false))
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("xso.cfg.external_menus"))
                        .description(OptionDescription.of(Component.translatable("xso.cfg.external_menus.tooltip")))
                        .binding(defaults.externalMenus, () -> config.externalMenus, val -> {
                            config.externalMenus = val;
                            dirty = true;
                        })
                        .controller(opt -> BooleanControllerBuilder.create(opt)
                                .yesNoFormatter()
                                .coloured(false))
                        .build())
                .build();
    }

    public static void load() {
        migrateLegacyFile();
        INSTANCE.load();
    }

    /** Se so existir a config do jar antigo do XSO, copia para o arquivo novo antes de carregar. */
    private static void migrateLegacyFile() {
        Path configDir = FMLPaths.CONFIGDIR.get();
        Path current = configDir.resolve(FILE_NAME);
        Path legacy = configDir.resolve(LEGACY_FILE_NAME);
        if (Files.exists(current) || !Files.exists(legacy)) {
            return;
        }
        try {
            Files.copy(legacy, current);
            LOGGER.info("[NexusCore] config antiga do XSO migrada: {} -> {}", LEGACY_FILE_NAME, FILE_NAME);
        } catch (IOException e) {
            LOGGER.warn("[NexusCore] nao consegui migrar {}: {}", LEGACY_FILE_NAME, e.toString());
        }
    }

    public static void applyChanges() {
        if (dirty) {
            INSTANCE.save();
            dirty = false;
        }
    }
}
