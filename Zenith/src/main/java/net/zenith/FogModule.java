package net.zenith;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * Realistic fog: soft distance haze that thickens a bit in rain and at dawn,
 * and darkens with the time of day so it never glows at night.
 */
public class FogModule extends Module {
    public FogModule() {
        super("Fog", true);
    }

    /** Called from the mixin. Returns null to keep vanilla fog. */
    public static Fog apply(Camera camera, Fog original, float viewDistance, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc.world;
        ClientPlayerEntity p = mc.player;
        if (!Modules.FOG.enabled || world == null || p == null) return null;
        if (world.getRegistryKey() != World.OVERWORLD) return null;
        if (camera.getSubmersionType() != CameraSubmersionType.NONE) return null;
        if (p.hasStatusEffect(StatusEffects.BLINDNESS) || p.hasStatusEffect(StatusEffects.DARKNESS)) return null;

        float rain = world.getRainGradient(tickDelta);
        long t = world.getTimeOfDay() % 24000L;
        float dawn = t >= 22000 ? (t - 22000) / 2000f : (t < 3000 ? 1f - t / 3000f : 0f);

        // 0 = clear, 1 = thick. Default setting (40%) is a light haze.
        float eff = MathHelper.clamp(ZenithConfig.fogDensity / 100f + rain * 0.25f + dawn * 0.20f, 0.02f, 1f);
        float end = viewDistance * (1f - 0.5f * eff);
        float start = end * (0.6f - 0.45f * eff);
        end = Math.min(end, original.end());
        start = Math.min(start, original.start());

        float bright = MathHelper.clamp(MathHelper.cos(world.getSkyAngle(tickDelta) * MathHelper.TAU) * 2f + 0.5f, 0.08f, 1f);
        float dim = bright * (1f - 0.25f * rain);
        float r = ZenithConfig.fogR / 255f * dim;
        float g = ZenithConfig.fogG / 255f * dim;
        float b = ZenithConfig.fogB / 255f * dim;
        return new Fog(start, end, FogShape.SPHERE, r, g, b, original.alpha());
    }
}
