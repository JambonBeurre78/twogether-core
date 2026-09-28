package com.twogether.core;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One segment of the Mixer's centre shaft. Blades are fitted by right-clicking with a Mixer
 * Blade and taken back with an empty hand. The block itself is just the shaft; the blades are
 * drawn by MixerRotorRenderer with Mekanism's turbine model, spinning while ACTIVE - which the
 * controller sets whenever the Mixer is running.
 */
public class MixerRotorBlock extends Block implements EntityBlock {

    public static final MapCodec<MixerRotorBlock> CODEC = simpleCodec(MixerRotorBlock::new);
    public static final IntegerProperty BLADES = IntegerProperty.create("blades", 0, MixerShape.MAX_BLADES_PER_ROTOR);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private static final VoxelShape SHAFT = Block.box(6, 0, 6, 10, 16, 10);

    public MixerRotorBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(BLADES, 0).setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BLADES, ACTIVE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MixerRotorBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAFT;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(TwoGetherCoreMod.MIXER_BLADE.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        int blades = state.getValue(BLADES);
        if (blades >= MixerShape.MAX_BLADES_PER_ROTOR) return ItemInteractionResult.CONSUME;
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(BLADES, blades + 1), Block.UPDATE_ALL);
            stack.consume(1, player);
            level.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.3F, 1.6F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        int blades = state.getValue(BLADES);
        if (blades == 0) return InteractionResult.PASS;
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(BLADES, blades - 1), Block.UPDATE_ALL);
            ItemStack blade = new ItemStack(TwoGetherCoreMod.MIXER_BLADE.get());
            if (!player.getInventory().add(blade)) player.drop(blade, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // Breaking a fitted rotor gives its blades back instead of deleting them.
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            int blades = state.getValue(BLADES);
            if (blades > 0) {
                Block.popResource(level, pos, new ItemStack(TwoGetherCoreMod.MIXER_BLADE.get(), blades));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
