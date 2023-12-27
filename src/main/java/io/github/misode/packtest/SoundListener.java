package io.github.misode.packtest;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class SoundListener {
    private static final List<SoundListener> listeners = Lists.newArrayList();

    public static void broadcast(Player player, Vec3 pos, Holder<SoundEvent> holder) {
        String playerName = player == null ? null : player.getName().getString();
        ResourceLocation sound = holder.value().getLocation();
        SoundListener.Event message = new SoundListener.Event(playerName, pos, sound);
        SoundListener.listeners.forEach(l -> l.events.add(message));
    }

    public SoundListener() {
        SoundListener.listeners.add(this);
    }

    public final List<SoundListener.Event> events = Lists.newArrayList();

    public void stop() {
        SoundListener.listeners.remove(this);
    }

    public List<String> filter(Predicate<SoundListener.Event> predicate) {
        return this.events.stream()
                .filter(predicate)
                .map(m -> m.sound.toString())
                .toList();
    }

    public void reset() {
        this.events.clear();
    }

    public record Event(@Nullable String player, Vec3 pos, ResourceLocation sound) {}
}
