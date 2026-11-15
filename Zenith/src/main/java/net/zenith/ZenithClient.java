package net.zenith;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;

public class ZenithClient implements ClientModInitializer {
    public static KeyBinding MENU_KEY;
    public static KeyBinding ZOOM_KEY;

    private static final ArrayDeque<Long> LEFT = new ArrayDeque<>();
    private static final ArrayDeque<Long> RIGHT = new ArrayDeque<>();
    private static boolean wasL, wasR;

    public static int leftCps() { return LEFT.size(); }
    public static int rightCps() { return RIGHT.size(); }

    @Override
    public void onInitializeClient() {
        ZenithConfig.load();

        MENU_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zenith.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.zenith"));
        ZOOM_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zenith.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.zenith"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (MENU_KEY.wasPressed()) mc.setScreen(new ZenithScreen());
            for (Module m : Modules.ALL) m.tick(mc);
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> renderHud(ctx, MinecraftClient.getInstance()));

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() && entity instanceof LivingEntity) {
                double reach = hit != null ? player.getEyePos().distanceTo(hit.getPos()) : player.distanceTo(entity);
                Modules.onHit(reach);
            }
            return ActionResult.PASS;
        });
    }

    private static void pollClicks(MinecraftClient mc) {
        long win = mc.getWindow().getHandle();
        boolean l = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        long now = System.currentTimeMillis();
        if (mc.currentScreen == null) {
            if (l && !wasL) LEFT.addLast(now);
            if (r && !wasR) RIGHT.addLast(now);
        }
        wasL = l;
        wasR = r;
        while (!LEFT.isEmpty() && now - LEFT.peekFirst() > 1000) LEFT.pollFirst();
        while (!RIGHT.isEmpty() && now - RIGHT.peekFirst() > 1000) RIGHT.pollFirst();
    }

    private static void renderHud(DrawContext ctx, MinecraftClient mc) {
        pollClicks(mc);
        if (mc.options.hudHidden || mc.player == null) return;
        TextRenderer tr = mc.textRenderer;
        int y = 4;
        for (Module m : Modules.ALL) {
            if (!m.enabled) continue;
            String s = m.line(mc);
            if (s == null) continue;
            for (String part : s.split("\n")) {
                ctx.drawTextWithShadow(tr, part, 4, y, 0xFFFFFF);
                y += 10;
            }
        }
        for (Module m : Modules.ALL) {
            if (m.enabled) m.draw(ctx, mc);
        }
    }
}
