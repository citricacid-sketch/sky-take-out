import { defineStore } from 'pinia'
import { ref } from 'vue'
import { setToken, getToken, removeToken } from '@/utils/auth'
import { login as loginApi } from '@/api/employee'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken() || '')
  const name = ref('')
  const userName = ref('')
  const userId = ref(null)

  async function login(loginForm) {
    const data = await loginApi(loginForm)
    token.value = data.token
    name.value = data.name
    userName.value = data.userName
    userId.value = data.id
    setToken(data.token)
    return data
  }

  function logout() {
    token.value = ''
    name.value = ''
    userName.value = ''
    userId.value = null
    removeToken()
  }

  return {
    token,
    name,
    userName,
    userId,
    login,
    logout,
  }
})
