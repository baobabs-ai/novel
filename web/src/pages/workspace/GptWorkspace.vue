<script lang="ts" setup>
import {
  BookOutlined,
  DeleteOutlineOutlined,
  PlusOutlined,
} from '@vicons/material';
import { VueDraggable } from 'vue-draggable-plus';

import { Locator } from '@/data';
import { TranslateJob } from '@/model/Translator';

import { doAction } from '@/pages/util';

const message = useMessage();

const workspace = Locator.gptWorkspaceRepository();
const workspaceRef = workspace.ref;

const showCreateWorkerModal = ref(false);
const showLocalVolumeDrawer = ref(false);

type ProcessedJob = TranslateJob & {
  progress?: { finished: number; error: number; total: number };
};

const processedJobs = ref<Map<string, ProcessedJob>>(new Map());

const getNextJob = () => {
  const job = workspace.ref.value.jobs.find(
    (it) => !processedJobs.value.has(it.task),
  );
  if (job !== undefined) {
    processedJobs.value.set(job.task, job);
  }
  return job;
};

const deleteJob = (task: string) => {
  if (processedJobs.value.has(task)) {
    message.error('The task is occupied by the translator');
    return;
  }
  workspace.deleteJob(task);
};
const deleteAllJobs = () => {
  workspaceRef.value.jobs.forEach((job) => {
    if (processedJobs.value.has(job.task)) {
      return;
    }
    workspace.deleteJob(job.task);
  });
};

const onProgressUpdated = (
  task: string,
  state:
    | { state: 'finish'; abort: boolean }
    | { state: 'processed'; finished: number; error: number; total: number },
) => {
  if (state.state === 'finish') {
    const job = processedJobs.value.get(task)!;
    processedJobs.value.delete(task);
    if (!state.abort) {
      workspace.addJobRecord(job);
      workspace.deleteJob(task);
    }
  } else {
    const job = processedJobs.value.get(task)!;
    job.progress = {
      finished: state.finished,
      error: state.error,
      total: state.total,
    };
  }
};

const clearCache = async () =>
  doAction(
    Locator.cachedSegRepository().then((repo) => repo.clear('gpt-seg-cache')),
    'Cache cleared',
    message,
  );
</script>

<template>
  <div class="layout-content">
    <n-h1>GPT Workspace</n-h1>

    <bulletin>
      <n-flex>
        <c-a to="/forum/64f3d63f794cbb1321145c07" target="_blank">User guide</c-a>
        /
        <n-a href="https://chat.deepseek.com" target="_blank">
          DeepSeek Chat
        </n-a>
        /
        <n-a href="https://platform.deepseek.com/usage" target="_blank">
          DeepSeek API
        </n-a>
      </n-flex>
      <n-p>GPT web is no longer supported. It is recommended to use the deepseek API, the price is very low.</n-p>
      <n-p>Local novels support other languages such as Korean, while online novels/library novels only support Japanese for the time being.</n-p>
    </bulletin>

    <section-header title="Translator">
      <c-button
        label="Add translator"
        :icon="PlusOutlined"
        @action="showCreateWorkerModal = true"
      />
      <c-button-confirm
        hint="Are you sure you want to clear the cache?"
        label="Clear cache"
        :icon="DeleteOutlineOutlined"
        @action="clearCache"
      />
    </section-header>

    <n-empty
      v-if="workspaceRef.workers.length === 0"
      description="No translator"
    />
    <n-list>
      <vue-draggable
        v-model="workspaceRef.workers"
        :animation="150"
        handle=".drag-trigger"
      >
        <n-list-item v-for="worker of workspaceRef.workers" :key="worker.id">
          <job-worker
            :worker="{ translatorId: 'gpt', ...worker }"
            :get-next-job="getNextJob"
            @update:progress="onProgressUpdated"
          />
        </n-list-item>
      </vue-draggable>
    </n-list>

    <section-header title="Task queue">
      <c-button
        label="Local bookshelf"
        :icon="BookOutlined"
        @action="showLocalVolumeDrawer = true"
      />
      <c-button-confirm
        hint="Are you sure you want to clear the queue?"
        label="Clear queue"
        :icon="DeleteOutlineOutlined"
        @action="deleteAllJobs"
      />
    </section-header>
    <n-empty v-if="workspaceRef.jobs.length === 0" description="No tasks" />
    <n-list>
      <vue-draggable
        v-model="workspaceRef.jobs"
        :animation="150"
        handle=".drag-trigger"
      >
        <n-list-item v-for="job of workspaceRef.jobs" :key="job.task">
          <job-queue
            :job="job"
            :progress="processedJobs.get(job.task)?.progress"
            @top-job="workspace.topJob(job)"
            @bottom-job="workspace.bottomJob(job)"
            @delete-job="deleteJob(job.task)"
          />
        </n-list-item>
      </vue-draggable>
    </n-list>

    <job-record-section id="gpt" />
  </div>

  <local-volume-list-specific-translation
    v-model:show="showLocalVolumeDrawer"
    type="gpt"
  />

  <gpt-worker-modal v-model:show="showCreateWorkerModal" />
</template>
