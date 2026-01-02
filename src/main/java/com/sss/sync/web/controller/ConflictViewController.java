package com.sss.sync.web.controller;

import com.sss.sync.domain.sync.ConflictRecordRow;
import com.sss.sync.infra.mapper.mysql.MysqlSyncSupportMapper;
import com.sss.sync.service.conflict.ConflictLinkTokenService;
import com.sss.sync.service.conflict.ConflictResolutionService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ConflictViewController {

  private final ConflictLinkTokenService tokenService;
  private final MysqlSyncSupportMapper syncSupportMapper;
  private final ConflictResolutionService resolutionService;
  
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  @GetMapping(value = "/conflicts/view", produces = MediaType.TEXT_HTML_VALUE)
  @ResponseBody
  public String viewConflict(@RequestParam("token") String token) {
    try {
      // Parse and validate token
      Claims claims = tokenService.parse(token);
      Object conflictIdObj = claims.get("conflictId");
      
      if (conflictIdObj == null) {
        return errorPage("Invalid token: missing conflict ID");
      }
      
      long conflictId;
      if (conflictIdObj instanceof Number) {
        conflictId = ((Number) conflictIdObj).longValue();
      } else {
        conflictId = Long.parseLong(String.valueOf(conflictIdObj));
      }
      
      // Fetch conflict record
      ConflictRecordRow conflict = syncSupportMapper.getConflictById(conflictId);
      
      if (conflict == null) {
        return errorPage("Conflict record not found: ID " + conflictId);
      }
      
      // Generate HTML page
      return generateConflictPage(conflict);
      
    } catch (io.jsonwebtoken.ExpiredJwtException e) {
      log.warn("Expired token accessed: {}", e.getMessage());
      return errorPage("Token has expired. Please request a new link from the administrator.");
    } catch (io.jsonwebtoken.JwtException e) {
      log.warn("Invalid token: {}", e.getMessage());
      return errorPage("Invalid or malformed token");
    } catch (Exception e) {
      log.error("Error viewing conflict", e);
      return errorPage("An internal error occurred. Please contact the administrator.");
    }
  }
  
  private String generateConflictPage(ConflictRecordRow conflict) {
    String createdAtStr = conflict.getCreatedAt() != null 
      ? conflict.getCreatedAt().format(FORMATTER) 
      : "N/A";
    String sourceUpdatedAtStr = conflict.getSourceUpdatedAt() != null 
      ? conflict.getSourceUpdatedAt().format(FORMATTER) 
      : "N/A";
    String targetUpdatedAtStr = conflict.getTargetUpdatedAt() != null 
      ? conflict.getTargetUpdatedAt().format(FORMATTER) 
      : "N/A";
    String resolvedAtStr = conflict.getResolvedAt() != null 
      ? conflict.getResolvedAt().format(FORMATTER) 
      : "N/A";
    
    String sourceJson = conflict.getSourcePayloadJson() != null ? conflict.getSourcePayloadJson() : "{}";
    String targetJson = conflict.getTargetPayloadJson() != null ? conflict.getTargetPayloadJson() : "{}";
    
    return """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>数据同步冲突详情</title>
  <style>
    * {
      margin: 0;
      padding: 0;
      box-sizing: border-box;
    }
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
      line-height: 1.6;
      color: #333;
      background: #f5f5f5;
      padding: 20px;
    }
    .container {
      max-width: 1200px;
      margin: 0 auto;
      background: white;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      padding: 30px;
    }
    h1 {
      color: #d32f2f;
      margin-bottom: 10px;
      font-size: 24px;
    }
    .meta {
      color: #666;
      font-size: 14px;
      margin-bottom: 30px;
      padding-bottom: 15px;
      border-bottom: 2px solid #f0f0f0;
    }
    .section {
      margin-bottom: 25px;
    }
    .section-title {
      font-size: 18px;
      font-weight: 600;
      color: #444;
      margin-bottom: 12px;
      padding-bottom: 8px;
      border-bottom: 1px solid #e0e0e0;
    }
    .info-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 15px;
      margin-bottom: 20px;
    }
    .info-item {
      background: #f9f9f9;
      padding: 12px;
      border-radius: 4px;
    }
    .info-label {
      font-weight: 600;
      color: #555;
      font-size: 13px;
      margin-bottom: 4px;
    }
    .info-value {
      color: #333;
      font-size: 14px;
    }
    .status {
      display: inline-block;
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 600;
    }
    .status-open {
      background: #ffebee;
      color: #c62828;
    }
    .status-resolved {
      background: #e8f5e9;
      color: #2e7d32;
    }
    .json-block {
      background: #f5f5f5;
      border: 1px solid #e0e0e0;
      border-radius: 4px;
      padding: 15px;
      overflow-x: auto;
      font-family: 'Courier New', Consolas, monospace;
      font-size: 13px;
      line-height: 1.4;
      white-space: pre-wrap;
      word-wrap: break-word;
    }
    .comparison {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;
      margin-top: 15px;
    }
    .comparison-item {
      background: #fafafa;
      padding: 15px;
      border-radius: 4px;
      border: 1px solid #e0e0e0;
    }
    .comparison-title {
      font-weight: 600;
      color: #1976d2;
      margin-bottom: 10px;
      font-size: 15px;
    }
    @media (max-width: 768px) {
      .container {
        padding: 20px;
      }
      h1 {
        font-size: 20px;
      }
      .comparison {
        grid-template-columns: 1fr;
      }
      .info-grid {
        grid-template-columns: 1fr;
      }
    }
  </style>
</head>
<body>
  <div class="container">
    <h1>🔴 数据同步冲突详情</h1>
    <div class="meta">
      Conflict ID: <strong>%s</strong> | 
      Created: <strong>%s</strong> | 
      Status: <span class="status %s">%s</span>
    </div>
    
    <div class="section">
      <div class="section-title">基本信息</div>
      <div class="info-grid">
        <div class="info-item">
          <div class="info-label">Table Name</div>
          <div class="info-value">%s</div>
        </div>
        <div class="info-item">
          <div class="info-label">Primary Key</div>
          <div class="info-value">%s</div>
        </div>
        <div class="info-item">
          <div class="info-label">Source DB</div>
          <div class="info-value">%s</div>
        </div>
        <div class="info-item">
          <div class="info-label">Target DB</div>
          <div class="info-value">%s</div>
        </div>
      </div>
    </div>
    
    <div class="section">
      <div class="section-title">版本信息</div>
      <div class="info-grid">
        <div class="info-item">
          <div class="info-label">Source Version</div>
          <div class="info-value">%s</div>
        </div>
        <div class="info-item">
          <div class="info-label">Target Version</div>
          <div class="info-value">%s</div>
        </div>
        <div class="info-item">
          <div class="info-label">Source Updated At</div>
          <div class="info-value">%s</div>
        </div>
        <div class="info-item">
          <div class="info-label">Target Updated At</div>
          <div class="info-value">%s</div>
        </div>
      </div>
    </div>
    
    <div class="section">
      <div class="section-title">数据对比</div>
      <div class="comparison">
        <div class="comparison-item">
          <div class="comparison-title">📤 Source Payload (from %s)</div>
          <div class="json-block">%s</div>
        </div>
        <div class="comparison-item">
          <div class="comparison-title">📥 Target Payload (from %s)</div>
          <div class="json-block">%s</div>
        </div>
      </div>
    </div>
    
    %s
  </div>
</body>
</html>
""".formatted(
      conflict.getConflictId(),
      createdAtStr,
      "OPEN".equals(conflict.getStatus()) ? "status-open" : "status-resolved",
      conflict.getStatus(),
      escapeHtml(conflict.getTableName()),
      escapeHtml(conflict.getPkValue()),
      escapeHtml(conflict.getSourceDb()),
      escapeHtml(conflict.getTargetDb()),
      String.valueOf(conflict.getSourceVersion()),
      String.valueOf(conflict.getTargetVersion()),
      sourceUpdatedAtStr,
      targetUpdatedAtStr,
      escapeHtml(conflict.getSourceDb()),
      escapeHtml(formatJson(sourceJson)),
      escapeHtml(conflict.getTargetDb()),
      escapeHtml(formatJson(targetJson)),
      generateResolutionSection(conflict, resolvedAtStr)
    );
  }
  
  private String generateResolutionSection(ConflictRecordRow conflict, String resolvedAtStr) {
    if ("RESOLVED".equals(conflict.getStatus())) {
      return """
      <div class="section">
        <div class="section-title">解决信息</div>
        <div class="info-grid">
          <div class="info-item">
            <div class="info-label">Resolved By</div>
            <div class="info-value">%s</div>
          </div>
          <div class="info-item">
            <div class="info-label">Resolved At</div>
            <div class="info-value">%s</div>
          </div>
          <div class="info-item">
            <div class="info-label">Resolution</div>
            <div class="info-value">%s</div>
          </div>
        </div>
      </div>
      """.formatted(
        escapeHtml(conflict.getResolvedBy()),
        resolvedAtStr,
        escapeHtml(conflict.getResolution())
      );
    } else {
      // Show resolution form for OPEN conflicts
      String currentUrl = "/conflicts/view?token=";
      return """
      <div class="section">
        <div class="section-title">冲突解决</div>
        <form id="resolutionForm" style="padding: 15px; background: #f9f9f9; border-radius: 4px;">
          <div style="margin-bottom: 15px;">
            <label style="display: block; font-weight: 600; margin-bottom: 10px; color: #555;">
              选择权威数据源 (Authoritative Database):
            </label>
            <div style="display: flex; flex-direction: column; gap: 10px;">
              <label style="display: flex; align-items: center; cursor: pointer; padding: 8px; border-radius: 4px; transition: background 0.2s;">
                <input type="radio" name="authoritativeDb" value="MYSQL" required style="margin-right: 8px; cursor: pointer;">
                <span>MYSQL</span>
              </label>
              <label style="display: flex; align-items: center; cursor: pointer; padding: 8px; border-radius: 4px; transition: background 0.2s;">
                <input type="radio" name="authoritativeDb" value="POSTGRES" required style="margin-right: 8px; cursor: pointer;">
                <span>POSTGRES</span>
              </label>
              <label style="display: flex; align-items: center; cursor: pointer; padding: 8px; border-radius: 4px; transition: background 0.2s;">
                <input type="radio" name="authoritativeDb" value="SQLSERVER" required style="margin-right: 8px; cursor: pointer;">
                <span>SQLSERVER</span>
              </label>
            </div>
          </div>
          <button type="submit" style="background: #1976d2; color: white; border: none; padding: 12px 24px; border-radius: 4px; font-size: 16px; font-weight: 600; cursor: pointer; width: 100%;">
            解决冲突 (Resolve Conflict)
          </button>
          <div id="message" style="margin-top: 15px; padding: 10px; border-radius: 4px; display: none;"></div>
        </form>
        <script>
          document.getElementById('resolutionForm').addEventListener('submit', function(e) {
            e.preventDefault();
            const authoritativeDb = document.querySelector('input[name="authoritativeDb"]:checked');
            if (!authoritativeDb) {
              showMessage('请选择一个权威数据源', 'error');
              return;
            }
            
            const messageDiv = document.getElementById('message');
            messageDiv.textContent = '正在处理...';
            messageDiv.style.display = 'block';
            messageDiv.style.background = '#e3f2fd';
            messageDiv.style.color = '#1976d2';
            
            const token = new URLSearchParams(window.location.search).get('token');
            fetch('/conflicts/resolve?token=' + encodeURIComponent(token), {
              method: 'POST',
              headers: {
                'Content-Type': 'application/json'
              },
              body: JSON.stringify({
                authoritativeDb: authoritativeDb.value
              })
            })
            .then(response => response.json())
            .then(data => {
              if (data.success) {
                showMessage('冲突已成功解决！页面将在3秒后刷新...', 'success');
                setTimeout(() => window.location.reload(), 3000);
              } else {
                showMessage('错误: ' + (data.message || '未知错误'), 'error');
              }
            })
            .catch(error => {
              showMessage('网络错误: ' + error.message, 'error');
            });
          });
          
          function showMessage(text, type) {
            const messageDiv = document.getElementById('message');
            messageDiv.textContent = text;
            messageDiv.style.display = 'block';
            if (type === 'success') {
              messageDiv.style.background = '#e8f5e9';
              messageDiv.style.color = '#2e7d32';
            } else {
              messageDiv.style.background = '#ffebee';
              messageDiv.style.color = '#c62828';
            }
          }
        </script>
      </div>
      """;
    }
  }
  
  private String formatJson(String json) {
    if (json == null || json.trim().isEmpty() || "{}".equals(json.trim())) {
      return "{}";
    }
    // Use Jackson ObjectMapper for proper JSON formatting
    try {
      com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
      Object jsonObj = mapper.readValue(json, Object.class);
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObj);
    } catch (Exception e) {
      // If JSON parsing fails, return original (will be escaped by caller)
      log.debug("Failed to format JSON, using original: {}", e.getMessage());
      return json;
    }
  }
  
  private String errorPage(String message) {
    return """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>错误</title>
  <style>
    * {
      margin: 0;
      padding: 0;
      box-sizing: border-box;
    }
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      background: #f5f5f5;
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      padding: 20px;
    }
    .error-container {
      background: white;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      padding: 40px;
      max-width: 500px;
      text-align: center;
    }
    .error-icon {
      font-size: 48px;
      margin-bottom: 20px;
    }
    h1 {
      color: #d32f2f;
      font-size: 24px;
      margin-bottom: 15px;
    }
    p {
      color: #666;
      line-height: 1.6;
      font-size: 16px;
    }
  </style>
</head>
<body>
  <div class="error-container">
    <div class="error-icon">⚠️</div>
    <h1>访问错误</h1>
    <p>%s</p>
  </div>
</body>
</html>
""".formatted(escapeHtml(message));
  }
  
  private String escapeHtml(String text) {
    if (text == null) {
      return "";
    }
    return text
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&#x27;");
  }

  @PostMapping("/conflicts/resolve")
  @ResponseBody
  public Map<String, Object> resolveConflict(
      @RequestParam("token") String token,
      @RequestBody Map<String, String> request) {
    try {
      // Parse and validate token
      Claims claims = tokenService.parse(token);
      Object conflictIdObj = claims.get("conflictId");
      
      if (conflictIdObj == null) {
        return Map.of("success", false, "message", "Invalid token: missing conflict ID");
      }
      
      long conflictId;
      if (conflictIdObj instanceof Number) {
        conflictId = ((Number) conflictIdObj).longValue();
      } else {
        conflictId = Long.parseLong(String.valueOf(conflictIdObj));
      }
      
      String adminUsername = (String) claims.get("admin");
      if (adminUsername == null || adminUsername.trim().isEmpty()) {
        return Map.of("success", false, "message", "Invalid token: missing admin username");
      }
      
      String authoritativeDb = request.get("authoritativeDb");
      if (authoritativeDb == null || authoritativeDb.trim().isEmpty()) {
        return Map.of("success", false, "message", "Missing authoritativeDb parameter");
      }
      
      // Perform conflict resolution
      resolutionService.resolveConflict(conflictId, authoritativeDb, adminUsername);
      
      return Map.of("success", true, "message", "Conflict resolved successfully");
      
    } catch (io.jsonwebtoken.ExpiredJwtException e) {
      log.warn("Expired token for conflict resolution: {}", e.getMessage());
      return Map.of("success", false, "message", "Token has expired. Please request a new link.");
    } catch (io.jsonwebtoken.JwtException e) {
      log.warn("Invalid token for conflict resolution: {}", e.getMessage());
      return Map.of("success", false, "message", "Invalid or malformed token");
    } catch (IllegalArgumentException | IllegalStateException e) {
      log.warn("Conflict resolution validation error: {}", e.getMessage());
      return Map.of("success", false, "message", e.getMessage());
    } catch (Exception e) {
      log.error("Error resolving conflict", e);
      return Map.of("success", false, "message", "An internal error occurred: " + e.getMessage());
    }
  }
}