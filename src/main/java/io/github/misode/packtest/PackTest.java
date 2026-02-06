package io.github.misode.packtest;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.mojang.brigadier.CommandDispatcher;
import io.github.misode.packtest.commands.*;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.util.FileUtil;
import net.minecraft.util.Util;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.synchronization.ArgumentUtils;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.ToIntFunction;

public class PackTest implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(PackTest.class);

	public static boolean isAutoEnabled() {
		return System.getProperty("packtest.auto") != null;
	}

	public static boolean isAutoColoringEnabled() {
		return isAutoEnabled() && !"false".equals(System.getProperty("packtest.auto.coloring"));
	}

	public static boolean isAnnotationsEnabled() {
		return isAutoEnabled() && System.getProperty("packtest.auto.annotations") != null;
	}

	public static boolean shouldGenerateCommands() {
		return System.getProperty("packtest.generate.commands") != null;
	}

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, environment) ->
			registerCommands(dispatcher, buildContext)
		);
    }

	public static void generateCommandsReport() {
		Path path = Paths.get("generated", "reports", "commands.json");
		CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
		registerCommands(dispatcher, Commands.createValidationContext(VanillaRegistries.createLookup()));
		JsonObject data = ArgumentUtils.serializeNodeToJson(dispatcher, dispatcher.getRoot());
		ToIntFunction<String> fixedOrderFields = Util.make(new Object2IntOpenHashMap<>(), map -> {
			map.put("type", 0);
			map.put("parser", 1);
			map.put("properties", 2);
			map.put("executable", 3);
			map.put("redirect", 4);
			map.put("children", 5);
			map.defaultReturnValue(6);
		});
		Comparator<String> keyComparator = Comparator.comparingInt(fixedOrderFields).thenComparing(string -> string);
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
				writer.setSerializeNulls(false);
				writer.setIndent("  ");
				GsonHelper.writeValue(writer, data, keyComparator);
			}
			FileUtil.createDirectoriesSafe(path.getParent());
			Files.write(path, outputStream.toByteArray());
			LOGGER.info("Saved file to {}", path);
		} catch (IOException e) {
			LOGGER.error("Failed to save file to {}", path, e);
		}
	}

	private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
		AssertCommand.register(dispatcher, buildContext);
		AwaitCommand.register(dispatcher, buildContext);
		FailCommand.register(dispatcher, buildContext);
		DummyCommand.register(dispatcher);
		SucceedCommand.register(dispatcher);
	}

	public static void runHeadlessServer(LevelStorageSource.LevelStorageAccess storage, PackRepository packRepository) {
		GameTestServer.spin(thread -> GameTestServer.create(thread, storage, packRepository, Optional.empty(), false, 0));
	}

	public static String wrapError(String message) {
		if (isAutoColoringEnabled()) {
			return "\u001b[0;31m" + message + "\u001b[0m";
		}
		return message;
	}

	public static String wrapWarning(String message) {
		if (isAutoColoringEnabled()) {
			return "\u001b[0;33m" + message + "\u001b[0m";
		}
		return message;
	}
}
