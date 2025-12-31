package com.sss.sync.web.controller;

import com.sss.sync.common.api.ApiResponse;
import com.sss.sync.common.exception.BizException;
import com.sss.sync.domain.entity.ConflictRecord;
import com.sss.sync.infra.mapper.mysql.MysqlConflictRecordMapper;
import com.sss.sync.service.conflict.ConflictLinkTokenService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ConflictViewController {

  private final ConflictLinkTokenService tokenService;
  private final MysqlConflictRecordMapper conflictRecordMapper;

  @GetMapping("/api/conflicts/view")
  public ApiResponse<ConflictRecord> view(@RequestParam("token") String token) {
    Claims c = tokenService.parse(token);
    Object conflictIdObj = c.get("conflictId");
    if (conflictIdObj == null) throw BizException.of(401, "INVALID_TOKEN");

    long conflictId = Long.parseLong(String.valueOf(conflictIdObj));

    ConflictRecord record = conflictRecordMapper.selectById(conflictId);

    if (record == null) throw BizException.of(404, "CONFLICT_NOT_FOUND");
    return ApiResponse.ok(record);
  }

  // 简单 HTML 页面（浏览器直接打开查看，后续第7包换成 Vue）
  @GetMapping(value = "/conflicts/view", produces = "text/html; charset=UTF-8")
  public String viewHtml(@RequestParam("token") String token) {
    // 复用 API 逻辑：只验证 token 是否有效
    Claims c = tokenService.parse(token);
    Object conflictIdObj = c.get("conflictId");
    if (conflictIdObj == null) throw BizException.of(401, "INVALID_TOKEN");

    long conflictId = Long.parseLong(String.valueOf(conflictIdObj));

    return """
      <!doctype html>
      <html lang="zh-CN">
      <head>
        <meta charset="utf-8"/>
        <meta name="viewport" content="width=device-width, initial-scale=1"/>
        <title>同步冲突详情</title>
        <style>
          body { font-family: Arial, sans-serif; padding: 12px; }
          pre { white-space: pre-wrap; word-break: break-all; background: #f5f5f5; padding: 12px; border-radius: 6px; }
          .box { max-width: 980px; margin: 0 auto; }
        </style>
      </head>
      <body>
        <div class="box">
          <h2>同步冲突详情（conflictId=%d）</h2>
          <p>本页面通过链接 token 鉴权，PC/手机浏览器均可打开。</p>
          <p>接口：<code>/api/conflicts/view?token=...</code></p>
          <hr/>
          <div id="content">加载中...</div>
        </div>
        <script>
          fetch('/api/conflicts/view?token=%s')
            .then(r => r.json())
            .then(res => {
              if(res.code !== 0){ document.getElementById('content').innerText = '加载失败：' + res.message; return; }
              const d = res.data;
              document.getElementById('content').innerHTML =
                '<p><b>table</b>: ' + d.tableName + ' <b>pk</b>: ' + d.pkValue + '</p>'
                + '<p><b>source</b>: ' + d.sourceDb + ' <b>target</b>: ' + d.targetDb + '</p>'
                + '<p><b>status</b>: ' + d.status + '</p>'
                + '<h3>source_payload_json</h3><pre>' + d.sourcePayloadJson + '</pre>'
                + '<h3>target_payload_json</h3><pre>' + d.targetPayloadJson + '</pre>';
            })
            .catch(err => document.getElementById('content').innerText = '加载异常：' + err);
        </script>
      </body>
      </html>
      """.formatted(conflictId, token);
  }
}