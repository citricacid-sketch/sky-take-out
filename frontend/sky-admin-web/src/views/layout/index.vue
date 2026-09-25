<template>
  <div class="layout-container" :class="{ collapsed: appStore.sidebarCollapsed }">
    <AppSidebar />
    <div class="layout-main">
      <AppHeader />
      <div class="layout-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useAppStore } from '@/stores/app'
import AppHeader from '@/components/AppHeader.vue'
import AppSidebar from '@/components/AppSidebar.vue'

const appStore = useAppStore()
</script>

<style lang="scss" scoped>
.layout-container {
  display: flex;
  height: 100%;
  width: 100%;

  .layout-main {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;

    .layout-content {
      flex: 1;
      padding: 16px;
      overflow-y: auto;
      background: #f0f2f5;
    }
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
