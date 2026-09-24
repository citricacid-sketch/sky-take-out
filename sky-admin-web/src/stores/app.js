import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  // 侧边栏折叠状态
  const sidebarCollapsed = ref(false)

  // 标签页列表
  const visitedTags = ref([])

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function addVisitedTag(route) {
    if (!route.meta?.title) return
    const exists = visitedTags.value.some((tag) => tag.path === route.path)
    if (!exists) {
      visitedTags.value.push({
        path: route.path,
        name: route.name,
        title: route.meta.title,
      })
    }
  }

  function removeVisitedTag(path) {
    const index = visitedTags.value.findIndex((tag) => tag.path === path)
    if (index !== -1) {
      visitedTags.value.splice(index, 1)
    }
  }

  return {
    sidebarCollapsed,
    visitedTags,
    toggleSidebar,
    addVisitedTag,
    removeVisitedTag,
  }
})
