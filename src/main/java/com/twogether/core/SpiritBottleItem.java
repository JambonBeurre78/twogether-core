package com.twogether.core;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A bottle of spirit, drunk a sip at a time. Full bottles stack; an opened one carries its sips
 * left and shows them as a durability bar. Sneak-use on a block sets the bottle down; using it on
 * a stemmed glass pours a sip into the glass (handled by the glass). Crafting a cocktail takes a
 * sip and hands the bottle back.
 */
public class SpiritBottleItem extends Item {

    public static final int MAX_SIPS = 4;
    private static final int BAR_COLOR = 0xD08A3C;

    private final Drink drink;

    public SpiritBottleItem(Drink drink, Properties properties) {
        super(properties.stacksTo(16));
        this.drink = drink;
    }

    public Drink drink() {
        return drink;
    }

    public static int sips(ItemStack stack) {
        return stack.getOrDefault(TwoGetherCoreMod.SIPS.get(), MAX_SIPS);
    }

    /** One bottle with the given sips left; full bottles carry no component so they stack. */
    public static ItemStack withSips(Item bottle, int sips) {
        if (sips <= 0) return new ItemStack(Items.GLASS_BOTTLE);
        ItemStack stack = new ItemStack(bottle);
        if (sips < MAX_SIPS) stack.set(TwoGetherCoreMod.SIPS.get(), sips);
        return stack;
    }

    /** This bottle after one sip: the same bottle a sip lighter, or the empty glass bottle. */
    public static ItemStack afterSip(ItemStack stack) {
        return withSips(stack.getItem(), sips(stack) - 1);
    }

    /**
     * Replaces one bottle of the stack in the player's hand with what is left after a sip. Used
     * when drinking and when pouring into a glass.
     */
    public static void takeSip(Player player, InteractionHand hand, ItemStack stack) {
        if (player.getAbilities().instabuild) return;
        ItemStack after = afterSip(stack);
        if (stack.getCount() == 1) {
            player.setItemInHand(hand, after);
        } else {
            stack.shrink(1);
            if (!player.getInventory().add(after)) player.drop(after, false);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity drinker) {
        drink.drink(drinker);
        if (drinker instanceof Player player) {
            if (player.getAbilities().instabuild) return stack;
            if (stack.getCount() > 1) {
                ItemStack after = afterSip(stack);
                stack.shrink(1);
                if (!player.getInventory().add(after)) player.drop(after, false);
                return stack;
            }
        }
        return afterSip(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isSecondaryUseActive()) return InteractionResult.PASS;
        return SpiritBottleBlock.place(context, this, sips(context.getItemInHand()));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return sips(stack) < MAX_SIPS;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * sips(stack) / MAX_SIPS);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOR;
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return afterSip(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.twogethercore.sips", sips(stack), MAX_SIPS));
    }
}
