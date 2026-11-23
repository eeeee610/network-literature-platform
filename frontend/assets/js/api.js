/* 统一请求封装：基址、令牌、错误码、mock 回落、轻提示 */
(function () {
  'use strict';

  const BASE = window.API_BASE || 'http://localhost:8080/api/v1';
  const TOKEN_KEY = 'np_token';
  const MOCK_KEY = 'np_mock';
  const TIMEOUT_MS = 12000;

  class ApiError extends Error {
    constructor(code, message, status) {
      super(message);
      this.name = 'ApiError';
      this.code = code;
      this.status = status || 0;
    }
  }

  let mockActive = new URLSearchParams(location.search).get('mock') === '1'
    || sessionStorage.getItem(MOCK_KEY) === '1';
  let demoMarked = false;

  if (mockActive) sessionStorage.setItem(MOCK_KEY, '1');

  /* ---------- 令牌 ---------- */
  function getToken() {
    return localStorage.getItem(TOKEN_KEY) || '';
  }
  function setToken(token) {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  }
  function clearToken() {
    localStorage.removeItem(TOKEN_KEY);
  }

  /* ---------- mock ---------- */
  function markDemo() {
    mockActive = true;
    if (demoMarked || !document.body) return;
    demoMarked = true;
    const bar = document.createElement('div');
    bar.className = 'demo-banner';
    bar.textContent = '演示数据 · 当前展示本地示例内容';
    document.body.prepend(bar);
  }

  function useMock(method, path, query, body) {
    markDemo();
    if (!window.MockData) {
      return Promise.reject(new ApiError(50000, '演示数据未加载'));
    }
    return Promise.resolve()
      .then(() => window.MockData.handle({ method, path, query: query || {}, body }))
      .catch((err) => {
        if (err instanceof ApiError) throw err;
        throw new ApiError(err.code || 50000, err.message || '演示数据异常', err.status);
      });
  }

  function buildUrl(path, query) {
    const url = BASE.replace(/\/$/, '') + path;
    const params = new URLSearchParams();
    Object.keys(query || {}).forEach((key) => {
      const value = query[key];
      if (value === undefined || value === null || value === '') return;
      params.append(key, String(value));
    });
    const qs = params.toString();
    return qs ? url + '?' + qs : url;
  }

  async function request(path, options) {
    const opts = options || {};
    const method = (opts.method || 'GET').toUpperCase();
    const query = opts.query || {};
    const body = opts.body;

    if (mockActive) return useMock(method, path, query, body);

    const headers = { Accept: 'application/json' };
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    const token = getToken();
    if (token) headers.Authorization = 'Bearer ' + token;

    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);
    let res;
    try {
      res = await fetch(buildUrl(path, query), {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body),
        signal: controller.signal,
      });
    } catch (err) {
      clearTimeout(timer);
      /* 仅网络层失败时回落到演示数据 */
      if (window.MockData) {
        console.info('[墨阅] 后端不可达，已切换到演示数据', err && err.message);
        return useMock(method, path, query, body);
      }
      throw new ApiError(0, '网络连接失败，请确认后端已启动');
    }
    clearTimeout(timer);

    let payload = null;
    try {
      payload = await res.json();
    } catch (err) {
      throw new ApiError(50000, '响应格式异常，无法解析', res.status);
    }

    if (!res.ok) {
      throw new ApiError(
        (payload && payload.code) || res.status,
        (payload && payload.message) || ('请求失败（HTTP ' + res.status + '）'),
        res.status
      );
    }
    if (!payload || payload.code !== 0) {
      throw new ApiError(
        (payload && payload.code) || 50000,
        (payload && payload.message) || '请求失败',
        res.status
      );
    }
    return payload.data;
  }

  /* ---------- 文本与格式化 ---------- */
  function esc(value) {
    return String(value === null || value === undefined ? '' : value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function words(n) {
    const num = Number(n) || 0;
    if (num >= 10000) return (num / 10000).toFixed(1).replace(/\.0$/, '') + ' 万字';
    return num + ' 字';
  }

  function date(value) {
    if (!value) return '—';
    return String(value).slice(0, 10);
  }

  function money(value) {
    const num = Number(value);
    return isNaN(num) ? '0.00' : num.toFixed(2);
  }

  /* 章节标题里若已带「第N章」，展示时去掉，避免与序号重复 */
  function chapterTitle(title) {
    const text = String(title || '').trim();
    const stripped = text.replace(/^第\s*[0-9一二三四五六七八九十百零两]+\s*章[：:\s·、,，]*/, '').trim();
    return stripped || text || '未命名章节';
  }

  /* 站内链接：mock 模式下自动带上 ?mock=1 */
  function url(path, params) {
    const p = new URLSearchParams();
    Object.keys(params || {}).forEach((key) => {
      const value = params[key];
      if (value === undefined || value === null || value === '') return;
      p.append(key, String(value));
    });
    if (mockActive) p.set('mock', '1');
    const qs = p.toString();
    return path + (qs ? '?' + qs : '');
  }

  /* ---------- 轻提示 ---------- */
  function toast(message, type) {
    let root = document.querySelector('.toast-root');
    if (!root) {
      root = document.createElement('div');
      root.className = 'toast-root';
      document.body.appendChild(root);
    }
    const item = document.createElement('div');
    item.className = 'toast' + (type ? ' ' + type : '');
    item.textContent = message;
    root.appendChild(item);
    setTimeout(() => item.remove(), 3200);
  }

  /* ---------- 业务方法 ---------- */
  const API = {
    base: BASE,
    ApiError,
    getToken,
    setToken,
    clearToken,
    isMock: () => mockActive,
    esc,
    words,
    date,
    money,
    chapterTitle,
    url,
    toast,

    getTags: () => request('/tags'),

    getNovels: (params) => request('/novels', { query: params }),
    searchNovels: (params) => request('/novels/search', { query: params }),
    getNovel: (novelId) => request('/novels/' + encodeURIComponent(novelId)),
    getChapters: (novelId) => request('/novels/' + encodeURIComponent(novelId) + '/chapters'),
    getChapter: (chapterId) => request('/chapters/' + encodeURIComponent(chapterId)),

    devToken: (userId, role) => request('/dev/token', { query: { userId, role: role || 'AUTHOR' } }),

    getAuthorNovels: (params) => request('/author/novels', { query: params }),
    getAuthorChapters: (novelId) => request('/author/novels/' + encodeURIComponent(novelId) + '/chapters'),
    createNovel: (payload) => request('/novels', { method: 'POST', body: payload }),
    createChapter: (novelId, payload) => request('/novels/' + encodeURIComponent(novelId) + '/chapters', { method: 'POST', body: payload }),
    updateChapter: (chapterId, payload) => request('/chapters/' + encodeURIComponent(chapterId), { method: 'PUT', body: payload }),
    publishChapter: (chapterId) => request('/chapters/' + encodeURIComponent(chapterId) + '/publish', { method: 'POST' }),
    offlineChapter: (chapterId) => request('/chapters/' + encodeURIComponent(chapterId) + '/offline', { method: 'POST' }),
  };

  window.API = API;

  if (mockActive && document.body) markDemo();
  document.addEventListener('DOMContentLoaded', () => { if (mockActive) markDemo(); });
})();
