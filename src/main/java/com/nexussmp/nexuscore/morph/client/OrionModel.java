package com.nexussmp.nexuscore.morph.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * O modelo do ORION, lido de {@code assets/nexuscore/models/entity/orion.json}: ossos com pivo e giro,
 * e pecas com a UV de cada face, exatamente como no Blockbench (UV por face, que o modelo do jogo nao
 * tem). O JSON sai do {@code robozinho.bbmodel}; editou no Blockbench, e so exportar de novo.
 *
 * <p>Coordenadas em pixels, com y para cima e a frente do robo para o norte (-z), como no Blockbench.
 */
public final class OrionModel {
    /** Giro e deslocamento extras de um osso naquele quadro (a animacao). */
    public interface Animator {
        /** @param rotation {x, y, z} em radianos, somados ao giro do osso; @param offset {x, y, z} em pixels */
        void animate(String bone, float[] rotation, float[] offset);
    }

    private record Cube(float[] from, float[] to, Map<String, float[]> uv) {}

    private record Bone(String name, float[] pivot, float[] rotation, List<Cube> cubes, List<Bone> children) {}

    private final List<Bone> roots;
    private final float textureWidth;
    private final float textureHeight;

    private OrionModel(List<Bone> roots, float textureWidth, float textureHeight) {
        this.roots = roots;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    public static OrionModel load(ResourceManager resources, ResourceLocation location) {
        Optional<Resource> resource = resources.getResource(location);
        if (resource.isEmpty()) {
            NexusCore.LOGGER.error("[NexusCore] orion: modelo {} nao encontrado", location);
            return new OrionModel(List.of(), 128, 128);
        }
        try (Reader reader = resource.get().openAsReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray size = json.getAsJsonArray("texture_size");
            List<Bone> roots = new ArrayList<>();
            for (JsonElement bone : json.getAsJsonArray("bones")) {
                roots.add(bone(bone.getAsJsonObject()));
            }
            return new OrionModel(roots, size.get(0).getAsFloat(), size.get(1).getAsFloat());
        } catch (Exception e) {
            NexusCore.LOGGER.error("[NexusCore] orion: modelo {} ilegivel", location, e);
            return new OrionModel(List.of(), 128, 128);
        }
    }

    private static Bone bone(JsonObject json) {
        List<Cube> cubes = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("cubes")) {
            JsonObject cube = element.getAsJsonObject();
            Map<String, float[]> uv = new java.util.HashMap<>();
            for (Map.Entry<String, JsonElement> face : cube.getAsJsonObject("uv").entrySet()) {
                uv.put(face.getKey(), floats(face.getValue().getAsJsonArray()));
            }
            cubes.add(new Cube(floats(cube.getAsJsonArray("from")), floats(cube.getAsJsonArray("to")), uv));
        }
        List<Bone> children = new ArrayList<>();
        for (JsonElement child : json.getAsJsonArray("children")) {
            children.add(bone(child.getAsJsonObject()));
        }
        return new Bone(json.get("name").getAsString(), floats(json.getAsJsonArray("pivot")),
                floats(json.getAsJsonArray("rotation")), cubes, children);
    }

    private static float[] floats(JsonArray array) {
        float[] values = new float[array.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = array.get(i).getAsFloat();
        }
        return values;
    }

    public void render(PoseStack pose, VertexConsumer consumer, int light, int overlay, Animator animator) {
        for (Bone bone : roots) {
            renderBone(bone, new float[3], pose, consumer, light, overlay, animator);
        }
    }

    private void renderBone(Bone bone, float[] parentPivot, PoseStack pose, VertexConsumer consumer, int light,
                            int overlay, Animator animator) {
        float[] rotation = new float[3];
        float[] offset = new float[3];
        animator.animate(bone.name(), rotation, offset);
        pose.pushPose();
        pose.translate(bone.pivot()[0] - parentPivot[0] + offset[0], bone.pivot()[1] - parentPivot[1] + offset[1],
                bone.pivot()[2] - parentPivot[2] + offset[2]);
        // Mesma ordem do Blockbench: Z, depois Y, depois X.
        rotate(pose, (float) Math.toRadians(bone.rotation()[0]), (float) Math.toRadians(bone.rotation()[1]),
                (float) Math.toRadians(bone.rotation()[2]));
        rotate(pose, rotation[0], rotation[1], rotation[2]);
        for (Cube cube : bone.cubes()) {
            renderCube(cube, bone.pivot(), pose.last(), consumer, light, overlay);
        }
        for (Bone child : bone.children()) {
            renderBone(child, bone.pivot(), pose, consumer, light, overlay, animator);
        }
        pose.popPose();
    }

    private static void rotate(PoseStack pose, float x, float y, float z) {
        if (z != 0) {
            pose.mulPose(Axis.ZP.rotation(z));
        }
        if (y != 0) {
            pose.mulPose(Axis.YP.rotation(y));
        }
        if (x != 0) {
            pose.mulPose(Axis.XP.rotation(x));
        }
    }

    /**
     * Cada face e um quadrado com os cantos em volta (cima-esquerda, cima-direita, baixo-direita,
     * baixo-esquerda, vistos de fora), e a UV {u1, v1, u2, v2} cai nessa ordem, como no Blockbench.
     */
    private void renderCube(Cube cube, float[] pivot, PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay) {
        float x0 = cube.from()[0] - pivot[0], y0 = cube.from()[1] - pivot[1], z0 = cube.from()[2] - pivot[2];
        float x1 = cube.to()[0] - pivot[0], y1 = cube.to()[1] - pivot[1], z1 = cube.to()[2] - pivot[2];
        face(cube, "north", pose, consumer, light, overlay, 0, 0, -1,
                x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0);
        face(cube, "south", pose, consumer, light, overlay, 0, 0, 1,
                x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1);
        face(cube, "east", pose, consumer, light, overlay, 1, 0, 0,
                x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1);
        face(cube, "west", pose, consumer, light, overlay, -1, 0, 0,
                x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0);
        face(cube, "up", pose, consumer, light, overlay, 0, 1, 0,
                x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
        face(cube, "down", pose, consumer, light, overlay, 0, -1, 0,
                x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0);
    }

    private void face(Cube cube, String name, PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay,
                      float nx, float ny, float nz, float... corners) {
        float[] uv = cube.uv().get(name);
        if (uv == null) {
            return;
        }
        float u1 = uv[0] / textureWidth, v1 = uv[1] / textureHeight, u2 = uv[2] / textureWidth, v2 = uv[3] / textureHeight;
        float[][] uvs = {{u1, v1}, {u2, v1}, {u2, v2}, {u1, v2}};
        for (int i = 0; i < 4; i++) {
            consumer.addVertex(pose, corners[i * 3], corners[i * 3 + 1], corners[i * 3 + 2])
                    .setColor(0xFFFFFFFF)
                    .setUv(uvs[i][0], uvs[i][1])
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(pose, nx, ny, nz);
        }
    }
}
