<template>
  <el-menu-item :index="item.path">
    <el-icon v-if="item.meta?.icon">
      <component :is="item.meta.icon" />
    </el-icon>
    <template #title>
      <span>{{ item.meta?.title }}</span>
    </template>
  </el-menu-item>
</template>

<script setup>
defineProps({
  item: { type: Object, required: true },
  basePath: { type: String, default: '' },
})
</script>
