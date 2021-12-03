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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class PlayerKillingCertainTaskType extends BukkitTaskType {

    private final BukkitQuestsPlugin plugin;

    public PlayerKillingCertainTaskType(BukkitQuestsPlugin plugin) {
        super("playerkillingcertain", TaskUtils.TASK_ATTRIBUTION_STRING, "Kill a set amount of specific players.");
        this.plugin = plugin;
    }

    @Override
    public @NotNull List<ConfigProblem> validateConfig(@NotNull String root, @NotNull HashMap<String, Object> config) {
        ArrayList<ConfigProblem> problems = new ArrayList<>();

        Object player = config.get("player");
        Object players = config.get("players");

        if (players == null && player == null) {
            TaskUtils.configValidateExists(root + ".player", null, problems, "player", super.getType());
        }

        if (TaskUtils.configValidateExists(root + ".amount", config.get("amount"), problems, "amount", super.getType()))
            TaskUtils.configValidateInt(root + ".amount", config.get("amount"), problems, false, true, "amount");
        return problems;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();

        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player killed = (Player) event.getEntity();

        if (killer == null || killer == killed) {
            return;
        }

        if (killer.hasMetadata("NPC")) return;

        QPlayer qPlayer = plugin.getPlayerManager().getPlayer(killer.getUniqueId());
        if (qPlayer == null) {
            return;
        }

        for (Quest quest : super.getRegisteredQuests()) {
            if (!qPlayer.hasStartedQuest(quest)) continue;
            QuestProgress questProgress = qPlayer.getQuestProgressFile().getQuestProgress(quest);

            for (Task task : quest.getTasksOfType(super.getType())) {
                if (!TaskUtils.validateWorld(killer, task)) continue;

                TaskProgress taskProgress = questProgress.getTaskProgress(task.getId());

                if (taskProgress.isCompleted()) {
                    continue;
                }

                List<String> players = (List<String>) task.getConfigValue("players", new ArrayList<>());
                if (task.getConfigValues().containsKey("player")) {
                    players.add(String.valueOf(task.getConfigValue("player")));
                }

                boolean found = false;
                for (String player : players) {
                    if (killed.getName().equalsIgnoreCase(player)) found = true;
                    if (killed.getUniqueId().equals(UUID.fromString(player))) found = true;
                    if (found) break;
                }

                if (!found) continue;

                int progressKills = (taskProgress.getProgress() == null) ? 0 : (int) taskProgress.getProgress();
                taskProgress.setProgress(progressKills + 1);

                int playerKillsNeeded = (int) task.getConfigValue("amount");
                if (((int) taskProgress.getProgress()) >= playerKillsNeeded) {
                    taskProgress.setProgress(playerKillsNeeded);
                    taskProgress.setCompleted(true);
                }
            }
        }

    }

}
