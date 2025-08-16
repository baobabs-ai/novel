<script lang="ts" setup>
import { ParsedFile } from '@/util/file';
import { DeleteOutlineOutlined } from '@vicons/material';

import { Locator } from '@/data';
import { Translator, TranslatorConfig } from '@/domain/translate';
import { Glossary } from '@/model/Glossary';

const props = defineProps<{
  files: ParsedFile[];
}>();

const message = useMessage();
const sakuraWorkspace = Locator.sakuraWorkspaceRepository().ref;

const countKatakana = (content: string) => {
  const regexp = /[\u30A0-\u30FF]{2,}/g;
  const matches = content.matchAll(regexp);
  const katakanaCounter = new Map<string, number>();
  for (const match of matches) {
    const w = match[0];
    katakanaCounter.set(w, (katakanaCounter.get(w) || 0) + 1);
  }
  const sortedKatakanaCounter = new Map(
    [...katakanaCounter].sort(([_w1, c1], [_w2, c2]) => c2 - c1),
  );
  return sortedKatakanaCounter;
};

interface GlossaryInfo {
  content: string;
  katakanas: Map<string, number>;
}

const infos = ref(new Map<string, GlossaryInfo>());

watch(props, async ({ files }) => {
  const newInfos = new Map<string, GlossaryInfo>();
  for (const file of files) {
    let content = '';
    if (file.type === 'txt') {
      content = file.text;
    } else if (file.type === 'epub') {
      content = await file.getText();
    }
    const katakanas = countKatakana(content);
    newInfos.set(file.name, { content, katakanas });
  }
  infos.value = newInfos;
});

const katakanaThredhold = ref(10);

const katakanaMerged = computed(() => {
  const map = new Map<string, number>();
  infos.value.forEach(({ katakanas }) => {
    katakanas.forEach((value, key) => {
      map.set(key, (map.get(key) ?? 0) + value);
    });
  });
  return map;
});

const katakanaDeleted = ref<string[]>([]);
const undoDeleteKatakana = () => {
  katakanaDeleted.value.pop();
};
const lastDeletedHint = computed(() => {
  const last = katakanaDeleted.value[katakanaDeleted.value.length - 1];
  if (last === undefined) return undefined;
  return `${last} => ${katakanaTranslations.value[last]}`;
});

const katakanas = computed(() => {
  return new Map(
    [...katakanaMerged.value].filter(
      ([w, c]) =>
        c > katakanaThredhold.value && !katakanaDeleted.value.includes(w),
    ),
  );
});

const copyTranslationJson = async () => {
  const obj = Object.fromEntries(
    Array.from(katakanas.value).map(([key]) => [
      key,
      katakanaTranslations.value[key] ?? '',
    ]),
  );
  const jsonString = Glossary.toText(obj);
  navigator.clipboard.writeText(jsonString);
  message.info('The translation result has been copied to the clipboard');
};

const showSakuraSelectModal = ref(false);
const selectedSakuraWorkerId = ref(sakuraWorkspace.value.workers[0]?.id);

const katakanaTranslations = ref<{ [key: string]: string }>({});
const translateKatakanas = async (id: 'baidu' | 'youdao' | 'sakura') => {
  const jpWords = [...katakanas.value.keys()];
  let config: TranslatorConfig;
  if (id === 'sakura') {
    const worker = sakuraWorkspace.value.workers.find(
      (it) => it.id === selectedSakuraWorkerId.value,
    );
    if (worker === undefined) {
      message.error('Sakura translator not selected');
      return;
    }
    config = {
      id,
      endpoint: worker.endpoint,
      segLength: worker.segLength,
      prevSegLength: worker.prevSegLength,
    };
  } else {
    config = { id };
  }
  try {
    const translator = await Translator.create(config, false);
    const zhWords = await translator.translate(jpWords, {});

    const jpToZh: { [key: string]: string } = {};
    jpWords.forEach((jpWord, index) => {
      jpToZh[jpWord] = zhWords[index];
    });
    katakanaTranslations.value = jpToZh;
  } catch (e: unknown) {
    message.error(`Translator error: ${e}`);
  }
};
</script>

<template>
  <n-flex vertical size="large">
    <bulletin>
      <n-p>The glossary-making tool is under development. The current plan is divided into two steps: recognition and translation.</n-p>
      <n-ul>
        <n-li>Recognition stage: Identify possible terminology based on the frequency of katakana vocabulary.</n-li>
        <n-li>Translation stage: Directly translate Japanese vocabulary.</n-li>
      </n-ul>
      <n-p><b>Note that this is an auxiliary production, not a fully automatic generation. Be sure to check the results before use.</b></n-p>
    </bulletin>

    <c-action-wrapper title="Minimum number of times">
      <n-input-number
        v-model:value="katakanaThredhold"
        clearable
        size="small"
        style="width: 16em"
        min="0"
      />
    </c-action-wrapper>

    <c-action-wrapper title="Operation">
      <n-flex vertical>
        <n-button-group size="small">
          <c-button
            label="Copy glossary"
            :round="false"
            @action="copyTranslationJson()"
          />
          <c-button
            label="Baidu Translate"
            :round="false"
            @action="translateKatakanas('baidu')"
          />
          <c-button
            label="Youdao Translate"
            :round="false"
            @action="translateKatakanas('youdao')"
          />
        </n-button-group>

        <n-button-group size="small">
          <c-button
            :label="`Sakura Translate-${selectedSakuraWorkerId ?? 'Not selected'}`"
            :round="false"
            @action="translateKatakanas('sakura')"
          />
          <c-button
            label="Select translator"
            :round="false"
            @action="showSakuraSelectModal = true"
          />
        </n-button-group>

        <n-flex align="center" :wrap="false">
          <c-button
            :disabled="katakanaDeleted.length === 0"
            label="Undo delete"
            :round="false"
            size="small"
            @action="undoDeleteKatakana"
          />
          <n-text
            v-if="katakanaDeleted.length > 0"
            depth="3"
            style="font-size: 12px"
          >
            {{ lastDeletedHint }}
          </n-text>
        </n-flex>
      </n-flex>
    </c-action-wrapper>

    <n-scrollbar
      v-if="katakanas.size !== 0"
      trigger="none"
      style="
        max-height: 60vh;
        max-width: 600px;
        margin-top: 16px;
        margin-left: 32px;
      "
    >
      <n-table striped size="small" style="font-size: 12px">
        <tr v-for="[word, number] in katakanas" :key="word">
          <td>
            <c-icon-button
              tooltip="Remove"
              :icon="DeleteOutlineOutlined"
              text
              size="small"
              type="error"
              @action="katakanaDeleted.push(word)"
            />
          </td>
          <td nowrap="nowrap">{{ number }}</td>
          <td style="min-width: 100px">{{ word }}</td>
          <td nowrap="nowrap">=></td>
          <td style="padding-right: 16px">
            <n-input
              v-model:value="katakanaTranslations[word]"
              size="tiny"
              placeholder="Please enter Chinese translation"
              :theme-overrides="{
                border: '0',
                color: 'transprent',
              }"
            />
          </td>
        </tr>
      </n-table>
    </n-scrollbar>
  </n-flex>

  <c-modal title="Select Sakura translator" v-model:show="showSakuraSelectModal">
    <n-radio-group v-model:value="selectedSakuraWorkerId">
      <n-flex vertical>
        <n-radio
          v-for="worker of sakuraWorkspace.workers"
          :key="worker.id"
          :value="worker.id"
        >
          {{ worker.id }}
          <n-text depth="3">
            {{ worker.endpoint }}
          </n-text>
        </n-radio>
      </n-flex>
    </n-radio-group>
  </c-modal>
</template>
