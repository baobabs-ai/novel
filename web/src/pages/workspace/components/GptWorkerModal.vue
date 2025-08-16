<script lang="ts" setup>
import { FormInst, FormItemRule, FormRules } from 'naive-ui';

import { Locator } from '@/data';
import { GptWorker } from '@/model/Translator';

const props = defineProps<{
  show: boolean;
  worker?: GptWorker;
}>();
const emit = defineEmits<{
  'update:show': [boolean];
}>();

const workspace = Locator.gptWorkspaceRepository();
const workspaceRef = workspace.ref;

const initFormValue = (): {
  id: string;
  model: string;
  endpoint: string;
  key: string;
} => {
  const worker = props.worker;
  if (worker === undefined) {
    return {
      id: '',
      model: 'deepseek-chat',
      endpoint: 'https://api.deepseek.com',
      key: '',
    };
  } else {
    return {
      id: worker.id,
      model: worker.model,
      endpoint: worker.endpoint,
      key: worker.key,
    };
  }
};

const formRef = ref<FormInst>();
const formValue = ref(initFormValue());

const emptyCheck = (name: string) => ({
  validator: (rule: FormItemRule, value: string) => value.trim().length > 0,
  message: name + ' cannot be empty',
  trigger: 'input',
});

const formRules: FormRules = {
  id: [
    emptyCheck('Name'),
    {
      validator: (rule: FormItemRule, value: string) =>
        workspaceRef.value.workers
          .filter(({ id }) => id !== props.worker?.id)
          .find(({ id }) => id === value) === undefined,
      message: 'Name cannot be repeated',
      trigger: 'input',
    },
  ],
  model: [emptyCheck('Model')],
  endpoint: [
    emptyCheck('Link'),
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
  key: [
    emptyCheck('Key'),
    {
      level: 'warning',
      validator: (rule: FormItemRule, value: string) =>
        workspaceRef.value.workers
          .filter(({ id }) => id !== props.worker?.id)
          .find(({ key }) => key === value) === undefined,
      message: 'There are duplicate keys, please ensure that the API used supports concurrency',
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

  const { id, model, endpoint, key } = formValue.value;
  const worker = {
    id: id.trim(),
    type: 'api' as const,
    model: model.trim(),
    endpoint: endpoint.trim(),
    key: key.trim(),
  };

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
    :title="verb + 'GPT Translator'"
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

      <n-form-item-row path="model" label="Model">
        <n-input
          v-model:value="formValue.model"
          placeholder="Model name"
          :input-props="{ spellcheck: false }"
        />
      </n-form-item-row>
      <n-form-item-row path="endpoint" label="Link">
        <n-input
          v-model:value="formValue.endpoint"
          placeholder="OpenAI-compatible API link, deepseek is used by default"
          :input-props="{ spellcheck: false }"
        />
      </n-form-item-row>

      <n-form-item-row path="key" label="Key">
        <n-input
          v-model:value="formValue.key"
          placeholder="Please enter Api key"
          :input-props="{ spellcheck: false }"
        />
      </n-form-item-row>

      <n-text depth="3" style="font-size: 12px">
        # Link example: https://api.deepseek.com
      </n-text>
    </n-form>

    <template #action>
      <c-button :label="verb" type="primary" @action="submit" />
    </template>
  </c-modal>
</template>
