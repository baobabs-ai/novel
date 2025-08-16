<script lang="ts" setup>
import {
  DeleteOutlineOutlined,
  KeyboardDoubleArrowDownOutlined,
  KeyboardDoubleArrowUpOutlined,
  UploadOutlined,
} from '@vicons/material';
import { FormInst, FormItemRule, FormRules } from 'naive-ui';
import { VueDraggable } from 'vue-draggable-plus';

import { Locator } from '@/data';
import { prettyCover, smartImport } from '@/domain/smart-import';
import coverPlaceholder from '@/image/cover_placeholder.png';
import {
  WenkuNovelOutlineDto,
  WenkuVolumeDto,
  presetKeywordsNonR18,
  presetKeywordsR18,
} from '@/model/WenkuNovel';
import { RegexUtil, delay } from '@/util';
import { runCatching } from '@/util/result';

import { doAction, useIsWideScreen } from '@/pages/util';
import { useWenkuNovelStore } from './WenkuNovelStore';

const { novelId } = defineProps<{
  novelId: string | undefined;
}>();

const store = novelId !== undefined ? useWenkuNovelStore(novelId) : undefined;

const router = useRouter();
const isWideScreen = useIsWideScreen();
const message = useMessage();

const { whoami } = Locator.authRepository();

const allowSubmit = ref(novelId === undefined);
const formRef = ref<FormInst>();
const formValue = ref({
  title: '',
  titleEn: '',
  cover: '',
  authors: <string[]>[],
  artists: <string[]>[],
  level: 'For All Ages',
  keywords: <string[]>[],
  introduction: '',
  volumes: <WenkuVolumeDto[]>[],
});
const formRules: FormRules = {
  title: [
    {
      validator: (_rule: FormItemRule, value: string) =>
        value.trim().length > 0,
      message: 'Title cannot be empty',
      trigger: 'input',
    },
    {
      validator: (_rule: FormItemRule, value: string) => value.length <= 80,
      message: 'Title length cannot exceed 80 characters',
      trigger: 'input',
    },
  ],
  titleEn: [
    {
      validator: (_rule: FormItemRule, value: string) =>
        value.trim().length > 0,
      message: 'Title cannot be empty',
      trigger: 'input',
    },
    {
      validator: (_rule: FormItemRule, value: string) => value.length <= 80,
      message: 'Title length cannot exceed 80 characters',
      trigger: 'input',
    },
    {
      validator: (_rule: FormItemRule, value: string) =>
        !RegexUtil.hasKanaChars(value),
      message: 'Do not use Japanese as the English title, if there is no recognized title, you can try to translate it yourself',
      trigger: 'input',
    },
  ],
  level: [
    {
      validator: (_rule: FormItemRule, value: string) =>
        value !== 'For Adults' || whoami.value.allowNsfw,
      message: 'You are too young to create an adult page',
      trigger: 'input',
    },
  ],
  introduction: [
    {
      validator: (_rule: FormItemRule, value: string) => value.length <= 500,
      message: 'Introduction length cannot exceed 500 characters',
      trigger: 'input',
    },
  ],
};

const amazonUrl = ref('');

import { AmazonNovel, WenkuNovelDto } from '@/model/WenkuNovel';
import { Result } from '@/util/result';

store?.loadNovel()?.then((result: Result<WenkuNovelDto> | undefined) => {
  if (result?.ok) {
    const {
      title,
      titleEn,
      cover,
      authors,
      artists,
      level,
      keywords,
      introduction,
    } = result.value;
    formValue.value = {
      title,
      titleEn,
      cover: prettyCover(cover ?? ''),
      authors,
      artists,
      level,
      keywords,
      introduction,
      volumes: result.value.volumes.map((it: WenkuVolumeDto) => {
        it.cover = prettyCover(it.cover);
        return it;
      }),
    };
    amazonUrl.value = result.value.title.replace(/[?？。!！]$/, '');
    allowSubmit.value = true;
  } else {
    message.error('Failed to load');
  }
});

const submit = async () => {
  if (!allowSubmit.value) {
    message.warning('Article not loaded, cannot submit');
    return;
  }

  try {
    await formRef.value?.validate();
  } catch (e) {
    return;
  }

  const allPresetKeywords = presetKeywords.value.groups.flatMap(
    (it: { presetKeywords: string[] }) => it.presetKeywords,
  );

  const body = {
    title: formValue.value.title,
    titleEn: formValue.value.titleEn,
    cover: formValue.value.cover,
    authors: formValue.value.authors,
    artists: formValue.value.artists,
    level: formValue.value.level,
    introduction: formValue.value.introduction,
    keywords: formValue.value.keywords.filter((it: string) =>
      allPresetKeywords.includes(it),
    ),
    volumes: formValue.value.volumes,
  };

  if (store === undefined) {
    await doAction(
      Locator.wenkuNovelRepository.createNovel(body).then((id) => {
        router.push({ path: `/wenku/${id}` });
      }),
      'New Wenku',
      message,
    );
  } else {
    await doAction(
      store.updateNovel(body).then(() => {
        router.push({ path: `/wenku/${novelId}` });
      }),
      'Edit Wenku',
      message,
    );
  }
};

const populateNovelFromAmazon = async (
  urlOrQuery: string,
  forcePopulateVolumes: boolean,
) => {
  const msgReactive = message.create('', {
    type: 'loading',
    duration: 0,
  });

  await smartImport(
    urlOrQuery.trim(),
    formValue.value.volumes,
    forcePopulateVolumes,
    {
      log: (message: string) => {
        msgReactive.content = message;
      },
      populateNovel: (novel: AmazonNovel) => {
        formValue.value = {
          title: formValue.value.title ? formValue.value.title : novel.title,
          titleEn: formValue.value.titleEn
            ? formValue.value.titleEn
            : novel.titleEn ?? '',
          cover: novel.volumes[0]?.cover,
          authors:
            formValue.value.authors.length > 0
              ? formValue.value.authors
              : novel.authors,
          artists:
            formValue.value.artists.length > 0
              ? formValue.value.artists
              : novel.artists,
          level: novel.r18 ? 'For Adults' : 'For All Ages',
          keywords: formValue.value.keywords,
          introduction: formValue.value.introduction
            ? formValue.value.introduction
            : novel.introduction,
          volumes: novel.volumes,
        };
      },
      populateVolume: (volume: WenkuVolumeDto) => {
        const index = formValue.value.volumes.findIndex(
          (it: WenkuVolumeDto) => it.asin === volume.asin,
        );
        if (index >= 0) {
          formValue.value.volumes[index] = volume;
        }
      },
    },
  );

  formValue.value.cover = formValue.value.volumes[0]?.cover;
  msgReactive.content = 'Smart import complete';
  msgReactive.type = 'info';
  delay(3000).then(() => msgReactive.destroy());
};

const submitCurrentStep = ref(1);
const title = computed(() => formValue.value.title);
const similarNovels = ref<WenkuNovelOutlineDto[] | null>(null);

watch(title, () => {
  similarNovels.value = null;
  submitCurrentStep.value = 1;
});
const findSimilarNovels = async () => {
  const query = title.value.split(
    /[^\u3040-\u309f\u30a0-\u30ff\u4e00-\u9faf\u3400-\u4dbf]/,
    2,
  )[0];
  const result = await runCatching(
    Locator.wenkuNovelRepository.listNovel({
      page: 0,
      pageSize: 6,
      query,
      level: 0,
    }),
  );
  if (result.ok) {
    similarNovels.value = result.value.items;
  } else {
    message.error('Failed to search for similar novels:' + result.error.message);
  }
};
const moveToPrevStep = () => {
  if (submitCurrentStep.value > 1) {
    submitCurrentStep.value -= 1;
  }
};
const moveToNextStep = () => {
  if (submitCurrentStep.value < 3) {
    submitCurrentStep.value += 1;
  }
};
const topVolume = (asin: string) => {
  formValue.value.volumes.sort((a: WenkuVolumeDto, b: WenkuVolumeDto) => {
    return a.asin == asin ? -1 : b.asin == asin ? 1 : 0;
  });
};
const bottomVolume = (asin: string) => {
  formValue.value.volumes.sort((a: WenkuVolumeDto, b: WenkuVolumeDto) => {
    return a.asin == asin ? 1 : b.asin == asin ? -1 : 0;
  });
};
const deleteVolume = (asin: string) => {
  formValue.value.volumes = formValue.value.volumes.filter(
    (it: WenkuVolumeDto) => it.asin !== asin,
  );
};

const markAsDuplicate = () => {
  formValue.value = {
    title: 'Duplicate, to be deleted',
    titleEn: 'Duplicate, to be deleted',
    cover: '',
    authors: [],
    artists: [],
    level: formValue.value.level,
    keywords: [],
    introduction: '',
    volumes: [],
  };
};

const presetKeywords = computed(() => {
  if (formValue.value.level === 'For All Ages') {
    return presetKeywordsNonR18;
  } else {
    return presetKeywordsR18;
  }
});
const showKeywordsModal = ref(false);

const togglePresetKeyword = (checked: boolean, keyword: string) => {
  if (checked) {
    formValue.value.keywords.push(keyword);
  } else {
    formValue.value.keywords = formValue.value.keywords.filter(
      (it: string) => it !== keyword,
    );
  }
};

const levelOptions = [
  { label: 'For All Ages', value: 'For All Ages' },
  { label: 'For Adults', value: 'For Adults' },
  { label: 'Serious', value: 'Serious' },
];
</script>

<template>
  <div class="layout-content">
    <n-h1>{{ novelId === undefined ? 'New' : 'Edit' }} Wenku Novel</n-h1>

    <n-card embedded :bordered="false" style="margin-bottom: 20px">
      <n-text type="error">
        <b>Notes for creating Wenku novels:</b>
      </n-text>
      <n-ul>
        <n-li>
          Please install the machine translation extension to enable the smart import function. In addition, the automatic machine translation of the introduction requires you to be able to use Youdao machine translation.
        </n-li>
        <n-li>
          Wenku novels only allow Japanese novels that have been published in paperback. In principle, they should be available on Amazon. Do not import series novels separately.
        </n-li>
        <n-li>
          Enter the Amazon series/single book link in the import bar to import directly, or enter the Japanese main title of the novel to search for import.
        </n-li>
        <n-li>
          To import R18 books, you need to be registered on the machine translation site for one month, use a Japanese IP, and have clicked "I am over 18" on Amazon.
        </n-li>
      </n-ul>
    </n-card>

    <n-flex style="margin-bottom: 48px; width: 100%">
      <div v-if="isWideScreen">
        <n-image
          width="160"
          :src="formValue.cover ? formValue.cover : coverPlaceholder"
          alt="cover"
        />
      </div>

      <n-flex size="large" vertical style="flex: auto">
        <n-input-group>
          <n-input
            v-model:value="amazonUrl"
            :placeholder="formValue.title"
            :input-props="{ spellcheck: false }"
          />
          <c-button
            label="Import"
            :round="false"
            type="primary"
            @action="populateNovelFromAmazon(amazonUrl, false)"
          />
        </n-input-group>
        <n-flex>
          <c-button
            label="Search on Amazon"
            secondary
            tag="a"
            :href="`https://www.amazon.co.jp/s?k=${encodeURIComponent(
              formValue.title,
            )}&i=stripbooks`"
            target="_blank"
          />
          <c-button
            secondary
            label="Refresh Volumes"
            @action="populateNovelFromAmazon('', true)"
          />
          <c-button
            v-if="whoami.isAdmin"
            type="error"
            secondary
            label="Mark as Duplicate"
            @action="markAsDuplicate"
          />
        </n-flex>
      </n-flex>
    </n-flex>

    <n-form
      ref="formRef"
      :model="formValue"
      :rules="formRules"
      :label-placement="isWideScreen ? 'left' : 'top'"
      label-width="auto"
    >
      <n-form-item-row path="title" label="Japanese Title">
        <n-input
          v-model:value="formValue.title"
          placeholder="Please enter the Japanese title"
          maxlength="80"
          show-count
          :input-props="{ spellcheck: false }"
        />
      </n-form-item-row>

      <n-form-item-row path="titleEn" label="English Title">
        <n-input
          v-model:value="formValue.titleEn"
          placeholder="Please enter the English title"
          maxlength="80"
          show-count
          :input-props="{ spellcheck: false }"
        />
      </n-form-item-row>

      <n-form-item-row path="cover" label="Cover Link">
        <n-input
          v-model:value="formValue.cover"
          placeholder="Please enter the cover link"
          :input-props="{ spellcheck: false }"
        />
      </n-form-item-row>

      <n-form-item-row path="authors" label="Authors">
        <n-dynamic-tags v-model:value="formValue.authors" />
      </n-form-item-row>

      <n-form-item-row path="artists" label="Artists">
        <n-dynamic-tags v-model:value="formValue.artists" />
      </n-form-item-row>

      <n-form-item-row path="level" label="Rating">
        <c-radio-group
          v-model:value="formValue.level"
          :options="levelOptions"
        />
      </n-form-item-row>

      <n-form-item-row path="content" label="Introduction">
        <n-input
          v-model:value="formValue.introduction"
          type="textarea"
          placeholder="Please enter the novel introduction"
          :autosize="{
            minRows: 8,
            maxRows: 24,
          }"
          maxlength="500"
          show-count
          :input-props="{ spellcheck: false }"
        />
      </n-form-item-row>

      <n-form-item-row label="Tags">
        <n-list bordered style="width: 100%">
          <n-list-item>
            <c-button
              v-if="presetKeywords.groups.length > 0"
              label="Must Read Before Use"
              @action="showKeywordsModal = true"
              text
              type="error"
            />
            <n-p v-else>Tags are not supported yet.</n-p>
          </n-list-item>
          <n-list-item
            v-for="group of presetKeywords.groups"
            :key="group.title"
          >
            <n-flex size="small">
              <n-tag :bordered="false" size="small">
                <b>{{ group.title }}</b>
              </n-tag>
              <n-tag
                v-for="keyword of group.presetKeywords"
                :key="keyword"
                size="small"
                checkable
                :checked="formValue.keywords.includes(keyword)"
                @update:checked="
                  (checked: boolean) => togglePresetKeyword(checked, keyword)
                "
              >
                {{ keyword }}
              </n-tag>
            </n-flex>
          </n-list-item>
        </n-list>
      </n-form-item-row>

      <n-form-item-row label="Volumes" v-if="formValue.volumes.length > 0">
        <n-list style="width: 100%; font-size: 12px">
          <vue-draggable
            v-model="formValue.volumes"
            :animation="150"
            handle=".drag-trigger"
          >
            <n-list-item v-for="volume of formValue.volumes" :key="volume.asin">
              <n-thing>
                <template #avatar>
                  <div>
                    <n-image
                      class="drag-trigger"
                      width="88"
                      :src="volume.cover"
                      :preview-src="volume.coverHires ?? volume.cover"
                      :alt="volume.asin"
                      lazy
                      style="border-radius: 2px; cursor: move"
                    />
                  </div>
                </template>

                <template #header>
                  <n-text style="font-size: 12px">
                    ASIN:
                    <n-a
                      :href="`https://www.amazon.co.jp/zh/dp/${volume.asin}`"
                    >
                      {{ volume.asin }}
                    </n-a>
                  </n-text>
                </template>

                <template #header-extra>
                  <n-flex :size="6" :wrap="false">
                    <c-icon-button
                      tooltip="Top"
                      :icon="KeyboardDoubleArrowUpOutlined"
                      @action="topVolume(volume.asin)"
                    />

                    <c-icon-button
                      tooltip="Bottom"
                      :icon="KeyboardDoubleArrowDownOutlined"
                      @action="bottomVolume(volume.asin)"
                    />

                    <c-icon-button
                      tooltip="Delete"
                      :icon="DeleteOutlineOutlined"
                      type="error"
                      @action="deleteVolume(volume.asin)"
                    />
                  </n-flex>
                </template>

                <template #description>
                  <n-flex align="center" :size="0" :wrap="false">
                    <n-text style="word-break: keep-all; font-size: 12px">
                      Title:
                    </n-text>
                    <n-input
                      v-model:value="volume.title"
                      placeholder="Title"
                      :input-props="{ spellcheck: false }"
                      size="small"
                      style="font-size: 12px"
                    />
                  </n-flex>
                  <n-text style="font-size: 12px">
                    Thumbnail: {{ volume.cover }}
                    <br />
                    High-res: {{ volume.coverHires }}
                    <br />
                    Publisher:
                    {{ volume.publisher ?? 'Unknown Publisher' }}
                    /
                    {{ volume.imprint ?? 'Unknown Imprint' }}
                    /
                    <n-time
                      v-if="volume.publishAt"
                      :time="volume.publishAt * 1000"
                      type="date"
                    />
                  </n-text>
                </template>
              </n-thing>
            </n-list-item>
          </vue-draggable>
        </n-list>
      </n-form-item-row>
    </n-form>

    <n-divider />

    <c-button
      v-if="novelId"
      label="Submit"
      :icon="UploadOutlined"
      require-login
      size="large"
      type="primary"
      class="float"
      @action="submit"
    />

    <n-steps
      v-else
      :current="submitCurrentStep"
      vertical
      style="margin-left: 8px"
    >
      <n-step title="Check if the novel already exists">
        <p>
          Before creating a Wenku page, please make sure that the novel page you want to create does not exist, and do not create it repeatedly. You can search for the chapter title through the search button below. Note that the automatic search may not always extract the keywords correctly. If the keywords are incorrect, please manually search for the Japanese title.
        </p>
        <p>
          Automatic search keywords:
          <b>
            {{
              title.split(
                /[^\u3040-\u309f\u30a0-\u30ff\u4e00-\u9faf\u3400-\u4dbf]/,
                2,
              )[0]
            }}
          </b>
        </p>
        <p v-if="similarNovels !== null">
          <template v-if="similarNovels.length === 0">No similar novels found</template>
          <n-grid v-else :x-gap="12" :y-gap="12" cols="3 600:6">
            <n-grid-item v-for="item in similarNovels" :key="item.id">
              <router-link :to="`/wenku/${item.id}`">
                <ImageCard
                  :src="item.cover"
                  :title="item.titleEn ? item.titleEn : item.title"
                />
              </router-link>
            </n-grid-item>
          </n-grid>
        </p>
        <n-button-group v-if="submitCurrentStep === 1">
          <c-button
            label="I'm sure the novel doesn't exist"
            type="warning"
            @click="moveToNextStep"
          />
          <c-button label="Automatically search for similar novels" @click="findSimilarNovels" />
        </n-button-group>
      </n-step>

      <n-step title="Check if the novel file can be uploaded">
        <p>
          Before creating a Wenku page, please confirm that you have a novel file that can be uploaded. Do not create a Wenku page and then look for resources, only to find that the resources cannot be used or cannot be found, leaving an empty Wenku page. It is especially forbidden to create an empty page to ask for books, which will result in a ban.
        </p>
        <p>PDF, or EPUB with only images, cannot be uploaded.</p>

        <n-button-group v-if="submitCurrentStep === 2">
          <c-button
            label="I'm sure I have a file that can be uploaded"
            type="warning"
            @click="moveToNextStep"
          />
          <c-button label="Previous" @click="moveToPrevStep" />
        </n-button-group>
      </n-step>

      <n-step title="Create Wenku Novel">
        <n-button-group v-if="submitCurrentStep === 3" style="margin-top: 16px">
          <c-button
            label="Submit"
            :icon="UploadOutlined"
            require-login
            type="primary"
            @action="submit"
          />
          <c-button label="Previous" @click="moveToPrevStep" />
        </n-button-group>
      </n-step>
    </n-steps>
  </div>

  <c-modal title="Instructions" v-model:show="showKeywordsModal">
    <n-p>
      The meaning of tags is to assist in searching. Whether a tag is appropriate depends on the proportion of related plots. The mere existence of related plots is not enough to justify adding a tag. In practice, you can consider whether users searching for that tag would want to see this book.
    </n-p>
    <n-p>
      Below are some specific explanations for the tags. Note that the same tag may have different meanings under "For All Ages" and "R18".
    </n-p>
    <n-divider />
    <n-p v-for="row of presetKeywords.explanations" :key="row.word">
      <b>{{ row.word }}</b>
      <br />
      {{ row.explanation }}
    </n-p>
  </c-modal>
</template>
