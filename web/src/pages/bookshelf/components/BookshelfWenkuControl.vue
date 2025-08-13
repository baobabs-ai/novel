<script lang="ts" setup>
import { Locator } from '@/data';
import { WenkuNovelOutlineDto } from '@/model/WenkuNovel';

const props = defineProps<{
  selectedNovels: WenkuNovelOutlineDto[];
  favoredId: string;
}>();
defineEmits<{
  selectAll: [];
  invertSelection: [];
}>();

const message = useMessage();

const favoredRepository = Locator.favoredRepository();
const { favoreds } = favoredRepository;

// Delete novel
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
  for (const { id } of novels) {
    try {
      await favoredRepository.unfavoriteNovel(props.favoredId, {
        type: 'wenku',
        novelId: id,
      });
    } catch (e) {
      failed += 1;
    }
  }
  const success = novels.length - failed;

  message.info(`${success} novels deleted, ${failed} failed`);
};

// Move novel
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
  for (const { id } of novels) {
    try {
      await favoredRepository.favoriteNovel(props.favoredId, {
        type: 'wenku',
        novelId: id,
      });
    } catch (e) {
      failed += 1;
    }
  }
  const success = novels.length - failed;

  message.info(`${success} novels moved, ${failed} failed`);
  window.location.reload();
};
</script>

<template>
  <n-list bordered>
    <n-list-item>
      <n-flex vertical>
        <n-flex align="baseline">
          <n-button-group size="small">
            <c-button
              label="Select All"
              :round="false"
              @action="$emit('selectAll')"
            />
            <c-button
              label="Invert Selection"
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
            :title="`Are you sure you want to delete ${
              selectedNovels.length === 1
                ? selectedNovels[0].titleEn ?? selectedNovels[0].title
                : `${selectedNovels.length} novels`
            }?`"
            v-model:show="showDeleteModal"
          >
            <template #action>
              <c-button label="Confirm" type="primary" @action="deleteSelected" />
            </template>
          </c-modal>
        </n-flex>
        <n-text depth="3"> {{ selectedNovels.length }} novels selected </n-text>
      </n-flex>
    </n-list-item>

    <n-list-item v-if="favoreds.wenku.length > 1">
      <n-p>Move novel function is temporarily closed</n-p>
      <n-flex v-if="false" vertical>
        <b>Move novel (low-spec version, very slow, wait until it shows completion)</b>

        <n-radio-group v-model:value="targetFavoredId">
          <n-flex align="center">
            <c-button
              label="Move"
              size="small"
              :round="false"
              @action="moveToFavored"
            />

            <n-radio
              v-for="favored in favoreds.wenku"
              :key="favored.id"
              :value="favored.id"
            >
              {{ favored.title }}
            </n-radio>
          </n-flex>
        </n-radio-group>
      </n-flex>
    </n-list-item>
  </n-list>
</template>
