import api from './api';

export const engineerService = {
  async getEngineers() {
    const response = await api.get('/engineers');
    return response.data;
  },

  async getEngineerById(id) {
    const response = await api.get(`/engineers/${id}`);
    return response.data;
  },

  async createEngineer(data) {
    const response = await api.post('/engineers', data);
    return response.data;
  },
};
