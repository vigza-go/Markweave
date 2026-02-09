<template>
  <el-dialog v-model="dialogVisible" title="选择目标文件夹" width="600px" :close-on-click-modal="false">
    <div class="folder-selector">
      <div class="breadcrumb">
        <span class="breadcrumb-path">
          <a href="#" @click.prevent="goToRoot">我的云盘</a>
          <template v-for="(item, index) in breadcrumb" :key="index">
            <span class="breadcrumb-separator">/</span>
            <a href="#" @click.prevent="navigateTo(index)">{{ item.name }}</a>
          </template>
          <span class="breadcrumb-separator">/</span>
          <span class="current-folder">{{ currentFolderName }}</span>
        </span>
      </div>

      <div class="folder-list">
        <div
          v-for="folder in folders"
          :key="folder.id"
          class="folder-item"
          @click="enterFolder(folder)"
        >
          <svg class="folder-icon" viewBox="0 0 24 24" fill="none" stroke="#f59e0b" stroke-width="2">
            <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
          </svg>
          <span class="folder-name">{{ folder.name }}</span>
          <el-button v-if="canSelectFolder(folder)" type="primary" size="small" @click.stop="selectFolder(folder)">
            选择
          </el-button>
        </div>
        <div v-if="folders.length === 0" class="empty-tip">
          该文件夹下没有子文件夹
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="confirmSelect" :disabled="!selectedFolder">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { fileSystemService } from '@/services/fileSystem';
import { ElMessage } from 'element-plus';
import { FS_NODE_TYPE } from '@/constants/fsNode';

const props = defineProps({
  modelValue: Boolean,
  rootNodeId: {
    type: [String, Number],
    default: null
  },
  excludeNodeId: {
    type: [String, Number],
    default: null
  }
});

const emit = defineEmits(['update:modelValue', 'select']);

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

const currentFolderId = ref(null);
const currentFolderName = ref('');
const folders = ref([]);
const breadcrumb = ref([]);
const selectedFolder = ref(null);
const loading = ref(false);

const init = async () => {
  if (!props.rootNodeId) {
    ElMessage.error('未初始化云盘，无法选择文件夹');
    dialogVisible.value = false;
    return;
  }
  currentFolderId.value = props.rootNodeId;
  currentFolderName.value = '我的云盘';
  breadcrumb.value = [];
  selectedFolder.value = null;
  await loadFolders();
};

const loadFolders = async () => {
  if (!currentFolderId.value) {
    ElMessage.error('文件夹 ID 无效');
    return;
  }
  loading.value = true;
  try {
    const response = await fileSystemService.listFiles(currentFolderId.value);
    if (response.code === 200) {
      const allDocs = response.data || [];
      folders.value = allDocs.filter(doc => doc.type === FS_NODE_TYPE.FOLDER);
    } else {
      ElMessage.error('加载文件夹失败');
    }
  } catch (error) {
    ElMessage.error('加载文件夹失败: ' + (error.response?.data?.message || error.message));
  } finally {
    loading.value = false;
  }
};

const isFolder = (doc) => doc.type === FS_NODE_TYPE.FOLDER;

const canSelectFolder = (folder) => {
  return folder.id !== props.excludeNodeId;
};

const enterFolder = (folder) => {
  if (!canSelectFolder(folder)) {
    ElMessage.warning('不能选择当前文件夹作为目标');
    return;
  }
  if (currentFolderId.value !== props.rootNodeId) {
    breadcrumb.value.push({
      id: currentFolderId.value,
      name: currentFolderName.value
    });
  }
  currentFolderId.value = folder.id;
  currentFolderName.value = folder.name;
  selectedFolder.value = null;
  loadFolders();
};

const goToRoot = () => {
  breadcrumb.value = [];
  currentFolderId.value = props.rootNodeId;
  currentFolderName.value = '我的云盘';
  selectedFolder.value = null;
  loadFolders();
};

const navigateTo = (index) => {
  const target = breadcrumb.value[index];
  currentFolderId.value = target.id;
  currentFolderName.value = target.name;
  breadcrumb.value = breadcrumb.value.slice(0, index);
  selectedFolder.value = null;
  loadFolders();
};

const selectFolder = (folder) => {
  if (!canSelectFolder(folder)) {
    ElMessage.warning('不能选择当前文件夹作为目标');
    return;
  }
  selectedFolder.value = folder;
};

const confirmSelect = () => {
  if (!selectedFolder.value) {
    ElMessage.warning('请选择一个目标文件夹');
    return;
  }
  emit('select', {
    folderId: selectedFolder.value.id,
    folderName: selectedFolder.value.name
  });
  dialogVisible.value = false;
};

watch(dialogVisible, (val) => {
  if (val) {
    init();
  }
});
</script>

<style scoped>
.folder-selector {
  max-height: 400px;
  overflow-y: auto;
}

.breadcrumb {
  margin-bottom: 16px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 14px;
}

.breadcrumb-path {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.breadcrumb-path a {
  color: #409eff;
  text-decoration: none;
}

.breadcrumb-path a:hover {
  color: #66b1ff;
}

.breadcrumb-separator {
  margin: 0 8px;
  color: #909399;
}

.current-folder {
  color: #303133;
  font-weight: 500;
}

.folder-list {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  min-height: 200px;
}

.folder-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f0f2f5;
  transition: background-color 0.2s;
}

.folder-item:hover {
  background-color: #f5f7fa;
}

.folder-item:last-child {
  border-bottom: none;
}

.folder-icon {
  width: 24px;
  height: 24px;
  margin-right: 12px;
}

.folder-name {
  flex: 1;
  font-size: 14px;
  color: #303133;
}

.empty-tip {
  padding: 40px;
  text-align: center;
  color: #909399;
}
</style>
