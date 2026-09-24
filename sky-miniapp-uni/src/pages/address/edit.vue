<template>
  <view class="address-edit-page">
    <view class="form-section card">
      <view class="form-row">
        <text class="form-label">联系人</text>
        <input class="form-input" v-model="form.consignee" placeholder="请输入姓名" />
      </view>
      <view class="form-row">
        <text class="form-label">手机号</text>
        <input class="form-input" v-model="form.phone" type="number" placeholder="请输入手机号" />
      </view>
      <view class="form-row">
        <text class="form-label">性别</text>
        <view class="gender-row">
          <view class="gender-opt" :class="{ active: form.sex === '0' }" @click="form.sex = '0'">先生</view>
          <view class="gender-opt" :class="{ active: form.sex === '1' }" @click="form.sex = '1'">女士</view>
        </view>
      </view>
      <view class="form-row">
        <text class="form-label">地区</text>
        <picker mode="region" @change="onRegionChange" :value="regionValue">
          <view class="picker-value" :class="{ placeholder: !form.provinceName }">
            {{ form.provinceName ? `${form.provinceName} ${form.cityName} ${form.districtName}` : '请选择地区' }}
          </view>
        </picker>
      </view>
      <view class="form-row">
        <text class="form-label">详细地址</text>
        <input class="form-input" v-model="form.detail" placeholder="街道、楼牌号等" />
      </view>
      <view class="form-row">
        <text class="form-label">标签</text>
        <view class="tag-row">
          <view
            v-for="tag in tags"
            :key="tag"
            class="tag-opt"
            :class="{ active: form.label === tag }"
            @click="form.label = tag"
          >{{ tag }}</view>
        </view>
      </view>
    </view>

    <view class="default-row card">
      <text>设为默认地址</text>
      <switch :checked="form.isDefault === 1" @change="onDefaultChange" color="#FFC300" />
    </view>

    <view v-if="isEdit" class="delete-btn" @click="handleDelete">删除地址</view>
    <view class="save-btn" @click="handleSave">保存</view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted, onLoad } from 'vue'
import request from '../../utils/request'

const isEdit = ref(false)
const addressId = ref(null)
const tags = ['家', '公司', '学校', '其他']
const regionValue = ref(['北京市', '北京市', '朝阳区'])

const form = reactive({
  consignee: '',
  phone: '',
  sex: '0',
  provinceName: '',
  cityName: '',
  districtName: '',
  detail: '',
  label: '',
  isDefault: 0
})

const loadAddress = async () => {
  if (!addressId.value) return
  // 后端暂无 GET /user/addressBook/${id} 接口，编辑模式暂不自动加载
  // 如需编辑加载，可在此补充对应接口调用并移除下方注释
  /* try {
    const data = await request(`/user/addressBook/${addressId.value}`, 'GET')
    if (data) {
      Object.assign(form, data)
      regionValue.value = [data.provinceName, data.cityName, data.districtName]
    }
  } catch (e) {} */
}

const onRegionChange = (e) => {
  const [province, city, district] = e.detail.value
  form.provinceName = province
  form.cityName = city
  form.districtName = district
  regionValue.value = e.detail.value
}

const onDefaultChange = (e) => {
  form.isDefault = e.detail.value ? 1 : 0
}

const handleSave = async () => {
  if (!form.consignee) return uni.showToast({ title: '请输入联系人', icon: 'none' })
  if (!form.phone) return uni.showToast({ title: '请输入手机号', icon: 'none' })
  if (!form.provinceName) return uni.showToast({ title: '请选择地区', icon: 'none' })
  if (!form.detail) return uni.showToast({ title: '请输入详细地址', icon: 'none' })

  try {
    uni.showLoading({ title: '保存中...' })
    if (isEdit.value) {
      await request('/user/addressBook', 'PUT', { ...form, id: addressId.value })
    } else {
      await request('/user/addressBook', 'POST', form)
    }
    uni.hideLoading()
    uni.showToast({ title: '保存成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 800)
  } catch (e) {
    uni.hideLoading()
  }
}

const handleDelete = () => {
  uni.showModal({
    title: '提示',
    content: '确定删除该地址吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await request(`/user/addressBook?id=${addressId.value}`, 'DELETE')
          uni.showToast({ title: '已删除', icon: 'success' })
          setTimeout(() => uni.navigateBack(), 800)
        } catch (e) {}
      }
    }
  })
}

onLoad((options) => {
  if (options && options.id) {
    isEdit.value = true
    addressId.value = options.id
    loadAddress()
  }
})
</script>

<style lang="scss" scoped>
.address-edit-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 20rpx;
  padding-bottom: 140rpx;
}

.form-section {
  padding: 0 30rpx;
  margin-bottom: 20rpx;
}

.form-row {
  display: flex;
  align-items: center;
  padding: 28rpx 0;
  border-bottom: 1rpx solid #f5f5f5;

  &:last-child { border-bottom: none; }
}

.form-label {
  width: 160rpx;
  font-size: 28rpx;
  color: #333;
}

.form-input {
  flex: 1;
  font-size: 28rpx;
  color: #333;
}

.gender-row {
  display: flex;
  gap: 20rpx;
}

.gender-opt {
  padding: 10rpx 28rpx;
  border-radius: 50rpx;
  background-color: #f5f5f5;
  font-size: 26rpx;
  color: #666;
  border: 1rpx solid transparent;

  &.active {
    background-color: #FFF8E0;
    color: #333;
    border-color: #FFC300;
  }
}

.picker-value {
  flex: 1;
  font-size: 28rpx;
  color: #333;

  &.placeholder {
    color: #ccc;
  }
}

.tag-row {
  display: flex;
  gap: 16rpx;
}

.tag-opt {
  padding: 8rpx 24rpx;
  border-radius: 50rpx;
  background-color: #f5f5f5;
  font-size: 24rpx;
  color: #666;
  border: 1rpx solid transparent;

  &.active {
    background-color: #FFF8E0;
    color: #333;
    border-color: #FFC300;
  }
}

.default-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx;
  margin-bottom: 20rpx;
  font-size: 28rpx;
  color: #333;
}

.delete-btn {
  background-color: #fff;
  color: #FF4B33;
  height: 88rpx;
  border-radius: 50rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  margin-bottom: 20rpx;
}

.save-btn {
  background-color: #FFC300;
  color: #fff;
  height: 88rpx;
  border-radius: 50rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  font-weight: 500;
}
</style>
