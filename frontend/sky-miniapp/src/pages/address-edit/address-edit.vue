<template>
  <view class="address-edit-page">
    <view class="form-card card">
      <view class="form-row">
        <text class="form-label">联系人</text>
        <input class="form-input" v-model="form.consignee" placeholder="请输入姓名" />
      </view>
      <view class="form-row">
        <text class="form-label">手机号</text>
        <input class="form-input" v-model="form.phone" type="number" placeholder="请输入手机号" />
      </view>
      <view class="form-row">
        <text class="form-label">地区</text>
        <picker mode="region" @change="onRegionChange" :value="regionValue">
          <view class="form-picker" :class="{ placeholder: !form.provinceName }">
            {{ form.provinceName ? `${form.provinceName} ${form.cityName} ${form.districtName}` : '请选择省市区' }}
          </view>
        </picker>
      </view>
      <view class="form-row form-row-top">
        <text class="form-label">详细地址</text>
        <textarea
          class="form-textarea"
          v-model="form.detail"
          placeholder="如道路、门牌号、小区、楼栋号、单元室等"
          placeholder-style="color: #B8BFC9"
        />
      </view>
      <view class="form-row">
        <text class="form-label">标签</text>
        <view class="label-options">
          <view
            v-for="tag in labelOptions"
            :key="tag"
            class="label-tag"
            :class="{ active: form.label === tag }"
            @click="form.label = tag"
          >
            {{ tag }}
          </view>
        </view>
      </view>
      <view class="form-row">
        <text class="form-label">设为默认</text>
        <switch
          :checked="form.isDefault === 1"
          @change="form.isDefault = $event.detail.value ? 1 : 0"
          color="#FFC300"
        />
      </view>
    </view>

    <view class="bottom-bar">
      <view v-if="isEdit" class="btn-delete" @click="onDelete">删除地址</view>
      <view class="btn-primary" @click="onSave">保存</view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { addAddress, updateAddress, getAddressById, deleteAddress } from '@/api/address'

const isEdit = ref(false)
const editId = ref(null)

const form = reactive({
  consignee: '',
  phone: '',
  provinceName: '',
  cityName: '',
  districtName: '',
  detail: '',
  label: '',
  isDefault: 0
})

const labelOptions = ['家', '公司', '学校', '父母家']

const regionValue = ref(['', '', ''])

function onRegionChange(e) {
  const [province, city, district] = e.detail.value
  form.provinceName = province
  form.cityName = city
  form.districtName = district
  regionValue.value = e.detail.value
}

async function fetchDetail() {
  try {
    const data = await getAddressById(editId.value)
    Object.assign(form, data)
    if (data.provinceName) {
      regionValue.value = [data.provinceName, data.cityName, data.districtName]
    }
  } catch (e) {}
}

async function onSave() {
  if (!form.consignee) return uni.showToast({ title: '请输入联系人', icon: 'none' })
  if (!form.phone || !/^1\d{10}$/.test(form.phone)) return uni.showToast({ title: '请输入正确手机号', icon: 'none' })
  if (!form.provinceName) return uni.showToast({ title: '请选择地区', icon: 'none' })
  if (!form.detail) return uni.showToast({ title: '请输入详细地址', icon: 'none' })

  try {
    if (isEdit.value) {
      await updateAddress({ id: editId.value, ...form })
      uni.showToast({ title: '修改成功', icon: 'success' })
    } else {
      await addAddress({ ...form })
      uni.showToast({ title: '添加成功', icon: 'success' })
    }
    setTimeout(() => uni.navigateBack(), 1000)
  } catch (e) {}
}

function onDelete() {
  uni.showModal({
    title: '提示',
    content: '确定删除该地址吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await deleteAddress(editId.value)
          uni.showToast({ title: '已删除', icon: 'success' })
          setTimeout(() => uni.navigateBack(), 1000)
        } catch (e) {}
      }
    }
  })
}

onMounted((options) => {
  if (options && options.id) {
    isEdit.value = true
    editId.value = options.id
    fetchDetail()
  }
})
</script>

<style lang="scss" scoped>
.address-edit-page {
  min-height: 100vh;
  padding: var(--space-s);
  padding-bottom: 160rpx;
}

.form-card {
  .form-row {
    display: flex;
    align-items: center;
    padding: 24rpx 0;
    border-bottom: 1rpx solid var(--sky-border);

    &.form-row-top {
      align-items: flex-start;
    }

    &:last-child {
      border-bottom: none;
    }

    .form-label {
      font-size: 26rpx;
      color: var(--sky-body);
      width: 160rpx;
      flex-shrink: 0;
    }

    .form-input,
    .form-textarea,
    .form-picker {
      flex: 1;
      font-size: 26rpx;
      color: var(--sky-title);
    }

    .form-textarea {
      height: 120rpx;
      padding: 8rpx 0;
    }

    .form-picker {
      &.placeholder {
        color: var(--sky-disable);
      }
    }

    .label-options {
      display: flex;
      flex-wrap: wrap;

      .label-tag {
        padding: 10rpx 24rpx;
        border-radius: 30rpx;
        background: var(--sky-page);
        color: var(--sky-body);
        font-size: 24rpx;
        margin-right: 16rpx;

        &.active {
          background: var(--sky-primary-soft);
          color: var(--sky-primary-dark);
          border: 2rpx solid var(--sky-primary);
        }
      }
    }
  }
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  padding: 24rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  background: var(--sky-card);
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.08);

  .btn-delete {
    font-size: 28rpx;
    color: var(--sky-danger);
    padding: 20rpx 32rpx;
    border: 2rpx solid #FFD6D6;
    border-radius: 50rpx;
    margin-right: 16rpx;
  }

  .btn-primary {
    flex: 1;
  }
}
</style>
