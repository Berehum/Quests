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
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InteractBlockTaskType extends BukkitTaskType {

    private final BukkitQuestsPlugin plugin;
    private final Table<String, String, QuestItem> fixedQuestItemCache = HashBasedTable.create();

    public InteractBlockTaskType(BukkitQuestsPlugin plugin) {
        super("interactblock", TaskUtils.TASK_ATTRIBUTION_STRING, "Interact with a specific block.");
        this.plugin = plugin;
    }

    @Override
    public void onReady() {
        fixedQuestItemCache.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (!player.isOnline() || player.hasMetadata("NPC") || block == null) return;

        QPlayer qPlayer = plugin.getPlayerManager().getPlayer(player.getUniqueId());

        if (qPlayer == null) return;

        for (Quest quest : super.getRegisteredQuests()) {
            if (!qPlayer.hasStartedQuest(quest)) continue;
            QuestProgress questProgress = qPlayer.getQuestProgressFile().getQuestProgress(quest);

            for (Task task : quest.getTasksOfType(super.getType())) {
                if (!TaskUtils.validateWorld(player, task)) continue;

                TaskProgress taskProgress = questProgress.getTaskProgress(task.getId());

                if (taskProgress.isCompleted()) {
                    continue;
                }

                Action action = null;
                try {
                    action = Action.valueOf(String.valueOf(task.getConfigValue("action")));
                } catch (Exception ignored) {}
                if (event.getAction() != action) continue;

                if (action != Action.PHYSICAL) {
                    if (task.getConfigValues().containsKey("equipment-slot")) {
                        EquipmentSlot equipmentSlot = null;
                        try {
                            equipmentSlot = EquipmentSlot.valueOf(String.valueOf(task.getConfigValue("equipment-slot")));
                        } catch (Exception ignored) {}
                        if (event.getHand() != equipmentSlot) continue;
                    }
                }


                List<String> blockConfig = (List<String>) task.getConfigValue("blocks", new ArrayList<>());
                if (task.getConfigValues().containsKey("block")) {
                    blockConfig.add(String.valueOf(task.getConfigValue("block")));
                }

                boolean found = false;
                for (String string : blockConfig) {
                    Material material = Material.matchMaterial(string);
                    if (material != block.getType()) continue;
                    found = true;
                    break;
                }

                if (!found) continue;

                Object configBlock = task.getConfigValue("item");
                if (configBlock != null) {
                    QuestItem qi = fixedQuestItemCache.get(quest.getId(), task.getId());
                    if (qi == null) {
                        if (configBlock instanceof ConfigurationSection) {
                            qi = plugin.getConfiguredQuestItem("", (ConfigurationSection) configBlock);
                        } else {
                            Object configData = task.getConfigValue("data");
                            Material material = Material.getMaterial(String.valueOf(configBlock));
                            ItemStack is;
                            if (material == null) {
                                continue;
                            }
                            if (configData != null) {
                                is = new ItemStack(material, 1, ((Integer) configData).shortValue());
                            } else {
                                is = new ItemStack(material, 1);
                            }
                            qi = new ParsedQuestItem("parsed", null, is);
                        }
                        fixedQuestItemCache.put(quest.getId(), task.getId(), qi);
                    }

                    ItemStack hand = event.getItem();
                    if (!qi.compareItemStack(hand)) continue;
                }


                int progress = (taskProgress.getProgress() == null) ? 0 : (int) taskProgress.getProgress();
                taskProgress.setProgress(progress + 1);

                int amount = (int) task.getConfigValue("amount");
                if ((int) taskProgress.getProgress() >= amount) {
                    taskProgress.setProgress(amount);
                    taskProgress.setCompleted(true);
                }
            }

        }

    }

    @Override
    public @NotNull List<ConfigProblem> validateConfig(@NotNull String root, @NotNull HashMap<String, Object> config) {
        ArrayList<ConfigProblem> problems = new ArrayList<>();

        Object block = config.get("block");
        Object blocks = config.get("blocks");

        if (blocks == null && block == null) {
            TaskUtils.configValidateExists(root + ".block", null, problems, "block", super.getType());
        }

        if (block != null) {
            TaskUtils.configValidateMaterial(root + ".block", block, problems, false, "block");
        }

        if (blocks != null) {
            List<String> stringList = TaskUtils.configValidateStringList(root + ".blocks", blocks, problems, false, "blocks");
            for (String string : stringList) {
                TaskUtils.configValidateMaterial(root + ".blocks", string, problems, false, "blocks");
            }
        }

        if (TaskUtils.configValidateExists(root + ".amount", config.get("amount"), problems, "amount", super.getType()))
            TaskUtils.configValidateInt(root + ".amount", config.get("amount"), problems, false, true, "amount");

        if (config.containsKey("item")) {
            TaskUtils.configValidateItemStack(root + ".item", config.get("item"), problems, false, "item");
        }
        if (config.containsKey("data")) {
            TaskUtils.configValidateInt(root + ".data", config.get("data"), problems, true, false, "data");
        }
        if (config.containsKey("action")) {
            TaskUtils.configValidateEnum(root + ".action", config.get("action"), problems, false, Action.class, "action");
        }
        if (config.containsKey("action")) {
            TaskUtils.configValidateEnum(root + ".action", config.get("action"), problems, false, Action.class, "action");
        }
        if (config.containsKey("equipment-slot")) {
            TaskUtils.configValidateEnum(root + ".equipment-slot", config.get("equipment-slot"), problems, false, EquipmentSlot.class, "equipment-slot");
        }
        return problems;
    }


}
