package com.leonardobishop.quests.bukkit.tasktype.type;

import com.leonardobishop.quests.bukkit.BukkitQuestsPlugin;
import com.leonardobishop.quests.bukkit.tasktype.BukkitTaskType;
import com.leonardobishop.quests.bukkit.util.TaskUtils;
import com.leonardobishop.quests.common.config.ConfigProblem;
import com.leonardobishop.quests.common.player.QPlayer;
import com.leonardobishop.quests.common.player.questprogressfile.QuestProgress;
import com.leonardobishop.quests.common.player.questprogressfile.TaskProgress;
import com.leonardobishop.quests.common.quest.Quest;
import com.leonardobishop.quests.common.quest.Task;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class WalkingTaskType extends BukkitTaskType {

    private final BukkitQuestsPlugin plugin;

    public WalkingTaskType(BukkitQuestsPlugin plugin) {
        super("walking", TaskUtils.TASK_ATTRIBUTION_STRING, "Walk a set distance.");
        this.plugin = plugin;
    }

    @Override
    public @NotNull List<ConfigProblem> validateConfig(@NotNull String root, @NotNull HashMap<String, Object> config) {
        ArrayList<ConfigProblem> problems = new ArrayList<>();
        if (TaskUtils.configValidateExists(root + ".distance", config.get("distance"), problems, "distance", super.getType()))
            TaskUtils.configValidateInt(root + ".distance", config.get("distance"), problems, false, true, "distance");
        return problems;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();

        if (player.hasMetadata("NPC")) return;

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

                if (task.getConfigValue("mode") != null && !validateTransportMethod(player, task.getConfigValue("mode").toString())) {
                    continue;
                }

                int distanceNeeded = (int) task.getConfigValue("distance");

                int progressDistance = (taskProgress.getProgress() == null) ? 0 : (int) taskProgress.getProgress();
                taskProgress.setProgress(progressDistance + 1);

                if (((int) taskProgress.getProgress()) >= distanceNeeded) {
                    taskProgress.setProgress(distanceNeeded);
                    taskProgress.setCompleted(true);
                }
            }

        }
    }

    private boolean validateTransportMethod(Player player, String mode) {
        switch (mode.toLowerCase()) {
            case "boat":
                return player.getVehicle() != null && player.getVehicle().getType() == EntityType.BOAT;
            case "horse":
                return player.getVehicle() != null && player.getVehicle().getType() == EntityType.HORSE;
            case "pig":
                return player.getVehicle() != null && player.getVehicle().getType() == EntityType.PIG;
            case "strider":
                return player.getVehicle() != null && player.getVehicle().getType() == EntityType.STRIDER;
            case "minecart":
                return player.getVehicle() != null && player.getVehicle().getType() == EntityType.MINECART;
            case "llama":
                return player.getVehicle() != null && player.getVehicle().getType() == EntityType.LLAMA;
            case "sneaking":
                return player.isSneaking();
            case "walking":
                return !player.isSprinting();
            case "running":
                return player.isSprinting();
            case "swimming":
                return player.isSwimming();
            default:
                return false;
        }
    }

}
