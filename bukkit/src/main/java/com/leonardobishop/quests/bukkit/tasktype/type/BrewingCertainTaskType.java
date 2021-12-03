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
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BrewingCertainTaskType extends BukkitTaskType {

    private final BukkitQuestsPlugin plugin;
    private final HashMap<Location, UUID> brewingStands = new HashMap<>();

    public BrewingCertainTaskType(BukkitQuestsPlugin plugin) {
        super("brewingcertain", TaskUtils.TASK_ATTRIBUTION_STRING, "Brew a specific potion.");
        this.plugin = plugin;
    }

    @Override
    public @NotNull List<ConfigProblem> validateConfig(@NotNull String root, @NotNull HashMap<String, Object> config) {
        ArrayList<ConfigProblem> problems = new ArrayList<>();
        if (TaskUtils.configValidateExists(root + ".amount", config.get("amount"), problems, "amount", super.getType()))
            TaskUtils.configValidateInt(root + ".amount", config.get("amount"), problems, false, true, "amount");

        if (TaskUtils.configValidateExists(root + ".potion-type", config.get("potion-type"), problems, "potion-type", super.getType()))
            TaskUtils.configValidateEnum(root + ".potion-type", config.get("potion-type"), problems, false, PotionType.class, "potion-type");

        return problems;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock().getType() != Material.BREWING_STAND) return;
        brewingStands.put(event.getClickedBlock().getLocation(), event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BrewEvent event) {
        UUID uuid = brewingStands.get(event.getBlock().getLocation());
        if (uuid == null) return;
        Player player = Bukkit.getPlayer(uuid);

        if (player == null || player.hasMetadata("NPC")) {
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

                int progress;
                if (taskProgress.getProgress() == null) {
                    progress = 0;
                } else {
                    progress = (int) taskProgress.getProgress();
                }

                BrewerInventory inventory = event.getContents();
                List<ItemStack> potions = Arrays.asList(inventory.getItem(0), inventory.getItem(1), inventory.getItem(2));
                PotionType type = PotionType.valueOf(String.valueOf(task.getConfigValue("potion-type")));

                taskProgress.setProgress(progress + checkPotions(potions, type));

                int potionsNeeded = (int) task.getConfigValue("amount");
                if (((int) taskProgress.getProgress()) >= potionsNeeded) {
                    taskProgress.setProgress(potionsNeeded);
                    taskProgress.setCompleted(true);
                }
            }
        }

    }

    private int checkPotions(List<ItemStack> itemStacks, PotionType potionType) {
        int toIncrement = 0;
        for (ItemStack itemStack : itemStacks) {
            final ItemMeta itemMeta = itemStack.getItemMeta();
            if (!(itemMeta instanceof PotionMeta)) continue;
            PotionMeta potionMeta = (PotionMeta) itemMeta;
            if( potionMeta.getBasePotionData().getType() != potionType) continue;
            toIncrement++;
        }
        return toIncrement;
    }


}

