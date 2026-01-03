<template>
  <div class="admin-layout">
    <aside class="sidebar" role="navigation" aria-label="Admin navigation">
      <div class="sidebar-header">
        <h2>🔧 Admin Console</h2>
        <p class="username">{{ authStore.username }}</p>
      </div>

      <nav class="sidebar-nav" aria-label="Main admin menu">
        <router-link to="/admin/queries/complex" class="nav-item">
          <span class="icon" aria-hidden="true">📊</span>
          <span>Complex Query</span>
        </router-link>

        <router-link to="/admin/reports/daily-sync" class="nav-item">
          <span class="icon" aria-hidden="true">📈</span>
          <span>Daily Sync Report</span>
        </router-link>

        <router-link to="/admin/conflicts" class="nav-item">
          <span class="icon" aria-hidden="true">⚠️</span>
          <span>Conflict Management</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <button @click="handleLogout" class="btn-logout" aria-label="Logout from admin console">
          Logout
        </button>
      </div>
    </aside>

    <main class="main-content" role="main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
const router = useRouter()
const authStore = useAuthStore()
function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
}
.sidebar {
  width: 260px;
  background: #2c3e50;
  color: white;
  display: flex;
  flex-direction: column;
  position: fixed;
  height: 100vh;
  left: 0;
  top: 0;
}
.sidebar-header {
  padding: 24px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}
.sidebar-header h2 {
  font-size: 20px;
  margin-bottom: 8px;
}
.username {
  font-size: 14px;
  color: #95a5a6;
}
.sidebar-nav {
  flex: 1;
  padding: 20px 0;
  overflow-y: auto;
}
.nav-item {
  display: flex;
  align-items: center;
  padding: 14px 20px;
  color: #ecf0f1;
  text-decoration: none;
  transition: all 0.2s;
  border-left: 3px solid transparent;
}
.nav-item:hover {
  background: rgba(255, 255, 255, 0.05);
  border-left-color: #3498db;
}
.nav-item.router-link-active {
  background: rgba(52, 152, 219, 0.15);
  border-left-color: #3498db;
  color: white;
}
.nav-item .icon {
  margin-right: 12px;
  font-size: 18px;
}
.sidebar-footer {
  padding: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}
.btn-logout {
  width: 100%;
  padding: 12px;
  background: #e74c3c;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}
.btn-logout:hover {
  background: #c0392b;
}
.main-content {
  flex: 1;
  margin-left: 260px;
  padding: 24px;
  background: #f5f7fa;
  min-height: 100vh;
}
@media (max-width: 768px) {
  .sidebar {
    width: 100%;
    height: auto;
    position: relative;
  }

  .main-content {
    margin-left: 0;
  }

  .admin-layout {
    flex-direction: column;
  }
}
</style>