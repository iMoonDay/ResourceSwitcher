package com.imoonday.resource_switcher.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Environment(EnvType.CLIENT)
public class ResourcePackGroup {

    private String id;
    private List<String> packs;
    @Nullable
    private KeyBindings.Group key;

    public ResourcePackGroup(String id) {
        this(id, new ArrayList<>());
    }

    public ResourcePackGroup(String id, List<String> packs) {
        this(id, packs, null);
    }

    public ResourcePackGroup(String id, List<String> packs, KeyBindings.@Nullable Group key) {
        this.id = id;
        this.packs = packs;
        this.key = key;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isIdEquals(String id) {
        return this.id.equals(id);
    }

    public List<String> getPacks() {
        if (packs == null) {
            packs = new ArrayList<>();
        }
        return packs;
    }

    public void setPacks(List<String> packs) {
        this.packs = new ArrayList<>(packs);
    }

    public KeyBindings.@Nullable Group getKey() {
        return key;
    }

    public void setKey(KeyBindings.@Nullable Group key) {
        this.key = key;
    }

    public void nextKey() {
        int index = this.key != null ? this.key.ordinal() : -1;
        int nextIndex = index + 1;
        KeyBindings.Group[] keys = KeyBindings.Group.values();
        this.key = nextIndex < keys.length ? keys[nextIndex] : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourcePackGroup group)) return false;
        return Objects.equals(id, group.id) && Objects.equals(packs, group.packs) && key == group.key;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, packs, key);
    }

    @Override
    public String toString() {
        return "ResourcePackGroup{" +
                "id='" + id + '\'' +
                ", names=" + packs +
                ", key=" + key +
                '}';
    }
}
