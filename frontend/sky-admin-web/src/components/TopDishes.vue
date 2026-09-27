<!--
 * TopDishes 热销商品排行组件
 * 功能：展示商品销量排行榜，按销量从高到低排列
 * 排名逻辑：
 *   - 菜品列表由父组件传入（已按 sales 降序排列）
 *   - 前三名（index 0/1/2）使用金色渐变背景 + 白色文字高亮显示
 *   - 其余名次使用普通灰色背景
 *   - 每行显示：排名序号 + 菜品名称 + 销售份数
 * 空态处理：当 dishes 为空数组时，显示"暂无销售数据"提示
 */
<template>
  <div class="top-dishes">
    <!-- 有数据时：渲染排行列表 -->
    <ul v-if="dishes && dishes.length" class="top-list">
      <!-- 遍历菜品，index 用于判断前三名的特殊样式 -->
      <li v-for="(d, i) in dishes" :key="d.id || i" class="top-item">
        <!-- 排名序号：前三名（i < 3）添加 top3 高亮样式 -->
        <span class="rank" :class="{ top3: i < 3 }">{{ i + 1 }}</span>
        <!-- 菜品名称：超长时省略号截断 -->
        <span class="name">{{ d.name }}</span>
        <!-- 销售份数 -->
        <span class="count">{{ d.sales || 0 }} 份</span>
      </li>
    </ul>
    <!-- 空态：无数据时显示提示 -->
    <div v-else class="top-empty">
      <el-icon :size="24" color="#cbd5e1"><DocumentCopy /></el-icon>
      <p>暂无销售数据</p>
    </div>
  </div>
</template>

<script setup>
import { DocumentCopy } from '@element-plus/icons-vue'
defineProps({
  /** 菜品列表，每项包含 id/name/sales 字段，由父组件按销量降序传入 */
  dishes: { type: Array, default: () => [] },
})
</script>

<style lang="scss" scoped>
.top-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.top-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 6px;
  border-radius: 8px;
  transition: background 0.15s ease;

  &:hover { background: #f8fafc; }

  .rank {
    width: 24px;
    height: 24px;
    border-radius: 6px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 700;
    background: #f1f5f9;
    color: #64748b;
    flex-shrink: 0;

    /* 前三名使用金色渐变高亮 */
    &.top3 { background: linear-gradient(135deg, #F59E0B, #D97706); color: #fff; }
  }

  .name {
    flex: 1;
    font-size: 14px;
    color: #1f2937;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .count {
    font-size: 12px;
    color: #64748b;
    font-weight: 500;
  }
}

.top-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 240px;
  color: #94a3b8;

  p { margin: 0; font-size: 13px; }
}
</style>
