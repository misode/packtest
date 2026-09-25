package io.github.misode.packtest;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestEnvironments;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class PackTestDirectives {
	private Identifier environment = GameTestEnvironments.DEFAULT_KEY.identifier();
	private Identifier dimension = Level.OVERWORLD.identifier();
	private Identifier structure = Identifier.withDefaultNamespace("empty");
	private int maxTicks = 100;
	private int setupTicks = 0;
	private boolean required = true;
	private boolean skyAccess = false;
	private Rotation rotation = Rotation.NONE;
	private int maxAttempts = 1;
	private boolean manualOnly = false;
	private int requiredSuccesses = 1;
	private int padding = 0;
	private @Nullable Coordinates dummy = null;

	public void add(String name, @Nullable String value) {
		switch (name.toLowerCase(Locale.ROOT)) {
			case "environment" ->
					this.environment = Identifier.parse(require(value));
			case "dimension" ->
					this.dimension = Identifier.parse(require(value));
			case "template", "structure" ->
					this.structure = Identifier.parse(require(value));
			case "timeout", "max_ticks" ->
					this.maxTicks = parsePositiveInt(value);
			case "setup_ticks" ->
					this.setupTicks = parseNonNegativeInt(value);
			case "optional" ->
					this.required = !parseBoolean(value);
			case "skyaccess", "sky_access" ->
					this.skyAccess = parseBoolean(value);
			case "rotation" ->
					this.rotation = parseRotation(value);
			case "manual", "manual_only" ->
					this.manualOnly = parseBoolean(value);
			case "max_attempts" ->
					this.maxAttempts = parsePositiveInt(value);
			case "required_successes" ->
					this.requiredSuccesses = parsePositiveInt(value);
			case "padding" -> {
				this.padding = parseNonNegativeInt(value);
				if (this.padding > 128) throw new IllegalArgumentException("Padding must be between 0 and 128");
			}
			case "dummy" -> {
				try {
					String pos = Objects.requireNonNullElse(value, "~ ~ ~");
					this.dummy = Vec3Argument.vec3().parse(new StringReader(pos));
				} catch (CommandSyntaxException e) {
					throw new IllegalArgumentException(e.getMessage());
				}
			}
			default -> throw new IllegalArgumentException("Unknown directive");
		}
	}

	public TestData<Holder<TestEnvironmentDefinition<?>>> createTestData(Registry<TestEnvironmentDefinition<?>> environments) {
		return new TestData<>(
				environments.getOrThrow(ResourceKey.create(Registries.TEST_ENVIRONMENT, this.environment)),
				ResourceKey.create(Registries.DIMENSION, this.dimension),
				this.structure,
				this.maxTicks,
				this.setupTicks,
				this.required,
				this.rotation,
				this.manualOnly,
				this.maxAttempts,
				this.requiredSuccesses,
				this.skyAccess,
				this.padding);
	}

	public int maxTicks() {
		return this.maxTicks;
	}

	public Optional<Coordinates> dummy() {
		return Optional.ofNullable(this.dummy);
	}

	private static Rotation parseRotation(@Nullable String value) {
		int degrees = Integer.parseInt(require(value));
		return switch (Math.floorMod(degrees, 360)) {
			case 0 -> Rotation.NONE;
			case 90 -> Rotation.CLOCKWISE_90;
			case 180 -> Rotation.CLOCKWISE_180;
			case 270 -> Rotation.COUNTERCLOCKWISE_90;
			default -> throw new IllegalArgumentException("Rotation must be a multiple of 90 degrees");
		};
	}

	private static int parsePositiveInt(@Nullable String value) {
		int parsed = Integer.parseInt(require(value));
		if (parsed <= 0) throw new IllegalArgumentException("Value must be positive");
		return parsed;
	}

	private static int parseNonNegativeInt(@Nullable String value) {
		int parsed = Integer.parseInt(require(value));
		if (parsed < 0) throw new IllegalArgumentException("Value must not be negative");
		return parsed;
	}

	private static boolean parseBoolean(@Nullable String value) {
		return value == null || Boolean.parseBoolean(value.trim());
	}

	private static String require(@Nullable String value) {
		if (value == null) throw new IllegalArgumentException("Missing value");
		return value.trim();
	}
}

