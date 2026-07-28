package com.spawnify.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ComponentUtil {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private ComponentUtil() {
    }

    public static Component legacy(String input, Map<String, String> placeholders) {
        return LEGACY.deserialize(apply(input, placeholders));
    }

    public static List<Component> legacyList(List<String> lines, Map<String, String> placeholders) {
        return lines.stream()
                .filter(Objects::nonNull)
                .map(line -> legacy(line, placeholders))
                .collect(Collectors.toList());
    }

    public static String apply(String input, Map<String, String> placeholders) {
        String value = input == null ? "" : input;
        if (placeholders == null || placeholders.isEmpty()) {
            return value;
        }
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            value = value.replace(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
        }
        return value;
    }
}
