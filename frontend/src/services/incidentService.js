import api from './api';

export const incidentService = {
  async getStats() {
    const response = await api.get('/incidents/stats');
    return response.data;
  },

  async getIncidents(filters = {}, priority = null, page = 0, size = 10, sort = 'createdAt,desc') {
    let params = { page, size, sort };

    if (typeof filters === 'string') {
      // Legacy argument signature: (status, priority, page, size)
      if (filters) params.status = filters;
      if (priority) params.priority = priority;
    } else if (filters && typeof filters === 'object') {
      if (filters.search) params.search = filters.search;
      if (filters.status) params.status = filters.status;
      if (filters.priority) params.priority = filters.priority;
      if (filters.assignedEngineerId) params.assignedEngineerId = filters.assignedEngineerId;
      if (filters.createdFrom) params.createdFrom = filters.createdFrom;
      if (filters.createdTo) params.createdTo = filters.createdTo;
      if (filters.breachedOnly !== undefined && filters.breachedOnly !== null && filters.breachedOnly !== '') {
        params.breachedOnly = filters.breachedOnly;
      }
      if (filters.sort) params.sort = filters.sort;
      if (filters.page !== undefined) params.page = filters.page;
      if (filters.size !== undefined) params.size = filters.size;
    }

    const response = await api.get('/incidents', { params });
    return response.data;
  },

  async updateIncident(id, data) {
    const response = await api.put(`/incidents/${id}`, data);
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

  async getComments(id) {
    const response = await api.get(`/incidents/${id}/comments`);
    return response.data;
  },

  async addComment(id, text) {
    const response = await api.post(`/incidents/${id}/comments`, { text });
    return response.data;
  },

  async getTimeline(id) {
    const response = await api.get(`/incidents/${id}/timeline`);
    return response.data;
  },
};
