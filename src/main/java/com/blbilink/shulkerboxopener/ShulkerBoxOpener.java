package com.blbilink.shulkerboxopener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.block.ShulkerBox;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShulkerBoxOpener extends JavaPlugin implements Listener {

    private Map<UUID, ShulkerBoxData> openShulkerBoxes = new HashMap<>();

    private static class ShulkerBoxData {
        ItemStack item;
        EquipmentSlot hand;

        ShulkerBoxData(ItemStack item, EquipmentSlot hand) {
            this.item = item;
            this.hand = hand;
        }
    }

    @Override
    public void onEnable() {
        int pluginId = 22517; // <-- Replace with the id of your plugin!
        Metrics metrics = new Metrics(this, pluginId);

        // Optional: Add custom charts
        metrics.addCustomChart(new Metrics.SimplePie("chart_id", () -> "My value"));
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("ShulkerBoxOpener has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ShulkerBoxOpener has been disabled!");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        EquipmentSlot hand = event.getHand();

        if ((event.getAction() == Action.RIGHT_CLICK_AIR)
                && isShulkerBox(item) && hand != null) {
            event.setCancelled(true);
            openShulkerBox(player, item, hand);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (openShulkerBoxes.containsKey(playerUUID)) {
            ShulkerBoxData data = openShulkerBoxes.get(playerUUID);
            updateShulkerBoxContents(player, data.item, event.getInventory(), data.hand);
            openShulkerBoxes.remove(playerUUID);
        }
    }

    private boolean isShulkerBox(ItemStack item) {
        return item != null && item.getType().name().endsWith("SHULKER_BOX");
    }

    private void openShulkerBox(Player player, ItemStack item, EquipmentSlot hand) {
        if (item.getItemMeta() instanceof BlockStateMeta) {
            BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
            if (meta.getBlockState() instanceof ShulkerBox) {
                ShulkerBox shulkerBox = (ShulkerBox) meta.getBlockState();
                Inventory inv = Bukkit.createInventory(null, 27, "Shulker Box");
                inv.setContents(shulkerBox.getInventory().getContents());
                player.openInventory(inv);
                openShulkerBoxes.put(player.getUniqueId(), new ShulkerBoxData(item, hand));
            } else {
                getLogger().warning("Attempted to open non-shulker box item for player " + player.getName());
            }
        } else {
            getLogger().warning("Invalid item meta for shulker box item held by player " + player.getName());
        }
    }

    private void updateShulkerBoxContents(Player player, ItemStack item, Inventory inventory, EquipmentSlot hand) {
        try {
            if (item.getItemMeta() instanceof BlockStateMeta) {
                BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
                if (meta.getBlockState() instanceof ShulkerBox) {
                    ShulkerBox shulkerBox = (ShulkerBox) meta.getBlockState();
                    shulkerBox.getInventory().setContents(inventory.getContents());
                    meta.setBlockState(shulkerBox);
                    item.setItemMeta(meta);

                    // 更新玩家对应手中的物品
                    if (hand == EquipmentSlot.HAND) {
                        player.getInventory().setItemInMainHand(item);
                    } else if (hand == EquipmentSlot.OFF_HAND) {
                        player.getInventory().setItemInOffHand(item);
                    }

                    player.updateInventory();
                    getLogger().info("Successfully updated shulker box contents for player " + player.getName() + " in " + hand);
                } else {
                    getLogger().warning("Invalid block state when updating shulker box for player " + player.getName());
                }
            } else {
                getLogger().warning("Invalid item meta when updating shulker box for player " + player.getName());
            }
        } catch (Exception e) {
            getLogger().severe("Error updating shulker box contents for player " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}