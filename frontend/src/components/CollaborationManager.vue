<template>
  <div class="collaboration-manager">
    <el-dialog v-model="inviteDialogVisible" title="邀请协作" width="450px">
      <el-form :model="inviteForm" label-width="80px">
        <el-form-item label="协作权限">
          <el-select v-model="inviteForm.permission" placeholder="选择权限">
            <el-option label="可查看" :value="1" />
            <el-option label="可编辑" :value="2" />
            <el-option label="可管理" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="有效期">
          <el-select v-model="inviteForm.expTime" placeholder="选择有效期">
            <el-option label="1小时" :value="1" />
            <el-option label="6小时" :value="6" />
            <el-option label="12小时" :value="12" />
            <el-option label="1天" :value="24" />
            <el-option label="7天" :value="168" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inviteDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateInvitation" :loading="creating">生成邀请链接</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="collaboratorsDialogVisible" title="协作者管理" width="600px">
      <el-table :data="collaborators" style="width: 100%">
        <el-table-column prop="userName" label="用户" width="150" />
        <el-table-column prop="permission" label="权限" width="120">
          <template #default="{ row }">
            <el-tag :type="getPermissionTagType(row.permission)">
              {{ getPermissionLabel(row.permission) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-dropdown trigger="click" @command="(command) => handlePermissionChange(command, row)">
              <el-button size="small">修改权限</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="1">可查看</el-dropdown-item>
                  <el-dropdown-item command="2">可编辑</el-dropdown-item>
                  <el-dropdown-item command="3">可管理</el-dropdown-item>
                  <el-dropdown-item command="0" divided>移除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="collaboratorsDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { collaborationService } from '@/services';

const emit = defineEmits(['refresh']);

const props = defineProps({
  docId: {
    type: Number,
    required: true
  }
});

const inviteDialogVisible = ref(false);
const collaboratorsDialogVisible = ref(false);

const creating = ref(false);
const collaborators = ref([]);

const inviteForm = reactive({
  permission: 2,
  expTime: 24
});

const showInviteDialog = () => {
  inviteForm.permission = 2;
  inviteForm.expTime = 24;
  inviteDialogVisible.value = true;
};

const showCollaboratorsDialog = async () => {
  try {
    const response = await collaborationService.getCollaborators(props.docId);
    if (response.code === 200) {
      collaborators.value = response.data || [];
    } else {
      ElMessage.error(response.message || '加载协作者失败');
    }
  } catch (error) {
    ElMessage.error('加载协作者失败: ' + (error.response?.data?.message || error.message));
  }
  
  collaboratorsDialogVisible.value = true;
};

const getPermissionLabel = (permission) => {
  const labels = {
    1: '可查看',
    2: '可编辑',
    3: '可管理'
  };
  return labels[permission] || '未知';
};

const getPermissionTagType = (permission) => {
  const types = {
    1: 'info',
    2: 'warning',
    3: 'success'
  };
  return types[permission] || 'info';
};

const handleCreateInvitation = async () => {
  creating.value = true;
  try {
    const response = await collaborationService.createInvitation(
      props.docId,
      inviteForm.permission,
      inviteForm.expTime
    );
    
    if (response.code === 200) {
      const inviteLink = `${window.location.origin}/invite/${response.data}`;
      await navigator.clipboard.writeText(inviteLink);
      ElMessage.success('邀请链接已复制到剪贴板');
      inviteDialogVisible.value = false;
    } else {
      ElMessage.error(response.message || '创建邀请失败');
    }
  } catch (error) {
    ElMessage.error('创建邀请失败: ' + (error.response?.data?.message || error.message));
  } finally {
    creating.value = false;
  }
};

const handlePermissionChange = async (command, row) => {
  const newPermission = parseInt(command);
  
  if (newPermission === 0) {
    try {
      await ElMessageBox.confirm('确定要移除该协作者吗？', '确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      });
      
      const response = await collaborationService.updatePermission(
        props.docId,
        row.userId,
        0
      );
      
      if (response.code === 200) {
        ElMessage.success('已移除协作者');
        showCollaboratorsDialog();
        emit('refresh');
      } else {
        ElMessage.error(response.message || '移除失败');
      }
    } catch (error) {
      if (error !== 'cancel') {
        ElMessage.error('移除失败: ' + (error.response?.data?.message || error.message));
      }
    }
  } else {
    try {
      const response = await collaborationService.updatePermission(
        props.docId,
        row.userId,
        newPermission
      );
      
      if (response.code === 200) {
        ElMessage.success('权限已更新');
        showCollaboratorsDialog();
        emit('refresh');
      } else {
        ElMessage.error(response.message || '更新权限失败');
      }
    } catch (error) {
      ElMessage.error('更新权限失败: ' + (error.response?.data?.message || error.message));
    }
  }
};

defineExpose({
  showInviteDialog,
  showCollaboratorsDialog
});
</script>
