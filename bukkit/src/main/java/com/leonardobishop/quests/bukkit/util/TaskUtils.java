package com.leonardobishop.quests.bukkit.util;

import com.leonardobishop.quests.bukkit.BukkitQuestsPlugin;
import com.leonardobishop.quests.common.config.ConfigProblem;
import com.leonardobishop.quests.common.config.ConfigProblemDescriptions;
import com.leonardobishop.quests.common.quest.Task;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;

public class TaskUtils {

    public static final String TASK_ATTRIBUTION_STRING = "<built-in>";
    private static final BukkitQuestsPlugin plugin;

    static {
        plugin = BukkitQuestsPlugin.getPlugin(BukkitQuestsPlugin.class);
    }

    public static boolean validateWorld(Player player, Task task) {
        return validateWorld(player.getLocation().getWorld().getName(), task.getConfigValue("worlds"));
    }

    public static boolean validateWorld(String worldName, Task task) {
        return validateWorld(worldName, task.getConfigValue("worlds"));
    }

    public static boolean validateWorld(String worldName, Object configurationData) {
        if (configurationData == null) {
            return true;
        }

        if (configurationData instanceof List) {
            List allowedWorlds = (List) configurationData;
            if (!allowedWorlds.isEmpty() && allowedWorlds.get(0) instanceof String) {
                List<String> allowedWorldNames = (List<String>) allowedWorlds;
                return allowedWorldNames.contains(worldName);
            }
            return true;
        }

        if (configurationData instanceof String) {
            String allowedWorld = (String) configurationData;
            return worldName.equals(allowedWorld);
        }

        return true;
    }

    public static void configValidateNumber(String path, Object object, List<ConfigProblem> problems, boolean allowNull, boolean greaterThanZero, String... args) {
        if (object == null) {
            if (!allowNull) {
                problems.add(new ConfigProblem(ConfigProblem.ConfigProblemType.ERROR,
                        String.format("Expected a number for '%s', but got null instead", (Object[]) args), path));
            }
            return;
        }

        try {
            double d = Double.parseDouble(String.valueOf(object));
            if (greaterThanZero && d <= 0) {
                problems.add(new ConfigProblem(ConfigProblem.ConfigProblemType.ERROR,
                        String.format("Value for field '%s' must be greater than 0", (Object[]) args), path));
            }
        } catch (ClassCastException ex) {
            problems.add(new ConfigProblem(ConfigProblem.ConfigProblemType.ERROR,
                    String.format("Expected a number for '%s', but got '" + object + "' instead", (Object[]) args), path));
        }
    }

    public static void configValidateInt(String path, Object object, List<ConfigProblem> problems, boolean allowNull, boolean greaterThanZero, String... args) {
        if (object == null) {
            if (!allowNull) {
                problems.add(new ConfigProblem(ConfigProblem.ConfigProblemType.ERROR,
                        String.format("Expected an integer for '%s', but got null instead", (Object[]) args), path));
            }
            return;
        }

        try {
            Integer i = (Integer) object;
            if (greaterThanZero && i <= 0) {
                problems.add(new ConfigProblem(ConfigProblem.ConfigProblemType.ERROR,
                        String.format("Value for field '%s' must be greater than 0", (Object[]) args), path));
            }
        } catch (ClassCastException ex) {
            problems.add(new ConfigProblem(ConfigProblem.ConfigProblemType.ERROR,
                    String.format("Expected an integer for '%s', but got '" + object + "' instead", (Object[]) args), path));
        }
    }

    public static void configValidateBoolean(String path, Object object, List<ConfigProblem> problems, boolean allowNull, String... args) {
        if (object == null) {
            if (!allowNull) {
                problems.add(new ConfigProblem(ConfigProblem.ConfigProblemType.ERROR,
                        String.format("Expected a boolean for '%s', but got null instead", (Object[]) args), path));
            }
            return;
        }

        try {
            Boolean b = (Boolean) object;
        } catch (ClassCastException ex) {
            problems.add(new ConfigProblem(ConfigProblem.ConfigProblemType.ERROR,
                    String.format("Expected a boolean for '%s', but got '" + object + "' instead", (Object[]) args), path));
        }
    }

    public static void configValidateItemStack(String path, Object object, List<ConfigProblem> problems, boolean allowNull, String... args) {
        if (object == null) {
            if (!allowNull) {
                problems.add(new ConfigProblem(ConfigProblem.ConfigProblemType.ERROR,
                        String.format("Expected item configuration for '%s', but got null instead", (Object[]) args), path));
            }
            return;
        }

        if (object instanceof ConfigurationSection) {
            ConfigurationSection section = (ConfigurationSection) object;

            if (section.contains("quest-item")) {
                String type = section.getString("quest-item");
                if (plugin.getQuestItemRegistry().getItem(section.getString("quest-item")) == null) {
                    problems.add(new ConfigProblem(ConfigProblem.ConfigProblemType.WARNING,
                            ConfigProblemDescriptions.UNKNOWN_QUEST_ITEM.getDescription(type), path + ".item.quest-item"));
                }
            } else {
                String itemloc = "item";
                if (!section.contains("item")) {
                    itemloc = "type";
                }
                if (!section.contains(itemloc)) {
                    problems.add(new ConfigProblem(ConfigProblem.ConfigProblemType.WARNING,
                            ConfigProblemDescriptions.UNKNOWN_MATERIAL.getDescription(""), path + ".type"));
                } else {
                    String type = String.valueOf(section.get(itemloc));
                    if (!plugin.getItemGetter().isValidMaterial(type)) {
                        problems.add(new ConfigProblem(ConfigProblem.ConfigProblemType.WARNING,
                                ConfigProblemDescriptions.UNKNOWN_MATERIAL.getDescription(type), path + itemloc));
                    }
                }
            }
        } else {
            if (Material.getMaterial(String.valueOf(object)) == null) {
                problems.add(new ConfigProblem(ConfigProblem.ConfigProblemType.WARNING,
                        ConfigProblemDescriptions.UNKNOWN_MATERIAL.getDescription(String.valueOf(object)), path));
            }
        }
    }

    public static List<String> configValidateStringList(String path, Object object, List<ConfigProblem> problems, boolean allowNull, String... args) {
        if (object == null) {
            if (!allowNull) {
                problems.add(new ConfigProblem(ConfigProblem.ConfigProblemType.ERROR,
                        String.format("Expected an stringlist for '%s', but got null instead", (Object[]) args), path));
            }
            return null;
        }

        try {
            List<String> list = (List<String>) object;
            return list;
        } catch (ClassCastException ex) {
            problems.add(new ConfigProblem(ConfigProblem.ConfigProblemType.ERROR,
                    String.format("Expected a stringlist for '%s', but got '" + object + "' instead", (Object[]) args), path));
        }
        return null;
    }

    public static void configValidateEnum(String path, Object object, List<ConfigProblem> problems, boolean allowNull, @NotNull Class<? extends Enum> enumClazz, String... args) {
        if (object == null) {
            if (!allowNull) {
                problems.add(new ConfigProblem(ConfigProblem.ConfigProblemType.ERROR,
                        String.format("Expected an enum for '%s', but got null instead", (Object[]) args), path));
            }
            return;
        }

        try {
            Enum.valueOf(enumClazz, String.valueOf(object));
        } catch (Exception ex) {
            problems.add(new ConfigProblem(ConfigProblem.ConfigProblemType.ERROR,
                    String.format("Expected an enum with type " + enumClazz.getSimpleName() + " for '%s', but got '" + object + "' instead", (Object[]) args), path));
        }
    }

    public static void configValidateMaterial(String path, Object object, List<ConfigProblem> problems, boolean allowNull, String... args) {
        if (object == null) {
            if (!allowNull) {
                problems.add(new ConfigProblem(ConfigProblem.ConfigProblemType.ERROR,
                        String.format("Expected a material for '%s', but got null instead", (Object[]) args), path));
            }
            return;
        }

        Material material = Material.matchMaterial(String.valueOf(object));

        if (material != null) return;
        problems.add(new ConfigProblem(ConfigProblem.ConfigProblemType.ERROR,
                    String.format("Expected a material for '%s', but got '" + object + "' instead", (Object[]) args), path));

    }

    public static boolean configValidateExists(String path, Object object, List<ConfigProblem> problems, String... args) {
        if (object == null) {
            problems.add(new ConfigProblem(ConfigProblem.ConfigProblemType.ERROR,
                    String.format(ConfigProblemDescriptions.TASK_MISSING_FIELD.getDescription(args), (Object[]) args), path));
            return false;
        }
        return true;
    }
}
