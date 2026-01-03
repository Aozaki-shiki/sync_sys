<template>
  <div class="complex-query-page">
    <div class="page-header">
      <h1>📊 Complex Order Analytics Query</h1>
      <p class="subtitle">Multi-table join with aggregations and nested subqueries for business insights</p>
    </div>

    <div class="query-form-card">
      <h2>Query Parameters</h2>
      <div class="form-grid">
        <div class="form-group">
          <label for="startDate">Start Date *</label>
          <input
              id="startDate"
              v-model="queryParams.startDate"
              type="date"
              required
          />
        </div>

        <div class="form-group">
          <label for="endDate">End Date *</label>
          <input
              id="endDate"
              v-model="queryParams.endDate"
              type="date"
              required
          />
        </div>

        <div class="form-group">
          <label for="categoryName">Category Name (Optional)</label>
          <input
              id="categoryName"
              v-model="queryParams.categoryName"
              type="text"
              placeholder="e.g., 食品"
          />
        </div>

        <div class="form-group">
          <label for="supplierName">Supplier Name (Optional)</label>
          <input
              id="supplierName"
              v-model="queryParams.supplierName"
              type="text"
              placeholder="e.g., 供应商A"
          />
        </div>

        <div class="form-group">
          <label for="pageSize">Page Size</label>
          <select id="pageSize" v-model.number="queryParams.pageSize">
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
            <option :value="100">100</option>
          </select>
        </div>
      </div>

      <button class="btn-primary" @click="executeQuery" :disabled="loading">
        {{ loading ? 'Querying...' : 'Execute Query' }}
      </button>
    </div>

    <div v-if="error" class="error-message">{{ error }}</div>

    <div v-if="results" class="results-card">
      <div class="stats-summary">
        <div class="stat-card">
          <div class="stat-label">Total Records</div>
          <div class="stat-value">{{ results.totalRecords }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Current Page</div>
          <div class="stat-value">{{ results.currentPage }} / {{ results.totalPages }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Results Shown</div>
          <div class="stat-value">{{ results.data.length }}</div>
        </div>
      </div>

      <div v-if="results.data.length > 0">
        <div class="table-container">
          <table>
            <thead>
            <tr>
              <th>Category</th>
              <th>Supplier</th>
              <th>Total Orders</th>
              <th>Total Quantity</th>
              <th>Total Revenue</th>
              <th>Avg Order Value</th>
              <th>Unique Customers</th>
              <th>Top Product</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="row in results.data" :key="row.categoryName + '-' + row.supplierName">
              <td>{{ row.categoryName }}</td>
              <td>{{ row.supplierName }}</td>
              <td>{{ row.totalOrders }}</td>
              <td>{{ row.totalQuantity }}</td>
              <td>¥{{ formatNumber(row.totalRevenue) }}</td>
              <td>¥{{ formatNumber(row.avgOrderValue) }}</td>
              <td>{{ row.uniqueCustomers }}</td>
              <td>{{ row.topProduct || 'N/A' }}</td>
            </tr>
            </tbody>
          </table>
        </div>

        <div class="pagination">
          <button @click="goToPage(1)" :disabled="results.currentPage === 1">First</button>
          <button @click="goToPage(results.currentPage - 1)" :disabled="results.currentPage === 1">Previous</button>
          <span class="page-info">Page {{ results.currentPage }} of {{ results.totalPages }}</span>
          <button @click="goToPage(results.currentPage + 1)" :disabled="results.currentPage >= results.totalPages">Next</button>
          <button @click="goToPage(results.totalPages)" :disabled="results.currentPage >= results.totalPages">Last</button>
        </div>
      </div>

      <div v-else class="no-results">No results found for the given criteria.</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../../api'

const queryParams = ref({
  startDate: '',
  endDate: '',
  categoryName: '',
  supplierName: '',
  page: 1,
  pageSize: 20
})

const results = ref(null)
const loading = ref(false)
const error = ref(null)

onMounted(() => {
  // Set default dates
  const today = new Date()
  const lastMonth = new Date()
  lastMonth.setMonth(lastMonth.getMonth() - 1)

  queryParams.value.startDate = lastMonth.toISOString().split('T')[0]
  queryParams.value.endDate = today.toISOString().split('T')[0]
})

async function executeQuery() {
  loading.value = true
  error.value = null
  queryParams.value.page = 1 // Reset to first page

  try {
    const response = await api.post('/api/queries/order-analytics', queryParams.value)
    if (response.data.code === 0) {
      results.value = response.data.data
    } else {
      error.value = response.data.message || 'Query failed'
    }
  } catch (err) {
    console.error('Query error:', err)
    error.value = err.response?.data?.message || 'Network error or unauthorized'
  } finally {
    loading.value = false
  }
}

async function goToPage(page) {
  if (page < 1 || page > results.value.totalPages) return

  loading.value = true
  error.value = null
  queryParams.value.page = page

  try {
    const response = await api.post('/api/queries/order-analytics', queryParams.value)
    if (response.data.code === 0) {
      results.value = response.data.data
    } else {
      error.value = response.data.message || 'Query failed'
    }
  } catch (err) {
    console.error('Query error:', err)
    error.value = err.response?.data?.message || 'Network error or unauthorized'
  } finally {
    loading.value = false
  }
}

function formatNumber(num) {
  if (!num) return '0.00'
  return Number(num).toFixed(2)
}
</script>

<style scoped>
.complex-query-page {
  max-width: 1400px;
}

.page-header {
  background: white;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 24px;
}

.page-header h1 {
  color: #1976d2;
  font-size: 28px;
  margin-bottom: 8px;
}

.subtitle {
  color: #666;
  font-size: 14px;
}

.query-form-card {
  background: white;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 24px;
}

.query-form-card h2 {
  margin-bottom: 16px;
  color: #333;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
}

label {
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
  font-size: 14px;
}

input,
select {
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.2s;
}

input:focus,
select:focus {
  outline: none;
  border-color: #1976d2;
}

.btn-primary {
  padding: 12px 24px;
  background: #1976d2;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary:hover:not(:disabled) {
  background: #1565c0;
}

.btn-primary:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.results-card {
  background: white;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.error-message {
  background: #ffebee;
  color: #c62828;
  padding: 16px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.stats-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #f5f5f5;
  padding: 16px;
  border-radius: 6px;
  border-left: 4px solid #1976d2;
}

.stat-label {
  font-size: 12px;
  color: #666;
  text-transform: uppercase;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #333;
}

.table-container {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 16px;
}

th,
td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
}

th {
  background: #f5f5f5;
  font-weight: 600;
  color: #333;
  font-size: 13px;
  text-transform: uppercase;
}

td {
  color: #555;
  font-size: 14px;
}

tr:hover {
  background: #f9f9f9;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #e0e0e0;
}

.pagination button {
  padding: 8px 16px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.pagination button:hover:not(:disabled) {
  background: #f5f5f5;
  border-color: #1976d2;
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  color: #666;
  font-size: 14px;
}

.no-results {
  text-align: center;
  padding: 40px;
  color: #666;
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  table {
    font-size: 12px;
  }

  th,
  td {
    padding: 8px;
  }
}
</style>
