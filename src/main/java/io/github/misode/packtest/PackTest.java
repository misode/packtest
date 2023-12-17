package io.github.misode.packtest;

import io.github.misode.packtest.commands.AssertCommand;
import io.github.misode.packtest.commands.FailCommand;
import io.github.misode.packtest.commands.SucceedCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.dedicated.DedicatedServer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PackTest implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(PackTest.class);
	public @Nullable PackTestServer testServer;

	public static boolean isAutoEnabled() {
		return System.getProperty("packtest.auto") != null;
	}

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, environment) -> {
			AssertCommand.register(dispatcher, buildContext);
			FailCommand.register(dispatcher);
			SucceedCommand.register(dispatcher);
		});

		if (PackTest.isAutoEnabled()) {
			ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
				if (server instanceof DedicatedServer dedicatedServer) {
					this.testServer = new PackTestServer(dedicatedServer);
					this.testServer.runTests();
				}
			});
			ServerTickEvents.END_SERVER_TICK.register((server) -> {
				if (testServer != null) {
					testServer.tick();
				}
			});
		}
	}
}
