<template>
  <div class="light-card">
      <slot/>
  </div>
</template>

<style scoped>
.light-card {
  background: rgba(255, 255, 255, 0.45);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: var(--card-radius, 16px);
  padding: 18px;
  box-shadow: 0 8px 32px rgba(31, 38, 135, 0.05); /* SaaS standard glass shadow */
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  overflow: hidden;
}

.dark .light-card {
  background: rgba(15, 23, 42, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.light-card:hover {
  box-shadow: 0 12px 40px rgba(31, 38, 135, 0.08);
  transform: translateY(-2px);
  border-color: rgba(255, 255, 255, 0.8);
}

.dark .light-card:hover {
  border-color: rgba(255, 255, 255, 0.15);
}
</style>