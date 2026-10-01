package com.twogether.core;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * The Bar Counter, the bartender's workstation. Its customer side, with the brass foot rail,
 * faces the player who places it.
 */
public class BarCounterBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<BarCounterBlock> CODEC = simpleCodec(BarCounterBlock::new);

    public BarCounterBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
}
