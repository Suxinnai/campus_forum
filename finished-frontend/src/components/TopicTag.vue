<script setup>
import {useCounterStore} from "@/stores/counter.js";

const store = useCounterStore();

defineProps({
  type: Number
})
</script>

<template>
  <div class="topic-type" :style="{
              color: store.findTypeById(type)?.color + 'EE',
              'border-color' : store.findTypeById(type)?.color + '77',
              'background' : store.findTypeById(type)?.color + '22',
            }">
    {{store.findTypeById(type)?.name}}
  </div>
</template>

<style scoped>
.topic-type {
  display: inline-flex;
  align-items: center;
  border: none;
  font-weight: 600;
  border-radius: 12px;
  font-size: 12px;
  padding: 0 8px;
  height: 22px;
}
</style>