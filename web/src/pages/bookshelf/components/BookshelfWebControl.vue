<script lang="ts" setup>
import { useKeyModifier } from '@vueuse/core';

import { Locator } from '@/data';
import { TranslateTaskDescriptor } from '@/model/Translator';
import { WebNovelOutlineDto } from '@/model/WebNovel';

const props = defineProps<{
  selectedNovels: WebNovelOutlineDto[];
  favoredId: string;
}>();
defineEmits<{
  selectAll: [];
  invertSelection: [];
}>();

const message = useMessage();

const { setting } = Locator.settingRepository();
const favoredRepository = Locator.favoredRepository();
const { favoreds } = favoredRepository;

// 删除小说
const showDeleteModal = ref(false);

const openDeleteModal = () => {
  const novels = props.selectedNovels;
  if (novels.length === 0) {
    message.info('No novel selected');
    return;
  }
  showDeleteModal.value = true;
};

const deleteSelected = async () => {
  const novels = props.selectedNovels;
  let failed = 0;
  for (const { providerId, novelId } of novels) {
    try {
      await favoredRepository.unfavoriteNovel(props.favoredId, {
        type: 'web',
        providerId,
        novelId,
      });
    } catch (e) {
      failed += 1;
    }
  }
  const success = novels.length - failed;

  message.info(`${success} novels were deleted, ${failed} failed`);
};

// 移动小说
const targetFavoredId = ref(props.favoredId);

const moveToFavored = async () => {
  const novels = props.selectedNovels;
  if (novels.length === 0) {
    message.info('No novel selected');
    return;
  }

  if (targetFavoredId.value === props.favoredId) {
    message.info('No need to move');
    return;
  }

  let failed = 0;
  for (const { providerId, novelId } of novels) {
    try {
      await favoredRepository.unfavoriteNovel(targetFavoredId.value, {
        type: 'web',

        providerId,
        novelId,
      });
    } catch (e) {
      failed += 1;
    }
  }
  const success = novels.length - failed;

  message.info(`${success} novels have been moved, ${failed} failed`);
  window.location.reload();
};

// 生成翻译任务
const translateLevel = ref<'normal' | 'expire' | 'all'>('normal');
const forceMetadata = ref(false);
const first5 = ref(false);
const reverseOrder = ref(false);
const shouldTopJob = useKeyModifier('Control');

const queueJobs = (type: 'gpt' | 'sakura') => {
  let novels = props.selectedNovels;
  if (novels.length === 0) {
    message.info('No novel selected');
    return;
  }

  const workspace =
    type === 'gpt'
      ? Locator.gptWorkspaceRepository()
      : Locator.sakuraWorkspaceRepository();

  if (reverseOrder.value) {
    novels = novels.slice().reverse();
  }

  let failed = 0;
  novels.forEach(({ providerId, novelId, titleJp }) => {
    const task = TranslateTaskDescriptor.web(providerId, novelId, {
      level: translateLevel.value,
      forceMetadata: forceMetadata.value,
      startIndex: 0,
      endIndex: first5.value ? 5 : 65535,
    });
    const job = {
      task,
      description: titleJp,
      createAt: Date.now(),
    };
    const success = workspace.addJob(job);
    if (success && shouldTopJob.value) {
      workspace.topJob(job);
    }
    if (!success) {
      failed += 1;
    }
  });
  const success = novels.length - failed;
  message.info(`${success} novels have been queued, ${failed} failed`);
};
</script>

<template>
  <n-list bordered>
    <n-list-item>
      <n-flex vertical>
        <n-flex align="baseline">
          <n-button-group size="small">
            <c-button
              label="Select all"
              :round="false"
              @action="$emit('selectAll')"
            />
            <c-button
              label="Invert selection"
              :round="false"
              @action="$emit('invertSelection')"
            />
          </n-button-group>

          <c-button
            label="Delete"
            secondary
            :round="false"
            size="small"
            type="error"
            @click="openDeleteModal"
          />
          <c-modal
            :title="`Confirm deletion ${
              selectedNovels.length === 1
                ? selectedNovels[0].titleZh ?? selectedNovels[0].titleJp
                : `${selectedNovels.length} novels`
            }？`"
            v-model:show="showDeleteModal"
          >
            <template #action>
              <c-button label="Confirm" type="primary" @action="deleteSelected" />
            </template>
          </c-modal>
        </n-flex>

        <n-text depth="3"> Selected {{ selectedNovels.length }} novels </n-text>
      </n-flex>
    </n-list-item>

    <n-list-item v-if="favoreds.web.length > 1">
      <n-p>The move novel function is temporarily closed</n-p>
      <n-flex v-if="false" vertical>
        <b>Move novels (low-spec version, very slow, wait until the move is complete)</b>

        <n-radio-group v-model:value="targetFavoredId">
          <n-flex align="center">
            <c-button
              label="Move"
              size="small"
              :round="false"
              @action="moveToFavored"
            />

            <n-radio
              v-for="favored in favoreds.web"
              :key="favored.id"
              :value="favored.id"
            >
              {{ favored.title }}
            </n-radio>
          </n-flex>
        </n-radio-group>
      </n-flex>
    </n-list-item>

    <n-list-item
      v-if="
        setting.enabledTranslator.includes('gpt') ||
        setting.enabledTranslator.includes('sakura')
      "
    >
      <n-flex vertical>
        <b>Generate translation task</b>

        <n-flex size="small">
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-flex :size="0" :wrap="false">
                <tag-button
                  label="Normal"
                  :checked="translateLevel === 'normal'"
                  @update:checked="translateLevel = 'normal'"
                />
                <tag-button
                  label="Expired"
                  :checked="translateLevel === 'expire'"
                  @update:checked="translateLevel = 'expire'"
                />
                <tag-button
                  label="Re-translate"
                  type="warning"
                  :checked="translateLevel === 'all'"
                  @update:checked="translateLevel = 'all'"
                />
              </n-flex>
            </template>
            Normal: Only translate untranslated chapters<br />
            Expired: Translate chapters with expired glossaries<br />
            Re-translate: Re-translate all chapters<br />
          </n-tooltip>

          <tag-button label="Re-translate table of contents" v-model:checked="forceMetadata" />
          <tag-button label="First 5 chapters" v-model:checked="first5" />
          <tag-button label="Add in reverse order" v-model:checked="reverseOrder" />

          <n-text
            v-if="translateLevel === 'all'"
            type="warning"
            style="font-size: 12px; flex-basis: 100%"
          >
            <b> * Please make sure you know what you are doing, do not use dangerous functions casually </b>
          </n-text>
        </n-flex>

        <n-button-group size="small">
          <c-button
            v-if="setting.enabledTranslator.includes('gpt')"
            label="Queue GPT"
            :round="false"
            @action="queueJobs('gpt')"
          />
          <c-button
            v-if="setting.enabledTranslator.includes('sakura')"
            label="Queue Sakura"
            :round="false"
            @action="queueJobs('sakura')"
          />
        </n-button-group>
      </n-flex>
    </n-list-item>
  </n-list>
</template>
