<script lang="ts" setup>
import { ScrollbarInst } from 'naive-ui';

const props = defineProps<{
  title: string;
  running: boolean;
}>();

const show = ref(false);
watch(
  () => props.running,
  (running) => {
    if (running) {
      show.value = true;
    }
  },
);

type LogLine = { message: string; detail?: string[] };
const logs = ref<LogLine[]>([]);

const clearLog = () => {
  logs.value = [];
};
const pushLog = (line: LogLine) => {
  logs.value.push(line);
};

const logRef = ref<ScrollbarInst>();
const enableAutoScroll = ref(true);
const expandLog = ref(false);

watch(
  logs,
  () => {
    if (enableAutoScroll.value) {
      nextTick(() => {
        logRef.value?.scrollBy({ top: 100 });
      });
    }
  },
  { deep: true },
);

const showLogDetailModal = ref(false);
const selectedLogDetail = ref([] as string[]);
const selectedLogMessage = ref('');
const showDetail = (message: string, detail: string[]) => {
  selectedLogMessage.value = message.trim();
  selectedLogDetail.value = detail;
  showLogDetailModal.value = true;
};

defineExpose({
  clearLog,
  pushLog,
  hide: () => {
    show.value = false;
  },
});
</script>

<template>
  <n-card
    v-show="show"
    :title="`${title} [${running ? 'Running' : 'Finished'}]`"
    embedded
    :bordered="false"
  >
    <template #header-extra>
      <n-flex align="center">
        <c-button
          :label="enableAutoScroll ? 'Pause scrolling' : 'Auto scroll'"
          size="small"
          @action="enableAutoScroll = !enableAutoScroll"
        />
        <c-button
          :label="expandLog ? 'Collapse log' : 'Expand log'"
          size="small"
          @action="expandLog = !expandLog"
        />
      </n-flex>
    </template>
    <n-flex :wrap="false">
      <n-scrollbar
        ref="logRef"
        style="flex: auto; white-space: pre-wrap"
        :style="{ height: expandLog ? '540px' : '180px' }"
      >
        <div v-for="log of logs" :key="log.message">
          {{ log.message }}
          <span v-if="log.detail" @click="showDetail(log.message, log.detail!)">
            [Detail]
          </span>
        </div>
      </n-scrollbar>
      <slot />
    </n-flex>

    <c-modal
      v-model:show="showLogDetailModal"
      :title="`Log detail - ${selectedLogMessage}`"
    >
      <n-p
        v-for="line of selectedLogDetail"
        :key="line"
        style="white-space: pre-wrap"
      >
        {{ line }}
      </n-p>
    </c-modal>
  </n-card>
</template>
