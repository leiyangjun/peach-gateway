# peach-gateway

Peach API 网关（**Spring Cloud Gateway** + **Nacos 服务发现**），依赖 **`peach-dependencies` BOM** 与 **JJWT** 校验访问令牌。

> 文档创作日期：2026-04-20，作者：leiyangjun

---

## 工程说明

本工程为 **独立可执行 Spring Boot 应用**（WebFlux 栈），**不依赖** `peach-common-start`。负责：

- 基于 **Nacos Discovery** 的手工/动态路由（`discovery.locator.enabled: false`，见 `route.discovery` 包）。
- **全局 JWT 校验**（`TokenGlobalFilter`）：解析 **HS256** Bearer Token，将 JWT `sub`（JSON）展开为下游查询参数 `peach_user_id`、`peach_username` 等，供 `peach-common-start` 的 `UserContext` 使用。
- **CORS**、**Swagger 文档门户**（`/peach-doc-portal/**`、`/index.html`）、网关本机 **springdoc**（`/swagger-ui.html`，仅扫描网关自身 Controller）。
- 路由调试：**`GET /routes`**（匿名，见过滤器白名单）。

## 功能说明

| 能力 | 说明 |
| --- | --- |
| 动态路由 | 注册中心服务变更监听与路由刷新（`peach.gateway.discovery.route-watch-interval-ms`） |
| 统一鉴权 | 除匿名路径外要求 `Authorization: Bearer <JWT>` |
| 文档入口 | 门户 `GET /index.html`、`/peach-doc-portal/**`；微服务 Swagger 经 `/peach-gateway/{serviceId}/swagger-ui.html`；本网关 springdoc 经 `/peach-gateway/swagger-ui/**`（勿直连 `:8090/swagger-ui`） |
| Shell 路由 | 本网关 shell 使用 `forward:/` + `ShellRouteForwardPathSupport`，避免 `lb://peach-gateway` 回环导致请求头膨胀；**无需** Cookie 剔头过滤器或 64KB 头大小兜底 |

## 使用说明

### 环境要求

- **JDK 21**
- **Maven 3.9+**（建议）
- 可选本地/远程 **Nacos**（不配时部分发现能力受限，以实际代码为准）

### 本地运行

前置：本机已 `mvn install` **`peach-dependencies`**（`0.0.1-SNAPSHOT` 需进本地仓库）。

```bash
mvn -f peach-gateway/pom.xml spring-boot:run
```

默认端口 **`8090`**（见 `src/main/resources/application.yml`）。

### 构建与校验

```bash
mvn -f peach-gateway/pom.xml clean verify -DskipTests
```

执行单测（静默）：

```bash
mvn -q test
```

单测默认激活 **`spring.profiles.active=test`**（Surefire 配置）。单测使用 **`src/test/resources/application-test.yml`**（如关闭 Nacos 发现、关闭 springdoc UI），与主配置叠加；需本地已安装父 BOM **`peach-dependencies`**（版本见 `pom.xml`）。

### 环境变量（摘要）

| 变量 | 含义 |
| --- | --- |
| `NACOS_SERVER_ADDR` | Nacos 地址，默认 `192.168.99.100:8848` |
| `NACOS_USERNAME` / `NACOS_PASSWORD` | Nacos 认证 |
| `NACOS_NAMESPACE` | 命名空间，默认 `dev` |
| `NACOS_GROUP` | 分组，默认 `DEFAULT_GROUP` |
| `NACOS_REGISTER_IP` / `NACOS_REGISTER_PORT` | 实例注册 IP/端口 |
| `PEACH_GATEWAY_ROUTE_WATCH_INTERVAL_MS` | 路由表轮询间隔（毫秒） |

### 与认证服务（JWT）对齐

当前 **`TokenGlobalFilter`** 与 **`peach-auth-service`** 中 **`JwtUtil.DEFAULT_HMAC_SECRET`** 使用**同一默认 HS256 明文秘钥**（UTF-8 长度 ≥ 32 字节）。生产环境应改为**配置中心 / 环境变量统一管理**并同时修改网关与认证侧实现，**勿长期依赖代码内默认值**。

## 开发约定

- 网关使用 **WebFlux**，不要引入阻塞式 Servlet 栈；与业务服务（MVC）技术栈分离。
- **业务码前缀**：`spring.application.module-code: GWAY`（四位），与全局过滤器错误码拼装规则一致。
- 新增 Swagger/文档匿名 GET 时同步维护 **`JwtAnonymousRuleCache`** 兜底规则中的路径模式。
- 文档/Swagger 经网关访问时依赖 shell **`forward:/`** 本机转发，勿再引入 `lb://` 自指回环；若出现 `TooLongHttpHeaderException`，优先检查路由是否误走回环而非调大 Netty 头限制。

---

## 统一错误模型（网关对外 JSON）

网关侧「错误 / 校验失败」响应体使用 **`ErrorResult`**：仅包含 **`code`**（字符串）、**`msg`**（字符串），**没有** `data` 字段，也**不是** `ApiResult` 那种 HTTP 200 包一层业务码的形态。

### `code` 拼装规则（11 位）

与代码注释中的约定一致：**四位模块前缀** + **三位 HTTP 状态数字** + **四位末段语义码**（字符串拼接，例如模块 `GWAY`、HTTP `404`、末段 `4013` → **`GWAY4044013`**）。

- 模块前缀来自配置 **`spring.application.module-code`**（必须为四位）；启动时由 **`GatewayModuleCodeConfiguration`** 校验并写入 **`ModuleCodeCache`**，供过滤器、全局异常处理等读取。
- **`Message400`**：用于网关过滤器等场景的「客户端类」末段，取值为 **4001–4008**（与 HTTP 401/403/404 等组合使用，由具体工厂方法选择 HTTP 段）。例如鉴权头缺失：`401` + `4005` → `GWAY4014005`。
- **`MessageError`**：用于 **`GlobalErrorWebExceptionHandler`**（隔离层）等按 HTTP 状态映射的末段：
  - **4xx 系列末段从 4009 起**（如 `NOT_FOUND` → **4013**），**不包含 HTTP 200**；该枚举仅表达错误语义，成功响应不走 `MessageError`。
  - **5xx 系列末段从 5001 起**（如 `INTERNAL_SERVER_ERROR` → **5001**）。
- 未知 HTTP 状态在 **`MessageError.formStatus(int)`** 中会回落为 **`INTERNAL_SERVER_ERROR`**（与当前实现一致）。

### 隔离层（`GlobalErrorWebExceptionHandler`）

- **职责**：处理**未命中下游路由**、**无网关自身 Controller/静态资源命中**时的框架异常，以及网关节点自身超时、连接类等问题（见类注释）。
- **不职责**：**不**改写「已匹配动态路由并成功转发到下游」时的响应体；该场景下游原样返回（含下游 4xx/5xx 的 body）。
- **响应**：HTTP 状态与异常一致；`Content-Type: application/json;charset=UTF-8`；JSON 体为 **`ErrorResult`**（`code` / `msg`）。

### 过滤器错误（`TokenGlobalFilter`）

对非匿名路径校验 JWT；失败时 HTTP **401**，体为 **`ErrorResult.unauthorized(Message400.*)`**，末段为 **4001–4008** 中对应枚举值（见 `Message400` 源码）。

**生效范围（与当前 WebFlux + Gateway 装配一致）**：仅当请求**匹配到**基于服务发现的动态路由（`DynamicDiscoveryRouteDefinitionLocator` 生成的 `Path=/{serviceId}/**`）并进入 Spring Cloud Gateway 全局过滤器链时，`TokenGlobalFilter` 才会执行。若当前无可用动态路由或路径未命中任一 Gateway 路由，请求会落到本机 `@RestController` 或静态资源处理；此时**不会**经过 `TokenGlobalFilter`，也就不会出现 JWT 相关的 401（例如单测里 `WebTestClient.bindToApplicationContext` 访问未映射路径时，常见为静态资源 404 再经 `GlobalErrorWebExceptionHandler` 转为隔离层 JSON）。

---

## Git 远程（一次推送，双端同步）

本仓库 **`master`** 为默认分支（与 Gitee 一致；GitHub 若新建仓库默认为 `main`，请在 GitHub 仓库 **Settings → Default branch** 改为 **`master`**，或首次推送后改默认分支）。

| 用途 | 地址 |
|------|------|
| **fetch** | `git@github.com:leiyangjun/peach-gateway.git` |
| **push** | 同上 + `git@gitee.com:leiyangjun/peach-gateway.git` |

```bash
git add .
git commit -m "your message"
git push -u origin master
```

之后可直接 `git push`（会依次推送到 GitHub 与 Gitee）。

核对：`git remote -v`、`git config --get-all remote.origin.pushurl`。
