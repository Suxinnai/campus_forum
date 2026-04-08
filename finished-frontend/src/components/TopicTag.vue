<script setup>
import {useAppStore} from "@/stores/app-store.js";

const store = useAppStore();

defineProps({
  type: Number
})
</script>

<template>
  <div class="topic-type" :style="{
    '--tag-color': store.findTypeById(type)?.color || '#909399'
  }">
    <span class="tag-dot"></span>
    {{store.findTypeById(type)?.name || '未知'}}
  </div>
</template>

<style scoped>
.topic-type {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-weight: 600;
  border-radius: 999px;
  font-size: 12px;
  padding: 0 10px 0 8px;
  height: 24px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  color: var(--tag-color);
  background: color-mix(in srgb, var(--tag-color) 11%, transparent);
  border: 1px solid color-mix(in srgb, var(--tag-color) 20%, transparent);
}

.tag-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--tag-color);
  flex-shrink: 0;
}

.topic-type:hover {
  background: color-mix(in srgb, var(--tag-color) 20%, transparent);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px color-mix(in srgb, var(--tag-color) 20%, transparent);
}
</style>
