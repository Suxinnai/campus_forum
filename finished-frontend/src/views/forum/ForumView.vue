<script setup>
import { get } from "@/net/api.js";
import { useAppStore } from "@/stores/app-store.js";

const store = useAppStore();

// 只在 types 未加载时才请求，防止重复 push
if (!store.forum.types || store.forum.types.length === 0) {
  // Prevent parallel requests giving duplicate tabs
  store.forum.types = [{ name: "全部", id: 0, color: 'linear-gradient(45deg, white, red, orange, gold, green, blue)' }];
  get("/api/forum/types", data => {
    store.forum.types = [
      { name: "全部", id: 0, color: 'linear-gradient(45deg, white, red, orange, gold, green, blue)' },
      ...data
    ];
  });
}
</script>

<template>
  <div>
    <router-view v-slot="{ Component }">
      <transition name="el-fade-in-linear">
        <keep-alive include="TopicListView">
          <component :is="Component" style="height: 100%" />
        </keep-alive>
      </transition>
    </router-view>
    <el-backtop target=".main-area .el-scrollbar__wrap" :right="20" :bottom="70" />
  </div>
</template>
