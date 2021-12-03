package com.leonardobishop.quests.bukkit.tasktype.type;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.leonardobishop.quests.bukkit.BukkitQuestsPlugin;
import com.leonardobishop.quests.bukkit.item.ParsedQuestItem;
import com.leonardobishop.quests.bukkit.item.QuestItem;
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
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InteractEntityTaskType extends BukkitTaskType {

    private final BukkitQuestsPlugin plugin;
    private final Table<String, String, QuestItem> fixedQuestItemCache = HashBasedTable.create();

    public InteractEntityTaskType(BukkitQuestsPlugin plugin) {
        super("interactentity", TaskUtils.TASK_ATTRIBUTION_STRING, "Interact with a specific entity.");
        this.plugin = plugin;
    }

    @Override
    public void onReady() {
        fixedQuestItemCache.clear();
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

                if (task.getConfigValues().containsKey("equipment-slot")) {
                    EquipmentSlot equipmentSlot = null;
                    try {
                        equipmentSlot = EquipmentSlot.valueOf(String.valueOf(task.getConfigValue("equipment-slot")));
                    } catch (Exception ignored) {}
                    if (event.getHand() != equipmentSlot) continue;
                }

                List<String> mob = (List<String>) task.getConfigValue("mobs", new ArrayList<>());
                if (task.getConfigValues().containsKey("mob")) {
                    mob.add(String.valueOf(task.getConfigValue("mob")));
                }
                if (!mob.contains(entity.getType().name())) continue;

                if (task.getConfigValues().containsKey("names") || task.getConfigValues().containsKey("name")) {
                    List<String> configNames = (List<String>) task.getConfigValue("names", new ArrayList<>());
                    if (task.getConfigValues().containsKey("name")) {
                        configNames.add(String.valueOf(task.getConfigValue("name")));
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

                    ItemStack hand = player.getInventory().getItem(event.getHand());
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

        if (config.containsKey("item")) {
            TaskUtils.configValidateItemStack(root + ".item", config.get("item"), problems, false, "item");
        }
        if (config.containsKey("data")) {
            TaskUtils.configValidateInt(root + ".data", config.get("data"), problems, true, false, "data");
        }

        if (config.containsKey("equipment-slot")) {
            TaskUtils.configValidateEnum(root + ".equipment-slot", config.get("equipment-slot"), problems, false, EquipmentSlot.class, "equipment-slot");
        }

        return problems;
    }


}
