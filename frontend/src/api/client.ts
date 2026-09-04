import axios from 'axios';
import { AuthResponse, Batch, BatchMetrics, ChatMessage, MatchResult, AuditLogEntry } from '../types';

let inMemoryAccessToken: string | null = null;
let inMemoryRefreshToken: string | null = null;

export const setTokens = (accessToken: string | null, refreshToken: string | null = null) => {
  inMemoryAccessToken = accessToken;
  if (refreshToken) inMemoryRefreshToken = refreshToken;
};

export const getAccessToken = () => inMemoryAccessToken;

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Attach Authorization header to every outgoing request
api.interceptors.request.use((config) => {
  if (inMemoryAccessToken) {
    config.headers.Authorization = `Bearer ${inMemoryAccessToken}`;
  }
  return config;
});

// Auto-refresh token interceptor on 401
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry && inMemoryRefreshToken) {
      originalRequest._retry = true;
      try {
        const refreshResponse = await axios.post<AuthResponse>('/api/auth/refresh', {
          refreshToken: inMemoryRefreshToken,
        });
        const { accessToken, refreshToken } = refreshResponse.data;
        setTokens(accessToken, refreshToken);
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return api(originalRequest);
      } catch (refreshErr) {
        setTokens(null, null);
        window.dispatchEvent(new Event('auth:logout'));
        return Promise.reject(refreshErr);
      }
    }
    return Promise.reject(error);
  }
);

export const authApi = {
  login: async (usernameOrEmail: string, password: string): Promise<AuthResponse> => {
    const res = await api.post<AuthResponse>('/auth/login', { usernameOrEmail, password });
    setTokens(res.data.accessToken, res.data.refreshToken);
    return res.data;
  },
  signup: async (username: string, email: string, password: string, role: string): Promise<AuthResponse> => {
    const res = await api.post<AuthResponse>('/auth/signup', { username, email, password, role });
    setTokens(res.data.accessToken, res.data.refreshToken);
    return res.data;
  },
  getCurrentUser: async () => {
    const res = await api.get('/auth/me');
    return res.data;
  },
  logout: () => {
    setTokens(null, null);
  },
};

export const batchApi = {
  list: async (): Promise<Batch[]> => {
    const res = await api.get<Batch[]>('/batches');
    return res.data;
  },
  get: async (batchId: number): Promise<Batch> => {
    const res = await api.get<Batch>(`/batches/${batchId}`);
    return res.data;
  },
  create: async (batchName?: string): Promise<Batch> => {
    const res = await api.post<Batch>('/batches', { batchName });
    return res.data;
  },
  delete: async (batchId: number): Promise<void> => {
    await api.delete(`/batches/${batchId}`);
  },
  uploadCsv: async (batchId: number, type: 'gateway' | 'bank' | 'ledger', file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    const res = await api.post(`/batches/${batchId}/upload/${type}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data;
  },
};

export const reconciliationApi = {
  runRulesPass: async (batchId: number) => {
    const res = await api.post(`/batches/${batchId}/reconcile/rules`);
    return res.data;
  },
  runAiPass: async (batchId: number) => {
    const res = await api.post(`/batches/${batchId}/reconcile/ai`);
    return res.data;
  },
  getRecords: async (batchId: number, status?: string, search?: string): Promise<MatchResult[]> => {
    const params = new URLSearchParams();
    if (status && status !== 'ALL') params.append('status', status);
    if (search && search.trim()) params.append('search', search.trim());
    const res = await api.get<MatchResult[]>(`/batches/${batchId}/records?${params.toString()}`);
    return res.data;
  },
  overrideManualReview: async (batchId: number, matchResultId: number, status: 'MATCHED' | 'DISCREPANCY', reasoning: string) => {
    const res = await api.post<MatchResult>(`/batches/${batchId}/manual-review/${matchResultId}`, {
      status,
      reasoning,
    });
    return res.data;
  },
};

export const metricsApi = {
  getMetrics: async (batchId: number): Promise<BatchMetrics> => {
    const res = await api.get<BatchMetrics>(`/batches/${batchId}/metrics`);
    return res.data;
  },
  getAuditTrail: async (batchId: number): Promise<AuditLogEntry[]> => {
    const res = await api.get<AuditLogEntry[]>(`/batches/${batchId}/audit`);
    return res.data;
  },
  downloadReport: async (batchId: number, batchName: string) => {
    const res = await api.get(`/batches/${batchId}/report`, { responseType: 'blob' });
    const url = window.URL.createObjectURL(new Blob([res.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `Reconciliation_Report_${batchName.replace(/\s+/g, '_')}_${batchId}.xlsx`);
    document.body.appendChild(link);
    link.click();
    link.remove();
  },
};

export const chatApi = {
  sendMessage: async (batchId: number, message: string): Promise<{ answer: string; modelUsed: string; confidence: number }> => {
    const res = await api.post(`/batches/${batchId}/chat`, { message });
    return res.data;
  },
  getHistory: async (batchId: number): Promise<ChatMessage[]> => {
    const res = await api.get<ChatMessage[]>(`/batches/${batchId}/chat/history`);
    return res.data;
  },
};

export default api;
