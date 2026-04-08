<template>
  <div class="welcome-container">
    <div class="welcome-card">
      <div class="welcome-card-left">
        <img src="@/assets/login_illustration.png" class="illustration" alt="Welcome Illustration">
      </div>
      <div class="welcome-card-right">
        <router-view v-slot="{ Component }">
          <transition name="el-fade-in-linear" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<style scoped>
.welcome-container {
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f7f9;
  overflow: auto;
  padding: 20px;
}

.welcome-card {
  width: 100%;
  max-width: 1000px;
  min-height: 600px;
  background-color: white;
  border-radius: 24px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.08);
  display: flex;
  overflow: hidden;
  z-index: 10;
  transition: all 0.3s ease;
}

.welcome-card-left {
  flex: 1.1;
  background-color: #eaf4ff;
  border-right: 1px solid #f0f0f0;
  overflow: hidden;
  display: block;
}

@media (max-width: 768px) {
  .welcome-card-left {
    display: none;
  }
  .welcome-card {
    max-width: 450px;
    min-height: auto;
  }
}

.illustration {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.3s ease;
}

.illustration:hover {
  transform: scale(1.02);
}

.welcome-card-right {
  flex: 1;
  padding: 60px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
</style>