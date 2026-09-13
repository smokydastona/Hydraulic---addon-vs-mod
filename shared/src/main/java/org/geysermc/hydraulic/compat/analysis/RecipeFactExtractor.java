package org.geysermc.hydraulic.compat.analysis;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.geysermc.hydraulic.compat.ContentInventory;
import org.jetbrains.annotations.NotNull;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

final class RecipeFactExtractor {
    private RecipeFactExtractor() {
    }

    @NotNull
    static Result extract(@NotNull ContentInventory.ModContentInventory inventory, @NotNull String recipeIdentifier) {
        Path recipePath = locate(inventory, recipeIdentifier);
        if (recipePath == null) {
            return new Result(Map.of(), Status.MISSING);
        }

        try (Reader reader = Files.newBufferedReader(recipePath)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) {
                return new Result(Map.of(), Status.MALFORMED);
            }

            JsonObject recipe = parsed.getAsJsonObject();
            Map<String, String> facts = new LinkedHashMap<>();
            putString(facts, "recipe.type", recipe, "type");
            putString(facts, "recipe.category", recipe, "category");
            putString(facts, "recipe.group", recipe, "group");
            putPositiveInt(facts, "recipe.duration", recipe, "cookingtime");
            putPositiveInt(facts, "recipe.duration", recipe, "time");

            JsonObject result = object(recipe, "result");
            if (result == null) {
                result = object(recipe, "output");
            }
            if (result != null) {
                putString(facts, "recipe.output.item", result, "item");
                putString(facts, "recipe.output.item", result, "id");
                putPositiveInt(facts, "recipe.output.count", result, "count");
                putPositiveInt(facts, "recipe.output.count", result, "amount");
            }

            JsonElement ingredient = recipe.get("ingredient");
            if (ingredient == null) {
                ingredient = recipe.get("ingredients");
            }
            JsonObject firstIngredient = firstObject(ingredient);
            if (firstIngredient != null) {
                putString(facts, "recipe.input.item", firstIngredient, "item");
                putString(facts, "recipe.input.item", firstIngredient, "id");
                putPositiveInt(facts, "recipe.input.count", firstIngredient, "count");
            }

            if (facts.isEmpty()) {
                return new Result(Map.of(), Status.UNSUPPORTED_SCHEMA);
            }
            facts.put("recipe.fact_status", "valid");
            return new Result(facts, Status.VALID);
        } catch (Exception ignored) {
            return new Result(Map.of(), Status.MALFORMED);
        }
    }

    private static Path locate(@NotNull ContentInventory.ModContentInventory inventory, @NotNull String recipeIdentifier) {
        Path indexedPath = inventory.recipePath(recipeIdentifier);
        if (indexedPath != null && Files.isRegularFile(indexedPath)) {
            return indexedPath;
        }
        int separator = recipeIdentifier.indexOf(':');
        if (separator <= 0 || separator == recipeIdentifier.length() - 1) {
            return null;
        }
        String namespace = recipeIdentifier.substring(0, separator);
        String relative = recipeIdentifier.substring(separator + 1) + ".json";
        for (String rootValue : inventory.roots()) {
            Path root = Path.of(rootValue).toAbsolutePath().normalize();
            Path candidate = root.resolve("data").resolve(namespace).resolve("recipes").resolve(relative).normalize();
            if (!candidate.startsWith(root) || !Files.isRegularFile(candidate)) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    private static JsonObject object(@NotNull JsonObject parent, @NotNull String key) {
        JsonElement value = parent.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    private static JsonObject firstObject(JsonElement value) {
        if (value == null) {
            return null;
        }
        if (value.isJsonObject()) {
            return value.getAsJsonObject();
        }
        if (value.isJsonArray()) {
            JsonArray values = value.getAsJsonArray();
            for (JsonElement element : values) {
                if (element.isJsonObject()) {
                    return element.getAsJsonObject();
                }
            }
        }
        return null;
    }

    private static void putString(@NotNull Map<String, String> facts, @NotNull String fact, @NotNull JsonObject object, @NotNull String key) {
        JsonElement value = object.get(key);
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            String text = value.getAsString().trim();
            if (!text.isEmpty()) {
                facts.putIfAbsent(fact, text);
            }
        }
    }

    private static void putPositiveInt(@NotNull Map<String, String> facts, @NotNull String fact, @NotNull JsonObject object, @NotNull String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            return;
        }
        int number = value.getAsInt();
        if (number > 0) {
            facts.putIfAbsent(fact, Integer.toString(number));
        }
    }

    enum Status {
        VALID,
        MISSING,
        MALFORMED,
        UNSUPPORTED_SCHEMA
    }

    record Result(@NotNull Map<String, String> facts, @NotNull Status status) {
        Result {
            facts = Map.copyOf(new LinkedHashMap<>(facts));
        }
    }
}