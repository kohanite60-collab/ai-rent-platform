# AI 算力租赁平台 · 前端

Vite + Vue 3 + vue-router 的纯前端项目，对接根目录下的 Spring Boot 后端（端口 `8080`）。
**没有修改任何后端代码**，所有交互都走后端已有接口。

## 启动方式

先启动后端（Spring Boot 需在 `8080` 端口，即 `application.yml` 里默认端口），再启动前端：

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
| 首页算力套餐 | `GET /admin/readspu`（前端筛 `status === 1`，⚠️ 该接口现需管理员）、`POST /pay`、`POST /check/sign` |
| 个人中心 | `GET /user/profile`、`POST /user/profile`、`POST /user/email`、`POST /check/sign` |
| AI 创作 | `POST /ai/poem`、`GET /ai/status?taskNo=`（2 秒轮询）、`GET /admin/readai`（⚠️ 该接口现需管理员，前端按用户过滤） |
| 作品广场 | `GET /ai/show/list?sortid=`、`GET /ai/show/{id}`、`POST /ai/upload` |
| 管理后台 | `GET /admin/read`、`POST /admin/revise`、`GET /admin/readspu`、`POST /admin/addspu`、`POST /admin/revisespu`、`POST /admin/revisespu/status`、`POST /admin/deletespu`、`GET /admin/readai` |
| 支付返回页 | 支付宝 `return-url` 回跳的落地页 |

## 请求层的四个兼容点

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

此外 `AiProduction.id` 定义的是 `int`，而 `/ai/show/{id}` 的路径参数被声明成 `String`，
Spring 会正常转换，前端直接拼在路径上即可。

## 后端改动记录（前端已同步）

最近一轮后端小改，前端跟着调整了这几处：

| 后端改动 | 前端对应调整 |
| --- | --- |
| `loginconfign` 放行名单补上 `/check/sendcode` | 邮箱验证码登录已可用，移除了登录页那条「发码会被拦」的提示 |
| `adminconfign` 由 `addPathPatterns("/admin")` 改为 `/admin/**` | 管理员角色校验真正生效；首页套餐列表、AI 创作「我的任务」对非管理员改为降级提示 |
| `PayController.notify` 路径确认为 `/pay/notify`（与 `application.yml` 的 `notify-url` 一致） | 支付返回页文案同步 |
| `PayController.notify` 增加「按套餐算力数给账号加算力」 | 支付返回页改为提示「算力由回调发放」，并提示确认 `notify-url` 可回访 |
| `RandomNum` 加 `@Component` | 前端无影响（`controller` 里 `@Autowired RandomNum` 现在能注入了） |

## 仍然存在的后端问题（前端只能规避）

1. **`/pay/notify` 的加算力逻辑有三个 bug**（本轮新增代码）：
   - 查用户用的是 `queryWrapper.gt("username", username)`，`gt` 是「大于」，
     条件会变成 `username > 'xxx'`，**匹配到的不是订单本人**，会加错账号；应为 `eq`。
   - 没有按 `trade_status` 判断，`WAIT_BUYER_PAY` / `TRADE_CLOSED` 也会加算力。
   - 没有幂等控制，支付宝会重复投递通知，**可能重复加算力**；
     另外 `userMapper.selectOne(...)` 查不到时会 NPE。
2. **管理员权限生效后，普通用户拿不到套餐和任务**：
   `/admin/readspu` 现在要求 `admin`，而商城需要给普通用户看套餐；
   `/admin/readai` 同理，普通用户看不到自己的任务记录。
   需要后端补公开/用户级接口（如 `GET /spu/list`、`GET /ai/task/list`），
   或在放行名单里排除这两个路径。**这是目前最影响可用性的一条。**
3. `RabbitMQConsumer` 只回写任务状态，生成的 `poem` 没有落库，`AiTask` 也无结果字段，
   AI 创作页能显示进度但拿不到诗句。需要加字段并保存。
4. `/ai/upload` 成功时直接 `return null`（无响应体）；`/admin/revisespu/status` 则是
   「更新行数 > 0 才返回 `Result`」，所以它的空响应体代表失败，前端按失败提示。
5. `/user/profile` 修改用户名后，Session 里仍是旧用户名，会被拦截器判为「用户不存在」，
   前端在改用户名的场景下直接引导重新登录。
6. 作品分类 `sortid` 没有字典表，`src/config.js` 里的分类名是占位文本，按实际语义改即可。
7. `/check/register` 没有校验用户名/密码是否为空，前端已做非空与两次密码一致校验。

## 首次使用建议

1. 先给一个账号的 `role` 设成 `admin`（直接改库，或用已有管理员账号）。
   **当前套餐列表只有管理员能拉到**，所以联调商城与支付也要用管理员账号。
2. 管理后台「套餐管理」新增套餐 → 点「上架」，首页套餐列表才会出现。
3. 算力为 0 时先在首页或个人中心点「每日签到 +100」。
4. 个人中心「绑定邮箱」之后，才能用邮箱验证码方式登录。
