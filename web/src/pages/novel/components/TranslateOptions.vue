<script lang="ts" setup>
import { InfoOutlined } from '@vicons/material';

import { Locator } from '@/data';
import { Setting } from '@/data/setting/Setting';
import { GenericNovelId } from '@/model/Common';
import { Glossary } from '@/model/Glossary';
import { TranslateTaskParams } from '@/model/Translator';
import { useIsWideScreen } from '@/pages/util';

const probs = defineProps<{
  gnid: GenericNovelId;
  glossary: Glossary;
}>();
const isWideScreen = useIsWideScreen(600);

const { setting } = Locator.settingRepository();

// 翻译设置
const translateLevel = ref<'normal' | 'expire' | 'all' | 'sync'>(
  probs.gnid.type === 'local' ? 'expire' : 'normal',
);
const forceMetadata = ref(false);
const startIndex = ref<number | null>(0);
const endIndex = ref<number | null>(65536);
const taskNumber = ref<number | null>(1);

defineExpose({
  getTranslateTaskParams: (): TranslateTaskParams => ({
    level: translateLevel.value,
    forceMetadata: forceMetadata.value,
    startIndex: startIndex.value ?? 0,
    endIndex: endIndex.value ?? 65536,
  }),
  getTaskNumber: () => taskNumber.value ?? 1,
});

const showDownloadModal = ref(false);
</script>

<template>
  <n-flex vertical>
    <c-action-wrapper title="Options">
      <n-flex size="small">
        <n-tooltip trigger="hover" style="max-width: 200px">
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
              <tag-button
                v-if="gnid.type === 'web'"
                label="Sync with source"
                type="warning"
                :checked="translateLevel === 'sync'"
                @update:checked="translateLevel = 'sync'"
              />
            </n-flex>
          </template>
          Normal: Only translate untranslated chapters<br />
          Expired: Translate chapters with expired glossaries<br />
          Re-translate: Re-translate all chapters<br />
          <template v-if="gnid.type === 'web'">
            Sync from source station: used when the original author modifies the original text, which may cause inconsistencies. It may clear the existing translation, use with caution!
          </template>
        </n-tooltip>

        <tag-button
          v-if="gnid.type === 'web'"
          label="Re-translate table of contents"
          v-model:checked="forceMetadata"
        />

        <n-text
          v-if="translateLevel === 'all' || translateLevel === 'sync'"
          type="warning"
          style="font-size: 12px; flex-basis: 100%"
        >
          <b> * Please make sure you know what you are doing, do not use dangerous functions casually </b>
        </n-text>
      </n-flex>
    </c-action-wrapper>

    <c-action-wrapper
      v-if="gnid.type === 'web' || gnid.type === 'local'"
      title="Range"
    >
      <n-flex style="text-align: center">
        <div>
          <n-input-group>
            <n-input-group-label size="small">From</n-input-group-label>
            <n-input-number
              size="small"
              v-model:value="startIndex"
              :show-button="false"
              button-placement="both"
              :min="0"
              style="width: 60px"
            />
            <n-input-group-label size="small">To</n-input-group-label>
            <n-input-number
              size="small"
              v-model:value="endIndex"
              :show-button="false"
              :min="0"
              style="width: 60px"
            />
          </n-input-group>
        </div>
        <div>
          <n-input-group>
            <n-input-group-label size="small">Equally divided</n-input-group-label>
            <n-input-number
              size="small"
              v-model:value="taskNumber"
              :show-button="false"
              :min="1"
              :max="gnid.type === 'local' ? 65536 : 10"
              style="width: 40px"
            />
            <n-input-group-label size="small">tasks</n-input-group-label>
          </n-input-group>
        </div>

        <n-tooltip trigger="hover" placement="top" style="max-width: 200px">
          <template #trigger>
            <n-button text>
              <n-icon depth="4" :component="InfoOutlined" />
            </n-button>
          </template>
          For the chapter number, see the number in square brackets in the table of contents below. "From 0 to 10" means from chapter 0 to chapter 9, not including chapter 10. The equal task is only effective for queuing, and the maximum is 10.
        </n-tooltip>
      </n-flex>
    </c-action-wrapper>

    <c-action-wrapper v-if="gnid.type !== 'local'" title="Operation">
      <n-button-group size="small">
        <c-button
          label="Download settings"
          :round="false"
          @action="showDownloadModal = true"
        />
        <glossary-button :gnid="gnid" :value="glossary" :round="false" />
      </n-button-group>
    </c-action-wrapper>

    <c-modal title="Download settings" v-model:show="showDownloadModal">
      <n-flex vertical size="large">
        <c-action-wrapper title="Language">
          <c-radio-group
            v-model:value="setting.downloadFormat.mode"
            :options="Setting.downloadModeOptions"
          />
        </c-action-wrapper>

        <c-action-wrapper title="Translate">
          <n-flex>
            <c-radio-group
              v-model:value="setting.downloadFormat.translationsMode"
              :options="Setting.downloadTranslationModeOptions"
            />
            <translator-check
              v-model:value="setting.downloadFormat.translations"
              show-order
              :two-line="!isWideScreen"
            />
          </n-flex>
        </c-action-wrapper>

        <c-action-wrapper v-if="gnid.type === 'web'" title="File">
          <c-radio-group
            v-model:value="setting.downloadFormat.type"
            :options="Setting.downloadTypeOptions"
          />
        </c-action-wrapper>

        <c-action-wrapper
          v-if="gnid.type === 'web'"
          title="English Filename"
          align="center"
        >
          <n-switch
            size="small"
            :value="setting.downloadFilenameType === 'en'"
            @update-value="
              (it: boolean) => (setting.downloadFilenameType = it ? 'en' : 'jp')
            "
          />
        </c-action-wrapper>

        <n-text depth="3" style="font-size: 12px">
          # Some EPUB readers cannot correctly display light-colored fonts in Japanese paragraphs
        </n-text>
      </n-flex>
    </c-modal>
  </n-flex>
</template>
