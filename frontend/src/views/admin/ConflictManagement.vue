<template>
  <div class="conflict-management-page">
    <div class="page-header">
      <h1>⚠️ Conflict Management</h1>
      <p class="subtitle">View and manage data synchronization conflicts</p>
    </div>

    <div class="info-card">
      <h3>About Conflict Management</h3>
      <p>
        When data changes occur simultaneously in multiple databases, conflicts may arise.
        Conflicts are tracked and can be resolved by administrators through secure token-based links.
      </p>
      <p class="note">
        <strong>Note:</strong> Conflict resolution requires a unique access token that is typically
        sent via email or generated through the admin API. The existing conflict view pages use
        token-based authentication separate from this SPA.
      </p>
    </div>

    <div class="conflict-access-card">
      <h3>Access Conflict Details</h3>
      <p>If you have a conflict token, enter it below to view the conflict details:</p>

      <div class="token-form">
        <div class="form-group">
          <label for="conflictToken">Conflict Access Token</label>
          <input
              id="conflictToken"
              v-model="conflictToken"
              type="text"
              placeholder="Enter your conflict token"
              :disabled="loading"
          />
        </div>

        <button class="btn-primary" @click="viewConflict" :disabled="!conflictToken || loading">
          View Conflict
        </button>
      </div>

      <div v-if="error" class="error-message">{{ error }}</div>
    </div>

    <div class="info-card">
      <h3>Alternative Access Methods</h3>
      <ul>
        <li>
          <strong>Direct Link:</strong> Access conflicts using the full URL:
          <code>http://localhost:8080/conflicts/view?token=YOUR_TOKEN</code>
        </li>
        <li>
          <strong>Email Notification:</strong> Conflict notification emails include direct links
          with embedded tokens for easy access.
        </li>
        <li>
          <strong>API Access:</strong> Administrators can generate conflict tokens programmatically
          using the admin API endpoints.
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const conflictToken = ref('')
const loading = ref(false)
const error = ref(null)
function viewConflict() {
  if (!conflictToken.value) {
    error.value = 'Please enter a conflict token'
    return
  }
  loading.value = true
  error.value = null
  // Redirect to the conflict view page with the token
  // This opens in a new window/tab since it's a separate page outside the SPA
  const url = `/conflicts/view?token=${encodeURIComponent(conflictToken.value)}`
  window.open(url, '_blank')

  loading.value = false
  conflictToken.value = ''
}
</script>

<style scoped>
.conflict-management-page {
  max-width: 900px;
}
.page-header {
  background: white;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 24px;
}
.page-header h1 {
  color: #f57c00;
  font-size: 28px;
  margin-bottom: 8px;
}
.subtitle {
  color: #666;
  font-size: 14px;
}
.info-card {
  background: white;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 24px;
}
.info-card h3 {
  color: #333;
  font-size: 18px;
  margin-bottom: 12px;
}
.info-card p {
  color: #555;
  line-height: 1.8;
  margin-bottom: 12px;
}
.info-card ul {
  margin-left: 20px;
  color: #555;
}
.info-card li {
  margin-bottom: 12px;
  line-height: 1.8;
}
.note {
  background: #fff3e0;
  padding: 12px;
  border-left: 4px solid #ff9800;
  border-radius: 4px;
  margin-top: 16px;
}
code {
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: #d32f2f;
}
.conflict-access-card {
  background: white;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 24px;
}
.conflict-access-card h3 {
  color: #333;
  font-size: 18px;
  margin-bottom: 12px;
}
.conflict-access-card > p {
  color: #555;
  margin-bottom: 20px;
}
.token-form {
  display: flex;
  gap: 16px;
  align-items: flex-end;
}
.form-group {
  flex: 1;
  display: flex;
  flex-direction: column;
}
label {
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
  font-size: 14px;
}
input {
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  transition: border-color 0.2s;
}
input:focus {
  outline: none;
  border-color: #1976d2;
  box-shadow: 0 0 0 3px rgba(25, 118, 210, 0.1);
}
input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}
.btn-primary {
  padding: 12px 24px;
  background: #1976d2;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}
.btn-primary:hover:not(:disabled) {
  background: #1565c0;
}
.btn-primary:disabled {
  background: #ccc;
  cursor: not-allowed;
}
.error-message {
  background: #ffebee;
  color: #c62828;
  padding: 12px 16px;
  border-radius: 6px;
  margin-top: 16px;
  font-size: 14px;
}
@media (max-width: 768px) {
  .token-form {
    flex-direction: column;
    align-items: stretch;
  }

  .btn-primary {
    width: 100%;
  }
}
</style>