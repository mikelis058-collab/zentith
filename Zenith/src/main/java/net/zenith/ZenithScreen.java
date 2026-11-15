package net.zenith;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.IntConsumer;

public class ZenithScreen extends Screen {
    private static final int COLS = 4, BW = 100, BH = 20, GAP = 4;
    private int fogLabelY, doneY, startX;

    public ZenithScreen() {
        super(Text.literal("Zenith"));
    }

    private static Text label(Module m) {
        return Text.literal(m.name + ": " + (m.enabled ? "ON" : "OFF"));
    }

    @Override
    protected void init() {
        startX = width / 2 - (COLS * BW + (COLS - 1) * GAP) / 2;
        int y0 = 28;
        for (int i = 0; i < Modules.ALL.size(); i++) {
            Module m = Modules.ALL.get(i);
            int x = startX + (i % COLS) * (BW + GAP);
            int y = y0 + (i / COLS) * (BH + GAP);
            addDrawableChild(ButtonWidget.builder(label(m), b -> {
                m.toggle();
                b.setMessage(label(m));
            }).dimensions(x, y, BW, BH).build());
        }
        int rows = (Modules.ALL.size() + COLS - 1) / COLS;
        fogLabelY = y0 + rows * (BH + GAP) + 8;
        int sy = fogLabelY + 12, sw = 150;
        int lx = width / 2 - sw - 2, rx = width / 2 + 2;
        addDrawableChild(new ConfigSlider(lx, sy, sw, BH, "Fog Red", 255, ZenithConfig.fogR, v -> ZenithConfig.fogR = v));
        addDrawableChild(new ConfigSlider(rx, sy, sw, BH, "Fog Green", 255, ZenithConfig.fogG, v -> ZenithConfig.fogG = v));
        addDrawableChild(new ConfigSlider(lx, sy + BH + GAP, sw, BH, "Fog Blue", 255, ZenithConfig.fogB, v -> ZenithConfig.fogB = v));
        addDrawableChild(new ConfigSlider(rx, sy + BH + GAP, sw, BH, "Fog Density", 100, ZenithConfig.fogDensity, v -> ZenithConfig.fogDensity = v));
        doneY = sy + 2 * (BH + GAP) + 4;
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(width / 2 - 60, doneY, 120, BH).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, "Zenith Client", width / 2, 10, 0xFFFFFF);
        ctx.drawTextWithShadow(textRenderer, "Fog settings", startX, fogLabelY, 0xAAAAAA);
        int rgb = (ZenithConfig.fogR << 16) | (ZenithConfig.fogG << 8) | ZenithConfig.fogB;
        ctx.fill(width / 2 + 70, doneY, width / 2 + 110, doneY + BH, 0xFF000000 | rgb);
    }

    @Override
    public void close() {
        ZenithConfig.save();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static class ConfigSlider extends SliderWidget {
        private final String label;
        private final int max;
        private final IntConsumer setter;

        ConfigSlider(int x, int y, int w, int h, String label, int max, int current, IntConsumer setter) {
            super(x, y, w, h, Text.empty(), (double) current / max);
            this.label = label;
            this.max = max;
            this.setter = setter;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(label + ": " + (int) Math.round(value * max)));
        }

        @Override
        protected void applyValue() {
            setter.accept((int) Math.round(value * max));
        }
    }
}
