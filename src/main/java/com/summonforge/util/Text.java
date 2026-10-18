package com.summonforge.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;

public final class Text {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Text() {}

    /** MiniMessage -> Component sin cursiva por defecto (items, menús, chat). */
    public static Component mm(String s) {
        return MM.deserialize(s == null ? "" : s).decoration(TextDecoration.ITALIC, false);
    }

    public static List<Component> mm(List<String> lines) {
        List<Component> out = new ArrayList<>(lines.size());
        for (String l : lines) out.add(mm(l));
        return out;
    }

    public static String plain(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    public static String escape(String s) {
        return MM.escapeTags(s == null ? "" : s);
    }
}
