package io.github.misode.packtest;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Records system messages sent to players so chat assertions can check them.
 */
public final class ChatRecorder {
    private static final int RETENTION_LIMIT = 4096;

    private record Message(UUID recipient, long sequence, String text) {
    }

    private static final Deque<Message> MESSAGES = new ArrayDeque<>();
    private static long sequence = 0;

    private ChatRecorder() {
    }

    public static void record(UUID recipient, String text) {
        if (MESSAGES.size() >= RETENTION_LIMIT) {
            MESSAGES.removeFirst();
        }

        MESSAGES.add(new Message(recipient, ++sequence, text));
    }

    public static long sequence() {
        return sequence;
    }

    public static Stream<String> since(long sequence) {
        return MESSAGES.stream().filter(message -> message.sequence() > sequence).map(Message::text);
    }

    public static Stream<String> since(long sequence, UUID recipient) {
        return MESSAGES.stream().filter(message -> message.sequence() > sequence && message.recipient().equals(recipient)).map(Message::text);
    }
}
