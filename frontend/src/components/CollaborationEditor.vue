<script setup>
import { ref, onMounted, onUnmounted, shallowRef } from 'vue'
import { Queue, TextOperation } from '../js/common.js'

import * as monaco from 'monaco-editor'

const props = defineProps({
  docId: {
    type: String,
    default: '123'
  },
  wsUrl: {
    type: String,
    default: 'ws://localhost:8080/ws/collaboration'
  }
})

const version = ref(0)
const editorContainer = ref(null)
const editorInstance = shallowRef(null)

const clientId = 'user_' + Math.random().toString(16).slice(2)
let currentVersion = 0

let ws = null

// 停等队列（我遇到了一个问题，在共同的3版本下，如果b发送操作B1,a发送操作A1,A2，服务器收到的是，B1，A1,版本更迭为4，5，但是对于A2操作，在变幻时应当跳过5。所以A2操作需要在前端transform，并等待A1版本号）
let waitStatus = false
const waitQueue = new Queue(1024)
// 缓冲槽
const bufferMap = ref({})
// 防止收到的广播的变化也被当做操作发出
let isApplyingRemote = false


const sendOp = (op) => {
  const msg = {
    clientId: clientId,
    version: currentVersion,
    docId : props.docId,
    op: op
  }
  waitQueue.push(msg);
  if (waitStatus == false) {
    waitStatus = true;
    ws.send(JSON.stringify(msg));
  }
}

const initEditor = () => {

  editorInstance.value = monaco.editor.create(editorContainer.value, {
    value: '',
    language: 'markdown',
    theme: 'vs-dark',
    automaticLayout: true,
    fontSize: 16
  })

  editorInstance.value.onDidChangeModelContent((e) => {
    // 如果是由于远程同步导致的变更，直接跳过，避免无限循环
    if (isApplyingRemote) return;

    const model = editorInstance.value.getModel();
    const operation = new TextOperation();

    // 1. 获取变更前的文档总长度
    // 因为 onDidChangeModelContent 触发时，model 已经是更新后的了
    // 我们需要通过计算还原出变更前的长度
    let currentLength = model.getValueLength();
    let delta = 0;
    e.changes.forEach(c => {
      delta += (c.text.length - c.rangeLength);
    });
    const originalLength = currentLength - delta;

    // 2. 对 changes 按偏移量(rangeOffset)进行升序排序
    // Monaco 的 changes 有时是无序的，OT 要求按文档顺序处理
    const sortedChanges = [...e.changes].sort((a, b) => a.rangeOffset - b.rangeOffset);

    let lastOffset = 0;

    // 3. 遍历变更，构建 OT 操作
    sortedChanges.forEach((change) => {
      const { rangeOffset, rangeLength, text } = change;

      // 保留(retain)从上一个偏移量到当前变更位置之间的字符
      const skip = rangeOffset - lastOffset;
      if (skip > 0) {
        operation.retain(skip);
      }

      // 处理删除(delete)
      if (rangeLength > 0) {
        operation.delete(rangeLength);
      }

      // 处理插入(insert)
      if (text !== "") {
        operation.insert(text);
      }

      // 更新游标位置（注意：在原始字符串上的位置）
      lastOffset = rangeOffset + rangeLength;
    });

    // 4. 最后一步：必须 retain 剩余的所有字符，使 baseLength 等于 originalLength
    if (lastOffset < originalLength) {
      operation.retain(originalLength - lastOffset);
    }

    // 验证 baseLength 是否匹配（调试用）
    if (operation.baseLength !== originalLength) {
      console.error("OT BaseLength 校验失败！");
      return;
    }

    // 发送操作到后端 (通常发送 operation.toJSON())
    sendOp(operation.toJSON());
  });
}

const applyRemoteOp = (msg) => {
  isApplyingRemote = true
  currentVersion = msg.version
  version.value = currentVersion

  if (msg.clientId === clientId) {
    waitQueue.pop()
    waitStatus = false
    isApplyingRemote = false

    if (waitQueue.size > 0) {
      waitStatus = true
      // 在没有ack之前不能pop，因为服务器操作后到需要穿过waitqueue
      // 没有ack，说明当前收到的操作，在服务器的视角下是先执行的 
      const nextMsg = waitQueue.getFront()
      nextMsg.version = currentVersion
      ws.send(JSON.stringify(nextMsg))
    }
    return
  }



  const model = editorInstance.value.getModel()

  let remoteOp = new TextOperation.fromJSON(msg.op)
  // 需要把本地操作经过操作链变换，才能发给服务器，同时远程操作也需要本地化变化
  for (const localMsg of waitQueue) {
    if(localMsg.clientId < msg.clientId){
      let localOp = new TextOperation.fromJSON(localMsg.op)
      const pair = TextOperation.transform(localOp, remoteOp)
      localMsg.op = pair[0].toJSON()
      remoteOp = pair[1]
    }else {
      let localOp = new TextOperation.fromJSON(localMsg.op)
      const pair = TextOperation.transform(remoteOp, localOp)
      remoteOp = pair[0]
      localMsg.op = pair[1].toJSON()
    }
  }
  console.log("remoteOp :", remoteOp)
  const edits = [];
  let index = 0; // 这里的 index 对应文档在应用该操作前的偏移量

  remoteOp.ops.forEach(op => {
    if (TextOperation.isRetain(op)) {
      // Retain: 仅仅移动指针，不产生编辑动作
      index += op;
    } else if (TextOperation.isInsert(op)) {
      // Insert: 在当前指针位置插入文本
      const pos = model.getPositionAt(index);
      edits.push({
        range: new monaco.Range(
          pos.lineNumber,
          pos.column,
          pos.lineNumber,
          pos.column
        ),
        text: op,
        forceMoveMarkers: false
      });
      // 注意：OT 的插入不消耗 baseLength 的 index，所以 index 不增加
    } else if (TextOperation.isDelete(op)) {
      // Delete: 从当前指针位置开始，删除后续 |op| 个字符
      const len = Math.abs(op);
      const startPos = model.getPositionAt(index);
      const endPos = model.getPositionAt(index + len);

      edits.push({
        range: new monaco.Range(
          startPos.lineNumber,
          startPos.column,
          endPos.lineNumber,
          endPos.column
        ),
        text: '',
        forceMoveMarkers: false
      });
      // 删除消耗了原始文档的字符，指针增加
      index += len;
    }
  });
  editorInstance.value.executeEdits('remote-sync', edits);
  isApplyingRemote = false
}


const connectWebSocket = () => {
  ws = new WebSocket(props.wsUrl)

  ws.onopen = () => {
    ws.send(JSON.stringify({
      docId: props.docId,
      method: 'get_new'
    }))
    editorInstance.value.updateOptions({ readOnly: true })
  }

  ws.onmessage = (e) => {
    const msg = JSON.parse(e.data)

    if (msg.method === 'get_new') {
      //上锁，不要载入文本时当用户操作
      isApplyingRemote = true
      currentVersion = msg.version
      version.value = currentVersion
      editorInstance.value.setValue(msg.text)
      isApplyingRemote = false
      editorInstance.value.updateOptions({ readOnly: false })
      return
    }

    if (msg.version <= currentVersion) return

    if (msg.version > currentVersion + 1) {
      bufferMap.value[msg.version] = msg
    } else {
      applyRemoteOp(msg)
      while (bufferMap.value[currentVersion + 1] != null) {
        const nextMsg = bufferMap.value[currentVersion + 1]
        delete bufferMap.value[currentVersion + 1]
        applyRemoteOp(nextMsg)
      }
    }
  }

  ws.onerror = (error) => {
    console.error('WebSocket error:', error)
  }

  ws.onclose = () => {
    console.log('WebSocket connection closed')
  }
}

let pullInterval = null

const startPullHistory = () => {
  pullInterval = setInterval(() => {
    const versions = Object.keys(bufferMap.value)
    if (versions.length === 0) return
    console.warn('检测到版本断档，请求补发...')
    ws.send(JSON.stringify({
      method: 'pull_history',
      version: currentVersion
    }))
  }, 5000)
}

onMounted(async () => {
  initEditor()
  connectWebSocket()
  startPullHistory()
})

onUnmounted(() => {
  if (editorInstance.value) {
    editorInstance.value.dispose()
  }
  if (ws) {
    ws.close()
  }
  if (pullInterval) {
    clearInterval(pullInterval)
  }
})
</script>



<template>
  <div class="editor-wrapper">
    <div class="toolbar">版本: {{ version }}</div>
    <div ref="editorContainer" class="monaco-box"></div>
  </div>
</template>

<style scoped>
.editor-wrapper {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.monaco-box {
  flex: 1;
  border-top: 1px solid #333;
}

.toolbar {
  padding: 8px;
  background: #1e1e1e;
  color: #fff;
}
</style>