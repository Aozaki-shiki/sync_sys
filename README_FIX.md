# SSS Sync System - 修复说明

## 问题总结

原项目存在 MyBatis-Plus BaseMapper 方法未正确注入的问题，导致启动后同步线程/定时任务触发时抛出 `BindingException: Invalid bound statement (not found)`。

## 解决方案

### 1. 核心修复

#### 1.1 添加 TypeAliasesPackage 配置
在所有数据源的 SqlSessionFactory 配置中添加了 `typeAliasesPackage`：
- `DataSourceMysqlConfig.java`
- `DataSourcePostgresConfig.java`
- `DataSourceSqlServerConfig.java`

```java
bean.setTypeAliasesPackage("com.sss.sync.domain.entity");
```

#### 1.2 使用注解 SQL 替代 BaseMapper 方法
为了绕过 BaseMapper 注入问题，所有关键业务逻辑都改用了 `@Select`、`@Insert`、`@Update` 等注解 SQL：

**新增注解方法的 Mapper：**
- `ReadProductMapper.selectAllActive()`
- `MysqlUserMapper.findByUsername()`
- `PostgresUserMapper.findByUsername()`
- `MysqlOrderMapper.insertOrder()`
- `PostgresOrderMapper.insertOrder()`
- `MysqlProductMapper.findById()`, `insertProduct()`, `updateProduct()`
- `PostgresProductMapper.findById()`, `insertProduct()`, `updateProduct()`

**同步相关 Mapper（完全基于注解）：**
- `MysqlChangeLogMapper` - 查询未同步的变更日志
- `PostgresChangeLogMapper` - 查询未同步的变更日志
- `MysqlConflictRecordMapper` - 冲突记录增删查

### 2. 新增功能实现

#### 2.1 实体类
- `ChangeLog.java` - 变更日志实体
- `ConflictRecord.java` - 冲突记录实体
- 为所有实体添加了 `@TableName` 注解

#### 2.2 同步引擎
- `SyncEngineService.java` - 实现了双向同步逻辑：
  - 从 MySQL/PostgreSQL 的 `change_log` 表拉取变更
  - 应用到目标数据库
  - 冲突检测（基于 version 字段）
  - 冲突记录落库并发送邮件通知
  
- `SyncScheduler.java` - 实现了两种同步模式：
  - 实时轮询（默认 1 秒间隔，可配置 `sss.sync.pollIntervalMillis`）
  - 定时任务（默认 10 秒间隔，可配置 `sss.datasource.sync.scheduled.fixedDelayMillis`）

#### 2.3 冲突处理
- `ConflictLinkTokenService.java` - 生成和解析冲突查看链接的 JWT token
- `ConflictViewController.java` - 提供冲突详情查看接口和 HTML 页面
- `MailService.java` - 163 邮箱发送通知

#### 2.4 Swagger 增强
- `OpenApiConfig.java` - 配置了 Bearer token 认证支持
- 所有 Controller 添加了 Swagger 注解 (`@Tag`, `@Operation`, `@SecurityRequirement`)

### 3. 配置修复
- 修正了 `application.yml` 中 `sss.sync` 配置层级（原本错误地嵌套在 datasource 下）
- 添加了 `.gitignore` 排除 `target/` 目录

## 验收步骤

### 前置准备

需要先执行数据库初始化脚本（第 1 包，不在此 PR 中），包括：
1. 在 MySQL、PostgreSQL、SQL Server 三个数据库中创建 `sss_db` 数据库
2. 执行建表脚本（user_info, product_info, order_info, change_log, conflict_record 等）
3. 插入测试数据（用户、商品等）

### 1. 编译测试
```bash
mvn -q -DskipTests package
```
✅ 已通过

### 2. 启动应用
```bash
java -jar target/sss-sync-system-0.0.1-SNAPSHOT.jar
```

确保配置文件中的数据库连接信息正确（`application.yml`）：
```yaml
sss:
  datasource:
    mysql:
      url: jdbc:mysql://127.0.0.1:3306/sss_db?...
      username: root
      password: "myPassWord++"
    postgres:
      url: jdbc:postgresql://127.0.0.1:5432/sss_db
      username: postgres
      password: "myPassWord++"
    sqlserver:
      url: jdbc:sqlserver://127.0.0.1:1433;databaseName=sss_db;...
      username: sa
      password: "myPassWord++"
```

### 3. 访问 Swagger UI
打开浏览器访问：
```
http://localhost:8080/swagger-ui/index.html
```

应该能看到所有 API 接口，并且有 "Authorize" 按钮支持 Bearer token 输入。

### 4. 测试第 2 包功能

#### 4.1 登录获取 JWT
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

响应示例：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 1,
    "username": "admin",
    "role": "ADMIN"
  }
}
```

#### 4.2 查询商品列表（从 SQL Server）
```bash
curl http://localhost:8080/api/products
```

#### 4.3 下单（写 MySQL）
```bash
curl -X POST http://localhost:8080/api/orders/place \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "userId": 1,
    "productId": 1,
    "quantity": 2,
    "shippingAddress": "北京市海淀区",
    "writeDb": "MYSQL"
  }'
```

库存不足时应返回 400 错误并回滚事务。

### 5. 测试第 3 包功能（同步引擎）

#### 5.1 开启同步
修改 `application.yml`：
```yaml
sss:
  sync:
    enabled: true  # 改为 true
```

重启应用后，查看日志应显示：
```
Starting sync engine (poll interval: 1000ms)
```

#### 5.2 触发同步
1. 在 MySQL 或 PostgreSQL 的写库中修改商品数据
2. 触发器会自动写入 `change_log` 表
3. 同步引擎会轮询并同步到另一个库

#### 5.3 制造冲突
1. 同时在 MySQL 和 PostgreSQL 修改同一条商品数据
2. 增加各自的 version 字段
3. 触发同步时会检测到冲突
4. 系统会：
   - 在 MySQL 的 `conflict_record` 表插入冲突记录
   - 发送邮件到 `sss.mail.adminTo` 配置的邮箱
   - 邮件中包含查看详情的链接

#### 5.4 查看冲突详情
邮件中的链接格式：
```
http://localhost:8080/conflicts/view?token=eyJhbGc...
```

可在 PC 或移动端浏览器打开，会显示冲突的详细信息（source 和 target 的 payload JSON）。

## 配置说明

### 同步配置
```yaml
sss:
  sync:
    enabled: false              # 是否启用同步引擎
    pollIntervalMillis: 1000    # 实时轮询间隔（毫秒）
    batchSize: 200              # 每次同步的最大变更数
    scheduled:
      enabled: false            # 是否启用定时同步
      fixedDelayMillis: 10000   # 定时同步间隔（毫秒）
```

### 邮件配置
```yaml
spring:
  mail:
    host: smtp.163.com
    port: 465
    username: mo1lly@163.com
    password: "TJcALtY5z39gyDax"  # 163 授权码

sss:
  mail:
    enabled: true
    from: "mo1lly@163.com"
    adminTo: "mo1lly@163.com"
    conflictViewBaseUrl: "http://localhost:8080"
```

## 技术栈
- Spring Boot 3.2.10
- MyBatis-Plus 3.5.7
- JDK 17 (maven release) / 22 (runtime)
- MySQL 8.0.44
- PostgreSQL 18
- SQL Server 2022
- SpringDoc OpenAPI 2.6.0
- JJWT 0.12.5

## 后续工作

如需进一步集成第 4 包或后续功能，可在此基础上扩展：
- 添加更多表的同步支持（目前仅支持 product_info）
- 实现冲突的自动解决策略
- 添加同步状态监控面板
- 优化同步性能（批量操作、并发控制等）

## 注意事项

1. **数据库初始化**：本 PR 不包含数据库脚本，需要先执行第 1 包的初始化脚本
2. **邮件配置**：如果不需要邮件通知，可设置 `sss.mail.enabled=false`
3. **同步开关**：默认同步引擎是关闭的（`sss.sync.enabled=false`），需要手动开启
4. **BaseMapper**：虽然添加了 typeAliasesPackage，但为了稳定性，关键路径都使用了注解 SQL
5. **事务管理**：不同数据源使用不同的事务管理器（`mysqlTxManager`、`postgresTxManager`）
