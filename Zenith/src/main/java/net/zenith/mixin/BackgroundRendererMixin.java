package net.zenith.mixin;

import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.zenith.FogModule;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {
    @Inject(method = "applyFog", at = @At("RETURN"), cancellable = true)
    private static void zenith$fog(Camera camera, BackgroundRenderer.FogType fogType, Vector4f color,
                                   float viewDistance, boolean thickenFog, float tickDelta,
                                   CallbackInfoReturnable<Fog> cir) {
        if (fogType != BackgroundRenderer.FogType.FOG_TERRAIN) return;
        Fog fog = FogModule.apply(camera, cir.getReturnValue(), viewDistance, tickDelta);
        if (fog != null) cir.setReturnValue(fog);
    }
}
