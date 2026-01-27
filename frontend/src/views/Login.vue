<template>
  <div class="login-container">
    <div class="login-box">
      <h2>登录</h2>
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label for="account">账号</label>
          <input 
            type="text" 
            id="account" 
            v-model="form.account" 
            placeholder="请输入账号"
            required
          >
        </div>
        
        <div class="form-group">
          <label for="password">密码</label>
          <input 
            type="password" 
            id="password" 
            v-model="form.password" 
            placeholder="请输入密码"
            required
          >
        </div>
        
        <div class="form-group">
          <button type="submit" :disabled="loading">
            {{ loading ? '登录中...' : '登录' }}
          </button>
        </div>
        
        <div class="form-links">
          <router-link to="/register">还没有账号？去注册</router-link>
        </div>
      </form>
      
      <div v-if="error" class="error-message">
        {{ error }}
      </div>
    </div>
  </div>
</template>

<script>
import md5 from 'md5';

export default {
  name: 'Login',
  data() {
    return {
      form: {
        account: '',
        password: ''
      },
      loading: false,
      error: ''
    }
  },
  methods: {
    async handleLogin() {
      this.loading = true;
      this.error = '';
      
      try {
        const response = await this.$http.post('/api/auth/login', {
          account: this.form.account,
          password: md5(this.form.password)
        });
        
        if (response.data.code === 200) {
          const { token, user } = response.data.data;
          localStorage.setItem('token', token);
          localStorage.setItem('user', JSON.stringify(user));
          this.$http.defaults.headers['Authorization'] = `Bearer ${token}`;
          this.$router.push('/dashboard');
        } else {
          this.error = response.data.message;
        }
      } catch (error) {
        this.error = error.response?.data?.message || '登录失败，请重试';
      } finally {
        this.loading = false;
      }
    }
  },
  created() {
    const token = localStorage.getItem('token');
    if (token) {
      this.$http.defaults.headers['Authorization'] = `Bearer ${token}`;
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f5f5;
}

.login-box {
  width: 100%;
  max-width: 400px;
  padding: 40px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.login-box h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #666;
}

.form-group input {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-group input:focus {
  outline: none;
  border-color: #409EFF;
}

.form-group button {
  width: 100%;
  padding: 12px;
  background-color: #409EFF;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
}

.form-group button:disabled {
  background-color: #a0cfff;
  cursor: not-allowed;
}

.form-group button:hover:not(:disabled) {
  background-color: #66b1ff;
}

.form-links {
  text-align: center;
  margin-top: 20px;
}

.form-links a {
  color: #409EFF;
  text-decoration: none;
}

.error-message {
  margin-top: 20px;
  padding: 10px;
  background-color: #fef0f0;
  color: #f56c6c;
  border-radius: 4px;
  text-align: center;
}
</style>