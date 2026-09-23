# AI 算力租赁平台 · 前端

Vite + Vue 3 + vue-router 的纯前端项目，对接上一层目录的 Spring Boot 后端（端口 `8080`）。
前端不新增后端接口，只做「已有接口的适配」——本次唯一触碰后端的地方是修一处编译错误（见文末改动记录）。

## 启动方式

### 1. 前置条件

| 依赖 | 地址 | 说明 |
| --- | --- | --- |
| MySQL | `localhost:3306` | 库名 `ai-rent-platform`，`application.yml` 里写的是 root/123456 |
| Redis | `192.168.246.134:6379` | 验证码、签到状态存在这里 |
| RabbitMQ | `192.168.246.134:5672` | guest/guest，AI 写诗任务队列 |
| JDK | **17 及以上** | 用 `D:\5E`（JDK 21）或 IDEA 自带的 JBR 21，别用默认的 java 11 |

### 2. 后端启动前必须设的 6 个环境变量

`application.yml` 里全是 `${...}` 占位符，缺一个后端就启动失败：

| 变量 | 用途 |
| --- | --- |
| `DEEPSEEK_API_KEY` | Spring AI 调 DeepSeek |
| `EMAIL` | 发件邮箱（`spring.mail.username`） |
| `PWD` | 发件邮箱授权码（`spring.mail.password`） |
| `ALIPAY_APP_ID`、`ALIPAY_APP_PRIVATE_KEY`、`ALIPAY_PUBLIC_KEY` | 支付宝沙箱 |

> `PWD` 在 bash 里是保留变量，建议用 PowerShell 或 IDEA 的 Run Configuration 配置。

**JDK 别用错**：PATH 里默认是 java 11（`<source>17</source>` 直接编不过），另装的
`C:\Program Files\Java\jdk-25.0.2` 也不行（Lombok 1.18.30 不支持 JDK 25）。
用 `D:\5E`（JDK 21，本机惯用）或 IDEA 自带的 JBR 21，两者都已实测可编译。

PowerShell 启动后端：

```powershell
$env:JAVA_HOME = "D:\5E"          # JDK 21；备选 "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.1\jbr"
$env:DEEPSEEK_API_KEY = "..."; $env:EMAIL = "..."; $env:PWD = "..."
$env:ALIPAY_APP_ID = "..."; $env:ALIPAY_APP_PRIVATE_KEY = "..."; $env:ALIPAY_PUBLIC_KEY = "..."
Set-Location ai-rent-platform     # 后端模块目录（含 pom.xml 的那一层）
& "C:\Users\26355\Desktop\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd" spring-boot:run
```

在 IDEA 里跑更省事：Run Configuration 选 `AiRentPlatformApplication`，把上面 6 个变量填进
「Environment variables」直接运行。（注意：本仓库 `.idea/` 里**没有存任何 Run Configuration**，
环境变量得重新配一次。）

### 3. 前端

先确保后端已在 `8080` 起来，然后：

```bash
cd frontend
npm install        # 首次运行需要
npm run dev        # 打开 http://localhost:5173
```

生产构建与本地预览：

```bash
npm run build      # 产物在 frontend/dist
npm run preview    # 同样跑在 5173
```

## 为什么是 5173 端口 + 代理

后端 `application.yml` 里支付宝的 `return-url` 写的是 `http://localhost:5173/pay/success`，
正好对应本项目的 `/pay/success` 路由，所以 dev server 固定 5173（`strictPort: true`）。

后端用 `HttpSession` 做登录态，且没有配置 CORS。因此 `vite.config.js` 里用代理转发：

```
浏览器 → localhost:5173/api/check/login → (代理) → localhost:8080/check/login
```

浏览器视角下前后端同源，`JSESSIONID` Cookie 自动携带，**不需要改后端加 CORS**。
`/api` 前缀在转发时被抹掉，这样也避免和后端本身是 `/ai`、`/admin` 开头的接口路径、
以及前端 `/ai`、`/admin` 路由互相冲突。

## 目录结构

```
frontend/
├── vite.config.js          # 5173 端口 + /api 代理到 8080
├── index.html
└── src/
    ├── api.js              # 全部接口封装（含 Result / 空响应体 / 拦截器的兼容处理）
    ├── config.js           # 作品分类、写诗消耗算力等可配置项
    ├── toast.js            # 全局轻提示
    ├── router/index.js     # 路由 + 登录 / 角色守卫
    ├── store/user.js       # 登录用户状态（算力余额、角色）
    ├── components/         # NavBar、ToastHost
    └── views/              # Home 套餐 / Login / Profile / AiCreate / Gallery(+Detail) / Admin / PaySuccess
```

## 页面与接口对照

| 页面 | 调用的后端接口 |
| --- | --- |
| 登录 / 注册 | `POST /check/login`（`type=1` 账密、`type=2` 邮箱验证码）、`GET /check/sendcode`、`POST /check/register`、`POST /check/logout` |
| 首页算力套餐 | `GET /spu/list`（**公开接口**，后端只返回已上架套餐）、`POST /pay`、`POST /check/sign` |
| 个人中心 | `GET /user/profile`、`POST /user/profile`、`POST /user/email`、`POST /check/sign` |
| AI 创作 | `POST /ai/poem`、`GET /ai/status?taskNo=`（2 秒轮询，返回体含 `content` 诗文）、`GET /user/aitask`（用户级，身份取 Session、无需入参，查自己的任务） |
| 作品广场 | `GET /ai/show/list?sortid=`、`GET /ai/show/{id}?id=`（双写兜底，见兼容点 5）、`POST /ai/upload` |
| 管理后台 | `GET /admin/read`、`POST /admin/revise`、`GET /admin/readspu`、`POST /admin/addspu`、`POST /admin/revisespu`、`POST /admin/revisespu/status`、`POST /admin/deletespu`、`GET /admin/readai` |
| 支付返回页 | 支付宝 `return-url` 回跳的落地页 |

## 请求层的五个兼容点

后端接口有些「不合常规」的地方，`src/api.js` 里做了统一兜底：

1. **写接口全部是表单参数**（Controller 方法签名是散装参数，没有 `@RequestBody`），
   所以统一用 `application/x-www-form-urlencoded` 提交，用 JSON 提交后端会全部收到 `null`。
2. **登录拦截器**直接 write 出 `{"msg":"请先登录"}`，**没有 `code` 字段**，
   请求层识别到没有 `code` 就当作「未登录」抛出，路由守卫据此跳登录页。
3. **少数接口成功时直接 `return null`**（`/ai/upload`、`/admin/revisespu/status`），
   响应体为空且不是合法 JSON，请求层把它们列进白名单，空响应体归一化成成功。
4. **管理员拦截器判定无权限时什么都不写** —— `adminintercepter` 是 `return false`
   且不写响应体，所以非管理员访问 `/admin/**` 会拿到一个空响应体（HTTP 仍是 200，
   不是 403，也不是 JSON）。请求层因此把「不在白名单里的空响应体」识别为
   无权限错误，并打上 `forbidden` 标记，由页面做降级提示。
5. **`/ai/show/{id}` 的路径变量收不到** —— `aicontroller.show(String id)` 挂在
   `@GetMapping("/show/{id}")` 上，但方法参数**漏写了 `@PathVariable`**。Spring MVC 只有
   见到该注解才会把 URI 模板变量绑到参数上（官方文档：能省的只是注解里的名字，注解本身不能省），
   否则只把它当普通请求参数去找 —— 后端拿到的 `id` 就是 `null`，详情接口恒返回「暂无对应内容」。
   前端因此把 id **同时**拼进路径和查询串（`/ai/show/5?id=5`）：后端补上注解后走路径变量，
   没补也能从查询参数取到，两种写法都成立。根治仍需后端在参数上加 `@PathVariable`。

## 后端改动记录（前端已同步）

### 本轮（2026-09-23）

| 后端改动 | 前端对应调整 |
| --- | --- |
| **`aitask` 表新增 `content TEXT` 列** + `AiTask` 加 `content` 字段 + `RabbitMQConsumer` 生成后 `setContent(poem)` 再写回 | `AiCreate.vue` 轮询到「构建完成」直接渲染诗文（`<pre>` + 复制按钮）；「我的任务」每条可展开看历史诗文 |
| **新增 `GET /user/aitask`**（`usercontroller`）：身份**只认 Session，接口不接收任何用户名参数**，最新在前 | `api.js` 增 `listMyTasks()`（无参）；「我的任务」由「借 `/admin/readai` 拉全量再前端过滤」改为直连本接口，删掉 `forbidden` 降级分支 |
| `aitask.prompt` 由 `varchar(50)` 放宽为 `varchar(255)` | 无（原先 prompt 超 50 字符会 500，创作页的示例文案本身就超长） |
| `usercontroller.updateprofile`：改用户名时先查重（重名返回「用户名已存在」），改成功后 `session.setAttribute("user", 新名)` 同步登录态 | `Profile.vue` 删掉「改完请重新登录一次」的过时提示，保存成功后直接刷新资料，不再引导重登 |
| `loginconfign` 放行名单补上 `/pay/notify` | 无（服务端回调，前端不涉及） |
| `utils/MailUtil` 抽出 `sendToUsername(username, text)`，供拿不到 `HttpSession` 的回调场景复用 | 无 |
| `PayController.notify`：补支付宝 `rsaCheckV1` 验签、补幂等（订单已是「支付成功」直接返回 `success`）、补空值保护；`TRADE_SUCCESS` 时给绑定邮箱发购买成功邮件 | `PaySuccess.vue` 文案补充「已验签 + 幂等 + 会发通知邮件」 |
| `spucontroller` 新增公开接口 `GET /spu/list`（服务端过滤 `status = 1`） | 首页套餐列表由 `/admin/readspu` 换成 `/spu/list`，普通用户也能看到并购买套餐 |
| `PayController.notify` 的幂等判断写成 `order.getStatus() != null`，而 `Order.status` 是 `int`，**整个后端编译不过** | 顺手修掉（去掉多余的 `!= null`），否则前后端都起不来 |

### 上一轮

| 后端改动 | 前端对应调整 |
| --- | --- |
| `loginconfign` 放行名单补上 `/check/sendcode` | 邮箱验证码登录已可用，移除了登录页那条「发码会被拦」的提示 |
| `adminconfign` 由 `addPathPatterns("/admin")` 改为 `/admin/**` | 管理员角色校验真正生效；AI 创作「我的任务」对非管理员改为降级提示 |
| `PayController.notify` 增加「按套餐算力数给账号加算力」 | 支付返回页改为提示「算力由回调发放」 |
| `RandomNum` 加 `@Component` | 前端无影响（`controller` 里 `@Autowired RandomNum` 现在能注入了） |

## 仍然存在的后端问题（前端只能规避）

1. **`/ai/show/{id}` 漏写 `@PathVariable`** —— 路径变量收不到，详情接口恒返回「暂无对应内容」。
   前端已用「路径 + 查询参数双写」兜底，根治要在 `aicontroller.show` 的参数上加注解。
   见「请求层的五个兼容点」第 5 条。
2. **`/ai/poem` 先扣算力再插任务，且不在同一事务里** —— 实测：prompt 超长触发
   `Data too long`（HTTP 500）时，**算力已扣、任务却没建**，用户白扣 50 算力。
   建议把「扣算力 + 建任务」包进同一个 `@Transactional` 方法（或改为先插任务、后扣算力）。
3. `/pay/notify` 的幂等只判断 `status == 2`：若同一订单先收到 `TRADE_SUCCESS`（已加算力），
   再收到 `TRADE_FINISHED` 把状态覆盖成 4，之后万一又来一次 `TRADE_SUCCESS`，会**再加一次算力**。
   建议把幂等条件放宽为「已经加过算力」（例如 `status >= 2`），或单独记录发放标记。
4. `/pay` 是「先建订单再调支付宝」，支付宝侧失败会留下 `status = 0` 的孤儿订单；
   回调也没有核对金额与订单是否一致（`order` 表本身没存金额）。
5. `GET /user/profile` 把整个 `User` 返回给前端，**包含明文 `password` 字段**。
   页面没有渲染它，但浏览器网络面板能看到，建议后端脱敏。
6. `/ai/upload` 成功时直接 `return null`（无响应体）；`/admin/revisespu/status` 则是
   「更新行数 > 0 才返回 `Result`」，所以它的空响应体代表失败，前端按失败提示。
7. 作品分类 `sortid` 没有字典表，`src/config.js` 里的分类名是占位文本，按实际语义改即可。
8. `/check/register` 没有校验用户名/密码是否为空，前端已做非空与两次密码一致校验。
9. `/check/sign` 的签到逻辑非原子（先查 Redis 再改库），高并发下可能重复加算力。
10. 作品广场（`ai_production`）和写诗任务（`aitask`）是两条互不相通的线：诗生成后
    仍需手动复制粘贴到「发布我的作品」才能进广场，没有「任务 → 作品」的一键通道。

## 首次使用建议

1. 首页套餐走公开接口 `GET /spu/list`，**普通账号就能浏览与下单**，联调商城不必再用管理员账号。
2. 管理后台「套餐管理」新增套餐 → 点「上架」，首页套餐列表才会出现。
3. 算力为 0 时先在首页或个人中心点「每日签到 +100」。
4. 个人中心「绑定邮箱」之后，才能用邮箱验证码方式登录，也才能收到购买成功通知邮件。
5. 要进管理后台，需先把某个账号的 `role` 改成 `admin`（直接改库）。
6. 库里现成测试账号：`ccc`（普通用户）、`cyr`（管理员），密码均为 `123456`。
