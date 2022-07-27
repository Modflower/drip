package gay.ampflower.drip;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The configuration class for Drip.
 *
 * @author Ampflower
 * @since ${version}
 **/
public class Config {
	private static final Logger logger = LogUtils.getLogger();
	/**
	 * The location of the config
	 */
	public static final Path config = FabricLoader.getInstance().getConfigDir().resolve("drip.json");

	/**
	 * An instance of GSON that allows for using complex keys in maps, pretty
	 * printing for ease of reading and editing, lenient parsing to allow for
	 * comments &amp; any errors that maybe introduced by the end user, and an
	 * {@link Identifier} type adaptor to read & write identifiers correctly.
	 */
	private static final Gson GSON = new GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting()
			.setLenient().registerTypeAdapter(Identifier.class, new IdentifierTypeAdaptor()).create();

	/**
	 * The live config instance.
	 */
	public static final Config INSTANCE;

	static {
		Config instance;
		if (Files.exists(config)) {
			try (var reader = Files.newBufferedReader(config)) {
				instance = GSON.fromJson(reader, Config.class);
				instance.finishRead();
			} catch (IOException ioe) {
				throw new ExceptionInInitializerError(ioe);
			}
		} else {
			// Initialises default map
			instance = new Config();
			var map = instance.entries = new HashMap<>();
			map.put(Blocks.MUD, new Entry(Fluids.WATER, false));
			map.put(Blocks.ICE, new Entry(Fluids.WATER, false));
			map.put(Blocks.PACKED_ICE, new Entry(Fluids.WATER, false));
			map.put(Blocks.BLUE_ICE, new Entry(Fluids.WATER, false));
			map.put(Blocks.MAGMA_BLOCK, new Entry(Fluids.LAVA, false));
			instance.startWrite();

			// The Fniges are a bad influence sometimes.
			// https://cdn.discordapp.com/emojis/834191878416105502.png

			// Random easter egg from leaving a random comment in the source code.
			instance.entriesRaw.put(new Identifier("ringlings", "gem"), new EntryRaw(new Identifier("water"), false));

			// Write to the file
			try (var writer = Files.newBufferedWriter(config, StandardOpenOption.CREATE)) {
				GSON.toJson(instance, writer);
			} catch (IOException ioe) {
				throw new ExceptionInInitializerError(ioe);
			}
			instance.entriesRaw = null;
		}
		INSTANCE = instance;
		Objects.requireNonNull(INSTANCE, "Bugcheck: INSTANCE");
		Objects.requireNonNull(INSTANCE.entries, "Bugcheck: INSTANCE#entries");
	}

	/**
	 * Intermediate representation of {@link #entries} for GSON.
	 */
	@SerializedName("entries")
	private Map<Identifier, EntryRaw> entriesRaw;

	/**
	 * Primary storage of block to fluid entries.
	 */
	public transient Map<Block, Entry> entries;

	/**
	 * Finishes reading the config by converting {@link #entriesRaw} to
	 * {@link #entries} for runtime usage. {@link #entriesRaw} is not preserved.
	 */
	private void finishRead() {
		Objects.requireNonNull(entriesRaw, "drip.json: entries");
		entries = new HashMap<>();
		for (Map.Entry<Identifier, EntryRaw> entry : entriesRaw.entrySet()) {
			final var id = entry.getKey();
			final var block = Registry.BLOCK.getOrEmpty(id);
			final var value = entry.getValue().toEntry();
			if (block.isEmpty()) {
				logger.warn("Unknown block {} for {}", id, value);
			} else {
				entries.put(block.get(), value);
			}
		}
		entriesRaw = null;
	}

	/**
	 * Starts writing the config by converting {@link #entries} to
	 * {@link #entriesRaw} for writing with GSON. {@link #entries} is preserved as
	 * is.
	 */
	private void startWrite() {
		Objects.requireNonNull(entries, "Bugcheck failure");
		entriesRaw = new HashMap<>();
		entries.forEach((block, entry) -> entriesRaw.put(Registry.BLOCK.getId(block), entry.toRaw()));
	}

	/**
	 * Intermediate form of {@link Entry} for GSON to read from and write to.
	 */
	// Intentionally not a record for GSON.
	@SuppressWarnings("ClassCanBeRecord")
	private static final class EntryRaw {
		private final Identifier fluid;
		private final boolean replace;

		/**
		 * @param fluid
		 *            The identifier for {@link Fluids}.
		 * @param replace
		 *            Whether the fluid replaces vanilla's entry.
		 */
		private EntryRaw(Identifier fluid, boolean replace) {
			this.fluid = fluid;
			this.replace = replace;
		}

		/**
		 * Converts the record into a regular {@link Entry} for runtime usage.
		 */
		Entry toEntry() {
			return new Entry(Registry.FLUID.get(fluid), replace);
		}
	}

	/**
	 * Entry of {@link Fluid} and whether it should replace Vanilla.
	 *
	 * @param fluid
	 *            The fluid for the block.
	 * @param replace
	 *            Whether the fluid replaces vanilla's entry.
	 */
	public record Entry(Fluid fluid, boolean replace) {
		/**
		 * Converts the record into {@link EntryRaw} for write-time usage.
		 */
		EntryRaw toRaw() {
			return new EntryRaw(Registry.FLUID.getId(fluid), replace);
		}
	}

	/**
	 * {@link Identifier} type adaptor to allow for reading &amp; writing of
	 * identifiers as keys &amp; values.
	 */
	private static class IdentifierTypeAdaptor implements JsonDeserializer<Identifier>, JsonSerializer<Identifier> {
		@Override
		public Identifier deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			return Identifier.tryParse(json.getAsString());
		}

		@Override
		public JsonElement serialize(Identifier src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive(src.toString());
		}
	}
}
