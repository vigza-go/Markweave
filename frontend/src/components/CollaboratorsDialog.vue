<template>
  <el-dialog
    v-model="dialogVisible"
    title="协作者"
    width="480px"
    :close-on-click-modal="false"
  >
    <div class="collaborators-content" v-loading="loading">
      <div class="collaborators-list" v-if="collaborators.length > 0">
        <div
          v-for="collab in collaborators"
          :key="collab.userId"
          class="collaborator-item"
        >
          <div class="collab-left">
            <el-avatar :size="40" :src="collab.headUrl">
              {{ collab.nickName?.charAt(0) }}
            </el-avatar>
            <div class="collab-info">
              <span class="collab-name">{{ collab.nickName }}</span>
              <el-tag
                v-if="collab.permission === 1"
                type="info"
                size="small"
                effect="plain"
              >
                创建者
              </el-tag>
            </div>
          </div>
          <div class="collab-right">
            <el-select
              v-if="isCreator && collab.permission !== 1"
              v-model="collab.permission"
              size="small"
              style="width: 100px"
              @change="(val) => handlePermissionChange(collab, val)"
            >
              <el-option label="可编辑" :value="2" />
              <el-option label="仅查看" :value="3" />
            </el-select>
            <span v-else class="permission-label">
              {{ getPermissionLabel(collab.permission) }}
            </span>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无协作者" :image-size="80" />
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { collaborationService } from '@/services/collaboration'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  docId: {
    type: [String, Number],
    required: true
  },
  isCreator: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible'])

const dialogVisible = ref(false)
const loading = ref(false)
const collaborators = ref([])

const getPermissionLabel = (permission) => {
  const labels = {
    1: '创建者',
    2: '可编辑',
    3: '仅查看'
  }
  return labels[permission] || '未知'
}

const loadCollaborators = async () => {
  loading.value = true
  try {
    const response = await collaborationService.getCollaborators(props.docId)
    if (response.code === 200) {
      collaborators.value = response.data || []
    } else {
      ElMessage.error(response.message || '加载协作者失败')
    }
  } catch (error) {
    console.error('加载协作者失败:', error)
    ElMessage.error('加载协作者失败')
  } finally {
    loading.value = false
  }
}

const handlePermissionChange = async (collab, newPermission) => {
  try {
    const response = await collaborationService.updatePermission(
      props.docId,
      collab.userId,
      newPermission
    )
    if (response.code === 200) {
      ElMessage.success('权限已更新')
      loadCollaborators()
    } else {
      ElMessage.error(response.message || '更新失败')
      loadCollaborators()
    }
  } catch (error) {
    ElMessage.error('更新失败: ' + (error.response?.data?.message || error.message))
    loadCollaborators()
  }
}

watch(() => props.visible, (val) => {
  dialogVisible.value = val
  if (val) {
    loadCollaborators()
  }
})

watch(dialogVisible, (val) => {
  emit('update:visible', val)
})
</script>

<style scoped>
.collaborators-content {
  min-height: 200px;
}

.collaborators-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.collaborator-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
}

.collab-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.collab-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.collab-name {
  font-size: 14px;
  color: #303133;
}

.collab-right {
  display: flex;
  align-items: center;
}

.permission-label {
  font-size: 12px;
  color: #909399;
}
</style>
