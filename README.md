# peach-gateway

Peach API 网关（**Spring Cloud Gateway** + **Nacos 服务发现**），依赖 **`peach-dependencies` BOM** 与 **JJWT** 校验访问令牌。

> 文档创作日期：2026-04-20，作者：leiyangjun

---

## 工程说明

本工程为 **独立可执行 Spring Boot 应用**（WebFlux 栈），**不依赖** `peach-common-start`。负责：

- 基于 **Nacos Discovery** 的手工/动态路由（`discovery.locator.enabled: false`，见 `route.discovery` 包）。
- **全局 JWT 校验**（`TokenGlobalFilter`）：解析 **HS256** Bearer Token，将 JWT `sub`（JSON）展开为下游查询参数 `peach_user_id`、`peach_username` 等，供 `peach-common-start` 的 `LoginUserUtil` 使用。
- **CORS**、**Swagger 文档门户**（`/peach-doc-portal/**`、`/index.html`）、网关本机 **springdoc**（`/swagger-ui.html`，仅扫描网关自身 Controller）。
- 路由调试：**`GET /routes`**（匿名，见过滤器白名单）。

## 功能说明

| 能力 | 说明 |
| --- | --- |
| 动态路由 | 注册中心服务变更监听与路由刷新（`peach.gateway.discovery.route-watch-interval-ms`） |
| 统一鉴权 | 除匿名路径外要求 `Authorization: Bearer <JWT>` |
| 文档入口 | 微服务文档经 `/{serviceId}/swagger-ui.html` 访问（与网关路由约定一致） |

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

单测默认激活 **`spring.profiles.active=test`**（Surefire 配置）。

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
- 新增匿名路径时同步维护 **`TokenGlobalFilter.ANONYMOUS_PATTERNS`**。

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
