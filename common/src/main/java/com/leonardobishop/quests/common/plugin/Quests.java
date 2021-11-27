package com.leonardobishop.quests.common.plugin;

import com.leonardobishop.quests.common.config.QuestsConfig;
import com.leonardobishop.quests.common.logger.QuestsLogger;
import com.leonardobishop.quests.common.player.QPlayerManager;
import com.leonardobishop.quests.common.quest.QuestCompleter;
import com.leonardobishop.quests.common.quest.QuestManager;
import com.leonardobishop.quests.common.questcontroller.QuestController;
import com.leonardobishop.quests.common.scheduler.ServerScheduler;
import com.leonardobishop.quests.common.storage.StorageProvider;
import com.leonardobishop.quests.common.tasktype.TaskTypeManager;
import com.leonardobishop.quests.common.updater.Updater;
import org.jetbrains.annotations.NotNull;

public interface Quests {

    /**
     * Obtain an instance of the Quests logger.
     *
     * @return quests logger
     * @see QuestsLogger
     */
    @NotNull QuestsLogger getQuestsLogger();

    /**
     * Obtain an instance of the QuestManager.
     *
     * @return quest manager
     * @see QuestManager
     */
    @NotNull QuestManager getQuestManager();

    /**
     * Obtain an instance of the QPlayerManager.
     *
     * @return quest player manager
     * @see QPlayerManager
     */
    @NotNull QPlayerManager getPlayerManager();

    /**
     * Obtain an instance of the QuestController.
     *
     * @return quest controller
     * @see QuestController
     */
    @NotNull QuestController getQuestController();

    /**
     * Obtain an instance of the TaskTypeManager.
     *
     * @return task type manager
     * @see TaskTypeManager
     */
    @NotNull TaskTypeManager getTaskTypeManager();

    /**
     * Obtain an instance of the QuestCompleter.
     *
     * @return quest completer
     * @see QuestCompleter
     */
    @NotNull QuestCompleter getQuestCompleter();

    /**
     * Obtain an instance of the QuestConfig.
     *
     * @return quest config
     * @see QuestsConfig
     */
    @NotNull QuestsConfig getQuestsConfig();

    /**
     * Obtain an instance of the Updater.
     *
     * @return updater
     * @see Updater
     */
    @NotNull Updater getUpdater();

    /**
     * Obtain an instance of the ServerScheduler.
     *
     * @return server scheduler
     * @see ServerScheduler
     */
    @NotNull ServerScheduler getScheduler();

    /**
     * Obtain an instance of the StorageProvider.
     *
     * @return storage provider
     * @see StorageProvider
     */
    @NotNull StorageProvider getStorageProvider();

    /**
     * Performs a full reload of the plugin, unloading and re-registering quests to their task types.
     */
    void reloadQuests();

}
