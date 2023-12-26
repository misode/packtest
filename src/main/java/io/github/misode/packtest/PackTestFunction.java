package io.github.misode.packtest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.misode.packtest.dummy.Dummy;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PackTestFunction {
    private static final Pattern DIRECTIVE_PATTERN = Pattern.compile("^#\\s*@(\\w+)(?:\\s+(.+))?$");
    private static final String DEFAULT_BATCH = "packtestBatch";
    private static final String DEFAULT_TEMPLATE = "packtest:empty";
    private final ResourceLocation id;
    private final Map<String, String> directives;
    private final @Nullable CommandFunction<CommandSourceStack> function;
    private final @Nullable String parseError;

    public PackTestFunction(ResourceLocation id, Map<String, String> directives, @Nullable CommandFunction<CommandSourceStack> function, @Nullable String parseError) {
        this.id = id;
        this.directives = directives;
        this.function = function;
        this.parseError = parseError;
    }

    public static PackTestFunction fromLines(ResourceLocation id, CommandDispatcher<CommandSourceStack> dispatcher, List<String> lines) {
        HashMap<String, String> directives = new HashMap<>();
        for (String line : lines) {
            if (!line.startsWith("#")) {
                break;
            }
            Matcher matcher = DIRECTIVE_PATTERN.matcher(line);
            if (matcher.find()) {
                String key = matcher.group(1);
                String value = matcher.group(2);
                directives.put(key, value != null ? value : "true");
            }
        }

        CommandSourceStack sourceStack = new CommandSourceStack(
                CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null, 2, "", CommonComponents.EMPTY, null, null
        );
        CommandFunction<CommandSourceStack> function = null;
        String parseError = null;
        try {
            function = CommandFunction.fromLines(id, dispatcher, sourceStack, lines);
        } catch (IllegalArgumentException e) {
            parseError = e.getMessage();
        }

        return new PackTestFunction(id, directives, function, parseError);
    }

    private String getTestName() {
        return this.id.toLanguageKey();
    }

    private String getTemplateName() {
        return this.directives.getOrDefault("template", DEFAULT_TEMPLATE);
    }

    private String getBatchName() {
        return this.directives.getOrDefault("batch", DEFAULT_BATCH);
    }

    private int getTimeout() {
        return Optional.ofNullable(this.directives.get("timeout")).map(Integer::parseInt).orElse(100);
    }

    private Optional<Vec3> getDummyPos(CommandSourceStack source) {
        String dummyValue = this.directives.get("dummy");
        if (dummyValue == null) {
            return Optional.empty();
        }
        if (dummyValue.equals("true")) {
            dummyValue = "~0.5 ~ ~0.5";
        }
        try {
            return Optional.of(Vec3Argument.vec3().parse(new StringReader(dummyValue)).getPosition(source));
        } catch (CommandSyntaxException e) {
            return Optional.empty();
        }
    }

    private boolean isRequired() {
        return Optional.ofNullable(this.directives.get("optional")).map(s -> !Boolean.parseBoolean(s)).orElse(true);
    }

    public void registerBatchHook(int permissionLevel, Map<String, Consumer<ServerLevel>> map, String type) {
        String command = this.directives.get(type + "batch");
        if (command == null) {
            return;
        }
        String batchName = this.getBatchName();
        Consumer<ServerLevel> oldBefore = map.putIfAbsent(batchName, (level) -> {
            CommandSourceStack source = level.getServer().createCommandSourceStack()
                    .withPermission(permissionLevel);
            level.getServer().getCommands().performPrefixedCommand(source, command);
        });
        if (oldBefore != null) {
            PackTest.LOGGER.error("Only one @" + type + "batch is allowed per batch. Batch '" + batchName + "' has more than one!");
        }
    }

    public TestFunction toTestFunction(int permissionLevel, CommandDispatcher<CommandSourceStack> dispatcher) {
        return new TestFunction(
                this.getBatchName(),
                this.getTestName(),
                this.getTemplateName(),
                StructureUtils.getRotationForRotationSteps(0),
                this.getTimeout(),
                0L,
                this.isRequired(),
                1,
                1,
                createTestBody(permissionLevel, dispatcher));
    }

    private Consumer<GameTestHelper> createTestBody(int permissionLevel, CommandDispatcher<CommandSourceStack> dispatcher) {
        return (helper) -> {
            if (this.function == null) {
                helper.fail(this.parseError != null ? this.parseError : "Failed to parse test function");
                return;
            }

            AtomicBoolean hasFailed = new AtomicBoolean(false);
            CommandSourceStack sourceStack = helper.getLevel().getServer().createCommandSourceStack()
                    .withPosition(helper.absoluteVec(Vec3.ZERO).add(0, 1, 0))
                    .withPermission(permissionLevel)
                    .withSuppressedOutput()
                    .withCallback((success, result) -> hasFailed.set(!success));

            ((PackTestSourceStack)sourceStack).packtest$setHelper(helper);

            InstantiatedFunction<CommandSourceStack> instantiatedFn;
            try {
                instantiatedFn = function.instantiate(null, dispatcher, sourceStack);
            } catch (FunctionInstantiationException e) {
                String message = e.messageComponent().getString();
                helper.fail("Failed to instantiate test function: " + message);
                return;
            }

            Vec3 dummyPos = this.getDummyPos(sourceStack).orElse(null);
            Dummy dummy;
            if (dummyPos != null) {
                try {
                    dummy = Dummy.createRandom(helper.getLevel().getServer(), helper.getLevel().dimension(), dummyPos);
                    dummy.setOnGround(true); // little hack because we know the dummy will be on the ground
                    sourceStack = sourceStack.withEntity(dummy);
                } catch (IllegalArgumentException e) {
                    helper.fail("Failed to initialize test with dummy");
                }
            }

            CommandSourceStack finalSourceStack = sourceStack;
            Commands.executeCommandInContext(sourceStack, execution -> ExecutionContext.queueInitialFunctionCall(
                    execution,
                    instantiatedFn,
                    finalSourceStack,
                    CommandResultCallback.EMPTY));

            if (hasFailed.get()) {
                helper.fail("Test failed without a message");
            } else if (!((PackTestHelper)helper).packtest$isFinalCheckAdded()) {
                helper.succeed();
            }
        };
    }
}
