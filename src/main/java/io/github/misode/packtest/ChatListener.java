package io.github.misode.packtest;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ChatListener {

    private static final List<ChatListener> listeners = new ArrayList<>();

    public static void broadcast(ServerPlayer player, Component chatMessage) {
        Message message = new Message(player.getName().getString(), chatMessage.getString());
        ChatListener.listeners.forEach(l -> l.messages.add(message));
    }

    public ChatListener() {
        ChatListener.listeners.add(this);
    }

    public final List<Message> messages = new ArrayList<>();

    public void stop() {
        ChatListener.listeners.remove(this);
    }

    public List<String> filter(Predicate<Message> predicate) {
        return this.messages.stream()
                .filter(predicate)
                .map(m -> m.content)
                .toList();
    }

    public void reset() {
        this.messages.clear();
    }

    public record Message(String player, String content) {}
}
