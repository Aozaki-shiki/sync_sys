import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '../api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || null)
  const userId = ref(localStorage.getItem('userId') || null)
  const username = ref(localStorage.getItem('username') || null)
  const role = ref(localStorage.getItem('role') || null)

  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'ADMIN')
  const isUser = computed(() => role.value === 'USER')

  async function login(credentials) {
    const response = await api.post('/api/auth/login', credentials)
    const data = response.data.data
    
    token.value = data.accessToken
    userId.value = data.userId
    username.value = data.username
    role.value = data.role

    localStorage.setItem('token', data.accessToken)
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('username', data.username)
    localStorage.setItem('role', data.role)

    return data
  }

  function logout() {
    token.value = null
    userId.value = null
    username.value = null
    role.value = null

    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
  }

  return {
    token,
    userId,
    username,
    role,
    isAuthenticated,
    isAdmin,
    isUser,
    login,
    logout
  }
})
