<template>
  <nav
      :data-islogin="global.isLogin? 'true' : 'false'"
      class="navbar navbar-expand-md bg-color-white fixed-top"
  >
    <div class="nav-body">
      <div class="nav-logo-wrap-lg">
        <a class="navbar-logo-wrap" href="/">
          <img class="logo hidden-when-screen-small" src="/src/assets/static/img/logo.png"/>
          <!--          <img src="/src/assets/static/img/icon.png" class="logo-lg display-when-screen-small" alt="" />-->
        </a>

        <el-dropdown :hide-on-click="false" class="display-when-screen-small center-content">
          <a class="el-dropdown-link nav-link display-when-screen-small">
            首页
          </a>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item><a class="dropdown-item" href="/">首页</a></el-dropdown-item>
              <el-dropdown-item><a class="dropdown-item" href="/column">教程</a></el-dropdown-item>
              <el-dropdown-item><a class="dropdown-item" href="/chat">LLM</a></el-dropdown-item>
              <el-dropdown-item><a class="dropdown-item" href="/about">关于作者</a></el-dropdown-item>
              <el-dropdown-item><a class="dropdown-item" href="/plan">更新计划</a></el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      <div class="navbar-collapse hidden-when-screen-small">
        <ul class="navbar-nav">
          <el-space size="small">
            <li :class="{'selected-domain': activeTab == '/'}">
              <a class="nav-link" href="/">首页</a>
            </li>
            <li :class="{'selected-domain': activeTab == '/column'}">
              <a class="nav-link" href="/column">教程</a>
            </li>
            <li :class="{'selected-domain': activeTab == '/about'}">
              <a class="nav-link" href="/about">关于作者</a>
            </li>
            <li :class="{'selected-domain': activeTab == '/chat'}">
              <a class="nav-link" href="/chat">LLM</a>
            </li>
            <li class="max-lg:hidden" :class="{'selected-domain': activeTab == '/plan'}">
              <a class="nav-link" href="/plan">更新计划</a>
            </li>
          </el-space>
        </ul>
      </div>
      <div class="nav-right">
        <button
            v-if="!route.path.includes('/article/edit') && route.path !== '/article/edit/' && global.isLogin"
            type="button"
            class="btn btn-primary nav-article"
            @click="writeArticle"
        >
          写文章
        </button>
        <button
            v-else-if="route.path.includes('/article/edit') || route.path === '/article/edit/'"
            type="button"
            class="btn btn-primary nav-article"
            @click="router.push('/')"
        >
          返回主页
        </button>
        <ul v-if="!global.isLogin">
          <!--  待登录 -->
          <li class="nav-item">
            <el-button @click="loginButton">登录</el-button>
          </li>
        </ul>
        <ul v-if="global.isLogin" class="nav-right-user">
          <!--  已登录 -->
          <li class="nav-item nav-notice">
            <a class="nav-link navbar-count-msg-box" href="/notice/">
                <span
                    v-if="global.msgNum != null && global.msgNum > 0"
                    class="navbar-count-msg"
                >
                  {{global.msgNum}}
                </span>
              <!-- 消息提醒的角标 -->
              <svg
                  xmlns="http://www.w3.org/2000/svg"
                  class="icon"
                  width="24"
                  height="24"
                  viewBox="0 0 24 24"
                  stroke-width="2"
                  stroke="currentColor"
                  fill="none"
                  stroke-linecap="round"
                  stroke-linejoin="round"
              >
                <path stroke="none" d="M0 0h24v24H0z" fill="none"></path>
                <path
                    d="M10 5a2 2 0 0 1 4 0a7 7 0 0 1 4 6v3a4 4 0 0 0 2 3h-16a4 4 0 0 0 2 -3v-3a7 7 0 0 1 4 -6"
                ></path>
                <path d="M9 17v1a3 3 0 0 0 6 0v-1"></path>
              </svg>
            </a>
          </li>

          <!-- 头像框 -->

          <li class="nav-right-user center-content">
            <el-dropdown :hide-on-click="false">
              <div style="display: flex">
                <img
                    class="nav-login-img"
                    style="border-radius: 50%"
                    :src="global.user.photo? global.user.photo : 'https://static.developers.pub/static/img/logo.b2ff606.jpeg'"
                    alt=""
                    loading="lazy"
                />
                <div class="center-content m-2"><el-icon size="15"><ArrowDownBold /></el-icon></div>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item><span @click="personalPage"> 个人主页 </span></el-dropdown-item>
                  <el-dropdown-item><span @click="toolsPage"> 工具 </span></el-dropdown-item>
                  <el-dropdown-item><span @click="logout"> 登出 </span></el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </li>
        </ul>
      </div>
    </div>
  </nav>
  <!--  登录对话框 -->
</template>

<script setup lang="ts">
import { inject, onMounted, ref, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
const router = useRouter()
const route = useRoute()
import { doGet} from '@/http/BackendRequests'
import {
  type CommonResponse,
} from '@/http/ResponseTypes/CommonResponseType'
import { useGlobalStore } from '@/stores/global'
const globalStore = useGlobalStore()

const global = globalStore.global

// SSE 相关变量
let eventSource: EventSource | null = null;

import { BASE_URL, SSE_NOTIFICATIONS_URL } from '@/http/URL'; // 导入 BASE_URL 和 SSE_NOTIFICATIONS_URL

// 连接 SSE 的函数
const connectSse = () => {
  if (eventSource) {
    eventSource.close(); // 如果存在旧连接，先关闭
  }

  // 确保用户已登录才尝试连接SSE
  if (!global.isLogin) {
    console.log("用户未登录，不建立 SSE 连接。");
    return;
  }

  eventSource = new EventSource(BASE_URL + SSE_NOTIFICATIONS_URL, {
    withCredentials: true // 允许传递 Cookie 和 HTTP 认证信息
  }); // 使用完整的后端地址

  eventSource.onopen = () => {
    console.log('SSE 连接已建立。');
  };

  // 统一处理 SSE 消息的函数
  const handleSseData = (event: MessageEvent) => {
    console.log('收到 SSE 消息:', event.data, '事件类型:', event.type);
    try {
      const data = JSON.parse(event.data);
      if (event.type === 'newNotification' && data && typeof data.notificationCount === 'number') {
        global.msgNum = data.notificationCount;
      } else {
        console.warn('收到的 SSE 消息数据格式不正确或类型不匹配:', event.data, '事件类型:', event.type);
      }
    } catch (e) {
      console.error('解析 SSE 消息数据失败:', e, '原始数据:', event.data);
      // Fallback for non-JSON data, though based on your image, it should be JSON for newNotification
      const newMsgNum = parseInt(event.data, 10);
      if (!isNaN(newMsgNum)) {
        global.msgNum = newMsgNum;
      }
    }
  };

  // 为默认的 'message' 事件类型添加监听器 (如果后端发送不带event字段的消息)
  eventSource.onmessage = handleSseData;

  // 根据 Network tab 显示的自定义事件类型添加监听器
  eventSource.addEventListener('newNotification', handleSseData);
  // eventSource.addEventListener('test', handleSseData);

  eventSource.onerror = (error) => {
    console.error('SSE 连接错误:', error);
    eventSource?.close(); // 关闭当前连接
    eventSource = null; // 清除实例

    // 简单的重连机制，生产环境可能需要更复杂的指数退避策略
    setTimeout(() => {
      console.log('尝试重新连接 SSE...');
      connectSse(); // 尝试重新连接
    }, 5000); // 5秒后重试
  };
};

// ======= 跳转到写文章 ==========
const writeArticle = () => {
  if(route.fullPath.includes("/article/edit")){
    window.location.reload()
  }else{
    router.push("/article/edit")
  }
}


// 确定当前选中的标签页是什么
const activeTab = ref('/')

onMounted(() => {
  activeTab.value = router.currentRoute.value.path
  console.log(activeTab.value)
  // connectSse(); // 移除直接调用，交由 watch 处理

  // 监听 global.isLogin 的变化，当登录状态变为 true 时，尝试建立 SSE 连接
  watch(() => global.isLogin, (newVal) => {
    if (newVal) {
      console.log("global.isLogin 变为 true，尝试连接 SSE。");
      connectSse();
    }
  }, { immediate: true }); // immediate: true 确保在组件挂载时立即执行一次回调，如果 global.isLogin 初始就是 true
})

onBeforeUnmount(() => {
  if (eventSource) {
    eventSource.close();
    console.log('SSE 连接已关闭。');
  }
});

const handleMessage = (event: MessageEvent) => {
// 在这里处理收到的消息
  console.log("Message received:", event.data);
};

const registerModal = ref(false)

import { messageTip, refreshPage, sleep } from '@/util/utils'
import { MESSAGE_TYPE } from '@/constants/MessageTipEnumConstant'
import { ArrowDownBold } from '@element-plus/icons-vue'
import { LOGOUT_URL } from '@/http/URL'

// 登录框的激活
const showLoginDialog = inject<() => void>('loginDialogClicked')

const loginButton = () => {
  if(showLoginDialog)
    showLoginDialog()
  else
    console.error("请先登录")
}

// ==========个人主页==========
const personalPage = () => {
  console.log(route.fullPath)
  if(route.fullPath.includes('/user/' + global.user.userId)){
    messageTip("已经在个人主页了", MESSAGE_TYPE.INFO)
    return
  }
  if(global.user.userId != route.params['userId']){
    router.push(global.user.userId? '/user/' + global.user.userId: '/login')
        .then(() => {
          window.location.reload()
        })
  }else{
    router.push(global.user.userId? '/user/' + global.user.userId: '/login')
  }
}

// ==========工具主页==========
const toolsPage = () => {
  console.log(route.fullPath)
  if(route.fullPath.includes('/tools/')){
    messageTip("已经在工具页了", MESSAGE_TYPE.INFO)
    return
  }
  router.push('/tools/')
}

// ==========退出登录==========
const logout = () => {
  console.log("退出登录")
  doGet<CommonResponse>(LOGOUT_URL, {})
      .then((response) => {
        if(response.data.status.code === 0){
          messageTip("退出登录成功", MESSAGE_TYPE.SUCCESS)
          sleep(1)
          console.log(response.data)
          // router.replace('/')
          refreshPage()
        }})
      .catch((error) => {
        console.error(error)
      })
}

</script>


<style scoped>

.wx-login-span-info{
  font-weight: bold;
  font-size: small;
  line-height: 10px;
}

.dropdown-item{
  margin: 5px;
}

.other-login {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 20px;
}

.other-login span {
  margin-right: 10px;
}

.qr-login img {
  width: 150px;
  height: 150px;
}

.qr-login .code {
  color: red;
  font-weight: bold;
}

span.wx-login-span-info{
  margin: 10px;
}

#login-agreement-message{
  margin: 10px
}

.el-main{
  padding: 0;
}

</style>
