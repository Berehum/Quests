package com.leonardobishop.quests.bukkit.tasktype.type;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.leonardobishop.quests.bukkit.BukkitQuestsPlugin;
import com.leonardobishop.quests.bukkit.item.ParsedQuestItem;
import com.leonardobishop.quests.bukkit.item.QuestItem;
import com.leonardobishop.quests.bukkit.tasktype.BukkitTaskType;
import com.leonardobishop.quests.bukkit.util.TaskUtils;
import com.leonardobishop.quests.common.config.ConfigProblem;
import com.leonardobishop.quests.common.player.QPlayer;
import com.leonardobishop.quests.common.player.questprogressfile.QuestProgress;
import com.leonardobishop.quests.common.player.questprogressfile.TaskProgress;
import com.leonardobishop.quests.common.quest.Quest;
import com.leonardobishop.quests.common.quest.Task;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class InventoryTaskType extends BukkitTaskType {

    private final BukkitQuestsPlugin plugin;
    private final Table<String, String, QuestItem> fixedQuestItemCache = HashBasedTable.create();

    public InventoryTaskType(BukkitQuestsPlugin plugin) {
        super("inventory", TaskUtils.TASK_ATTRIBUTION_STRING, "Obtain a set of items.");
        this.plugin = plugin;
    }

    @Override
    public @NotNull List<ConfigProblem> validateConfig(@NotNull String root, @NotNull HashMap<String, Object> config) {
        ArrayList<ConfigProblem> problems = new ArrayList<>();
        if (TaskUtils.configValidateExists(root + ".item", config.get("item"), problems, "item", super.getType()))
            TaskUtils.configValidateItemStack(root + ".item", config.get("item"), problems, false, "item");
        if (TaskUtils.configValidateExists(root + ".amount", config.get("amount"), problems, "amount", super.getType()))
            TaskUtils.configValidateInt(root + ".amount", config.get("amount"), problems, false, true, "amount");
        TaskUtils.configValidateInt(root + ".data", config.get("data"), problems, true, false, "data");
        TaskUtils.configValidateBoolean(root + ".remove-items-when-complete", config.get("remove-items-when-complete"), problems, true, "remove-items-when-complete", super.getType());
        TaskUtils.configValidateBoolean(root + ".update-progress", config.get("update-progress"), problems, true, "update-progress", super.getType());
        return problems;
    }

    @Override
    public void onReady() {
        fixedQuestItemCache.clear();
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(PlayerPickupItemEvent event) {
        if (event.getPlayer().hasMetadata("NPC")) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> this.checkInventory(event.getPlayer()), 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryCloseEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> checkInventory((Player) event.getPlayer()), 1L); //Still some work to do as it doesn't really work
    }

    @SuppressWarnings("deprecation")
    private void checkInventory(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        QPlayer qPlayer = plugin.getPlayerManager().getPlayer(player.getUniqueId());
        if (qPlayer == null) {
            return;
        }

        for (Quest quest : super.getRegisteredQuests()) {
            if (!qPlayer.hasStartedQuest(quest)) continue;
            QuestProgress questProgress = qPlayer.getQuestProgressFile().getQuestProgress(quest);

            for (Task task : quest.getTasksOfType(super.getType())) {
                if (!TaskUtils.validateWorld(player, task)) continue;

                TaskProgress taskProgress = questProgress.getTaskProgress(task.getId());

                if (taskProgress.isCompleted()) {
                    continue;
                }

                Material material;
                int amount = (int) task.getConfigValue("amount");
                Object configBlock = task.getConfigValue("item");
                Object configData = task.getConfigValue("data");
                Object remove = task.getConfigValue("remove-items-when-complete");

                QuestItem qi = fixedQuestItemCache.get(quest.getId(), task.getId());
                if (qi == null) {
                    if (configBlock instanceof ConfigurationSection) {
                        qi = plugin.getConfiguredQuestItem("", (ConfigurationSection) configBlock);
                    } else {
                        material = Material.getMaterial(String.valueOf(configBlock));
                        if (material == null) {
                            continue;
                        }
                        ItemStack is;
                        if (configData != null) {
                            is = new ItemStack(material, 1, ((Integer) configData).shortValue());
                        } else {
                            is = new ItemStack(material, 1);
                        }
                        qi = new ParsedQuestItem("parsed", null, is);
                    }
                    fixedQuestItemCache.put(quest.getId(), task.getId(), qi);
                }

                int[] amountPerSlot = getAmountsPerSlot(player, qi);
                int total = Math.min(amountPerSlot[36], amount);
                taskProgress.setProgress(total);

                if (total >= amount) {
                    taskProgress.setProgress(amount);
                    taskProgress.setCompleted(true);

                    if (remove != null && ((Boolean) remove)) removeItemsInSlots(player, amountPerSlot, total);
                }
            }

        }
    }

    private int[] getAmountsPerSlot(Player player, QuestItem qi) {
        int[] slotToAmount = new int[37];
        // idx 36 = total
        for (int i = 0; i < 36; i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot == null || !qi.compareItemStack(slot))
                continue;
            slotToAmount[36] = slotToAmount[36] + slot.getAmount();
            slotToAmount[i] = slot.getAmount();
        }
        return slotToAmount;
    }

    private void removeItemsInSlots(Player player, int[] amountPerSlot, int amountToRemove) {
        for (int i = 0; i < 36; i++) {
            if (amountPerSlot[i] == 0) continue;

            ItemStack slot = player.getInventory().getItem(i);
            if (slot == null) continue;

            int amountInStack = slot.getAmount();
            int min = Math.max(0, amountInStack - amountToRemove);
            slot.setAmount(min);
            amountToRemove = amountToRemove - amountInStack;
            if (amountToRemove <= 0) break;
        }
    }
}
