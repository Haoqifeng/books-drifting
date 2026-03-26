<template>
  <div class="admin">
    <h2>管理员中心</h2>
    <div class="admin-nav">
      <button @click="activeTab = 'users'" :class="{ active: activeTab === 'users' }">用户管理</button>
      <button @click="activeTab = 'books'" :class="{ active: activeTab === 'books' }">书籍管理</button>
      <button @click="activeTab = 'statistics'" :class="{ active: activeTab === 'statistics' }">系统统计</button>
    </div>
    
    <div class="admin-content">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>
      
      <!-- 用户管理 -->
      <div v-else-if="activeTab === 'users'" class="users-tab">
        <h3>用户列表</h3>
        <div class="search-bar">
          <input 
            type="text" 
            v-model="searchKeyword" 
            placeholder="搜索用户名/学号"
            @keyup.enter="searchUsers"
          >
          <button class="btn" @click="searchUsers">搜索</button>
          <button class="btn reset-btn" @click="resetSearch">重置</button>
        </div>
        <table class="user-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>学号</th>
              <th>姓名</th>
              <th>手机号</th>
              <th>邮箱</th>
              <th>角色</th>
              <th>借阅数</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.id">
              <td>{{ user.id }}</td>
              <td>{{ user.studentId }}</td>
              <td>{{ user.name }}</td>
              <td>{{ user.phone || '-' }}</td>
              <td>{{ user.email || '-' }}</td>
              <td>
                <select v-model="user.role" @change="updateUserRole(user)" :disabled="user.id === currentUserId">
                  <option value="user">普通用户</option>
                  <option value="admin">管理员</option>
                </select>
              </td>
              <td>{{ user.borrowedCount || 0 }}本</td>
              <td>
                <button class="btn delete-btn" @click="deleteUser(user)" :disabled="user.id === currentUserId">
                  删除
                </button>
              </td>
            </tr>
            <tr v-if="users.length === 0">
              <td colspan="8" class="empty-data">暂无用户数据</td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <!-- 书籍管理 -->
      <div v-else-if="activeTab === 'books'" class="books-tab">
        <h3>书籍列表</h3>
        <div class="search-bar">
          <input 
            type="text" 
            v-model="searchKeyword" 
            placeholder="搜索书名/作者/ISBN"
            @keyup.enter="searchBooks"
          >
          <button class="btn" @click="searchBooks">搜索</button>
          <button class="btn reset-btn" @click="resetSearch">重置</button>
        </div>
        <table class="book-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>书名</th>
              <th>作者</th>
              <th>ISBN</th>
              <th>出版社</th>
              <th>状态</th>
              <th>捐赠人</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="book in books" :key="book.id">
              <td>{{ book.id }}</td>
              <td>
                <router-link :to="`/book/${book.id}`" class="book-link">
                  {{ book.title }}
                </router-link>
              </td>
              <td>{{ book.author }}</td>
              <td>{{ book.isbn || '-' }}</td>
              <td>{{ book.publisher || '-' }}</td>
              <td>
                <span :class="['status-badge', book.status]">
                  {{ book.status === 'available' ? '可认领' : '已借出' }}
                </span>
              </td>
              <td>{{ book.donorName || '匿名' }}</td>
              <td>
                <button class="btn edit-btn" @click="editBook(book)">编辑</button>
                <button class="btn delete-btn" @click="deleteBook(book)">删除</button>
              </td>
            </tr>
            <tr v-if="books.length === 0">
              <td colspan="8" class="empty-data">暂无书籍数据</td>
            </tr>
          </tbody>
        </table>
        
        <!-- 编辑书籍弹窗 -->
        <div v-if="editDialogVisible" class="modal-overlay" @click="closeEditDialog">
          <div class="modal-container" @click.stop>
            <div class="modal-header">
              <h3>编辑书籍</h3>
              <button class="close-btn" @click="closeEditDialog">&times;</button>
            </div>
            <div class="modal-body">
              <div class="form-group">
                <label>书名</label>
                <input type="text" v-model="editForm.title">
              </div>
              <div class="form-group">
                <label>作者</label>
                <input type="text" v-model="editForm.author">
              </div>
              <div class="form-group">
                <label>ISBN</label>
                <input type="text" v-model="editForm.isbn">
              </div>
              <div class="form-group">
                <label>出版社</label>
                <input type="text" v-model="editForm.publisher">
              </div>
              <div class="form-group">
                <label>描述</label>
                <textarea v-model="editForm.description" rows="3"></textarea>
              </div>
              <div class="form-group">
                <label>状态</label>
                <select v-model="editForm.status">
                  <option value="available">可认领</option>
                  <option value="borrowed">已借出</option>
                </select>
              </div>
              <div class="form-group">
                <label>成色</label>
                <select v-model="editForm.condition">
                  <option value="new">崭新</option>
                  <option value="good">良好</option>
                  <option value="normal">正常</option>
                  <option value="worn">破旧</option>
                </select>
              </div>
            </div>
            <div class="modal-footer">
              <button class="btn cancel-btn" @click="closeEditDialog">取消</button>
              <button class="btn save-btn" @click="saveBookEdit">保存</button>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 系统统计 -->
      <div v-else-if="activeTab === 'statistics'" class="statistics-tab">
        <h3>系统统计</h3>
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-value">{{ statistics.totalBooks || 0 }}</div>
            <div class="stat-label">总书籍数</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ statistics.availableBooks || 0 }}</div>
            <div class="stat-label">可认领书籍</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ statistics.borrowedBooks || 0 }}</div>
            <div class="stat-label">已借出书籍</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ statistics.totalUsers || 0 }}</div>
            <div class="stat-label">总用户数</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ statistics.totalDonations || 0 }}</div>
            <div class="stat-label">总捐赠数</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ statistics.totalBorrows || 0 }}</div>
            <div class="stat-label">总借阅数</div>
          </div>
        </div>
        <button class="refresh-btn" @click="loadStatistics">刷新统计</button>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  data() {
    return {
      activeTab: 'users',
      searchKeyword: '',
      loading: false,
      users: [],
      books: [],
      statistics: {},
      editDialogVisible: false,
      editForm: {
        id: null,
        title: '',
        author: '',
        isbn: '',
        publisher: '',
        description: '',
        status: '',
        condition: ''
      },
      currentUserId: null,
      apiBaseUrl: 'http://localhost:10011/api'
    }
  },
  mounted() {
    // 获取当前登录用户ID
    const userInfo = localStorage.getItem('userInfo')
    if (userInfo) {
      try {
        this.currentUserId = JSON.parse(userInfo).id
      } catch (e) {
        console.error('解析用户信息失败', e)
      }
    }
    this.loadUsers()
    this.loadStatistics()
  },
  methods: {
    // 加载用户列表
    async loadUsers() {
      this.loading = true
      try {
        const token = localStorage.getItem('token')
        const response = await axios.get(`${this.apiBaseUrl}/users`, {
          headers: { 'Authorization': `Bearer ${token}` }
        })
        if (Array.isArray(response.data)) {
          this.users = response.data
        } else if (response.data.success && Array.isArray(response.data.data)) {
          this.users = response.data.data
        } else {
          console.error('用户数据格式错误', response.data)
          this.users = []
        }
      } catch (error) {
        console.error('加载用户失败:', error)
        this.$message?.error('加载用户失败')
      } finally {
        this.loading = false
      }
    },
    
    // 搜索用户
    async searchUsers() {
      if (!this.searchKeyword.trim()) {
        this.loadUsers()
        return
      }
      this.loading = true
      try {
        const token = localStorage.getItem('token')
        const response = await axios.get(`${this.apiBaseUrl}/users/search`, {
          params: { keyword: this.searchKeyword },
          headers: { 'Authorization': `Bearer ${token}` }
        })
        if (response.data.success && Array.isArray(response.data.data)) {
          this.users = response.data.data
        } else if (Array.isArray(response.data)) {
          this.users = response.data
        } else {
          this.users = []
        }
      } catch (error) {
        console.error('搜索失败:', error)
        this.$message?.error('搜索失败')
      } finally {
        this.loading = false
      }
    },
    
    // 重置搜索
    resetSearch() {
      this.searchKeyword = ''
      if (this.activeTab === 'users') {
        this.loadUsers()
      } else {
        this.loadBooks()
      }
    },
    
    // 更新用户角色
    async updateUserRole(user) {
      if (user.id === this.currentUserId) {
        this.$message?.warning('不能修改自己的角色')
        // 恢复原角色
        await this.loadUsers()
        return
      }
      try {
        const token = localStorage.getItem('token')
        const response = await axios.put(`${this.apiBaseUrl}/users/${user.id}/role`, 
          { role: user.role },
          { headers: { 'Authorization': `Bearer ${token}` } }
        )
        if (response.data.success) {
          this.$message?.success('角色更新成功')
        } else {
          this.$message?.error(response.data.message || '更新失败')
          await this.loadUsers()
        }
      } catch (error) {
        console.error('更新角色失败:', error)
        this.$message?.error('更新角色失败')
        await this.loadUsers()
      }
    },
    
    // 删除用户
    async deleteUser(user) {
      if (user.id === this.currentUserId) {
        this.$message?.warning('不能删除自己的账号')
        return
      }
      if (!confirm(`确定要删除用户"${user.name}"吗？`)) return
      
      try {
        const token = localStorage.getItem('token')
        const response = await axios.delete(`${this.apiBaseUrl}/users/${user.id}`, {
          headers: { 'Authorization': `Bearer ${token}` }
        })
        if (response.data.success) {
          this.$message?.success('删除成功')
          this.loadUsers()
          this.loadStatistics()
        } else {
          this.$message?.error(response.data.message || '删除失败')
        }
      } catch (error) {
        console.error('删除失败:', error)
        this.$message?.error('删除失败')
      }
    },
    
    // 加载书籍列表
    async loadBooks() {
      this.loading = true
      try {
        const token = localStorage.getItem('token')
        const response = await axios.get(`${this.apiBaseUrl}/books/all`, {
          headers: { 'Authorization': `Bearer ${token}` }
        })
        if (response.data.success && Array.isArray(response.data.data)) {
          this.books = response.data.data
        } else if (Array.isArray(response.data)) {
          this.books = response.data
        } else {
          console.error('书籍数据格式错误', response.data)
          this.books = []
        }
      } catch (error) {
        console.error('加载书籍失败:', error)
        this.$message?.error('加载书籍失败')
        // 使用备用数据
        this.books = []
      } finally {
        this.loading = false
      }
    },
    
    // 搜索书籍
    async searchBooks() {
      if (!this.searchKeyword.trim()) {
        this.loadBooks()
        return
      }
      this.loading = true
      try {
        const token = localStorage.getItem('token')
        const response = await axios.get(`${this.apiBaseUrl}/books/search`, {
          params: { keyword: this.searchKeyword },
          headers: { 'Authorization': `Bearer ${token}` }
        })
        if (response.data.success && Array.isArray(response.data.data)) {
          this.books = response.data.data
        } else if (Array.isArray(response.data)) {
          this.books = response.data
        } else {
          this.books = []
        }
      } catch (error) {
        console.error('搜索失败:', error)
        this.$message?.error('搜索失败')
      } finally {
        this.loading = false
      }
    },
    
    // 编辑书籍
    editBook(book) {
      this.editForm = {
        id: book.id,
        title: book.title,
        author: book.author,
        isbn: book.isbn || '',
        publisher: book.publisher || '',
        description: book.description || '',
        status: book.status,
        condition: book.condition || 'normal'
      }
      this.editDialogVisible = true
    },
    
    // 关闭编辑弹窗
    closeEditDialog() {
      this.editDialogVisible = false
      this.editForm = {
        id: null,
        title: '',
        author: '',
        isbn: '',
        publisher: '',
        description: '',
        status: '',
        condition: ''
      }
    },
    
    // 保存书籍编辑
    async saveBookEdit() {
      if (!this.editForm.title || !this.editForm.author) {
        this.$message?.warning('书名和作者不能为空')
        return
      }
      try {
        const token = localStorage.getItem('token')
        const response = await axios.put(`${this.apiBaseUrl}/books/${this.editForm.id}`, this.editForm, {
          headers: { 'Authorization': `Bearer ${token}` }
        })
        if (response.data.success) {
          this.$message?.success('更新成功')
          this.closeEditDialog()
          this.loadBooks()
        } else {
          this.$message?.error(response.data.message || '更新失败')
        }
      } catch (error) {
        console.error('更新失败:', error)
        this.$message?.error('更新失败')
      }
    },
    
    // 删除书籍
    async deleteBook(book) {
      if (!confirm(`确定要删除书籍"${book.title}"吗？此操作不可恢复！`)) return
      
      try {
        const token = localStorage.getItem('token')
        const response = await axios.delete(`${this.apiBaseUrl}/books/${book.id}`, {
          headers: { 'Authorization': `Bearer ${token}` }
        })
        if (response.data.success) {
          this.$message?.success('删除成功')
          this.loadBooks()
          this.loadStatistics()
        } else {
          this.$message?.error(response.data.message || '删除失败')
        }
      } catch (error) {
        console.error('删除失败:', error)
        this.$message?.error('删除失败')
      }
    },
    
    // 加载统计信息
    async loadStatistics() {
      try {
        const token = localStorage.getItem('token')
        const response = await axios.get(`${this.apiBaseUrl}/statistics/system`, {
          headers: { 'Authorization': `Bearer ${token}` }
        })
        if (response.data.success && response.data.data) {
          this.statistics = response.data.data
        } else if (response.data) {
          this.statistics = response.data
        } else {
          this.statistics = {}
        }
      } catch (error) {
        console.error('加载统计失败:', error)
        this.statistics = {}
      }
    }
  },
  watch: {
    activeTab(newTab) {
      this.searchKeyword = ''
      if (newTab === 'users') {
        this.loadUsers()
      } else if (newTab === 'books') {
        this.loadBooks()
      } else if (newTab === 'statistics') {
        this.loadStatistics()
      }
    }
  }
}
</script>

<style scoped>
.admin {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.admin-nav {
  display: flex;
  margin-bottom: 30px;
  border-bottom: 1px solid #ddd;
}

.admin-nav button {
  padding: 10px 20px;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 16px;
  color: #666;
  border-bottom: 2px solid transparent;
  transition: all 0.3s;
}

.admin-nav button:hover {
  color: #4CAF50;
}

.admin-nav button.active {
  color: #4CAF50;
  border-bottom-color: #4CAF50;
}

.admin-content {
  min-height: 400px;
}

.search-bar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.search-bar input {
  flex: 1;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s;
}

.edit-btn {
  background-color: #2196F3;
  color: white;
  margin-right: 10px;
}

.edit-btn:hover {
  background-color: #0b7dda;
}

.delete-btn {
  background-color: #f44336;
  color: white;
}

.delete-btn:hover {
  background-color: #d32f2f;
}

.delete-btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.reset-btn {
  background-color: #ff9800;
  color: white;
}

.reset-btn:hover {
  background-color: #fb8c00;
}

.user-table, .book-table {
  width: 100%;
  border-collapse: collapse;
}

.user-table th, .book-table th {
  background-color: #f2f2f2;
  padding: 12px;
  text-align: left;
  font-weight: bold;
  color: #333;
  border-bottom: 2px solid #ddd;
}

.user-table td, .book-table td {
  padding: 12px;
  border-bottom: 1px solid #ddd;
}

.user-table tr:hover, .book-table tr:hover {
  background-color: #f5f5f5;
}

.empty-data {
  text-align: center;
  color: #999;
  padding: 40px !important;
}

.book-link {
  color: #4CAF50;
  text-decoration: none;
}

.book-link:hover {
  text-decoration: underline;
}

.status-badge {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.status-badge.available {
  background-color: #d4edda;
  color: #155724;
}

.status-badge.borrowed {
  background-color: #f8d7da;
  color: #721c24;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  background-color: #f9f9f9;
  padding: 20px;
  border-radius: 8px;
  text-align: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #4CAF50;
  margin-bottom: 10px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.refresh-btn {
  display: block;
  margin: 0 auto;
  padding: 10px 24px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.refresh-btn:hover {
  background-color: #45a049;
}

.loading-container {
  text-align: center;
  padding: 40px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  margin: 0 auto 10px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #4CAF50;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-container {
  background-color: white;
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
}

.close-btn:hover {
  color: #333;
}

.modal-body {
  padding: 20px;
}

.modal-body .form-group {
  margin-bottom: 15px;
}

.modal-body label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
  color: #555;
  font-size: 14px;
}

.modal-body input,
.modal-body select,
.modal-body textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

.modal-body input:focus,
.modal-body select:focus,
.modal-body textarea:focus {
  outline: none;
  border-color: #4CAF50;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 15px 20px;
  border-top: 1px solid #eee;
}

.cancel-btn {
  background-color: #f5f5f5;
  color: #666;
}

.cancel-btn:hover {
  background-color: #e0e0e0;
}

.save-btn {
  background-color: #4CAF50;
  color: white;
}

.save-btn:hover {
  background-color: #45a049;
}

@media (max-width: 768px) {
  .admin {
    padding: 15px;
  }
  
  .admin-nav button {
    padding: 8px 12px;
    font-size: 14px;
  }
  
  .user-table, .book-table {
    font-size: 12px;
  }
  
  .user-table th, .book-table th,
  .user-table td, .book-table td {
    padding: 8px;
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .search-bar {
    flex-direction: column;
  }
  
  .modal-container {
    width: 95%;
  }
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>