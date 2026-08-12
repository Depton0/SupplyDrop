package com.depton.supplydrop.drop;

import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class LootItem {

    //item template and weight fields
    private final ItemStack item;
    private final int weight;
    private final int minAmount;
    private final int maxAmount;

    //construct weighted loot entry
    public LootItem(ItemStack item, int weight, int minAmount, int maxAmount) {
        this.item = item;
        this.weight = Math.max(1, weight);
        this.minAmount = Math.max(1, minAmount);
        this.maxAmount = Math.max(this.minAmount, maxAmount);
    }

    //generate item stack with random count
    public ItemStack createItemStack() {
        if (item == null) {
            return null;
        }
        ItemStack result = item.clone();
        int amount = minAmount >= maxAmount ? minAmount : ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);
        result.setAmount(amount);
        return result;
    }

    //getters for item properties
    public ItemStack getItem() { return item; }
    public int getWeight() { return weight; }
    public int getMinAmount() { return minAmount; }
    public int getMaxAmount() { return maxAmount; }
}
