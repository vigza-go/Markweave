// 该js用于测试该项目的ot算法是否正确

import { TextOperation } from '@/js/common.js';

/**
 * 模拟环境配置
 */
const CONFIG = {
    CLIENT_COUNT: 10,       // 模拟客户端数量
    OPERATIONS_PER_CLIENT: 100, // 每个客户端产生的操作数
    MAX_DELAY: 150         // 增加延迟，更容易触发版本乱序
};

class NetworkBus {
    constructor() {
        this.clients = [];
    }
    register(client) {
        this.clients.push(client);
    }
    broadcast(msg) {
        this.clients.forEach(client => {
            // 模拟随机乱序：随机延迟 0~MAX_DELAY
            const delay = Math.random() * CONFIG.MAX_DELAY;
            setTimeout(() => {
                client.onMessage(JSON.parse(JSON.stringify(msg)));
            }, delay);
        });
    }
    sendToServer(server, msg) {
        const delay = Math.random() * CONFIG.MAX_DELAY;
        setTimeout(() => {
            server.handleReceive(msg);
        }, delay);
    }
}

class MockServer {
    constructor(initialText, networkBus) {
        this.fullText = initialText;
        this.currentVersion = 0;
        this.history = [];
        this.networkBus = networkBus;
    }

    handleReceive(clientMsg) {
        let { clientId, version: clientVer, op: opJSON } = clientMsg;
        let clientOp = TextOperation.fromJSON(opJSON);
        console.log("server version", this.currentVersion, ":handleReceive:", clientMsg)

        if (clientVer < this.currentVersion) {
            for (let i = clientVer; i < this.currentVersion; i++) {
                if(this.history[i].clientId < clientId){
                    const historyOp = TextOperation.fromJSON(this.history[i].op);
                    const pair = TextOperation.transform(historyOp, clientOp);
                    clientOp = pair[1];
                }else{
                    const historyOp = TextOperation.fromJSON(this.history[i].op);
                    const pair = TextOperation.transform(clientOp, historyOp);
                    clientOp = pair[0];
                }
            }
        }
        console.log("server:handleReceive:transform:", clientOp)

        this.fullText = clientOp.apply(this.fullText);
        this.currentVersion++;

        const broadcastMsg = {
            clientId,
            version: this.currentVersion,
            op: clientOp.toJSON(),
            method: 'sync'
        };

        this.history.push(broadcastMsg);
        this.networkBus.broadcast(broadcastMsg);
    }
}

class MockClient {
    constructor(id, initialText, server, networkBus) {
        this.id = id;
        this.server = server;
        this.networkBus = networkBus;
        this.localText = initialText;
        this.version = 0;

        this.waitQueue = [];
        this.waitStatus = false;
        // 新增：缓冲槽，处理乱序到达的消息
        this.bufferMap = {};
    }

    generateRandomOp() {
        const op = new TextOperation();
        const textLen = this.localText.length;
        const pos = Math.floor(Math.random() * (textLen));

        if (pos > 0) op.retain(pos);

        if (Math.random() < 0.5 || textLen === 0) {
            const str = Math.random().toString(36).slice(2, 4);
            op.insert(str);
            if (textLen - pos > 0) op.retain(textLen - pos);
        } else {
            const delLen = Math.floor(Math.random() * Math.min(2, textLen - pos)) + 1;
            op.delete(delLen);
            if (textLen - pos - delLen > 0) op.retain(textLen - pos - delLen);
        }

        this.localText = op.apply(this.localText);

        this.waitQueue.push({
            clientId: this.id,
            version: this.version,
            op: op.toJSON()
        });

        this.trySendNext();
    }

    trySendNext() {
        if (!this.waitStatus && this.waitQueue.length > 0) {
            this.waitStatus = true;
            const msg = this.waitQueue[0];
            msg.version = this.version;
            console.log("clientId:", this.id, ",currentVersion:", this.version, ",send msg:", msg)
            this.networkBus.sendToServer(this.server, JSON.parse(JSON.stringify(msg)));
        }
    }

    // 封装原本的 applyRemoteOp 逻辑
    applyRemoteOp(msg) {
        console.log("clientId:", this.id, ",currentVersion:", this.version, ",Applying op version:", msg.version);

        if (msg.clientId === this.id) {
            this.waitQueue.shift();
            this.version = msg.version;
            this.waitStatus = false;
            this.trySendNext();
        } else {
            let remoteOp = TextOperation.fromJSON(msg.op);
            for (let i = 0; i < this.waitQueue.length; i++) {
                if(this.id < msg.clientId){
                    const localOp = TextOperation.fromJSON(this.waitQueue[i].op);
                    const pair = TextOperation.transform(localOp, remoteOp);
                    this.waitQueue[i].op = pair[0].toJSON();
                    remoteOp = pair[1];
                }else {
                    const localOp = TextOperation.fromJSON(this.waitQueue[i].op);
                    const pair = TextOperation.transform(remoteOp,localOp);
                    this.waitQueue[i].op = pair[1].toJSON();
                    remoteOp = pair[0];
                }
            }
            this.localText = remoteOp.apply(this.localText);
            this.version = msg.version;
        }
    }

    onMessage(msg) {
        console.log("clientId:", this.id, ",currentVersion:", this.version, ",receive msg:", msg);

        // 1. 如果收到的版本已经处理过了，直接丢弃
        if (msg.version <= this.version) {
            console.log(`clientId: ${this.id} 忽略旧版本: ${msg.version}`);
            return;
        }

        // 2. 如果版本号大于当前版本+1，说明中间有消息还没到，进入缓存
        if (msg.version > this.version + 1) {
            console.warn(`clientId: ${this.id} 检测到版本断档！当前:${this.version}, 收到:${msg.version}。进入缓存。`);
            this.bufferMap[msg.version] = msg;
        } else {
            // 3. 版本号正好是下一个，直接应用
            this.applyRemoteOp(msg);

            // 4. 检查缓存中是否有后续版本可以连带应用
            while (this.bufferMap[this.version + 1] != null) {
                const nextMsg = this.bufferMap[this.version + 1];
                console.log(`clientId: ${this.id} 从缓存中提取并应用版本: ${nextMsg.version}`);
                delete this.bufferMap[this.version + 1];
                this.applyRemoteOp(nextMsg);
            }
        }
    }
}

async function runTest() {
    console.log("🚀 启动协同编辑压力测试 (带 BufferMap 逻辑)...");
    const initialText = "Start";
    const bus = new NetworkBus();
    const server = new MockServer(initialText, bus);
    const clients = [];

    for (let i = 0; i < CONFIG.CLIENT_COUNT; i++) {
        const client = new MockClient(`Client-${i}`, initialText, server, bus);
        clients.push(client);
        bus.register(client);
    }

    const tasks = clients.map(c => {
        return new Promise(resolve => {
            let count = 0;
            const interval = setInterval(() => {
                c.generateRandomOp();
                count++;
                if (count >= CONFIG.OPERATIONS_PER_CLIENT) {
                    clearInterval(interval);
                    resolve();
                }
            }, 100);
        });
    });

    await Promise.all(tasks);

    console.log("⏳ 操作生成完毕，等待网络同步...");

    // 2. 精确等待所有客户端版本与服务器版本一致
    const maxWaitTime = 10000; // 最大等待10秒
    const startTime = Date.now();

    const isAllSynced = () => {
        return clients.every(c => c.version === server.currentVersion) &&
            clients.every(c => c.waitQueue.length === 0);
    };

    while (!isAllSynced() && Date.now() - startTime < maxWaitTime) {
        await new Promise(r => setTimeout(r, 10)); // 每10ms检查一次
    }

    if (Date.now() - startTime >= maxWaitTime) {
        console.warn("⚠️ 等待超时，强制检查");
    }

    console.log("\n==============================");
    console.log(`服务器最终状态: [${server.fullText}] (版本: ${server.currentVersion})`);

    console.log("\n==============================");
    console.log(`服务器最终状态: [${server.fullText}] (版本: ${server.currentVersion})`);

    let success = true;
    clients.forEach(c => {
        const isMatch = c.localText === server.fullText;
        console.log(`${c.id}: ${isMatch ? '✅' : '❌'} 内容: [${c.localText}] 版本: ${c.version}`);
        if (!isMatch) success = false;
    });

    if (success) {
        console.log("\n🎉 测试通过！最终一致性达成。");
    } else {
        console.log("\n🚨 测试失败！BufferMap 或 OT 变换逻辑有误。");
    }
    return success;
}

let count = 0
while (1) {
    count++;
    console.log("\n\n====== 第", count, "次测试 ======")
    const success = await runTest();
    if (!success) {
        break
    }
};