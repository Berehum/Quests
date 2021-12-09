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
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityTameEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TamingCertainTaskType extends BukkitTaskType {

    private final BukkitQuestsPlugin plugin;

    public TamingCertainTaskType(BukkitQuestsPlugin plugin) {
        super("tamingcertain", TaskUtils.TASK_ATTRIBUTION_STRING, "Tame a set amount of specific animals.");
        this.plugin = plugin;
    }

    @Override
    public @NotNull List<ConfigProblem> validateConfig(@NotNull String root, @NotNull HashMap<String, Object> config) {
        ArrayList<ConfigProblem> problems = new ArrayList<>();

        Object mob = config.get("mob");
        Object mobs = config.get("mobs");

        if (mobs == null && mob == null) {
            TaskUtils.configValidateExists(root + ".mob", null, problems, "mob", super.getType());
        }

        if (mob != null) {
            TaskUtils.configValidateEnum(root + ".mob", mob, problems, false, EntityType.class, "mob");
        }

        if (mobs != null) {
            List<String> stringList = TaskUtils.configValidateStringList(root + ".mobs", mobs, problems, false, "mobs");
            for (String string : stringList) {
                TaskUtils.configValidateEnum(root + ".mobs", string, problems, false, EntityType.class, "mobs");
            }
        }

        if (TaskUtils.configValidateExists(root + ".amount", config.get("amount"), problems, "amount", super.getType()))
            TaskUtils.configValidateInt(root + ".amount", config.get("amount"), problems, false, true, "amount");
        return problems;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getOwner();

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

                List<String> mob = (List<String>) task.getConfigValue("mobs", new ArrayList<>());
                if (task.getConfigValues().containsKey("mob")) {
                    mob.add(String.valueOf(task.getConfigValue("mob")));
                }
                if (!mob.contains(event.getEntity().getType().name())) continue;


                int tamesNeeded = (int) task.getConfigValue("amount");

                int progressTamed = (taskProgress.getProgress() == null) ? 0 : (int) taskProgress.getProgress();
                taskProgress.setProgress(progressTamed + 1);

                if (((int) taskProgress.getProgress()) >= tamesNeeded) {
                    taskProgress.setProgress(tamesNeeded);
                    taskProgress.setCompleted(true);
                }
            }
        }
    }

}
