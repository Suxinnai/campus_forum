<template>
  <div class="light-card">
      <slot/>
  </div>
</template>

<style scoped>
.light-card {
  background-color: var(--el-bg-color);
  border-radius: var(--card-radius);
  padding: 18px;
  box-shadow: var(--card-shadow);
  transition: box-shadow 0.3s var(--transition-cubic), transform 0.3s var(--transition-cubic);
}

.light-card:hover {
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08);
}

.dark .light-card:hover {
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.3);
}
</style>