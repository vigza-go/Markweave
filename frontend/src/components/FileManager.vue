<template>
  <div class="file-manager">
    <el-dialog v-if="!trashMode" v-model="createDialogVisible" title="新建文档" width="400px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="createForm.fileName" placeholder="请输入文档名称" />
        </el-form-item>
        
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="renameDialogVisible" title="重命名" width="400px">
      <el-form :model="renameForm" label-width="80px">
        <el-form-item label="新名称">
          <el-input v-model="renameForm.newName" placeholder="请输入新名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRename" :loading="renaming">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-if="!trashMode" v-model="moveDialogVisible" title="移动" width="400px">
      <el-tree
        :data="folderTree"
        :props="{ label: 'name', children: 'children' }"
        default-expand-all
        highlight-current
        @node-click="handleFolderSelect"
      >
        <template #default="{ node, data }">
          <span class="folder-node">
            <svg viewBox="0 0 24 24" fill="none" stroke="#f59e0b" stroke-width="2" width="16" height="16">
              <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
            </svg>
            {{ node.label }}
          </span>
        </template>
      </el-tree>
      <template #footer>
        <el-button @click="moveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleMove" :loading="moving">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-if="!trashMode" v-model="shortcutDialogVisible" title="创建快捷方式" width="420px">
      <p class="dialog-desc">将为 <strong>{{ shortcutForm.sourceName }}</strong> 创建快捷方式到所选文件夹。</p>
      <el-tree
        :data="folderTree"
        :props="{ label: 'name', children: 'children' }"
        default-expand-all
        highlight-current
        @node-click="handleShortcutFolderSelect"
      />
      <template #footer>
        <el-button @click="shortcutDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateShortcut" :loading="shortcutting">确定</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { fileSystemService } from '@/services';

const emit = defineEmits(['refresh']);

const props = defineProps({
  currentFolderId: {
    type: Number,
    default: 0
  },
  trashMode: {
    type: Boolean,
    default: false
  }
});

const createDialogVisible = ref(false);
const renameDialogVisible = ref(false);
const moveDialogVisible = ref(false);
const shortcutDialogVisible = ref(false);

const creating = ref(false);
const renaming = ref(false);
const moving = ref(false);
const shortcutting = ref(false);

const createForm = reactive({
  fileName: '',
  fileType: 2
});

const renameForm = reactive({
  nodeId: null,
  newName: ''
});

const moveForm = reactive({
  nodeId: null,
  targetFolderId: null
});

const shortcutForm = reactive({
  srcNodeId: null,
  targetFolderId: null,
  sourceName: ''
});

const folderTree = ref([]);

const showCreateDialog = () => {
  createForm.fileName = '';
  createForm.fileType = 1;
  createDialogVisible.value = true;
};

const showRenameDialog = (node) => {
  renameForm.nodeId = node.id;
  renameForm.newName = node.name;
  renameDialogVisible.value = true;
};

const loadFolderTree = async () => {
  const userSpaceNodeId = getUserSpaceNodeId();
  const targetRootId = userSpaceNodeId || 0;
  const response = await fileSystemService.listFiles(targetRootId);
  if (response.code === 200) {
    folderTree.value = buildFolderTree(response.data || []);
    return;
  }
  throw new Error(response.message || '加载文件夹失败');
};

const showMoveDialog = async (node) => {
  moveForm.nodeId = node.id;
  moveForm.targetFolderId = null;

  try {
    await loadFolderTree();
  } catch (error) {
    ElMessage.error(error.message || '加载文件夹失败');
  }

  moveDialogVisible.value = true;
};

const showShortcutDialog = async (node) => {
  shortcutForm.srcNodeId = node.ptId || node.id;
  shortcutForm.targetFolderId = null;
  shortcutForm.sourceName = node.name;

  try {
    await loadFolderTree();
  } catch (error) {
    ElMessage.error(error.message || '加载文件夹失败');
  }

  shortcutDialogVisible.value = true;
};

const buildFolderTree = (nodes) => {
  const folders = nodes.filter(n => n.type == 1);
  return folders.map(folder => ({
    id: folder.id,
    name: folder.name,
    children: []
  }));
};

const handleFolderSelect = (data) => {
  moveForm.targetFolderId = data.id;
};

const handleShortcutFolderSelect = (data) => {
  shortcutForm.targetFolderId = data.id;
};

const getUserSpaceNodeId = () => {
  const user = JSON.parse(localStorage.getItem('user') || '{}');
  return user.userSpaceNodeId || 0;
};

const handleCreate = async () => {
  if (!createForm.fileName.trim()) {
    ElMessage.warning('请输入文档名称');
    return;
  }

  creating.value = true;
  try {
    const faId = getUserSpaceNodeId();
    const response = await fileSystemService.createFile(
      createForm.fileName,
      faId,
      createForm.fileType
    );

    if (response.code === 200) {
      ElMessage.success('创建成功');
      createDialogVisible.value = false;
      emit('refresh');
    } else {
      ElMessage.error(response.message || '创建失败');
    }
  } catch (error) {
    ElMessage.error('创建失败: ' + (error.response?.data?.message || error.message));
  } finally {
    creating.value = false;
  }
};

const handleRename = async () => {
  if (!renameForm.newName.trim()) {
    ElMessage.warning('请输入新名称');
    return;
  }

  renaming.value = true;
  try {
    const response = await fileSystemService.rename(renameForm.nodeId, renameForm.newName);
    
    if (response.code === 200) {
      ElMessage.success('重命名成功');
      renameDialogVisible.value = false;
      emit('refresh');
    } else {
      ElMessage.error(response.message || '重命名失败');
    }
  } catch (error) {
    ElMessage.error('重命名失败: ' + (error.response?.data?.message || error.message));
  } finally {
    renaming.value = false;
  }
};

const handleMove = async () => {
  if (!moveForm.targetFolderId) {
    ElMessage.warning('请选择目标文件夹');
    return;
  }

  moving.value = true;
  try {
    const response = await fileSystemService.move(moveForm.nodeId, moveForm.targetFolderId);
    
    if (response.code === 200) {
      ElMessage.success('移动成功');
      moveDialogVisible.value = false;
      emit('refresh');
    } else {
      ElMessage.error(response.message || '移动失败');
    }
  } catch (error) {
    ElMessage.error('移动失败: ' + (error.response?.data?.message || error.message));
  } finally {
    moving.value = false;
  }
};

const handleCreateShortcut = async () => {
  if (!shortcutForm.targetFolderId) {
    ElMessage.warning('请选择目标文件夹');
    return;
  }

  shortcutting.value = true;
  try {
    const response = await fileSystemService.createShortcut(shortcutForm.targetFolderId, shortcutForm.srcNodeId);
    if (response.code === 200) {
      ElMessage.success('快捷方式创建成功');
      shortcutDialogVisible.value = false;
      emit('refresh');
    } else {
      ElMessage.error(response.message || '快捷方式创建失败');
    }
  } catch (error) {
    ElMessage.error('快捷方式创建失败: ' + (error.response?.data?.message || error.message));
  } finally {
    shortcutting.value = false;
  }
};

const handleDelete = async (node) => {
  try {
    await ElMessageBox.confirm('确定要将该文档移到回收站吗？', '确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    
    const response = await fileSystemService.recycle(node.id);
    
    if (response.code === 200) {
      ElMessage.success('已移到回收站');
      emit('refresh');
    } else {
      ElMessage.error(response.message || '删除失败');
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + (error.response?.data?.message || error.message));
    }
  }
};

defineExpose({
  showCreateDialog,
  showRenameDialog,
  showMoveDialog,
  showShortcutDialog,
  handleDelete
});
</script>

<style scoped>
.folder-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dialog-desc {
  margin: 0 0 12px;
  color: #6b7280;
}
</style>
