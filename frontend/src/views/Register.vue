<template>
  <div class="register-container">
    <div class="register-box">
      <h2>注册</h2>
      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label for="account">账号</label>
          <input 
            type="text" 
            id="account" 
            v-model="form.account" 
            placeholder="请输入3-20位账号"
            @blur="checkAccount"
            required
          >
          <span v-if="accountExists" class="validation-error">该账号已被注册</span>
        </div>
        
        <div class="form-group">
          <label for="nickname">昵称</label>
          <input 
            type="text" 
            id="nickname" 
            v-model="form.nickname" 
            placeholder="请输入昵称（可选）"
          >
        </div>
        
        <div class="form-group">
          <label for="password">密码</label>
          <input 
            type="password" 
            id="password" 
            v-model="form.password" 
            placeholder="请输入6-20位密码，需包含大小写字母和数字"
            required
          >
        </div>
        
        <div class="form-group">
          <label for="confirmPassword">确认密码</label>
          <input 
            type="password" 
            id="confirmPassword" 
            v-model="form.confirmPassword" 
            placeholder="请再次输入密码"
            required
          >
          <span v-if="passwordMismatch" class="validation-error">两次输入的密码不一致</span>
        </div>
        
        <div class="form-group">
          <button type="submit" :disabled="loading || accountExists">
            {{ loading ? '注册中...' : '注册' }}
          </button>
        </div>
        
        <div class="form-links">
          <router-link to="/login">已有账号？去登录</router-link>
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
  name: 'Register',
  data() {
    return {
      form: {
        account: '',
        nickname: '',
        password: '',
        confirmPassword: ''
      },
      loading: false,
      error: '',
      accountExists: false
    }
  },
  computed: {
    passwordMismatch() {
      return this.form.confirmPassword && 
             this.form.password !== this.form.confirmPassword;
    }
  },
  methods: {
    
    async handleRegister() {
      if (this.passwordMismatch) {
        this.error = '两次输入的密码不一致';
        return;
      }

      this.loading = true;
      this.error = '';
      
      try {
        const response = await this.$http.post('/api/auth/register', {
          account: this.form.account,
          password: md5(this.form.password),
          confirmPassword: md5(this.form.confirmPassword),
          nickname: this.form.nickname
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
        this.error = error.response?.data?.message || '注册失败，请重试';
      } finally {
        this.loading = false;
      }
    }
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f5f5;
}

.register-box {
  width: 100%;
  max-width: 400px;
  padding: 40px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.register-box h2 {
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
  background-color: #67c23a;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
}

.form-group button:disabled {
  background-color: #b3e19d;
  cursor: not-allowed;
}

.form-group button:hover:not(:disabled) {
  background-color: #85ce61;
}

.form-links {
  text-align: center;
  margin-top: 20px;
}

.form-links a {
  color: #409EFF;
  text-decoration: none;
}

.validation-error {
  color: #f56c6c;
  font-size: 12px;
  display: block;
  margin-top: 5px;
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