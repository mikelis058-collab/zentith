package net.zenith;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ZenithConfig {
    // Fog settings (edit in the Zenith menu or in config/zenith.json)
    public static int fogR = 190, fogG = 205, fogB = 220;
    public static int fogDensity = 40; // 0-100

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("zenith.json");
    }

    private static int num(JsonObject o, String key, int def, int max) {
        if (!o.has(key)) return def;
        return Math.max(0, Math.min(max, o.get(key).getAsInt()));
    }

    public static void load() {
        Path p = path();
        if (!Files.exists(p)) return;
        try (Reader r = Files.newBufferedReader(p)) {
            JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
            fogR = num(o, "fogR", fogR, 255);
            fogG = num(o, "fogG", fogG, 255);
            fogB = num(o, "fogB", fogB, 255);
            fogDensity = num(o, "fogDensity", fogDensity, 100);
            if (o.has("modules")) {
                JsonObject mods = o.getAsJsonObject("modules");
                for (Module m : Modules.ALL) {
                    if (mods.has(m.name)) m.enabled = mods.get(m.name).getAsBoolean();
                }
            }
        } catch (Exception e) {
            System.err.println("[Zenith] Could not read config: " + e);
        }
    }

    public static void save() {
        JsonObject o = new JsonObject();
        o.addProperty("fogR", fogR);
        o.addProperty("fogG", fogG);
        o.addProperty("fogB", fogB);
        o.addProperty("fogDensity", fogDensity);
        JsonObject mods = new JsonObject();
        for (Module m : Modules.ALL) mods.addProperty(m.name, m.enabled);
        o.add("modules", mods);
        try (Writer w = Files.newBufferedWriter(path())) {
            GSON.toJson(o, w);
        } catch (Exception e) {
            System.err.println("[Zenith] Could not save config: " + e);
        }
    }
}
