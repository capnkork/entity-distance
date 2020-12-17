package com.capnkork.entitydistance.config;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.EntityType;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

public final class EntityDistanceConfig {
    private static EntityDistanceConfig INSTANCE = null;

    private static final int MIN_DISTANCE = 0;
    private static final int DEFAULT_DISTANCE = 64;
    private static final int MAX_DISTANCE = 128;

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("entitydistancemod.json");

    private int versionId = 0;

    private class EntityTypeComparator implements Comparator<EntityType<?>> {
        @Override
        public int compare(EntityType<?> a, EntityType<?> b) {
            return EntityType.getId(a).compareTo(EntityType.getId(b));
        }
    }

    private Map<EntityType<?>, Integer> distances = new LinkedHashMap<>();

    private EntityDistanceConfig() {
        // get all types
        List<EntityType<?>> types = new ArrayList<>();
        for (Map.Entry<RegistryKey<EntityType<?>>, EntityType<?>> entry : Registry.ENTITY_TYPE.getEntries()) {
            types.add(entry.getValue());
        }

        // sort alphabetically
        Collections.sort(types, new EntityTypeComparator());

        // insert into LinkedHashMap (which preserves insertion order)
        for (EntityType<?> type : types) {
            distances.put(type, DEFAULT_DISTANCE);
        }

        loadConfig();
    }

    public static EntityDistanceConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new EntityDistanceConfig();
        }

        return INSTANCE;
    }

    public int getVersionId() {
        return versionId;
    }

    public Integer getEntityDistanceByType(EntityType<?> type) {
        return distances.get(type);
    }

    public Screen buildScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(new TranslatableText("Entity Distance Mod"));

        builder.setSavingRunnable(() -> {
            saveConfig();
        });

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(new TranslatableText("Entity Distances"));

        for (Map.Entry<EntityType<?>, Integer> entry : distances.entrySet()) {
            String entityId = EntityType.getId(entry.getKey()).toString();

            general.addEntry(entryBuilder.startIntSlider(new TranslatableText(entityId), entry.getValue(), MIN_DISTANCE, MAX_DISTANCE)
                .setDefaultValue(DEFAULT_DISTANCE)
                .setTooltip(new TranslatableText(entityId))
                .setSaveConsumer(newValue -> {
                    entry.setValue(newValue);
                    versionId++;
                })
                .build()
            );
        }
        
        return builder.build();
    }

    private class EntityTypeJson implements JsonSerializer<EntityType<?>>, JsonDeserializer<EntityType<?>> {
        @Override
        public JsonElement serialize(EntityType<?> entity, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(EntityType.getId(entity).toString());
        }

        @Override
        public EntityType<?> deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
            String entityId = json.getAsString();
            Optional<EntityType<?>> entityType = EntityType.get(entityId);
            if (!entityType.isPresent()) {
                System.err.printf("[Entity Distance Mod] Unable to find entity corresponding to id \"%s\" in config file\n", entityId);
                return null;
            }

            return entityType.get();
        }
    }

    private void saveConfig() {
        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            Map<EntityType<?>, Integer> changedDistances = new LinkedHashMap<>();
            for (Map.Entry<EntityType<?>, Integer> entry : distances.entrySet()) {
                if (entry.getValue() != DEFAULT_DISTANCE) {
                    changedDistances.put(entry.getKey(), entry.getValue());
                }
            }

            Gson gson = new GsonBuilder()
                .registerTypeAdapter(EntityType.class, new EntityTypeJson())
                .enableComplexMapKeySerialization()
                .create();
        
            gson.toJson(changedDistances, writer);
        } catch (IOException e) {
            System.err.printf("[Entity Distance Mod] Unable to write Entity Distance Mod config to %s\n\tException is: %s\n", CONFIG_PATH.toString(), e.toString());
        }
    }

    private void loadConfig() {
        try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
            Gson gson = new GsonBuilder()
                .registerTypeAdapter(EntityType.class, new EntityTypeJson())
                .enableComplexMapKeySerialization()
                .create();
        
            Type mapType = TypeToken.getParameterized(HashMap.class, EntityType.class, Integer.class).getType();
            Map<EntityType<?>, Integer> savedDistances = gson.fromJson(reader, mapType);
            for (Map.Entry<EntityType<?>, Integer> entry : savedDistances.entrySet()) {
                if (entry.getKey() != null) {
                    distances.put(entry.getKey(), entry.getValue());
                }
            }

            versionId++;
        } catch (Exception e) {
            System.err.printf("[Entity Distance Mod] Unable to read Entity Distance Mod config at %s\n\tException is: %s\n", CONFIG_PATH.toString(), e.toString());
        }
    }
}
