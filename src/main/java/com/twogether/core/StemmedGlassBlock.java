package com.twogether.core;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A stemmed glass standing on the floor or a table. Use a spirit bottle on an empty one to pour a
 * sip; right-click a full one with an empty hand to drink it, or an empty one to take it back
 * (sneak to take a full one without drinking).
 */
public class StemmedGlassBlock extends Block {

    public static final MapCodec<StemmedGlassBlock> CODEC = simpleCodec(StemmedGlassBlock::new);
    public static final EnumProperty<Drink> DRINK = EnumProperty.create("drink", Drink.class);

    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 10, 11);

    public StemmedGlassBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(DRINK, Drink.EMPTY));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DRINK);
    }

    static ItemStack glassStack(Drink drink) {
        ItemStack stack = new ItemStack(TwoGetherCoreMod.STEMMED_GLASS.get());
        if (drink != Drink.EMPTY) stack.set(TwoGetherCoreMod.DRINK.get(), drink);
        return stack;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof SpiritBottleItem bottle) || state.getValue(DRINK) != Drink.EMPTY) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(DRINK, bottle.drink()), Block.UPDATE_ALL);
            SpiritBottleItem.takeSip(player, hand, stack);
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.8F, 1.2F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        Drink drink = state.getValue(DRINK);
        if (!level.isClientSide) {
            if (drink != Drink.EMPTY && !player.isSecondaryUseActive()) {
                drink.drink(player);
                level.setBlock(pos, state.setValue(DRINK, Drink.EMPTY), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.8F, 1.0F);
            } else {
                ItemStack glass = glassStack(drink);
                level.removeBlock(pos, false);
                if (!player.getInventory().add(glass)) player.drop(glass, false);
                level.playSound(null, pos, SoundEvents.GLASS_HIT, SoundSource.BLOCKS, 0.6F, 1.6F);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return glassStack(state.getValue(DRINK));
    }
}
