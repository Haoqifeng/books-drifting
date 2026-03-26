<template>
  <div class="home" :class="themeClass">
    <header class="header">
      <h1>校园闲置书籍漂流</h1>
      <nav class="nav">
        <router-link to="/donate">捐赠书籍</router-link>
        <router-link to="/borrow">认领书籍</router-link>
        <router-link to="/scan">归还书籍</router-link>
        <router-link to="/user">个人中心</router-link>
        <router-link to="/medal-wall">勋章墙</router-link>
        <router-link v-if="isAdmin" to="/admin">管理员</router-link>
        <a href="#" @click.prevent="logout" class="logout">退出登录</a>
      </nav>
      
      <!-- 主题切换按钮 -->
      <div class="theme-switcher">
        <button 
          class="theme-toggle" 
          @click="showThemePanel = !showThemePanel"
          :title="'切换主题'"
        >
          <span class="theme-icon">🎨</span>
        </button>
        
        <!-- 主题选择面板 -->
        <div v-if="showThemePanel" class="theme-panel">
          <h4>选择主题色</h4>
          <div class="theme-colors">
            <button 
              v-for="theme in themes" 
              :key="theme.value"
              class="theme-color-btn"
              :class="{ active: currentTheme === theme.value }"
              :style="{ background: theme.gradient }"
              @click="changeTheme(theme.value)"
              :title="theme.name"
            >
              <span class="theme-name">{{ theme.name }}</span>
            </button>
          </div>
          <button class="theme-close" @click="showThemePanel = false">✕</button>
        </div>
      </div>
    </header>
    
    <main class="main">
      <section class="intro">
        <h2>欢迎参与书籍漂流</h2>
        <p>让闲置书籍找到新的主人，让知识传递更远</p>
        <p><small><i>捐赠、认领、归还行为均为参与书籍漂流</i></small></p>
      </section>
      
      <!-- 搜索区域 -->
      <section class="search-section">
        <div class="search-container">
          <div class="search-box">
            <input 
              type="text" 
              v-model="searchKeyword" 
              placeholder="搜索书名、作者或出版社..."
              @keyup.enter="handleSearch"
              class="search-input"
            >
            <button @click="handleSearch" class="search-btn">
              <span class="search-icon">🔍</span>
              搜索
            </button>
          </div>
          
          <div class="search-stats" v-if="!loading">
            <span class="stats-text">
              {{ searchKeyword ? `找到 ${filteredBooks.length} 本相关书籍` : `共有 ${books.length} 本可认领书籍` }}
            </span>
            <button 
              v-if="searchKeyword" 
              @click="clearSearch" 
              class="clear-search"
            >
              清除搜索 ✕
            </button>
          </div>
        </div>
      </section>
      
      <section class="books">
        <div class="section-header">
          <h3>📚 可认领书籍</h3>
          <div class="sort-filter" v-if="filteredBooks.length > 0">
            <select v-model="sortOption" class="sort-select" @change="handleSort">
              <option value="default">默认排序</option>
              <option value="title-asc">书名 A-Z</option>
              <option value="title-desc">书名 Z-A</option>
              <option value="author-asc">作者 A-Z</option>
              <option value="author-desc">作者 Z-A</option>
            </select>
          </div>
        </div>
        
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-container">
          <div class="loading-spinner"></div>
          <p>正在加载书籍...</p>
        </div>
        
        <!-- 无搜索结果 -->
        <div v-else-if="filteredBooks.length === 0" class="empty-state">
          <div class="empty-icon">📚</div>
          <h3>暂无书籍</h3>
          <p v-if="searchKeyword">没有找到与 "{{ searchKeyword }}" 相关的书籍</p>
          <p v-else>暂时没有可认领的书籍</p>
          <button v-if="searchKeyword" @click="clearSearch" class="empty-btn">
            查看全部书籍
          </button>
          <router-link v-else to="/donate" class="empty-btn">
            去捐赠书籍
          </router-link>
        </div>
        
        <!-- 书籍网格 -->
        <div v-else class="book-grid">
          <div 
            v-for="book in sortedBooks" 
            :key="book.id" 
            class="book-card"
            @click="goToBookDetail(book.id)"
          >
            <div class="book-cover-wrapper">
              <div class="book-cover" :class="{ 'no-cover': !book.coverUrl }">
                <img v-if="book.coverUrl" :src="book.coverUrl" :alt="book.title" loading="lazy">
                <div v-else class="cover-placeholder">
                  <span class="placeholder-text">{{ book.title.charAt(0) }}</span>
                </div>
              </div>
              <div class="book-status" :class="book.status">
                {{ book.status === 'available' ? '可认领' : '已借出' }}
              </div>
            </div>
            
            <div class="book-info">
              <h4 class="book-title" :title="book.title">{{ book.title }}</h4>
              <p class="book-author" :title="book.author">{{ book.author }}</p>
              <div class="book-meta">
                <span v-if="book.publisher" class="book-publisher">{{ book.publisher }}</span>
                <span v-if="book.condition" class="book-condition" :class="book.condition">
                  {{ getConditionText(book.condition) }}
                </span>
              </div>
              <div class="book-footer">
                <span class="book-donor">{{ book.donorName || '匿名' }}</span>
                <span class="detail-link">查看详情 →</span>
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>
    
    <!-- 回到顶部按钮 -->
    <button 
      v-show="showScrollTop" 
      @click="scrollToTop" 
      class="scroll-top"
      title="回到顶部"
    >
      ↑
    </button>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'Home',
  data() {
    return {
      books: [],
      filteredBooks: [],
      sortedBooks: [],
      searchKeyword: '',
      sortOption: 'default',
      loading: false,
      showScrollTop: false,
      showThemePanel: false,
      currentTheme: 'default',
      apiBaseUrl: 'http://localhost:10011/api',
      themes: [
        {
          value: 'default',
          name: '默认紫',
          gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 50%, #4facfe 100%)'
        },
        {
          value: 'green',
          name: '青灰色',
          gradient: 'linear-gradient(135deg, #dbdbdb 0%, #2f6e62 100%)'
        },
        {
          value: 'blue',
          name: '海洋蓝',
          gradient: 'linear-gradient(135deg, #84fab0 0%, #8fd3f4 100%)'
        },
        {
          value: 'orange',
          name: '温暖橙',
          gradient: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)'
        },
        {
          value: 'pink',
          name: '浪漫粉',
          gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)'
        },
        {
          value: 'purple',
          name: '梦幻紫',
          gradient: 'linear-gradient(135deg, #5f2c82 0%, #49a09d 100%)'
        }
      ]
    }
  },
  computed: {
    isAdmin() {
      const userInfo = localStorage.getItem('userInfo')
      if (userInfo) {
        return JSON.parse(userInfo).role === 'admin'
      }
      return false
    },
    themeClass() {
      return `theme-${this.currentTheme}`
    }
  },
  watch: {
    searchKeyword() {
      this.filterBooks()
    },
    filteredBooks: {
      handler() {
        this.sortBooks()
      },
      immediate: true
    }
  },
  mounted() {
    const token = localStorage.getItem('token')
    if (!token) {
      console.log('未登录，跳转到登录页')
      this.$router.push('/login')
      return
    }
    this.fetchAvailableBooks()
    
    // 从本地存储加载主题
    const savedTheme = localStorage.getItem('appTheme')
    if (savedTheme) {
      this.currentTheme = savedTheme
    }
    
    // 监听滚动事件
    window.addEventListener('scroll', this.handleScroll)
    
    // 点击其他地方关闭主题面板
    document.addEventListener('click', this.handleClickOutside)
  },
  beforeDestroy() {
    window.removeEventListener('scroll', this.handleScroll)
    document.removeEventListener('click', this.handleClickOutside)
  },
  methods: {
    async fetchAvailableBooks() {
      this.loading = true
      try {
        const token = localStorage.getItem('token')
        
        if (!token) {
          this.$router.push('/login')
          return
        }
        
        const response = await axios.get(`${this.apiBaseUrl}/books/available`, {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        })
        
        console.log('获取到可认领书籍:', response.data)
        
        if (Array.isArray(response.data)) {
          this.books = response.data
        } else if (response.data.success && Array.isArray(response.data.data)) {
          this.books = response.data.data
        } else {
          this.useMockBooks()
        }
        
        this.filteredBooks = [...this.books]
      } catch (error) {
        console.error('获取书籍失败:', error)
        if (error.response && error.response.status === 403) {
          localStorage.removeItem('token')
          localStorage.removeItem('userInfo')
          this.$router.push('/login')
        } else {
          this.useMockBooks()
          this.filteredBooks = [...this.books]
        }
      } finally {
        this.loading = false
      }
    },
    
    filterBooks() {
      if (!this.searchKeyword.trim()) {
        this.filteredBooks = [...this.books]
      } else {
        const keyword = this.searchKeyword.toLowerCase().trim()
        this.filteredBooks = this.books.filter(book => {
          return (
            book.title.toLowerCase().includes(keyword) ||
            book.author.toLowerCase().includes(keyword) ||
            (book.publisher && book.publisher.toLowerCase().includes(keyword)) ||
            (book.isbn && book.isbn.includes(keyword))
          )
        })
      }
    },
    
    sortBooks() {
      let sorted = [...this.filteredBooks]
      
      switch (this.sortOption) {
        case 'title-asc':
          sorted.sort((a, b) => a.title.localeCompare(b.title))
          break
        case 'title-desc':
          sorted.sort((a, b) => b.title.localeCompare(a.title))
          break
        case 'author-asc':
          sorted.sort((a, b) => a.author.localeCompare(b.author))
          break
        case 'author-desc':
          sorted.sort((a, b) => b.author.localeCompare(a.author))
          break
        default:
          // 默认按捐赠时间倒序
          sorted.sort((a, b) => new Date(b.donateTime) - new Date(a.donateTime))
      }
      
      this.sortedBooks = sorted
    },
    
    handleSearch() {
      this.filterBooks()
    },
    
    clearSearch() {
      this.searchKeyword = ''
      this.sortOption = 'default'
      this.filteredBooks = [...this.books]
    },
    
    handleSort() {
      this.sortBooks()
    },
    
    goToBookDetail(bookId) {
      this.$router.push(`/book/${bookId}`)
    },
    
    handleScroll() {
      this.showScrollTop = window.scrollY > 500
    },
    
    scrollToTop() {
      window.scrollTo({
        top: 0,
        behavior: 'smooth'
      })
    },
    
    handleClickOutside(event) {
      const themeSwitcher = this.$el.querySelector('.theme-switcher')
      if (themeSwitcher && !themeSwitcher.contains(event.target)) {
        this.showThemePanel = false
      }
    },
    
    changeTheme(themeValue) {
      this.currentTheme = themeValue
      localStorage.setItem('appTheme', themeValue)
      this.showThemePanel = false
    },
    
    getConditionText(condition) {
      const map = {
        new: '全新',
        good: '良好',
        normal: '正常',
        worn: '破旧'
      }
      return map[condition] || condition
    },
    
    useMockBooks() {
      this.books = [
        {
          id: 1,
          title: 'JavaScript高级程序设计',
          author: 'Nicholas C. Zakas',
          publisher: '人民邮电出版社',
          isbn: '9787121028953',
          condition: 'good',
          donorName: '张三',
          status: 'available',
          coverUrl: 'https://img1.doubanio.com/view/subject/s/public/s1727290.jpg'
        },
        {
          id: 2,
          title: 'Vue.js实战',
          author: '梁灏',
          publisher: '清华大学出版社',
          isbn: '9787302456789',
          condition: 'new',
          donorName: '李四',
          status: 'available',
          coverUrl: 'https://img2.doubanio.com/view/subject/s/public/s33687085.jpg'
        },
        {
          id: 3,
          title: '算法导论',
          author: 'Thomas H. Cormen',
          publisher: '机械工业出版社',
          isbn: '9787111407010',
          condition: 'normal',
          donorName: '王五',
          status: 'available',
          coverUrl: 'https://img2.doubanio.com/view/subject/s/public/s27283856.jpg'
        },
        {
          id: 4,
          title: '深入理解计算机系统',
          author: 'Randal E. Bryant',
          publisher: '机械工业出版社',
          isbn: '9787111544937',
          condition: 'good',
          donorName: '赵六',
          status: 'available',
          coverUrl: ''
        },
        {
          id: 5,
          title: '设计模式',
          author: 'Erich Gamma',
          publisher: '机械工业出版社',
          isbn: '9787111213826',
          condition: 'worn',
          donorName: '钱七',
          status: 'available',
          coverUrl: ''
        }
      ]
    },
    
    logout() {
      localStorage.removeItem('userInfo')
      localStorage.removeItem('token')
      this.$router.push('/login')
    }
  }
}
</script>

<style scoped>
/* ==================== 全局样式 ==================== */
.home {
  min-height: 100vh;
  background-attachment: fixed;
  position: relative;
  transition: background 0.5s ease;
}

/* 默认主题 - 紫色渐变 */
.home.theme-default {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #4facfe 100%);
}

/* 清新绿主题 */
.home.theme-green {
  background: linear-gradient(135deg, #dbdbdb 0%, #2f6e62 100%);
}

/* 海洋蓝主题 */
.home.theme-blue {
  background: linear-gradient(135deg, #84fab0 0%, #8fd3f4 100%);
}

/* 温暖橙主题 */
.home.theme-orange {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

/* 浪漫粉主题 */
.home.theme-pink {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

/* 梦幻紫主题 */
.home.theme-purple {
  background: linear-gradient(135deg, #5f2c82 0%, #49a09d 100%);
}

.home::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" opacity="0.05"><path d="M20 20 L80 20 L80 80 L20 80 Z" fill="none" stroke="white" stroke-width="2"/><circle cx="50" cy="50" r="20" fill="none" stroke="white" stroke-width="2"/></svg>') repeat;
  pointer-events: none;
}

/* ==================== 头部样式 ==================== */
.header {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  color: white;
  padding: 16px 20px;
  text-align: center;
  box-shadow: 0 4px 20px rgba(31, 38, 135, 0.3);
  border-bottom: 1px solid rgba(255, 255, 255, 0.18);
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
}

.header h1 {
  font-size: 1.8rem;
  font-weight: 600;
  letter-spacing: 1px;
  background: linear-gradient(135deg, #ffffff, #f0f0f0);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0;
}

.nav {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}

.nav a {
  color: white;
  text-decoration: none;
  padding: 8px 16px;
  border-radius: 30px;
  transition: all 0.3s ease;
  font-weight: 500;
  font-size: 0.9rem;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.nav a:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.nav a.router-link-active {
  background: white;
  color: #4CAF50;
  box-shadow: 0 4px 12px rgba(255, 255, 255, 0.3);
}

.nav .logout {
  background: rgba(244, 67, 54, 0.25);
}

.nav .logout:hover {
  background: #f44336;
  color: white;
}

/* ==================== 主题切换器 ==================== */
.theme-switcher {
  position: relative;
  margin-left: 10px;
}

.theme-toggle {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  border: 2px solid rgba(255, 255, 255, 0.3);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  color: white;
  font-size: 1.2rem;
}

.theme-toggle:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: rotate(180deg);
  border-color: rgba(255, 255, 255, 0.5);
}

.theme-panel {
  position: absolute;
  top: 50px;
  right: 0;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 20px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  width: 280px;
  z-index: 1000;
  animation: slideDown 0.3s ease;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.theme-panel h4 {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 1rem;
  text-align: center;
  font-weight: 600;
}

.theme-colors {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.theme-color-btn {
  border: none;
  border-radius: 12px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  color: white;
  font-weight: 500;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.theme-color-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.theme-color-btn.active {
  border: 2px solid #333;
  transform: scale(1.02);
}

.theme-name {
  font-size: 0.85rem;
}

.theme-close {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.1);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  font-size: 0.8rem;
  transition: all 0.3s ease;
}

.theme-close:hover {
  background: rgba(0, 0, 0, 0.2);
  transform: scale(1.1);
}

/* ==================== 主要内容区 ==================== */
.main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
}

/* ==================== 介绍区域 ==================== */
.intro {
  text-align: center;
  margin-bottom: 30px;
  padding: 24px 20px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  box-shadow: 0 10px 30px -10px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.intro h2 {
  font-size: 2rem;
  color: white;
  margin-bottom: 12px;
  font-weight: 600;
  text-shadow: 1px 1px 3px rgba(0, 0, 0, 0.3);
  position: relative;
  display: inline-block;
}

.intro h2::after {
  content: '';
  position: absolute;
  bottom: -8px;
  left: 50%;
  transform: translateX(-50%);
  width: 60px;
  height: 3px;
  background: linear-gradient(90deg, transparent, white, transparent);
}

.intro p {
  color: rgba(255, 255, 255, 0.95);
  font-size: 1rem;
  line-height: 1.6;
  max-width: 600px;
  margin: 0 auto;
}

.intro p:last-child {
  margin-top: 12px;
  font-size: 0.9rem;
  opacity: 0.9;
}

/* ==================== 搜索区域 ==================== */
.search-section {
  margin-bottom: 24px;
}

.search-container {
  max-width: 600px;
  margin: 0 auto;
}

.search-box {
  display: flex;
  gap: 10px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  border-radius: 40px;
  padding: 4px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.search-input {
  flex: 1;
  padding: 10px 18px;
  border: none;
  background: transparent;
  font-size: 0.95rem;
  color: white;
  outline: none;
}

.search-input::placeholder {
  color: rgba(255, 255, 255, 0.6);
}

.search-btn {
  padding: 8px 20px;
  border: none;
  background: white;
  color: #4CAF50;
  border-radius: 30px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 5px;
}

.search-btn:hover {
  background: #4CAF50;
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.search-stats {
  margin-top: 10px;
  text-align: center;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(5px);
  padding: 6px 16px;
  border-radius: 30px;
  width: fit-content;
  margin-left: auto;
  margin-right: auto;
  font-size: 0.85rem;
}

.clear-search {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: white;
  padding: 4px 12px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 0.8rem;
  transition: all 0.3s ease;
}

.clear-search:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* ==================== 书籍区域标题 ==================== */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.books h3 {
  font-size: 1.5rem;
  color: white;
  font-weight: 600;
  position: relative;
  padding-bottom: 8px;
}

.books h3::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 40px;
  height: 3px;
  background: linear-gradient(90deg, white, transparent);
}

.sort-filter {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(5px);
  border-radius: 30px;
  padding: 3px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.sort-select {
  padding: 6px 16px;
  border: none;
  background: transparent;
  color: white;
  font-size: 0.85rem;
  outline: none;
  cursor: pointer;
  border-radius: 30px;
}

.sort-select option {
  background: #2c3e50;
  color: white;
}

/* ==================== 加载状态 ==================== */
.loading-container {
  text-align: center;
  padding: 40px 20px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-radius: 24px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  margin: 0 auto 12px;
  border: 3px solid rgba(255, 255, 255, 0.2);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.loading-container p {
  color: white;
  font-size: 0.95rem;
}

/* ==================== 空状态 ==================== */
.empty-state {
  text-align: center;
  padding: 40px 20px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  color: white;
}

.empty-icon {
  font-size: 3.5rem;
  margin-bottom: 12px;
  opacity: 0.8;
}

.empty-state h3 {
  font-size: 1.3rem;
  margin-bottom: 8px;
}

.empty-state p {
  font-size: 0.95rem;
  margin-bottom: 16px;
  opacity: 0.9;
}

.empty-btn {
  display: inline-block;
  padding: 8px 24px;
  background: white;
  color: #4CAF50;
  text-decoration: none;
  border-radius: 30px;
  font-weight: 500;
  font-size: 0.9rem;
  transition: all 0.3s ease;
  border: none;
  cursor: pointer;
}

.empty-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
  background: #4CAF50;
  color: white;
}

/* ==================== 书籍网格 ==================== */
.book-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
  padding: 10px 0;
}

/* 书籍卡片 */
.book-card {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border-radius: 16px;
  overflow: hidden;
  transition: all 0.3s ease;
  cursor: pointer;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 8px 20px -8px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
}

.book-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 30px -10px rgba(0, 0, 0, 0.4);
  border-color: rgba(255, 255, 255, 0.4);
}

.book-cover-wrapper {
  position: relative;
  padding-top: 130%;
  overflow: hidden;
}

.book-cover {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.4s ease;
}

.book-card:hover .book-cover {
  transform: scale(1.05);
}

.book-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #4CAF50, #45a049);
}

.placeholder-text {
  font-size: 3rem;
  font-weight: bold;
  color: rgba(255, 255, 255, 0.9);
}

.book-status {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 0.7rem;
  font-weight: 600;
  z-index: 2;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
}

.book-status.available {
  background: rgba(76, 175, 80, 0.9);
  color: white;
}

.book-status.borrowed {
  background: rgba(244, 67, 54, 0.9);
  color: white;
}

.book-info {
  padding: 14px;
  flex: 1;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.1);
}

.book-title {
  font-size: 1rem;
  font-weight: 600;
  color: white;
  margin-bottom: 4px;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.book-author {
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.8rem;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.book-meta {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.book-publisher {
  background: rgba(255, 255, 255, 0.2);
  padding: 3px 8px;
  border-radius: 16px;
  font-size: 0.65rem;
  color: white;
}

.book-condition {
  padding: 3px 8px;
  border-radius: 16px;
  font-size: 0.65rem;
  font-weight: 500;
}

.book-condition.new {
  background: rgba(76, 175, 80, 0.3);
  color: #fff;
  border: 1px solid rgba(76, 175, 80, 0.5);
}

.book-condition.good {
  background: rgba(33, 150, 243, 0.3);
  color: #fff;
  border: 1px solid rgba(33, 150, 243, 0.5);
}

.book-condition.normal {
  background: rgba(255, 152, 0, 0.3);
  color: #fff;
  border: 1px solid rgba(255, 152, 0, 0.5);
}

.book-condition.worn {
  background: rgba(158, 158, 158, 0.3);
  color: #fff;
  border: 1px solid rgba(158, 158, 158, 0.5);
}

.book-footer {
  margin-top: auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
  padding-top: 8px;
}

.book-donor {
  color: rgba(255, 255, 255, 0.7);
  font-size: 0.7rem;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-link {
  color: white;
  font-size: 0.7rem;
  padding: 4px 8px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
  white-space: nowrap;
}

.detail-link:hover {
  background: white;
  color: #4CAF50;
}

/* ==================== 回到顶部按钮 ==================== */
.scroll-top {
  position: fixed;
  bottom: 24px;
  right: 24px;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: white;
  color: #4CAF50;
  border: none;
  font-size: 1.4rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
  z-index: 1000;
  border: 1px solid rgba(255, 255, 255, 0.5);
}

.scroll-top:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(255, 255, 255, 0.3);
  background: #4CAF50;
  color: white;
}

/* ==================== 响应式设计 ==================== */
@media (max-width: 768px) {
  .header {
    flex-direction: column;
    gap: 10px;
  }
  
  .header h1 {
    font-size: 1.5rem;
  }
  
  .nav a {
    padding: 6px 12px;
    font-size: 0.8rem;
  }
  
  .theme-switcher {
    position: absolute;
    top: 10px;
    right: 10px;
  }
  
  .intro {
    padding: 20px 15px;
  }
  
  .intro h2 {
    font-size: 1.5rem;
  }
  
  .intro p {
    font-size: 0.9rem;
  }
  
  .search-box {
    flex-direction: column;
    background: transparent;
    gap: 8px;
  }
  
  .search-input {
    background: rgba(255, 255, 255, 0.15);
    padding: 10px 16px;
  }
  
  .search-btn {
    width: 100%;
  }
  
  .book-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 15px;
  }
}

@media (max-width: 480px) {
  .nav {
    flex-direction: column;
    width: 100%;
  }
  
  .nav a {
    width: 100%;
    text-align: center;
  }
  
  .theme-panel {
    width: 250px;
    right: -10px;
  }
  
  .theme-colors {
    grid-template-columns: 1fr;
  }
  
  .book-grid {
    grid-template-columns: 1fr;
  }
  
  .scroll-top {
    bottom: 16px;
    right: 16px;
    width: 40px;
    height: 40px;
    font-size: 1.2rem;
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>