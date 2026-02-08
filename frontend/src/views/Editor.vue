<template>
  <div class="editor-page">
    <EditorToolbar 
      :doc-id="docId" 
      @format="handleFormat" 
      @export="handleExport"
    />
    
    <div class="editor-container">
      <div ref="editorContainer" class="monaco-editor"></div>
    </div>
    
    <CollaborationManager 
      ref="collaborationManager"
      :doc-id="docId"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import * as monaco from 'monaco-editor';
import { Queue, TextOperation } from '@/js/common.js';
import EditorToolbar from '@/components/EditorToolbar.vue';
import CollaborationManager from '@/components/CollaborationManager.vue';

const route = useRoute();
const docId = ref(parseInt(route.params.docId) || 123);

const editorContainer = ref(null);
const editorInstance = ref(null);
const collaborationManager = ref(null);

const clientId = 'user_' + Math.random().toString(16).slice(2);
let currentVersion = 0;

let ws = null;
let waitStatus = false;
const waitQueue = new Queue(1024);
const bufferMap = ref({});
let isApplyingRemote = false;
let contentChangeDisposable = null;

const initEditor = () => {
  editorInstance.value = monaco.editor.create(editorContainer.value, {
    value: '',
    language: 'markdown',
    theme: 'vs-dark',
    automaticLayout: true,
    fontSize: 16,
    minimap: { enabled: true },
    wordWrap: 'on'
  });

  contentChangeDisposable = editorInstance.value.onDidChangeModelContent((e) => {
    if (isApplyingRemote || !ws || ws.readyState !== WebSocket.OPEN || waitQueue.size >= 1024) return;

    const model = editorInstance.value.getModel();
    if (!model || !e.changes?.length) return;

    const operation = new TextOperation();
    const currentLength = model.getValueLength();
    let delta = 0;

    e.changes.forEach(c => {
      delta += (c.text.length - c.rangeLength);
    });
    const originalLength = currentLength - delta;

    const sortedChanges = [...e.changes].sort((a, b) => a.rangeOffset - b.rangeOffset);
    let lastOffset = 0;

    sortedChanges.forEach((change) => {
      const { rangeOffset, rangeLength, text } = change;
      const skip = rangeOffset - lastOffset;
      if (skip > 0) {
        operation.retain(skip);
      }
      if (rangeLength > 0) {
        operation.delete(rangeLength);
      }
      if (text !== '') {
        operation.insert(text);
      }
      lastOffset = rangeOffset + rangeLength;
    });

    if (lastOffset < originalLength) {
      operation.retain(originalLength - lastOffset);
    }

    if (operation.baseLength !== originalLength || operation.isNoop()) {
      return;
    }

    sendOp(operation.toJSON());
  });
};

const sendOp = (op) => {
  const msg = {
    clientId: clientId,
    version: currentVersion,
    docId: docId.value,
    op: op
  };
  waitQueue.push(msg);
  if (waitStatus === false && ws && ws.readyState === WebSocket.OPEN) {
    waitStatus = true;
    ws.send(JSON.stringify(msg));
  }
};

const applyRemoteOp = (msg) => {
  isApplyingRemote = true;
  currentVersion = msg.version;

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
  let remoteOp = TextOperation.fromJSON(msg.op);

  for (const localMsg of waitQueue) {
    if (localMsg.clientId < msg.clientId) {
      let localOp = TextOperation.fromJSON(localMsg.op);
      const pair = TextOperation.transform(localOp, remoteOp);
      localMsg.op = pair[0].toJSON();
      remoteOp = pair[1];
    } else {
      let localOp = TextOperation.fromJSON(localMsg.op);
      const pair = TextOperation.transform(remoteOp, localOp);
      remoteOp = pair[0];
      localMsg.op = pair[1].toJSON();
    }
  }

  const edits = [];
  let index = 0;

  remoteOp.ops.forEach(op => {
    if (TextOperation.isRetain(op)) {
      index += op;
    } else if (TextOperation.isInsert(op)) {
      const pos = model.getPositionAt(index);
      edits.push({
        range: new monaco.Range(pos.lineNumber, pos.column, pos.lineNumber, pos.column),
        text: op,
        forceMoveMarkers: false
      });
    } else if (TextOperation.isDelete(op)) {
      const len = Math.abs(op);
      const startPos = model.getPositionAt(index);
      const endPos = model.getPositionAt(index + len);
      edits.push({
        range: new monaco.Range(startPos.lineNumber, startPos.column, endPos.lineNumber, endPos.column),
        text: '',
        forceMoveMarkers: false
      });
      index += len;
    }
  });

  editorInstance.value.executeEdits('remote-sync', edits);
  isApplyingRemote = false;
};

const connectWebSocket = () => {
  const token = localStorage.getItem('token');
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const wsUrl = `${protocol}//${window.location.host}/ws/collaboration?token=${token}`;
  
  ws = new WebSocket(wsUrl);

  ws.onopen = () => {
    ws.send(JSON.stringify({
      docId: docId.value,
      method: 'get_new'
    }));
    editorInstance.value.updateOptions({ readOnly: true });
  };

  ws.onmessage = (e) => {
    const msg = JSON.parse(e.data);

    if (msg.error === 'need_get_new') {
      ws.send(JSON.stringify({ docId: docId.value, method: 'get_new' }));
      return;
    }

    if (msg.method === 'get_new') {
      isApplyingRemote = true;
      currentVersion = msg.version;
      editorInstance.value.setValue(msg.text || '');
      isApplyingRemote = false;
      editorInstance.value.updateOptions({ readOnly: false });
      return;
    }

    if (msg.version <= currentVersion) return;

    if (msg.version > currentVersion + 1) {
      bufferMap.value[msg.version] = msg;
    } else {
      applyRemoteOp(msg);
      while (bufferMap.value[currentVersion + 1] != null) {
        const nextMsg = bufferMap.value[currentVersion + 1];
        delete bufferMap.value[currentVersion + 1];
        applyRemoteOp(nextMsg);
      }
    }
  };

  ws.onerror = (error) => {
    console.error('WebSocket error:', error);
    ElMessage.error('协作连接失败');
  };

  ws.onclose = () => {
    console.log('WebSocket connection closed');
  };
};

let pullInterval = null;

const startPullHistory = () => {
  pullInterval = setInterval(() => {
    const versions = Object.keys(bufferMap.value);
    if (versions.length === 0) return;
    console.warn('检测到版本断档，请求补发...');
    if (!ws || ws.readyState !== WebSocket.OPEN) return;
    ws.send(JSON.stringify({
      docId: docId.value,
      method: 'pull_history',
      version: currentVersion
    }));
  }, 5000);
};

const handleFormat = (type, data) => {
  switch (type) {
    case 'bold':
      insertText('**', '**');
      break;
    case 'italic':
      insertText('*', '*');
      break;
    case 'heading':
      insertText('\n# ', '');
      break;
    case 'link':
      insertText('[', '](url)');
      break;
    case 'code':
      insertText('`', '`');
      break;
    case 'quote':
      insertText('\n> ', '');
      break;
    case 'orderedList':
      insertText('\n1. ', '');
      break;
    case 'unorderedList':
      insertText('\n- ', '');
      break;
    case 'ai':
      if (data) {
        isApplyingRemote = true;
        const fullRange = editorInstance.value.getModel().getFullModelRange();
        editorInstance.value.executeEdits('ai-format', [{
          range: fullRange,
          text: data
        }]);
        isApplyingRemote = false;
      }
      break;
  }
};

const insertText = (before, after) => {
  const editor = editorInstance.value;
  const selection = editor.getSelection();
  const model = editor.getModel();
  const selectedText = model.getValueInRange(selection);
  
  const edit = {
    range: selection,
    text: before + selectedText + after
  };
  
  editor.executeEdits('format', [edit]);
};

const handleExport = (format) => {
  const content = editorInstance.value?.getValue() || '';
  
  switch (format) {
    case 'markdown':
      downloadFile('document.md', content, 'text/markdown');
      break;
    case 'html':
      const html = `<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Document</title>
</head>
<body>
${parseMarkdown(content)}
</body>
</html>`;
      downloadFile('document.html', html, 'text/html');
      break;
    case 'pdf':
      ElMessage.info('PDF导出功能开发中');
      break;
  }
};

const parseMarkdown = (text) => {
  return text
    .replace(/^### (.*$)/gim, '<h3>$1</h3>')
    .replace(/^## (.*$)/gim, '<h2>$1</h2>')
    .replace(/^# (.*$)/gim, '<h1>$1</h1>')
    .replace(/\*\*(.*)\*\*/gim, '<b>$1</b>')
    .replace(/\*(.*)\*/gim, '<i>$1</i>')
    .replace(/\n/gim, '<br>');
};

const downloadFile = (filename, content, type) => {
  const blob = new Blob([content], { type });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
  ElMessage.success(`已导出 ${filename}`);
};

onMounted(() => {
  initEditor();
  connectWebSocket();
  startPullHistory();
});

onUnmounted(() => {
  if (contentChangeDisposable) {
    contentChangeDisposable.dispose();
  }
  if (editorInstance.value) {
    editorInstance.value.dispose();
  }
  if (ws) {
    ws.close();
  }
  if (pullInterval) {
    clearInterval(pullInterval);
  }
});
</script>

<style scoped>
.editor-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #1e1e1e;
}

.editor-container {
  flex: 1;
  overflow: hidden;
}

.monaco-editor {
  height: 100%;
}
</style>
