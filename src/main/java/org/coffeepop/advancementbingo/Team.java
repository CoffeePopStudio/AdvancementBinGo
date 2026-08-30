package org.coffeepop.advancementbingo;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public enum Team {
    RED("红队", NamedTextColor.RED, TextColor.color(0xFF5555)),
    BLUE("蓝队", NamedTextColor.BLUE, TextColor.color(0x5555FF));

    private final String displayName;
    private final NamedTextColor chatColor;
    private final TextColor guiColor;

    Team(String displayName, NamedTextColor chatColor, TextColor guiColor) {
        this.displayName = displayName;
        this.chatColor = chatColor;
        this.guiColor = guiColor;
    }

    public String displayName() {
        return displayName;
    }

    public NamedTextColor chatColor() {
        return chatColor;
    }

    public TextColor guiColor() {
        return guiColor;
    }

    public Team other() {
        return this == RED ? BLUE : RED;
    }
}
