package com.twogether.core;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The stemmed glass, empty or holding a drink (a data component). An empty glass is placed like
 * any block. A full one is drunk when used, and set down only when sneaking, so the player does
 * not lose their cocktail on the table by accident.
 */
public class StemmedGlassItem extends BlockItem {

    public StemmedGlassItem(Block block, Properties properties) {
        super(block, properties.stacksTo(16));
    }

    public static Drink contents(ItemStack stack) {
        return stack.getOrDefault(TwoGetherCoreMod.DRINK.get(), Drink.EMPTY);
    }

    @Nullable
    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(StemmedGlassBlock.DRINK, contents(context.getItemInHand()));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (contents(context.getItemInHand()) != Drink.EMPTY && (player == null || !player.isSecondaryUseActive())) {
            return InteractionResult.PASS;
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (contents(player.getItemInHand(hand)) == Drink.EMPTY) return InteractionResultHolder.pass(player.getItemInHand(hand));
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity drinker) {
        contents(stack).drink(drinker);
        if (drinker instanceof Player player) {
            if (player.getAbilities().instabuild) return stack;
            if (stack.getCount() > 1) {
                stack.shrink(1);
                ItemStack empty = StemmedGlassBlock.glassStack(Drink.EMPTY);
                if (!player.getInventory().add(empty)) player.drop(empty, false);
                return stack;
            }
        }
        return StemmedGlassBlock.glassStack(Drink.EMPTY);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return contents(stack) == Drink.EMPTY ? UseAnim.NONE : UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return contents(stack) == Drink.EMPTY ? 0 : 32;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public Component getName(ItemStack stack) {
        Drink drink = contents(stack);
        return drink == Drink.EMPTY ? super.getName(stack)
                : Component.translatable("item.twogethercore.stemmed_glass.filled", drink.displayName());
    }
}
