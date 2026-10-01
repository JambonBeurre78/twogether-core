package com.twogether.core;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A spirit bottle set down on the floor or a table. Which spirit and how many sips are left live
 * in the block state, so no block entity is needed. Right-click with an empty hand to take it back.
 */
public class SpiritBottleBlock extends Block {

    public static final MapCodec<SpiritBottleBlock> CODEC = simpleCodec(SpiritBottleBlock::new);
    public static final EnumProperty<Drink> SPIRIT = EnumProperty.create("spirit", Drink.class, Drink::isSpirit);
    public static final IntegerProperty SIPS = IntegerProperty.create("sips", 1, SpiritBottleItem.MAX_SIPS);

    private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 13, 10);

    public SpiritBottleBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(SPIRIT, Drink.WHISKY).setValue(SIPS, SpiritBottleItem.MAX_SIPS));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SPIRIT, SIPS);
    }

    /** Sets a bottle down where the player is looking; shared by the bottle items. */
    static InteractionResult place(UseOnContext context, SpiritBottleItem bottle, int sips) {
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        if (!placeContext.canPlace()) return InteractionResult.FAIL;
        Level level = context.getLevel();
        BlockPos pos = placeContext.getClickedPos();
        BlockState state = TwoGetherCoreMod.SPIRIT_BOTTLE_BLOCK.get().defaultBlockState()
                .setValue(SPIRIT, bottle.drink())
                .setValue(SIPS, Math.max(1, sips));
        if (!state.canSurvive(level, pos)) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            level.setBlock(pos, state, Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 0.6F, 1.4F);
            level.gameEvent(context.getPlayer(), GameEvent.BLOCK_PLACE, pos);
            Player player = context.getPlayer();
            if (player == null || !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    static ItemStack bottleStack(BlockState state) {
        return SpiritBottleItem.withSips(TwoGetherCoreMod.bottleFor(state.getValue(SPIRIT)), state.getValue(SIPS));
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            ItemStack bottle = bottleStack(state);
            level.removeBlock(pos, false);
            if (!player.getInventory().add(bottle)) player.drop(bottle, false);
            level.playSound(null, pos, SoundEvents.GLASS_HIT, SoundSource.BLOCKS, 0.6F, 1.4F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return bottleStack(state);
    }
}
