package com.leonardobishop.quests.bukkit.tasktype.type;

import com.leonardobishop.quests.bukkit.BukkitQuestsPlugin;
import com.leonardobishop.quests.bukkit.tasktype.BukkitTaskType;
import com.leonardobishop.quests.bukkit.util.TaskUtils;
import com.leonardobishop.quests.bukkit.util.chat.Chat;
import com.leonardobishop.quests.common.config.ConfigProblem;
import com.leonardobishop.quests.common.config.ConfigProblemDescriptions;
import com.leonardobishop.quests.common.player.QPlayer;
import com.leonardobishop.quests.common.player.questprogressfile.QuestProgress;
import com.leonardobishop.quests.common.player.questprogressfile.TaskProgress;
import com.leonardobishop.quests.common.quest.Quest;
import com.leonardobishop.quests.common.quest.Task;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InteractEntityTaskType extends BukkitTaskType {

    private final BukkitQuestsPlugin plugin;

    public InteractEntityTaskType(BukkitQuestsPlugin plugin) {
        super("interactentity", TaskUtils.TASK_ATTRIBUTION_STRING, "Interact with a specific entity.");
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        if (!player.isOnline() || player.hasMetadata("NPC") || entity instanceof Player) return;

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

                Object mob = task.getConfigValue("mob");
                EntityType type;
                try {
                    type = EntityType.valueOf((String) mob);
                } catch (Exception e) {
                    continue;
                }

                if (entity.getType() != type) {
                    continue;
                }

                Object configName = task.getConfigValues().containsKey("name") ? task.getConfigValue("name") : task.getConfigValue("names");

                if (configName != null) {
                    List<String> configNames = new ArrayList<>();
                    if (configName instanceof List) {
                        configNames.addAll((List) configName);
                    } else {
                        configNames.add(String.valueOf(configName));
                    }

                    boolean validName = false;
                    for (String name : configNames) {
                        name = Chat.color(name);
                        if (entity.getCustomName() == null || !entity.getCustomName().equals(name)) {
                            validName = true;
                            break;
                        }
                    }

                    if (!validName) continue;
                }

                int amount = (int) task.getConfigValue("amount");

                int progress = (taskProgress.getProgress() == null) ? 0 : (int) taskProgress.getProgress();
                taskProgress.setProgress(progress + 1);

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
        if (TaskUtils.configValidateExists(root + ".mob", config.get("mob"), problems, "mob", super.getType())) {
            try {
                EntityType.valueOf(String.valueOf(config.get("mob")));
            } catch (IllegalArgumentException ex) {
                problems.add(new ConfigProblem(ConfigProblem.ConfigProblemType.WARNING,
                        ConfigProblemDescriptions.UNKNOWN_ENTITY_TYPE.getDescription(String.valueOf(config.get("mob"))), root + ".mob"));
            }
        }
        if (TaskUtils.configValidateExists(root + ".amount", config.get("amount"), problems, "amount", super.getType()))
            TaskUtils.configValidateInt(root + ".amount", config.get("amount"), problems, false, true, "amount");

        return problems;
    }


}
