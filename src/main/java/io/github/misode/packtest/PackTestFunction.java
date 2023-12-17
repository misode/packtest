package io.github.misode.packtest;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.*;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PackTestFunction {
    private static final Pattern DIRECTIVE_PATTERN = Pattern.compile("#\\s*@(\\w+)\\s+(\\S+)");
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
                directives.put(key, value);
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

    public TestFunction toTestFunction(int permissionLevel, CommandDispatcher<CommandSourceStack> dispatcher) {
        Rotation rotation = StructureUtils.getRotationForRotationSteps(0);

        return new TestFunction(DEFAULT_BATCH, this.getTestName(), this.getTemplateName(), rotation, 100, 0L, true, 1, 1, (helper) -> {
            if (function == null) {
                helper.fail(this.parseError != null ? this.parseError : "Failed to parse test function");
                return;
            }

            AtomicBoolean hasFailed = new AtomicBoolean(false);
            CommandSourceStack sourceStack = helper.getLevel().getServer().createCommandSourceStack()
                    .withPosition(helper.absoluteVec(Vec3.ZERO).add(0, 1, 0))
                    .withPermission(permissionLevel)
                    .withSuppressedOutput()
                    .withCallback((success, result) -> hasFailed.set(!success));

            try {
                InstantiatedFunction<CommandSourceStack> instantiatedFn = function.instantiate(null, dispatcher, sourceStack);
                Commands.executeCommandInContext(sourceStack, execution -> ExecutionContext.queueInitialFunctionCall(
                        execution,
                        instantiatedFn,
                        sourceStack,
                        CommandResultCallback.EMPTY));
            } catch (FunctionInstantiationException e) {
                String message = e.messageComponent().getString();
                helper.fail("Failed to instantiate test function: " + message);
                return;
            }

            if (hasFailed.get()) {
                helper.fail("Test failed without a message");
            } else {
                helper.succeed();
            }
        });
    }
}
