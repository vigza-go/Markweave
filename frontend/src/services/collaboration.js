import http from '@/http';

export const collaborationService = {
  async createInvitation(docId, permission, expTime) {
    const response = await http.post('/api/collaboration/invite', {
      docId,
      permission,
      expTime
    });
    return response.data;
  },

  async acceptInvitation(invToken) {
    const response = await http.post('/api/collaboration/invite/accept', invToken, {
      headers: { 'Content-Type': 'text/plain' }
    });
    return response.data;
  },

  async updatePermission(docId, targetUserId, permission) {
    const response = await http.post('/api/collaboration/permission', {
      docId,
      targetUserId,
      permission
    });
    return response.data;
  },

  async getCollaborators(docId) {
    const response = await http.get(`/api/collaboration/docs/${docId}/collaborators`);
    return response.data;
  }
};

export default collaborationService;
