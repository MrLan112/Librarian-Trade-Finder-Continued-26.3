package de.greenman999.librariantradefinder.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class HudUtils {
    private HudUtils() {}

    public static void chatMessage(Component text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(text);
        }
    }

    // In Minecraft 26.2+, the overlay message API is exposed through Gui.hud.
    public static void overlayMessage(Component text, boolean tinted) {
        Minecraft.getInstance().gui.hud.setOverlayMessage(text, tinted);
    }

    public static Component textTranslatable(ChatFormatting color, String messageKey, Object... args) {
        return Component.translatable(messageKey, args).withStyle(style -> style.withColor(color));
    }

    public static Component textTranslatable(String messageKey, Object... args) {
        return Component.translatable(messageKey, args);
    }

    public static Component text(String message, ChatFormatting color) {
        return Component.literal(message).withStyle(style -> style.withColor(color));
    }
}
