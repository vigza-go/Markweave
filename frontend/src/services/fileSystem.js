import http from '@/http';

export const fileSystemService = {
  async createFile(fileName, faId, fileType) {
    const response = await http.post('/api/fs/file/create', {
      fileName,
      faId,
      fileType
    });
    return response.data;
  },

  async createShortcut(faId, srcNodeId) {
    const response = await http.post('/api/fs/file/shortcut/create', {
      faId,
      srcNodeId
    });
    return response.data;
  },

  async rename(nodeId, newName) {
    const response = await http.post('/api/fs/file/rename', {
      nodeId,
      newName
    });
    return response.data;
  },

  async move(nodeId, targetFolderId) {
    const response = await http.post('/api/fs/file/move', {
      nodeId,
      targetFolderId
    });
    return response.data;
  },

  async recycle(nodeId) {
    const response = await http.post('/api/fs/file/recycle', {
      nodeId
    }, {
      params: { nodeId }
    });
    return response.data;
  },

  async listFiles(faId) {
    const response = await http.get(`/api/fs/files/${faId}/list`);
    return response.data;
  },

  async updateViewTime(nodeId) {
    const response = await http.post(`/api/fs/file/viewtime/update?nodeId=${nodeId}`);
    return response.data;
  },

  async getRecentDocs() {
    const response = await http.get('/api/fs/docs/recent');
    return response.data;
  },

  async getRecycledFiles() {
    const response = await http.get('/api/fs/files/recycled/list');
    return response.data;
  },

  async restoreFile(nodeId) {
    const response = await http.post('/api/fs/file/restore', {
      nodeId
    });
    return response.data;
  },

  async permanentlyDelete(nodeId) {
    const response = await http.post('/api/fs/file/delete/permanent', {
      nodeId
    });
    return response.data;
  }
};

export default fileSystemService;
