package de.ellpeck.nyx.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.ellpeck.nyx.Nyx;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Renders a falling meteor as a full-bright textured cube scaled by its size.
 *
 * <p><b>Port note:</b> the 1.12 version used a {@code ModelBase}/{@code ModelRenderer}
 * box; that API is gone, so this emits the cube directly to a
 * {@link com.mojang.blaze3d.vertex.VertexConsumer}. Each face samples the whole
 * meteor texture rather than a 32px sub-rect, which is a minor visual
 * simplification.
 */
public class MeteorRenderer extends EntityRenderer<FallingMeteor> {

    private static final ResourceLocation TEX = new ResourceLocation(Nyx.ID, "textures/models/meteor.png");

    public MeteorRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(FallingMeteor entity) {
        return TEX;
    }

    @Override
    public void render(FallingMeteor entity, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffer, int packedLight) {
        if (!entity.isLoaded())
            return;
        float size = entity.getEntityData().get(FallingMeteor.SIZE) / 2F;
        pose.pushPose();
        pose.scale(size, size, size);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEX));
        renderCube(pose, vc);
        pose.popPose();
        super.render(entity, yaw, partialTicks, pose, buffer, packedLight);
    }

    private static void renderCube(PoseStack pose, VertexConsumer vc) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float a = -1, b = 1; // cube from -1..1
        // each face: 4 corners (CCW) with full UV
        // bottom (y=a)
        face(vc, m, n, 0, -1, 0, a, a, a, b, a, a, b, a, b, a, a, b);
        // top (y=b)
        face(vc, m, n, 0, 1, 0, a, b, b, b, b, b, b, b, a, a, b, a);
        // north (z=a)
        face(vc, m, n, 0, 0, -1, a, a, a, a, b, a, b, b, a, b, a, a);
        // south (z=b)
        face(vc, m, n, 0, 0, 1, b, a, b, b, b, b, a, b, b, a, a, b);
        // west (x=a)
        face(vc, m, n, -1, 0, 0, a, a, b, a, b, b, a, b, a, a, a, a);
        // east (x=b)
        face(vc, m, n, 1, 0, 0, b, a, a, b, b, a, b, b, b, b, a, b);
    }

    private static void face(VertexConsumer vc, Matrix4f m, Matrix3f n, float nx, float ny, float nz,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4) {
        int light = LightTexture.FULL_BRIGHT;
        vertex(vc, m, n, x1, y1, z1, 0, 0, nx, ny, nz, light);
        vertex(vc, m, n, x2, y2, z2, 0, 1, nx, ny, nz, light);
        vertex(vc, m, n, x3, y3, z3, 1, 1, nx, ny, nz, light);
        vertex(vc, m, n, x4, y4, z4, 1, 0, nx, ny, nz, light);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z,
                              float u, float v, float nx, float ny, float nz, int light) {
        vc.vertex(m, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(n, nx, ny, nz)
                .endVertex();
    }
}
