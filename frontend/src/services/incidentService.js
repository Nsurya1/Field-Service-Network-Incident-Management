import api from './api';

export const incidentService = {
  async getStats() {
    const response = await api.get('/incidents/stats');
    return response.data;
  },

  async getIncidents(status, priority, page = 0, size = 10) {
    const params = { page, size };
    if (status) params.status = status;
    if (priority) params.priority = priority;

    const response = await api.get('/incidents', { params });
    return response.data;
  },

  async getIncidentById(id) {
    const response = await api.get(`/incidents/${id}`);
    return response.data;
  },

  async createIncident(data) {
    const response = await api.post('/incidents', data);
    return response.data;
  },

  async updateStatus(id, newStatus) {
    const response = await api.patch(`/incidents/${id}/status`, { status: newStatus });
    return response.data;
  },

  async assignIncident(id, engineerId) {
    const response = await api.patch(`/incidents/${id}/assign`, { engineerId });
    return response.data;
  },

  async getHistory(id) {
    const response = await api.get(`/incidents/${id}/history`);
    return response.data;
  },

  async getVisits(id) {
    const response = await api.get(`/incidents/${id}/visits`);
    return response.data;
  },

  async createVisit(id, visitData) {
    const response = await api.post(`/incidents/${id}/visits`, visitData);
    return response.data;
  },
};
