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
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class FarmingTaskType extends BukkitTaskType {

    public static BukkitQuestsPlugin plugin;

    public FarmingTaskType(BukkitQuestsPlugin plugin) {
        super("farming", TaskUtils.TASK_ATTRIBUTION_STRING, "Break a set amount of any crop.");
        this.plugin = plugin;
    }

    @Override
    public @NotNull List<ConfigProblem> validateConfig(@NotNull String root, @NotNull HashMap<String, Object> config) {
        ArrayList<ConfigProblem> problems = new ArrayList<>();
        if (TaskUtils.configValidateExists(root + ".amount", config.get("amount"), problems, "amount", super.getType()))
            TaskUtils.configValidateInt(root + ".amount", config.get("amount"), problems, false, true, "amount");
        return problems;
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!(event.getBlock().getState().getBlockData() instanceof Ageable)) {
            return;
        }

        QPlayer qPlayer = plugin.getPlayerManager().getPlayer(event.getPlayer().getUniqueId());
        if (qPlayer == null) {
            return;
        }

        Ageable crop = (Ageable) event.getBlock().getState().getBlockData();
        if (crop.getAge() != crop.getMaximumAge()) {
            return;
        }

        for (Quest quest : super.getRegisteredQuests()) {
            if (!qPlayer.hasStartedQuest(quest)) continue;
            QuestProgress questProgress = qPlayer.getQuestProgressFile().getQuestProgress(quest);

            for (Task task : quest.getTasksOfType(super.getType())) {
                if (!TaskUtils.validateWorld(event.getPlayer(), task)) continue;

                TaskProgress taskProgress = questProgress.getTaskProgress(task.getId());

                if (taskProgress.isCompleted()) {
                    continue;
                }

                int brokenBlocksNeeded = (int) task.getConfigValue("amount");

                int progressBlocksBroken;
                if (taskProgress.getProgress() == null) {
                    progressBlocksBroken = 0;
                } else {
                    progressBlocksBroken = (int) taskProgress.getProgress();
                }

                taskProgress.setProgress(progressBlocksBroken + 1);

                if (((int) taskProgress.getProgress()) >= brokenBlocksNeeded) {
                    taskProgress.setProgress(brokenBlocksNeeded);
                    taskProgress.setCompleted(true);
                }
            }
        }

    }

}
