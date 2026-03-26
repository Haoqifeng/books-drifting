<template>
  <div class="medal-wall">
    <div class="header-section">
      <h2>✨ 荣耀勋章墙 ✨</h2>
      <router-link to="/user" class="back-link">
        <span class="arrow-icon">←</span> 返回个人中心
      </router-link>
    </div>
    
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载勋章中...</p>
    </div>
    
    <!-- 错误状态 -->
    <div v-else-if="error" class="error-state">
      <p class="error-icon">❌</p>
      <p class="error-message">{{ error }}</p>
      <button class="retry-btn" @click="fetchMedals">重新加载</button>
    </div>
    
    <!-- 正常显示勋章内容 -->
    <div v-else>
      <!-- 捐赠模块 -->
      <div class="medal-section">
        <h3 class="section-title">📦 捐赠模块</h3>
        <div v-if="donationMedals.length === 0" class="empty-module">
          暂无捐赠勋章
        </div>
        <div v-else class="medal-grid">
          <div v-for="medal in donationMedals" :key="medal.id" class="medal-item">
            <div class="medal-icon" :class="{ 'unlocked': medal.unlocked }">
              {{ medal.icon }}
            </div>
            <div class="medal-name">{{ medal.name }}</div>
            <div class="medal-desc">{{ medal.description }}</div>
            <div class="medal-progress">
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
      
      <!-- 阅读模块 -->
      <div class="medal-section">
        <h3 class="section-title">📖 阅读模块</h3>
        <div v-if="readingMedals.length === 0" class="empty-module">
          暂无阅读勋章
        </div>
        <div v-else class="medal-grid">
          <div v-for="medal in readingMedals" :key="medal.id" class="medal-item">
            <div class="medal-icon" :class="{ 'unlocked': medal.unlocked }">
              {{ medal.icon }}
            </div>
            <div class="medal-name">{{ medal.name }}</div>
            <div class="medal-desc">{{ medal.description }}</div>
            <div class="medal-progress">
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
      
      <!-- 笔记模块 -->
      <div class="medal-section">
        <h3 class="section-title">📝 笔记模块</h3>
        <div v-if="noteMedals.length === 0" class="empty-module">
          暂无笔记勋章
        </div>
        <div v-else class="medal-grid">
          <div v-for="medal in noteMedals" :key="medal.id" class="medal-item">
            <div class="medal-icon" :class="{ 'unlocked': medal.unlocked }">
              {{ medal.icon }}
            </div>
            <div class="medal-name">{{ medal.name }}</div>
            <div class="medal-desc">{{ medal.description }}</div>
            <div class="medal-progress">
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
    </div>
  </div>
</template>

<script>
export default {
  name: 'MedalWall',
  data() {
    return {
      allMedals: [],
      loading: true,
      error: null,
      apiBaseUrl: 'http://localhost:10011/api'  // 使用代理路径
    }
  },
  computed: {
    donationMedals() {
      return this.allMedals.filter(medal => medal.type === 'donation');
    },
    readingMedals() {
      return this.allMedals.filter(medal => medal.type === 'borrow');
    },
    noteMedals() {
      return this.allMedals.filter(medal => medal.type === 'note');
    }
  },
  async created() {
    await this.fetchMedals();
  },
  methods: {
    async fetchMedals() {
      this.loading = true;
      this.error = null;
      
      try {
        const userId = this.getCurrentUserId();
        const token = localStorage.getItem('token');
        
        console.log('正在获取勋章数据，用户ID:', userId);
        console.log('请求URL:', `${this.apiBaseUrl}/medals/${userId}`);
        
        const response = await fetch(`${this.apiBaseUrl}/medals/${userId}`, {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': token ? `Bearer ${token}` : ''
          }
        });
        
        if (!response.ok) {
          throw new Error(`HTTP错误: ${response.status}`);
        }
        
        const data = await response.json();
        console.log('获取到勋章数据:', data);
        
        // 处理返回的数据
        if (data.success && Array.isArray(data.data)) {
          // 检查第一条数据的结构，决定如何处理
          if (data.data.length > 0) {
            const firstItem = data.data[0];
            console.log('第一条数据结构:', firstItem);
            
            // 根据实际结构处理
            if (firstItem.medal) {
              // 格式1: 有 medal 嵌套
              this.allMedals = data.data.map(item => ({
                id: item.medal.id,
                name: item.medal.name,
                description: item.medal.description,
                icon: item.medal.icon,
                type: item.medal.type,
                required: item.medal.required,
                current: item.current || 0,
                unlocked: item.unlocked || false,
                progress: Math.min(100, Math.floor(((item.current || 0) / item.medal.required) * 100))
              }));
            } else {
              // 格式2: 直接是勋章对象
              this.allMedals = data.data.map(item => ({
                id: item.id,
                name: item.name,
                description: item.description,
                icon: item.icon,
                type: item.type,
                required: item.required,
                current: item.current || 0,
                unlocked: item.unlocked || false,
                progress: Math.min(100, Math.floor(((item.current || 0) / item.required) * 100))
              }));
            }
          } else {
            // 空数组
            this.allMedals = [];
          }
          
          console.log('转换后的勋章数据:', this.allMedals);
        } else {
          console.log('返回数据格式错误，使用模拟数据');
          this.useMockMedals();
        }
      } catch (error) {
        console.error('获取勋章数据失败:', error);
        this.error = error.message || '加载失败，请稍后重试';
        // 出错时使用模拟数据
        this.useMockMedals();
      } finally {
        this.loading = false;
      }
    },
    
    // 模拟数据（备选方案）
    useMockMedals() {
      this.allMedals = [
        {
          id: 1,
          name: '初次捐赠',
          description: '捐赠第一本书',
          icon: '🏆',
          type: 'donation',
          required: 1,
          current: 1,
          progress: 100,
          unlocked: true
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
          unlocked: true
        },
        {
          id: 3,
          name: '捐赠大师',
          description: '捐赠10本书',
          icon: '👑',
          type: 'donation',
          required: 10,
          current: 5,
          progress: 50,
          unlocked: false
        },
        {
          id: 4,
          name: '借阅新手',
          description: '首次借阅',
          icon: '📚',
          type: 'borrow',
          required: 1,
          current: 1,
          progress: 100,
          unlocked: true
        },
        {
          id: 5,
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
          id: 6,
          name: '阅读达人',
          description: '借阅20本书',
          icon: '📚',
          type: 'borrow',
          required: 20,
          current: 8,
          progress: 40,
          unlocked: false
        },
        {
          id: 7,
          name: '笔记分享者',
          description: '分享5篇笔记',
          icon: '✍️',
          type: 'note',
          required: 5,
          current: 5,
          progress: 100,
          unlocked: true
        },
        {
          id: 8,
          name: '笔记达人',
          description: '分享10篇笔记',
          icon: '📝',
          type: 'note',
          required: 10,
          current: 5,
          progress: 50,
          unlocked: false
        },
        {
          id: 9,
          name: '笔记大师',
          description: '分享20篇笔记',
          icon: '📔',
          type: 'note',
          required: 20,
          current: 5,
          progress: 25,
          unlocked: false
        }
      ];
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
    }
  }
}
</script>

<style scoped>
.medal-wall {
  max-width: 1300px;
  margin: 0 auto;
  padding: 36px 40px;
  background: rgba(255, 255, 255, 0.68);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  border-radius: 48px;
  box-shadow: 0 25px 50px -12px rgba(0, 32, 64, 0.25),
              inset 0 1px 2px rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.6);
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 15px;
}

h2 {
  font-size: 2.6rem;
  font-weight: 600;
  letter-spacing: 2px;
  background: linear-gradient(135deg, #2c3e50, #1e7b5e, #2d6a4f);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  text-shadow: 0 2px 10px rgba(46, 125, 89, 0.2);
  margin: 0;
}

.back-link {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #4CAF50;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  padding: 8px 16px;
  border-radius: 20px;
  background-color: rgba(76, 175, 80, 0.1);
  transition: all 0.3s ease;
}

.back-link:hover {
  background-color: #4CAF50;
  color: white;
  transform: translateX(-2px);
}

.arrow-icon {
  font-size: 16px;
  transition: transform 0.3s ease;
}

.back-link:hover .arrow-icon {
  transform: translateX(-4px);
}

/* 加载状态 */
.loading-state {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  margin: 0 auto 20px;
  border: 3px solid #f0f0f0;
  border-top-color: #4CAF50;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 错误状态 */
.error-state {
  text-align: center;
  padding: 60px 20px;
}

.error-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.error-message {
  margin-bottom: 20px;
  color: #666;
}

.retry-btn {
  background-color: #4CAF50;
  color: white;
  border: none;
  padding: 10px 24px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s;
}

.retry-btn:hover {
  background-color: #45a049;
}

/* 空模块状态 */
.empty-module {
  text-align: center;
  padding: 40px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 32px;
  color: #999;
  font-size: 14px;
}

.medal-section {
  margin-bottom: 48px;
}

.section-title {
  font-size: 1.8rem;
  font-weight: 500;
  padding-left: 20px;
  margin-bottom: 28px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-title::before {
  content: '';
  width: 10px;
  height: 40px;
  background: linear-gradient(0deg, #2a9d8f, #4caf92);
  border-radius: 20px;
  box-shadow: 0 4px 10px rgba(42, 157, 143, 0.5);
}

.medal-section:nth-of-type(1) .section-title::before {
  background: linear-gradient(0deg, #e76f51, #f4a261);
}
.medal-section:nth-of-type(2) .section-title::before {
  background: linear-gradient(0deg, #2a9d8f, #2f9e79);
}
.medal-section:nth-of-type(3) .section-title::before {
  background: linear-gradient(0deg, #8e7cc3, #b7a9e0);
}

/* 勋章网格 */
.medal-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
}

/* 勋章卡片 */
.medal-item {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  border-radius: 24px;
  padding: 20px 15px 18px;
  text-align: center;
  box-shadow: 0 10px 20px -8px rgba(0, 40, 20, 0.12);
  transition: all 0.3s cubic-bezier(0.2, 0, 0, 1);
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(255, 255, 255, 0.7);
}

.medal-item:hover {
  transform: translateY(-8px) scale(1.02);
  box-shadow: 0 30px 40px -20px rgba(30, 80, 60, 0.4),
              0 0 0 2px rgba(255, 255, 255, 0.8) inset,
              0 0 30px 2px rgba(72, 187, 120, 0.2);
  background: rgba(255, 255, 255, 0.9);
}

/* 勋章图标 */
.medal-icon {
  font-size: 56px;
  line-height: 1;
  margin-bottom: 12px;
  filter: drop-shadow(0 8px 10px rgba(0, 0, 0, 0.1));
  transition: filter 0.3s, transform 0.3s;
  transform-origin: center;
  opacity: 0.6;
  transform: scale(0.95);
  position: relative;
  display: inline-block;
}

.medal-icon.unlocked {
  opacity: 1;
  transform: scale(1);
  filter: drop-shadow(0 6px 12px rgba(255, 215, 0, 0.4));
  animation: floatGlow 3s ease-in-out infinite;
}

/* 解锁标记 */
.medal-icon.unlocked::after {
  content: '✓';
  position: absolute;
  font-size: 18px;
  background: #2ecc71;
  color: white;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  bottom: 0;
  right: -6px;
  box-shadow: 0 3px 8px #2ecc71;
  border: 2px solid white;
  font-weight: bold;
  animation: pop 0.3s;
}

@keyframes floatGlow {
  0% { filter: drop-shadow(0 6px 10px rgba(255, 200, 50, 0.4)); transform: scale(1); }
  50% { filter: drop-shadow(0 12px 18px rgba(255, 180, 40, 0.6)); transform: scale(1.05); }
  100% { filter: drop-shadow(0 6px 10px rgba(255, 200, 50, 0.4)); transform: scale(1); }
}

@keyframes pop {
  0% { transform: scale(0); }
  80% { transform: scale(1.1); }
  100% { transform: scale(1); }
}

/* 勋章名称 */
.medal-name {
  font-size: 1.2rem;
  font-weight: 600;
  margin-bottom: 6px;
  background: linear-gradient(145deg, #1e293b, #2d4a3e);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  letter-spacing: 0.3px;
}

/* 勋章描述 */
.medal-desc {
  font-size: 0.85rem;
  color: #3b5e4b;
  background: rgba(220, 240, 230, 0.6);
  padding: 6px 10px;
  border-radius: 40px;
  margin: 6px 0 12px;
  font-weight: 500;
  backdrop-filter: blur(2px);
  border: 1px solid rgba(130, 200, 160, 0.5);
  width: fit-content;
  max-width: 90%;
  margin-left: auto;
  margin-right: auto;
}

.medal-progress {
  margin-top: auto;
  width: 100%;
}

/* 进度条 */
.progress-bar {
  width: 100%;
  height: 8px;
  background: rgba(190, 200, 200, 0.3);
  border-radius: 20px;
  overflow: hidden;
  margin-bottom: 6px;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(2px);
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #34b48c, #71cfb0, #2f9e79);
  border-radius: 20px;
  transition: width 0.8s ease-out;
  box-shadow: 0 0 6px #2ecc71;
}

/* 进度文字 */
.progress-text {
  font-size: 0.75rem;
  font-weight: 600;
  color: #1b5e3f;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(4px);
  padding: 3px 10px;
  border-radius: 30px;
  display: inline-block;
  border: 1px solid #cae3d7;
  letter-spacing: 0.2px;
}

.medal-section:nth-of-type(1) .progress-fill {
  background: linear-gradient(90deg, #f4a261, #e9b384, #e76f51);
}
.medal-section:nth-of-type(2) .progress-fill {
  background: linear-gradient(90deg, #3b9e8b, #52b9a5, #2a9d8f);
}
.medal-section:nth-of-type(3) .progress-fill {
  background: linear-gradient(90deg, #9f8ad0, #bba6e6, #8e7cc3);
}

/* 响应式布局 */
@media (max-width: 720px) {
  .medal-wall {
    padding: 20px 15px;
    border-radius: 28px;
  }
  h2 {
    font-size: 2rem;
  }
  .medal-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 15px;
  }
  .medal-icon {
    font-size: 48px;
  }
  .medal-name {
    font-size: 1.1rem;
  }
  .medal-desc {
    font-size: 0.75rem;
    padding: 4px 8px;
  }
  .section-title {
    font-size: 1.3rem;
  }
}

@media (max-width: 480px) {
  .medal-wall {
    padding: 15px 10px;
  }
  .header-section {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }
  h2 {
    font-size: 1.8rem;
  }
  .medal-grid {
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }
  .medal-item {
    padding: 15px 8px;
  }
  .medal-icon {
    font-size: 42px;
  }
  .medal-icon.unlocked::after {
    width: 22px;
    height: 22px;
    font-size: 16px;
    right: -5px;
  }
  .medal-name {
    font-size: 1rem;
  }
  .medal-desc {
    font-size: 0.7rem;
    padding: 4px 6px;
  }
}
</style>