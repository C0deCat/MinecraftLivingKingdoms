package livingkingdoms.world;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * The World Content Manifest (M1-C1): what a world must contain, read from data files
 * {@code data/<namespace>/content_manifest/<name>.json}, one entry per file. Later milestones add
 * entries the same way. Categories and numbers: {@code docs/design-docs/world-content.md}.
 *
 * <pre>{@code
 * {
 *   "category": "progression",              // free label, see the design doc
 *   "description": "End portal",
 *   "dimension": "minecraft:overworld",
 *   "structures": ["betterstrongholds:stronghold", "#some:tag"],
 *   "min": {"small": 2, "medium": 2, "large": 2},   // reachable starts per size; Test is not validated
 *   "max": {"large": 20},                   // optional, a warning above it
 *   "severity": "critical",                 // or "warning": how a shortfall is reported
 *   "access": {"entrance": "ok", "max_hanging": 0.25, "allow_water": false},
 *   "access_by_structure": {"ns:id": {"entrance": "any"}},
 *   "spacing": 1000,                        // optional, blocks between starts of this entry (warning)
 *   "min_kinds": 3                          // optional, distinct structures that must be present
 * }
 * }</pre>
 *
 * Access: {@code entrance} {@code ok} (the start piece sits on the ground), {@code buried_ok} (not
 * floating) or {@code any}; {@code max_hanging} the largest share of a piece's footprint that may hang
 * more than 3 blocks above the ground; {@code allow_water} whether more than half the ground under the
 * structure may be below sea level. Only surface structures in dimensions with terrain are measured;
 * the others always pass.
 */
public final class ContentManifest {
    enum Severity { CRITICAL, WARNING }

    record Access(String entrance, double maxHanging, boolean allowWater) {
        static final Access ANY = new Access("any", -1, true);

        static Access parse(JsonObject json) {
            String entrance = GsonHelper.getAsString(json, "entrance", "any");
            if (!entrance.equals("any") && !entrance.equals("ok") && !entrance.equals("buried_ok")) {
                throw new IllegalArgumentException("entrance must be any, ok or buried_ok: " + entrance);
            }
            return new Access(entrance, GsonHelper.getAsDouble(json, "max_hanging", -1), GsonHelper.getAsBoolean(json, "allow_water", true));
        }
    }

    record Entry(ResourceLocation id, String category, String description, ResourceLocation dimension, List<String> structures,
                 Map<WorldSize, Integer> min, Map<WorldSize, Integer> max, Severity severity, Access access,
                 Map<String, Access> accessByStructure, int spacing, int minKinds) {
        Access accessFor(String structure) {
            return accessByStructure.getOrDefault(structure, access);
        }

        static Entry parse(ResourceLocation id, JsonObject json) {
            List<String> structures = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "structures")) structures.add(element.getAsString());
            Map<String, Access> byStructure = new LinkedHashMap<>();
            if (json.has("access_by_structure")) {
                for (Map.Entry<String, JsonElement> e : GsonHelper.getAsJsonObject(json, "access_by_structure").entrySet()) {
                    byStructure.put(e.getKey(), Access.parse(e.getValue().getAsJsonObject()));
                }
            }
            return new Entry(id, GsonHelper.getAsString(json, "category"), GsonHelper.getAsString(json, "description", ""),
                    ResourceLocation.parse(GsonHelper.getAsString(json, "dimension", "minecraft:overworld")), List.copyOf(structures),
                    sizes(json, "min"), sizes(json, "max"),
                    Severity.valueOf(GsonHelper.getAsString(json, "severity", "warning").toUpperCase(java.util.Locale.ROOT)),
                    json.has("access") ? Access.parse(GsonHelper.getAsJsonObject(json, "access")) : Access.ANY, Map.copyOf(byStructure),
                    GsonHelper.getAsInt(json, "spacing", 0), GsonHelper.getAsInt(json, "min_kinds", 0));
        }

        private static Map<WorldSize, Integer> sizes(JsonObject json, String key) {
            Map<WorldSize, Integer> result = new EnumMap<>(WorldSize.class);
            if (!json.has(key)) return result;
            for (Map.Entry<String, JsonElement> e : GsonHelper.getAsJsonObject(json, key).entrySet()) {
                WorldSize size = WorldSize.byId(e.getKey()).orElseThrow(() -> new IllegalArgumentException("unknown size " + e.getKey()));
                result.put(size, e.getValue().getAsInt());
            }
            return result;
        }
    }

    private static volatile List<Entry> entries = List.of();

    private ContentManifest() {}

    static List<Entry> entries() {
        return entries;
    }

    static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new Loader());
    }

    private static final class Loader extends SimpleJsonResourceReloadListener {
        Loader() {
            super(new Gson(), "content_manifest");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            List<Entry> loaded = new ArrayList<>();
            files.forEach((id, json) -> {
                try {
                    loaded.add(Entry.parse(id, json.getAsJsonObject()));
                } catch (RuntimeException e) {
                    LivingKingdomsWorld.LOGGER.error("Invalid content manifest entry {}: {}", id, e.getMessage());
                }
            });
            loaded.sort(java.util.Comparator.comparing(e -> e.id().toString()));
            entries = Collections.unmodifiableList(loaded);
            LivingKingdomsWorld.LOGGER.info("Content manifest: {} entries", loaded.size());
        }
    }
}
