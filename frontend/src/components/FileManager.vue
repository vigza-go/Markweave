<template>
  <div>
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="400px" :close-on-click-modal="false">
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="0">
        <el-form-item prop="fileName">
          <el-input v-model="formData.fileName" :placeholder="namePlaceholder" maxlength="100" show-word-limit
            autofocus />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="loading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="renameDialogVisible" title="重命名" width="400px" :close-on-click-modal="false">
      <el-form :model="renameFormData" :rules="renameRules" ref="renameFormRef" label-width="0">
        <el-form-item prop="newName">
          <el-input v-model="renameFormData.newName" placeholder="请输入新名称" maxlength="100" show-word-limit autofocus />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRenameSubmit" :loading="renameLoading">确定</el-button>
      </template>
    </el-dialog>

    <FolderSelector
      v-model="folderSelectorVisible"
      :root-node-id="userSpaceNodeId"
      :exclude-node-id="currentMoveRow?.id"
      @select="handleFolderSelect"
    />

    <FolderSelector
      v-model="shortcutFolderSelectorVisible"
      :root-node-id="userSpaceNodeId"
      :exclude-node-id="currentShortcutRow?.id"
      @select="handleShortcutFolderSelect"
    />
  </div>
</template>

<script>
import { ref, computed, nextTick, toRef } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { fileSystemService } from '@/services';
import { FS_NODE_TYPE } from '@/constants/fsNode';
import FolderSelector from './FolderSelector.vue';

export default {
  name: 'FileManager',
  components: {
    FolderSelector
  },
  props: {
    currentFolderId: {
      type: [String, Number],
      default: null
    },
    userSpaceNodeId: {
      type: [String, Number],
      default: null
    },
    cloudDriveNodeId: {
      type: [String, Number],
      default: null
    },
    trashMode: {
      type: Boolean,
      default: false
    }
  },
  emits: ['refresh'],
  setup(props, { emit }) {
    const formRef = ref(null);
    const renameFormRef = ref(null);

    const dialogVisible = ref(false);
    const dialogTitle = ref('创建文件');
    const loading = ref(false);
    const formData = ref({
      fileName: '',
      faId: null,
      fileType: FS_NODE_TYPE.FILE
    });

    const currentFolderIdRef = toRef(props, 'currentFolderId');
    const cloudDriveNodeIdRef = toRef(props, 'cloudDriveNodeId');

    const userSpaceNodeId = computed(() => props.userSpaceNodeId);

    const rules = {
      fileName: [
        { required: true, message: '请输入名称', trigger: 'blur' },
        { min: 1, max: 100, message: '名称长度必须在1-100个字符之间', trigger: 'blur' },
        { pattern: /^[^\\/:*?"<>|]+$/, message: '名称不能包含特殊字符 \\ / : * ? " < > |', trigger: 'blur' }
      ]
    };

    const namePlaceholder = computed(() => {
      return formData.value.fileType === FS_NODE_TYPE.FOLDER ? '请输入文件夹名称' : '请输入文件名称';
    });

    const renameDialogVisible = ref(false);
    const renameLoading = ref(false);
    const renameFormData = ref({
      nodeId: null,
      newName: ''
    });
    const currentRenameRow = ref(null);

    const renameRules = {
      newName: [
        { required: true, message: '请输入新名称', trigger: 'blur' },
        { min: 1, max: 100, message: '名称长度必须在1-100个字符之间', trigger: 'blur' },
        { pattern: /^[^\\/:*?"<>|]+$/, message: '名称不能包含特殊字符 \\ / : * ? " < > |', trigger: 'blur' }
      ]
    };

    const folderSelectorVisible = ref(false);
    const currentMoveRow = ref(null);
    const selectedMoveTargetId = ref(null);

    const shortcutFolderSelectorVisible = ref(false);
    const shortcutLoading = ref(false);
    const shortcutFormData = ref({
      faId: null
    });
    const selectedShortcutTargetId = ref(null);
    const currentShortcutRow = ref(null);

    const shortcutRules = {
      faId: [
        { required: true, message: '请选择目标文件夹', trigger: 'change' }
      ]
    };

    const showCreateDialog = async (fileType, parentFolderId = null) => {
      formData.value.fileName = '';
      formData.value.fileType = fileType;
      
      const folderId = parentFolderId !== null ? parentFolderId : currentFolderIdRef.value;
      
      if (folderId === null || folderId === undefined || folderId === 0) {
        ElMessage.warning('请先进入云盘目录后再创建文件');
        return;
      }
      
      formData.value.faId = folderId;

      dialogTitle.value = fileType === FS_NODE_TYPE.FOLDER ? '创建文件夹' : '创建文件';

      dialogVisible.value = true;

      await nextTick();
      formRef.value?.clearValidate();
    };

    const showRenameDialog = async (row) => {
      currentRenameRow.value = row;
      renameFormData.value.nodeId = row.id;
      renameFormData.value.newName = row.name || row.docName || '';

      renameDialogVisible.value = true;

      await nextTick();
      renameFormRef.value?.clearValidate();
    };

    const showMoveDialog = async (row) => {
      currentMoveRow.value = row;
      selectedMoveTargetId.value = null;
      folderSelectorVisible.value = true;
    };

    const handleFolderSelect = (folderInfo) => {
      selectedMoveTargetId.value = folderInfo.folderId;
      handleMoveSubmit();
    };

    const showShortcutDialog = async (row) => {
      currentShortcutRow.value = row;
      selectedShortcutTargetId.value = null;
      shortcutFormData.value.faId = null;
      shortcutFolderSelectorVisible.value = true;
    };

    const handleShortcutFolderSelect = (folderInfo) => {
      selectedShortcutTargetId.value = folderInfo.folderId;
      handleShortcutSubmit();
    };

    const handleSubmit = async () => {
      try {
        await formRef.value.validate();
        loading.value = true;

        const response = await fileSystemService.createFile(
          formData.value.fileName,
          formData.value.faId,
          formData.value.fileType
        );

        if (response.code === 200) {
          ElMessage.success(formData.value.fileType === FS_NODE_TYPE.FOLDER ? '文件夹创建成功' : '文件创建成功');
          dialogVisible.value = false;
          emit('refresh');
        } else {
          ElMessage.error(response.message || '创建失败');
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('创建失败: ' + (error.response?.data?.message || error.message));
        }
      } finally {
        loading.value = false;
      }
    };

    const handleRenameSubmit = async () => {
      try {
        await renameFormRef.value.validate();
        renameLoading.value = true;

        const response = await fileSystemService.rename(
          renameFormData.value.nodeId,
          renameFormData.value.newName
        );

        if (response.code === 200) {
          ElMessage.success('重命名成功');
          renameDialogVisible.value = false;
          emit('refresh');
        } else {
          ElMessage.error(response.message || '重命名失败');
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('重命名失败: ' + (error.response?.data?.message || error.message));
        }
      } finally {
        renameLoading.value = false;
      }
    };

    const handleMoveSubmit = async () => {
      if (!selectedMoveTargetId.value) {
        ElMessage.warning('请选择目标文件夹');
        return;
      }

      if (selectedMoveTargetId.value === currentMoveRow.value.id) {
        ElMessage.warning('不能将文件夹移动到自身');
        return;
      }

      try {
        const response = await fileSystemService.move(
          currentMoveRow.value.id,
          selectedMoveTargetId.value
        );

        if (response.code === 200) {
          ElMessage.success('移动成功');
          emit('refresh');
        } else {
          ElMessage.error(response.message || '移动失败');
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('移动失败: ' + (error.response?.data?.message || error.message));
        }
      }
    };

    const handleShortcutSubmit = async () => {
      if (!selectedShortcutTargetId.value) {
        ElMessage.warning('请选择目标文件夹');
        return;
      }

      try {
        shortcutLoading.value = true;

        const response = await fileSystemService.createShortcut(
          selectedShortcutTargetId.value,
          currentShortcutRow.value.id
        );

        if (response.code === 200) {
          ElMessage.success('快捷方式创建成功');
          emit('refresh');
        } else {
          ElMessage.error(response.message || '创建快捷方式失败');
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('创建快捷方式失败: ' + (error.response?.data?.message || error.message));
        }
      } finally {
        shortcutLoading.value = false;
      }
    };

    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm(
          `确定要删除 "${row.name || row.docName}" 吗？删除后可在回收站恢复。`,
          '确认删除',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        );

        const response = await fileSystemService.recycle(row.id);

        if (response.code === 200) {
          ElMessage.success('删除成功');
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

    return {
      formRef,
      renameFormRef,
      dialogVisible,
      dialogTitle,
      loading,
      formData,
      rules,
      namePlaceholder,
      renameDialogVisible,
      renameLoading,
      renameFormData,
      renameRules,
      folderSelectorVisible,
      currentMoveRow,
      userSpaceNodeId,
      cloudDriveNodeIdRef,
      shortcutFolderSelectorVisible,
      shortcutLoading,
      shortcutFormData,
      shortcutRules,
      currentShortcutRow,
      selectedShortcutTargetId,
      showCreateDialog,
      showRenameDialog,
      showMoveDialog,
      showShortcutDialog,
      handleSubmit,
      handleRenameSubmit,
      handleMoveSubmit,
      handleFolderSelect,
      handleShortcutFolderSelect,
      handleShortcutSubmit,
      handleDelete
    };
  }
};
</script>
