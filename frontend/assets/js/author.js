/* 创作台：令牌、作品、标签、章节草稿 / 发布 / 下架 */
(function () {
  'use strict';

  const USER_KEY = 'np_user_id';
  const state = {
    tags: [],
    novels: [],
    chapters: [],
    selectedId: null,
    editingId: null,
    partial: false,
  };

  const el = {
    userId: document.getElementById('userId'),
    devTokenBtn: document.getElementById('devTokenBtn'),
    saveTokenBtn: document.getElementById('saveTokenBtn'),
    tokenInput: document.getElementById('tokenInput'),
    logoutBtn: document.getElementById('logoutBtn'),
    authStatus: document.getElementById('authStatus'),
    loggedOut: document.getElementById('loggedOut'),
    workspace: document.getElementById('workspace'),
    reloadWorksBtn: document.getElementById('reloadWorksBtn'),
    workList: document.getElementById('workList'),
    novelForm: document.getElementById('novelForm'),
    novelTitle: document.getElementById('novelTitle'),
    coverUrl: document.getElementById('coverUrl'),
    novelDesc: document.getElementById('novelDesc'),
    novelTags: document.getElementById('novelTags'),
    tagCounter: document.getElementById('tagCounter'),
    createNovelBtn: document.getElementById('createNovelBtn'),
    selectedNovelHint: document.getElementById('selectedNovelHint'),
    chapterEmpty: document.getElementById('chapterEmpty'),
    chapterLayout: document.getElementById('chapterLayout'),
    chapterList: document.getElementById('chapterList'),
    chapterNote: document.getElementById('chapterNote'),
    chapterForm: document.getElementById('chapterForm'),
    chapterFormTitle: document.getElementById('chapterFormTitle'),
    chapterTitle: document.getElementById('chapterTitle'),
    chapterContent: document.getElementById('chapterContent'),
    chapterVip: document.getElementById('chapterVip'),
    chapterPrice: document.getElementById('chapterPrice'),
    saveChapterBtn: document.getElementById('saveChapterBtn'),
    cancelEditBtn: document.getElementById('cancelEditBtn'),
    chapterFormHint: document.getElementById('chapterFormHint'),
  };

  /* ---------- 身份 ---------- */
  function isLoggedIn() {
    return Boolean(API.getToken());
  }

  function renderAuth() {
    const logged = isLoggedIn();
    const uid = localStorage.getItem(USER_KEY);
    el.workspace.hidden = !logged;
    el.loggedOut.hidden = logged;
    el.logoutBtn.disabled = !logged;
    el.authStatus.className = 'auth-status ' + (logged ? 'is-in' : 'is-out');
    el.authStatus.textContent = logged
      ? (uid ? '已登录 · 用户 ' + uid : '已登录 · 令牌已保存')
      : '未登录';
  }

  async function fetchDevToken() {
    const userId = Number(String(el.userId.value).trim());
    if (!Number.isInteger(userId) || userId <= 0) {
      API.toast('用户 ID 必须是正整数', 'error');
      el.userId.focus();
      return;
    }
    const original = el.devTokenBtn.textContent;
    el.devTokenBtn.disabled = true;
    el.devTokenBtn.textContent = '获取中…';
    try {
      const data = await API.devToken(userId, 'AUTHOR');
      API.setToken(data.token);
      localStorage.setItem(USER_KEY, String(data.user_id || userId));
      API.toast('已获取作者令牌', 'success');
      renderAuth();
      enterWorkspace();
    } catch (err) {
      API.toast(err.message || '获取令牌失败', 'error');
    } finally {
      el.devTokenBtn.disabled = false;
      el.devTokenBtn.textContent = original;
    }
  }

  function saveManualToken() {
    const token = el.tokenInput.value.trim();
    if (!token) {
      API.toast('请先粘贴令牌', 'error');
      el.tokenInput.focus();
      return;
    }
    API.setToken(token);
    API.toast('令牌已保存', 'success');
    renderAuth();
    enterWorkspace();
  }

  function logout(silent) {
    API.clearToken();
    localStorage.removeItem(USER_KEY);
    state.novels = [];
    state.chapters = [];
    state.selectedId = null;
    state.editingId = null;
    el.workList.innerHTML = '';
    el.chapterList.innerHTML = '';
    el.chapterLayout.hidden = true;
    el.chapterEmpty.hidden = false;
    renderAuth();
    if (!silent) API.toast('已退出登录');
  }

  function handleAuthError(err) {
    if (err && err.code === 40101) {
      logout(true);
      API.toast('令牌已失效，请重新获取', 'error');
      return true;
    }
    return false;
  }

  async function enterWorkspace() {
    if (!isLoggedIn()) return;
    await ensureTags();
    await loadWorks();
  }

  /* ---------- 标签 ---------- */
  async function ensureTags() {
    if (state.tags.length) return;
    try {
      state.tags = await API.getTags();
      renderTagPicker();
    } catch (err) {
      el.novelTags.innerHTML = '<span class="field-hint">' + API.esc(err.message) + '</span>';
    }
  }

  function renderTagPicker() {
    const list = state.tags.filter((tag) => tag.is_active !== 0);
    el.novelTags.innerHTML = list.map((tag) =>
      '<label class="tag-option"><input type="checkbox" value="' + tag.tag_id + '"><span>' + API.esc(tag.tag_name) + '</span></label>'
    ).join('');
    updateTagCounter();
  }

  function selectedTagIds() {
    return Array.from(el.novelTags.querySelectorAll('input:checked')).map((input) => Number(input.value));
  }

  function updateTagCounter() {
    const count = selectedTagIds().length;
    el.tagCounter.textContent = '已选 ' + count + ' / 10';
  }

  /* ---------- 作品 ---------- */
  function statusBadge(status) {
    if (status === 0) return '<span class="badge serial">连载中</span>';
    if (status === 1) return '<span class="badge done">已完结</span>';
    return '<span class="badge offline">已下架</span>';
  }

  function auditBadge(audit) {
    if (audit === 0) return '<span class="badge audit-0">待审核</span>';
    if (audit === 1) return '<span class="badge audit-1">审核通过</span>';
    if (audit === 2) return '<span class="badge audit-2">审核驳回</span>';
    return '<span class="badge draft">未提交审核</span>';
  }

  async function loadWorks() {
    el.workList.innerHTML = '<div class="sk sk-line" style="margin-bottom:12px"></div><div class="sk sk-line" style="margin-bottom:12px"></div><div class="sk sk-line" style="width:70%"></div>';
    try {
      const data = await API.getAuthorNovels({ page: 1, page_size: 50 });
      state.novels = Array.isArray(data.list) ? data.list : [];
      if (state.selectedId && !state.novels.some((n) => Number(n.novel_id) === Number(state.selectedId))) {
        state.selectedId = null;
        state.chapters = [];
        el.chapterLayout.hidden = true;
        el.chapterEmpty.hidden = false;
        el.selectedNovelHint.textContent = '先在「我的作品」里选择一部作品';
      }
      renderWorks();
    } catch (err) {
      el.workList.innerHTML = '<div class="state-box"><div class="state-icon">!</div><strong>作品加载失败</strong><p>' +
        API.esc(err.message) + '</p><button type="button" class="btn" data-action="retry-works">重新加载</button></div>';
      handleAuthError(err);
    }
  }

  function renderWorks() {
    if (!state.novels.length) {
      el.workList.innerHTML = '<div class="state-box"><div class="state-icon">书</div><strong>还没有作品</strong><p>用下面的「新建作品」表单发布第一部作品。</p></div>';
      return;
    }
    el.workList.innerHTML = '<div class="work-list">' + state.novels.map((novel) => {
      const id = Number(novel.novel_id);
      const selected = id === Number(state.selectedId);
      const cover = 'cover--' + (id % 6 + 1);
      const title = novel.title || '未命名作品';
      return '<div class="work-row' + (selected ? ' is-selected' : '') + '">' +
        '<div class="work-mini-cover ' + cover + '">' + API.esc(Array.from(title)[0] || '书') + '</div>' +
        '<div class="work-main">' +
          '<p class="work-title">' + API.esc(title) + '</p>' +
          '<div class="work-meta">' +
            statusBadge(novel.status) + auditBadge(novel.audit_status) +
            '<span>' + (novel.chapter_count || 0) + ' 章</span>' +
            '<span>' + API.words(novel.total_words) + '</span>' +
            '<span>更新 ' + API.date(novel.updated_at) + '</span>' +
          '</div>' +
        '</div>' +
        '<div class="work-actions"><button type="button" class="btn small" data-select="' + id + '">' +
          (selected ? '管理中的作品' : '管理章节') +
        '</button></div>' +
      '</div>';
    }).join('') + '</div>';

    if (state.selectedId) {
      const current = state.novels.find((n) => Number(n.novel_id) === Number(state.selectedId));
      if (current) el.selectedNovelHint.textContent = '当前作品：' + current.title;
    }
  }

  async function selectNovel(id) {
    state.selectedId = Number(id);
    state.editingId = null;
    state.chapters = [];
    renderWorks();
    el.chapterEmpty.hidden = true;
    el.chapterLayout.hidden = false;
    resetChapterForm();
    await loadChapters();
  }

  /* ---------- 章节 ---------- */
  async function fetchChapters(novelId) {
    try {
      const data = await API.getAuthorChapters(novelId);
      return { list: Array.isArray(data.list) ? data.list : [], partial: false };
    } catch (err) {
      if (err.status === 404 || err.code === 40401) {
        const data = await API.getChapters(novelId);
        const list = (Array.isArray(data.list) ? data.list : []).map((item) => Object.assign({}, item, {
          status: 1,
          audit_status: 1,
        }));
        return { list, partial: true };
      }
      throw err;
    }
  }

  async function loadChapters() {
    const id = state.selectedId;
    if (!id) return;
    el.chapterList.innerHTML = '<div class="sk sk-line" style="margin-bottom:10px"></div><div class="sk sk-line" style="margin-bottom:10px"></div><div class="sk sk-line" style="width:76%"></div>';
    el.chapterNote.hidden = true;
    if (!state.tags.length) ensureTags();
    try {
      const result = await fetchChapters(id);
      if (state.selectedId !== id) return;
      state.chapters = result.list;
      state.partial = result.partial;
      if (result.partial) {
        el.chapterNote.hidden = false;
        el.chapterNote.textContent = '后端暂未提供作者章节列表接口，当前仅显示已发布章节；草稿与审核状态请以数据库为准。';
      }
      renderChapters();
    } catch (err) {
      if (handleAuthError(err)) return;
      el.chapterList.innerHTML = '<div class="state-box"><div class="state-icon">!</div><strong>章节加载失败</strong><p>' +
        API.esc(err.message) + '</p><button type="button" class="btn" data-action="retry-chapters">重新加载</button></div>';
    }
  }

  function chapterStatusBadge(status) {
    if (status === 1) return '<span class="badge audit-1">已发布</span>';
    if (status === 2) return '<span class="badge offline">已下架</span>';
    return '<span class="badge draft">草稿</span>';
  }

  function renderChapters() {
    if (!state.chapters.length) {
      el.chapterList.innerHTML = '<div class="state-box"><div class="state-icon">稿</div><strong>还没有章节</strong><p>用右侧表单创建第一章草稿。</p></div>';
      return;
    }
    el.chapterList.innerHTML = state.chapters.map((chapter) => {
      const editing = Number(chapter.chapter_id) === Number(state.editingId);
      const canPublish = chapter.status === 0;
      const vip = chapter.is_vip === 1
        ? '<span class="badge vip">VIP ¥' + API.money(chapter.vip_price) + '</span>'
        : '';
      return '<div class="chapter-row' + (editing ? ' is-editing' : '') + '">' +
        '<span class="chapter-no">' + String(chapter.chapter_no).padStart(2, '0') + '</span>' +
        '<div class="chapter-row-main">' +
          '<p class="chapter-row-title">' + API.esc(API.chapterTitle(chapter.title)) + '</p>' +
          '<div class="chapter-row-meta">' +
            chapterStatusBadge(chapter.status) + auditBadge(chapter.audit_status) +
            '<span>' + API.words(chapter.word_count) + '</span>' + vip +
          '</div>' +
          '<div class="chapter-row-actions">' +
            '<button type="button" class="btn small ghost" data-act="edit" data-id="' + chapter.chapter_id + '"' +
              (chapter.status === 2 ? ' disabled title="已下架章节不可编辑"' : '') + '>改稿</button>' +
            (canPublish
              ? '<button type="button" class="btn small" data-act="publish" data-id="' + chapter.chapter_id + '"' +
                (chapter.audit_status === 1 ? '' : ' title="审核通过后才能发布"') + '>发布</button>'
              : '') +
            (chapter.status === 1
              ? '<button type="button" class="btn small danger" data-act="offline" data-id="' + chapter.chapter_id + '">下架</button>'
              : '') +
          '</div>' +
        '</div>' +
      '</div>';
    }).join('');
  }

  async function startEdit(chapterId) {
    const chapter = state.chapters.find((c) => Number(c.chapter_id) === Number(chapterId));
    if (!chapter) return;
    let content = chapter.content;
    if (content === undefined || content === null) {
      try {
        const detail = await API.getChapter(chapterId);
        content = detail.content || '';
      } catch (err) {
        API.toast('无法读取该章正文：' + err.message, 'error');
        return;
      }
    }
    state.editingId = Number(chapterId);
    el.chapterFormTitle.textContent = '修改第 ' + chapter.chapter_no + ' 章';
    el.chapterTitle.value = chapter.title || '';
    el.chapterContent.value = content || '';
    el.chapterVip.value = String(chapter.is_vip === 1 ? 1 : 0);
    el.chapterPrice.value = API.money(chapter.vip_price);
    el.chapterPrice.disabled = chapter.is_vip !== 1;
    el.saveChapterBtn.textContent = '保存修改';
    el.cancelEditBtn.hidden = false;
    el.chapterFormHint.textContent = chapter.status === 1
      ? '已发布章节保存后会先下线，并重新进入审核。'
      : '保存后章节回到草稿状态，需重新提交审核。';
    renderChapters();
    el.chapterForm.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }

  function resetChapterForm() {
    state.editingId = null;
    el.chapterForm.reset();
    el.chapterPrice.value = '0.00';
    el.chapterPrice.disabled = true;
    el.chapterFormTitle.textContent = '新建章节草稿';
    el.saveChapterBtn.textContent = '保存草稿';
    el.cancelEditBtn.hidden = true;
    el.chapterFormHint.textContent = '';
    if (state.chapters.length) renderChapters();
  }

  async function saveChapter() {
    if (!state.selectedId) {
      API.toast('请先选择一部作品', 'error');
      return;
    }
    const title = el.chapterTitle.value.trim();
    const content = el.chapterContent.value;
    const isVip = Number(el.chapterVip.value) === 1 ? 1 : 0;
    const price = Number(el.chapterPrice.value);

    if (!title) {
      API.toast('请填写章节标题', 'error');
      el.chapterTitle.focus();
      return;
    }
    if (!content.trim()) {
      API.toast('正文不能为空', 'error');
      el.chapterContent.focus();
      return;
    }
    if (isVip && !(price > 0)) {
      API.toast('VIP 章节价格必须大于 0', 'error');
      el.chapterPrice.focus();
      return;
    }

    const payload = {
      title,
      content,
      is_vip: isVip,
      vip_price: isVip ? Number(price.toFixed(2)) : 0,
    };
    const editing = Number(state.editingId);
    el.saveChapterBtn.disabled = true;
    el.saveChapterBtn.textContent = '保存中…';
    try {
      if (editing) {
        await API.updateChapter(editing, payload);
        API.toast('章节已修改', 'success');
      } else {
        const res = await API.createChapter(state.selectedId, payload);
        API.toast('草稿已创建（第 ' + (res && res.chapter_no ? res.chapter_no : '新') + ' 章）', 'success');
      }
      resetChapterForm();
      await loadChapters();
      await loadWorks();
    } catch (err) {
      if (!handleAuthError(err)) API.toast(err.message, 'error');
    } finally {
      el.saveChapterBtn.disabled = false;
      el.saveChapterBtn.textContent = state.editingId ? '保存修改' : '保存草稿';
    }
  }

  async function publishChapter(chapterId) {
    try {
      await API.publishChapter(chapterId);
      API.toast('章节已发布', 'success');
      await loadChapters();
      await loadWorks();
    } catch (err) {
      if (!handleAuthError(err)) API.toast(err.message, 'error');
    }
  }

  async function offlineChapter(chapterId) {
    if (!window.confirm('下架后读者将无法阅读本章，确认下架？')) return;
    try {
      await API.offlineChapter(chapterId);
      API.toast('章节已下架', 'success');
      await loadChapters();
      await loadWorks();
    } catch (err) {
      if (!handleAuthError(err)) API.toast(err.message, 'error');
    }
  }

  /* ---------- 新建作品 ---------- */
  async function createNovel(event) {
    event.preventDefault();
    const title = el.novelTitle.value.trim();
    if (!title) {
      API.toast('请填写书名', 'error');
      el.novelTitle.focus();
      return;
    }
    const tagIds = selectedTagIds();
    if (tagIds.length > 10) {
      API.toast('标签最多选择 10 个', 'error');
      return;
    }
    const original = el.createNovelBtn.textContent;
    el.createNovelBtn.disabled = true;
    el.createNovelBtn.textContent = '创建中…';
    try {
      const data = await API.createNovel({
        title,
        cover_url: el.coverUrl.value.trim(),
        description: el.novelDesc.value.trim(),
        tag_ids: tagIds,
      });
      API.toast('作品已创建，等待审核', 'success');
      el.novelForm.reset();
      Array.from(el.novelTags.querySelectorAll('input')).forEach((input) => { input.disabled = false; });
      updateTagCounter();
      await loadWorks();
      if (data && data.novel_id) await selectNovel(data.novel_id);
    } catch (err) {
      if (!handleAuthError(err)) API.toast(err.message, 'error');
    } finally {
      el.createNovelBtn.disabled = false;
      el.createNovelBtn.textContent = original;
    }
  }

  /* ---------- 事件 ---------- */
  function bindEvents() {
    el.devTokenBtn.addEventListener('click', fetchDevToken);
    el.saveTokenBtn.addEventListener('click', saveManualToken);
    el.logoutBtn.addEventListener('click', () => logout(false));
    el.reloadWorksBtn.addEventListener('click', loadWorks);
    el.novelForm.addEventListener('submit', createNovel);
    el.chapterForm.addEventListener('submit', (event) => {
      event.preventDefault();
      saveChapter();
    });
    el.cancelEditBtn.addEventListener('click', resetChapterForm);

    el.chapterVip.addEventListener('change', () => {
      const vip = el.chapterVip.value === '1';
      el.chapterPrice.disabled = !vip;
      if (vip && !(Number(el.chapterPrice.value) > 0)) el.chapterPrice.value = '0.10';
    });

    el.novelTags.addEventListener('change', (event) => {
      const inputs = Array.from(el.novelTags.querySelectorAll('input'));
      if (inputs.filter((input) => input.checked).length > 10) {
        event.target.checked = false;
        API.toast('标签最多选择 10 个', 'error');
      }
      const full = inputs.filter((input) => input.checked).length >= 10;
      inputs.forEach((input) => { if (!input.checked) input.disabled = full; });
      updateTagCounter();
    });

    el.workList.addEventListener('click', (event) => {
      const select = event.target.closest('[data-select]');
      if (select) {
        selectNovel(Number(select.dataset.select));
        return;
      }
      if (event.target.closest('[data-action="retry-works"]')) loadWorks();
    });

    el.chapterList.addEventListener('click', (event) => {
      if (event.target.closest('[data-action="retry-chapters"]')) {
        loadChapters();
        return;
      }
      const btn = event.target.closest('button[data-act]');
      if (!btn) return;
      const id = Number(btn.dataset.id);
      if (btn.dataset.act === 'edit') startEdit(id);
      if (btn.dataset.act === 'publish') publishChapter(id);
      if (btn.dataset.act === 'offline') offlineChapter(id);
    });
  }

  function init() {
    bindEvents();
    renderAuth();
    if (isLoggedIn()) enterWorkspace();
  }

  init();
})();
