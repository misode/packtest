package io.github.misode.packtest;

import io.github.misode.packtest.commands.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PackTest implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(PackTest.class);

	public static boolean isAutoEnabled() {
		return System.getProperty("packtest.auto") != null;
	}

	public static boolean isAutoColoringEnabled() {
		return !"false".equals(System.getProperty("packtest.auto.coloring"));
	}

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, environment) -> {
			AssertCommand.register(dispatcher, buildContext);
			AwaitCommand.register(dispatcher, buildContext);
			FailCommand.register(dispatcher);
			DummyCommand.register(dispatcher);
			SucceedCommand.register(dispatcher);
		});
	}

	public static void runHeadlessServer(LevelStorageSource.LevelStorageAccess storage, PackRepository packRepository) {
		GameTestServer.spin(thread -> GameTestServer.create(thread, storage, packRepository, List.of(), BlockPos.ZERO));
	}
}
