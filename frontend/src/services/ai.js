import http from '@/http';

export const aiService = {
  async formatMarkdown(docId) {
    const response = await http.post('/api/ai/format', Number(docId), {
      headers: { 'Content-Type': 'application/json' }
    });
    return response.data;
  }
};

export default aiService;
