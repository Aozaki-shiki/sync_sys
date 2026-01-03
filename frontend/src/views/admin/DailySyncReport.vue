<template>
  <div class="daily-sync-page">
    <div class="page-header">
      <h1>📈 Daily Sync Analytics Report</h1>
      <p class="subtitle">Visualize sync operations, conflicts, and performance metrics over time</p>
    </div>

    <div class="controls-card">
      <label for="daysSelect">Time Range:</label>
      <select id="daysSelect" v-model.number="selectedDays">
        <option :value="7">Last 7 Days</option>
        <option :value="14">Last 14 Days</option>
        <option :value="30">Last 30 Days</option>
        <option :value="60">Last 60 Days</option>
        <option :value="90">Last 90 Days</option>
      </select>
      <button class="btn-primary" @click="loadData" :disabled="loading">
        {{ loading ? 'Loading...' : 'Refresh' }}
      </button>
    </div>

    <div v-if="error" class="error-message">{{ error }}</div>

    <div v-if="loading && !statsData" class="loading">
      <p>Loading data...</p>
    </div>

    <div v-if="statsData">
      <div class="stats-summary">
        <div class="stat-card green">
          <div class="stat-label">Total Synced Changes</div>
          <div class="stat-value">{{ formatNumber(statsData.summary.totalSyncedChanges) }}</div>
          <div class="stat-change">Avg: {{ formatNumber(statsData.summary.avgDailyChanges) }}/day</div>
        </div>
        <div class="stat-card orange">
          <div class="stat-label">Conflicts Created</div>
          <div class="stat-value">{{ formatNumber(statsData.summary.totalConflictsCreated) }}</div>
        </div>
        <div class="stat-card blue">
          <div class="stat-label">Conflicts Resolved</div>
          <div class="stat-value">{{ formatNumber(statsData.summary.totalConflictsResolved) }}</div>
        </div>
        <div class="stat-card red">
          <div class="stat-label">Failures</div>
          <div class="stat-value">{{ formatNumber(statsData.summary.totalFailures) }}</div>
        </div>
      </div>

      <div class="chart-card">
        <div class="chart-title">Daily Sync Operations</div>
        <div ref="syncChart" class="chart"></div>
      </div>

      <div class="chart-card">
        <div class="chart-title">Conflict Tracking</div>
        <div ref="conflictChart" class="chart"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import api from '../../api'

const selectedDays = ref(30)
const statsData = ref(null)
const loading = ref(false)
const error = ref(null)

const syncChart = ref(null)
const conflictChart = ref(null)

let syncChartInstance = null
let conflictChartInstance = null

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (syncChartInstance) {
    syncChartInstance.dispose()
  }
  if (conflictChartInstance) {
    conflictChartInstance.dispose()
  }
  window.removeEventListener('resize', handleResize)
})

async function loadData() {
  loading.value = true
  error.value = null

  try {
    const response = await api.get('/api/reports/daily-sync', {
      params: { days: selectedDays.value }
    })

    if (response.data.code === 0) {
      statsData.value = response.data.data
      await nextTick()
      renderCharts()
    } else {
      error.value = response.data.message || 'Failed to load data'
    }
  } catch (err) {
    console.error('Load error:', err)
    error.value = err.response?.data?.message || 'Network error or unauthorized'
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  if (!statsData.value || !statsData.value.dailyStats) return

  const dates = statsData.value.dailyStats.map(d => d.statDate)
  const syncedChanges = statsData.value.dailyStats.map(d => d.syncedChanges)
  const conflictsCreated = statsData.value.dailyStats.map(d => d.conflictsCreated)
  const conflictsResolved = statsData.value.dailyStats.map(d => d.conflictsResolved)
  const failures = statsData.value.dailyStats.map(d => d.failures)

  // Sync Operations Chart
  if (syncChartInstance) {
    syncChartInstance.dispose()
  }
  syncChartInstance = echarts.init(syncChart.value)

  syncChartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    legend: {
      data: ['Synced Changes', 'Failures']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: { rotate: 45 }
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: 'Synced Changes',
        type: 'bar',
        data: syncedChanges,
        itemStyle: { color: '#4caf50' }
      },
      {
        name: 'Failures',
        type: 'bar',
        data: failures,
        itemStyle: { color: '#f44336' }
      }
    ]
  })

  // Conflict Chart
  if (conflictChartInstance) {
    conflictChartInstance.dispose()
  }
  conflictChartInstance = echarts.init(conflictChart.value)

  conflictChartInstance.setOption({
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['Conflicts Created', 'Conflicts Resolved']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: { rotate: 45 }
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: 'Conflicts Created',
        type: 'line',
        data: conflictsCreated,
        smooth: true,
        itemStyle: { color: '#ff9800' },
        areaStyle: { opacity: 0.3 }
      },
      {
        name: 'Conflicts Resolved',
        type: 'line',
        data: conflictsResolved,
        smooth: true,
        itemStyle: { color: '#2196f3' },
        areaStyle: { opacity: 0.3 }
      }
    ]
  })
}

function handleResize() {
  if (syncChartInstance) {
    syncChartInstance.resize()
  }
  if (conflictChartInstance) {
    conflictChartInstance.resize()
  }
}

function formatNumber(num) {
  if (num == null) return '0'
  return Number(num).toLocaleString()
}
</script>

<style scoped>
.daily-sync-page {
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

.controls-card {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 24px;
  display: flex;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

label {
  font-weight: 600;
  color: #333;
  font-size: 14px;
}

select {
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  min-width: 150px;
}

.btn-primary {
  padding: 10px 20px;
  background: #1976d2;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 14px;
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

.stats-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-left: 4px solid #1976d2;
}

.stat-card.green {
  border-left-color: #4caf50;
}

.stat-card.orange {
  border-left-color: #ff9800;
}

.stat-card.blue {
  border-left-color: #2196f3;
}

.stat-card.red {
  border-left-color: #f44336;
}

.stat-label {
  font-size: 13px;
  color: #666;
  text-transform: uppercase;
  margin-bottom: 8px;
  font-weight: 600;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #333;
}

.stat-change {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.chart-card {
  background: white;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 24px;
}

.chart-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

.chart {
  width: 100%;
  height: 400px;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #666;
  background: white;
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

@media (max-width: 768px) {
  .stats-summary {
    grid-template-columns: 1fr;
  }

  .chart {
    height: 300px;
  }

  .controls-card {
    flex-direction: column;
    align-items: stretch;
  }

  select {
    width: 100%;
  }
}
</style>
