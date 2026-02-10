<script setup>
import { ref, onMounted, onUnmounted, shallowRef, nextTick, watch } from 'vue'
import { Queue, TextOperation } from '@/js/common.js'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus';
import * as monaco from 'monaco-editor'
import { debounce } from 'lodash-es'
import ShareDialog from '@/components/ShareDialog.vue'
import CollaboratorsDialog from '@/components/CollaboratorsDialog.vue'
import { collaborationService } from '@/services/collaboration'
// --- 深度渲染依赖 ---
import MarkdownIt from 'markdown-it'
import markdownItTaskLists from 'markdown-it-task-lists'
import texmath from 'markdown-it-texmath'
import katex from 'katex'
import mermaid from 'mermaid'
import hljs from 'highlight.js'

// --- 样式导入 ---
import 'katex/dist/katex.min.css'
import 'highlight.js/styles/github.css' // 代码高亮主题
import 'github-markdown-css/github-markdown-light.css'
import EditorToolbar from '@/components/EditorToolbar.vue' // 工具栏组件
// --- 渲染逻辑控制 ---
const debouncedRender = debounce(async () => {
  if (!editorInstance.value) return;
  const val = editorInstance.value.getModel().getValue();

  // 1. Markdown 转换为 HTML 字符串
  previewHtml.value = md.render(val);

  // 2. 异步等待：Vue 更新 DOM 是异步的，这里必须等 DOM 节点生成出来
  await nextTick();

  // 3. 此时预览区已经有了 <div class="mermaid-container">，再去画图
  renderMermaid();
}, 300);

const props = defineProps({
  wsUrl: { type: String, default: 'ws://localhost:8080/ws/collaboration' }
})

const route = useRoute()
const router = useRouter()

const docId = ref(route.params.docId)
const docName = ref(decodeURIComponent(route.query.docName || '未命名文档'))
const isCreator = ref(false)
const canEdit = ref(true)
const version = ref(0)
const editorContainer = ref(null)
const previewPane = ref(null)
const editorInstance = shallowRef(null)
const editorContent = ref('')

watch(() => route.query.docName, (newName) => {
  docName.value = decodeURIComponent(newName || '未命名文档')
  document.title = `${docName.value} - MarkWeave`
})

watch(canEdit, (newVal) => {
  if (editorInstance.value) {
    editorInstance.value.updateOptions({ readOnly: !newVal })
  }
})

onMounted(async () => {
  document.title = `${docName.value} - MarkWeave`
  await checkCreatorStatus()
})

const checkCreatorStatus = async () => {
  try {
    const response = await collaborationService.getCollaborators(docId.value)
    if (response.code === 200) {
      const collaborators = response.data || []

      const currentUser = JSON.parse(localStorage.getItem('user') || '{}')
      const currentUserId = currentUser.id

      console.log('=== 检查权限状态 ===')
      console.log('当前用户ID:', currentUserId)

      let matchedUser = null
      for (const c of collaborators) {
        if (String(c.userId) === String(currentUserId)) {
          matchedUser = c
        }
      }

      console.log('当前用户权限:', matchedUser?.permission)
      isCreator.value = matchedUser?.permission === 1
      canEdit.value = matchedUser?.permission !== 3
      console.log('是否为创建者:', isCreator.value)
      console.log('是否可以编辑:', canEdit.value)
    }
  } catch (error) {
    console.error('检查权限状态失败:', error)
  }
}

const token = localStorage.getItem('token')
const clientId = 'user_' + (localStorage.getItem('userId') || Math.random().toString(16).slice(2))
let currentVersion = 0

let ws = null
let pullInterval = null
let resendInterval = null
let msgId = 0;
// --- 修改后的 Markdown-it 配置 ---
const md = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true,
  highlight: (str, lang) => {
    if (lang && hljs.getLanguage(lang)) {
      try { return hljs.highlight(str, { language: lang }).value; } catch (__) { }
    }
    return '';
  }
})
  .use(markdownItTaskLists, { enabled: true })
  .use(texmath, {
    engine: katex,
    delimiters: 'dollars',
    katexOptions: { throwOnError: false }
  });

// 1. 通用行号注入 (段落、标题、列表等)
const injectLineNumbers = (tokens, idx, options, env, self) => {
  if (tokens[idx].map && tokens[idx].level === 0) {
    tokens[idx].attrSet('data-line', String(tokens[idx].map[0]));
  }
  return self.renderToken(tokens, idx, options);
};
md.renderer.rules.paragraph_open = injectLineNumbers;
md.renderer.rules.heading_open = injectLineNumbers;
md.renderer.rules.list_item_open = injectLineNumbers;

// --- Markdown-it 自定义渲染器 ---
// 这里的逻辑类似后端的拦截器/过滤器
const defaultFence = md.renderer.rules.fence;
md.renderer.rules.fence = (tokens, idx, options, env, self) => {
  const token = tokens[idx];
  const info = token.info ? token.info.trim().toLowerCase() : '';
  const content = token.content;
  const lineAttribute = token.map ? `data-line="${token.map[0]}"` : '';

  if (info === 'mermaid') {
    // 【关键防御方案】：将原始代码进行 URL 编码后存入 data-code 属性
    // 这样就避免了 HTML 解析器把其中的 > 或 & 转义掉，保持了数据的纯净性
    const encodedCode = encodeURIComponent(token.content);
    return `<div class="mermaid-container" ${lineAttribute} data-code="${encodedCode}" data-processed="false"></div>`;
  }
  // 处理数学公式块 (math, katex, latex)
  if (['math', 'katex', 'latex'].includes(info)) {
    try {
      const renderedMath = katex.renderToString(content, {
        displayMode: true,
        throwOnError: false
      });
      return `<div class="katex-block" ${lineAttribute}>${renderedMath}</div>`;
    } catch (e) {
      return `<pre ${lineAttribute}><code>${content}</code></pre>`;
    }
  }

  // 普通代码块：注入行号并使用默认高亮逻辑
  const renderedCode = defaultFence(tokens, idx, options, env, self);
  // 在生成的 <pre> 标签中插入 data-line
  return renderedCode.replace('<pre>', `<pre ${lineAttribute}>`);
};
const previewHtml = ref('');

// --- 2. 协作逻辑锁与队列 (严格保留你的原版) ---
let waitStatus = false
const waitQueue = new Queue(1024)
const bufferMap = ref({})
let isApplyingRemote = false

// --- Mermaid 图表异步生成 ---
const renderMermaid = async () => {
  // 只捞出还没处理过（data-processed="false"）的节点
  const nodes = previewPane.value?.querySelectorAll('.mermaid-container[data-processed="false"]');
  if (!nodes || nodes.length === 0) return;

  mermaid.initialize({ startOnLoad: false, theme: 'default' });

  for (const node of nodes) {
    // 从属性中取回编码后的原始字符串并解码
    const rawCode = decodeURIComponent(node.dataset.code); // 拿到最原始、纯净的代码
    if (!rawCode) continue;

    node.setAttribute('data-processed', 'true');// 立即打上标记，防止重复进入
    const id = `mermaid-${Date.now()}-${Math.random().toString(16).slice(2)}`;

    try {
      const { svg } = await mermaid.render(id, rawCode);
      node.innerHTML = svg;
    } catch (e) {
      node.innerHTML = `<pre style="color:red">Mermaid Error</pre>`;
    }
  }
};

// --- 滚动同步系统 (互斥锁逻辑) ---
let scrollTimer = null;
let activeSide = null; // 这是一个“分布式锁”的思想，标记当前谁拥有滚动权

const handlePreviewScroll = () => {
  // 如果当前是编辑器侧在触发滚动，预览侧就不要反向去干扰
  if (activeSide === 'editor' || !previewPane.value || !editorInstance.value) return;

  activeSide = 'preview';// 抢占锁
  clearTimeout(scrollTimer);

  const nodes = Array.from(previewPane.value.querySelectorAll('[data-line]'));
  // 寻找出现在视口顶部附近的第一个带有 data-line 的 HTML 元素
  const targetNode = nodes.find(node => {
    const rect = node.getBoundingClientRect();
    return rect.top >= 0 && rect.top <= 150; // 窗口顶部 150px 范围内的第一个元素
  });

  if (targetNode) {
    const lineNumber = parseInt(targetNode.getAttribute('data-line')) + 1;
    // 让编辑器跳转到对应行，这里会触发 handleEditorScroll，所以上面的 activeSide 锁至关重要
    editorInstance.value.revealLineInCenterIfOutsideViewport(lineNumber);
  }

  // 滚动停止 100ms 后释放锁
  scrollTimer = setTimeout(() => { activeSide = null; }, 100);
};

const handleEditorScroll = () => {
  if (activeSide === 'preview' || !editorInstance.value || !previewPane.value) return;

  activeSide = 'editor';// 抢占锁
  clearTimeout(scrollTimer);

  const ranges = editorInstance.value.getVisibleRanges();
  if (ranges.length > 0) {
    const line = ranges[0].startLineNumber - 1;
    const target = previewPane.value.querySelector(`[data-line="${line}"]`);
    if (target) {
      // 计算得出预览区对应的 OffsetTop，直接跳转
      previewPane.value.scrollTo({
        top: target.offsetTop,
        behavior: 'auto' // 'auto' 代表立即跳转，比 'smooth' 延迟更低，减少同步偏差
      });
    }
  }

  scrollTimer = setTimeout(() => { activeSide = null; }, 100);
};
const getMsgBody = () => {
  return { docId: docId.value, method: null, clientId, msgId: msgId++, version: currentVersion, data: null }
}

// --- 4. 协作逻辑保持 (sendOp, initEditor, applyRemoteOp) ---
const RESEND_TIMEOUT = 3000; // 3 秒没收到 ACK 就重发
const MAX_RESEND_ATTEMPTS = 3; // 最大重试次数
let lst_send_time = Date.now();
const sendOp = (op) => {
  const msg = getMsgBody();
  msg.method = "sendOp";
  msg.data = { op: op };
  waitQueue.push(msg);
  if (!waitStatus) {
    waitStatus = true;
    lst_send_time = Date.now();
    ws.send(JSON.stringify(msg));
  }
}

const initEditor = () => {
  editorInstance.value = monaco.editor.create(editorContainer.value, {
    value: '',
    language: 'markdown',
    theme: 'vs-light',
    automaticLayout: true,
    fontSize: 16,
    wordWrap: 'on',
    readOnly: !canEdit.value
  });

  editorInstance.value.onDidScrollChange(handleEditorScroll);

  editorInstance.value.onDidChangeModelContent((e) => {
    if (isApplyingRemote) return;
    const model = editorInstance.value.getModel();
    const val = model.getValue();
    // 更新内容并触发预览
    editorContent.value = val;
    previewHtml.value = md.render(val);
    debouncedRender();

    // 你的原版 OT 生成逻辑
    const operation = new TextOperation();
    let currentLength = model.getValueLength();
    let delta = 0;
    e.changes.forEach(c => delta += (c.text.length - c.rangeLength));
    const originalLength = currentLength - delta;
    const sortedChanges = [...e.changes].sort((a, b) => a.rangeOffset - b.rangeOffset);
    let lastOffset = 0;
    sortedChanges.forEach((change) => {
      const { rangeOffset, rangeLength, text } = change;
      const skip = rangeOffset - lastOffset;
      if (skip > 0) operation.retain(skip);
      if (rangeLength > 0) operation.delete(rangeLength);
      if (text !== "") operation.insert(text);
      lastOffset = rangeOffset + rangeLength;
    });
    if (lastOffset < originalLength) operation.retain(originalLength - lastOffset);
    sendOp(operation.toJSON());
  });
}
// --- 远程数据应用 (OT 冲突处理的核心入口) ---
const applyRemoteOp = async (msg) => {
  isApplyingRemote = true;
  currentVersion = msg.version;
  version.value = currentVersion;

  if (msg.clientId === clientId) {
    waitQueue.pop();
    waitStatus = false;
    isApplyingRemote = false;
    if (waitQueue.size > 0) {
      waitStatus = true;
      const nextMsg = waitQueue.getFront();
      nextMsg.version = currentVersion;
      ws.send(JSON.stringify(nextMsg));
    }
    return;
  }

  const model = editorInstance.value.getModel();
  let remoteOp = new TextOperation.fromJSON(msg.data.op);
  // 你的原版 Transform 逻辑
  for (const localMsg of waitQueue) {
    const localOp = new TextOperation.fromJSON(localMsg.data.op);
    if (localMsg.clientId < msg.clientId) {
      const pair = TextOperation.transform(localOp, remoteOp);
      localMsg.data.op = pair[0].toJSON(); remoteOp = pair[1];
    } else {
      const pair = TextOperation.transform(remoteOp, localOp);
      remoteOp = pair[0]; localMsg.data.op = pair[1].toJSON();
    }
  }

  const edits = [];
  let index = 0;
  remoteOp.ops.forEach(op => {
    if (TextOperation.isRetain(op)) index += op;
    else if (TextOperation.isInsert(op)) {
      const pos = model.getPositionAt(index);
      edits.push({ range: new monaco.Range(pos.lineNumber, pos.column, pos.lineNumber, pos.column), text: op });
    } else if (TextOperation.isDelete(op)) {
      const len = Math.abs(op);
      const startPos = model.getPositionAt(index);
      const endPos = model.getPositionAt(index + len);
      edits.push({ range: new monaco.Range(startPos.lineNumber, startPos.column, endPos.lineNumber, endPos.column), text: '' });
      index += len;
    }
  });
  // 将计算后的 Edits 应用到 Monaco 编辑器
  editorInstance.value.executeEdits('remote-sync', edits);

  // 此时文本已变，触发一次手动渲染
  const newVal = model.getValue();
  editorContent.value = newVal;
  previewHtml.value = md.render(newVal);
  renderMermaid();

  // 【后端特别注意】：远程数据应用后，高度会变化
  // 先等 HTML 渲染，再等 Mermaid 画图，画完后浏览器才会知道最终的页面高度
  await nextTick();
  await renderMermaid();
  // 渲染完后，不要手动去改滚动条，让用户保持在原来的位置
  isApplyingRemote = false;
}

// --- 5. 工具栏逻辑回归 ---
const applyFormat = (type, content) => {
  if (!editorInstance.value) return;
  const selection = editorInstance.value.getSelection();
  const model = editorInstance.value.getModel();
  const selected = model.getValueInRange(selection);

  if (type === 'ai' && typeof content === 'string') {
    editorInstance.value.setValue(content);
    return;
  }

  const formats = {
    bold: `**${selected || '文本'}**`,
    italic: `*${selected || '文本'}*`,
    heading: `## ${selected || '标题'}`,
    code: `\`\`\`\n${selected || 'code'}\n\`\`\``
  };

  const text = formats[type];
  if (text) {
    editorInstance.value.executeEdits('format', [{ range: selection, text, forceMoveMarkers: true }]);
  }
};

const connectWebSocket = () => {
  const wsUrl = new URL(props.wsUrl);
  wsUrl.searchParams.set('token', token);
  wsUrl.searchParams.set('clientId', clientId);
  ws = new WebSocket(wsUrl.toString());

  ws.onopen = () => {
    let msg = getMsgBody();
    msg.method = 'getNew';
    ws.send(JSON.stringify(msg));
  }

  ws.onmessage = (e) => {
    const msg = JSON.parse(e.data);
    msg.version = Number(msg.version)
    if (msg.method === 'error') {
      ElMessage.error(msg.data.msg);
      return;
    }
    if (msg.method === 'fullText') {
      isApplyingRemote = true;
      currentVersion = msg.version;
      version.value = currentVersion;
      editorInstance.value.setValue(msg.data.text);
      while (waitQueue.size > 0) {
        waitQueue.pop();
      }
      previewHtml.value = md.render(msg.data.text);
      renderMermaid();
      isApplyingRemote = false;
      return;
    }
    // 版本同步逻辑
    if (msg.version <= currentVersion) return;
    if (msg.version > currentVersion + 1) {
      bufferMap.value[msg.version] = msg;
    } else {
      applyRemoteOp(msg);
      while (bufferMap.value[currentVersion + 1]) {
        applyRemoteOp(bufferMap.value[++currentVersion]);
        delete bufferMap.value[currentVersion];
      }
    }
  };
};

const startPullHistory = () => {
  pullInterval = setInterval(() => {
    if (Object.keys(bufferMap.value).length > 0) {
      let msg = getMsgBody();
      msg.method = "pullHistory";
      ws.send(JSON.stringify(msg));
    }
  }, 5000);
};

// 超时补发
const startAckWatchdog = () => {
  resendInterval = setInterval(() => {
    if (waitQueue.size > 0 && waitStatus) {
      const now = Date.now();
      const frontMsg = waitQueue.getFront();
      const lastTime = lst_send_time || 0;

      if (now - lastTime > RESEND_TIMEOUT) {
        console.warn(`检测到消息 ${frontMsg.msgId} 的 ACK 超时，补发中...`);
        frontMsg.version = currentVersion;
        lst_send_time = Date.now();
        // 发送给后端（此时 msg 还是干净的，后端能正常反序列化）
        ws.send(JSON.stringify(frontMsg));
      }
    }
  }, 1000);
};

onMounted(() => {
  initEditor();
  connectWebSocket();
  startPullHistory();
  startAckWatchdog();
  // 类似于一个后台 Daemon 线程，持续监听 DOM 变化
  const observer = new MutationObserver(debounce(() => {
    // 只要 previewPane 内部结构变了（比如协作同步了新内容），就检查有没有新的图表要画
    renderMermaid();
  }, 100));

  if (previewPane.value) {
    observer.observe(previewPane.value, { childList: true, subtree: true });
    // 监听原生滚动事件
    previewPane.value.addEventListener('scroll', handlePreviewScroll, { passive: true });
  }
});

onUnmounted(() => {
  if (editorInstance.value) editorInstance.value.dispose();
  if (ws) ws.close();
  if (pullInterval) clearInterval(pullInterval);
  if (resendInterval) clearInterval(resendInterval);
});

const goBack = () => router.push('/dashboard');

const showShareDialog = ref(false);
const showCollaboratorsDialog = ref(false);
</script>

<template>
  <div class="editor-wrapper">
    <div class="editor-header">
      <button class="back-btn" @click="goBack">← 返回</button>
      <div class="doc-title">{{ docName }}</div>
      <div class="status-info">
        <span v-if="!canEdit" class="readonly-badge">只读</span>
        版本: {{ version }} | 协作 ID: {{ clientId }}
      </div>
      <div class="header-actions">
        <button class="action-btn" @click="showCollaboratorsDialog = true">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
            <path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/>
          </svg>
          协作
        </button>
        <button class="action-btn" @click="showShareDialog = true" v-if="isCreator">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
            <path d="M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92s2.92-1.31 2.92-2.92-1.31-2.92-2.92-2.92z"/>
          </svg>
          分享
        </button>
      </div>
    </div>

    <EditorToolbar :doc-id="Number(docId)" @format="applyFormat" />

    <div class="main-content">
      <div ref="editorContainer" class="monaco-box"></div>
      <div ref="previewPane" class="preview-pane markdown-body" v-html="previewHtml"></div>
    </div>

    <ShareDialog
      v-model:visible="showShareDialog"
      :doc-id="docId"
      :doc-name="docName"
      :is-creator="isCreator"
    />

    <CollaboratorsDialog
      v-model:visible="showCollaboratorsDialog"
      :doc-id="docId"
      :is-creator="isCreator"
    />
  </div>
</template>

<style scoped>
.editor-wrapper {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.editor-header {
  padding: 8px 16px;
  background: #1e1e1e;
  color: #fff;
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.readonly-badge {
  background: #e6a23c;
  color: #fff;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.doc-title {
  font-size: 16px;
  font-weight: 500;
  color: #fff;
  max-width: 400px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.back-btn {
  background: #333;
  border: none;
  color: #ccc;
  padding: 4px 12px;
  border-radius: 4px;
  cursor: pointer;
}

.back-btn:hover {
  background: #444;
  color: #fff;
}

.header-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

.action-btn {
  background: #333;
  border: none;
  color: #ccc;
  padding: 4px 12px;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
}

.action-btn:hover {
  background: #444;
  color: #fff;
}

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
  background: #fff;
}

.monaco-box {
  flex: 1;
  border-right: 1px solid #ddd;
}

.preview-pane {
  flex: 1;
  padding: 30px;
  overflow-y: auto;
}

/* 深度渲染代码高亮样式微调 */
:deep(pre code.hljs) {
  padding: 1.2em;
  border-radius: 8px;
  font-family: 'Fira Code', monospace;
}

:deep(.katex-display) {
  margin: 1em 0;
  overflow-x: auto;
}

/* 代码块容器样式 */
:deep(pre[data-line]) {
  position: relative;
  background: #f6f8fa;
  border-radius: 6px;
  padding: 16px;
  margin: 16px 0;
  overflow: auto;
}

/* 公式块容器样式 */
:deep(.katex-block[data-line]) {
  padding: 1rem 0;
  overflow-x: auto;
  overflow-y: hidden;
  text-align: center;
}




:deep(.mermaid) {
  min-height: 50px;
  /* 给个占位高度 */
  display: flex;
  justify-content: center;
  background: #fff;
}

/* 当源码还没被渲染时，隐藏那些乱掉的文字，避免闪烁 */
:deep(.mermaid[data-processed="false"]) {
  visibility: hidden;
  height: 0;
}

:deep(.mermaid svg) {
  max-width: 100% !important;
  height: auto !important;
}

:deep(.mermaid-container) {
  min-height: 150px;
  /* 预估一个平均高度 */
  display: block;
  margin: 10px 0;
  transition: min-height 0.3s;
  /* 平滑过渡 */
}
</style>