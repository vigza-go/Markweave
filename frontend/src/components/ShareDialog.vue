<template>
  <el-dialog
    v-model="dialogVisible"
    title="分享协作"
    width="480px"
    :close-on-click-modal="false"
  >
    <div class="share-content">
      <el-form label-position="top" :model="shareForm">
        <el-form-item label="权限设置">
          <el-radio-group v-model="shareForm.permission">
            <el-radio :value="2">
              <el-icon><Edit /></el-icon>
              可编辑
            </el-radio>
            <el-radio :value="3">
              <el-icon><View /></el-icon>
              仅查看
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="链接有效期">
          <el-select v-model="shareForm.expTime" style="width: 100%">
            <el-option label="7天" :value="7 * 24 * 60" />
            <el-option label="30天" :value="30 * 24 * 60" />
            <el-option label="永久" :value="0" />
          </el-select>
        </el-form-item>

        <el-form-item label="邀请链接">
          <el-input
            v-model="inviteLink"
            readonly
            placeholder="点击生成链接"
          >
            <template #append>
              <el-button @click="generateLink" :loading="generating" :disabled="!isCreator">
                {{ inviteLink ? '重新生成' : '生成链接' }}
              </el-button>
            </template>
          </el-input>
          <div class="link-tip" v-if="inviteLink">
            复制链接发送给协作者，对方登录后即可加入协作
          </div>
          <div class="link-tip" v-else-if="!isCreator">
            您不是文档创建者，无法生成邀请链接
          </div>
        </el-form-item>

        <el-form-item v-if="inviteLink">
          <el-button
            type="primary"
            style="width: 100%"
            @click="copyLink"
          >
            <el-icon><CopyDocument /></el-icon>
            复制链接
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit, View, CopyDocument } from '@element-plus/icons-vue'
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
  docName: {
    type: String,
    default: ''
  },
  isCreator: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible'])

const dialogVisible = ref(false)
const inviteLink = ref('')
const generating = ref(false)

const shareForm = reactive({
  permission: 2,
  expTime: 7 * 24 * 60
})

watch(() => props.visible, (val) => {
  dialogVisible.value = val
  if (val) {
    inviteLink.value = ''
  }
})

watch(dialogVisible, (val) => {
  emit('update:visible', val)
})

const generateLink = async () => {
  if (!props.docId) {
    ElMessage.warning('文档ID无效')
    return
  }

  if (!props.isCreator) {
    ElMessage.warning('您不是文档创建者，无法生成邀请链接')
    return
  }

  generating.value = true
  try {
    const response = await collaborationService.createInvitation({
      docId: props.docId,
      fileName: props.docName || '未命名文档',
      permission: shareForm.permission,
      expTime: shareForm.expTime
    })

    if (response.code === 200) {
      const baseUrl = window.location.origin
      inviteLink.value = `${baseUrl}/invite/${response.data}`
      ElMessage.success('链接生成成功')
    } else {
      ElMessage.error(response.message || '生成失败')
    }
  } catch (error) {
    ElMessage.error('生成失败: ' + (error.response?.data?.message || error.message))
  } finally {
    generating.value = false
  }
}

const copyLink = async () => {
  try {
    await navigator.clipboard.writeText(inviteLink.value)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}
</script>

<style scoped>
.share-content {
  padding: 0 8px;
}

.link-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
</style>
