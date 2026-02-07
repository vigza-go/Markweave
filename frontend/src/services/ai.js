import http from '@/http';

export const aiService = {
  async formatMarkdown(docId) {
    const response = await http.post('/api/ai/format', null, {
      params: { docId }
    });
    return response.data;
  }
};

export default aiService;
