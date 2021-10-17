package com.jab125.egt.entities;

import com.jab125.egt.EGobT;
import net.hat.gt.GobT;
import net.hat.gt.GobTClient;
import net.hat.gt.entities.AbstractGoblinEntity;
import net.hat.gt.entities.GoblinTraderEntity;
import net.hat.gt.entities.GoblinTraderModel;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.GlowSquidEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3f;

public class EndGoblinTraderRenderer extends MobEntityRenderer<AbstractGoblinEntity, GoblinTraderModel<GoblinTraderEntity>> {


    public EndGoblinTraderRenderer(EntityRendererFactory.Context context) {
        super(context, new GoblinTraderModel<>(context.getPart(GobTClient.GOBLIN_MODEL_LAYER)), .5F);
        this.addFeature(new HeldItemFeatureRenderer<>(this));
    }

    @Override
    public Identifier getTexture(AbstractGoblinEntity entity) {
        return EGobT.endGoblinTexture();
    }

    public void render(AbstractGoblinEntity mobEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        matrixStack.push();
        if(mobEntity.isUsingItem())
        {
            matrixStack.translate(0, -0.15, 0);
        }
        if(mobEntity.isStunned() && mobEntity.isAlive() && GobT.config.GOBLINS_FALL)
        {
            float progress = Math.min(10F, mobEntity.getFallCounter() + g) / 10F;
            matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-mobEntity.getStunRotation()));
            matrixStack.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90F * progress));
            matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(mobEntity.getStunRotation()));
        }
        super.render(mobEntity, 0F, g, matrixStack, vertexConsumerProvider, i);
        matrixStack.pop();
    }
    protected int getBlockLight(AbstractGoblinEntity goblinEntity, BlockPos blockPos) {

        return Math.max(super.getBlockLight(goblinEntity, blockPos), 0);
    }
}
