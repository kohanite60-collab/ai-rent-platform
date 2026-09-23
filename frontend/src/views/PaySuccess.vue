<template>
  <div class="pay-wrap">
    <div class="card pay-card">
      <div class="check">✓</div>
      <h2>支付流程已跳转完成</h2>
      <p class="muted">
        你刚从支付宝收银台返回。订单状态由支付宝异步通知 <code>/pay/notify</code> 写回
        <code>order</code> 表（未付款=1、支付成功=2、交易关闭=3、交易完成=4）。
      </p>
      <p class="faint">
        算力会由支付宝异步通知回调发放，按订单对应套餐的算力数量增加账号余额。
        若长时间未到账，先确认 <code>notify-url</code> 能被支付宝回访（本地需要内网穿透），
        再看后端回调日志里的 <code>trade_status</code>。
      </p>
      <div class="btn-row" style="justify-content: center; margin-top: 22px">
        <router-link to="/" class="btn btn-primary">返回算力套餐</router-link>
        <router-link v-if="isLoggedIn" to="/profile" class="btn">查看个人中心</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { isLoggedIn } from '../store/user'
</script>

<style scoped>
.pay-wrap {
  display: flex;
  justify-content: center;
  padding-top: 40px;
}

.pay-card {
  max-width: 520px;
  text-align: center;
  padding: 34px 30px;
}

.check {
  width: 54px;
  height: 54px;
  margin: 0 auto 18px;
  border-radius: 50%;
  background: var(--success-soft);
  border: 1px solid #c6ecd5;
  color: var(--success);
  font-size: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pay-card p {
  margin-top: 12px;
}

code {
  background: rgba(0, 0, 0, 0.05);
  padding: 1px 5px;
  border-radius: 5px;
  font-size: 12px;
}
</style>
