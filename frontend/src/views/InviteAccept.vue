<template>
  <div class="invite-page">
    <div class="card">
      <h2>协作邀请</h2>
      <p>{{ message }}</p>
      <el-button type="primary" :loading="loading" @click="accept">接受邀请</el-button>
      <el-button @click="$router.push('/dashboard')">返回首页</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { collaborationService } from '@/services';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const message = ref('点击按钮接受此文档协作邀请。');

const accept = async () => {
  loading.value = true;
  try {
    const response = await collaborationService.acceptInvitation(route.params.token);
    if (response.code === 200) {
      ElMessage.success('已加入协作');
      router.push('/dashboard');
    } else {
      ElMessage.error(response.message || '接受邀请失败');
      message.value = '邀请可能已失效，请联系邀请人重新生成链接。';
    }
  } catch (error) {
    ElMessage.error('接受邀请失败: ' + (error.response?.data?.message || error.message));
    message.value = '邀请处理失败，请稍后重试。';
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.invite-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #f3f4f6; }
.card { background: white; border-radius: 12px; padding: 24px; width: 420px; box-shadow: 0 10px 30px rgba(0,0,0,.08); display: flex; flex-direction: column; gap: 12px; }
</style>
