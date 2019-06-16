package com.builtbroken.handheldpiston.client;

import com.builtbroken.handheldpiston.HandheldPiston;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import org.apache.commons.lang3.tuple.Pair;

import javax.vecmath.Matrix4f;
import java.util.List;

public class PistonModel implements IBakedModel
{

    private final IBakedModel oldModel;

    public PistonModel(IBakedModel internal)
    {
        this.oldModel = internal;
    }

    @Override
    public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand)
    {
        return oldModel.getQuads(state, side, rand);
    }

    @Override
    public boolean isAmbientOcclusion()
    {
        return oldModel.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d()
    {
        return oldModel.isGui3d();
    }

    public IBakedModel getInternal()
    {
        return oldModel;
    }

    @Override
    public boolean isBuiltInRenderer()
    {
        return true;
    }

    @Override
    public TextureAtlasSprite getParticleTexture()
    {
        return oldModel.getParticleTexture();
    }

    @Override
    public ItemOverrideList getOverrides()
    {
        return ItemOverrideList.NONE;
    }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType type)
    {
        ((HandheldPistonRenderer) HandheldPiston.piston.getTileEntityItemStackRenderer()).transform = type;
        return Pair.of(this, oldModel.handlePerspective(type).getRight());
    }

}
