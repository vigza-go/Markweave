<template>
  <div class="dashboard">
    <header class="header">
      <div class="logo">MarkWeave</div>
      <div class="user-info">
        <span class="nickname">{{ user.nickname || user.account }}</span>
        <button @click="handleLogout" class="logout-btn">退出登录</button>
      </div>
    </header>
    
    <main class="main-content">
      <h1>欢迎回来！</h1>
      <p>您已成功登录MarkWeave协同编辑平台。</p>
      <div class="features">
        <div class="feature-card">
          <h3>📝 文档编辑</h3>
          <p>创建和编辑您的Markdown文档</p>
        </div>
        <div class="feature-card">
          <h3>👥 协同编辑</h3>
          <p>与团队成员实时协作</p>
        </div>
        <div class="feature-card">
          <h3>📁 文件管理</h3>
          <p>管理您的文档和文件夹</p>
        </div>
      </div>
    </main>
  </div>
</template>

<script>
export default {
  name: 'Dashboard',
  data() {
    return {
      user: {}
    }
  },
  methods: {
    handleLogout() {
      this.$http.post('/api/auth/logout')
        .then(() => {
          localStorage.removeItem('token');
          localStorage.removeItem('user');
          delete this.$http.defaults.headers['Authorization'];
          this.$router.push('/login');
        })
        .catch(error => {
          console.error('登出失败:', error);
          localStorage.removeItem('token');
          localStorage.removeItem('user');
          this.$router.push('/login');
        });
    },
    loadUserInfo() {
      const userStr = localStorage.getItem('user');
      if (userStr) {
        this.user = JSON.parse(userStr);
      }
    }
  },
  created() {
    this.loadUserInfo();
  }
}
</script>

<style scoped>
.dashboard {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 40px;
  background-color: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.logo {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 20px;
}

.nickname {
  color: #333;
}

.logout-btn {
  padding: 8px 16px;
  background-color: #f56c6c;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.logout-btn:hover {
  background-color: #f78989;
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px;
}

.main-content h1 {
  color: #333;
  margin-bottom: 10px;
}

.main-content p {
  color: #666;
  margin-bottom: 40px;
}

.features {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
}

.feature-card {
  background: white;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.feature-card h3 {
  color: #409EFF;
  margin-bottom: 10px;
}

.feature-card p {
  color: #666;
  margin-bottom: 0;
}
</style>