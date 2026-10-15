package com.nexussmp.nexuscore.hologram;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Onde as gravacoes ficam: {@code config/nexuscore/holograms/<nome>.nbt} (NBT comprimido).
 *
 * <p>Fica na config, e nao dentro do mundo, de proposito: assim da para gravar no mundo de teste e so
 * copiar o arquivo para o servidor. Uma gravacao carregada fica em memoria ate o servidor desligar.
 */
public final class HologramStorage {
    public static final Pattern VALID_NAME = Pattern.compile("[a-z0-9_-]{1,32}");
    /** Um arquivo maior que isso e sinal de problema, nao de discurso. */
    private static final long MAX_FILE_BYTES = 16L * 1024 * 1024;

    private static final Map<String, HologramRecording> CACHE = new HashMap<>();
    /** Arquivos que ja falharam: o erro vai para o log uma vez, e nao a cada pessoa que chega perto. */
    private static final Set<String> BROKEN = new HashSet<>();

    public static Path directory() {
        return FMLPaths.CONFIGDIR.get().resolve(NexusCore.MOD_ID).resolve("holograms");
    }

    public static boolean isValidName(String name) {
        return VALID_NAME.matcher(name).matches();
    }

    public static List<String> list() {
        Path directory = directory();
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            List<String> names = new ArrayList<>();
            files.filter(path -> path.getFileName().toString().endsWith(".nbt"))
                    .forEach(path -> {
                        String file = path.getFileName().toString();
                        names.add(file.substring(0, file.length() - 4));
                    });
            names.sort(String::compareTo);
            return List.copyOf(names);
        } catch (IOException e) {
            NexusCore.LOGGER.warn("[NexusCore] nao consegui listar as gravacoes: {}", e.toString());
            return List.of();
        }
    }

    /** {@code null} quando nao existe ou o arquivo esta quebrado (o erro vai para o log uma vez). */
    @Nullable
    public static HologramRecording load(MinecraftServer server, String name) {
        String key = name.toLowerCase(Locale.ROOT);
        // O nome vira caminho de arquivo. Os comandos ja validam, mas este e o ponto onde o nome
        // encosta no disco -- e um nome vindo de NBT editada nao passou por comando nenhum.
        if (!isValidName(key)) {
            return null;
        }
        HologramRecording cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        Path file = directory().resolve(key + ".nbt");
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            if (Files.size(file) > MAX_FILE_BYTES) {
                if (BROKEN.add(key)) {
                    NexusCore.LOGGER.warn("[NexusCore] gravacao {} e grande demais ({} bytes); ignorada.", key, Files.size(file));
                }
                return null;
            }
            CompoundTag tag = NbtIo.readCompressed(file, NbtAccounter.create(MAX_FILE_BYTES));
            HologramRecording recording = HologramRecording.load(tag, server.registryAccess(), key);
            CACHE.put(key, recording);
            BROKEN.remove(key);
            return recording;
        } catch (Exception e) {
            if (BROKEN.add(key)) {
                NexusCore.LOGGER.error("[NexusCore] gravacao {} nao pode ser lida: {}", key, e.toString());
            }
            return null;
        }
    }

    public static boolean save(MinecraftServer server, HologramRecording recording) {
        // O nome e conferido antes de qualquer coisa encostar no disco, inclusive criar a pasta.
        if (!isValidName(recording.name())) {
            NexusCore.LOGGER.warn("[NexusCore] gravacao com nome invalido nao foi salva: {}", recording.name());
            return false;
        }
        Path directory = directory();
        try {
            Files.createDirectories(directory);
            Path file = directory.resolve(recording.name() + ".nbt");
            // Escreve ao lado e troca de uma vez: se o servidor cair no meio, a gravacao antiga
            // continua inteira, em vez de sobrar meio arquivo que nao abre mais.
            Path partial = directory.resolve(recording.name() + ".nbt.tmp");
            NbtIo.writeCompressed(recording.save(server.registryAccess()), partial);
            try {
                Files.move(partial, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(partial, file, StandardCopyOption.REPLACE_EXISTING);
            }
            CACHE.put(recording.name(), recording);
            BROKEN.remove(recording.name());
            return true;
        } catch (IOException e) {
            NexusCore.LOGGER.error("[NexusCore] nao consegui salvar a gravacao {}: {}", recording.name(), e.toString());
            return false;
        }
    }

    public static boolean delete(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        try {
            if (!isValidName(key)) {
                return false;
            }
            boolean deleted = Files.deleteIfExists(directory().resolve(key + ".nbt"));
            CACHE.remove(key);
            BROKEN.remove(key);
            return deleted;
        } catch (IOException e) {
            NexusCore.LOGGER.error("[NexusCore] nao consegui apagar a gravacao {}: {}", key, e.toString());
            return false;
        }
    }

    public static void clearCache() {
        CACHE.clear();
        BROKEN.clear();
    }

    private HologramStorage() {}
}
