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
        <el-tooltip content="删除线" placement="top">
          <el-button size="small" @click="formatText('strikethrough')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <path d="M17.3 4.9c-1.3-1-3-1.4-5-1.4-4 0-6.2 2-6.2 4.9 0 1.5.8 2.7 2 3.4" />
              <path d="M4 12h16" />
              <path d="M6.7 19.1c1.3 1 3 1.4 5 1.4 4 0 6.2-2 6.2-4.9 0-1.5-.8-2.7-2-3.4" />
            </svg>
          </el-button>
        </el-tooltip>
      </el-button-group>

      <el-divider direction="vertical" />

      <el-button-group>
        <el-tooltip content="标题1" placement="top">
          <el-button size="small" @click="formatText('h1')">H1</el-button>
        </el-tooltip>
        <el-tooltip content="标题2" placement="top">
          <el-button size="small" @click="formatText('h2')">H2</el-button>
        </el-tooltip>
        <el-tooltip content="标题3" placement="top">
          <el-button size="small" @click="formatText('h3')">H3</el-button>
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
        <el-tooltip content="图片" placement="top">
          <el-button size="small" @click="formatText('image')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
              <circle cx="8.5" cy="8.5" r="1.5" />
              <polyline points="21 15 16 10 5 21" />
            </svg>
          </el-button>
        </el-tooltip>
        <el-tooltip content="代码块" placement="top">
          <el-button size="small" @click="formatText('codeBlock')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <polyline points="16 18 22 12 16 6" />
              <polyline points="8 6 2 12 8 18" />
            </svg>
          </el-button>
        </el-tooltip>
        <el-tooltip content="行内代码" placement="top">
          <el-button size="small" @click="formatText('inlineCode')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <polyline points="4 17 10 11 4 5" />
              <line x1="12" y1="19" x2="20" y2="19" />
            </svg>
          </el-button>
        </el-tooltip>
      </el-button-group>

      <el-divider direction="vertical" />

      <el-button-group>
        <el-tooltip content="引用" placement="top">
          <el-button size="small" @click="formatText('quote')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <path d="M3 21c3 0 7-1 7-8V5c0-1.25-.756-2.017-2-2H4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2 1 0 1 0 1 1v1c0 1-1 2-2 2s-1 .008-1 1.031V21" />
              <path d="M15 21c3 0 7-1 7-8V5c0-1.25-.757-2.017-2-2h-4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2h.75c0 2.25.25 4-2.75 4v3" />
            </svg>
          </el-button>
        </el-tooltip>
        <el-tooltip content="表格" placement="top">
          <el-button size="small" @click="formatText('table')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <rect x="3" y="3" width="18" height="18" rx="2" />
              <line x1="3" y1="9" x2="21" y2="9" />
              <line x1="3" y1="15" x2="21" y2="15" />
              <line x1="9" y1="3" x2="9" y2="21" />
              <line x1="15" y1="3" x2="15" y2="21" />
            </svg>
          </el-button>
        </el-tooltip>
        <el-tooltip content="水平分割线" placement="top">
          <el-button size="small" @click="formatText('hr')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <line x1="3" y1="12" x2="21" y2="12" />
            </svg>
          </el-button>
        </el-tooltip>
      </el-button-group>

      <el-divider direction="vertical" />

      <el-button-group>
        <el-tooltip content="无序列表" placement="top">
          <el-button size="small" @click="formatText('unorderedList')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <line x1="8" y1="6" x2="21" y2="6" />
              <line x1="8" y1="12" x2="21" y2="12" />
              <line x1="8" y1="18" x2="21" y2="18" />
              <circle cx="4" cy="6" r="1" fill="currentColor" />
              <circle cx="4" cy="12" r="1" fill="currentColor" />
              <circle cx="4" cy="18" r="1" fill="currentColor" />
            </svg>
          </el-button>
        </el-tooltip>
        <el-tooltip content="有序列表" placement="top">
          <el-button size="small" @click="formatText('orderedList')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <line x1="10" y1="6" x2="21" y2="6" />
              <line x1="10" y1="12" x2="21" y2="12" />
              <line x1="10" y1="18" x2="21" y2="18" />
              <text x="4" y="8" font-size="8" fill="currentColor">1</text>
              <text x="4" y="14" font-size="8" fill="currentColor">2</text>
              <text x="4" y="20" font-size="8" fill="currentColor">3</text>
            </svg>
          </el-button>
        </el-tooltip>
        <el-tooltip content="任务列表" placement="top">
          <el-button size="small" @click="formatText('taskList')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <line x1="3" y1="6" x2="21" y2="6" />
              <line x1="3" y1="12" x2="21" y2="12" />
              <line x1="3" y1="18" x2="21" y2="18" />
              <rect x="6" y="4" width="4" height="4" rx="1" />
              <rect x="6" y="10" width="4" height="4" rx="1" />
              <rect x="6" y="16" width="4" height="4" rx="1" />
            </svg>
          </el-button>
        </el-tooltip>
      </el-button-group>
    </div>
  </div>
</template>

<script setup>
const emit = defineEmits(['format']);

const formatText = (type) => {
  emit('format', type);
};
</script>

<style scoped>
.editor-toolbar {
  display: flex;
  align-items: center;
  padding: 6px 12px;
  background-color: #fafafa;
  border-bottom: 1px solid #e5e7eb;
  flex-wrap: wrap;
  gap: 4px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 4px;
}

:deep(.el-divider--vertical) {
  height: 20px;
  margin: 0 8px;
  border-left: 1px solid #e5e7eb;
}

:deep(.el-button) {
  border-color: #d1d5db;
  color: #374151;
  background: #fff;
  padding: 6px 10px;
}

:deep(.el-button:hover) {
  background: #f3f4f6;
  color: #111827;
}

:deep(.el-button-group > .el-button) {
  border-radius: 4px;
}
</style>
