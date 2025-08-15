<script lang="ts" setup>
import { formatError } from '@/data';

const message = useMessage();

const props = defineProps<{
  label: string;
  allowSendEmail: () => boolean;
  sendEmail: () => Promise<unknown>;
}>();

type VerifyState =
  | { state: 'sending' }
  | { state: 'cooldown'; seconds: number }
  | undefined;

const verifyState = ref<VerifyState>(undefined);

const verifyButtonLabel = computed(() => {
  if (verifyState.value === undefined) {
    return props.label;
  } else if (verifyState.value.state === 'sending') {
    return 'Sending';
  } else {
    return `${verifyState.value.seconds}s cooldown`;
  }
});

async function realSendEmail() {
  if (verifyState.value !== undefined) return;
  if (!props.allowSendEmail) return;

  verifyState.value = { state: 'sending' };
  await props
    .sendEmail()
    .then(() => {
      verifyState.value = { state: 'cooldown', seconds: 60 };
      message.info('Email sent');

      const timer = window.setInterval(() => {
        if (
          verifyState.value?.state === 'cooldown' &&
          verifyState.value.seconds > 0
        ) {
          verifyState.value.seconds--;
        } else {
          verifyState.value = undefined;
          window.clearInterval(timer);
        }
      }, 1000);
    })
    .catch(async (e) => {
      verifyState.value = undefined;
      message.error('Failed to send email:' + (await formatError(e)));
    });
}
</script>

<template>
  <n-button
    type="primary"
    :disabled="verifyState !== undefined"
    @click="realSendEmail()"
    style="width: 100px"
  >
    {{ verifyButtonLabel }}
  </n-button>
</template>
