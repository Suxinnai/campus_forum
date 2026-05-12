<script setup>
import { get } from "@/net/api.js";
import { useAppStore } from "@/stores/app-store.js";

const store = useAppStore();

// 使用 _typesLoaded 标志严格防止重复加载
if (!store.forum._typesLoaded) {
  store.forum._typesLoaded = true;
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
