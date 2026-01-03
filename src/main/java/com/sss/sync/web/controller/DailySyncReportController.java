package com.sss.sync.web.controller;

import com.sss.sync.common.api.ApiResponse;
import com.sss.sync.service.DailySyncReportService;
import com.sss.sync.web.dto.DailySyncStatsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DailySyncReportController {
  
  private final DailySyncReportService dailySyncReportService;
  
  /**
   * API endpoint for daily sync statistics
   * Requires ADMIN role for access
   */
  @GetMapping("/api/reports/daily-sync")
  @ResponseBody
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<DailySyncStatsResponse> getDailySyncStats(
      @RequestParam(defaultValue = "30") int days) {
    log.info("Daily sync stats API called for {} days", days);
    DailySyncStatsResponse response = dailySyncReportService.getDailySyncStats(days);
    return ApiResponse.ok(response);
  }
  
  /**
   * Frontend page for daily sync analytics report
   */
  @GetMapping(value = "/reports/daily-sync", produces = MediaType.TEXT_HTML_VALUE)
  @ResponseBody
  @PreAuthorize("hasRole('ADMIN')")
  public String dailySyncReportPage() {
    return generateDailySyncReportPage();
  }
  
  private String generateDailySyncReportPage() {
    return """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Daily Sync Analytics Report</title>
  <script src="https://cdn.jsdelivr.net/npm/vue@3.4.21/dist/vue.global.min.js" integrity="sha256-gvHfumO9DEJIxWfhfzf9JRCnXsRn8MRh5DjRiXNPH2Y=" crossorigin="anonymous"></script>
  <script src="https://cdn.jsdelivr.net/npm/axios@1.6.7/dist/axios.min.js" integrity="sha256-aS4IHLo61MhvGR4NW4TEyOXWj6Y4VQYkHZ2MQJRS0cU=" crossorigin="anonymous"></script>
  <script src="https://cdn.jsdelivr.net/npm/echarts@5.4.3/dist/echarts.min.js" integrity="sha256-EVZCmhajjLhgTcxlGMGUBtQiYULZCPjt0uNTFEPFTRk=" crossorigin="anonymous"></script>
  <style>
    * {
      margin: 0;
      padding: 0;
      box-sizing: border-box;
    }
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
      background: #f5f7fa;
      padding: 20px;
      line-height: 1.6;
    }
    .container {
      max-width: 1400px;
      margin: 0 auto;
    }
    .header {
      background: white;
      padding: 24px;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      margin-bottom: 24px;
    }
    h1 {
      color: #1976d2;
      font-size: 28px;
      margin-bottom: 8px;
    }
    .subtitle {
      color: #666;
      font-size: 14px;
    }
    .controls {
      background: white;
      padding: 20px;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
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
    .btn {
      padding: 10px 20px;
      border: none;
      border-radius: 4px;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
    }
    .btn-primary {
      background: #1976d2;
      color: white;
    }
    .btn-primary:hover {
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
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      border-left: 4px solid #1976d2;
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
    .chart-container {
      background: white;
      padding: 24px;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
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
    }
    .error {
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
      .controls {
        flex-direction: column;
        align-items: stretch;
      }
      select {
        width: 100%;
      }
    }
  </style>
</head>
<body>
  <div id="app" class="container">
    <div class="header">
      <h1>📈 Daily Sync Analytics Report</h1>
      <p class="subtitle">Visualize sync operations, conflicts, and performance metrics over time</p>
    </div>

    <div class="controls">
      <label for="daysSelect">Time Range:</label>
      <select id="daysSelect" v-model.number="selectedDays">
        <option :value="7">Last 7 Days</option>
        <option :value="14">Last 14 Days</option>
        <option :value="30">Last 30 Days</option>
        <option :value="60">Last 60 Days</option>
        <option :value="90">Last 90 Days</option>
      </select>
      <button class="btn btn-primary" @click="loadData" :disabled="loading">
        {{ loading ? 'Loading...' : 'Refresh' }}
      </button>
    </div>

    <div class="error" v-if="error">{{ error }}</div>

    <div v-if="loading && !statsData" class="loading">
      <p>Loading data...</p>
    </div>

    <div v-if="statsData">
      <div class="stats-summary">
        <div class="stat-card" style="border-left-color: #4caf50;">
          <div class="stat-label">Total Synced Changes</div>
          <div class="stat-value">{{ formatNumber(statsData.summary.totalSyncedChanges) }}</div>
          <div class="stat-change">Avg: {{ formatNumber(statsData.summary.avgDailyChanges) }}/day</div>
        </div>
        <div class="stat-card" style="border-left-color: #ff9800;">
          <div class="stat-label">Conflicts Created</div>
          <div class="stat-value">{{ formatNumber(statsData.summary.totalConflictsCreated) }}</div>
        </div>
        <div class="stat-card" style="border-left-color: #2196f3;">
          <div class="stat-label">Conflicts Resolved</div>
          <div class="stat-value">{{ formatNumber(statsData.summary.totalConflictsResolved) }}</div>
        </div>
        <div class="stat-card" style="border-left-color: #f44336;">
          <div class="stat-label">Failures</div>
          <div class="stat-value">{{ formatNumber(statsData.summary.totalFailures) }}</div>
        </div>
      </div>

      <div class="chart-container">
        <div class="chart-title">Daily Sync Operations</div>
        <div ref="syncChart" class="chart"></div>
      </div>

      <div class="chart-container">
        <div class="chart-title">Conflict Tracking</div>
        <div ref="conflictChart" class="chart"></div>
      </div>
    </div>
  </div>

  <script>
    const { createApp } = Vue;
    
    createApp({
      data() {
        return {
          selectedDays: 30,
          statsData: null,
          loading: false,
          error: null,
          syncChart: null,
          conflictChart: null
        }
      },
      mounted() {
        this.loadData();
      },
      methods: {
        async loadData() {
          this.loading = true;
          this.error = null;
          
          try {
            const response = await axios.get('/api/reports/daily-sync', {
              params: { days: this.selectedDays }
            });
            
            if (response.data.code === 0) {
              this.statsData = response.data.data;
              this.$nextTick(() => {
                this.renderCharts();
              });
            } else {
              this.error = response.data.message || 'Failed to load data';
            }
          } catch (err) {
            console.error('Load error:', err);
            this.error = err.response?.data?.message || 'Network error or unauthorized';
          } finally {
            this.loading = false;
          }
        },
        renderCharts() {
          if (!this.statsData || !this.statsData.dailyStats) return;
          
          const dates = this.statsData.dailyStats.map(d => d.statDate);
          const syncedChanges = this.statsData.dailyStats.map(d => d.syncedChanges);
          const conflictsCreated = this.statsData.dailyStats.map(d => d.conflictsCreated);
          const conflictsResolved = this.statsData.dailyStats.map(d => d.conflictsResolved);
          const failures = this.statsData.dailyStats.map(d => d.failures);
          
          // Sync Operations Chart
          if (this.syncChart) {
            this.syncChart.dispose();
          }
          this.syncChart = echarts.init(this.$refs.syncChart);
          
          this.syncChart.setOption({
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
          });
          
          // Conflict Chart
          if (this.conflictChart) {
            this.conflictChart.dispose();
          }
          this.conflictChart = echarts.init(this.$refs.conflictChart);
          
          this.conflictChart.setOption({
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
          });
          
          // Handle window resize
          window.addEventListener('resize', () => {
            this.syncChart && this.syncChart.resize();
            this.conflictChart && this.conflictChart.resize();
          });
        },
        formatNumber(num) {
          if (num == null) return '0';
          return Number(num).toLocaleString();
        }
      },
      beforeUnmount() {
        if (this.syncChart) {
          this.syncChart.dispose();
        }
        if (this.conflictChart) {
          this.conflictChart.dispose();
        }
      }
    }).mount('#app');
  </script>
</body>
</html>
""";
  }
}