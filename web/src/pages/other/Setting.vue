<script lang="ts" setup>
import { Locator } from '@/data';
import { Setting } from '@/data/setting/Setting';
import SoundAllTaskCompleted from '@/sound/all_task_completed.mp3';
import { InfoOutlined } from '@vicons/material';

const message = useMessage();

const { setting } = Locator.settingRepository();

const clearWebSearchHistory = () => {
  Locator.webSearchHistoryRepository().clear();
  message.success('Cleared successfully');
};

const clearWenkuSearchHistory = () => {
  Locator.wenkuSearchHistoryRepository().clear();
  message.success('Cleared successfully');
};

const playSound = (source: string) => {
  return new Audio(source).play();
};
</script>

<template>
  <div class="layout-content">
    <n-h1>Settings</n-h1>

    <n-list bordered>
      <n-list-item>
        <n-flex vertical>
          <b>Theme</b>
          <c-radio-group
            v-model:value="setting.theme"
            :options="Setting.themeOptions"
            size="small"
          />
        </n-flex>
      </n-list-item>

      <n-list-item>
        <n-flex vertical>
          <b>Shortcut Keys</b>
          <n-ul>
            <n-li>On the list page, you can use the left and right arrow keys to turn pages.</n-li>
            <n-li>For the GPT/Sakura queue button, hold down the Ctrl key and click to automatically pin the task to the top.</n-li>
            <n-li>On the reading page, you can use the left and right arrow keys to jump to the previous/next chapter.</n-li>
            <n-li>On the reading page, you can use the number keys 1-4 to quickly switch translations.</n-li>
          </n-ul>
        </n-flex>
      </n-list-item>

      <n-list-item>
        <n-flex vertical>
          <b>Web Novel Table of Contents</b>
          <n-checkbox v-model:checked="setting.tocCollapseInNarrowScreen">
            Collapse table of contents in sidebar (mobile)
          </n-checkbox>
          <n-checkbox v-model:checked="setting.tocExpandAll">
            Expand all chapters by default
            <n-tooltip trigger="hover" placement="top" style="max-width: 400px">
              <template #trigger>
                <n-button text @click.stop>
                  <n-icon depth="4" :component="InfoOutlined" size="12" />
                </n-button>
              </template>
              On: Expand all chapters by default (may cause performance issues)
              <br />
              Off: Only expand the last read chapter (if there is no record, expand the first chapter)
              <br />
              Does not affect web novels without chapters
            </n-tooltip>
          </n-checkbox>
          <b>Comments</b>
          <n-checkbox v-model:checked="setting.hideCommmentWebNovel">
            Hide web novel comments
          </n-checkbox>
          <n-checkbox v-model:checked="setting.hideCommmentWenkuNovel">
            Hide Wenku novel comments
          </n-checkbox>
          <b>Favorites</b>
          <n-checkbox v-model:checked="setting.showTagInWebFavored">
            Show tags of web novels in favorites
          </n-checkbox>
          <n-checkbox v-model:checked="setting.favoriteCreateTimeFirst">
            Sort by favorite time first
          </n-checkbox>
        </n-flex>
      </n-list-item>

      <n-list-item>
        <n-flex vertical>
          <b>Workspace</b>
          <n-checkbox v-model:checked="setting.autoTopJobWhenAddTask">
            Automatically pin to top when adding tasks in workspace
          </n-checkbox>
        </n-flex>
      </n-list-item>

      <n-list-item>
        <n-flex vertical>
          <b>List Pagination Method</b>
          <c-radio-group
            v-model:value="setting.paginationMode"
            :options="Setting.paginationModeOptions"
            size="small"
          />
        </n-flex>
      </n-list-item>

      <n-list-item>
        <n-flex vertical>
          <b>Displayed Translation Buttons</b>
          <translator-check
            v-model:value="setting.enabledTranslator"
            size="small"
          />
        </n-flex>
      </n-list-item>

      <n-list-item>
        <n-flex vertical>
          <b>Workspace Sound Reminder</b>
          <n-flex :wrap="false" :size="0">
            <n-checkbox v-model:checked="setting.workspaceSound">
              All tasks completed
            </n-checkbox>

            [
            <c-button
              label="Click to play"
              text
              type="primary"
              @action="playSound(SoundAllTaskCompleted)"
            />
            ]
          </n-flex>
        </n-flex>
      </n-list-item>

      <n-list-item>
        <n-flex vertical align="start">
          <b>Clear Search History</b>
          <n-flex>
            <c-button
              label="Clear web search history"
              size="small"
              @action="clearWebSearchHistory"
            />
            <c-button
              label="Clear Wenku search history"
              size="small"
              @action="clearWenkuSearchHistory"
            />
          </n-flex>
        </n-flex>
      </n-list-item>

      <n-list-item>
        <n-flex vertical>
          <b>Language</b>
          Simplified and Traditional Chinese conversion currently only covers web chapter content.
          <c-radio-group
            v-model:value="setting.locale"
            :options="Setting.localeOptions"
            size="small"
          />
          <n-checkbox v-model:checked="setting.searchLocaleAware">
            Support Traditional Chinese search (unstable)
          </n-checkbox>
        </n-flex>
      </n-list-item>
    </n-list>
  </div>
</template>
