import api from './api';

export const reportService = {
  async getSummary(from, to) {
    const params = {};
    if (from) params.from = from;
    if (to) params.to = to;
    const response = await api.get('/reports/summary', { params });
    return response.data;
  },

  async exportCsv(from, to) {
    const params = {};
    if (from) params.from = from;
    if (to) params.to = to;
    const response = await api.get('/reports/export/csv', {
      params,
      responseType: 'blob',
    });

    const blob = new Blob([response.data], { type: 'text/csv;charset=utf-8;' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `incident-report-${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },
};
