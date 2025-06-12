package top.bearcabbage.catbinding;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CatBinding implements ModInitializer {
	public static final String MOD_ID = "catbinding";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final CBConfig config = new CBConfig(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve("config.json"));

	public static final NbtCompound EMPTY_NBT = new NbtCompound() {{this.putString("Binding", "Empty");}};
	public static final NbtCompound ALL_NBT = new NbtCompound() {{this.putString("Binding", "All");}};
	public static HashMap<Item, Set<NbtCompound>> BINDING_DATA = new HashMap<>();

	public static Set<Item> BINDING_ITEMS;

	@Override
	public void onInitialize() {
		loadBindingData();
		CatBindingCommands.init();
	}

	public static int loadBindingData() {
		LOGGER.info("[CatBinding] Loading Cat Binding Mod");
		HashMap<String, Set<String>> bindingReads = config.getOrDefault("Banding-Items", new HashMap<>());
		BINDING_DATA.clear();
		for (Map.Entry<String, Set<String>> entry : bindingReads.entrySet()) {
			String[] parts = entry.getKey().split(":");
			Item item;
			if (parts.length == 2) {
				item = Registries.ITEM.get(Identifier.of(parts[0], parts[1]));
				if (item == Items.AIR) {
					LOGGER.warn("[CatBinding] Item {} not found. Ignored.", entry.getKey());
					continue;
				}
			}
			else if (parts.length == 1) {
				item = Registries.ITEM.get(Identifier.of("minecraft", parts[0]));
			} else {
				LOGGER.warn("[CatBinding] Invalid item identifier: {}.  Ignored.", entry.getKey());
				continue;
			}
			Set<String> readingNbtSet = new HashSet<>(entry.getValue());
			Set<NbtCompound> itemNbtSet = BINDING_DATA.getOrDefault(item, new HashSet<>());
			if (itemNbtSet.contains(ALL_NBT)) {
				continue;
			}
			if (readingNbtSet.isEmpty() || readingNbtSet.contains(ALL_NBT.toString())) {
				itemNbtSet.clear();
				itemNbtSet.add(ALL_NBT);
			} else {
				for (String itemNbt : readingNbtSet) {
					try {
						NbtCompound nbt = StringNbtReader.parse(itemNbt);
						itemNbtSet.add(nbt);
					} catch (Exception e) {
						LOGGER.warn("[CatBinding] Invalid NBT for item {}: {}. Nbt compound Ignored. List this item with empty nbt tags.", entry.getKey(), e.getMessage());
						itemNbtSet.add(EMPTY_NBT);
					}
				}
			}
			BINDING_DATA.put(item, itemNbtSet);
		}
		LOGGER.info("[CatBinding] Loaded {} binding items.", BINDING_DATA.size());
		return BINDING_DATA.size();
	}

	public static int saveBindingData() {
		HashMap<String, Set<String>> bindingWrites = new HashMap<>();
		for (Map.Entry<Item, Set<NbtCompound>> entry : BINDING_DATA.entrySet()) {
			String itemKey = Registries.ITEM.getId(entry.getKey()).toString();
			Set<String> nbtSet = new HashSet<>();
			for (NbtCompound nbt : entry.getValue()) {
				if (nbt.equals(ALL_NBT)) {
					nbtSet.add(ALL_NBT.toString());
				} else if (nbt.equals(EMPTY_NBT)) {
					continue; // Skip empty NBT
				} else {
					nbtSet.add(nbt.toString());
				}
			}
			bindingWrites.put(itemKey, nbtSet);
		}
		config.set("Banding-Items", bindingWrites);
		config.save();
		return bindingWrites.size();
	}

	private static class CBConfig {
		private final Path filePath;
		private JsonObject jsonObject;
		private final Gson gson;

		public CBConfig(Path filePath) {
			this.filePath = filePath;
			this.gson = new GsonBuilder().setPrettyPrinting().create();
			try {
				if (Files.notExists(filePath.getParent())) {
					Files.createDirectories(filePath.getParent());
				}
				if (Files.notExists(filePath)) {
					Files.createFile(filePath);
					try (FileWriter writer = new FileWriter(filePath.toFile())) {
						writer.write("{}");
					}
				}

			} catch (IOException e) {
				LOGGER.error(e.toString());
			}
			load();
		}

		public void load() {
			try (FileReader reader = new FileReader(filePath.toFile())) {
				this.jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
			} catch (IOException e) {
				this.jsonObject = new JsonObject();
			}
		}

		public void save() {
			try (FileWriter writer = new FileWriter(filePath.toFile())) {
				gson.toJson(jsonObject, writer);
			} catch (IOException e) {
				LOGGER.error(e.toString());
			}
		}

		public void set(String key, Object value) {
			jsonObject.add(key, gson.toJsonTree(value));
		}

		public <T> T get(String key, Class<T> clazz) {
			return gson.fromJson(jsonObject.get(key), clazz);
		}

		public <T> T getOrDefault(String key, T defaultValue) {
			if (jsonObject.has(key)) {
				return gson.fromJson(jsonObject.get(key), (Class<T>) defaultValue.getClass());
			}
			else {
				set(key, defaultValue);
				save();
				return defaultValue;
			}
		}

		public <T> T getAll(Class<T> clazz) {
			return gson.fromJson(jsonObject, clazz);
		}
	}
}