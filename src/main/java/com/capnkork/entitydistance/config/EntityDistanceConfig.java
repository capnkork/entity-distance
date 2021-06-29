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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

    private static class EntityTypeComparator implements Comparator<EntityType<?>> {
        @Override
        public int compare(EntityType<?> a, EntityType<?> b) {
            return EntityType.getId(a).compareTo(EntityType.getId(b));
        }
    }

    private final Map<EntityType<?>, Integer> distances = new LinkedHashMap<>();
    private final Map<String, Integer> unknownConfig = new LinkedHashMap<>();

    private EntityDistanceConfig() {
        // get all types
        List<EntityType<?>> types = new ArrayList<>();
        for (Map.Entry<RegistryKey<EntityType<?>>, EntityType<?>> entry : Registry.ENTITY_TYPE.getEntries()) {
            types.add(entry.getValue());
        }

        // sort alphabetically
        types.sort(new EntityTypeComparator());

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

        builder.setSavingRunnable(this::saveConfig);

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

    private void saveConfig() {
        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            // add values changed from defaults to map
            Map<String, Integer> outMap = new LinkedHashMap<>();
            for (Map.Entry<EntityType<?>, Integer> entry : distances.entrySet()) {
                if (entry.getValue() != DEFAULT_DISTANCE) {
                    outMap.put(EntityType.getId(entry.getKey()).toString(), entry.getValue());
                }
            }

            // add values from original config that were not found as entities
            for (Map.Entry<String, Integer> entry : unknownConfig.entrySet()) {
                outMap.put(entry.getKey(), entry.getValue());
            }

            Gson gson = new GsonBuilder().create();
            gson.toJson(outMap, writer);
        } catch (IOException e) {
            System.err.printf(
                "[Entity Distance Mod] Unable to write Entity Distance Mod config to %s\n\tException is: %s\n",
                CONFIG_PATH,
                e
            );
        }
    }

    private void loadConfig() {
        try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
            Gson gson = new GsonBuilder().create();
        
            Type mapType = TypeToken.getParameterized(HashMap.class, String.class, Integer.class).getType();
            Map<String, Integer> savedDistances = gson.fromJson(reader, mapType);
            for (Map.Entry<String, Integer> entry : savedDistances.entrySet()) {
                Optional<EntityType<?>> entityType = EntityType.get(entry.getKey());
                if (entityType.isPresent()) {
                    distances.put(entityType.get(), entry.getValue());
                } else {
                    System.err.printf(
                        "[Entity Distance Mod] Unable to find entity corresponding to id \"%s\" in config file\n",
                        entry.getKey()
                    );
                    // keep values from config so they are not lost
                    unknownConfig.put(entry.getKey(), entry.getValue());
                }
            }

            versionId++;
        } catch (Exception e) {
            System.err.printf(
                "[Entity Distance Mod] Unable to read Entity Distance Mod config at %s\n\tException is: %s\n",
                CONFIG_PATH,
                e
            );
        }
    }
}
