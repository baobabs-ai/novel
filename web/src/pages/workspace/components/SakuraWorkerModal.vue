<script lang="ts" setup>
import { FormInst, FormItemRule, FormRules } from 'naive-ui';

import { Locator } from '@/data';
import { SakuraWorker } from '@/model/Translator';

const props = defineProps<{
  show: boolean;
  worker?: SakuraWorker;
}>();
const emit = defineEmits<{
  'update:show': [boolean];
}>();

const workspace = Locator.sakuraWorkspaceRepository();
const workspaceRef = workspace.ref;

const initFormValue = () => {
  const worker = props.worker;
  if (worker === undefined) {
    return {
      id: '',
      endpoint: '',
      segLength: 500,
      prevSegLength: 500,
    };
  } else {
    return { ...worker };
  }
};

const formRef = ref<FormInst>();
const formValue = ref(initFormValue());
const formRules: FormRules = {
  id: [
    {
      validator: (rule: FormItemRule, value: string) => value.trim().length > 0,
      message: 'Name cannot be empty',
      trigger: 'input',
    },
    {
      validator: (rule: FormItemRule, value: string) =>
        workspaceRef.value.workers
          .filter(({ id }) => id !== props.worker?.id)
          .find(({ id }) => id === value) === undefined,
      message: 'Name cannot be repeated',
      trigger: 'input',
    },
  ],
  endpoint: [
    {
      validator: (rule: FormItemRule, value: string) => value.trim().length > 0,
      message: 'Link cannot be empty',
      trigger: 'input',
    },
    {
      validator: (rule: FormItemRule, value: string) => {
        try {
          const url = new URL(value);
          return url.protocol === 'http:' || url.protocol === 'https:';
        } catch (_) {
          return false;
        }
      },
      message: 'Link is not valid',
      trigger: 'input',
    },
  ],
};

const submit = async () => {
  const validated = await new Promise<boolean>(function (resolve, _reject) {
    formRef.value?.validate((errors) => {
      if (errors) resolve(false);
      else resolve(true);
    });
  });

  if (!validated) return;
  const worker = { ...formValue.value };
  worker.id = worker.id.trim();
  worker.endpoint = worker.endpoint.trim();

  if (props.worker === undefined) {
    workspace.addWorker(worker);
  } else {
    const index = workspaceRef.value.workers.findIndex(
      ({ id }) => id === props.worker?.id,
    );
    workspaceRef.value.workers[index] = worker;
    emit('update:show', false);
  }
};

const verb = computed(() => (props.worker === undefined ? 'Add' : 'Update'));
</script>

<template>
  <c-modal
    :show="show"
    @update:show="$emit('update:show', $event)"
    :title="verb + 'Sakura Translator'"
  >
    <n-form
      ref="formRef"
      :model="formValue"
      :rules="formRules"
      label-placement="left"
      label-width="auto"
    >
      <n-form-item-row path="id" label="Name">
        <n-input
          v-model:value="formValue.id"
          placeholder="Give your translator a name"
          :input-props="{ spellcheck: false }"
        />
      </n-form-item-row>
      <n-form-item-row path="endpoint" label="Link">
        <n-input
          v-model:value="formValue.endpoint"
          placeholder="Translator link"
          :input-props="{ spellcheck: false }"
        />
      </n-form-item-row>

      <n-form-item-row path="segLength" label="Segment length">
        <n-input-number
          v-model:value="formValue.segLength"
          :show-button="false"
          :min="100"
        />
      </n-form-item-row>

      <n-form-item-row path="prevSegLength" label="Previous text length">
        <n-input-number
          v-model:value="formValue.prevSegLength"
          :show-button="false"
          :min="0"
        />
      </n-form-item-row>

      <n-text type="error" style="font-size: 12px">
        # Previous text length is a temporary function, cannot upload if not the default 500
      </n-text>
      <br />
      <n-text depth="3" style="font-size: 12px">
        # Segment length is still under testing, cannot upload if not the default 500
        <br />
        # Link example: http://127.0.0.1:8080
      </n-text>
    </n-form>

    <template #action>
      <c-button :label="verb" type="primary" @action="submit" />
    </template>
  </c-modal>
</template>
