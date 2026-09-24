<template>
  <aside class="app-sidebar" :class="{ collapsed: appStore.sidebarCollapsed }">
    <div class="logo">
      <h1 v-show="!appStore.sidebarCollapsed">苍穹外卖</h1>
      <h1 v-show="appStore.sidebarCollapsed">苍</h1>
    </div>
    <el-menu
      :default-active="activeMenu"
      :collapse="appStore.sidebarCollapsed"
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#FFC300"
      router
      unique-opened
    >
      <sidebar-item
        v-for="route in menuRoutes"
        :key="route.path"
        :item="route"
        :base-path="route.path"
      />
    </el-menu>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import SidebarItem from './SidebarItem.vue'

const route = useRoute()
const appStore = useAppStore()

const menuRoutes = [
  { path: '/dashboard', meta: { title: '工作台', icon: 'Odometer' } },
  { path: '/employee', meta: { title: '员工管理', icon: 'User' } },
  { path: '/category', meta: { title: '分类管理', icon: 'Menu' } },
  { path: '/dish', meta: { title: '菜品管理', icon: 'Food' } },
  { path: '/setmeal', meta: { title: '套餐管理', icon: 'Dish' } },
  { path: '/order', meta: { title: '订单管理', icon: 'List' } },
  { path: '/shop', meta: { title: '店铺设置', icon: 'Shop' } },
  { path: '/report', meta: { title: '数据统计', icon: 'DataLine' } },
  { path: '/ai-assistant', meta: { title: 'AI 数据助手', icon: 'ChatDotRound' } },
  { path: '/rider', meta: { title: '骑手管理', icon: 'Van' } },
  { path: '/delivery', meta: { title: '配送管理', icon: 'Position' } },
]

const activeMenu = computed(() => {
  return route.path
})
</script>

<style lang="scss" scoped>
.app-sidebar {
  width: 210px;
  height: 100%;
  background: #304156;
  transition: width 0.3s;
  overflow: hidden;

  &.collapsed {
    width: 64px;
  }

  .logo {
    height: 50px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #263445;

    h1 {
      color: #FFC300;
      font-size: 18px;
      font-weight: 600;
      margin: 0;
      white-space: nowrap;
    }
  }

  :deep(.el-menu) {
    border-right: none;
  }

  :deep(.el-menu--collapse) {
    width: 64px;
  }
}
</style>
