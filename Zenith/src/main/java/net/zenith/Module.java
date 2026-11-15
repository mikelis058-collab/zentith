package net.zenith;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public abstract class Module {
    public final String name;
    public boolean enabled;

    protected Module(String name, boolean enabledByDefault) {
        this.name = name;
        this.enabled = enabledByDefault;
    }

    public void toggle() {
        enabled = !enabled;
        ZenithConfig.save();
    }

    /** Runs every client tick (even when disabled, so modules can clean up). */
    public void tick(MinecraftClient mc) {}

    /** Text line(s) for the top-left list. Use \n for several lines. Return null for none. */
    public String line(MinecraftClient mc) { return null; }

    /** Custom drawing (armor, keystrokes, potions...). Only called when enabled. */
    public void draw(DrawContext ctx, MinecraftClient mc) {}
}
