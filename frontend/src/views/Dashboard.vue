<template>
  <div class="dashboard">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <h1 class="logo">MarkWeave</h1>
      </div>
      
      <div class="sidebar-actions">
        <button class="btn-new" @click="handleCreate">
          <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 5v14M5 12h14"/>
          </svg>
          新建
        </button>
        <button class="btn-upload" @click="handleUpload">
          <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="17 8 12 3 7 8"/>
            <line x1="12" y1="3" x2="12" y2="15"/>
          </svg>
          上传
        </button>
      </div>
      
      <nav class="nav-menu">
        <a
          v-for="item in navItems"
          :key="item.id"
          :class="['nav-item', { active: activeNav === item.id }]"
          @click="handleNavClick(item.id)"
        >
          <component :is="item.icon" class="nav-icon" />
          {{ item.label }}
        </a>
      </nav>
      
      <div class="storage-info">
        <div class="storage-label">
          <span>存储空间</span>
          <span class="storage-value">{{ formatSize(usedStorage) }} / {{ formatSize(totalStorage) }}</span>
        </div>
        <el-progress 
          :percentage="storagePercentage" 
          :stroke-width="6"
          :show-text="false"
          color="#3b82f6"
        />
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="main-content">
      <!-- 顶部栏 -->
      <header class="header">
        <div class="search-wrapper">
          <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <path d="m21 21-4.35-4.35"/>
          </svg>
          <input 
            v-model="searchQuery"
            type="text" 
            class="search-input" 
            placeholder="搜索文档..."
            @keydown.ctrl.f="focusSearch"
          />
          <span class="shortcut-hint">Ctrl+F</span>
        </div>
        
        <div class="header-actions">
          <button class="icon-btn" @click="handleNotifications">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
              <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
            </svg>
            <span class="notification-badge">3</span>
          </button>
          
          <el-dropdown trigger="click" @command="handleUserCommand">
            <div class="user-avatar">
              <img :src="userAvatar" alt="用户头像" />
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="settings">设置</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 内容区域 -->
      <div class="content-area">
        <FileManager ref="fileManager" :current-folder-id="currentFolderId" :trash-mode="trashMode" @refresh="refreshCurrent" />

        <div v-if="breadcrumb.length > 0" class="breadcrumb">
          <span
            v-if="activeNav === 'cloud'"
            class="breadcrumb-item"
            @click="handleBackToCloud"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
              <path d="M18 10h-1.26A8 8 0 1 0 9 20h9a5 5 0 0 0 0-10z"/>
            </svg>
            我的云盘
          </span>
          <span
            v-else-if="activeNav === 'share'"
            class="breadcrumb-item"
            @click="handleBackToShare"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
              <circle cx="9" cy="7" r="4"/>
            </svg>
            我的共享
          </span>
          <template v-for="(item, index) in breadcrumb" :key="item.id">
            <span v-if="index > 0 || (activeNav !== 'cloud' && activeNav !== 'share')" class="breadcrumb-separator">/</span>
            <span
              :class="['breadcrumb-item', { active: index === breadcrumb.length - 1 }]"
              @click="handleBreadcrumbClick(item.id)"
            >{{ item.name }}</span>
          </template>
        </div>

        <div class="content-header">
          <div class="tabs">
            <button 
              v-for="tab in tabs" 
              :key="tab.id"
              :class="['tab-btn', { active: activeTab === tab.id }]"
              @click="activeTab = tab.id"
            >
              {{ tab.label }}
            </button>
          </div>
          
          <div class="filter-section">
            <el-dropdown trigger="click" @command="handleFilterCommand">
              <button class="filter-btn">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/>
                </svg>
                筛选
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="all">全部时间</el-dropdown-item>
                  <el-dropdown-item command="week">近七天</el-dropdown-item>
                  <el-dropdown-item command="month">近一个月</el-dropdown-item>
                  <el-dropdown-item divided command="owner">按所有者筛选</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <!-- 文档表格 -->
        <div class="table-wrapper">
          <el-table 
            :data="filteredDocuments" 
            style="width: 100%"
            :row-class-name="tableRowClassName"
            @row-click="handleRowClick"
            highlight-current-row
          >
            <el-table-column prop="name" label="名称" min-width="250">
              <template #default="{ row }">
                <div class="doc-name-cell">
                  <component :is="getFileIcon(row.type)" class="doc-icon" />
                  <span class="doc-name">{{ row.name || row.docName || '-' }}</span>
                </div>
              </template>
            </el-table-column>
            
            <el-table-column prop="owner" label="所有者" width="150">
              <template #default="{ row }">
                <div class="owner-cell">
                  <span class="owner-avatar">{{ (row.owner || row.ownerName || 'U').charAt(0) }}</span>
                  <span>{{ row.owner || row.ownerName || '-' }}</span>
                </div>
              </template>
            </el-table-column>
            
            <el-table-column prop="owner" label="所有者" width="150">
              <template #default="{ row }">
                {{ formatDate(row.lastViewed || row.updateTime) }}
              </template>
            </el-table-column>
            
            <el-table-column prop="size" label="文档大小" width="120">
              <template #default="{ row }">
                {{ formatFileSize(row.size) }}
              </template>
            </el-table-column>

            <el-table-column v-if="!trashMode" label="操作" width="150" align="center">
              <template #default="{ row }">
                <div class="action-btns">
                  <button class="action-btn" @click.stop="handleDownload(row)" title="下载">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                      <polyline points="7 10 12 15 17 10"/>
                      <line x1="12" y1="15" x2="12" y2="3"/>
                    </svg>
                  </button>
                  <el-dropdown trigger="click" @command="(command) => handleActionCommand(command, row)">
                    <button class="action-btn" @click.stop>
                      <svg viewBox="0 0 24 24" fill="currentColor">
                        <circle cx="12" cy="6" r="2"/>
                        <circle cx="12" cy="12" r="2"/>
                        <circle cx="12" cy="18" r="2"/>
                      </svg>
                    </button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="rename" :disabled="row.type == 1">重命名</el-dropdown-item>
                        <el-dropdown-item command="move" :disabled="row.type == 1">移动</el-dropdown-item>
                        <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </template>
            </el-table-column>

            <el-table-column v-else label="操作" width="120" align="center">
              <template #default="{ row }">
                <div class="action-btns">
                  <button class="action-btn" @click.stop="handleRestore(row)" title="恢复">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/>
                      <path d="M3 3v5h5"/>
                    </svg>
                  </button>
                  <button class="action-btn delete-btn" @click.stop="handlePermanentDelete(row)" title="永久删除">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="3 6 5 6 21 6"/>
                      <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                    </svg>
                  </button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </main>
  </div>
</template>

<script>
import { ref, computed, onMounted, h, defineComponent } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { fileSystemService, authService } from '@/services';
import FileManager from '@/components/FileManager.vue';

const folderIcon = defineComponent({
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: '#f59e0b', 'stroke-width': '2' }, [
      h('path', { d: 'M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z' })
    ]);
  }
});

const markdownIcon = defineComponent({
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: '#3b82f6', 'stroke-width': '2' }, [
      h('path', { d: 'M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z' }),
      h('polyline', { points: '14 2 14 8 20 8' }),
      h('line', { x1: '16', y1: '13', x2: '8', y2: '13' }),
      h('line', { x1: '16', y1: '17', x2: '8', y2: '17' }),
      h('polyline', { points: '10 9 9 9 8 9' })
    ]);
  }
});

const pdfIcon = defineComponent({
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: '#ef4444', 'stroke-width': '2' }, [
      h('path', { d: 'M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z' }),
      h('polyline', { points: '14 2 14 8 20 8' }),
      h('path', { d: 'M10 12h4' })
    ]);
  }
});

const wordIcon = defineComponent({
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: '#3b82f6', 'stroke-width': '2' }, [
      h('path', { d: 'M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z' }),
      h('polyline', { points: '14 2 14 8 20 8' }),
      h('line', { x1: '16', y1: '13', x2: '8', y2: '13' }),
      h('line', { x1: '16', y1: '17', x2: '8', y2: '17' })
    ]);
  }
});

const excelIcon = defineComponent({
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: '#10b981', 'stroke-width': '2' }, [
      h('rect', { x: '3', y: '3', width: '18', height: '18', rx: '2' }),
      h('line', { x1: '3', y1: '9', x2: '21', y2: '9' }),
      h('line', { x1: '3', y1: '15', x2: '21', y2: '15' }),
      h('line', { x1: '9', y1: '3', x2: '9', y2: '21' }),
      h('line', { x1: '15', y1: '3', x2: '15', y2: '21' })
    ]);
  }
});

const pptIcon = defineComponent({
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: '#f59e0b', 'stroke-width': '2' }, [
      h('path', { d: 'M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z' }),
      h('polyline', { points: '14 2 14 8 20 8' }),
      h('path', { d: 'M8 13h8' }),
      h('path', { d: 'M8 17h8' })
    ]);
  }
});

const codeIcon = defineComponent({
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: '#8b5cf6', 'stroke-width': '2' }, [
      h('polyline', { points: '16 18 22 12 16 6' }),
      h('polyline', { points: '8 6 2 12 8 18' })
    ]);
  }
});

const fileIcons = {
  1: folderIcon,
  2: markdownIcon,
  3: pdfIcon,
  4: wordIcon,
  5: excelIcon,
  6: pptIcon,
  7: codeIcon
};

export default {
  name: 'Dashboard',
  components: {
    FileManager
  },
  setup() {
    const router = useRouter();
    const searchQuery = ref('');
    const activeNav = ref('home');
    const activeTab = ref('recent');
    const usedStorage = ref(30054200);
    const totalStorage = 1073741824;
    const documents = ref([]);
    const loading = ref(false);
    const currentFolderId = ref(null);
    const fileManager = ref(null);
    const breadcrumb = ref([]);
    const isInCloudDrive = ref(false);
    const cloudDriveNodeId = ref(null);
    const shareDriveNodeId = ref(null);
    const trashMode = ref(false);

    const getUserSpaceNodeId = () => {
      const user = authService.getUser();
      return user?.userSpaceNodeId || null;
    };

    const getUserShareSpaceNodeId = () => {
      const user = authService.getUser();
      return user?.userShareSpaceNodeId || null;
    };

    const findMyCloudFolder = async () => {
      try {
        const response = await fileSystemService.listFiles(0);
        if (response.code === 200 && response.data) {
          const cloudFolder = response.data.find(node =>
            node.name === '我的云盘' && node.type == 1
          );
          if (cloudFolder) {
            return cloudFolder.id;
          }
        }
      } catch (error) {
        console.error('查找云盘文件夹失败:', error);
      }
      return null;
    };

    const findMyShareFolder = async () => {
      try {
        const response = await fileSystemService.listFiles(0);
        if (response.code === 200 && response.data) {
          const shareFolder = response.data.find(node =>
            node.name === '我的共享' && node.type == 1
          );
          if (shareFolder) {
            return shareFolder.id;
          }
        }
      } catch (error) {
        console.error('查找共享文件夹失败:', error);
      }
      return null;
    };

    const handleNavClick = async (navId) => {
      activeNav.value = navId;
      trashMode.value = false;

      if (navId === 'home') {
        isInCloudDrive.value = false;
        currentFolderId.value = 0;
        loadRecentDocs();
      } else if (navId === 'cloud') {
        isInCloudDrive.value = true;
        let spaceNodeId = getUserSpaceNodeId();
        if (!spaceNodeId) {
          spaceNodeId = await findMyCloudFolder();
        }

        if (!spaceNodeId) {
          ElMessage.error('未找到云盘文件夹');
          return;
        }

        cloudDriveNodeId.value = spaceNodeId;
        currentFolderId.value = spaceNodeId;
        loadFiles();
      } else if (navId === 'share') {
        isInCloudDrive.value = true;
        let spaceNodeId = getUserShareSpaceNodeId();
        if (!spaceNodeId) {
          spaceNodeId = await findMyShareFolder();
        }

        if (!spaceNodeId) {
          ElMessage.error('未找到共享文件夹');
          return;
        }

        shareDriveNodeId.value = spaceNodeId;
        currentFolderId.value = spaceNodeId;
        loadFiles();
      } else if (navId === 'trash') {
        isInCloudDrive.value = false;
        trashMode.value = true;
        loadTrashFiles();
      }
    };

    const loadTrashFiles = async () => {
      loading.value = true;
      try {
        const response = await fileSystemService.getRecycledFiles();
        if (response.code === 200) {
          documents.value = response.data || [];
        } else {
          ElMessage.error(response.message || '加载回收站失败');
        }
      } catch (error) {
        ElMessage.error('加载回收站失败: ' + (error.response?.data?.message || error.message));
      } finally {
        loading.value = false;
      }
    };

    const navItems = [
      { id: 'home', label: '首页', icon: defineComponent({
        render() {
          return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
            h('path', { d: 'M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z' }),
            h('polyline', { points: '9 22 9 12 15 12 15 22' })
          ]);
        }
      }) },
      { id: 'cloud', label: '我的云盘', icon: defineComponent({
        render() {
          return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
            h('path', { d: 'M18 10h-1.26A8 8 0 1 0 9 20h9a5 5 0 0 0 0-10z' })
          ]);
        }
      }) },
      { id: 'share', label: '我的共享', icon: defineComponent({
        render() {
          return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
            h('path', { d: 'M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2' }),
            h('circle', { cx: '9', cy: '7', r: '4' }),
            h('path', { d: 'M23 21v-2a4 4 0 0 0-3-3.87' }),
            h('path', { d: 'M16 3.13a4 4 0 0 1 0 7.75' })
          ]);
        }
      }) },
      { id: 'trash', label: '回收站', icon: defineComponent({
        render() {
          return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
            h('polyline', { points: '3 6 5 6 21 6' }),
            h('path', { d: 'M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2' })
          ]);
        }
      }) }
    ];
    
    const tabs = [
      { id: 'recent', label: '最近' },
      { id: 'all', label: '全部' }
    ];
    
    const storagePercentage = computed(() => Math.round((usedStorage.value / totalStorage) * 100));
    
    const filteredDocuments = computed(() => {
      let docs = [...documents.value];
      
      if (searchQuery.value) {
        const query = searchQuery.value.toLowerCase();
        docs = docs.filter(doc => 
          doc.name.toLowerCase().includes(query) ||
          (doc.ownerName && doc.ownerName.toLowerCase().includes(query))
        );
      }
      
      return docs.sort((a, b) => new Date(b.updateTime) - new Date(a.updateTime));
    });
    
    const userAvatar = computed(() => {
      const user = authService.getUser();
      const initial = user?.nickname?.charAt(0) || user?.account?.charAt(0) || 'U';
      return `data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect fill="%23374151" width="100" height="100" rx="50"/><text x="50" y="65" font-size="40" fill="white" text-anchor="middle" font-family="system-ui">${initial}</text></svg>`;
    });
    
    const getFileIcon = (type) => {
      return fileIcons[type] || markdownIcon;
    };
    
    const formatFileSize = (bytes) => {
      if (!bytes) return '-';
      if (bytes < 1024) return bytes + ' B';
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
      return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
    };
    
    const formatSize = (bytes) => {
      if (!bytes) return '0 B';
      if (bytes < 1024) return bytes + ' B';
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
      if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
      return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
    };
    
    const formatDate = (date) => {
      if (!date) return '-';
      const d = new Date(date);
      const now = new Date();
      const diff = now - d;
      const days = Math.floor(diff / (1000 * 60 * 60 * 24));
      
      if (days === 0) return '今天 ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
      if (days === 1) return '昨天 ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
      if (days < 7) return days + '天前';
      
      return d.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' });
    };
    
    const focusSearch = (e) => {
      e.preventDefault();
      document.querySelector('.search-input')?.focus();
    };
    
    const handleCreate = () => {
      fileManager.value?.showCreateDialog();
    };
    
    const handleUpload = () => {
      ElMessage.info('上传文件功能开发中');
    };
    
    const handleNotifications = () => {
      ElMessage.info('通知中心开发中');
    };
    
    const handleUserCommand = (command) => {
      switch (command) {
        case 'profile':
          ElMessage.info('个人中心开发中');
          break;
        case 'settings':
          ElMessage.info('设置开发中');
          break;
        case 'logout':
          handleLogout();
          break;
      }
    };
    
    const handleFilterCommand = (command) => {
      ElMessage.info('筛选: ' + command);
    };
    
    const handleRowClick = async (row) => {
      try {
        if (trashMode.value) {
          return;
        }

        await fileSystemService.updateViewTime(row.id);

        if (row.type == 1) {
          currentFolderId.value = row.id;
          loadFiles();
        } else if (activeNav.value === 'home' || activeNav.value === 'cloud' || activeNav.value === 'share') {
          router.push(`/editor/${row.id}`);
        }
      } catch (error) {
        ElMessage.error('操作失败: ' + (error.response?.data?.message || error.message));
      }
    };
    
    const handleDownload = (row) => {
      ElMessage.success('下载: ' + row.name);
    };

    const handleRestore = async (row) => {
      try {
        const response = await fileSystemService.restoreFile(row.id);
        if (response.code === 200) {
          ElMessage.success('恢复成功');
          loadTrashFiles();
        } else {
          ElMessage.error(response.message || '恢复失败');
        }
      } catch (error) {
        ElMessage.error('恢复失败: ' + (error.response?.data?.message || error.message));
      }
    };

    const handlePermanentDelete = async (row) => {
      try {
        await ElMessageBox.confirm('确定要永久删除该文件吗？此操作不可恢复！', '确认', {
          confirmButtonText: '永久删除',
          cancelButtonText: '取消',
          type: 'warning'
        });

        const response = await fileSystemService.permanentlyDelete(row.id);
        if (response.code === 200) {
          ElMessage.success('已永久删除');
          loadTrashFiles();
        } else {
          ElMessage.error(response.message || '删除失败');
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败: ' + (error.response?.data?.message || error.message));
        }
      }
    };

    const refreshCurrent = () => {
      if (trashMode.value) {
        loadTrashFiles();
      } else if (activeNav.value === 'cloud') {
        loadFiles();
      } else {
        loadRecentDocs();
      }
    };
    
    const handleActionCommand = (command, row) => {
      switch (command) {
        case 'rename':
          fileManager.value?.showRenameDialog(row);
          break;
        case 'move':
          fileManager.value?.showMoveDialog(row);
          break;
        case 'delete':
          fileManager.value?.handleDelete(row);
          break;
      }
    };
    
    const handleMore = (row) => {
      ElMessage.info('更多操作: ' + row.name);
    };
    
    const tableRowClassName = ({ row }) => {
      return '';
    };
    
    const loadFiles = async () => {
      loading.value = true;
      try {
        const response = await fileSystemService.listFiles(currentFolderId.value);
        if (response.code === 200) {
          documents.value = response.data || [];
          loadBreadcrumb();
        } else {
          ElMessage.error(response.message || '加载失败');
        }
      } catch (error) {
        ElMessage.error('加载失败: ' + (error.response?.data?.message || error.message));
      } finally {
        loading.value = false;
      }
    };

    const loadBreadcrumb = () => {
      if (activeNav.value === 'cloud' && cloudDriveNodeId.value) {
        if (String(currentFolderId.value) !== String(cloudDriveNodeId.value)) {
          breadcrumb.value = [
            { id: cloudDriveNodeId.value, name: '我的云盘' },
            { id: currentFolderId.value, name: documents.value.find(d => String(d.id) === String(currentFolderId.value))?.name || '当前文件夹' }
          ];
        } else {
          breadcrumb.value = [
            { id: cloudDriveNodeId.value, name: '我的云盘' }
          ];
        }
      } else if (activeNav.value === 'share' && shareDriveNodeId.value) {
        if (String(currentFolderId.value) !== String(shareDriveNodeId.value)) {
          breadcrumb.value = [
            { id: shareDriveNodeId.value, name: '我的共享' },
            { id: currentFolderId.value, name: documents.value.find(d => String(d.id) === String(currentFolderId.value))?.name || '当前文件夹' }
          ];
        } else {
          breadcrumb.value = [
            { id: shareDriveNodeId.value, name: '我的共享' }
          ];
        }
      } else {
        breadcrumb.value = [];
      }
    };

    const handleBreadcrumbClick = (folderId) => {
      currentFolderId.value = folderId;
      loadFiles();
      loadBreadcrumb();
    };

    const handleBackToCloud = () => {
      activeNav.value = 'cloud';
      currentFolderId.value = cloudDriveNodeId.value;
      loadFiles();
      loadBreadcrumb();
    };

    const handleBackToShare = () => {
      activeNav.value = 'share';
      currentFolderId.value = shareDriveNodeId.value;
      loadFiles();
      loadBreadcrumb();
    };
    
    const loadRecentDocs = async () => {
      loading.value = true;
      try {
        const response = await fileSystemService.getRecentDocs();
        if (response.code === 200) {
          documents.value = (response.data || []).map(doc => ({
            id: doc.docId,
            name: doc.docName,
            owner: doc.ownerName || '-',
            ownerName: doc.ownerName,
            type: 2,
            size: doc.size || 0,
            lastViewed: doc.lastViewTime,
            updateTime: doc.lastViewTime
          }));
        } else {
          ElMessage.error(response.message || '加载失败');
        }
      } catch (error) {
        ElMessage.error('加载失败: ' + (error.response?.data?.message || error.message));
      } finally {
        loading.value = false;
      }
    };
    
    const handleLogout = async () => {
      try {
        await authService.logout();
        authService.clearAuth();
        ElMessage.success('已退出登录');
        window.location.href = '/login';
      } catch (error) {
        console.error('Logout error:', error);
        authService.clearAuth();
        window.location.href = '/login';
      }
    };
    
    onMounted(() => {
      document.addEventListener('keydown', (e) => {
        if (e.ctrlKey && e.key === 'f') {
          e.preventDefault();
          focusSearch(e);
        }
      });
      
      loadRecentDocs();
    });
    
    return {
      searchQuery,
      activeNav,
      activeTab,
      navItems,
      tabs,
      usedStorage,
      totalStorage,
      storagePercentage,
      filteredDocuments,
      userAvatar,
      breadcrumb,
      trashMode,
      getFileIcon,
      formatFileSize,
      formatSize,
      formatDate,
      focusSearch,
      handleCreate,
      handleUpload,
      handleNotifications,
      handleUserCommand,
      handleFilterCommand,
      handleRowClick,
      handleDownload,
      handleRestore,
      handlePermanentDelete,
      handleNavClick,
      handleBreadcrumbClick,
      handleBackToCloud,
      handleBackToShare,
      handleActionCommand,
      findMyCloudFolder,
      findMyShareFolder,
      handleMore,
      tableRowClassName,
      loading,
      fileManager,
      handleLogout
    };
  }
};
</script>

<style scoped>
.dashboard {
  display: flex;
  min-height: 100vh;
  background-color: #111827;
  color: #f9fafb;
  font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.sidebar {
  width: 260px;
  background-color: #1f2937;
  border-right: 1px solid #374151;
  display: flex;
  flex-direction: column;
  padding: 20px 12px;
}

.sidebar-header {
  padding: 8px 12px;
  margin-bottom: 24px;
}

.logo {
  font-size: 22px;
  font-weight: 700;
  color: #3b82f6;
  letter-spacing: -0.5px;
}

.sidebar-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 24px;
}

.btn-new,
.btn-upload {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
}

.btn-new {
  background-color: #3b82f6;
  color: white;
}

.btn-new:hover {
  background-color: #2563eb;
}

.btn-upload {
  background-color: #374151;
  color: #d1d5db;
  border: 1px solid #4b5563;
}

.btn-upload:hover {
  background-color: #4b5563;
  color: white;
}

.icon {
  width: 18px;
  height: 18px;
}

.nav-menu {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  color: #9ca3af;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
}

.nav-item:hover {
  background-color: #374151;
  color: white;
}

.nav-item.active {
  background-color: #3b82f6;
  color: white;
}

.nav-icon {
  width: 20px;
  height: 20px;
}

.storage-info {
  padding: 16px;
  background-color: #111827;
  border-radius: 8px;
  margin-top: auto;
}

.storage-label {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #9ca3af;
  margin-bottom: 8px;
}

.storage-value {
  color: #d1d5db;
  font-weight: 500;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background-color: #1f2937;
  border-bottom: 1px solid #374151;
}

.search-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  background-color: #374151;
  border-radius: 8px;
  padding: 10px 16px;
  width: 400px;
  transition: all 0.2s ease;
}

.search-wrapper:focus-within {
  background-color: #4b5563;
  box-shadow: 0 0 0 2px #3b82f6;
}

.search-icon {
  width: 18px;
  height: 18px;
  color: #9ca3af;
}

.search-input {
  flex: 1;
  background: none;
  border: none;
  outline: none;
  color: white;
  font-size: 14px;
}

.search-input::placeholder {
  color: #6b7280;
}

.shortcut-hint {
  font-size: 11px;
  color: #6b7280;
  background-color: #4b5563;
  padding: 2px 6px;
  border-radius: 4px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.icon-btn {
  position: relative;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #374151;
  border: none;
  border-radius: 8px;
  color: #d1d5db;
  cursor: pointer;
  transition: all 0.2s ease;
}

.icon-btn:hover {
  background-color: #4b5563;
  color: white;
}

.icon-btn svg {
  width: 20px;
  height: 20px;
}

.notification-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #ef4444;
  color: white;
  font-size: 10px;
  font-weight: 600;
  border-radius: 50%;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid #3b82f6;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.content-area {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.tabs {
  display: flex;
  gap: 8px;
}

.tab-btn {
  padding: 10px 20px;
  background: none;
  border: none;
  color: #9ca3af;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.tab-btn:hover {
  color: white;
  background-color: #374151;
}

.tab-btn.active {
  background-color: #3b82f6;
  color: white;
}

.filter-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background-color: #374151;
  border: none;
  border-radius: 8px;
  color: #d1d5db;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-btn:hover {
  background-color: #4b5563;
  color: white;
}

.filter-btn svg {
  width: 16px;
  height: 16px;
}

.table-wrapper {
  background-color: #1f2937;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #374151;
}

.doc-name-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.doc-icon {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
}

.doc-name {
  color: #f9fafb;
  font-weight: 500;
}

.star-icon {
  font-size: 14px;
}

.owner-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.owner-avatar {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #3b82f6;
  color: white;
  font-size: 12px;
  font-weight: 600;
  border-radius: 50%;
}

.location-tag {
  padding: 4px 10px;
  background-color: #374151;
  color: #d1d5db;
  font-size: 12px;
  border-radius: 4px;
}

.action-btns {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.action-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: #9ca3af;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.action-btn:hover {
  background-color: #374151;
  color: white;
}

.action-btn svg {
  width: 16px;
  height: 16px;
}

:deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: #111827;
  --el-table-row-hover-bg-color: #374151;
  --el-table-current-row-bg-color: #3b82f6;
  --el-table-border-color: #374151;
  --el-table-text-color: #f9fafb;
  --el-table-header-text-color: #9ca3af;
  --el-table-border: none;
}

:deep(.el-table th.el-table__cell) {
  font-weight: 600;
  font-size: 13px;
}

:deep(.el-table .cell) {
  padding-left: 20px;
  padding-right: 20px;
}

:deep(.el-progress-bar__outer) {
  background-color: #374151;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 0;
  color: #9ca3af;
  font-size: 14px;
}

.breadcrumb-item {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  transition: color 0.2s;
}

.breadcrumb-item:hover {
  color: #3b82f6;
}

.breadcrumb-item.active {
  color: #f9fafb;
  cursor: default;
}

.breadcrumb-separator {
  color: #6b7280;
}

.delete-btn:hover {
  background-color: #ef4444 !important;
  color: white !important;
}
</style>
