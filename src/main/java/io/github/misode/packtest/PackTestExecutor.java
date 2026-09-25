package io.github.misode.packtest;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.misode.packtest.commands.assertions.AssertResult;
import io.github.misode.packtest.dummy.Dummy;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class PackTestExecutor {
    private static final SimpleCommandExceptionType ERROR_NOT_IN_TEST = new SimpleCommandExceptionType(
            Component.literal("Command can only be used inside a test"));
    private static @Nullable PackTestExecutor current;

    private final List<Supplier<Boolean>> awaits = new ArrayList<>();
    private final GameTestHelper helper;
    private final int timeout;
    private final long chatSequence = ChatRecorder.sequence();
    private int line = 0;
    private boolean done = false;

    public PackTestExecutor(GameTestHelper helper, int timeout) {
        this.helper = helper;
        this.timeout = timeout;
    }

    public static PackTestExecutor current() throws CommandSyntaxException {
        if (current == null) {
            throw ERROR_NOT_IN_TEST.create();
        }
        return current;
    }

    public void run(PackTestFunction function) {
        CommandSourceStack source = createCommandSourceStack(function);
        Queue<PackTestFunction.Step> steps = new ArrayDeque<>(function.steps());

        Runnable tick = () -> {
            current = this;

            try {
                if (!this.awaits.isEmpty() && this.awaits.getFirst().get()) {
                    this.awaits.removeFirst();
                }
                while (!steps.isEmpty() && !this.done && this.awaits.isEmpty()) {
                    PackTestFunction.Step step = steps.poll();
                    this.line = step.line();
                    Commands.executeCommandInContext(source, ctx ->
                            ExecutionContext.queueInitialCommandExecution(ctx, step.command(), step.chain(), source, CommandResultCallback.EMPTY));
                }
                if (!this.done && this.awaits.isEmpty()) {
                    this.succeed();
                }
            } finally {
                current = null;
            }
        };

        this.helper.onEachTick(tick);
        this.helper.runAtTickTime(this.timeout, tick);
    }

    private CommandSourceStack createCommandSourceStack(PackTestFunction function) {
        CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack()
                .withLevel(helper.getLevel())
                .withPosition(helper.absoluteVec(Vec3.ZERO))
                .withSuppressedOutput();

        Optional<Coordinates> coordinates = this.getDummyPos(function);
        if (coordinates.isPresent()) {
            try {
                Vec3 pos = coordinates.get().getPosition(source);
                Vec2 rot = coordinates.get().getRotation(source);
                Dummy dummy = Dummy.createRandom(helper.getLevel(), pos, rot);
                dummy.setOnGround(true); // little hack because we know the dummy will be on the ground
                source = source.withEntity(dummy);
            } catch (IllegalArgumentException e) {
                helper.fail(Component.literal("Failed to initialize test with dummy"));
            }
        }

        return source;
    }

    private Optional<Coordinates> getDummyPos(PackTestFunction function) {
        String dummyValue = function.directives().get("dummy");
        if (dummyValue == null) {
            return Optional.empty();
        }
        if (dummyValue.equals("true")) {
            dummyValue = "~0.5 ~ ~0.5";
        }
        try {
            return Optional.of(Vec3Argument.vec3().parse(new StringReader(dummyValue)));
        } catch (CommandSyntaxException e) {
            return Optional.empty();
        }
    }

    public void succeed() {
        this.done = true;
        this.helper.succeed();
    }

    public void fail(Component message) {
        this.done = true;
        throw failure(message);
    }

    public void await(int delay) {
        AtomicInteger remaining = new AtomicInteger(delay);
        this.awaits.add(() -> {
            if (remaining.decrementAndGet() <= 0) {
                return true;
            }
            if (this.helper.getTick() + remaining.get() > this.timeout) {
                throw failure(Component.literal("Exceeded timeout"));
            }
            return false;
        });
    }

    public int assertThat(AssertResult result, boolean negated) {
        if (satisfied(result, negated)) return negated ? 1 : result.count();
        fail(result.message().apply(negated));
        return 0;
    }

    public void awaitThat(AssertResult first, Supplier<AssertResult> check, boolean negated) {
        if (satisfied(first, negated)) return;
        registerPoll(check, negated);
    }

    private boolean satisfied(AssertResult result, boolean negated) {
        return negated ? result.count() == 0 && !result.errored() : result.count() > 0;
    }

    private void registerPoll(Supplier<AssertResult> check, boolean negated) {
        this.awaits.add(() -> {
            AssertResult retry = check.get();
            if (satisfied(retry, negated)) return true;
            if (!isLastTick()) return false;
            throw failure(retry.message().apply(negated));
        });
    }

    private PackTestException failure(Component message) {
        return new PackTestException(message, (int)this.helper.getTick(), this.line);
    }

    private boolean isLastTick() {
        return this.helper.getTick() + 1 >= this.timeout;
    }

    public AABB getBounds() {
        return this.helper.getBounds();
    }

    public Stream<String> chatMessages() {
        return ChatRecorder.since(this.chatSequence);
    }

    public Stream<String> chatMessages(UUID recipient) {
        return ChatRecorder.since(this.chatSequence, recipient);
    }
}
