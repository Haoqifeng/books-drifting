<template>
  <div class="return-book">
    <h2>归还书籍</h2>
    
    <!-- 步骤条 -->
    <el-steps :active="activeStep" finish-status="success" simple style="margin-top: 20px; margin-bottom: 30px">
      <el-step title="扫码验证书籍" />
      <el-step title="确认归还信息" />
      <el-step title="归还完成" />
    </el-steps>

    <!-- 步骤1：扫码验证书籍 -->
    <div v-if="activeStep === 0" class="step-content">
      <el-card class="scan-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="header-title">扫码验证书籍</span>
            <el-tag type="warning" effect="dark" size="small">请扫描书籍上的二维码</el-tag>
          </div>
        </template>

        <!-- 模式切换 -->
        <div class="mode-switch">
          <el-radio-group v-model="scanMode" size="large">
            <el-radio-button value="camera">
              <el-icon><Camera /></el-icon>
              <span>摄像头扫码</span>
            </el-radio-button>
            <el-radio-button value="upload">
              <el-icon><Picture /></el-icon>
              <span>上传图片</span>
            </el-radio-button>
          </el-radio-group>
        </div>

        <!-- 摄像头扫码模式 -->
        <div v-if="scanMode === 'camera'" class="camera-mode">
          <div class="camera-wrapper">
            <div class="camera-container">
              <video 
                ref="videoRef" 
                class="scanner-video"
                :class="{ 'hidden': !isCameraActive }"
                autoplay
                playsinline
              ></video>
              
              <!-- 扫描框 -->
              <div v-if="isCameraActive" class="scan-frame">
                <div class="scan-line"></div>
                <div class="scan-corner top-left"></div>
                <div class="scan-corner top-right"></div>
                <div class="scan-corner bottom-left"></div>
                <div class="scan-corner bottom-right"></div>
              </div>

              <!-- 摄像头占位图 -->
              <div v-if="!isCameraActive" class="camera-placeholder">
                <el-icon :size="50"><Camera /></el-icon>
                <p class="placeholder-text">摄像头未开启</p>
              </div>
            </div>

            <!-- 摄像头选择 -->
            <div class="camera-select" v-if="cameras.length > 0 && isCameraActive">
              <span class="select-label">切换摄像头：</span>
              <el-select v-model="selectedCamera" size="small" style="width: 200px" @change="switchCamera">
                <el-option
                  v-for="camera in cameras"
                  :key="camera.deviceId"
                  :label="camera.label || `摄像头 ${cameras.indexOf(camera) + 1}`"
                  :value="camera.deviceId"
                />
              </el-select>
            </div>

            <!-- 摄像头控制按钮 -->
            <div class="camera-controls">
              <el-button 
                v-if="!isCameraActive" 
                type="primary" 
                @click="startCamera"
                :icon="VideoCamera"
                :loading="cameraLoading"
                size="large"
              >
                开启摄像头
              </el-button>
              <el-button 
                v-else 
                type="danger" 
                @click="stopCamera"
                :icon="VideoPause"
                size="large"
              >
                关闭摄像头
              </el-button>
            </div>
          </div>
        </div>

        <!-- 上传图片模式 -->
        <div v-if="scanMode === 'upload'" class="upload-mode">
          <el-upload
            class="upload-area"
            drag
            action="#"
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleFileChange"
            accept="image/*"
          >
            <el-icon class="upload-icon"><Picture /></el-icon>
            <div class="upload-text">
              点击或拖拽二维码图片到此区域
            </div>
            <div class="upload-hint">
              支持 jpg、png 格式，请确保图片清晰
            </div>
          </el-upload>

          <!-- 预览区域 -->
          <div v-if="previewUrl" class="preview-area">
            <img :src="previewUrl" class="preview-image" alt="预览图" />
            <div class="preview-actions">
              <el-button type="success" @click="scanQRFromImage" :loading="imageScanning">
                {{ imageScanning ? '识别中...' : '识别二维码' }}
              </el-button>
              <el-button type="danger" @click="clearPreview" plain>
                重新上传
              </el-button>
            </div>
          </div>
        </div>

        <!-- 扫描状态提示 -->
        <div v-if="scanStatus" class="scan-status" :class="statusClass">
          <el-icon v-if="scanStatus.includes('成功')"><SuccessFilled /></el-icon>
          <el-icon v-else-if="scanStatus.includes('失败') || scanStatus.includes('错误') || scanStatus.includes('未借阅')"><WarningFilled /></el-icon>
          <el-icon v-else><Loading /></el-icon>
          <span>{{ scanStatus }}</span>
        </div>
      </el-card>
    </div>

    <!-- 步骤2：确认归还信息 -->
    <div v-if="activeStep === 1" class="step-content">
      <el-card class="return-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="header-title">确认归还信息</span>
            <el-tag type="success" effect="dark" size="small">
              书籍已识别
            </el-tag>
          </div>
        </template>

        <!-- 书籍信息卡片 -->
        <div v-if="bookInfo" class="book-info-card">
          <div class="book-cover">
            <img v-if="bookInfo.coverUrl" :src="bookInfo.coverUrl" :alt="bookInfo.title">
            <div v-else class="cover-placeholder">📚</div>
          </div>
          <div class="book-details">
            <h3 class="book-title">{{ bookInfo.title }}</h3>
            <p class="book-author">{{ bookInfo.author }}</p>
            <p class="book-isbn">ISBN: {{ bookInfo.isbn || '暂无' }}</p>
          </div>
        </div>

        <!-- 借阅信息卡片 -->
        <div v-if="borrowInfo" class="borrow-info-card">
          <h4>借阅信息</h4>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="借阅人">{{ borrowInfo.userName || borrowInfo.name }}</el-descriptions-item>
            <el-descriptions-item label="学号">{{ borrowInfo.studentId }}</el-descriptions-item>
            <el-descriptions-item label="借阅日期">{{ formatDate(borrowInfo.borrowDate) }}</el-descriptions-item>
            <el-descriptions-item label="应还日期">
              <span :class="{ 'overdue': isOverdue }">{{ formatDate(borrowInfo.dueDate) }}</span>
            </el-descriptions-item>
          </el-descriptions>
          
          <!-- 超期提醒 -->
          <div v-if="isOverdue" class="overdue-warning">
            <el-alert
              :title="`书籍已超期 ${overdueDays} 天`"
              type="error"
              :description="`请尽快归还，超期每天将扣除1积分`"
              show-icon
              :closable="false"
              effect="dark"
            />
          </div>
          
          <div v-else class="return-timely">
            <el-alert
              title="书籍在借阅期内归还"
              type="success"
              :description="`距离应还日期还有 ${daysUntilDue} 天`"
              show-icon
              :closable="false"
            />
          </div>
        </div>

        <!-- 归还确认 -->
        <div class="return-confirm">
          <el-checkbox v-model="confirmReturn" size="large">
            我已确认归还《{{ bookInfo?.title }}》
          </el-checkbox>
        </div>

        <div class="form-actions">
          <el-button @click="activeStep = 0">重新扫码</el-button>
          <el-button 
            type="success" 
            @click="confirmReturnBook"
            :disabled="!confirmReturn || returning"
            :loading="returning"
          >
            {{ returning ? '归还中...' : '确认归还' }}
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 步骤3：归还完成 -->
    <div v-if="activeStep === 2" class="step-content">
      <el-card class="success-card">
        <div class="success-content">
          <div class="success-icon">✓</div>
          <h2>归还成功！</h2>
          <p>《{{ bookInfo?.title }}》已成功归还</p>
          
          <div class="success-info">
            <p>归还时间：{{ returnTime }}</p>
            <p v-if="isOverdue" class="overdue-text">
              书籍超期 {{ overdueDays }} 天，已扣除 {{ overdueDays }} 积分
            </p>
          </div>
          
          <div class="success-actions">
            <el-button type="primary" @click="returnAnother">继续归还</el-button>
            <el-button @click="goToHome">返回首页</el-button>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Camera, 
  Picture, 
  VideoCamera, 
  VideoPause, 
  SuccessFilled,
  WarningFilled,
  Loading
} from '@element-plus/icons-vue'
import axios from 'axios'
import jsQR from 'jsqr'
import dayjs from 'dayjs'

const router = useRouter()

// 步骤状态
const activeStep = ref(0)

// 扫描模式
const scanMode = ref('camera')

// 摄像头相关
const videoRef = ref(null)
const isCameraActive = ref(false)
const cameraLoading = ref(false)
const cameras = ref([])
const selectedCamera = ref('')
const scanInterval = ref(null)

// 上传相关
const previewUrl = ref('')
const imageScanning = ref(false)

// 扫描状态
const scanStatus = ref('')

// 书籍信息
const bookInfo = ref(null)

// 借阅信息
const borrowInfo = ref(null)

// 归还确认
const confirmReturn = ref(false)
const returning = ref(false)

// 归还时间
const returnTime = ref('')

// 当前用户信息
const currentUser = ref(null)

// 计算是否超期
const isOverdue = computed(() => {
  if (!borrowInfo.value) return false
  return dayjs().isAfter(dayjs(borrowInfo.value.dueDate))
})

// 计算超期天数
const overdueDays = computed(() => {
  if (!borrowInfo.value || !isOverdue.value) return 0
  const due = dayjs(borrowInfo.value.dueDate)
  const now = dayjs()
  const days = now.diff(due, 'day')
  return days > 365 ? Math.min(days, 30) : days
})

// 计算距离应还日期天数
const daysUntilDue = computed(() => {
  if (!borrowInfo.value || isOverdue.value) return 0
  const due = dayjs(borrowInfo.value.dueDate)
  const now = dayjs()
  return due.diff(now, 'day')
})

// 状态样式
const statusClass = computed(() => {
  if (scanStatus.value.includes('成功')) return 'status-success'
  if (scanStatus.value.includes('失败') || scanStatus.value.includes('错误')) return 'status-error'
  return 'status-scanning'
})

// 格式化日期
const formatDate = (date) => {
  if (!date) return '未知日期'
  return dayjs(date).format('YYYY-MM-DD')
}

// ==================== 辅助函数 ====================
// 设置 Canvas 的 willReadFrequently 属性以提高性能
const setupCanvasForFrequentRead = (canvas) => {
  if (canvas && canvas.getContext) {
    const ctx = canvas.getContext('2d', { willReadFrequently: true })
    return ctx
  }
  return null
}

// 获取当前用户信息
const getCurrentUser = () => {
  const userInfoStr = localStorage.getItem('userInfo')
  if (userInfoStr) {
    try {
      currentUser.value = JSON.parse(userInfoStr)
      console.log('当前用户:', currentUser.value)
    } catch (e) {
      console.error('解析用户信息失败', e)
      currentUser.value = null
    }
  }
}

// ==================== 摄像头相关方法 ====================
const getCameras = async () => {
  try {
    const devices = await navigator.mediaDevices.enumerateDevices()
    cameras.value = devices.filter(device => device.kind === 'videoinput')
    
    const backCamera = cameras.value.find(camera => 
      camera.label.toLowerCase().includes('back') || 
      camera.label.toLowerCase().includes('rear') ||
      camera.label.toLowerCase().includes('后置')
    )
    
    if (backCamera) {
      selectedCamera.value = backCamera.deviceId
    } else if (cameras.value.length > 0) {
      selectedCamera.value = cameras.value[0].deviceId
    }
  } catch (error) {
    console.error('获取摄像头列表失败:', error)
  }
}

const startCamera = async () => {
  cameraLoading.value = true
  scanStatus.value = '正在启动摄像头...'
  
  try {
    await getCameras()
    
    const constraints = {
      video: selectedCamera.value ? {
        deviceId: { exact: selectedCamera.value }
      } : {
        facingMode: 'environment'
      }
    }
    
    const stream = await navigator.mediaDevices.getUserMedia({ video: constraints.video })
    
    if (videoRef.value) {
      videoRef.value.srcObject = stream
      isCameraActive.value = true
      
      videoRef.value.onloadedmetadata = () => {
        videoRef.value.play()
        scanStatus.value = '正在扫描二维码...'
        startScanning()
      }
    }
  } catch (error) {
    console.error('摄像头启动失败:', error)
    if (error.name === 'NotAllowedError') {
      scanStatus.value = '请允许访问摄像头权限'
      ElMessage.error('请允许访问摄像头权限')
    } else if (error.name === 'NotFoundError') {
      scanStatus.value = '未找到摄像头设备'
      ElMessage.error('未找到摄像头设备')
    } else {
      scanStatus.value = '摄像头启动失败'
      ElMessage.error('摄像头启动失败：' + error.message)
    }
  } finally {
    cameraLoading.value = false
  }
}

const startScanning = () => {
  if (!videoRef.value || !isCameraActive.value) return
  
  const canvas = document.createElement('canvas')
  const context = setupCanvasForFrequentRead(canvas)
  
  scanInterval.value = setInterval(() => {
    if (!videoRef.value || !isCameraActive.value) return
    
    const video = videoRef.value
    if (video.readyState === video.HAVE_ENOUGH_DATA && video.videoWidth > 0 && video.videoHeight > 0) {
      canvas.width = video.videoWidth
      canvas.height = video.videoHeight
      context.drawImage(video, 0, 0, canvas.width, canvas.height)
      
      const imageData = context.getImageData(0, 0, canvas.width, canvas.height)
      const code = jsQR(imageData.data, imageData.width, imageData.height, {
        inversionAttempts: 'dontInvert'
      })
      
      if (code) {
        handleQRResult(code.data)
      }
    }
  }, 500)
}

const switchCamera = () => {
  if (isCameraActive.value) {
    stopCamera()
    setTimeout(() => {
      startCamera()
    }, 300)
  }
}

const stopCamera = () => {
  if (scanInterval.value) {
    clearInterval(scanInterval.value)
    scanInterval.value = null
  }
  
  if (videoRef.value && videoRef.value.srcObject) {
    const tracks = videoRef.value.srcObject.getTracks()
    tracks.forEach(track => track.stop())
    videoRef.value.srcObject = null
  }
  
  isCameraActive.value = false
  scanStatus.value = ''
}

// ==================== 上传相关方法 ====================
const handleFileChange = (file) => {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
  previewUrl.value = URL.createObjectURL(file.raw)
  scanStatus.value = ''
}

const clearPreview = () => {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
}

const scanQRFromImage = async () => {
  if (!previewUrl.value) {
    ElMessage.warning('请先上传二维码图片')
    return
  }

  imageScanning.value = true
  scanStatus.value = '正在识别二维码...'

  try {
    const img = new Image()
    img.src = previewUrl.value
    
    await new Promise((resolve, reject) => {
      img.onload = resolve
      img.onerror = reject
    })
    
    const canvas = document.createElement('canvas')
    const ctx = setupCanvasForFrequentRead(canvas)
    
    canvas.width = img.width
    canvas.height = img.height
    ctx.drawImage(img, 0, 0, img.width, img.height)
    
    const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height)
    const code = jsQR(imageData.data, imageData.width, imageData.height, {
      inversionAttempts: 'dontInvert'
    })
    
    if (code) {
      handleQRResult(code.data)
    } else {
      scanStatus.value = '未识别到二维码'
      ElMessage.warning('未识别到二维码，请确保图片清晰且包含有效的二维码')
    }
  } catch (error) {
    console.error('识别失败:', error)
    scanStatus.value = '识别失败，请重试'
    ElMessage.error('二维码识别失败，请确保图片清晰')
  } finally {
    imageScanning.value = false
  }
}

// ==================== 二维码结果处理 ====================
const handleQRResult = (text) => {
  console.log('识别到书籍二维码:', text)
  
  if (navigator.vibrate) {
    navigator.vibrate(200)
  }
  
  // 解析二维码内容
  let bookTitle = null
  let bookId = null
  
  try {
    // 尝试解析JSON格式
    if (text.startsWith('{') && text.endsWith('}')) {
      const bookData = JSON.parse(text)
      bookTitle = bookData.title
      bookId = bookData.bookId || bookData.id
    } 
    // 尝试解析URL参数格式
    else if (text.includes('bookId=')) {
      const params = new URLSearchParams(text)
      bookId = params.get('bookId')
      bookTitle = params.get('title')
    } 
    // 直接作为书名
    else {
      bookTitle = text
    }
  } catch (e) {
    console.error('解析书籍二维码失败:', e)
    bookTitle = text
  }
  
  if (!bookTitle && !bookId) {
    scanStatus.value = '二维码数据无效'
    ElMessage.error('二维码数据无效，请扫描正确的书籍二维码')
    return
  }
  
  scanStatus.value = '识别成功！正在验证书籍...'
  
  // 停止摄像头扫描
  stopCamera()
  
  // 验证书籍
  verifyBook(bookTitle, bookId)
}

// ==================== 验证书籍 ====================
const verifyBook = async (bookTitle, bookId) => {
  try {
    console.log('验证书籍，书名:', bookTitle, '书籍ID:', bookId)
    
    // 获取当前用户ID
    if (!currentUser.value || !currentUser.value.id) {
      scanStatus.value = '请先登录'
      ElMessage.error('请先登录')
      return
    }
    
    const userId = currentUser.value.id
    console.log('当前用户ID:', userId)
    
    // 获取当前用户的借阅记录
    const response = await axios.get(`http://localhost:10011/api/borrow/current/user/${userId}`)
    console.log('借阅记录响应:', response.data)
    
    if (response.data.success && Array.isArray(response.data.data)) {
      const borrowRecords = response.data.data
      console.log('用户借阅记录数量:', borrowRecords.length)
      
      if (borrowRecords.length === 0) {
        scanStatus.value = '您当前没有借阅任何书籍'
        ElMessage.warning('您当前没有借阅任何书籍')
        return
      }
      
      // 在用户的借阅记录中查找匹配的书籍
      let matchedRecord = null
      
      // 方法1：通过书名匹配
      if (bookTitle) {
        matchedRecord = borrowRecords.find(record => {
          const recordTitle = (record.bookTitle || '').toLowerCase()
          const searchTitle = String(bookTitle).toLowerCase()
          return recordTitle.includes(searchTitle) || searchTitle.includes(recordTitle)
        })
      }
      
      // 方法2：通过bookId匹配
      if (!matchedRecord && bookId) {
        matchedRecord = borrowRecords.find(record => {
          const recordBookId = String(record.bookId || '')
          const searchId = String(bookId)
          return recordBookId === searchId || 
                 (searchId.startsWith('BOOK') && recordBookId === searchId.replace('BOOK', ''))
        })
      }
      
      // 方法3：如果还是没找到，显示所有借阅书籍供用户选择
      if (!matchedRecord) {
        console.log('未找到匹配的书籍，用户借阅的书籍:', borrowRecords.map(r => r.bookTitle))
        scanStatus.value = `未找到匹配的书籍，您当前借阅了：${borrowRecords.map(r => r.bookTitle).join('、')}`
        ElMessage.warning('未找到匹配的书籍，请确认二维码是否正确')
        return
      }
      
      console.log('匹配到的借阅记录:', matchedRecord)
      
      // 构建书籍信息
      bookInfo.value = {
        id: matchedRecord.bookId,
        title: matchedRecord.bookTitle,
        author: matchedRecord.bookAuthor,
        isbn: matchedRecord.isbn,
        coverUrl: matchedRecord.coverUrl
      }
      
      // 构建借阅信息
      borrowInfo.value = {
        userName: currentUser.value.name,
        studentId: currentUser.value.studentId,
        borrowDate: matchedRecord.borrowDate,
        dueDate: matchedRecord.dueDate
      }
      
      scanStatus.value = '验证成功！'
      ElMessage.success(`成功识别书籍：${bookInfo.value.title}`)
      
      setTimeout(() => {
        activeStep.value = 1
      }, 1000)
      
    } else {
      scanStatus.value = '获取借阅记录失败'
      ElMessage.error('获取借阅记录失败')
    }
    
  } catch (error) {
    console.error('验证书籍失败:', error)
    console.error('错误详情:', error.response?.data)
    
    if (error.response?.status === 404) {
      scanStatus.value = '您当前没有借阅任何书籍'
      ElMessage.warning('您当前没有借阅任何书籍')
    } else {
      scanStatus.value = '验证失败，请重试'
      ElMessage.error('验证失败：' + (error.response?.data?.message || error.message || '请重试'))
    }
  }
}

// 确认归还
const confirmReturnBook = async () => {
  if (!confirmReturn.value) return
  
  returning.value = true
  
  try {
    const response = await axios.post(`http://localhost:10011/api/borrow/return/${bookInfo.value.id}`)
    console.log('归还响应:', response.data)
    
    if (response.data.success) {
      returnTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss')
      activeStep.value = 2
      ElMessage.success('归还成功')
      
      // 触发统计信息更新
      window.dispatchEvent(new CustomEvent('stats-updated', { 
        detail: { userId: currentUser.value?.id || 1 }
      }))
      
      // 触发借阅列表更新
      window.dispatchEvent(new CustomEvent('borrowed-books-updated', { 
        detail: { 
          userId: currentUser.value?.id || 1,
          books: []
        }
      }))
    } else {
      ElMessage.error(response.data.message || '归还失败')
    }
  } catch (error) {
    console.error('归还失败:', error)
    ElMessage.error('归还失败：' + (error.response?.data?.message || '请重试'))
  } finally {
    returning.value = false
  }
}

// 继续归还
const returnAnother = () => {
  activeStep.value = 0
  bookInfo.value = null
  borrowInfo.value = null
  confirmReturn.value = false
  scanStatus.value = ''
  
  // 清空预览
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
}

// 返回首页
const goToHome = () => {
  router.push('/')
}

// 监听模式切换
watch(scanMode, (newMode) => {
  if (newMode !== 'camera' && isCameraActive.value) {
    stopCamera()
  }
})

// 监听摄像头切换
watch(selectedCamera, (newVal, oldVal) => {
  if (newVal && newVal !== oldVal && isCameraActive.value) {
    stopCamera()
    setTimeout(() => {
      startCamera()
    }, 100)
  }
})

// 组件挂载
onMounted(() => {
  getCurrentUser()
  getCameras()
})

// 组件卸载
onUnmounted(() => {
  stopCamera()
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
})
</script>

<style scoped>
.return-book {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 40px);
}

h2 {
  text-align: center;
  margin-bottom: 20px;
  color: #303133;
  font-weight: 500;
}

.step-content {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  font-size: 18px;
  font-weight: 500;
  color: #303133;
}

.mode-switch {
  display: flex;
  justify-content: center;
  margin-bottom: 30px;
}

.mode-switch .el-radio-button {
  margin: 0 10px;
}

.camera-mode {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.camera-wrapper {
  width: 100%;
  max-width: 500px;
}

.camera-container {
  width: 100%;
  height: 350px;
  background: #1a1a1a;
  border-radius: 12px;
  overflow: hidden;
  position: relative;
  border: 2px solid #409EFF;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  margin-bottom: 15px;
}

.scanner-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.scanner-video.hidden {
  display: none;
}

.camera-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #2c3e50;
  color: white;
}

.camera-placeholder .el-icon {
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 15px;
}

.placeholder-text {
  font-size: 16px;
  opacity: 0.9;
  color: #ecf0f1;
}

.scan-frame {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 250px;
  height: 250px;
  pointer-events: none;
}

.scan-corner {
  position: absolute;
  width: 30px;
  height: 30px;
  border-color: #409EFF;
  border-style: solid;
  filter: drop-shadow(0 0 5px rgba(64,158,255,0.5));
}

.scan-corner.top-left {
  top: 0;
  left: 0;
  border-width: 3px 0 0 3px;
}

.scan-corner.top-right {
  top: 0;
  right: 0;
  border-width: 3px 3px 0 0;
}

.scan-corner.bottom-left {
  bottom: 0;
  left: 0;
  border-width: 0 0 3px 3px;
}

.scan-corner.bottom-right {
  bottom: 0;
  right: 0;
  border-width: 0 3px 3px 0;
}

.scan-line {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, transparent, #409EFF, transparent);
  animation: scan 2s linear infinite;
}

@keyframes scan {
  0% { top: 0; }
  50% { top: 100%; }
  100% { top: 0; }
}

.camera-select {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
}

.select-label {
  font-size: 14px;
  color: #606266;
}

.camera-controls {
  display: flex;
  justify-content: center;
  margin-top: 10px;
}

.upload-mode {
  padding: 20px 0;
}

.upload-area {
  width: 100%;
  margin-bottom: 20px;
}

.upload-icon {
  font-size: 48px;
  color: #909399;
  margin-bottom: 10px;
}

.upload-text {
  font-size: 16px;
  color: #606266;
  margin-bottom: 5px;
}

.upload-hint {
  font-size: 12px;
  color: #909399;
}

.preview-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
}

.preview-image {
  max-width: 300px;
  max-height: 300px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  object-fit: contain;
}

.preview-actions {
  display: flex;
  gap: 10px;
}

.scan-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px;
  border-radius: 4px;
  margin-top: 20px;
  font-size: 14px;
}

.status-success {
  background-color: #f0f9eb;
  color: #67C23A;
}

.status-error {
  background-color: #fef0f0;
  color: #F56C6C;
}

.status-scanning {
  background-color: #ecf5ff;
  color: #409EFF;
}

.book-info-card {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.book-cover {
  width: 100px;
  height: 140px;
  flex-shrink: 0;
  border-radius: 4px;
  overflow: hidden;
  background-color: #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.book-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  font-size: 36px;
}

.book-details {
  flex: 1;
}

.book-title {
  margin: 0 0 10px;
  font-size: 20px;
  color: #303133;
}

.book-author {
  margin: 5px 0;
  color: #606266;
}

.book-isbn {
  margin: 5px 0;
  color: #909399;
  font-size: 14px;
}

.borrow-info-card {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.borrow-info-card h4 {
  margin: 0 0 15px;
  color: #303133;
}

.overdue {
  color: #F56C6C;
  font-weight: bold;
}

.overdue-warning {
  margin-top: 15px;
}

.return-timely {
  margin-top: 15px;
}

.return-confirm {
  margin: 20px 0;
  padding: 15px;
  background-color: #f9f9f9;
  border-radius: 4px;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 15px;
  margin-top: 30px;
}

.success-card {
  text-align: center;
}

.success-content {
  padding: 40px 20px;
}

.success-icon {
  width: 80px;
  height: 80px;
  background: #67C23A;
  color: white;
  font-size: 48px;
  line-height: 80px;
  border-radius: 50%;
  margin: 0 auto 20px;
}

.success-content h2 {
  margin: 20px 0 10px;
  color: #303133;
}

.success-content p {
  color: #606266;
  margin-bottom: 10px;
}

.success-info {
  margin: 20px 0;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.overdue-text {
  color: #F56C6C;
}

.success-actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  margin-top: 30px;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .return-book {
    padding: 10px;
  }
  
  .camera-container {
    height: 300px;
  }
  
  .scan-frame {
    width: 200px;
    height: 200px;
  }
  
  .book-info-card {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  
  .book-cover {
    width: 120px;
    height: 160px;
  }
  
  .form-actions {
    flex-direction: column;
  }
  
  .success-actions {
    flex-direction: column;
  }
  
  .success-actions .el-button {
    width: 100%;
  }
  
  .preview-image {
    max-width: 250px;
  }
  
  .preview-actions {
    flex-direction: column;
  }
}
</style>