package de.greenman999.librariantradefinder.screens;

import com.mojang.blaze3d.platform.InputConstants;
import de.greenman999.librariantradefinder.LibrarianTradeFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;

public class EnchantmentsListWidget extends AbstractSelectionList<EnchantmentEntry> {
    public GrayButtonWidget resetButton;

    public EnchantmentsListWidget(Minecraft client, int width, int height, int top, int itemHeight) {
        super(client, width, height, top, itemHeight);
        for (Enchantment enchantment : LibrarianTradeFinder.getConfig().enchantments.keySet()) {
            addEntry(new EnchantmentEntry(enchantment));
        }

        resetButton = GrayButtonWidget.builder(Component.translatable("tradefinderui.reset"), button -> {
            for (EnchantmentEntry entry : children()) {
                entry.maxPriceField.setValue("64");
                entry.levelField.setValue(String.valueOf(entry.enchantment.getMaxLevel()));
                entry.enabled = false;
            }
        }).color(0x5FC7C0C0)
            .bounds(this.width - 45, 5, 50, 15)
            .tooltip(Tooltip.create(Component.translatable("tradefinderui.reset.tooltip")))
            .build();
    }

    @Override
    protected void extractListBackground(GuiGraphicsExtractor context) {
        setSelected(null);
        context.fill(5, 5, width + 5, 20, 0xAFC7C0C0);
        context.text(Minecraft.getInstance().font, Component.translatable("tradefinderui.enchantments.title"), 9, 9, 0xFFFFFFFF);
    }

    @Override
    protected void extractListSeparators(GuiGraphicsExtractor context) { }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) { }

    @Override
    public int getRowWidth() { return width - 12; }

    @Override
    public int getRight() { return width + 7; }

    @Override
    protected int scrollBarX() { return width; }

    @Override
    public int getRowTop(int index) {
        return getY() - (int) scrollAmount() + index * defaultEntryHeight;
    }

    @Override
    public int getRowLeft() {
        return getX() + width / 2 - getRowWidth() / 2 - 1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        resetButton.mouseClicked(click, doubled);
        return super.mouseClicked(click, doubled);
    }

    private static boolean isNumericOrNavigationKey(int keyCode) {
        // SDL scancodes for the number row are not ordered like GLFW: 1..9 are 30..38 and 0 is 39.
        return keyCode == InputConstants.KEY_BACKSPACE
            || (keyCode >= InputConstants.KEY_1 && keyCode <= InputConstants.KEY_9)
            || keyCode == InputConstants.KEY_0
            || keyCode == InputConstants.KEY_LEFT
            || keyCode == InputConstants.KEY_RIGHT;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (!isNumericOrNavigationKey(input.input())) return false;
        for (EnchantmentEntry entry : children()) {
            if (entry.maxPriceField.isFocused()) return entry.maxPriceField.keyPressed(input);
            if (entry.levelField.isFocused()) return entry.levelField.keyPressed(input);
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean keyReleased(KeyEvent input) {
        if (!isNumericOrNavigationKey(input.input())) return false;
        for (EnchantmentEntry entry : children()) {
            entry.maxPriceField.keyReleased(input);
            entry.levelField.keyReleased(input);
        }
        return super.keyReleased(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (!input.isAllowedChatCharacter()) return false;
        for (EnchantmentEntry entry : children()) {
            entry.maxPriceField.charTyped(input);
            entry.levelField.charTyped(input);
        }
        return super.charTyped(input);
    }
}
