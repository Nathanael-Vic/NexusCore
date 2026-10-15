package com.nexussmp.nexuscore.hologram;

import com.nexussmp.nexuscore.core.util.Profiles;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Uma gravacao de movimento: o que o jogador fez, tick a tick.
 *
 * <p>Guardado em colunas (um array por dado) em vez de um objeto por tick: 1 minuto de gravacao da
 * ~20 KB. As rotacoes usam 1 byte cada, que e exatamente a precisao com que o jogo manda rotacao de
 * entidade pela rede — ou seja, nao se perde nada que os outros jogadores veriam.
 *
 * <p>As posicoes sao relativas a origem da gravacao, entao a mesma gravacao pode ser reproduzida em
 * qualquer lugar do mundo.
 */
public final class HologramRecording {
    public static final int FORMAT_VERSION = 1;
    /** Milesimos de bloco: precisao muito acima do que o olho ve, e cabe num int. */
    private static final double POSITION_SCALE = 1000.0;

    public static final int FLAG_SPRINTING = 1;
    public static final int FLAG_SWING_MAIN = 1 << 1;
    public static final int FLAG_SWING_OFF = 1 << 2;

    private static final Pose[] POSES = Pose.values();

    /** Troca de item/armadura num tick. Guardado so quando muda, e nao a cada tick. */
    public record EquipmentChange(int tick, EquipmentSlot slot, ItemStack stack) {}

    private final String name;
    private final UUID authorId;
    private final String authorName;
    private final HumanoidArm mainArm;
    private final Vec3 origin;
    private final float originYaw;
    private final int[] posX;
    private final int[] posY;
    private final int[] posZ;
    private final byte[] bodyYaw;
    private final byte[] headYaw;
    private final byte[] pitch;
    private final byte[] pose;
    private final byte[] flags;
    private final List<EquipmentChange> equipment;
    @Nullable
    private ResourceLocation sound;
    /**
     * O perfil de quem gravou, textura inclusive.
     *
     * <p>Sem ele o cliente nao tem como achar a skin de um autor que esta offline — o jogo so sabe
     * desembrulhar a textura que vem dentro do perfil — e o holograma apareceria com a cara padrao.
     * Gravacoes antigas nao tem este campo; nesse caso vale o que sempre valeu: id e nome.
     */
    @Nullable
    private CompoundTag authorProfile;

    private HologramRecording(String name, UUID authorId, String authorName, HumanoidArm mainArm, Vec3 origin,
                              float originYaw, int[] posX, int[] posY, int[] posZ, byte[] bodyYaw, byte[] headYaw,
                              byte[] pitch, byte[] pose, byte[] flags, List<EquipmentChange> equipment,
                              @Nullable ResourceLocation sound) {
        this.name = name;
        this.authorId = authorId;
        this.authorName = authorName;
        this.mainArm = mainArm;
        this.origin = origin;
        this.originYaw = originYaw;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.bodyYaw = bodyYaw;
        this.headYaw = headYaw;
        this.pitch = pitch;
        this.pose = pose;
        this.flags = flags;
        this.equipment = equipment;
        this.sound = sound;
    }

    public String name() {
        return name;
    }

    public UUID authorId() {
        return authorId;
    }

    public String authorName() {
        return authorName;
    }

    /** O perfil do autor, ou {@code null} em gravacoes feitas antes de ele passar a ser guardado. */
    @Nullable
    public CompoundTag authorProfile() {
        return authorProfile;
    }

    public HumanoidArm mainArm() {
        return mainArm;
    }

    public int tickCount() {
        return posX.length;
    }

    /** Duracao em segundos. */
    public float seconds() {
        return tickCount() / 20.0F;
    }

    @Nullable
    public ResourceLocation sound() {
        return sound;
    }

    public void setSound(@Nullable ResourceLocation sound) {
        this.sound = sound;
    }

    public List<EquipmentChange> equipment() {
        return equipment;
    }

    public double x(int tick) {
        return posX[tick] / POSITION_SCALE;
    }

    public double y(int tick) {
        return posY[tick] / POSITION_SCALE;
    }

    public double z(int tick) {
        return posZ[tick] / POSITION_SCALE;
    }

    public float bodyYaw(int tick) {
        return decodeAngle(bodyYaw[tick]);
    }

    public float headYaw(int tick) {
        return decodeAngle(headYaw[tick]);
    }

    public float pitch(int tick) {
        return decodeAngle(pitch[tick]);
    }

    public Pose pose(int tick) {
        byte value = pose[tick];
        return value >= 0 && value < POSES.length ? POSES[value] : Pose.STANDING;
    }

    public boolean flag(int tick, int flag) {
        return (flags[tick] & flag) != 0;
    }

    private static float decodeAngle(byte value) {
        return value * 360.0F / 256.0F;
    }

    private static byte encodeAngle(float degrees) {
        return (byte) Mth.floor(Mth.wrapDegrees(degrees) * 256.0F / 360.0F);
    }

    // ---- gravacao ----

    /** Coleta os ticks enquanto a gravacao acontece. */
    public static final class Builder {
        private final String name;
        private final UUID authorId;
        private final String authorName;
        private final CompoundTag authorProfile;
        private final HumanoidArm mainArm;
        private final Vec3 origin;
        private final float originYaw;
        private final IntArrayList posX = new IntArrayList();
        private final IntArrayList posY = new IntArrayList();
        private final IntArrayList posZ = new IntArrayList();
        private final ByteArrayList bodyYaw = new ByteArrayList();
        private final ByteArrayList headYaw = new ByteArrayList();
        private final ByteArrayList pitch = new ByteArrayList();
        private final ByteArrayList pose = new ByteArrayList();
        private final ByteArrayList flags = new ByteArrayList();
        private final List<EquipmentChange> equipment = new ArrayList<>();
        private final ItemStack[] lastEquipment = new ItemStack[EquipmentSlot.values().length];

        public Builder(String name, ServerPlayer player) {
            this.name = name;
            this.authorId = player.getUUID();
            this.authorName = player.getGameProfile().getName();
            this.authorProfile = Profiles.write(player.getGameProfile());
            this.mainArm = player.getMainArm();
            this.origin = player.position();
            this.originYaw = player.getYRot();
            for (int i = 0; i < lastEquipment.length; i++) {
                lastEquipment[i] = ItemStack.EMPTY;
            }
        }

        public int tickCount() {
            return posX.size();
        }

        public void capture(ServerPlayer player, boolean swingMain, boolean swingOff) {
            int tick = posX.size();
            posX.add(encodePosition(player.getX() - origin.x));
            posY.add(encodePosition(player.getY() - origin.y));
            posZ.add(encodePosition(player.getZ() - origin.z));
            bodyYaw.add(encodeAngle(player.getYRot()));
            headYaw.add(encodeAngle(player.getYHeadRot()));
            pitch.add(encodeAngle(player.getXRot()));
            pose.add((byte) player.getPose().ordinal());

            int frameFlags = 0;
            if (player.isSprinting()) {
                frameFlags |= FLAG_SPRINTING;
            }
            if (swingMain) {
                frameFlags |= FLAG_SWING_MAIN;
            }
            if (swingOff) {
                frameFlags |= FLAG_SWING_OFF;
            }
            flags.add((byte) frameFlags);

            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack current = player.getItemBySlot(slot);
                if (!ItemStack.matches(current, lastEquipment[slot.ordinal()])) {
                    lastEquipment[slot.ordinal()] = current.copy();
                    equipment.add(new EquipmentChange(tick, slot, current.copy()));
                }
            }
        }

        public HologramRecording build() {
            HologramRecording recording = new HologramRecording(name, authorId, authorName, mainArm, origin, originYaw,
                    posX.toIntArray(), posY.toIntArray(), posZ.toIntArray(),
                    bodyYaw.toByteArray(), headYaw.toByteArray(), pitch.toByteArray(),
                    pose.toByteArray(), flags.toByteArray(), List.copyOf(equipment), null);
            recording.authorProfile = authorProfile;
            return recording;
        }

        private static int encodePosition(double offset) {
            return (int) Math.round(Mth.clamp(offset, -1_000_000.0, 1_000_000.0) * POSITION_SCALE);
        }
    }

    // ---- arquivo ----

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("version", FORMAT_VERSION);
        tag.putString("name", name);
        tag.putUUID("author", authorId);
        tag.putString("authorName", authorName);
        if (authorProfile != null && !authorProfile.isEmpty()) {
            tag.put("authorProfile", authorProfile);
        }
        tag.putBoolean("leftHanded", mainArm == HumanoidArm.LEFT);
        tag.putDouble("originX", origin.x);
        tag.putDouble("originY", origin.y);
        tag.putDouble("originZ", origin.z);
        tag.putFloat("originYaw", originYaw);
        tag.putIntArray("posX", posX);
        tag.putIntArray("posY", posY);
        tag.putIntArray("posZ", posZ);
        tag.putByteArray("bodyYaw", bodyYaw);
        tag.putByteArray("headYaw", headYaw);
        tag.putByteArray("pitch", pitch);
        tag.putByteArray("pose", pose);
        tag.putByteArray("flags", flags);
        if (sound != null) {
            tag.putString("sound", sound.toString());
        }

        ListTag changes = new ListTag();
        for (EquipmentChange change : equipment) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("tick", change.tick());
            entry.putByte("slot", (byte) change.slot().ordinal());
            if (!change.stack().isEmpty()) {
                entry.put("item", change.stack().save(registries));
            }
            changes.add(entry);
        }
        tag.put("equipment", changes);
        return tag;
    }

    /**
     * Le uma gravacao do disco. O nome e o do <b>arquivo</b>, e nao o que ficou gravado dentro dele:
     * quem renomeia o {@code .nbt} esta renomeando a gravacao, e e por esse nome que o holograma
     * acha o audio, que o comando de parar acha o holograma e que o proximo salvamento acha o arquivo.
     */
    public static HologramRecording load(CompoundTag tag, HolderLookup.Provider registries, String name) {
        int[] x = tag.getIntArray("posX");
        int[] y = tag.getIntArray("posY");
        int[] z = tag.getIntArray("posZ");
        int ticks = Math.min(x.length, Math.min(y.length, z.length));
        if (ticks == 0) {
            throw new IllegalArgumentException("gravacao sem nenhum tick");
        }

        List<EquipmentChange> equipment = new ArrayList<>();
        ListTag changes = tag.getList("equipment", Tag.TAG_COMPOUND);
        EquipmentSlot[] slots = EquipmentSlot.values();
        for (int i = 0; i < changes.size(); i++) {
            CompoundTag entry = changes.getCompound(i);
            byte slot = entry.getByte("slot");
            if (slot < 0 || slot >= slots.length) {
                continue;
            }
            ItemStack stack = entry.contains("item")
                    ? ItemStack.parse(registries, entry.get("item")).orElse(ItemStack.EMPTY)
                    : ItemStack.EMPTY;
            equipment.add(new EquipmentChange(entry.getInt("tick"), slots[slot], stack));
        }

        ResourceLocation sound = tag.contains("sound") ? ResourceLocation.tryParse(tag.getString("sound")) : null;
        HologramRecording recording = new HologramRecording(
                name,
                tag.hasUUID("author") ? tag.getUUID("author") : new UUID(0, 0),
                tag.getString("authorName"),
                tag.getBoolean("leftHanded") ? HumanoidArm.LEFT : HumanoidArm.RIGHT,
                new Vec3(tag.getDouble("originX"), tag.getDouble("originY"), tag.getDouble("originZ")),
                tag.getFloat("originYaw"),
                trim(x, ticks), trim(y, ticks), trim(z, ticks),
                trim(tag.getByteArray("bodyYaw"), ticks), trim(tag.getByteArray("headYaw"), ticks),
                trim(tag.getByteArray("pitch"), ticks), trim(tag.getByteArray("pose"), ticks),
                trim(tag.getByteArray("flags"), ticks),
                List.copyOf(equipment), sound);
        if (tag.contains("authorProfile", Tag.TAG_COMPOUND)) {
            recording.authorProfile = tag.getCompound("authorProfile");
        }
        return recording;
    }

    private static int[] trim(int[] values, int length) {
        return values.length == length ? values : java.util.Arrays.copyOf(values, length);
    }

    private static byte[] trim(byte[] values, int length) {
        return values.length == length ? values : java.util.Arrays.copyOf(values, length);
    }
}
