package com.leonardobishop.quests.bukkit.tasktype.type;

import com.leonardobishop.quests.bukkit.BukkitQuestsPlugin;
import com.leonardobishop.quests.bukkit.tasktype.BukkitTaskType;
import com.leonardobishop.quests.bukkit.util.TaskUtils;
import com.leonardobishop.quests.common.config.ConfigProblem;
import com.leonardobishop.quests.common.config.ConfigProblemDescriptions;
import com.leonardobishop.quests.common.player.QPlayer;
import com.leonardobishop.quests.common.player.questprogressfile.QuestProgress;
import com.leonardobishop.quests.common.player.questprogressfile.TaskProgress;
import com.leonardobishop.quests.common.quest.Quest;
import com.leonardobishop.quests.common.quest.Task;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InteractBlockTaskType extends BukkitTaskType {

    private final BukkitQuestsPlugin plugin;

    public InteractBlockTaskType(BukkitQuestsPlugin plugin) {
        super("interactblock", TaskUtils.TASK_ATTRIBUTION_STRING, "Interact with a specific block.");
        this.plugin = plugin;
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

                Object configAction = task.getConfigValue("action");
                Action action = null;
                try {
                    action = Action.valueOf((String) configAction);
                } catch (Exception ignored) {}

                if (action != null && event.getAction() != action) {
                    continue;
                }
                if (action != Action.PHYSICAL) {
                    if (event.getHand() != EquipmentSlot.HAND) continue;
                }

                if (action != null && event.getAction() != action) {
                    continue;
                }

                Object configBlock = task.getConfigValue("block");
                Material material = Material.getMaterial((String) configBlock);

                if (block.getType() != material) {
                    continue;
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
        if (TaskUtils.configValidateExists(root + ".block", config.get("block"), problems, "block", super.getType())) {
            if (Material.getMaterial(String.valueOf(config.get("block"))) == null) {
                problems.add(new ConfigProblem(ConfigProblem.ConfigProblemType.WARNING,
                        ConfigProblemDescriptions.UNKNOWN_MATERIAL.getDescription(String.valueOf(config.get("block"))), root + ".block"));
            }
        }
        if (TaskUtils.configValidateExists(root + ".amount", config.get("amount"), problems, "amount", super.getType()))
            TaskUtils.configValidateInt(root + ".amount", config.get("amount"), problems, false, true, "amount");

        if (config.containsKey("action")) {
            try {
                Action.valueOf(String.valueOf(config.get("action")));
            } catch (IllegalArgumentException ex) {
                problems.add(new ConfigProblem(ConfigProblem.ConfigProblemType.WARNING,
                        ConfigProblemDescriptions.UNKNOWN_ENTITY_TYPE.getDescription(String.valueOf(config.get("action"))), root + ".action"));
            }
        }
        return problems;
    }


}
