<template>
  <div class="book-detail">
    <!-- 加载状态与错误提示 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>
    <div v-else-if="error" class="error-state">
      <div class="error-icon">❌</div>
      <p>{{ error }}</p>
      <button @click="loadBookData($route.params.id)" class="retry-btn">重试</button>
    </div>
    <template v-else>
      <div class="book-header">
        <h2>{{ book.title }}</h2>
        <router-link to="/" class="back-home-btn">
          <span class="back-icon">←</span> 返回首页
        </router-link>
      </div>
      
      <div class="book-info-card">
        <div class="book-cover-large" v-if="book.coverUrl">
          <img :src="book.coverUrl" :alt="book.title">
        </div>
        <div class="book-details-wrapper">
          <div class="book-details">
            <div class="detail-item">
              <span class="detail-label">📖 作者</span>
              <span class="detail-value">{{ book.author }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">📚 ISBN</span>
              <span class="detail-value">{{ book.isbn || '暂无' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">📝 描述</span>
              <span class="detail-value">{{ book.description || '暂无描述' }}</span>
            </div>
          </div>
          
          <!-- 书籍二维码区域 - 挪到右侧 -->
          <div class="book-qrcode-side" v-if="book.qrCodeData">
            <div class="qrcode-side-container">
              <canvas ref="bookQrCanvas" width="120" height="120" class="book-qrcode-small"></canvas>
              <p class="qrcode-side-hint">扫码借阅/归还</p>
              <button @click="downloadQRCode" class="download-qr-btn-small" title="保存二维码">
                <span class="btn-icon">⬇️</span>
              </button>
            </div>
          </div>
        </div>
      </div>
      
      <section class="trajectory-section">
        <div class="section-header">
          <h3>📊 书籍漂流轨迹</h3>
          <span class="section-subtitle">从捐赠到现在的完整历程</span>
        </div>
        <div id="trajectory-chart" ref="chartRef" class="chart-container"></div>
        <div class="trajectory-timeline">
          <div v-for="(record, index) in trajectoryRecords" :key="index" class="timeline-item">
            <div class="timeline-marker" :class="getTimelineClass(record.action)"></div>
            <div class="timeline-content">
              <span class="timeline-time">{{ record.time }}</span>
              <span class="timeline-action" :class="getActionClass(record.action)">{{ record.action }}</span>
              <span class="timeline-user">{{ record.user }}</span>
            </div>
          </div>
        </div>
      </section>
      
      <section class="notes-section">
        <div class="section-header">
          <h3>📝 读书笔记</h3>
          <div class="notes-controls">
            <el-select v-model="sortOrder" size="small" placeholder="排序" class="sort-select">
              <el-option label="最新优先" value="desc" />
              <el-option label="最早优先" value="asc" />
            </el-select>
          </div>
        </div>
        
        <div v-if="notes.length === 0" class="empty-notes">
          <p>暂无笔记，快来写下第一条笔记吧！</p>
        </div>
        
        <div v-for="note in sortedNotes" :key="note.id" class="note-card">
          <div class="note-card-header">
            <div class="note-user-info">
              <span class="note-user-avatar">{{ note.userName?.charAt(0) || '用' }}</span>
              <span class="note-user-name">{{ note.userName || note.user }}</span>
            </div>
            <span class="note-time">{{ formatDate(note.createdAt || note.time) }}</span>
          </div>
          
          <div v-if="!note.isEditing" class="note-card-content">{{ note.content }}</div>
          <div v-else class="note-edit-area">
            <textarea v-model="note.content" rows="3" class="edit-textarea" maxlength="500"></textarea>
            <div class="word-count">
              <span :class="{ 'exceed-limit': note.content.length > 500 }">
                {{ note.content.length }}/500
              </span>
            </div>
            <div class="edit-buttons">
              <button @click="saveNote(note)" class="btn-save" :disabled="note.content.length > 500">保存</button>
              <button @click="cancelEdit(note)" class="btn-cancel">取消</button>
            </div>
          </div>
          
          <div class="note-card-footer">
            <button @click="toggleLike(note)" class="like-btn" :class="{ 'liked': note.liked }">
              <span class="like-icon">{{ note.liked ? '❤️' : '🤍' }}</span>
              <span>{{ note.likes || 0 }} 点赞</span>
            </button>
            
            <div v-if="isCurrentUser(note)" class="note-actions">
              <button @click="editNote(note)" class="action-btn edit-btn">编辑</button>
              <button @click="deleteNote(note)" class="action-btn delete-btn">删除</button>
            </div>
          </div>
        </div>
        
        <div class="add-note-card">
          <h4>📝 写下你的读书笔记</h4>
          <textarea 
            v-model="newNote" 
            rows="4" 
            placeholder="分享你的读书心得、感悟或摘抄...(500字以内)" 
            maxlength="500" 
            class="add-textarea"
          ></textarea>
          <div class="word-count">
            <span :class="{ 'exceed-limit': newNote.length > 500 }">
              {{ newNote.length }}/500
            </span>
          </div>
          <button @click="submitNote" class="submit-btn" :disabled="newNote.length > 500 || !newNote.trim()">
            发布笔记
          </button>
        </div>
      </section>
    </template>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { Star, StarFilled } from '@element-plus/icons-vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import QRCode from 'qrcode'

export default {
  components: { Star, StarFilled},
  data() {
    return {
      book: null,
      trajectoryRecords: [],
      notes: [],
      loading: true,
      error: null,
      sortOrder: 'desc',
      newNote: '',
      chart: null,
      apiBaseUrl: 'http://localhost:10011/api',
      currentUser: null,
      handleResize: null,
      bookQrCanvas: null
    }
  },
  computed: {
    sortedNotes() {
      return [...this.notes].sort((a, b) => {
        const timeA = new Date(a.createdAt || a.time).getTime()
        const timeB = new Date(b.createdAt || b.time).getTime()
        return this.sortOrder === 'desc' ? timeB - timeA : timeA - timeB
      })
    }
  },
  created() {
    this.getCurrentUser()
    const bookId = this.$route.params.id
    if (bookId) this.loadBookData(bookId)
  },
  watch: {
    '$route.params.id': {
      handler(newId) {
        if (newId) this.loadBookData(newId)
      },
      immediate: true
    }
  },
  beforeDestroy() {
    if (this.chart) {
      this.chart.dispose()
      this.chart = null
    }
    if (this.handleResize) {
      window.removeEventListener('resize', this.handleResize)
    }
  },
  methods: {
    getCurrentUser() {
      const userInfoStr = localStorage.getItem('userInfo')
      if (userInfoStr) {
        try {
          this.currentUser = JSON.parse(userInfoStr)
          console.log('当前登录用户:', this.currentUser)
        } catch (e) {
          console.error('解析用户信息失败', e)
        }
      }
    },
    
    isCurrentUser(note) {
      if (!this.currentUser || !note) return false
      return note.userId === this.currentUser.id
    },
    
    formatDate(dateStr) {
      if (!dateStr) return ''
      try {
        const date = new Date(dateStr)
        return date.toLocaleDateString('zh-CN', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit'
        })
      } catch (e) {
        return dateStr
      }
    },
    
    getTimelineClass(action) {
      switch(action) {
        case '捐赠': return 'marker-donate'
        case '认领': return 'marker-borrow'
        case '归还': return 'marker-return'
        default: return 'marker-other'
      }
    },
    
    getActionClass(action) {
      switch(action) {
        case '捐赠': return 'action-donate'
        case '认领': return 'action-borrow'
        case '归还': return 'action-return'
        default: return ''
      }
    },
    
    // 生成书籍二维码
    generateBookQRCode() {
      if (!this.book || !this.book.qrCodeData) return
      
      const canvas = this.$refs.bookQrCanvas
      if (!canvas) return
      
      console.log('生成书籍二维码，数据:', this.book.qrCodeData)
      
      let qrString = this.book.qrCodeData
      if (typeof this.book.qrCodeData === 'object') {
        qrString = JSON.stringify(this.book.qrCodeData)
      }
      
      QRCode.toCanvas(canvas, qrString, {
        width: 120,
        margin: 1,
        color: {
          dark: '#000000',
          light: '#ffffff'
        },
        errorCorrectionLevel: 'H'
      }).then(() => {
        console.log('书籍二维码生成成功')
      }).catch(error => {
        console.error('书籍二维码生成失败:', error)
      })
    },
    
    // 下载二维码
    downloadQRCode() {
      const canvas = this.$refs.bookQrCanvas
      if (!canvas) {
        ElMessage.error('二维码不存在')
        return
      }
      
      const link = document.createElement('a')
      link.download = `${this.book.title}_二维码.png`
      link.href = canvas.toDataURL('image/png')
      link.click()
      
      ElMessage.success('二维码已保存')
    },
    
    async loadBookData(bookId) {
      this.loading = true
      this.error = null
      
      if (this.chart) {
        this.chart.dispose()
        this.chart = null
      }
      
      try {
        console.log('正在获取书籍详情，ID:', bookId)
        
        const response = await axios.get(`${this.apiBaseUrl}/books/${bookId}`)
        console.log('书籍详情响应:', response.data)
        
        if (response.data) {
          this.book = response.data
          
          await this.loadBookTrajectory(bookId)
          await this.loadBookNotes(bookId)
          
          this.$nextTick(() => {
            setTimeout(() => {
              this.initChart()
              this.generateBookQRCode()
            }, 100)
          })
        } else {
          this.error = '书籍不存在'
        }
      } catch (err) {
        console.error('获取书籍详情失败:', err)
        this.error = '加载失败，请重试'
        this.useMockBookData(bookId)
      } finally {
        this.loading = false
      }
    },
    
    async loadBookTrajectory(bookId) {
      try {
        const response = await axios.get(`${this.apiBaseUrl}/books/${bookId}/trajectory`)
        this.trajectoryRecords = response.data || []
      } catch (err) {
        console.log('使用模拟轨迹数据')
        this.trajectoryRecords = [
          { time: '2024-03-01', action: '捐赠', user: '张三' },
          { time: '2024-03-05', action: '认领', user: '李四' },
          { time: '2024-03-10', action: '归还', user: '李四' },
          { time: '2024-03-15', action: '认领', user: '王五' }
        ]
      }
    },
    
    async loadBookNotes(bookId) {
      try {
        const response = await axios.get(`${this.apiBaseUrl}/notes/book/${bookId}`)
        console.log('笔记响应:', response.data)
        
        if (response.data && response.data.success) {
          this.notes = response.data.data.map(note => ({
            ...note,
            isEditing: false,
            backupContent: null,
            liked: false,
            userId: note.userId
          }))
          
          if (this.currentUser) {
            for (const note of this.notes) {
              try {
                const likeResponse = await axios.get(`${this.apiBaseUrl}/notes/check-like`, {
                  params: {
                    noteId: note.id,
                    userId: this.currentUser.id
                  }
                })
                note.liked = likeResponse.data.liked || false
              } catch (e) {
                note.liked = false
              }
            }
          }
        } else {
          this.useMockNotes()
        }
      } catch (err) {
        console.log('使用模拟笔记数据')
        this.useMockNotes()
      }
    },
    
    useMockNotes() {
      const currentUserId = this.currentUser?.id || 1
      this.notes = [
        {
          id: 1,
          userId: currentUserId,
          userName: this.currentUser?.name || '当前用户',
          content: '这本书非常精彩，值得一读！',
          likes: 3,
          liked: false,
          createdAt: '2024-03-15T10:00:00Z',
          isEditing: false,
          backupContent: null
        },
        {
          id: 2,
          userId: 999,
          userName: '李四',
          content: '第三章讲得很好，受益匪浅。',
          likes: 2,
          liked: false,
          createdAt: '2024-03-10T08:30:00Z',
          isEditing: false,
          backupContent: null
        },
        {
          id: 3,
          userId: 998,
          userName: '王五',
          content: '准备考试用，内容很全面。',
          likes: 1,
          liked: false,
          createdAt: '2024-03-05T14:20:00Z',
          isEditing: false,
          backupContent: null
        }
      ]
    },
    
    useMockBookData(bookId) {
      const mockBooks = {
        1: {
          id: 1,
          title: 'JavaScript高级程序设计',
          author: 'Nicholas C. Zakas',
          isbn: '9787121028953',
          description: 'JavaScript经典教材，适合前端开发学习',
          qrCodeData: JSON.stringify({
            bookId: 'BOOK1',
            title: 'JavaScript高级程序设计',
            donor: '张三'
          })
        },
        2: {
          id: 2,
          title: 'Vue.js实战',
          author: '梁灏',
          isbn: '9787302456789',
          description: 'Vue.js框架实战教程',
          qrCodeData: JSON.stringify({
            bookId: 'BOOK2',
            title: 'Vue.js实战',
            donor: '李四'
          })
        },
        3: {
          id: 3,
          title: '算法导论',
          author: 'Thomas H. Cormen',
          isbn: '9787111407010',
          description: '算法经典教材',
          qrCodeData: JSON.stringify({
            bookId: 'BOOK3',
            title: '算法导论',
            donor: '王五'
          })
        }
      }
      
      this.book = mockBooks[bookId] || null
      if (!this.book) {
        this.error = '书籍不存在'
      } else {
        this.trajectoryRecords = [
          { time: '2024-03-01', action: '捐赠', user: '张三' },
          { time: '2024-03-05', action: '认领', user: '李四' }
        ]
        this.useMockNotes()
        
        this.$nextTick(() => {
          setTimeout(() => {
            this.initChart()
            this.generateBookQRCode()
          }, 100)
        })
      }
    },
    
    initChart() {
      if (!this.$refs.chartRef || !this.trajectoryRecords || this.trajectoryRecords.length === 0) {
        return
      }
      
      if (this.chart) {
        this.chart.dispose()
        this.chart = null
      }
      
      try {
        this.chart = echarts.init(this.$refs.chartRef)
        
        const dates = this.trajectoryRecords.map(record => record.time)
        const actions = this.trajectoryRecords.map(record => {
          switch(record.action) {
            case '捐赠': return 3
            case '认领': return 2
            case '归还': return 1
            default: return 2
          }
        })
        
        const option = {
          title: { 
            text: '书籍漂流时间轴',
            left: 'center',
            top: 0,
            textStyle: {
              fontSize: 16,
              fontWeight: 'normal',
              color: '#333'
            }
          },
          tooltip: {
            trigger: 'axis',
            formatter: (params) => {
              const index = params[0].dataIndex
              const record = this.trajectoryRecords[index]
              return `${record.time}<br/>${record.action}: ${record.user}`
            }
          },
          grid: {
            left: '10%',
            right: '5%',
            bottom: '15%',
            top: '15%',
            containLabel: true
          },
          xAxis: [{
            type: 'category',
            data: dates,
            axisLine: { lineStyle: { color: '#999', width: 1 } },
            axisTick: { show: false },
            axisLabel: {
              formatter: (value, index) => {
                const record = this.trajectoryRecords[index]
                return `${record.user}\n${value}`
              },
              interval: 0,
              rotate: 30,
              fontSize: 11
            }
          }],
          yAxis: [{
            type: 'value',
            min: 0,
            max: 4,
            interval: 1,
            axisLine: { show: false },
            axisTick: { show: false },
            axisLabel: {
              formatter: (value) => {
                switch(value) {
                  case 3: return '捐赠'
                  case 2: return '借出'
                  case 1: return '归还'
                  default: return ''
                }
              },
              fontSize: 12,
              color: '#666'
            },
            splitLine: { 
              lineStyle: { 
                type: 'dashed', 
                color: '#e0e0e0' 
              } 
            }
          }],
          series: [{
            data: actions,
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 10,
            lineStyle: { width: 3, color: '#4CAF50' },
            itemStyle: { color: '#4CAF50' },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(76, 175, 80, 0.3)' },
                { offset: 1, color: 'rgba(76, 175, 80, 0.1)' }
              ])
            },
            markPoint: {
              data: [
                { type: 'max', name: '最高' },
                { type: 'min', name: '最低' }
              ]
            }
          }]
        }
        
        this.chart.setOption(option)
        
        if (this.handleResize) {
          window.removeEventListener('resize', this.handleResize)
        }
        
        this.handleResize = () => {
          if (this.chart) {
            this.chart.resize()
          }
        }
        window.addEventListener('resize', this.handleResize)
        
      } catch (error) {
        console.error('图表初始化失败:', error)
      }
    },
    
    async toggleLike(targetNote) {
      const note = this.notes.find(n => n.id === targetNote.id)
      if (!note) return
      
      try {
        const userId = this.getCurrentUserId()
        const response = await axios.post(`${this.apiBaseUrl}/notes/like`, {
          noteId: note.id,
          userId: userId
        })
        
        if (response.data.success) {
          note.liked = response.data.liked
          note.likes += note.liked ? 1 : -1
        }
      } catch (err) {
        console.error('点赞失败:', err)
        note.liked = !note.liked
        note.likes += note.liked ? 1 : -1
      }
    },
    
    editNote(targetNote) {
      const note = this.notes.find(n => n.id === targetNote.id)
      if (note) {
        note.isEditing = true
        note.backupContent = note.content
      }
    },
    
    async saveNote(targetNote) {
      const note = this.notes.find(n => n.id === targetNote.id)
      if (!note || !note.content.trim()) {
        ElMessage.warning('笔记内容不能为空')
        return
      }
      
      try {
        const userId = this.getCurrentUserId()
        const response = await axios.put(`${this.apiBaseUrl}/notes/${note.id}`, {
          content: note.content,
          userId: userId
        })
        
        if (response.data.success) {
          note.isEditing = false
          delete note.backupContent
          ElMessage.success('保存成功')
        }
      } catch (err) {
        console.error('保存失败:', err)
        ElMessage.error('保存失败，请重试')
        note.isEditing = false
        delete note.backupContent
      }
    },
    
    cancelEdit(targetNote) {
      const note = this.notes.find(n => n.id === targetNote.id)
      if (note) {
        note.content = note.backupContent || note.content
        note.isEditing = false
        delete note.backupContent
      }
    },
    
    async deleteNote(targetNote) {
      if (confirm('确定删除吗？')) {
        try {
          const userId = this.getCurrentUserId()
          const response = await axios.delete(`${this.apiBaseUrl}/notes/${targetNote.id}`, {
            params: { userId }
          })
          
          if (response.data.success) {
            this.notes = this.notes.filter(n => n.id !== targetNote.id)
            ElMessage.success('删除成功')
          }
        } catch (err) {
          console.error('删除失败:', err)
          ElMessage.error('删除失败，请重试')
          this.notes = this.notes.filter(n => n.id !== targetNote.id)
        }
      }
    },
    
    async submitNote() {
      if (!this.newNote.trim() || this.newNote.length > 500) return
      
      try {
        const userId = this.getCurrentUserId()
        const userName = this.getCurrentUserName()
        
        const response = await axios.post(`${this.apiBaseUrl}/notes`, {
          bookId: this.book.id,
          userId: userId,
          userName: userName,
          content: this.newNote
        })
        
        if (response.data.success) {
          const newNote = response.data.data
          this.notes.push({
            id: newNote.id,
            userId: userId,
            userName: userName,
            content: this.newNote,
            likes: 0,
            liked: false,
            createdAt: new Date().toISOString(),
            isEditing: false,
            backupContent: null
          })
          this.newNote = ''
          
          window.dispatchEvent(new CustomEvent('stats-updated', { 
            detail: { userId: userId }
          }))
          
          ElMessage.success('笔记发布成功')
        }
      } catch (err) {
        console.error('提交失败:', err)
        ElMessage.error('提交失败，请重试')
      }
    },
    
    getCurrentUserId() {
      if (this.currentUser) {
        return this.currentUser.id
      }
      const userInfo = localStorage.getItem('userInfo')
      if (userInfo) {
        return JSON.parse(userInfo).id
      }
      return 1
    },
    
    getCurrentUserName() {
      if (this.currentUser) {
        return this.currentUser.name
      }
      const userInfo = localStorage.getItem('userInfo')
      if (userInfo) {
        return JSON.parse(userInfo).name
      }
      return '当前用户'
    }
  }
}
</script>

<style scoped>
.book-detail {
  max-width: 1000px;
  margin: 0 auto;
  padding: 30px 20px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  min-height: calc(100vh - 60px);
}

/* 头部样式 */
.book-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  background: white;
  padding: 20px 25px;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.book-header h2 {
  font-size: 2rem;
  color: #2c3e50;
  margin: 0;
  font-weight: 600;
  background: linear-gradient(135deg, #2c3e50, #4CAF50);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.back-home-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #4CAF50;
  text-decoration: none;
  font-size: 1rem;
  padding: 8px 16px;
  border-radius: 30px;
  background: rgba(76, 175, 80, 0.1);
  transition: all 0.3s ease;
}

.back-home-btn:hover {
  background: #4CAF50;
  color: white;
  transform: translateX(-2px);
}

.back-icon {
  font-size: 1.2rem;
}

/* 书籍信息卡片 */
.book-info-card {
  background: white;
  border-radius: 20px;
  padding: 30px;
  margin-bottom: 30px;
  display: flex;
  gap: 30px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.5);
}

.book-cover-large {
  flex-shrink: 0;
  width: 200px;
  height: 280px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
}

.book-cover-large img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.book-cover-large:hover img {
  transform: scale(1.05);
}

.book-details-wrapper {
  flex: 1;
  display: flex;
  gap: 30px;
  align-items: flex-start;
}

.book-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-item {
  display: flex;
  align-items: flex-start;
  gap: 15px;
  padding: 12px 15px;
  background: #f8f9fa;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.detail-item:hover {
  transform: translateX(5px);
  background: #e9ecef;
}

.detail-label {
  font-size: 1rem;
  color: #4CAF50;
  font-weight: 600;
  min-width: 60px;
}

.detail-value {
  font-size: 1rem;
  color: #2c3e50;
  line-height: 1.5;
}

/* 侧边二维码样式 */
.book-qrcode-side {
  flex-shrink: 0;
  width: 140px;
  background: linear-gradient(135deg, #f8f9fa, #ffffff);
  border-radius: 16px;
  padding: 15px 10px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(76, 175, 80, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.qrcode-side-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  text-align: center;
}

.book-qrcode-small {
  width: 120px;
  height: 120px;
  padding: 8px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
  border: 2px solid #f0f0f0;
  transition: transform 0.3s ease;
}

.book-qrcode-small:hover {
  transform: scale(1.05);
}

.qrcode-side-hint {
  color: #666;
  font-size: 0.8rem;
  margin: 2px 0;
  font-weight: 500;
}

.download-qr-btn-small {
  background: #4CAF50;
  color: white;
  border: none;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(76, 175, 80, 0.3);
  font-size: 1.2rem;
}

.download-qr-btn-small:hover {
  background: #45a049;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.4);
}

/* 章节通用样式 */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 0 10px;
}

.section-header h3 {
  font-size: 1.5rem;
  color: #2c3e50;
  margin: 0;
  font-weight: 600;
}

.section-subtitle {
  color: #999;
  font-size: 0.9rem;
}

/* 轨迹区域 */
.trajectory-section {
  background: white;
  border-radius: 20px;
  padding: 25px;
  margin-bottom: 30px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
}

.chart-container {
  width: 100%;
  height: 350px;
  margin: 20px 0;
}

.trajectory-timeline {
  margin-top: 30px;
  padding: 0 20px;
}

.timeline-item {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 15px;
  position: relative;
  padding-left: 20px;
}

.timeline-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: -15px;
  width: 2px;
  background: linear-gradient(to bottom, #4CAF50, #8BC34A);
}

.timeline-item:last-child::before {
  display: none;
}

.timeline-marker {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  z-index: 1;
}

.marker-donate {
  background: linear-gradient(135deg, #FF6B6B, #FF8E8E);
  box-shadow: 0 0 0 4px rgba(255, 107, 107, 0.2);
}

.marker-borrow {
  background: linear-gradient(135deg, #4CAF50, #8BC34A);
  box-shadow: 0 0 0 4px rgba(76, 175, 80, 0.2);
}

.marker-return {
  background: linear-gradient(135deg, #2196F3, #64B5F6);
  box-shadow: 0 0 0 4px rgba(33, 150, 243, 0.2);
}

.timeline-content {
  display: flex;
  gap: 20px;
  align-items: center;
  flex-wrap: wrap;
}

.timeline-time {
  font-weight: 600;
  color: #2c3e50;
  min-width: 100px;
}

.timeline-action {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 0.9rem;
  font-weight: 500;
}

.action-donate {
  background: rgba(255, 107, 107, 0.1);
  color: #FF6B6B;
}

.action-borrow {
  background: rgba(76, 175, 80, 0.1);
  color: #4CAF50;
}

.action-return {
  background: rgba(33, 150, 243, 0.1);
  color: #2196F3;
}

.timeline-user {
  color: #666;
}

/* 笔记区域 */
.notes-section {
  background: white;
  border-radius: 20px;
  padding: 25px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
}

.notes-controls {
  display: flex;
  gap: 10px;
}

.sort-select {
  width: 120px;
}

.empty-notes {
  text-align: center;
  padding: 60px;
  color: #999;
  background: #f8f9fa;
  border-radius: 16px;
  margin: 20px 0;
}

/* 笔记卡片 */
.note-card {
  background: #f8f9fa;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 20px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  transition: all 0.3s ease;
}

.note-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

.note-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.note-user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.note-user-avatar {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #4CAF50, #8BC34A);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 1rem;
}

.note-user-name {
  font-weight: 600;
  color: #2c3e50;
}

.note-time {
  color: #999;
  font-size: 0.9rem;
}

.note-card-content {
  font-size: 1rem;
  line-height: 1.6;
  color: #333;
  margin-bottom: 15px;
  padding: 10px;
  background: white;
  border-radius: 12px;
}

.note-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.like-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border: none;
  background: white;
  border-radius: 30px;
  cursor: pointer;
  transition: all 0.3s ease;
  color: #666;
}

.like-btn:hover {
  background: #f0f0f0;
}

.like-btn.liked {
  color: #FF6B6B;
}

.like-icon {
  font-size: 1.2rem;
}

.note-actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.3s ease;
}

.edit-btn {
  background: rgba(33, 150, 243, 0.1);
  color: #2196F3;
}

.edit-btn:hover {
  background: #2196F3;
  color: white;
}

.delete-btn {
  background: rgba(244, 67, 54, 0.1);
  color: #f44336;
}

.delete-btn:hover {
  background: #f44336;
  color: white;
}

/* 笔记编辑区域 */
.note-edit-area {
  margin: 10px 0;
}

.edit-textarea {
  width: 100%;
  padding: 12px;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  font-size: 1rem;
  resize: vertical;
  transition: border-color 0.3s;
}

.edit-textarea:focus {
  outline: none;
  border-color: #4CAF50;
}

.edit-buttons {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 10px;
}

.btn-save, .btn-cancel {
  padding: 8px 20px;
  border: none;
  border-radius: 30px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.3s ease;
}

.btn-save {
  background: #4CAF50;
  color: white;
}

.btn-save:hover:not(:disabled) {
  background: #45a049;
  transform: translateY(-2px);
}

.btn-save:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.btn-cancel {
  background: #e0e0e0;
  color: #666;
}

.btn-cancel:hover {
  background: #d0d0d0;
}

/* 添加笔记卡片 */
.add-note-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  padding: 25px;
  margin-top: 30px;
  color: white;
}

.add-note-card h4 {
  margin: 0 0 15px;
  font-size: 1.3rem;
}

.add-textarea {
  width: 100%;
  padding: 15px;
  border: none;
  border-radius: 12px;
  font-size: 1rem;
  resize: vertical;
  margin-bottom: 10px;
}

.add-textarea:focus {
  outline: none;
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.3);
}

.submit-btn {
  width: 100%;
  padding: 12px;
  background: white;
  color: #4CAF50;
  border: none;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.2);
}

.submit-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

/* 加载状态 */
.loading-state {
  text-align: center;
  padding: 100px 20px;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  margin: 0 auto 20px;
  border: 4px solid #f0f0f0;
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
  padding: 100px 20px;
}

.error-icon {
  font-size: 48px;
  margin-bottom: 20px;
}

.retry-btn {
  padding: 10px 30px;
  background: #4CAF50;
  color: white;
  border: none;
  border-radius: 30px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s ease;
}

.retry-btn:hover {
  background: #45a049;
  transform: translateY(-2px);
}

/* 字数统计 */
.word-count {
  text-align: right;
  font-size: 0.85rem;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 10px;
}

.exceed-limit {
  color: #ff6b6b;
  font-weight: bold;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .book-info-card {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  
  .book-cover-large {
    width: 160px;
    height: 220px;
  }
  
  .book-details-wrapper {
    flex-direction: column;
    align-items: center;
    width: 100%;
  }
  
  .book-details {
    width: 100%;
  }
  
  .detail-item {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  
  .timeline-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 5px;
  }
  
  .book-header {
    flex-direction: column;
    gap: 15px;
    text-align: center;
  }
  
  .book-header h2 {
    font-size: 1.5rem;
  }
  
  .book-qrcode-side {
    width: 100%;
    max-width: 200px;
    margin-top: 15px;
  }
}

@media (max-width: 480px) {
  .book-detail {
    padding: 15px;
  }
  
  .note-card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .note-card-footer {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }
  
  .note-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>