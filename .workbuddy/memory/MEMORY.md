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
- 后端日常在 IDEA 跑（Run Config `AiRentPlatformApplication`，主类 org.example.airentplatform.AiRentPlatformApplication；
  ⚠️ RunManager 里有**两份同名**配置，一份蓝色 Application 型、一份绿色 Spring Boot 型，见下「环境变量」条）。
- JDK：本机无 17，PATH 默认 java 是 11；`D:\5E` 是 JDK 21.0.11（node/npm 混装在同目录），命令行跑设 `JAVA_HOME=D:\5E`。
  `C:\Program Files\Java\jdk-25.0.2` **不可用**（Lombok 1.18.30 不支持 25）；IDEA 自带 JBR 21 也可用。两条路径 9-23 实测均 BUILD SUCCESS。
- 环境变量：DEEPSEEK_API_KEY/EMAIL/PWD/ALIPAY_APP_ID/ALIPAY_APP_PRIVATE_KEY/ALIPAY_PUBLIC_KEY。
  **9-23 终极定因（此前「配置丢了」的判断已推翻）**：值一直存在，配在 IDEA 的**蓝色 `Application` 型** Run Config 里；
  而 Spring Boot 工程点运行默认走**绿色 `SpringBootApplicationConfigurationType`** 那份**同名**配置。
  `<component name="RunManager" selected="Spring Boot.AiRentPlatformApplication">` 证明选中的是绿色那份，
  而绿色配置**整段没有 `<envs>`** → 运行时变量为空 → `Could not resolve placeholder`。
  **不是变量丢了，是配好的那份没被运行。**
  ⚠️ IDEA 工程在**内层** `<workspace>/ai-rent-platform/.idea/workspace.xml`（外层那份仅 1.9KB 是空壳，无 RunManager）；
  workspace.xml 已被 `.idea/.gitignore` 忽略，凭据不会进 git。两份配置都是 `temporary="true"`，
  临时配置超 5 个会被 IDEA 清理，长期用要「另存为正式配置」或改写系统环境变量。
  蓝色配置里多设了 `USERNAME`（yml 无 `${USERNAME}`，纯冗余，且覆盖 Windows 内置变量）—— 建议删除。
  两个 JUnit 配置只设了 EMAIL/PWD，`contextLoads` 照样会炸。
  **已解决（9-23 15:54，方案 A）**：6 个变量已写入 **Windows 用户级**（HKCU\Environment），值从蓝色配置程序化读出、
  全程未打印明文；`USERNAME` 那条**故意不设**（yml 无 `${USERNAME}`，且会覆盖系统内置变量）。
  此后任何启动方式都无需再 `$env:` 注入，但**改前已启动的进程看不到** ——
  实测当时 IDEA 本体 PID 7296 于 15:41 启动、早于 15:54 的写入，故**必须 File → Exit 完全退出 IDEA 再开**（关窗口不算）。
  真机回归：真实凭据下 `mvn spring-boot:run --server.port=8080` → `Tomcat 8080` + `amqp://guest@192.168.246.134:5672` +
  `Started AiRentPlatformApplication in 3.651 seconds`，零 ERROR。回滚：删掉 HKCU\Environment 下这 6 个值即可。
- Maven：`C:\Users\26355\Desktop\apache-maven-3.9.15-bin\...\bin\mvn`，无 settings.xml。
  ⚠️ 环境变量里写的是 **`MEVEN_HOME`**（`MAVEN_HOME` 拼错，User 与 Machine 两级都有），但 **Path 里加的是字面全路径
  `...\apache-maven-3.9.15\bin`，并未引用该变量** → 它是个**孤儿变量**，实测 `mvn -v` 按名字调用正常（Maven 3.9.15）。
  **此前的「mvn 不能按名字调用、必须全路径」是错误结论，已更正**（全路径只是当时的习惯写法）。
  隐性风险：标准名 `MAVEN_HOME` 缺失，靠标准名探测 Maven 的工具会退回自带 Maven；谁要是只把它改名却忘了看 Path，容易误判。
  **9-23 16:05 已按方案①改名**：User 级 → `MAVEN_HOME`（`MEVEN_HOME` 已删）；Machine 级**需管理员权限，尚未处理**（待办）。
  **另证：`mvn.cmd` L67-68 无条件 `set "MAVEN_HOME=%~dp0"` 覆盖、且完全不引用 `M2_HOME`** → 该环境变量对 mvn 行为
  **零影响**（改名纯整理，功能不变）；它的价值只在「其它按标准名探测 Maven 的工具」。`JAVA_HOME` 按主人选择**未动**。
- ⚠️ `JAVA_HOME` 在 User/Machine 两级**都没设**，且系统 Path 最前面是 `jdk-11.0.30`，故裸 `mvn -v` 报的是 **Java 11**
  → 命令行构建必须显式 `JAVA_HOME=D:\5E`（JDK 21），否则编不过 / 用错 JDK。
- MySQL 本机 3306/ai-rent-platform（root/123456）。Redis/RabbitMQ 在虚拟机 192.168.246.132（yml 配置正确，勿改 localhost）；探测不通先开虚拟机。RabbitMQ 连不上不会导致启动失败。
- 启动顺序：MySQL → 虚拟机 → 后端 8080 → 前端 5173。
- **宿主注入 `SERVER__PORT=51083` 陷阱（9-23 实测定位）**：WorkBuddy 给子进程注入该变量（双下划线），
  Spring 宽松绑定解析为 `server.port=51083`，而 51083 被 WorkBuddy.exe 自身占用 → 在 WorkBuddy 终端启动
  必报 "Port 51083 already in use"（与 8080 无关，极具误导性）。解法：`Remove-Item Env:SERVER__PORT`
  或显式 `--server.port=8080`。IDEA / 普通 cmd 里无此变量。
- **9-23 实测启动成功**：注入 6 个占位值后 `mvn spring-boot:run` → Tomcat 8080 + RabbitMQ .134:5672 连上
  + `Started ... in 2.657s`、零 ERROR。确认 6 个环境变量是唯一硬阻断；MySQL/Redis 为懒连接，首次查询才建连。
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
3. ~~`/pay/notify` 被登录拦截器拦住、无验签、幂等缺口、selectOne NPE~~ 已全修（2026-09-23）：loginconfign 放行该路径；notify 进口 rsaCheckV1 验签 + 参数/订单/SPU/用户判空 + status==2 幂等直返；TRADE_SUCCESS 后调 MailUtil 发「购买算力套餐X成功」邮件（未绑邮箱静默跳过）。
4. `/ai/upload` 成功时 return null 空 body；`/admin/revisespu/status` 空 body 代表失败。
5. ~~`POST /user/profile` 改用户名后 Session 仍旧值~~ 已修（2026-09-23）：改名成功后同步刷新 Session；`adminintercepter` selectOne 也已判空；改名入库前已加查重（新名≠当前名才查，重名返回"用户名已存在"）。剩余：`user.username` 无唯一索引，并发下查重可穿透。
6. RabbitMQConsumer 只回写任务状态，AI 诗文未落库（AiTask 无结果字段）；`GET /ai/task/list` 用户级接口未补（`/spu/list` 已补）。
7. 作品分类 sortid 纯数字无字典表；服务端 /error 会泄露完整堆栈（生产应 include-stacktrace=never）。
8. `aicontroller.show(String id)` 挂在 `@GetMapping("/show/{id}")` 上**漏写 `@PathVariable`** → 路径变量收不到，
   id 恒为 null，详情接口恒返回「暂无对应内容」。前端已在 `api.js` 用「路径 + `?id=` 双写」兜底，**根治要加注解**。
9. `PayController.notify` 幂等只判 `status == 2`：若 `TRADE_FINISHED`(4) 覆盖掉已成功的 2，再来一次
   `TRADE_SUCCESS` 会**重复加算力**。建议放宽为 `status >= 2` 或另记发放标记。
10. `GET /user/profile` 把整个 User 返给前端，**含明文 password**，建议后端脱敏。
11. 教训：`Order.status` 是 `int`，写 `!= null` 会直接编译失败（9-23 发生并已修）。改带判空的代码前先看实体字段类型。

## /spu/list 设计原则（用户拍板：浏览放开、交易不放宽）
- `GET /spu/list` 只返回 status=1，已放行未登录可看；`/admin/readspu` 返回全部仅管理员。
- 放行写精确路径不写 `/spu/**`；不用 excludePathPatterns 破坏「/admin/** = 管理员」约定；新增写接口另起路径。

## 前端要点
- 页面：首页套餐/支付、登录注册、个人中心(签到/改资料/绑邮箱)、AI创作(轮询)、作品广场+详情、管理后台、支付返回页。
- `api.js` 有 `listSpus()` 走 /spu/list；`adminListSpus()` 给 Admin.vue。Home.vue 未登录也可浏览，购买跳登录。
- **`frontend/README.md` 是项目的启动手册**（9-23 重写）：前置条件表、6 个环境变量的注入命令、
  页面↔接口对照表、请求层 5 个兼容点、现存后端问题清单、测试账号。要起项目先看它。
- 9-23 对齐：`Profile.vue` 改资料成功后不再引导重新登录（后端已同步 Session）；
  `api.js#getProduction` 对 `/ai/show/{id}` 采用「路径 + `?id=` 双写」；`PaySuccess.vue` 补了验签/幂等/邮件说明。
