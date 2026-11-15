package net.zenith;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/** All modules live here. To add one: copy a block below and add() it. */
public final class Modules {
    public static final List<Module> ALL = new ArrayList<>();

    // shared combat state (set by the attack hook in ZenithClient)
    private static int combo = 0;
    private static long lastHitMs = 0;
    private static double lastReach = 0;

    public static void onHit(double reach) {
        combo++;
        lastHitMs = System.currentTimeMillis();
        lastReach = reach;
    }

    private static <T extends Module> T add(T m) {
        ALL.add(m);
        return m;
    }

    // 1. FPS
    public static final Module FPS = add(new Module("FPS", true) {
        @Override public String line(MinecraftClient mc) { return "FPS: " + mc.getCurrentFps(); }
    });

    // 2. CPS
    public static final Module CPS = add(new Module("CPS", true) {
        @Override public String line(MinecraftClient mc) {
            return "CPS: " + ZenithClient.leftCps() + " | " + ZenithClient.rightCps();
        }
    });

    // 3. Coordinates
    public static final Module COORDS = add(new Module("Coords", true) {
        @Override public String line(MinecraftClient mc) {
            ClientPlayerEntity p = mc.player;
            if (p == null) return null;
            return String.format("XYZ: %d %d %d", (int) Math.floor(p.getX()), (int) Math.floor(p.getY()), (int) Math.floor(p.getZ()));
        }
    });

    // 4. Direction
    public static final Module DIRECTION = add(new Module("Direction", false) {
        private final String[] names = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        @Override public String line(MinecraftClient mc) {
            if (mc.player == null) return null;
            float bearing = (MathHelper.wrapDegrees(mc.player.getYaw()) + 180f + 360f) % 360f;
            return "Facing: " + names[Math.round(bearing / 45f) % 8];
        }
    });

    // 5. Ping
    public static final Module PING = add(new Module("Ping", true) {
        @Override public String line(MinecraftClient mc) {
            if (mc.player == null || mc.getNetworkHandler() == null) return null;
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            return "Ping: " + (e == null ? "--" : e.getLatency() + "ms");
        }
    });

    // 6. Armor HUD (right side, with durability %)
    public static final Module ARMOR = add(new Module("Armor HUD", true) {
        private final EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};
        @Override public void draw(DrawContext ctx, MinecraftClient mc) {
            if (mc.player == null) return;
            TextRenderer tr = mc.textRenderer;
            int x = ctx.getScaledWindowWidth() - 22;
            int y = ctx.getScaledWindowHeight() / 2 - 46;
            for (EquipmentSlot s : slots) {
                ItemStack st = mc.player.getEquippedStack(s);
                if (st.isEmpty()) continue;
                ctx.drawItem(st, x, y);
                ctx.drawStackOverlay(tr, st, x, y);
                if (st.isDamageable()) {
                    int max = st.getMaxDamage();
                    int pct = (max - st.getDamage()) * 100 / max;
                    String t = pct + "%";
                    ctx.drawTextWithShadow(tr, t, x - 3 - tr.getWidth(t), y + 4, MathHelper.hsvToRgb(pct / 300f, 1f, 1f));
                }
                y += 18;
            }
        }
    });

    // 7. Keystrokes (bottom-left)
    public static final Module KEYSTROKES = add(new Module("Keystrokes", true) {
        private void key(DrawContext ctx, TextRenderer tr, String label, boolean down, int x, int y, int w, int h) {
            ctx.fill(x, y, x + w, y + h, down ? 0xAAFFFFFF : 0x77000000);
            ctx.drawCenteredTextWithShadow(tr, label, x + w / 2, y + (h - 8) / 2, down ? 0x000000 : 0xFFFFFF);
        }
        @Override public void draw(DrawContext ctx, MinecraftClient mc) {
            TextRenderer tr = mc.textRenderer;
            int s = 22, g = 2;
            int x = 6, y = ctx.getScaledWindowHeight() - 80;
            key(ctx, tr, "W", mc.options.forwardKey.isPressed(), x + s + g, y, s, s);
            key(ctx, tr, "A", mc.options.leftKey.isPressed(), x, y + s + g, s, s);
            key(ctx, tr, "S", mc.options.backKey.isPressed(), x + s + g, y + s + g, s, s);
            key(ctx, tr, "D", mc.options.rightKey.isPressed(), x + 2 * (s + g), y + s + g, s, s);
            int w = (3 * s + 2 * g - g) / 2;
            key(ctx, tr, "LMB " + ZenithClient.leftCps(), mc.options.attackKey.isPressed(), x, y + 2 * (s + g), w, s);
            key(ctx, tr, "RMB " + ZenithClient.rightCps(), mc.options.useKey.isPressed(), x + w + g, y + 2 * (s + g), w, s);
        }
    });

    // 8. Potion effects (right side, under the armor)
    public static final Module POTIONS = add(new Module("Potions", true) {
        @Override public void draw(DrawContext ctx, MinecraftClient mc) {
            if (mc.player == null) return;
            TextRenderer tr = mc.textRenderer;
            int y = ctx.getScaledWindowHeight() / 2 + 52;
            for (StatusEffectInstance e : mc.player.getStatusEffects()) {
                if (Modules.FULLBRIGHT.enabled && e.getEffectType().equals(StatusEffects.NIGHT_VISION)) continue;
                String name = Text.translatable(e.getTranslationKey()).getString();
                if (e.getAmplifier() > 0) name += " " + (e.getAmplifier() + 1);
                int secs = e.getDuration() / 20;
                String time = e.isInfinite() ? "inf" : String.format("%d:%02d", secs / 60, secs % 60);
                String s = name + " " + time;
                int color = e.getEffectType().value().isBeneficial() ? 0x55FF55 : 0xFF5555;
                ctx.drawTextWithShadow(tr, s, ctx.getScaledWindowWidth() - 4 - tr.getWidth(s), y, color);
                y += 10;
            }
        }
    });

    // 9. Zoom (hold C by default)
    public static final Module ZOOM = add(new Module("Zoom", true) {
        private boolean active = false;
        private int savedFov;
        private double savedSens;
        @Override public void tick(MinecraftClient mc) {
            boolean want = enabled && mc.player != null && mc.currentScreen == null && ZenithClient.ZOOM_KEY.isPressed();
            if (want && !active) {
                savedFov = mc.options.getFov().getValue();
                savedSens = mc.options.getMouseSensitivity().getValue();
                mc.options.getFov().setValue(30);
                mc.options.getMouseSensitivity().setValue(savedSens * 0.35);
                active = true;
            } else if (!want && active) {
                mc.options.getFov().setValue(savedFov);
                mc.options.getMouseSensitivity().setValue(savedSens);
                active = false;
            }
        }
    });

    // 10. Fullbright (client-side night vision, no icon)
    public static final Module FULLBRIGHT = add(new Module("Fullbright", false) {
        private boolean applied = false;
        @Override public void tick(MinecraftClient mc) {
            if (mc.player == null) { applied = false; return; }
            if (enabled) {
                StatusEffectInstance cur = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
                if (cur == null || cur.getDuration() < 400) {
                    mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1000, 0, false, false, false));
                    applied = true;
                }
            } else if (applied) {
                mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
                applied = false;
            }
        }
    });

    // 11. Toggle sprint (auto-sprint while holding forward)
    public static final Module SPRINT = add(new Module("Sprint", false) {
        @Override public void tick(MinecraftClient mc) {
            ClientPlayerEntity p = mc.player;
            if (!enabled || p == null) return;
            if (mc.options.forwardKey.isPressed() && !p.isSneaking() && !p.horizontalCollision
                    && !p.isUsingItem() && !p.hasStatusEffect(StatusEffects.BLINDNESS)
                    && p.getHungerManager().getFoodLevel() > 6) {
                p.setSprinting(true);
            }
        }
    });

    // 12. Combo counter (resets when you get hit or after 3s)
    public static final Module COMBO = add(new Module("Combo", true) {
        @Override public void tick(MinecraftClient mc) {
            if (mc.player == null) { combo = 0; return; }
            if (mc.player.hurtTime > 0 || System.currentTimeMillis() - lastHitMs > 3000) combo = 0;
        }
        @Override public String line(MinecraftClient mc) { return "Combo: " + combo; }
    });

    // 13. Reach display (distance of your last hit)
    public static final Module REACH = add(new Module("Reach", true) {
        @Override public String line(MinecraftClient mc) {
            boolean recent = System.currentTimeMillis() - lastHitMs < 5000;
            return recent ? String.format("Reach: %.2f", lastReach) : "Reach: -";
        }
    });

    // 14. Item counter (gapples, pearls, totems)
    public static final Module ITEMS = add(new Module("Items", false) {
        @Override public String line(MinecraftClient mc) {
            if (mc.player == null) return null;
            var inv = mc.player.getInventory();
            int gapples = inv.count(Items.GOLDEN_APPLE) + inv.count(Items.ENCHANTED_GOLDEN_APPLE);
            return "Gapples: " + gapples + "  Pearls: " + inv.count(Items.ENDER_PEARL) + "  Totems: " + inv.count(Items.TOTEM_OF_UNDYING);
        }
    });

    // 15. Fog (see FogModule + mixin)
    public static final FogModule FOG = add(new FogModule());

    private Modules() {}
}
