# 项目长期备忘 · ai-rent-platform

## 项目概况
- Spring Boot 3.4.12 + MyBatis-Plus + MySQL + Redis + RabbitMQ + Spring AI(DeepSeek) + 支付宝沙箱。
- 后端 Maven 工程在 `<workspace>/ai-rent-platform/`，端口 8080；前端 `<workspace>/frontend/`，端口 5173。
- 业务：算力租赁平台。扣算力提交 AI 写诗任务 → RabbitMQ 异步生成 → 作品可上架广场。
- 库里没有 .sql 建表脚本；测试账号 `ccc`(user)、`cyr`(admin)，密码均 123456（没有 `cc`）。

## 约定
- 允许修 bug，不做无关重构；改动逐条说明、保持最小化。回复用中文简体。
- 接口统一封装 `Result{code,msg,data}`，成功 200 失败 500。

## 启动（2026-09-19 实测）
- 后端日常在 IDEA 跑（Run Config `AiRentPlatformApplication`，主类 org.example.airentplatform.AiRentPlatformApplication）。
- JDK：本机无 17，PATH 默认 java 是 11；IDEA SDK 用 `D:\5E` 的 JDK 21。命令行跑必须 `JAVA_HOME=D:\5E`。
- 环境变量：DEEPSEEK_API_KEY/EMAIL/PWD/ALIPAY_APP_ID/ALIPAY_APP_PRIVATE_KEY/ALIPAY_PUBLIC_KEY 只配在 IDEA Run Config 里，命令行启动缺了会 `Could not resolve placeholder`。
- Maven：`C:\Users\26355\Desktop\apache-maven-3.9.15-bin\...\bin\mvn`，无 settings.xml。
- MySQL 本机 3306/ai-rent-platform（root/123456）。Redis/RabbitMQ 在虚拟机 192.168.246.132（yml 配置正确，勿改 localhost）；探测不通先开虚拟机。RabbitMQ 连不上不会导致启动失败。
- 启动顺序：MySQL → 虚拟机 → 后端 8080 → 前端 5173。沙箱里 java -jar 需显式 `--server.port=8080`。
- 前端启动：`npm.cmd run dev`（本机 ExecutionPolicy 禁 .ps1）；node/JDK21 都混装在 `D:\5E`。
- 前端用 Vite proxy 把 /api 转发 8080 并 rewrite，同源免 CORS；5173 端口固定（支付宝 return-url 写死）。

## 已修复的致命坑（复发线索，勿回退）
1. pom 双 ORM starter 冲突 → MP 自动配置失效，所有 Mapper 500。已换 `mybatis-plus-spring-boot3-starter:3.5.9` 并删原生 starter；`loginconfign` 放行 `/error`。
2. 非 parent 继承拿不到 `-parameters` → 散装参数接口 500。已在 maven-compiler-plugin 加 `<parameters>true</parameters>`（改完必须 `mvn clean package`）。
3. `aitask.taskNo` 列名是驼峰，MP 下划线映射会错 → 已加 `@TableField("taskNo")`。库内列名下划线/驼峰混用，不能全局关映射。
4. `user` 表 AUTO_INCREMENT 曾是脏计数器 → `ALTER TABLE user AUTO_INCREMENT=3;` 修过；重灌种子数据可能复发。
5. 前端判错：拦截器返回 HTTP 200 + `{"msg":"请先登录"}`；后端异常是 HTTP 500 + `/error` JSON（无 code 字段）。必须靠 HTTP 状态码区分，已在 `frontend/src/api.js` serverError() 修正。

## 后端现存坑（未修，联调注意）
1. 所有写接口散装表单参数（无 @RequestBody），必须 x-www-form-urlencoded，JSON 传全 null。
2. 未登录 `{"msg":"请先登录"}` HTTP 200 无 code 字段；`/error` 响应也无 code。`adminintercepter` 拒绝时 200 + 空 body（非 403）。
3. `/pay/notify` 被登录拦截器拦住，支付宝回调进不来（未修）。修法：放行 + 补 rsaCheckV1 验签。notify 里加算力还有幂等缺口（重复投递重复加）与 selectOne NPE。
4. `/ai/upload` 成功时 return null 空 body；`/admin/revisespu/status` 空 body 代表失败。
5. ~~`POST /user/profile` 改用户名后 Session 仍旧值~~ 已修（2026-09-23）：改名成功后同步刷新 Session；`adminintercepter` selectOne 也已判空；改名入库前已加查重（新名≠当前名才查，重名返回"用户名已存在"）。剩余：`user.username` 无唯一索引，并发下查重可穿透。
6. RabbitMQConsumer 只回写任务状态，AI 诗文未落库（AiTask 无结果字段）；`GET /ai/task/list` 用户级接口未补（`/spu/list` 已补）。
7. 作品分类 sortid 纯数字无字典表；服务端 /error 会泄露完整堆栈（生产应 include-stacktrace=never）。

## /spu/list 设计原则（用户拍板：浏览放开、交易不放宽）
- `GET /spu/list` 只返回 status=1，已放行未登录可看；`/admin/readspu` 返回全部仅管理员。
- 放行写精确路径不写 `/spu/**`；不用 excludePathPatterns 破坏「/admin/** = 管理员」约定；新增写接口另起路径。

## 前端要点
- 页面：首页套餐/支付、登录注册、个人中心(签到/改资料/绑邮箱)、AI创作(轮询)、作品广场+详情、管理后台、支付返回页。
- `api.js` 有 `listSpus()` 走 /spu/list；`adminListSpus()` 给 Admin.vue。Home.vue 未登录也可浏览，购买跳登录。
