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

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PackTestFunction {
    private static final Pattern DIRECTIVE_PATTERN = Pattern.compile("#\\s*@(\\w+)\\s+(\\S+)");
    private static final String DEFAULT_BATCH = "packtestBatch";
    private static final String DEFAULT_TEMPLATE = "packtest:empty";
    private final String templateName;
    private final CommandFunction<CommandSourceStack> function;

    public PackTestFunction(String templateName, CommandFunction<CommandSourceStack> function) {
        this.templateName = templateName;
        this.function = function;
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
        String templateName = directives.getOrDefault("template", DEFAULT_TEMPLATE);
        CommandSourceStack sourceStack = new CommandSourceStack(
                CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null, 2, "", CommonComponents.EMPTY, null, null
        );
        CommandFunction<CommandSourceStack> function = CommandFunction.fromLines(id, dispatcher, sourceStack, lines);
        return new PackTestFunction(templateName, function);
    }

    private String getTestName() {
        return this.function.id().toLanguageKey();
    }

    public TestFunction toTestFunction(int permissionLevel, CommandDispatcher<CommandSourceStack> dispatcher) {
        Rotation rotation = StructureUtils.getRotationForRotationSteps(0);

        return new TestFunction(DEFAULT_BATCH, this.getTestName(), this.templateName, rotation, 100, 0L, true, 1, 1, (helper) -> {
            AtomicBoolean hasFailed = new AtomicBoolean(false);
            CommandSourceStack sourceStack = helper.getLevel().getServer().createCommandSourceStack()
                    .withPosition(helper.absoluteVec(Vec3.ZERO))
                    .withPermission(permissionLevel)
                    .withSuppressedOutput()
                    .withCallback((success, result) -> hasFailed.set(!success));
            try {
                AtomicReference<String> failMessage = new AtomicReference<>("Test failed");
                PackTestLibrary.INSTANCE.setMessageConsumer(failMessage::set);
                InstantiatedFunction<CommandSourceStack> instantiatedFn = function.instantiate(null, dispatcher, sourceStack);
                Commands.executeCommandInContext(sourceStack, execution -> {
                    ExecutionContext.queueInitialFunctionCall(execution, instantiatedFn, sourceStack, CommandResultCallback.EMPTY);
                });
                if (hasFailed.get()) {
                    helper.fail(failMessage.toString());
                } else {
                    helper.succeed();
                }
            } catch (FunctionInstantiationException e) {
                String message = e.messageComponent().getString();
                helper.fail("Failed to instantiate test function: " + message);
            }
        });
    }
}
