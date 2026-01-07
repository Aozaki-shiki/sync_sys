# 数据库系统实践实验报告：多数据库增量同步与冲突处理系统

> 项目：`sync_sys`（Multi-DB sync system demo）  
> 日期：2026 年 1 月 4 日  
> 作者：李隆业  
> 学号：8208230202 
> 班级：计科2309  

## 1. 实验目的

1. 熟悉关系数据库（MySQL / PostgreSQL / SQL Server）的建库建表、约束、索引、视图等基础对象。
2. 掌握触发器（Trigger）记录数据变更日志的方案，实现**增量同步**（CDC 简化版）。
3. 设计并实现多库数据一致性维护的同步引擎，支持批量拉取变更、跨库 upsert、检查点（checkpoint）推进。
4. 实现冲突检测与人工仲裁：当多库并发写入同一业务主键时，能够记录冲突、通知管理员、并在管理员选择“权威数据源”后将结果回写所有库。
5. 通过前后端与 Docker Compose 完成一套可运行的综合实践项目。

## 2. 实验环境

### 2.1 软件与版本（以项目配置为准）

- 操作系统：Windows
- JDK：17（见 [pom.xml](pom.xml)）
- 后端框架：Spring Boot 3.2.x
- ORM：MyBatis-Plus（Spring Boot 3 starter）
- 鉴权：Spring Security + JWT
- 前端：Vue 3 + Vite + Pinia + Axios + ECharts（见 [frontend/package.json](frontend/package.json)）
- 数据库：
  - MySQL 8.0（Docker Compose）
  - PostgreSQL 15（Docker Compose）
  - SQL Server 2019（Docker Compose）
- 容器编排：Docker Compose（见 [docker-compose.yml](docker-compose.yml)）

### 2.2 目录结构（关键部分）

- 后端：`src/main/java/com/sss/sync/...`
- 前端：`frontend/src/...`
- 数据库脚本：`db/mysql`、`db/postgres`、`db/sqlserver`

## 3. 需求分析与功能概述

### 3.1 业务场景

系统模拟“商品与订单”业务：管理员维护商品信息，用户下单。系统同时连接多个数据库实例（MySQL、PostgreSQL、SQL Server），允许在 MySQL 或 PostgreSQL 上进行业务写入，并将变更增量同步到另外两个库，从而在多数据库之间保持数据一致。

### 3.2 功能列表

1. **用户登录（JWT）**：`/api/auth/login` 登录后返回 token，并在前端请求中自动带上 `Authorization: Bearer ...`。
2. **用户下单**：`/api/orders/place`，支持选择写入库（MySQL / PostgreSQL），库存不足则失败回滚。
3. **管理员商品管理**：
   - 列表：`GET /api/admin/products?search=`
   - 更新：`PUT /api/admin/products/{productId}`（修改名称/价格/库存，支持选择写入库）
4. **复杂查询（多表连接 + 聚合 + 分页）**：`POST /api/queries/order-analytics`
5. **每日同步报表**：`GET /api/reports/daily-sync?days=30`，前端使用 ECharts 可视化。
6. **冲突检测与处理**：
   - 自动记录冲突并邮件通知管理员（token 链接）
   - 管理员通过 token 查看冲突详情页：`GET /conflicts/view?token=...`
   - 管理员选择权威库并执行解决：`POST /conflicts/resolve?token=...`

## 4. 总体设计

### 4.1 系统架构

- **数据库层（3 套 DB）**：业务表 + 变更日志表 `change_log` + 冲突表 `conflict_record` + 报表表 `sync_run_daily` + 检查点表 `sync_checkpoint`。
- **后端服务层**：
  - 业务写入服务（下单、更新商品）
  - 同步引擎：周期性拉取 `change_log`，跨库 upsert
  - 冲突服务：冲突记录、token 链接、冲突解决传播
  - 分析/复杂查询：从“只读数据源”执行多表分析
- **前端展示层**：管理员后台（复杂查询、报表、冲突入口、商品管理）+ 用户下单页面。
- **部署层**：Docker Compose 统一启动三库、后端与前端，Nginx 反向代理 `/api` 与 `/conflicts`。

### 4.2 增量同步方案

核心思想：各数据库通过触发器把业务表的 INSERT/UPDATE/DELETE 写入统一结构的 `change_log`（包含：表名、操作类型、主键、行版本、更新时间、整行 JSON 快照）。同步引擎读取增量日志并跨库写入。

- MySQL：AFTER INSERT/UPDATE/DELETE Trigger，使用 `JSON_OBJECT` 生成快照。
- PostgreSQL：plpgsql trigger function，使用 `to_jsonb(NEW/OLD)`。
- SQL Server：AFTER Trigger，使用 `FOR JSON PATH`。

为避免“同步写入导致触发器再次记录日志、形成回环”，三库均提供跳过日志的会话开关：

- MySQL：会话变量 `@sss_skip_changelog = 1`
- PostgreSQL：`set_config('sss.skip_changelog','1', true)`（脚本中用 `current_setting(..., true)` 检测）
- SQL Server：`SESSION_CONTEXT(N'sss_skip_changelog') = 1`

（备注：具体在写入服务中设置/清理。）

### 4.3 冲突处理设计

- 冲突判定：当目标库已存在同主键记录，且目标 `version >= source version` 时认定为冲突（详见后端同步引擎）。
- 冲突记录：写入 `conflict_record`（本项目统一落在 MySQL 的支撑表中），保存源/目标库、版本、更新时间、两边 JSON 快照。
- 通知与访问：生成带 `conflictId` 的 token 链接供管理员查看（可通过邮件发送，邮件可在容器环境关闭）。
- 解决策略：管理员选择权威库（MYSQL/POSTGRES/SQLSERVER），系统拉取该库最新数据，并将最终版本号设为 “三库最大 version + 1”，再 upsert 到其余两库，最后将冲突状态置为 RESOLVED。

## 5. 数据库设计

### 5.1 业务表

三库业务表结构一致：

- `user_info`：用户信息（用户名、密码哈希、邮箱、角色）
- `category_info`：商品类别
- `supplier_info`：供应商
- `product_info`：商品（关联类别与供应商，含价格、库存、版本、更新时间、逻辑删除）
- `order_info`：订单（关联用户与商品，含数量、状态、地址、版本、更新时间、逻辑删除）

关键设计点：

- 主键采用 `BIGINT`，由应用层 Snowflake 生成（见后端 `SnowflakeIdGenerator` 的使用）。
- 采用 `version + updated_at` 作为同步/冲突判断的元信息。
- 逻辑删除字段 `deleted`：同步引擎当前忽略 DELETE 操作（只处理 INSERT/UPDATE）。

业务表e-r图：
![graphhhhh.png](graphhhhh.png)

### 5.2 同步支撑表

1. `change_log`：记录业务表的增量变更
   - `change_id`：自增序列（MySQL AUTO_INCREMENT / PG BIGSERIAL / SQLServer IDENTITY）
   - `db_code`：来源库标识（MYSQL/POSTGRES/SQLSERVER）
   - `table_name`、`op_type`、`pk_value`
   - `row_version`、`row_updated_at`
   - `payload_json`：整行快照（JSON/JSONB/NVARCHAR(MAX)）

2. `sync_checkpoint`：同步检查点（按来源库保存已处理到的最大 `change_id`）

- MySQL：`db/mysql/createSyncTab.sql`
- PostgreSQL：`db/postgres/create_s.sql`
- SQL Server：`db/sqlserver/createSynTab.sql`

3. `conflict_record`：冲突记录表

4. `sync_run_daily`：同步日报聚合表（用于报表与可视化）

### 5.3 索引与视图

- 常用查询索引：订单按用户/时间、按商品/时间，商品按类别/供应商。
- 视图 `v_order_detail`：订单明细（订单 + 用户 + 商品 + 类别 + 供应商）用于查询展示。

## 6. 关键实现说明（后端）

### 6.1 鉴权与权限控制

- 登录：`POST /api/auth/login`（见 `AuthController`）
- JWT 生成与解析：`JwtUtil` 负责签发 token 与解析 claims。
- 过滤器：`JwtAuthFilter` 从请求头提取 token 并写入 Spring Security 上下文。
- 访问控制：
  - `/api/auth/**`、Swagger、`GET /api/products/**`、`/conflicts/view`、`/conflicts/resolve` 允许匿名访问
  - 其余 API 需要 token
  - 管理员接口通过 `@PreAuthorize("hasRole('ADMIN')")` 控制

**示例代码（JWT 生成与解析）**

后端通过 `JwtUtil` 统一签发与解析 token：

```java
// src/main/java/com/sss/sync/config/security/JwtUtil.java
public String generateAccessToken(Long userId, String username, String role) {
  Instant now = Instant.now();
  Instant exp = now.plus(props.getAccessTokenExpireMinutes(), ChronoUnit.MINUTES);

  return Jwts.builder()
    .issuer(props.getIssuer())
    .subject(String.valueOf(userId))
    .claims(Map.of("username", username, "role", role))
    .issuedAt(Date.from(now))
    .expiration(Date.from(exp))
    .signWith(key())
    .compact();
}

public Claims parseClaims(String token) {
  return Jwts.parser()
    .verifyWith(key())
    .build()
    .parseSignedClaims(token)
    .getPayload();
}
```

**逻辑分析**

1. 登录成功后后端签发 JWT：`subject=userId`，并把 `username/role` 放进 claims。
2. 前端每次请求通过 Axios 拦截器加 `Authorization: Bearer <token>`。
3. `JwtAuthFilter` 拦截请求：解析 claims -> 组装 `ROLE_${role}` -> 写入 `SecurityContext`。
4. 业务接口通过 `@PreAuthorize` 做角色校验，实现“认证 + 授权”分离。

### 6.2 业务写入：下单与商品更新（事务）

- 下单：`OrderService.placeOrder(writeDb, ...)`
  - 先执行“库存足够则扣减”的原子更新（避免超卖）
  - 再插入订单，订单号由 Snowflake 生成
  - 按写入库选择不同事务管理器（MySQL / PostgreSQL）

- 商品更新：`ProductAdminService.updateProduct(productId, writeDb, ...)`
  - 更新名称/价格/库存
  - 更新时会触发变更日志写入 `change_log`，供同步引擎增量同步

**示例代码（下单：扣库存 + 插订单，事务回滚）**

```java
// src/main/java/com/sss/sync/service/OrderService.java
@Transactional(transactionManager = "mysqlTxManager", rollbackFor = Exception.class)
public Long placeOrderOnMysql(Long userId, Long productId, int qty, String address) {
  int affected = mysqlProductMapper.decreaseStockIfEnough(productId, qty);
  if (affected != 1) {
    throw BizException.of(400, "INSUFFICIENT_STOCK_OR_PRODUCT_NOT_FOUND");
  }

  OrderInfo o = new OrderInfo();
  o.setOrderId(snowflakeIdGenerator.nextId());
  o.setUserId(userId);
  o.setProductId(productId);
  o.setQuantity(qty);
  o.setOrderStatus("CREATED");
  o.setShippingAddress(address);
  o.setVersion(1L);

  mysqlOrderMapper.insert(o);
  return o.getOrderId();
}
```

**逻辑分析**

1. 先调用 `decreaseStockIfEnough(productId, qty)`：通常是 `UPDATE ... SET stock=stock-? WHERE stock>=?` 形式，保证并发场景下不会“超卖”。
2. 若影响行数不为 1，说明库存不足或商品不存在，直接抛业务异常，触发事务回滚。
3. 订单主键由 Snowflake 生成，避免依赖数据库自增（多库同步更友好）。
4. 插入订单后由触发器写入 `change_log`，后续由同步引擎增量同步到其他库。

**示例代码（商品更新：可选择写入库）**

```java
// src/main/java/com/sss/sync/service/ProductAdminService.java
public void updateProduct(Long productId, WriteDb writeDb, String productName, BigDecimal price, Integer stock) {
  switch (writeDb) {
    case MYSQL -> updateProductOnMysql(productId, productName, price, stock);
    case POSTGRES -> updateProductOnPostgres(productId, productName, price, stock);
  }
}
```

**逻辑分析**

1. 通过 `WriteDb` 显式选择写库（MYSQL/POSTGRES），用于演示“多主写”的冲突场景。
2. 更新发生在业务表 `product_info`，触发器会记录到各自数据库的 `change_log`，形成可同步的增量事件。

### 6.3 同步引擎与调度

- 同步引擎：`SyncEngineService.syncOnce()`
  - 从 MySQL 与 PostgreSQL 各自读取 `change_log` 增量
  - 按 `sync_checkpoint` 维护进度（已处理到的最大 `change_id`）
  - 只处理 `product_info` 与 `order_info` 的 INSERT/UPDATE
  - 对目标库执行 upsert；如命中冲突则写入 `conflict_record`

- 调度器：`SyncScheduler`
  - 支持三种触发：
    1) fixedDelay 定时
    2) cron 定时（默认每日 02:00）
    3) 实时轮询线程（pollIntervalMillis）
  - 使用 `ReentrantLock` 防止并发同步

**示例代码（同步引擎：读取检查点并推进）**

```java
// src/main/java/com/sss/sync/service/sync/SyncEngineService.java
private void syncFromMysql() {
  SyncCheckpointRow cp = mysqlSupport.getCheckpoint("MYSQL");
  long last = cp == null ? 0 : Optional.ofNullable(cp.getLastChangeId()).orElse(0L);

  List<ChangeLogRow> logs = mysqlSupport.fetchMysqlChangeAfter(last, props.getBatchSize());
  if (logs.isEmpty()) return;

  for (ChangeLogRow log : logs) {
    handleChange("MYSQL", log);
    last = log.getChangeId();
  }
  mysqlSupport.updateCheckpoint("MYSQL", last);
}
```

**逻辑分析**

1. `sync_checkpoint` 记录“每个源库已消费到的 change_id”，实现断点续跑与幂等。
2. 每轮最多拉取 `batchSize` 条变更，避免一次性同步过大。
3. 处理完一批后推进检查点：下次只拉取 `change_id > last` 的增量。

**示例代码（调度器：实时轮询 + 并发互斥）**

```java
// src/main/java/com/sss/sync/service/sync/SyncScheduler.java
@PostConstruct
public void startRealtimeLoop() {
  if (!props.isEnabled()) return;

  Thread t = new Thread(() -> {
    while (!Thread.currentThread().isInterrupted()) {
      try {
        if (!syncLock.tryLock()) {
          Thread.sleep(props.getPollIntervalMillis());
          continue;
        }
        try {
          engine.syncOnce();
        } finally {
          syncLock.unlock();
        }
        Thread.sleep(props.getPollIntervalMillis());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }, "sync-realtime-loop");

  t.setDaemon(true);
  t.start();
}
```

**逻辑分析**

1. 使用 `ReentrantLock.tryLock()` 防止“定时任务 + 实时线程”同时触发导致并发同步。
2. 轮询间隔由 `pollIntervalMillis` 控制，适合课堂演示“写入后很快就同步”。
3. 线程设置为 daemon，随应用退出自动结束。

### 6.4 冲突查看与解决

- 查看页：`GET /conflicts/view?token=...`
  - 后端直接返回 HTML（不依赖 SPA 登录），适合邮件链接访问
- 解决：`POST /conflicts/resolve?token=...` + body `{ authoritativeDb: "MYSQL"|"POSTGRES"|"SQLSERVER" }`
  - 解析 token 得到 conflictId 与 adminUsername
  - 拉取权威库当前 JSON
  - 计算三库最大版本号并设置 `finalVersion = max + 1`
  - 将最终结果 upsert 到其余两库
  - 将冲突状态置为 RESOLVED

**示例代码（冲突解决入口：token 校验 + 调用解决服务）**

```java
// src/main/java/com/sss/sync/web/controller/ConflictViewController.java
@PostMapping("/conflicts/resolve")
@ResponseBody
public Map<String, Object> resolveConflict(
    @RequestParam("token") String token,
    @RequestBody Map<String, String> request) {

  Claims claims = tokenService.parse(token);
  long conflictId = ((Number) claims.get("conflictId")).longValue();
  String adminUsername = (String) claims.get("admin");
  String authoritativeDb = request.get("authoritativeDb");

  resolutionService.resolveConflict(conflictId, authoritativeDb, adminUsername);
  return Map.of("success", true, "message", "Conflict resolved successfully");
}
```

**示例代码（解决策略：maxVersion + 1，确保最终结果“获胜”）**

```java
// src/main/java/com/sss/sync/service/conflict/ConflictResolutionService.java
long maxVersion = computeMaxVersion(tableName, id);
long finalVersion = maxVersion + 1;
authRow.put("version", finalVersion);

for (String targetDb : List.of("MYSQL", "POSTGRES", "SQLSERVER")) {
  if (targetDb.equals(authoritativeDb)) continue;
  if ("product_info".equals(tableName)) upsertProduct(targetDb, authRow);
  else upsertOrder(targetDb, authRow);
}

mysqlSupport.resolveConflict(conflictId, adminUsername, authoritativeDb);
```

**逻辑分析**

1. 解决入口采用 token（不依赖 SPA 登录），便于邮件直达、降低操作成本。
2. “maxVersion + 1” 的核心意义：把仲裁结果提升到三库最高版本之上，避免后续同步又被旧事件覆盖。
3. 传播时对两个目标库做 upsert，最后把 `conflict_record` 标为 RESOLVED，形成可审计闭环。

## 7. 关键实现说明（前端）

### 7.1 页面与路由

- `/login`：登录
- `/orders/new`：下单（USER/ADMIN）
- `/admin`：管理员布局页，包含
  - `/admin/queries/complex`：复杂查询
  - `/admin/reports/daily-sync`：每日同步报表（ECharts）
  - `/admin/products`：商品管理（更新时可选择写库）
  - `/admin/conflicts`：冲突入口（输入 token 打开 `/conflicts/view` 新页面）

### 7.2 API 调用

- Axios 拦截器自动注入 token；401 会清理本地缓存并跳转登录（见 `frontend/src/api/index.js`）。

## 8. 部署与运行步骤

### 8.1 Docker Compose 一键启动

1. 安装 Docker Desktop 并确保可运行 `docker compose`。
2. 在项目根目录执行：

```bash
docker compose up -d --build
```

3. 访问：

- 前端：`http://localhost/`
- 后端：`http://localhost:8080/`
- MySQL：`localhost:3306`
- PostgreSQL：`localhost:5432`
- SQL Server：`localhost:1433`

说明：

- Nginx 将 `/api/` 与 `/conflicts` 反向代理到后端（见 `frontend/nginx.conf`）。
- 容器环境可以通过环境变量关闭邮件：`SSS_MAIL_ENABLED=false`（见 `docker-compose.yml`）。

### 8.2 本地启动

- 后端：使用 IDEA 运行 Spring Boot 主类或 `mvn spring-boot:run`
- 前端：在 `frontend` 目录执行 `npm install`，然后 `npm run dev`


## 9. 总结与改进方向

### 9.1 实验总结

- 通过触发器统一记录 `change_log`，实现跨 MySQL/PostgreSQL/SQL Server 的增量同步。
- 通过 `sync_checkpoint` 保证同步幂等与断点续跑。
- 通过 `version` 进行冲突判定并将冲突记录持久化，结合 token 页面完成“人工仲裁式一致性维护”。
- 前后端与 Docker Compose 集成，使实验可一键复现。

### 9.2 可改进点

1. 对 DELETE/逻辑删除的同步支持（目前同步引擎忽略 DELETE）。
2. 更精细的冲突判定（例如结合 `updated_at`、字段级 diff、或向量时钟）。
3. 完善初始化数据：业务表主键由应用生成，若使用 SQL seed 建议补齐 `*_id` 值或改为通过 API 创建。
4. 邮件与密钥配置建议改为环境变量注入并脱敏；生产环境需更严格的 token 权限与过期策略。

---

## 附录 A：核心配置与脚本位置索引

- Docker Compose：`docker-compose.yml`
- 后端配置：`src/main/resources/application.yml`
- MySQL 脚本：`db/mysql/*`
- PostgreSQL 脚本：`db/postgres/*`
- SQL Server 脚本：`db/sqlserver/*`
- 同步引擎：`src/main/java/com/sss/sync/service/sync/SyncEngineService.java`
- 同步调度：`src/main/java/com/sss/sync/service/sync/SyncScheduler.java`
- 冲突查看/解决：`src/main/java/com/sss/sync/web/controller/ConflictViewController.java`
- 复杂查询：`src/main/java/com/sss/sync/service/ComplexQueryService.java`
- 同步报表：`src/main/java/com/sss/sync/service/DailySyncReportService.java`
