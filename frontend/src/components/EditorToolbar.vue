<template>
  <div class="editor-toolbar">
    <div class="toolbar-left">
      <el-button-group>
        <el-tooltip content="粗体" placement="top">
          <el-button size="small" @click="formatText('bold')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <path d="M6 4h8a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z" />
              <path d="M6 12h9a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z" />
            </svg>
          </el-button>
        </el-tooltip>
        <el-tooltip content="斜体" placement="top">
          <el-button size="small" @click="formatText('italic')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <line x1="19" y1="4" x2="10" y2="4" />
              <line x1="14" y1="20" x2="5" y2="20" />
              <line x1="15" y1="4" x2="9" y2="20" />
            </svg>
          </el-button>
        </el-tooltip>
        <el-tooltip content="标题" placement="top">
          <el-button size="small" @click="formatText('heading')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <path d="M6 12h12" />
              <path d="M6 4v16" />
              <path d="M18 4v16" />
            </svg>
          </el-button>
        </el-tooltip>
      </el-button-group>

      <el-divider direction="vertical" />

      <el-button-group>
        <el-tooltip content="链接" placement="top">
          <el-button size="small" @click="formatText('link')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" />
              <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" />
            </svg>
          </el-button>
        </el-tooltip>
        <el-tooltip content="代码" placement="top">
          <el-button size="small" @click="formatText('code')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <polyline points="16 18 22 12 16 6" />
              <polyline points="8 6 2 12 8 18" />
            </svg>
          </el-button>
        </el-tooltip>
        <el-tooltip content="引用" placement="top">
          <el-button size="small" @click="formatText('quote')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <path d="M3 21c3 0 7-1 7-8V5c0-1.25-.756-2.017-2-2H4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2 1 0 1 0 1 1v1c0 1-1 2-2 2s-1 .008-1 1.031V21" />
              <path d="M15 21c3 0 7-1 7-8V5c0-1.25-.757-2.017-2-2h-4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2h.75c0 2.25.25 4-2.75 4v3" />
            </svg>
          </el-button>
        </el-tooltip>
      </el-button-group>

      <el-divider direction="vertical" />

      <el-button-group>
        <el-tooltip content="有序列表" placement="top">
          <el-button size="small" @click="formatText('orderedList')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <line x1="10" y1="6" x2="21" y2="6" />
              <line x1="10" y1="12" x2="21" y2="12" />
              <line x1="10" y1="18" x2="21" y2="18" />
              <path d="M4 6h1v4" />
              <path d="M4 10h2" />
              <path d="M6 18H4c0-1 2-2 2-3s-1-1.5-2-1" />
            </svg>
          </el-button>
        </el-tooltip>
        <el-tooltip content="无序列表" placement="top">
          <el-button size="small" @click="formatText('unorderedList')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <line x1="8" y1="6" x2="21" y2="6" />
              <line x1="8" y1="12" x2="21" y2="12" />
              <line x1="8" y1="18" x2="21" y2="18" />
              <line x1="3" y1="6" x2="3.01" y2="6" />
              <line x1="3" y1="12" x2="3.01" y2="12" />
              <line x1="3" y1="18" x2="3.01" y2="18" />
            </svg>
          </el-button>
        </el-tooltip>
      </el-button-group>
    </div>

    <div class="toolbar-right">
      <el-button type="primary" size="small" @click="handleAiFormat" :loading="formatting">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16" style="margin-right: 4px;">
          <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
        </svg>
        AI智能格式化
      </el-button>
      
      <el-dropdown trigger="click" @command="handleExportCommand">
        <el-button size="small">
          导出
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" style="margin-left: 4px;">
            <polyline points="6 9 12 15 18 9" />
          </svg>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="markdown">Markdown</el-dropdown-item>
            <el-dropdown-item command="html">HTML</el-dropdown-item>
            <el-dropdown-item command="pdf">PDF</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { aiService } from '@/services';

const emit = defineEmits(['format', 'export']);

const props = defineProps({
  docId: {
    type: Number,
    required: true
  }
});

const formatting = ref(false);

const formatText = (type) => {
  emit('format', type);
};

const handleAiFormat = async () => {
  formatting.value = true;
  try {
    const response = await aiService.formatMarkdown(props.docId);
    
    if (response.code === 200) {
      emit('format', 'ai', response.data);
      ElMessage.success('格式化完成');
    } else {
      ElMessage.error(response.message || '格式化失败');
    }
  } catch (error) {
    ElMessage.error('格式化失败: ' + (error.response?.data?.message || error.message));
  } finally {
    formatting.value = false;
  }
};

const handleExportCommand = (command) => {
  emit('export', command);
};
</script>

<style scoped>
.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background-color: #fafafa;
  border-bottom: 1px solid #dcdfe6;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.el-divider--vertical) {
  height: 20px;
  margin: 0 8px;
}

:deep(.el-button) { border-color: #d1d5db; color: #374151; background: #fff; }
:deep(.el-button:hover) { background: #f3f4f6; color: #111827; }
</style>


