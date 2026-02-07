export const websocketService = {
  ws: null,
  reconnectAttempts: 0,
  maxReconnectAttempts: 5,
  reconnectInterval: 3000,
  messageHandlers: new Map(),
  docId: null,
  clientId: null,

  connect(docId, onMessage, onOpen, onClose, onError) {
    this.docId = docId;
    this.clientId = 'user_' + Math.random().toString(16).slice(2);
    
    const token = localStorage.getItem('token');
    const wsUrl = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws/collaboration?token=${token}`;
    
    this.ws = new WebSocket(wsUrl);

    this.ws.onopen = (event) => {
      console.log('WebSocket connected');
      this.reconnectAttempts = 0;
      
      this.ws.send(JSON.stringify({
        docId: this.docId,
        method: 'get_new'
      }));
      
      if (onOpen) onOpen(event);
    };

    this.ws.onmessage = (event) => {
      const msg = JSON.parse(event.data);
      
      if (msg.method === 'get_new') {
        if (onMessage) onMessage(msg);
        return;
      }
      
      if (onMessage) onMessage(msg);
    };

    this.ws.onclose = (event) => {
      console.log('WebSocket disconnected');
      if (onClose) onClose(event);
      
      this.attemptReconnect(onMessage, onOpen, onClose, onError);
    };

    this.ws.onerror = (error) => {
      console.error('WebSocket error:', error);
      if (onError) onError(error);
    };
  },

  attemptReconnect(onMessage, onOpen, onClose, onError) {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('Max reconnection attempts reached');
      return;
    }

    this.reconnectAttempts++;
    console.log(`Attempting to reconnect... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);

    setTimeout(() => {
      this.connect(this.docId, onMessage, onOpen, onClose, onError);
    }, this.reconnectInterval);
  },

  send(message) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    } else {
      console.error('WebSocket is not connected');
    }
  },

  sendOperation(operation) {
    const msg = {
      clientId: this.clientId,
      docId: this.docId,
      op: operation
    };
    this.send(msg);
  },

  requestHistory(version) {
    this.send({
      method: 'pull_history',
      version: version
    });
  },

  disconnect() {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  },

  isConnected() {
    return this.ws && this.ws.readyState === WebSocket.OPEN;
  }
};

export default websocketService;
