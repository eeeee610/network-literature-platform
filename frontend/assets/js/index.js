/* 书城首页：列表、标签、状态、排序、搜索、分页 */
(function () {
  'use strict';

  const PAGE_SIZE = 12;
  const state = { keyword: '', tagId: '', status: '', sort: 'update', page: 1 };
  let seq = 0;

  const el = {
    searchForm: document.getElementById('searchForm'),
    keyword: document.getElementById('keyword'),
    tagSelect: document.getElementById('tagSelect'),
    statusGroup: document.getElementById('statusGroup'),
    sortSelect: document.getElementById('sortSelect'),
    resetBtn: document.getElementById('resetBtn'),
    listArea: document.getElementById('listArea'),
    pager: document.getElementById('pager'),
    resultInfo: document.getElementById('resultInfo'),
  };

  function init() {
    const fromUrl = new URLSearchParams(location.search).get('keyword');
    if (fromUrl) {
      state.keyword = fromUrl.trim();
      el.keyword.value = state.keyword;
    }
    bindEvents();
    loadTags();
    load();
  }

  function bindEvents() {
    el.searchForm.addEventListener('submit', (event) => {
      event.preventDefault();
      state.keyword = el.keyword.value.trim();
      state.page = 1;
      load();
    });

    el.tagSelect.addEventListener('change', () => {
      state.tagId = el.tagSelect.value;
      state.page = 1;
      load();
    });

    el.sortSelect.addEventListener('change', () => {
      state.sort = el.sortSelect.value;
      state.page = 1;
      load();
    });

    el.statusGroup.addEventListener('click', (event) => {
      const btn = event.target.closest('button[data-status]');
      if (!btn) return;
      state.status = btn.dataset.status;
      setPressed(btn);
      state.page = 1;
      load();
    });

    el.resetBtn.addEventListener('click', () => {
      state.keyword = '';
      state.tagId = '';
      state.status = '';
      state.sort = 'update';
      state.page = 1;
      el.keyword.value = '';
      el.tagSelect.value = '';
      el.sortSelect.value = 'update';
      setPressed(el.statusGroup.querySelector('button[data-status=""]'));
      load();
    });

    el.pager.addEventListener('click', (event) => {
      const btn = event.target.closest('button[data-page]');
      if (!btn || btn.disabled) return;
      const page = Number(btn.dataset.page);
      if (!page || page === state.page) return;
      state.page = page;
      load();
      window.scrollTo({ top: 0, behavior: 'smooth' });
    });

    el.listArea.addEventListener('click', (event) => {
      const action = event.target.closest('[data-action]');
      if (!action) return;
      if (action.dataset.action === 'retry') load();
      if (action.dataset.action === 'reset') el.resetBtn.click();
    });
  }

  function setPressed(active) {
    el.statusGroup.querySelectorAll('button').forEach((btn) => {
      btn.setAttribute('aria-pressed', String(btn === active));
    });
  }

  async function loadTags() {
    try {
      const list = await API.getTags();
      list.forEach((tag) => {
        if (tag.is_active === 0) return;
        const option = document.createElement('option');
        option.value = tag.tag_id;
        option.textContent = tag.tag_name;
        el.tagSelect.appendChild(option);
      });
    } catch (err) {
      /* 标签加载失败不影响主列表 */
      console.warn('标签加载失败：', err.message);
    }
  }

  async function load() {
    const current = ++seq;
    showSkeleton();
    el.pager.innerHTML = '';
    el.listArea.setAttribute('aria-busy', 'true');

    const params = { page: state.page, page_size: PAGE_SIZE };
    try {
      let data;
      if (state.keyword) {
        params.keyword = state.keyword;
        if (state.tagId) params.tag_id = state.tagId;
        data = await API.searchNovels(params);
      } else {
        if (state.tagId) params.tag_id = state.tagId;
        if (state.status !== '') params.status = state.status;
        params.sort = state.sort;
        data = await API.getNovels(params);
      }
      if (current !== seq) return;
      render(data || { total: 0, list: [] });
    } catch (err) {
      if (current !== seq) return;
      renderError(err);
    } finally {
      if (current === seq) el.listArea.setAttribute('aria-busy', 'false');
    }
  }

  function showSkeleton() {
    el.resultInfo.textContent = '正在整理书架…';
    let html = '<div class="skeleton-grid">';
    for (let i = 0; i < 8; i += 1) {
      html += '<div class="sk-card"><div class="sk sk-cover"></div><div class="sk sk-line"></div><div class="sk sk-line short"></div></div>';
    }
    el.listArea.innerHTML = html + '</div>';
  }

  function render(data) {
    const list = Array.isArray(data.list) ? data.list : [];
    const total = Number(data.total) || 0;

    let info = '共 ' + total + ' 部作品';
    if (state.keyword) info = '搜索「' + state.keyword + '」 · ' + info + (state.tagId ? ' · 已筛选标签' : '');
    else if (state.tagId) info += ' · 已按标签筛选';
    el.resultInfo.textContent = info;

    if (!list.length) {
      el.listArea.innerHTML = emptyState();
      el.pager.innerHTML = '';
      return;
    }

    el.listArea.innerHTML = '<div class="book-grid">' + list.map(cardHtml).join('') + '</div>';
    renderPager(total);
  }

  function cardHtml(item) {
    const title = item.title || '未命名作品';
    const cover = 'cover--' + ((Number(item.novel_id) || 1) % 6 + 1);
    const badges = [];
    if (item.status === 0) badges.push('<span class="badge serial">连载中</span>');
    if (item.status === 1) badges.push('<span class="badge done">已完结</span>');
    const tagNames = Array.isArray(item.tag_names) ? item.tag_names : [];
    const tagsHtml = tagNames.map((name) => '<span class="chip">' + API.esc(name) + '</span>').join('');

    return '' +
      '<a class="book-card" href="' + API.esc(API.url('novel.html', { novelId: item.novel_id })) + '">' +
        '<div class="book-cover ' + cover + '"><span class="book-cover-char">' + API.esc(Array.from(title)[0] || '书') + '</span></div>' +
        '<div class="book-card-body">' +
          '<h2 class="book-title">' + API.esc(title) + '</h2>' +
          '<p class="book-author">' + API.esc(item.author_name || '佚名') + '</p>' +
          '<div class="book-meta-row">' + badges.join('') +
            '<span class="book-words">' + API.words(item.total_words) + '</span>' +
          '</div>' +
          (tagsHtml ? '<div class="book-tags">' + tagsHtml + '</div>' : '') +
          '<p class="book-updated">更新于 ' + API.date(item.updated_at) + '</p>' +
        '</div>' +
      '</a>';
  }

  function emptyState() {
    return '<div class="state-box">' +
      '<div class="state-icon">空</div>' +
      '<strong>没有找到作品</strong>' +
      '<p>试试换个关键词，或放宽标签与状态筛选。</p>' +
      '<button type="button" class="btn ghost" data-action="reset">重置筛选</button>' +
      '</div>';
  }

  function renderError(err) {
    el.resultInfo.textContent = '书架暂时没有响应';
    el.listArea.innerHTML = '<div class="state-box">' +
      '<div class="state-icon">!</div>' +
      '<strong>加载失败</strong>' +
      '<p>' + API.esc(err.message || '请稍后重试') + '</p>' +
      '<button type="button" class="btn" data-action="retry">重新加载</button>' +
      '</div>';
  }

  function renderPager(total) {
    const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
    if (totalPages <= 1) {
      el.pager.innerHTML = '';
      return;
    }
    const pages = new Set([1, totalPages, state.page, state.page - 1, state.page + 1]);
    const valid = Array.from(pages).filter((p) => p >= 1 && p <= totalPages).sort((a, b) => a - b);
    let html = '<button type="button" data-page="' + (state.page - 1) + '"' + (state.page <= 1 ? ' disabled' : '') + '>上一页</button>';
    let prev = 0;
    valid.forEach((page) => {
      if (page - prev > 1) html += '<span class="pager-gap">…</span>';
      html += '<button type="button" data-page="' + page + '"' +
        (page === state.page ? ' aria-current="page"' : '') + '>' + page + '</button>';
      prev = page;
    });
    html += '<button type="button" data-page="' + (state.page + 1) + '"' + (state.page >= totalPages ? ' disabled' : '') + '>下一页</button>';
    el.pager.innerHTML = html;
  }

  init();
})();
