package com.uda.client.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.uda.UltimateDarkAdventure;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Monta modelos a partir de assets/uda/geo/&lt;nome&gt;.json, gerados por tools/bosses/gerar_bosses.mjs
 * junto com as texturas (assim o UV do modelo e o desenho da textura sempre batem).
 */
public final class GeoModels {
    private GeoModels() {
    }

    public static LayerDefinition load(String name) {
        String path = "/assets/" + UltimateDarkAdventure.MODID + "/geo/" + name + ".json";
        try (InputStream in = GeoModels.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Geometria não encontrada: " + path);
            }
            JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray texture = json.getAsJsonArray("texture");
            MeshDefinition mesh = new MeshDefinition();
            Map<String, PartDefinition> parts = new HashMap<>();
            parts.put("root", mesh.getRoot());
            for (JsonElement element : json.getAsJsonArray("parts")) {
                JsonObject part = element.getAsJsonObject();
                CubeListBuilder cubes = CubeListBuilder.create();
                for (JsonElement boxElement : part.getAsJsonArray("boxes")) {
                    JsonObject box = boxElement.getAsJsonObject();
                    float[] origin = floats(box.getAsJsonArray("origin"));
                    float[] size = floats(box.getAsJsonArray("size"));
                    float[] uv = floats(box.getAsJsonArray("uv"));
                    cubes.texOffs((int) uv[0], (int) uv[1]).mirror(box.get("mirror").getAsBoolean())
                            .addBox(origin[0], origin[1], origin[2], size[0], size[1], size[2], new CubeDeformation(box.get("inflate").getAsFloat()));
                }
                float[] pivot = floats(part.getAsJsonArray("pivot"));
                float[] rotation = floats(part.getAsJsonArray("rotation"));
                PartDefinition parent = parts.get(part.get("parent").getAsString());
                String partName = part.get("name").getAsString();
                parts.put(partName, parent.addOrReplaceChild(partName, cubes,
                        PartPose.offsetAndRotation(pivot[0], pivot[1], pivot[2], rotation[0], rotation[1], rotation[2])));
            }
            return LayerDefinition.create(mesh, texture.get(0).getAsInt(), texture.get(1).getAsInt());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static float[] floats(JsonArray array) {
        float[] out = new float[array.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = array.get(i).getAsFloat();
        }
        return out;
    }
}
