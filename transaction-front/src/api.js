const API = import.meta.env.VITE_API_URL || 'http://localhost:8081';
let accessToken = null;


export function setAccessToken(token) {
  accessToken = token;
}


export function clearAccessToken() {
  accessToken = null;
}


function problemMessage(body, status) {
  if (body?.errors && typeof body.errors === 'object') {
    const messages = Object.values(body.errors).filter(Boolean);
    if (messages.length > 0) {
      return messages.join(' · ');
    }
  }
  return body?.detail || body?.message || body?.title || `HTTP ${status}`;
}


async function request(path, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
    ...(options.headers || {})
  };


  const response = await fetch(`${API}${path}`, {
    ...options,
    headers
  });


  const contentType = response.headers.get('content-type') || '';
  const body = contentType.includes('application/json')
    ? await response.json().catch(() => ({}))
    : {};


  if (!response.ok) {
    if (response.status === 401) {
      clearAccessToken();
    }
    throw new Error(problemMessage(body, response.status));
  }


  return body;
}


export const login = (data) =>
  request('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(data)
  });


export const createTransaction = (data) =>
  request('/api/transactions', {
    method: 'POST',
    body: JSON.stringify(data)
  });


export const cancelTransaction = (data) =>
  request('/api/transactions/status', {
    method: 'PATCH',
    body: JSON.stringify(data)
  });


export function listTransactions(page = 0, size = 10, sort = 'id,desc') {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
    sort
  });
  return request(`/api/transactions?${params.toString()}`);
}