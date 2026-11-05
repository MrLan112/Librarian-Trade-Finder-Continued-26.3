package de.greenman999.librariantradefinder.screens;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;

public class GrayButtonWidget extends Button {
    private final int color;
    private final int id;

    protected GrayButtonWidget(int x, int y, int width, int height, Component message, OnPress onPress,
                               CreateNarration narrationSupplier, int color, int id) {
        super(x, y, width, height, message, onPress, narrationSupplier);
        this.color = color;
        this.id = id;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        setFocused(false);
        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), color);
        int textColor = active ? 0xFFFFFF : 0xA0A0A0;
        context.centeredText(Minecraft.getInstance().font, getMessage(), getX() + width / 2,
            getY() + (height - 7) / 2, textColor | (Mth.ceil(alpha * 255.0F) << 24));
        matrices.popMatrix();
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int keyCode = input.input();
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_SPACE
            || keyCode == InputConstants.KEY_UP || keyCode == InputConstants.KEY_DOWN
            || keyCode == InputConstants.KEY_LEFT || keyCode == InputConstants.KEY_RIGHT) {
            return false;
        }
        return super.keyPressed(input);
    }

    public static Builder builder(Component message, OnPress onPress) {
        return new Builder(message, onPress);
    }

    public int getId() { return id; }

    public static class Builder extends Button.Builder {
        private final Component message;
        private final OnPress onPress;
        @Nullable private Tooltip tooltip;
        private int x;
        private int y;
        private int width = 150;
        private int height = 20;
        private CreateNarration narrationSupplier;
        private int color;
        private int id;

        public Builder(Component message, OnPress onPress) {
            super(message, onPress);
            this.narrationSupplier = Button.DEFAULT_NARRATION;
            this.message = message;
            this.onPress = onPress;
        }

        @Override
        public Builder pos(int x, int y) { this.x = x; this.y = y; return this; }
        @Override
        public Builder width(int width) { this.width = width; return this; }
        @Override
        public Builder size(int width, int height) { this.width = width; this.height = height; return this; }
        @Override
        public Builder bounds(int x, int y, int width, int height) { return pos(x, y).size(width, height); }
        @Override
        public Builder tooltip(@Nullable Tooltip tooltip) { this.tooltip = tooltip; return this; }
        @Override
        public Builder createNarration(CreateNarration narrationSupplier) { this.narrationSupplier = narrationSupplier; return this; }

        public Builder color(int color) { this.color = color; return this; }
        public Builder id(int id) { this.id = id; return this; }

        @Override
        public GrayButtonWidget build() {
            GrayButtonWidget button = new GrayButtonWidget(x, y, width, height, message, onPress, narrationSupplier, color, id);
            button.setTooltip(tooltip);
            return button;
        }
    }
}
