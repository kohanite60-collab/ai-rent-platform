/**
 * 统一请求层
 *
 * 后端所有写接口都是「普通表单参数」绑定（没有 @RequestBody），
 * 所以这里统一用 application/x-www-form-urlencoded 提交，
 * 而不是 JSON —— 否则后端参数会全部收到 null。
 *
 * 响应体统一是 Result{code,msg,data}，但有几种特殊情况要兼容：
 *   1. 登录拦截器直接写 {"msg":"请先登录"}，没有 code 字段；
 *   2. /ai/upload 成功时直接 return null，响应体为空；
 *      /admin/revisespu/status 是「更新行数 > 0 才返回 Result」，空响应体其实代表失败；
 *   3. adminintercepter 判定无权限时直接 return false，既不写响应体也不是 JSON，
 *      只能靠「空响应体」来识别（HTTP 仍是 200），所以要做白名单区分；
 *   4. /pay 返回的是一段支付宝表单 HTML，不是 JSON；
 *   5. **后端抛未捕获异常时**，Spring forward 到 /error，返回的是
 *      {"timestamp","status","error","path","trace"}，同样没有 code 字段。
 *      它和第 1 条长得很像，但语义完全相反：一个是「没登录」，一个是「后端炸了」。
 *      必须靠 HTTP 状态码 + 有无 status/error 字段区分开，否则真实错误会被
 *      伪装成「请先登录」（曾把「Redis 连不上」显示成「请先登录」）。
 */

const BASE = '/api'

/** 这些接口成功时响应体本来就是空的（无条件 return null），直接当成功 */
const EMPTY_BODY_IS_OK = ['/ai/upload']

/**
 * 这个接口是「更新行数 > 0 才返回 Result，否则 return null」，
 * 所以空响应体代表更新 0 行（状态没变化或 id 不存在），应当算失败。
 */
const EMPTY_BODY_IS_FAILURE = { '/admin/revisespu/status': '状态未发生变化，或该套餐不存在' }

export class ApiError extends Error {
  constructor(message, code) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

/**
 * 后端抛了未捕获异常时的统一错误。
 *
 * 触发场景：Spring 把异常 forward 到 /error，返回
 * {"timestamp","status","error","path","trace"} —— **没有 Result 的 code 字段**。
 * 以前这种响应会被下面「没有 code 就当未登录」的分支吃掉，
 * 于是「Redis 连不上」这类 500 在界面上显示成「请先登录」，非常误导。
 * 现在单独识别，并把 serverError 打上标记，页面可以据此给出「去检查后端」的提示。
 */
function serverError(status, detail) {
  const err = new ApiError(
    `后端服务异常（HTTP ${status}）：多半是后端依赖不可用（Redis / RabbitMQ / 数据库），请查看后端控制台日志`,
    status,
  )
  err.serverError = true
  err.detail = detail || ''
  return err
}

function toQuery(params) {
  const usp = new URLSearchParams()
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return
    usp.append(key, String(value))
  })
  return usp.toString()
}

async function request(path, { method = 'GET', params, form, raw = false } = {}) {
  let url = BASE + path
  const qs = toQuery(params)
  if (qs) url += '?' + qs

  const options = { method, credentials: 'include', headers: {} }
  if (form) {
    options.headers['Content-Type'] = 'application/x-www-form-urlencoded;charset=UTF-8'
    options.body = toQuery(form)
  }

  let res
  try {
    res = await fetch(url, options)
  } catch (e) {
    throw new ApiError('无法连接到后端服务，请确认 Spring Boot 已启动在 8080 端口')
  }

  const text = await res.text()

  // 直接返回原始文本（支付表单）。但 5xx 时后端返回的是错误 JSON 而不是
  // 支付宝表单 HTML，不能当成功往下传。
  if (raw) {
    if (res.status >= 500) throw serverError(res.status, text)
    return text
  }

  // 空响应体
  if (!text) {
    // 0) HTTP 5xx 且无响应体：服务端异常，不能误判成「无权限」
    if (res.status >= 500) throw serverError(res.status, '')

    // 1) 白名单内的接口成功时就是空的，直接当成功
    if (EMPTY_BODY_IS_OK.includes(path)) return { code: 200, msg: 'success', data: null }

    // 2) 已知「空 = 操作没生效」的接口，抛业务错误
    if (EMPTY_BODY_IS_FAILURE[path]) throw new ApiError(EMPTY_BODY_IS_FAILURE[path])

    // 3) 其余情况：被 adminintercepter 拦下时后端什么都不写，只能这样识别
    const err = new ApiError('没有访问权限，该接口要求管理员身份', 403)
    err.forbidden = true
    throw err
  }

  let json
  try {
    json = JSON.parse(text)
  } catch (e) {
    throw new ApiError(`接口返回了非 JSON 内容（HTTP ${res.status}），可能是后端异常`)
  }

  // 没有 code 字段 -> 不是 Result 结构，只可能是下面两种之一，必须区分清楚：
  if (json.code === undefined) {
    // (a) Spring 的 /error 响应体（带 status + error + trace）：后端抛了未捕获异常。
    //     这条必须排在「未登录」判断之前，否则真实错误会被伪装成「请先登录」。
    if (json.status && json.error) throw serverError(json.status, json.message)

    // (b) 登录拦截器直接 write 的 {"msg":"请先登录"}（HTTP 200，无 code）
    const err = new ApiError(json.msg || '登录状态已失效，请重新登录', res.status)
    err.unauthorized = true
    throw err
  }

  if (json.code !== 200) throw new ApiError(json.msg || '请求失败', json.code)

  return json
}

/* ---------------- 认证 /check ---------------- */

export const loginByPassword = (username, password) =>
  request('/check/login', { method: 'POST', form: { type: 1, username, password } })

export const loginByEmail = (email, code) =>
  request('/check/login', { method: 'POST', form: { type: 2, email, code } })

/** 发邮箱验证码。后端已把 /check/sendcode 加入放行名单，未登录也可调用 */
export const sendCode = (email) => request('/check/sendcode', { params: { email } })

export const register = (username, password) =>
  request('/check/register', { method: 'POST', form: { username, password } })

export const logout = () => request('/check/logout', { method: 'POST' })

/** 每日签到 +100 算力 */
export const signIn = () => request('/check/sign', { method: 'POST' })

/* ---------------- 用户 /user ---------------- */

export const getProfile = () => request('/user/profile')

export const updateProfile = (username, password) =>
  request('/user/profile', { method: 'POST', form: { username, password } })

export const bindEmail = (email) => request('/user/email', { method: 'POST', form: { email } })

/* ---------------- AI /ai ---------------- */

/** 提交写诗任务（扣 50 算力），返回 taskNo */
export const createPoem = (prompt) => request('/ai/poem', { method: 'POST', form: { prompt } })

export const getTaskStatus = (taskNo) => request('/ai/status', { params: { taskNo } })

/**
 * 查询当前登录用户自己的 AI 任务（含生成的诗文）。
 *
 * 身份完全由后端 Session 决定，前端不传任何身份参数 —— 后端也就没有
 * 「传别人的名字遍历他人记录」的口子。
 * 它替代了此前「借 /admin/readai 拉全量再前端过滤」的绕法 —— 那个接口要求 admin，
 * 普通用户会被 adminintercepter 拦下只剩空响应体。
 */
export const listMyTasks = () => request('/user/aitask')

export const listProductions = (sortid) => request('/ai/show/list', { params: { sortid } })

/**
 * 作品详情。
 *
 * 后端 aicontroller.show(String id) 挂在 @GetMapping("/show/{id}") 上，但方法参数没有写
 * @PathVariable —— Spring 只有见到该注解才会绑定路径变量，否则只按「请求参数」找 id，
 * 所以「只拼路径」时后端收到的 id 是 null，详情页恒为空。
 * 这里把 id 同时作为查询参数带上：后端补了注解走路径、没补就走查询参数，两种都能取到。
 */
export const getProduction = (id) =>
  request(`/ai/show/${encodeURIComponent(id)}`, { params: { id } })

export const uploadProduction = (title, data, sortid) =>
  request('/ai/upload', { method: 'POST', form: { title, data, sortid } })

/* ---------------- 套餐 /spu ----------------
 * 用户侧只读接口，公开（已在后端放行名单里），未登录也能浏览套餐。
 * 后端在服务端就过滤掉了 status = 0 的下架套餐，前端不必再筛。
 * 管理后台要看全部（含下架）请用下面的 adminListSpus。
 */

export const listSpus = () => request('/spu/list')

/* ---------------- 管理端 /admin ----------------
 * 后端已把 adminconfign 的拦截路径从 "/admin" 改成 "/admin/**"，
 * 现在这一组接口都要求 role == "admin"，非管理员会被拦下（空响应体）。
 * 用户侧不再调用这里的接口：「我的任务」已改用 /user/aitask（见上）。
 */

export const adminListUsers = () => request('/admin/read')

export const adminReviseUser = (id, role, money) =>
  request('/admin/revise', { method: 'POST', form: { id, role, money } })

export const adminListTasks = () => request('/admin/readai')

export const adminListSpus = () => request('/admin/readspu')

export const adminAddSpu = (name, money, rmb) =>
  request('/admin/addspu', { method: 'POST', form: { name, money, rmb } })

export const adminReviseSpu = (id, name, money, rmb) =>
  request('/admin/revisespu', { method: 'POST', form: { id, name, money, rmb } })

export const adminSetSpuStatus = (id, status) =>
  request('/admin/revisespu/status', { method: 'POST', form: { id, status } })

export const adminDeleteSpu = (id) =>
  request('/admin/deletespu', { method: 'POST', form: { id } })

/* ---------------- 支付 ---------------- */

/** 返回支付宝表单 HTML 字符串 */
export const payOrder = (spuId) => request('/pay', { method: 'POST', form: { spuId }, raw: true })
