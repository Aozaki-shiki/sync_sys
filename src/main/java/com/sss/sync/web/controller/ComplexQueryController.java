package com.sss.sync.web.controller;

import com.sss.sync.common.api.ApiResponse;
import com.sss.sync.service.ComplexQueryService;
import com.sss.sync.web.dto.ComplexQueryRequest;
import com.sss.sync.web.dto.ComplexQueryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ComplexQueryController {

  private final ComplexQueryService complexQueryService;

  /**
   * API endpoint for complex order analytics query
   * Requires ADMIN role for access
   */
  @PostMapping("/api/queries/order-analytics")
  @ResponseBody
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<ComplexQueryResponse> queryOrderAnalytics(@Valid @RequestBody ComplexQueryRequest request) {
    log.info("Complex query API called by admin");
    ComplexQueryResponse response = complexQueryService.queryOrderAnalytics(request);
    return ApiResponse.ok(response);
  }

  /**
   * Frontend page for complex query interface
   */
  @GetMapping(value = "/queries/complex", produces = MediaType.TEXT_HTML_VALUE)
  @ResponseBody
  @PreAuthorize("hasRole('ADMIN')")
  public String complexQueryPage() {
    return generateComplexQueryPage();
  }

  private String generateComplexQueryPage() {
    return """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Complex Order Analytics Query</title>
  <script src="https://cdn.jsdelivr.net/npm/vue@3.4.21/dist/vue.global.min.js" integrity="sha256-gvHfumO9DEJIxWfhfzf9JRCnXsRn8MRh5DjRiXNPH2Y=" crossorigin="anonymous"></script>
  <script src="https://cdn.jsdelivr.net/npm/axios@1.6.7/dist/axios.min.js" integrity="sha256-aS4IHLo61MhvGR4NW4TEyOXWj6Y4VQYkHZ2MQJRS0cU=" crossorigin="anonymous"></script>
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
    .query-form {
      background: white;
      padding: 24px;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      margin-bottom: 24px;
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
    input, select {
      padding: 10px 12px;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 14px;
      transition: border-color 0.2s;
    }
    input:focus, select:focus {
      outline: none;
      border-color: #1976d2;
    }
    .btn {
      padding: 12px 24px;
      border: none;
      border-radius: 4px;
      font-size: 16px;
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
    .results-section {
      background: white;
      padding: 24px;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
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
    table {
      width: 100%;
      border-collapse: collapse;
      margin-top: 16px;
    }
    th, td {
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
    @media (max-width: 768px) {
      .form-grid {
        grid-template-columns: 1fr;
      }
      table {
        font-size: 12px;
      }
      th, td {
        padding: 8px;
      }
    }
  </style>
</head>
<body>
  <div id="app" class="container">
    <div class="header">
      <h1>📊 Complex Order Analytics Query</h1>
      <p class="subtitle">Multi-table join with aggregations and nested subqueries for business insights</p>
    </div>
    <div class="query-form">
      <h2 style="margin-bottom: 16px; color: #333;">Query Parameters</h2>
      <div class="form-grid">
        <div class="form-group">
          <label for="startDate">Start Date *</label>
          <input type="date" id="startDate" v-model="queryParams.startDate" required>
        </div>
        <div class="form-group">
          <label for="endDate">End Date *</label>
          <input type="date" id="endDate" v-model="queryParams.endDate" required>
        </div>
        <div class="form-group">
          <label for="categoryName">Category Name (Optional)</label>
          <input type="text" id="categoryName" v-model="queryParams.categoryName" placeholder="e.g., 食品">
        </div>
        <div class="form-group">
          <label for="supplierName">Supplier Name (Optional)</label>
          <input type="text" id="supplierName" v-model="queryParams.supplierName" placeholder="e.g., 供应商A">
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
      <button class="btn btn-primary" @click="executeQuery" :disabled="loading">
        {{ loading ? 'Querying...' : 'Execute Query' }}
      </button>
    </div>
    <div class="results-section" v-if="error || results">
      <div class="error" v-if="error">{{ error }}</div>
      
      <div v-if="results">
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
          <div class="pagination">
            <button @click="goToPage(1)" :disabled="results.currentPage === 1">First</button>
            <button @click="goToPage(results.currentPage - 1)" :disabled="results.currentPage === 1">Previous</button>
            <span class="page-info">Page {{ results.currentPage }} of {{ results.totalPages }}</span>
            <button @click="goToPage(results.currentPage + 1)" :disabled="results.currentPage >= results.totalPages">Next</button>
            <button @click="goToPage(results.totalPages)" :disabled="results.currentPage >= results.totalPages">Last</button>
          </div>
        </div>
        <div v-else class="loading">No results found for the given criteria.</div>
      </div>
    </div>
  </div>
  <script>
    const { createApp } = Vue;
    
    createApp({
      data() {
        const today = new Date();
        const lastMonth = new Date();
        lastMonth.setMonth(lastMonth.getMonth() - 1);
        
        return {
          queryParams: {
            startDate: lastMonth.toISOString().split('T')[0],
            endDate: today.toISOString().split('T')[0],
            categoryName: '',
            supplierName: '',
            page: 1,
            pageSize: 20
          },
          results: null,
          loading: false,
          error: null
        }
      },
      methods: {
        async executeQuery() {
          this.loading = true;
          this.error = null;
          this.queryParams.page = 1; // Reset to first page
          
          try {
            const response = await axios.post('/api/queries/order-analytics', this.queryParams);
            if (response.data.code === 0) {
              this.results = response.data.data;
            } else {
              this.error = response.data.message || 'Query failed';
            }
          } catch (err) {
            console.error('Query error:', err);
            this.error = err.response?.data?.message || 'Network error or unauthorized';
          } finally {
            this.loading = false;
          }
        },
        async goToPage(page) {
          if (page < 1 || page > this.results.totalPages) return;
          
          this.loading = true;
          this.error = null;
          this.queryParams.page = page;
          
          try {
            const response = await axios.post('/api/queries/order-analytics', this.queryParams);
            if (response.data.code === 0) {
              this.results = response.data.data;
            } else {
              this.error = response.data.message || 'Query failed';
            }
          } catch (err) {
            console.error('Query error:', err);
            this.error = err.response?.data?.message || 'Network error or unauthorized';
          } finally {
            this.loading = false;
          }
        },
        formatNumber(num) {
          if (!num) return '0.00';
          return Number(num).toFixed(2);
        }
      }
    }).mount('#app');
  </script>
</body>
</html>
""";
  }
}