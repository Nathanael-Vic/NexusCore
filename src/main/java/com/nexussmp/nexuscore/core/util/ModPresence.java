package com.nexussmp.nexuscore.core.util;

import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

/**
 * Presenca e versao de mods usando o {@link LoadingModList}, que ja existe quando o Mixin pergunta
 * se deve aplicar cada mixin (o {@code ModList} ainda nao existe nessa etapa).
 */
public final class ModPresence {

    public record Result(Status status, String foundVersion) {
        public boolean ok() {
            return status == Status.OK;
        }

        public String describe(ModRequirement requirement) {
            return switch (status) {
                case OK -> "ok";
                case MISSING -> requirement.modId() + " ausente";
                case OUT_OF_RANGE -> requirement.modId() + " " + foundVersion + " fora da faixa " + requirement.versionRange();
            };
        }
    }

    public enum Status { OK, MISSING, OUT_OF_RANGE }

    public static Result check(ModRequirement requirement) {
        LoadingModList mods = LoadingModList.get();
        if (mods == null) {
            return new Result(Status.MISSING, null);
        }
        for (ModInfo info : mods.getMods()) {
            if (!info.getModId().equals(requirement.modId())) {
                continue;
            }
            String version = info.getVersion().toString();
            return new Result(inRange(version, requirement.versionRange()) ? Status.OK : Status.OUT_OF_RANGE, version);
        }
        return new Result(Status.MISSING, null);
    }

    private static boolean inRange(String version, String range) {
        try {
            // Metadados depois do "+" (ex.: "0.8.13+mc1.21.1") nao entram na comparacao.
            int plus = version.indexOf('+');
            String comparable = plus >= 0 ? version.substring(0, plus) : version;
            return VersionRange.createFromVersionSpec(range).containsVersion(new DefaultArtifactVersion(comparable));
        } catch (InvalidVersionSpecificationException e) {
            return false;
        }
    }

    private ModPresence() {}
}
