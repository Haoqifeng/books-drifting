<template>
  <div class="register">
    <h2>📚 校园闲置书籍漂流系统</h2>
    <div class="register-form">
      <h3>📝 用户注册</h3>
      <form @submit.prevent="register">
        <!-- 学号 -->
        <div class="form-group">
          <label for="studentId">
            学号
            <span class="required-star">*</span>
          </label>
          <input 
            type="text" 
            id="studentId" 
            v-model="form.studentId" 
            placeholder="请输入10位学号"
            maxlength="10"
            required
            :class="{ 'input-error': errors.studentId }"
            @input="validateStudentId"
            @blur="validateStudentId"
          >
          <div class="input-hint" v-if="form.studentId">
            <span v-if="errors.studentId" class="hint-error">
              ❌ {{ errors.studentId }}
            </span>
            <span v-else-if="form.studentId.length < 10" class="hint-warning">
              ⚠️ 还需输入 {{ 10 - form.studentId.length }} 位
            </span>
            <span v-else class="hint-success">
              ✅ 学号格式正确
            </span>
          </div>
          <div v-else class="input-hint hint-info">
            ℹ️ 请输入10位学号
          </div>
        </div>

        <!-- 姓名 -->
        <div class="form-group">
          <label for="name">
            姓名
            <span class="required-star">*</span>
          </label>
          <input 
            type="text" 
            id="name" 
            v-model="form.name" 
            placeholder="请输入中文姓名，至少2个字"
            required
            :class="{ 'input-error': errors.name }"
            @input="validateName"
            @blur="validateName"
          >
          <div class="input-hint" v-if="form.name">
            <span v-if="errors.name" class="hint-error">
              ❌ {{ errors.name }}
            </span>
            <span v-else class="hint-success">
              ✅ 姓名格式正确
            </span>
          </div>
          <div v-else class="input-hint hint-info">
            ℹ️ 请输入中文姓名，至少2个字
          </div>
        </div>

        <!-- 手机号 -->
        <div class="form-group">
          <label for="phone">
            手机号码
            <span class="required-star">*</span>
          </label>
          <input 
            type="tel" 
            id="phone" 
            v-model="form.phone" 
            placeholder="请输入11位手机号"
            maxlength="11"
            required
            :class="{ 'input-error': errors.phone }"
            @input="validatePhone"
            @blur="validatePhone"
          >
          <div class="input-hint" v-if="form.phone">
            <span v-if="errors.phone" class="hint-error">
              ❌ {{ errors.phone }}
            </span>
            <span v-else-if="form.phone.length < 11" class="hint-warning">
              ⚠️ 还需输入 {{ 11 - form.phone.length }} 位
            </span>
            <span v-else class="hint-success">
              ✅ 手机号格式正确
            </span>
          </div>
          <div v-else class="input-hint hint-info">
            ℹ️ 请输入11位手机号
          </div>
        </div>

        <!-- 邮箱 -->
        <div class="form-group">
          <label for="email">
            邮箱
            <span class="required-star">*</span>
          </label>
          <input 
            type="email" 
            id="email" 
            v-model="form.email" 
            placeholder="请输入邮箱地址"
            required
            :class="{ 'input-error': errors.email }"
            @input="validateEmail"
            @blur="validateEmail"
          >
          <div class="input-hint" v-if="form.email">
            <span v-if="errors.email" class="hint-error">
              ❌ {{ errors.email }}
            </span>
            <span v-else class="hint-success">
              ✅ 邮箱格式正确
            </span>
          </div>
          <div v-else class="input-hint hint-info">
            ℹ️ 请输入正确的邮箱格式
          </div>
        </div>

        <!-- 密码 -->
        <div class="form-group">
          <label for="password">
            密码
            <span class="required-star">*</span>
          </label>
          <input 
            type="password" 
            id="password" 
            v-model="form.password" 
            placeholder="请输入密码，至少6位"
            required
            :class="{ 'input-error': errors.password }"
            @input="validatePassword"
            @blur="validatePassword"
          >
          <div class="input-hint" v-if="form.password">
            <span v-if="errors.password" class="hint-error">
              ❌ {{ errors.password }}
            </span>
            <span v-else-if="form.password.length < 6" class="hint-warning">
              ⚠️ 还需输入 {{ 6 - form.password.length }} 位
            </span>
            <span v-else class="hint-success">
              ✅ 密码强度足够
            </span>
          </div>
          <div v-else class="input-hint hint-info">
            ℹ️ 密码长度至少6位
          </div>
        </div>

        <!-- 确认密码 -->
        <div class="form-group">
          <label for="confirmPassword">
            确认密码
            <span class="required-star">*</span>
          </label>
          <input 
            type="password" 
            id="confirmPassword" 
            v-model="form.confirmPassword" 
            placeholder="请再次输入密码"
            required
            :class="{ 'input-error': errors.confirmPassword }"
            @input="validateConfirmPassword"
            @blur="validateConfirmPassword"
          >
          <div class="input-hint" v-if="form.confirmPassword">
            <span v-if="errors.confirmPassword" class="hint-error">
              ❌ {{ errors.confirmPassword }}
            </span>
            <span v-else-if="form.confirmPassword !== form.password" class="hint-warning">
              ⚠️ 两次输入的密码不一致
            </span>
            <span v-else class="hint-success">
              ✅ 密码确认无误
            </span>
          </div>
        </div>

        <button 
          type="submit" 
          class="btn" 
          :disabled="loading || !isFormValid"
          :class="{ 'btn-disabled': !isFormValid }"
        >
          <span v-if="loading" class="loading-spinner"></span>
          <span>{{ loading ? '注册中...' : '注 册' }}</span>
        </button>
      </form>
      
      <div class="register-footer">
        <span>已有账号？</span>
        <router-link to="/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      form: {
        studentId: '',
        name: '',
        phone: '',
        email: '',
        password: '',
        confirmPassword: ''
      },
      errors: {
        studentId: '',
        name: '',
        phone: '',
        email: '',
        password: '',
        confirmPassword: ''
      },
      loading: false
    }
  },
  computed: {
    // 计算表单是否全部有效
    isFormValid() {
      return !Object.values(this.errors).some(error => error) &&
             this.form.studentId &&
             this.form.name &&
             this.form.phone &&
             this.form.email &&
             this.form.password &&
             this.form.confirmPassword &&
             this.form.password === this.form.confirmPassword
    }
  },
  methods: {
    // 验证学号（10位数字）
    validateStudentId() {
      const studentId = this.form.studentId
      if (!studentId) {
        this.errors.studentId = '学号不能为空'
      } else if (!/^\d{10}$/.test(studentId)) {
        this.errors.studentId = '学号必须为10位数字'
      } else {
        this.errors.studentId = ''
      }
    },

    // 验证姓名（中文，至少2个字符）
    validateName() {
      const name = this.form.name
      if (!name) {
        this.errors.name = '姓名不能为空'
      } else if (name.length < 2) {
        this.errors.name = '姓名至少需要2个字符'
      } else if (!/^[\u4e00-\u9fa5]{2,}$/.test(name)) {
        this.errors.name = '姓名必须为中文'
      } else {
        this.errors.name = ''
      }
    },

    // 验证手机号（11位数字，以1开头）
    validatePhone() {
      const phone = this.form.phone
      if (!phone) {
        this.errors.phone = '手机号不能为空'
      } else if (!/^1[3-9]\d{9}$/.test(phone)) {
        this.errors.phone = '请输入正确的11位手机号'
      } else {
        this.errors.phone = ''
      }
    },

    // 验证邮箱
    validateEmail() {
      const email = this.form.email
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      if (!email) {
        this.errors.email = '邮箱不能为空'
      } else if (!emailRegex.test(email)) {
        this.errors.email = '请输入正确的邮箱格式'
      } else {
        this.errors.email = ''
      }
    },

    // 验证密码（至少6位）
    validatePassword() {
      const password = this.form.password
      if (!password) {
        this.errors.password = '密码不能为空'
      } else if (password.length < 6) {
        this.errors.password = '密码长度至少6位'
      } else {
        this.errors.password = ''
      }
      // 如果确认密码已填写，同时验证确认密码
      if (this.form.confirmPassword) {
        this.validateConfirmPassword()
      }
    },

    // 验证确认密码
    validateConfirmPassword() {
      const confirmPwd = this.form.confirmPassword
      const password = this.form.password
      if (!confirmPwd) {
        this.errors.confirmPassword = '请再次输入密码'
      } else if (confirmPwd !== password) {
        this.errors.confirmPassword = '两次输入的密码不一致'
      } else {
        this.errors.confirmPassword = ''
      }
    },

    async register() {
      // 执行所有验证
      this.validateStudentId()
      this.validateName()
      this.validatePhone()
      this.validateEmail()
      this.validatePassword()
      this.validateConfirmPassword()

      // 如果有错误，不提交
      if (!this.isFormValid) {
        alert('请正确填写所有表单')
        return
      }

      this.loading = true

      try {
        const registerData = {
          studentId: this.form.studentId,
          name: this.form.name,
          phone: this.form.phone,
          email: this.form.email,
          password: this.form.password
        }

        console.log('发送注册请求:', registerData)

        const response = await this.$request.post('/users/register', registerData)
        
        console.log('注册响应:', response)
        
        if (response.success || response.code === 200) {
          alert('🎉 注册成功！请登录')
          this.$router.push('/login')
        } else {
          alert(response.message || '注册失败')
        }
      } catch (error) {
        console.error('注册错误:', error)
        if (error.response) {
          alert(error.response.data?.message || '注册失败，请稍后重试')
        } else if (error.request) {
          alert('无法连接到服务器，请检查网络')
        } else {
          alert('注册失败：' + error.message)
        }
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.register {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #55a099 0%, #169f9f 100%);
  padding: 20px;
}

.register h2 {
  color: white;
  margin-bottom: 30px;
  font-size: 32px;
  font-weight: 600;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.2);
}

.register-form {
  background-color: white;
  padding: 40px;
  border-radius: 20px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  width: 100%;
  max-width: 450px;
  animation: slideUp 0.5s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.register-form h3 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
  font-size: 24px;
  font-weight: 500;
}

.form-group {
  margin-bottom: 20px;
  position: relative;
}

label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #555;
  font-size: 14px;
}

.required-star {
  color: #f44336;
  margin-left: 3px;
}

input {
  width: 100%;
  padding: 12px 15px;
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  font-size: 15px;
  transition: all 0.3s ease;
  box-sizing: border-box;
}

input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

input.input-error {
  border-color: #f44336;
}

input.input-error:focus {
  border-color: #f44336;
  box-shadow: 0 0 0 3px rgba(244, 67, 54, 0.1);
}

.input-hint {
  margin-top: 5px;
  font-size: 12px;
  min-height: 18px;
}

.hint-info {
  color: #667eea;
}

.hint-warning {
  color: #ff9800;
}

.hint-success {
  color: #4CAF50;
}

.hint-error {
  color: #f44336;
}

.btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  background: linear-gradient(135deg, #3d797c 0%, #764ba2 100%);
  color: white;
  padding: 14px;
  border: none;
  border-radius: 10px;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-top: 30px;
  position: relative;
  overflow: hidden;
}

.btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 25px rgba(102, 126, 234, 0.4);
}

.btn:active:not(:disabled) {
  transform: translateY(0);
}

.btn-disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background: linear-gradient(135deg, #999 0%, #777 100%);
}

.loading-spinner {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 3px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: white;
  animation: spin 1s ease-in-out infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.register-footer {
  margin-top: 25px;
  text-align: center;
  font-size: 15px;
  color: #666;
}

.register-footer a {
  color: #65a8bc;
  text-decoration: none;
  margin-left: 5px;
  font-weight: 600;
  transition: color 0.3s;
}

.register-footer a:hover {
  color: #764ba2;
  text-decoration: underline;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .register h2 {
    font-size: 24px;
  }
  
  .register-form {
    padding: 30px 20px;
  }
  
  .register-form h3 {
    font-size: 20px;
  }
  
  input {
    padding: 10px 12px;
  }
  
  .btn {
    padding: 12px;
    font-size: 16px;
  }
}
</style>