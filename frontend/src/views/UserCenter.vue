<template>
  <div class="user-center" :class="themeClass">
    <!-- 背景装饰 -->
    <div class="bg-decoration"></div>
    
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

    <h2>个人中心</h2>
    
    <!-- 用户信息卡片 -->
    <div class="user-info glass-card">
      <div class="avatar-section">
        <div class="avatar-container">
          <img :src="avatarUrl" alt="用户头像" class="avatar">
          <input type="file" id="avatar-upload" class="avatar-upload" @change="handleAvatarUpload" accept="image/*">
          <label for="avatar-upload" class="avatar-edit">
            <span class="edit-icon">📷</span>
          </label>
        </div>
      </div>
      <div class="info-content">
        <h3>用户信息</h3>
        <div class="info-item">
          <span class="label">学号:</span>
          <span class="value">{{ userInfo.studentId || '2021001' }}</span>
        </div>
        <div class="info-item">
          <span class="label">姓名:</span>
          <span class="value">{{ userInfo.name || '张三' }}</span>
        </div>
        <div class="info-item">
          <span class="label">手机号:</span>
          <span class="value">{{ userInfo.phone || '13800138000' }}</span>
        </div>
        <div class="info-item">
          <span class="label">邮箱:</span>
          <span class="value">{{ userInfo.email || 'zhangsan@example.com' }}</span>
        </div>
      </div>
      <div class="qrcode-section">
        <h3>用户专属二维码</h3>
        <div class="qrcode-container">
          <img :src="qrcodeUrl" alt="用户专属二维码" class="user-qrcode">
          <p class="qrcode-hint">用于认领和还书操作</p>
        </div>
      </div>
    </div>
    
    <!-- 统计信息 -->
    <div class="user-stats">
      <h3>统计信息</h3>
      <div class="stats-grid">
        <div class="stat-item glass-card">
          <div class="stat-value">{{ userStats.donation_count || 0 }}</div>
          <div class="stat-label">捐赠书籍</div>
        </div>
        <div class="stat-item glass-card">
          <div class="stat-value">{{ userStats.borrow_count || 0 }}</div>
          <div class="stat-label">当前借阅</div>
        </div>
        <div class="stat-item glass-card">
          <div class="stat-value">{{ userStats.total_borrow || 0 }}</div>
          <div class="stat-label">历史借阅</div>
        </div>
        <div class="stat-item glass-card">
          <div class="stat-value">{{ userStats.note_count || 0 }}</div>
          <div class="stat-label">分享笔记</div>
        </div>
      </div>
    </div>
    
    <!-- 勋章区域 -->
    <div class="medals">
      <div class="medals-header">
        <h3>我的勋章 ({{ unlockedMedals.length }})</h3>
        <router-link to="/medal-wall" class="view-all-link">
          查看全部勋章
          <span class="arrow-icon">→</span>
        </router-link>
      </div>
      
      <!-- 加载状态 -->
      <div v-if="medalsLoading" class="loading-state glass-card">
        <div class="loading-spinner"></div>
        <p>加载勋章中...</p>
      </div>
      
      <!-- 勋章列表 -->
      <div v-else class="medal-grid">
        <div v-if="unlockedMedals.length === 0" class="empty-medals glass-card">
          <p>暂无已解锁勋章</p>
          <p class="empty-hint">完成相应任务即可获得勋章</p>
        </div>
        <div v-else v-for="medal in unlockedMedals" :key="medal.id" class="medal-item glass-card" @click="showMedalDetail(medal)">
          <div class="medal-icon unlocked">
            {{ medal.icon }}
          </div>
          <div class="medal-name">{{ medal.name }}</div>
          <div class="medal-desc">{{ medal.description }}</div>
          <!-- 显示进度（如果是未完成的勋章） -->
          <div v-if="medal.current < medal.required" class="medal-progress">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: medal.progress + '%' }"></div>
            </div>
            <div class="progress-text">
              {{ medal.current }}/{{ medal.required }}
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 当前借阅 -->
    <div class="borrowed-books">
      <h3>当前借阅</h3>
      <div class="book-list">
        <div v-for="book in borrowedBooks" :key="book.id" class="book-item glass-card">
          <!-- 书籍封面 -->
          <div class="book-cover" v-if="book.bookCover">
            <img :src="book.bookCover" :alt="book.bookTitle">
          </div>
          <div class="book-cover-placeholder" v-else>
            📚
          </div>
          
          <!-- 书籍信息 -->
          <div class="book-info">
            <h4>{{ book.bookTitle || '未知书名' }}</h4>
            <p class="book-author">{{ book.bookAuthor || '未知作者' }}</p>
            <p class="borrow-date">借阅日期：{{ formatDate(book.borrowDate) }}</p>
            <p class="due-date">应还日期：{{ formatDate(book.dueDate) }}</p>
          </div>
        </div>
        
        <div v-if="borrowedBooks.length === 0" class="empty-books glass-card">
          <p>暂无借阅书籍</p>
        </div>
      </div>
    </div>

    <!-- 勋章详情弹窗 -->
    <div v-if="showModal" class="modal-overlay" @click="closeModal">
      <div class="modal-container" @click.stop>
        <div class="modal-header">
          <h3>勋章详情</h3>
          <button class="close-btn" @click="closeModal">&times;</button>
        </div>
        <div class="modal-content">
          <div class="modal-medal-icon" :class="{ 'unlocked': selectedMedal.unlocked }">
            {{ selectedMedal.icon }}
          </div>
          <div class="modal-medal-name">{{ selectedMedal.name }}</div>
          <div class="modal-medal-desc">{{ selectedMedal.description }}</div>
          
          <!-- 解锁要求 -->
          <div class="requirement-section">
            <h4>解锁要求</h4>
            <div class="requirement-item">
              <span class="requirement-label">{{ getRequirementText(selectedMedal) }}</span>
            </div>
          </div>

          <!-- 进度条 -->
          <div class="progress-section">
            <div class="progress-info">
              <span>当前进度</span>
              <span>{{ selectedMedal.current }}/{{ selectedMedal.required }}</span>
            </div>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: selectedMedal.progress + '%' }"></div>
            </div>
          </div>

          <!-- 解锁状态 -->
          <div class="status-section">
            <div class="status-badge" :class="{ 'unlocked': selectedMedal.unlocked, 'locked': !selectedMedal.unlocked }">
              {{ selectedMedal.unlocked ? '已解锁' : '未解锁' }}
            </div>
            <div v-if="!selectedMedal.unlocked" class="hint-text">
              还需 {{ selectedMedal.required - selectedMedal.current }} {{ getUnit(selectedMedal.type) }} 即可解锁
            </div>
            <div v-else class="unlock-time">
              解锁于: {{ formatDate(selectedMedal.unlockedAt) }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import QRCode from 'qrcode'

export default {
  name: 'UserCenter',
  data() {
    return {
      avatarUrl: 'https://via.placeholder.com/120',
      qrcodeUrl: '',
      showModal: false,
      selectedMedal: null,
      medals: [],
      medalsLoading: true,
      showThemePanel: false,
      currentTheme: 'default',
      userStats: {
        donation_count: 0,
        borrow_count: 0,
        total_borrow: 0,
        note_count: 0
      },
      userInfo: {
        studentId: '',
        name: '',
        phone: '',
        email: ''
      },
      borrowedBooks: [],
      apiBaseUrl: 'http://localhost:10011/api',
      themes: [
        {
          value: 'default',
          name: '经典蓝紫',
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
    unlockedMedals() {
      console.log('medals数组:', this.medals);
      const unlocked = this.medals.filter(medal => medal.unlocked === true);
      console.log('过滤后的已解锁勋章:', unlocked);
      return unlocked;
    },
    themeClass() {
      return `theme-${this.currentTheme}`
    }
  },
  async created() {
    await this.fetchUserData();
    await this.fetchBorrowedBooks();
    
    // 获取头像
    await this.fetchUserAvatar();
    
    // 从本地存储加载主题
    const savedTheme = localStorage.getItem('userCenterTheme')
    if (savedTheme) {
      this.currentTheme = savedTheme
    }
    
    // 监听统计信息更新事件
    window.addEventListener('stats-updated', this.handleStatsUpdated);

    // 添加对借阅更新事件的监听
    window.addEventListener('borrowed-books-updated', this.handleBorrowedBooksUpdated);
    
    // 点击其他地方关闭主题面板
    document.addEventListener('click', this.handleClickOutside);
  },
  beforeDestroy() {
    // 移除事件监听
    window.removeEventListener('stats-updated', this.handleStatsUpdated);
    window.removeEventListener('borrowed-books-updated', this.handleBorrowedBooksUpdated);
    document.removeEventListener('click', this.handleClickOutside);
  },
  mounted() {
    this.generateUserQRCode();
  },
  methods: {
    // 处理点击外部关闭主题面板
    handleClickOutside(event) {
      const themeSwitcher = this.$el.querySelector('.theme-switcher')
      if (themeSwitcher && !themeSwitcher.contains(event.target)) {
        this.showThemePanel = false
      }
    },
    
    // 切换主题
    changeTheme(themeValue) {
      this.currentTheme = themeValue
      localStorage.setItem('userCenterTheme', themeValue)
      this.showThemePanel = false
    },
    
    // 处理借阅更新事件
    async handleBorrowedBooksUpdated(event) {
      console.log('收到借阅更新事件:', event.detail);
      const userId = event.detail.userId || this.getCurrentUserId();
      
      // 重新获取借阅列表
      await this.fetchBorrowedBooks();
      
      // 重新获取统计信息
      const token = localStorage.getItem('token');
      await this.fetchUserStats(userId, token);
    },
    
    // 获取头像
    async fetchUserAvatar() {
      try {
        const userId = this.getCurrentUserId();
        const token = localStorage.getItem('token');
        
        const response = await fetch(`${this.apiBaseUrl}/users/avatar/${userId}`, {
          headers: {
            'Authorization': token ? `Bearer ${token}` : ''
          }
        });
        
        if (response.ok) {
          const data = await response.json();
          if (data.success && data.avatarUrl) {
            this.avatarUrl = data.avatarUrl;
            
            // 更新 localStorage
            const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
            userInfo.avatarUrl = data.avatarUrl;
            localStorage.setItem('userInfo', JSON.stringify(userInfo));
          }
        }
      } catch (error) {
        console.error('获取头像失败:', error);
      }
    },
    
    // 上传头像
    async handleAvatarUpload(event) {
      const file = event.target.files[0];
      if (!file) return;
      
      // 检查文件大小（限制为2MB）
      if (file.size > 2 * 1024 * 1024) {
        alert('图片大小不能超过2MB');
        return;
      }
      
      // 检查图片格式
      if (!file.type.startsWith('image/')) {
        alert('请选择图片文件');
        return;
      }
      
      const formData = new FormData();
      formData.append('file', file);
      
      try {
        const userId = this.getCurrentUserId();
        const token = localStorage.getItem('token');
        
        console.log('上传头像，用户ID:', userId);
        console.log('请求URL:', `${this.apiBaseUrl}/users/avatar/${userId}`);
        
        const response = await fetch(`${this.apiBaseUrl}/users/avatar/${userId}`, {
          method: 'POST',
          headers: {
            'Authorization': token ? `Bearer ${token}` : ''
          },
          body: formData
        });
        
        console.log('上传响应状态:', response.status);
        
        if (!response.ok) {
          throw new Error(`HTTP错误: ${response.status}`);
        }
        
        const data = await response.json();
        console.log('上传响应数据:', data);
        
        if (data.success) {
          this.avatarUrl = data.avatarUrl;
          
          // 更新 localStorage
          const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
          userInfo.avatarUrl = data.avatarUrl;
          localStorage.setItem('userInfo', JSON.stringify(userInfo));
          
          alert('头像上传成功');
        } else {
          alert(data.message || '上传失败');
        }
      } catch (error) {
        console.error('上传头像失败:', error);
        alert('上传失败，请重试: ' + error.message);
      }
    },
    
    // 处理统计信息更新
    async handleStatsUpdated(event) {
      console.log('收到统计更新事件:', event.detail);
      const userId = this.getCurrentUserId();
      const token = localStorage.getItem('token');
      
      // 只重新获取统计信息，不重新获取勋章数据
      await this.fetchUserStats(userId, token);
      console.log('统计信息已刷新');
    },
    
    async fetchUserData() {
      this.medalsLoading = true;
      try {
        const userId = this.getCurrentUserId();
        const token = localStorage.getItem('token');
        
        console.log('===== 开始获取用户数据 =====');
        console.log('用户ID:', userId);
        
        // 1. 获取勋章数据
        await this.fetchMedals(userId, token);
        
        // 2. 获取统计信息
        await this.fetchUserStats(userId, token);
        
        // 3. 获取用户基本信息
        this.getUserInfoFromStorage();
        
      } catch (error) {
        console.error('获取用户数据失败:', error);
        this.useMockMedals();
        this.useMockStats();
      } finally {
        this.medalsLoading = false;
      }
    },
    
    // 专门获取勋章数据的方法
    async fetchMedals(userId, token) {
      try {
        const medalsResponse = await fetch(`${this.apiBaseUrl}/medals/${userId}`, {
          headers: {
            'Authorization': token ? `Bearer ${token}` : ''
          }
        });
        
        const medalsData = await medalsResponse.json();
        console.log('勋章API返回数据:', medalsData);
        
        if (medalsData.success && Array.isArray(medalsData.data)) {
          // 处理勋章数据
          if (medalsData.data.length > 0) {
            const firstItem = medalsData.data[0];
            
            if (firstItem.medal) {
              // 有 medal 嵌套
              this.medals = medalsData.data.map(item => ({
                id: item.medal.id,
                name: item.medal.name,
                description: item.medal.description,
                icon: item.medal.icon,
                type: item.medal.type,
                required: item.medal.required,
                current: item.current || 0,
                unlocked: item.unlocked || false,
                progress: Math.min(100, Math.floor(((item.current || 0) / item.medal.required) * 100)),
                unlockedAt: item.unlockedAt
              }));
            } else {
              // 直接是勋章对象
              this.medals = medalsData.data.map(item => ({
                id: item.id,
                name: item.name,
                description: item.description,
                icon: item.icon,
                type: item.type,
                required: item.required,
                current: item.current || 0,
                unlocked: item.unlocked || false,
                progress: Math.min(100, Math.floor(((item.current || 0) / item.required) * 100)),
                unlockedAt: item.unlockedAt
              }));
            }
          } else {
            this.medals = [];
          }
        } else {
          console.error('获取勋章失败，使用模拟数据');
          this.useMockMedals();
        }
      } catch (error) {
        console.error('获取勋章数据出错:', error);
        this.useMockMedals();
      }
    },
    
    // 专门获取统计信息的方法
    async fetchUserStats(userId, token) {
      try {
        console.log('开始获取统计信息, userId:', userId);
        console.log('请求URL:', `${this.apiBaseUrl}/statistics/${userId}`);
        
        const statsResponse = await fetch(`${this.apiBaseUrl}/statistics/${userId}`, {
          headers: {
            'Authorization': token ? `Bearer ${token}` : ''
          }
        });
        
        console.log('统计信息响应状态:', statsResponse.status);
        
        const statsData = await statsResponse.json();
        console.log('统计信息原始数据:', statsData);
        
        if (statsData.success) {
          this.userStats = statsData.data;
          console.log('统计信息更新成功:', this.userStats);
        } else {
          console.log('统计信息获取失败，使用模拟数据');
          this.useMockStats();
        }
      } catch (error) {
        console.error('获取统计信息失败:', error);
        this.useMockStats();
      }
    },
    
    // 获取借阅列表
    async fetchBorrowedBooks() {
      try {
        const userInfo = JSON.parse(localStorage.getItem('userInfo'));
        const userId = userInfo.id;
        const token = localStorage.getItem('token');
        
        console.log('正在获取借阅书籍，用户ID:', userId);
        
        const response = await fetch(`${this.apiBaseUrl}/borrow/current/user/${userId}`, {
          headers: {
            'Authorization': token ? `Bearer ${token}` : ''
          }
        });
        
        const data = await response.json();
        console.log('原始返回数据:', data);
        
        if (data.success && Array.isArray(data.data)) {
          // 正确映射所有字段
          this.borrowedBooks = data.data.map(record => ({
            id: record.id,
            bookId: record.bookId,
            bookTitle: record.bookTitle || '未知书名',
            bookAuthor: record.bookAuthor || '未知作者',
            bookCover: record.bookCover || '',
            borrowDate: record.borrowDate,
            dueDate: record.dueDate
          }));
          
          console.log('处理后的借阅列表:', this.borrowedBooks);
        } else {
          this.borrowedBooks = [];
        }
      } catch (error) {
        console.error('获取借阅书籍失败:', error);
        this.borrowedBooks = [];
      }
    },
    
    // 格式化日期
    formatDate(dateStr) {
      if (!dateStr) return '未知日期';
      try {
        const date = new Date(dateStr);
        // 转换为本地日期格式 YYYY/MM/DD
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}/${month}/${day}`;
      } catch (e) {
        return dateStr;
      }
    },
    
    useMockMedals() {
      this.medals = [
        {
          id: 1,
          name: '初次捐赠',
          description: '捐赠第一本书',
          icon: '🏆',
          type: 'donation',
          required: 1,
          current: 1,
          progress: 100,
          unlocked: true,
          unlockedAt: '2024-03-01T10:00:00Z'
        },
        {
          id: 2,
          name: '捐赠达人',
          description: '捐赠5本书',
          icon: '🌟',
          type: 'donation',
          required: 5,
          current: 5,
          progress: 100,
          unlocked: true,
          unlockedAt: '2024-03-15T14:30:00Z'
        },
        {
          id: 3,
          name: '借阅新手',
          description: '首次借阅',
          icon: '📚',
          type: 'borrow',
          required: 1,
          current: 1,
          progress: 100,
          unlocked: true,
          unlockedAt: '2024-03-05T09:20:00Z'
        },
        {
          id: 4,
          name: '阅读爱好者',
          description: '借阅10本书',
          icon: '📖',
          type: 'borrow',
          required: 10,
          current: 8,
          progress: 80,
          unlocked: false
        },
        {
          id: 5,
          name: '笔记分享者',
          description: '分享5篇笔记',
          icon: '✍️',
          type: 'note',
          required: 5,
          current: 5,
          progress: 100,
          unlocked: true,
          unlockedAt: '2024-03-10T16:45:00Z'
        }
      ];
    },
    
    useMockStats() {
      this.userStats = {
        donation_count: 5,
        borrow_count: 3,
        total_borrow: 12,
        note_count: 8
      };
    },
    
    getUserInfoFromStorage() {
      const userInfoStr = localStorage.getItem('userInfo');
      if (userInfoStr) {
        try {
          this.userInfo = JSON.parse(userInfoStr);
          
          // 如果有保存的头像URL，也加载进来
          if (this.userInfo.avatarUrl) {
            this.avatarUrl = this.userInfo.avatarUrl;
          }
        } catch (e) {
          console.error('解析用户信息失败:', e);
          this.setDefaultUserInfo();
        }
      } else {
        this.setDefaultUserInfo();
      }
    },
    
    setDefaultUserInfo() {
      this.userInfo = {
        studentId: '2021001',
        name: '张三',
        phone: '13800138000',
        email: 'zhangsan@example.com'
      };
    },
    
    getCurrentUserId() {
      const userInfoStr = localStorage.getItem('userInfo');
      if (userInfoStr) {
        try {
          const userInfo = JSON.parse(userInfoStr);
          return userInfo.id || 1;
        } catch (e) {
          return 1;
        }
      }
      return 1;
    },
    
    async generateUserQRCode() {
      try {
        const userInfo = this.userInfo;
        const userData = `studentId=${userInfo.studentId}&name=${encodeURIComponent(userInfo.name)}&phone=${userInfo.phone}&action=user`;
        this.qrcodeUrl = await QRCode.toDataURL(userData);
      } catch (error) {
        console.error('生成二维码失败:', error);
      }
    },
    
    showMedalDetail(medal) {
      this.selectedMedal = medal;
      this.showModal = true;
      document.body.style.overflow = 'hidden';
    },
    
    closeModal() {
      this.showModal = false;
      this.selectedMedal = null;
      document.body.style.overflow = '';
    },
    
    // 归还书籍
    async returnBook(bookId) {
      try {
        const userId = this.getCurrentUserId();
        const token = localStorage.getItem('token');
        
        console.log('归还书籍，bookId:', bookId);
        
        const response = await fetch(`${this.apiBaseUrl}/borrow/return/${bookId}`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': token ? `Bearer ${token}` : ''
          }
        });
        
        console.log('归还响应状态:', response.status);
        const data = await response.json();
        console.log('归还响应数据:', data);
        
        if (data.success) {
          alert('归还成功');
          
          // 重新获取借阅列表
          await this.fetchBorrowedBooks();
          
          // 重新获取统计信息
          await this.fetchUserStats(userId, token);
          
          // 触发统计更新事件
          window.dispatchEvent(new CustomEvent('stats-updated', { 
            detail: { userId: userId }
          }));
        } else {
          alert(data.message || '归还失败');
        }
      } catch (error) {
        console.error('归还失败:', error);
        alert('归还失败，请稍后重试');
      }
    },
    
    getRequirementText(medal) {
      const typeMap = {
        donation: '捐赠书籍',
        borrow: '阅读书籍',
        note: '发布笔记'
      };
      return `${typeMap[medal.type] || '完成'}数量达到 ${medal.required} 本/条`;
    },
    
    getUnit(type) {
      const unitMap = {
        donation: '本',
        borrow: '本',
        note: '条'
      };
      return unitMap[type] || '个';
    }
  },
  watch: {
    userInfo: {
      handler() {
        this.generateUserQRCode();
      },
      deep: true
    }
  }
}
</script>

<style scoped>
/* ==================== 全局样式（完全对齐 Home.vue） ==================== */
.user-center {
  min-height: 100vh;
  background-attachment: fixed;
  position: relative;
  transition: background 0.5s ease;
  padding: 20px;
}

/* 背景装饰（完全对齐 Home.vue） */
.bg-decoration {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" opacity="0.05"><path d="M20 20 L80 20 L80 80 L20 80 Z" fill="none" stroke="white" stroke-width="2"/><circle cx="50" cy="50" r="20" fill="none" stroke="white" stroke-width="2"/></svg>') repeat;
  pointer-events: none;
  z-index: 0;
}

/* 主题样式 - 完全对齐 Home.vue */
.user-center.theme-default {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #4facfe 100%);
}

.user-center.theme-green {
  background: linear-gradient(135deg, #dbdbdb 0%, #2f6e62 100%);
}

.user-center.theme-blue {
  background: linear-gradient(135deg, #84fab0 0%, #8fd3f4 100%);
}

.user-center.theme-orange {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

.user-center.theme-pink {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.user-center.theme-purple {
  background: linear-gradient(135deg, #5f2c82 0%, #49a09d 100%);
}

/* ==================== 毛玻璃卡片样式 ==================== */
.glass-card {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 8px 20px -8px rgba(0, 0, 0, 0.3);
  transition: all 0.3s ease;
}

.glass-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 16px 30px -10px rgba(0, 0, 0, 0.4);
  border-color: rgba(255, 255, 255, 0.4);
  background: rgba(255, 255, 255, 0.2);
}

/* ==================== 主题切换器（完全对齐 Home.vue） ==================== */
.theme-switcher {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 100;
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

/* ==================== 标题样式 ==================== */
h2 {
  text-align: center;
  margin-bottom: 30px;
  font-size: 2rem;
  font-weight: 600;
  color: white;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.2);
  position: relative;
  z-index: 1;
}

h3 {
  color: white;
  font-size: 1.3rem;
  font-weight: 600;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
  margin-bottom: 15px;
}

/* ==================== 用户信息卡片 ==================== */
.user-info {
  margin-bottom: 30px;
  padding: 25px;
  display: flex;
  align-items: flex-start;
  gap: 30px;
  flex-wrap: wrap;
  border-radius: 16px;
  position: relative;
  z-index: 1;
}

.avatar-section {
  flex-shrink: 0;
}

.avatar-container {
  position: relative;
  width: 120px;
  height: 120px;
}

.avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.2);
  transition: transform 0.3s ease;
}

.avatar:hover {
  transform: scale(1.05);
}

.avatar-upload {
  display: none;
}

.avatar-edit {
  position: absolute;
  bottom: 0;
  right: 0;
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(5px);
  color: white;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid white;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
}

.avatar-edit:hover {
  background: rgba(255, 255, 255, 0.5);
  transform: scale(1.1);
}

.edit-icon {
  font-size: 18px;
}

.info-content {
  flex: 1;
}

.info-item {
  display: flex;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(5px);
  border-radius: 8px;
  transition: all 0.3s ease;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.info-item:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateX(5px);
}

.label {
  width: 80px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}

.value {
  color: white;
  font-weight: 500;
}

/* 二维码区域 */
.qrcode-section {
  flex-shrink: 0;
  text-align: center;
}

.qrcode-section h3 {
  margin-bottom: 15px;
  font-size: 16px;
  color: white;
}

.qrcode-container {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.user-qrcode {
  width: 150px;
  height: 150px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 12px;
  padding: 10px;
  background-color: white;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.2);
  transition: transform 0.3s ease;
}

.user-qrcode:hover {
  transform: scale(1.05);
}

.qrcode-hint {
  margin-top: 10px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  text-align: center;
}

/* ==================== 统计信息 ==================== */
.user-stats {
  margin-bottom: 30px;
  position: relative;
  z-index: 1;
}

.user-stats h3 {
  margin-bottom: 20px;
  padding-left: 10px;
  border-left: 4px solid rgba(255, 255, 255, 0.8);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 20px;
}

.stat-item {
  padding: 25px 20px;
  border-radius: 16px;
  text-align: center;
}

.stat-value {
  font-size: 2rem;
  font-weight: bold;
  color: white;
  margin-bottom: 10px;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.2);
}

.stat-label {
  font-size: 0.9rem;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
}

/* ==================== 勋章区域 ==================== */
.medals {
  margin-bottom: 30px;
  position: relative;
  z-index: 1;
}

.medals-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-left: 10px;
  border-left: 4px solid rgba(255, 255, 255, 0.8);
}

.medals-header h3 {
  margin: 0;
  border: none;
  padding: 0;
}

.view-all-link {
  display: flex;
  align-items: center;
  gap: 6px;
  color: white;
  text-decoration: none;
  font-size: 0.9rem;
  font-weight: 500;
  padding: 8px 16px;
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(5px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
}

.view-all-link:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateX(2px);
}

.arrow-icon {
  font-size: 16px;
  transition: transform 0.3s ease;
}

.view-all-link:hover .arrow-icon {
  transform: translateX(4px);
}

.loading-state {
  text-align: center;
  padding: 40px 20px;
  border-radius: 16px;
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

.loading-state p {
  color: white;
  font-size: 0.95rem;
}

.medal-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 20px;
}

.medal-item {
  text-align: center;
  padding: 20px;
  border-radius: 16px;
  cursor: pointer;
}

.medal-icon {
  font-size: 3rem;
  margin-bottom: 15px;
  filter: drop-shadow(0 5px 10px rgba(0, 0, 0, 0.2));
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.1); }
  100% { transform: scale(1); }
}

.medal-name {
  font-weight: 600;
  margin-bottom: 8px;
  color: white;
  font-size: 1rem;
}

.medal-desc {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 15px;
  line-height: 1.4;
}

.medal-progress {
  margin-top: 10px;
}

.progress-bar {
  width: 100%;
  height: 6px;
  background-color: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 5px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #4CAF50, #8BC34A);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 0.7rem;
  color: rgba(255, 255, 255, 0.8);
}

.empty-medals {
  grid-column: 1 / -1;
  text-align: center;
  padding: 40px 20px;
  border-radius: 16px;
  color: white;
}

.empty-hint {
  font-size: 0.8rem;
  margin-top: 10px;
  color: rgba(255, 255, 255, 0.7);
}

/* ==================== 当前借阅 ==================== */
.borrowed-books {
  margin-bottom: 30px;
  position: relative;
  z-index: 1;
}

.borrowed-books h3 {
  margin-bottom: 20px;
  padding-left: 10px;
  border-left: 4px solid rgba(255, 255, 255, 0.8);
}

.book-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.book-item {
  display: flex;
  gap: 15px;
  padding: 15px;
  border-radius: 12px;
}

.book-cover, .book-cover-placeholder {
  width: 80px;
  height: 100px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
}

.book-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.book-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.book-info h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: white;
}

.book-author {
  margin: 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
}

.borrow-date, .due-date {
  margin: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
}

.due-date {
  color: #ff9800;
  font-weight: 500;
}

.return-btn {
  align-self: flex-end;
  padding: 8px 16px;
  background-color: #f44336;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.return-btn:hover {
  background-color: #d32f2f;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
}

.empty-books {
  grid-column: 1 / -1;
  text-align: center;
  padding: 40px;
  border-radius: 16px;
  color: white;
}

/* 弹窗样式保持不变 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  animation: fadeIn 0.2s ease;
  backdrop-filter: blur(5px);
}

.modal-container {
  background: white;
  border-radius: 24px;
  width: 90%;
  max-width: 400px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 30px 60px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.3s ease;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #eee;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 24px 24px 0 0;
}

.modal-header h3 {
  margin: 0;
  color: white;
  font-size: 1.3rem;
  font-weight: 600;
  border: none;
  padding: 0;
}

.close-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: white;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.1);
}

.modal-content {
  padding: 24px;
  text-align: center;
}

.modal-medal-icon {
  font-size: 5rem;
  margin-bottom: 16px;
  filter: drop-shadow(0 10px 20px rgba(0, 0, 0, 0.1));
}

.modal-medal-icon.unlocked {
  animation: modalPulse 2s infinite;
}

@keyframes modalPulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.1); }
  100% { transform: scale(1); }
}

.modal-medal-name {
  font-size: 1.8rem;
  font-weight: bold;
  color: #2c3e50;
  margin-bottom: 8px;
}

.modal-medal-desc {
  font-size: 1rem;
  color: #666;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
}

.requirement-section {
  text-align: left;
  margin-bottom: 20px;
}

.requirement-section h4 {
  color: #2c3e50;
  font-size: 1rem;
  margin-bottom: 12px;
}

.requirement-item {
  background: linear-gradient(135deg, #f5f7fa 0%, #e9ecef 100%);
  padding: 12px 16px;
  border-radius: 12px;
}

.requirement-label {
  color: #4CAF50;
  font-size: 0.9rem;
  line-height: 1.5;
  font-weight: 500;
}

/* 动画 */
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .theme-switcher {
    top: 10px;
    right: 10px;
  }
  
  .theme-panel {
    width: 250px;
  }
  
  .user-info {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  
  .info-item {
    justify-content: center;
  }
  
  .label {
    width: auto;
    margin-right: 10px;
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .book-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .theme-colors {
    grid-template-columns: 1fr;
  }
  
  .medal-grid {
    grid-template-columns: 1fr;
  }
}
</style>