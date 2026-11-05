package de.greenman999.librariantradefinder.screens;

import de.greenman999.librariantradefinder.LibrarianTradeFinder;
import de.greenman999.librariantradefinder.TradeFinder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ControlUi extends Screen {
    private final Screen parent;
    private EnchantmentsListWidget enchantmentsListWidget;
    private EditBox focusedTextField;

    public ControlUi(Screen parent) {
        super(Component.translatable("tradefinderui.screen.title"));
        this.parent = parent;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        extractBackground(context, mouseX, mouseY, delta);
        context.fill(width / 2, 4, width / 2 + 1, height - 5, 0xFFC7C0C0);
        context.fill(width / 2 + 6, 5, width - 5, 20, 0x3FC7C0C0);
        context.text(Minecraft.getInstance().font, Component.translatable("tradefinderui.options.title"), width / 2 + 10, 9, 0xFFFFFFFF);
        // Screen.renderables is not accessible in the Fabric-mapped 26.3 API.
        // All widgets registered with addRenderableWidget are also exposed via children().
        for (GuiEventListener child : children()) {
            if (child instanceof Renderable renderable) {
                renderable.extractRenderState(context, mouseX, mouseY, delta);
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        if (minecraft != null && minecraft.level == null) {
            extractPanorama(context, deltaTicks);
        } else {
            context.fill(0, 0, width, height, 0x90202020);
        }
        extractMenuBackground(context);
    }

    private void returnToParent() {
        if (minecraft != null) {
            // Since Minecraft 26.2 screens are accessed through Gui, not Minecraft.setScreen/screen fields.
            minecraft.gui.setScreen(parent);
        }
    }

    @Override
    protected void init() {
        addRenderableWidget(GrayButtonWidget.builder(Component.translatable("tradefinderui.buttons.save"), button -> {
            LibrarianTradeFinder.getConfig().save();
            returnToParent();
        }).bounds(width / 2 + 6, height - 25, width / 2 / 2 - 9, 20)
            .color(0x4FC7C0C0).id(0).build());

        addRenderableWidget(GrayButtonWidget.builder(
            Component.translatable("tradefinderui.buttons.start").withStyle(ChatFormatting.GREEN), button -> {
                if ((TradeFinder.villager == null || TradeFinder.lecternPos == null) && minecraft != null && minecraft.player != null) {
                    minecraft.player.sendSystemMessage(Component.translatable("commands.tradefinder.start.not-selected")
                        .withStyle(ChatFormatting.RED));
                    returnToParent();
                } else {
                    TradeFinder.searchList();
                    LibrarianTradeFinder.getConfig().save();
                    returnToParent();
                }
            }).bounds(width / 2 + width / 4 + 3, height - 25, width / 4 - 6, 20)
            .color(0x4FC7C0C0).id(1).build());

        enchantmentsListWidget = new EnchantmentsListWidget(minecraft, width / 2 - 10, height - 30, 25, 20);
        addRenderableWidget(enchantmentsListWidget);
        addRenderableWidget(enchantmentsListWidget.resetButton);

        addRenderableWidget(GrayButtonWidget.builder(getButtonText("tradefinderui.options.tp-to-villager", LibrarianTradeFinder.getConfig().tpToVillager), button -> {
            LibrarianTradeFinder.getConfig().tpToVillager = !LibrarianTradeFinder.getConfig().tpToVillager;
            updateButtonTexts();
        }).bounds(width / 2 + 6, 25, width / 2 - 10, 20).color(0x4FC7C0C0).id(2)
            .tooltip(Tooltip.create(Component.translatable("tradefinderui.options.tp-to-villager.tooltip"))).build());

        addRenderableWidget(GrayButtonWidget.builder(getButtonText("tradefinderui.options.prevent-axe-break", LibrarianTradeFinder.getConfig().preventAxeBreaking), button -> {
            LibrarianTradeFinder.getConfig().preventAxeBreaking = !LibrarianTradeFinder.getConfig().preventAxeBreaking;
            updateButtonTexts();
        }).bounds(width / 2 + 6, 50, width / 2 - 10, 20).color(0x4FC7C0C0).id(3)
            .tooltip(Tooltip.create(Component.translatable("tradefinderui.options.prevent-axe-break.tooltip"))).build());

        addRenderableWidget(GrayButtonWidget.builder(getButtonText("tradefinderui.options.legit-mode", LibrarianTradeFinder.getConfig().legitMode), button -> {
            LibrarianTradeFinder.getConfig().legitMode = !LibrarianTradeFinder.getConfig().legitMode;
            if (!LibrarianTradeFinder.getConfig().legitMode) LibrarianTradeFinder.getConfig().slowMode = false;
            updateButtonTexts();
        }).bounds(width / 2 + 6, 75, width / 2 - 10, 20).color(0x4FC7C0C0).id(4)
            .tooltip(Tooltip.create(Component.translatable("tradefinderui.options.legit-mode.tooltip"))).build());

        addRenderableWidget(GrayButtonWidget.builder(getButtonText("tradefinderui.options.slow-mode", LibrarianTradeFinder.getConfig().slowMode), button -> {
            LibrarianTradeFinder.getConfig().slowMode = !LibrarianTradeFinder.getConfig().slowMode;
            LibrarianTradeFinder.getConfig().legitMode = LibrarianTradeFinder.getConfig().slowMode || LibrarianTradeFinder.getConfig().legitMode;
            updateButtonTexts();
        }).bounds(width / 2 + 6, 100, width / 2 - 10, 20).color(0x4FC7C0C0).id(5)
            .tooltip(Tooltip.create(Component.translatable("tradefinderui.options.slow-mode.tooltip"))).build());

        super.init();
    }

    private void updateButtonTexts() {
        for (GuiEventListener element : children()) {
            if (!(element instanceof GrayButtonWidget buttonWidget)) continue;
            switch (buttonWidget.getId()) {
                case 2 -> buttonWidget.setMessage(getButtonText("tradefinderui.options.tp-to-villager", LibrarianTradeFinder.getConfig().tpToVillager));
                case 3 -> buttonWidget.setMessage(getButtonText("tradefinderui.options.prevent-axe-break", LibrarianTradeFinder.getConfig().preventAxeBreaking));
                case 4 -> buttonWidget.setMessage(getButtonText("tradefinderui.options.legit-mode", LibrarianTradeFinder.getConfig().legitMode));
                case 5 -> buttonWidget.setMessage(getButtonText("tradefinderui.options.slow-mode", LibrarianTradeFinder.getConfig().slowMode));
            }
        }
    }

    public Component getButtonText(String key, boolean enabled) {
        return Component.translatable(key, enabled ? ChatFormatting.GREEN + "Enabled" : ChatFormatting.RED + "Disabled");
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (super.keyPressed(input)) return true;
        return enchantmentsListWidget.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        return enchantmentsListWidget.charTyped(input);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        EditBox nextFocusedField = null;
        for (EnchantmentEntry entry : enchantmentsListWidget.children()) {
            boolean priceFocused = entry.maxPriceField.mouseClicked(click, doubled);
            boolean levelFocused = entry.levelField.mouseClicked(click, doubled);
            entry.maxPriceField.setFocused(priceFocused);
            entry.levelField.setFocused(levelFocused);
            if (priceFocused) nextFocusedField = entry.maxPriceField;
            if (levelFocused) nextFocusedField = entry.levelField;
        }

        // Notify SDL only for the actual focus transition. Calling this with
        // false for every non-focused field would clear a focus set earlier in the loop.
        Minecraft client = Minecraft.getInstance();
        if (focusedTextField != nextFocusedField) {
            if (focusedTextField != null) {
                client.onTextInputFocusChange(focusedTextField, false);
            }
            focusedTextField = nextFocusedField;
            if (focusedTextField != null) {
                client.onTextInputFocusChange(focusedTextField, true);
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (EnchantmentEntry entry : enchantmentsListWidget.children()) {
            if (entry.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void clearTextInputFocus() {
        if (focusedTextField != null && minecraft != null) {
            minecraft.onTextInputFocusChange(focusedTextField, false);
            focusedTextField = null;
        }
    }

    @Override
    public void removed() {
        // Buttons can switch screens directly through Gui#setScreen, which calls
        // removed() without necessarily routing through onClose(). Keep SDL's
        // text-input manager in sync for every screen transition.
        clearTextInputFocus();
        super.removed();
    }

    @Override
    public void onClose() {
        LibrarianTradeFinder.getConfig().save();
        clearTextInputFocus();
        super.onClose();
    }
}
