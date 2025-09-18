package com.arisweeping.gui.components;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * 抽象配置组件基类
 */
public abstract class AbstractConfigWidget extends AbstractWidget {
    protected static final int COLOR_TEXT = 0xFFFFFFFF;
    protected static final int COLOR_BUTTON_NORMAL = 0xFF606060;
    protected static final int COLOR_BUTTON_HOVER = 0xFF808080;
    protected static final int COLOR_BUTTON_PRESSED = 0xFF404040;
    protected static final int COLOR_ENABLED = 0xFF40FF40;
    protected static final int COLOR_DISABLED = 0xFF808080;

    protected AbstractConfigWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}