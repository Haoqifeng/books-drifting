<template>
  <div class="login">
    <h2>校园闲置书籍漂流系统</h2>
    <div class="login-form">
      <h3>用户登录</h3>
      <form @submit.prevent="login">
        <div class="form-group">
          <label for="username">用户名/学号</label>
          <input type="text" id="username" v-model="form.username" required>
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input type="password" id="password" v-model="form.password" required>
        </div>
        <div class="form-group">
          <label class="checkbox-label">
            <input type="checkbox" v-model="form.remember">
            记住我
          </label>
        </div>
        <button type="submit" class="btn" :disabled="loading">
          <span v-if="loading">登录中...</span>
          <span v-else>登录</span>
        </button>
      </form>
      <div class="login-footer">
        <a href="#">忘记密码？</a>
        <span>|</span>
        <router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      form: {
        username: '',
        password: '',
        remember: false
      },
      loading: false  // 添加加载状态
    }
  },
  methods: {
    async login() {
      this.loading = true;

      try {
        // ===== 模拟登录代码（测试用，可以删除）=====
        // 如果用户名和密码是测试账号，直接模拟登录成功
        if (this.form.username === '2021001' && this.form.password === '123456') {
          console.log('使用模拟登录');
          
          const mockUserInfo = {
            id: 1,
            studentId: '2021001',
            name: '张三',
            phone: '13800138000',
            email: 'zhangsan@example.com',
            role: 'user',
            borrowedCount: 2
          };
          
          localStorage.setItem('userInfo', JSON.stringify(mockUserInfo));
          localStorage.setItem('token', 'mock-token-' + Date.now());
          
          console.log('模拟登录成功');
          this.$router.push('/');
          return; // 重要：这里要 return，不再执行后面的真实登录
        }
        // ===== 模拟登录代码结束 =====

        // 构建登录请求数据
        const loginData = {
          username: this.form.username,
          password: this.form.password
        };

        console.log('发送登录请求:', loginData);

        // 调用后端登录接口 - 修改为 /users/login
        const response = await this.$request.post('/users/login', loginData);
        
        console.log('登录响应:', response);
        
        // 根据后端返回的数据结构调整
        if (response.success) {
          // 构建用户信息对象
          const userInfo = {
            id: response.userId,
            studentId: response.studentId,
            name: response.username,
            phone: response.phone || '',
            email: response.email || '',
            role: response.role || 'user',
            borrowedCount: response.borrowedCount || 0
          };
          
          // 存储用户信息到本地存储
          localStorage.setItem('userInfo', JSON.stringify(userInfo));
          
          // 存储 token（如果后端返回了token就用后端的，否则生成一个简单的）
          const token = response.token || 'mock-token-' + Date.now();
          localStorage.setItem('token', token);
          
          console.log('登录成功，token:', token);
          
          // 跳转到首页
          this.$router.push('/');
        } else {
          alert(response.message || '登录失败');
        }
      } catch (error) {
        console.error('登录错误:', error);
        if (error.response) {
          // 服务器返回了错误状态码
          alert(error.response.data?.message || '登录失败，请稍后重试');
        } else if (error.request) {
          // 请求发送了但没有收到响应
          alert('无法连接到服务器，请检查网络');
        } else {
          // 请求配置出错
          alert('登录失败：' + error.message);
        }
      } finally {
        this.loading = false;
      }
    }
  }
}
</script>

<style scoped>
.login {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: #f5f5f5;
  padding: 20px;
}

.login h2 {
  color: #4CAF50;
  margin-bottom: 30px;
  font-size: 28px;
}

.login-form {
  background-color: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  width: 100%;
  max-width: 400px;
}

.login-form h3 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.form-group {
  margin-bottom: 20px;
}

label {
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
  color: #555;
  font-size: 14px;
}

input[type="text"],
input[type="password"] {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.3s;
}

input:focus {
  outline: none;
  border-color: #4CAF50;
}

.checkbox-label {
  display: flex;
  align-items: center;
  font-weight: normal;
  cursor: pointer;
}

.checkbox-label input {
  width: auto;
  margin-right: 8px;
}

.btn {
  display: block;
  width: 100%;
  background-color: #4CAF50;
  color: white;
  padding: 12px;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.3s;
  margin-top: 20px;
}

.btn:hover {
  background-color: #45a049;
}

.btn:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.login-footer {
  margin-top: 20px;
  text-align: center;
  font-size: 14px;
  color: #666;
}

.login-footer a {
  color: #4CAF50;
  text-decoration: none;
  margin: 0 10px;
}

.login-footer a:hover {
  text-decoration: underline;
}

.login-footer span {
  margin: 0 10px;
  color: #999;
}
</style>