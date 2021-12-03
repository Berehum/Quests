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
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RaidTaskType extends BukkitTaskType {

    private final BukkitQuestsPlugin plugin;

    public RaidTaskType(BukkitQuestsPlugin plugin) {
        super("raid", TaskUtils.TASK_ATTRIBUTION_STRING, "Start or win a raid.");
        this.plugin = plugin;
    }

    @Override
    public @NotNull List<ConfigProblem> validateConfig(@NotNull String root, @NotNull HashMap<String, Object> config) {
        ArrayList<ConfigProblem> problems = new ArrayList<>();
        if (TaskUtils.configValidateExists(root + ".amount", config.get("amount"), problems, "amount", super.getType()))
            TaskUtils.configValidateInt(root + ".amount", config.get("amount"), problems, false, true, "amount");
        if (config.containsKey("must-win")) {
            TaskUtils.configValidateBoolean(root + ".must-win", config.get("must-win"), problems, false, "must-win");
        }
        return problems;
    }

    private void checkShouldIncrement(Player player, boolean raidWon) {
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

                if (task.getConfigValues().containsKey("must-win")) {
                    Boolean mustWin = (Boolean) task.getConfigValue("must-win");
                    if (mustWin == Boolean.TRUE && !raidWon) continue;
                    if (mustWin == Boolean.FALSE && raidWon) continue;
                } else if (raidWon) {
                    continue;
                }

                int progress = (taskProgress.getProgress() == null) ? 0 : (int) taskProgress.getProgress();
                taskProgress.setProgress(progress + 1);

                int amount = (int) task.getConfigValue("amount");
                if (((int) taskProgress.getProgress()) >= amount) {
                    taskProgress.setProgress(amount);
                    taskProgress.setCompleted(true);
                }
            }

        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRaidStart(RaidTriggerEvent event) {
        checkShouldIncrement(event.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRaidWin(RaidFinishEvent event) {
        for (Player player : event.getWinners()) {
            checkShouldIncrement(player, true);
        }
    }

}
