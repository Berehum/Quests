package com.leonardobishop.quests.bukkit.command;

import com.leonardobishop.quests.bukkit.BukkitQuestsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

public class AdminCommandSwitcher extends CommandSwitcher {

    private final BukkitQuestsPlugin plugin;

    public AdminCommandSwitcher(BukkitQuestsPlugin plugin) {
        super(1);
        this.plugin = plugin;

        super.subcommands.put("opengui", new AdminOpenguiCommandSwitcher(plugin));
        super.subcommands.put("moddata", new AdminModdataCommandSwitcher(plugin));
        super.subcommands.put("types", new AdminTypesCommandHandler(plugin));
        super.subcommands.put("info", new AdminInfoCommandHandler(plugin));
        super.subcommands.put("reload", new AdminReloadCommandHandler(plugin));
        super.subcommands.put("items", new AdminItemsCommandHandler(plugin));
        super.subcommands.put("config", new AdminConfigCommandHandler(plugin));
        super.subcommands.put("update", new AdminUpdateCommandHandler(plugin));
        super.subcommands.put("wiki", new AdminWikiCommandHandler(plugin));
        super.subcommands.put("about", new AdminAboutCommandHandler(plugin));
    }

    @Override
    public void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GRAY.toString() + ChatColor.STRIKETHROUGH + "------------=[" + ChatColor.RED + " Quests Admin " + ChatColor.GRAY
                .toString() + ChatColor.STRIKETHROUGH + "]=------------");
        sender.sendMessage(ChatColor.GRAY + "The following commands are available: ");
        sender.sendMessage(ChatColor.DARK_GRAY + " * " + ChatColor.RED + "/quests a opengui " + ChatColor.DARK_GRAY + ": view help for opengui");
        sender.sendMessage(ChatColor.DARK_GRAY + " * " + ChatColor.RED + "/quests a moddata " + ChatColor.DARK_GRAY + ": view help for quest progression");
        sender.sendMessage(ChatColor.DARK_GRAY + " * " + ChatColor.RED + "/quests a types [type]" + ChatColor.DARK_GRAY + ": view registered task types");
        sender.sendMessage(ChatColor.DARK_GRAY + " * " + ChatColor.RED + "/quests a info [quest]" + ChatColor.DARK_GRAY + ": see information about loaded quests");
        sender.sendMessage(ChatColor.DARK_GRAY + " * " + ChatColor.RED + "/quests a items [import <id>] " + ChatColor.DARK_GRAY + ": view registered quest items");
        sender.sendMessage(ChatColor.DARK_GRAY + " * " + ChatColor.RED + "/quests a reload " + ChatColor.DARK_GRAY + ": reload Quests configuration");
        sender.sendMessage(ChatColor.DARK_GRAY + " * " + ChatColor.RED + "/quests a config " + ChatColor.DARK_GRAY + ": see detected problems in config");
        sender.sendMessage(ChatColor.DARK_GRAY + " * " + ChatColor.RED + "/quests a update " + ChatColor.DARK_GRAY + ": check for updates");
        sender.sendMessage(ChatColor.DARK_GRAY + " * " + ChatColor.RED + "/quests a wiki " + ChatColor.DARK_GRAY + ": get a link to the Quests wiki");
        sender.sendMessage(ChatColor.DARK_GRAY + " * " + ChatColor.RED + "/quests a about " + ChatColor.DARK_GRAY + ": get information about Quests");
    }

    @Override
    public @Nullable String getPermission() {
        return "quests.admin";
    }
}
