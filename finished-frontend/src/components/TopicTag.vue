<script setup>
import {useAppStore} from "@/stores/app-store.js";
import { computed } from 'vue'

const store = useAppStore();

const props = defineProps({
  type: Number,
  tags: Array
})

// 优先使用 tags，回退到 type
const tagLabel = computed(() => {
  if (props.tags !== undefined) return props.tags?.length > 0 ? props.tags[0] : '未分类'
  const found = store.findTypeById(props.type)
  return found ? found.name : null
})

const tagColor = computed(() => {
  if (tagLabel.value === '未分类') return '#94a3b8'
  // 尝试通过标签名匹配类型颜色
  if (tagLabel.value) {
    const match = store.forum.types?.find(t => t.name === tagLabel.value)
    if (match) return match.color
  }
  // 回退到通过 type id 查找
  const found = store.findTypeById(props.type)
  return found ? found.color : '#64748b'
})
</script>

<template>
  <div v-if="tagLabel" class="topic-type" :style="{ '--tag-color': tagColor }">
    <span class="tag-dot"></span>
    {{ tagLabel }}
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
