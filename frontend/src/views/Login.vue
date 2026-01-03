<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h1>🔐 SSS Sync System</h1>
        <p class="subtitle">Multi-Database Synchronization Platform</p>
      </div>

      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label for="username">Username</label>
          <input
              id="username"
              v-model="credentials.username"
              type="text"
              placeholder="Enter your username"
              required
              :disabled="loading"
          />
        </div>

        <div class="form-group">
          <label for="password">Password</label>
          <input
              id="password"
              v-model="credentials.password"
              type="password"
              placeholder="Enter your password"
              required
              :disabled="loading"
          />
        </div>

        <div v-if="error" class="error-message">
          {{ error }}
        </div>

        <button type="submit" class="btn-login" :disabled="loading">
          {{ loading ? 'Logging in...' : 'Login' }}
        </button>
      </form>

      <div class="demo-info">
        <p><strong>Demo Accounts:</strong></p>
        <p>Admin: admin / admin123</p>
        <p>User: user1 / user123</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
const router = useRouter()
const authStore = useAuthStore()
const credentials = ref({
  username: '',
  password: ''
})
const loading = ref(false)
const error = ref(null)
async function handleLogin() {
  loading.value = true
  error.value = null

  try {
    await authStore.login(credentials.value)
    // Redirect based on role
    router.push(authStore.isAdmin ? '/admin' : '/orders/new')
  } catch (err) {
    console.error('Login error:', err)
    error.value = err.response?.data?.message || 'Invalid username or password'
  } finally {
    loading.value = false
  }
}
</script>
<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}
.login-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  padding: 40px;
  max-width: 440px;
  width: 100%;
}
.login-header {
  text-align: center;
  margin-bottom: 32px;
}
.login-header h1 {
  color: #333;
  font-size: 28px;
  margin-bottom: 8px;
}
.subtitle {
  color: #666;
  font-size: 14px;
}
.login-form {
  margin-bottom: 24px;
}
.form-group {
  margin-bottom: 20px;
}
.form-group label {
  display: block;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
  font-size: 14px;
}
.form-group input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  transition: border-color 0.2s;
}
.form-group input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}
.form-group input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}
.error-message {
  background: #ffebee;
  color: #c62828;
  padding: 12px 16px;
  border-radius: 6px;
  margin-bottom: 20px;
  font-size: 14px;
}
.btn-login {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.btn-login:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}
.btn-login:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}
.demo-info {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 6px;
  text-align: center;
  font-size: 13px;
  color: #666;
}
.demo-info p {
  margin: 4px 0;
}
.demo-info strong {
  color: #333;
}
@media (max-width: 480px) {
  .login-card {
    padding: 24px;
  }

  .login-header h1 {
    font-size: 24px;
  }
}
</style>