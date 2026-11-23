/* 阅读页：正文、目录抽屉、上一章 / 下一章、浅色深色 */
(function () {
  'use strict';

  const params = new URLSearchParams(location.search);
  const chapterId = params.get('chapterId');
  let novelId = params.get('novelId');

  const main = document.getElementById('readerMain');
  const tocList = document.getElementById('tocList');
  const tocCount = document.getElementById('tocCount');
  const tocToggle = document.getElementById('tocToggle');
  const tocClose = document.getElementById('tocClose');
  const backdrop = document.getElementById('drawerBackdrop');
  const themeToggle = document.getElementById('themeToggle');
  const bookLink = document.getElementById('bookLink');

  let catalog = [];
  let theme = document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light';

  function init() {
    applyTheme(theme);
    if (novelId) setNovelLink(novelId);
    themeToggle.addEventListener('click', () => {
      theme = theme === 'dark' ? 'light' : 'dark';
      applyTheme(theme);
      try { localStorage.setItem('np_reader_theme', theme); } catch (e) {}
    });

    tocToggle.addEventListener('click', () => setDrawer(true));
    tocClose.addEventListener('click', () => setDrawer(false));
    backdrop.addEventListener('click', () => setDrawer(false));
    document.addEventListener('keydown', (event) => {
      if (event.key === 'Escape') setDrawer(false);
    });
    tocList.addEventListener('click', (event) => {
      if (event.target.closest('a')) setDrawer(false);
    });

    load();
  }

  function applyTheme(next) {
    document.documentElement.dataset.theme = next;
    const dark = next === 'dark';
    themeToggle.textContent = dark ? '浅色' : '深色';
    themeToggle.setAttribute('aria-pressed', String(dark));
  }

  function setDrawer(open) {
    document.body.classList.toggle('drawer-open', open);
    tocToggle.setAttribute('aria-expanded', String(open));
  }

  function setNovelLink(id) {
    if (!id) return;
    bookLink.href = API.url('novel.html', { novelId: id });
    bookLink.textContent = '作品详情';
  }

  async function load() {
    if (!chapterId) {
      renderError('章节不存在或已下架', '链接里没有带章节编号。');
      tocList.innerHTML = '<p class="toc-fail">缺少章节编号，目录不可用。</p>';
      return;
    }
    main.innerHTML = skeleton();
    try {
      const data = await API.getChapter(chapterId);
      novelId = data.novel_id;
      setNovelLink(novelId);
      renderChapter(data);
      const list = await fetchCatalog(novelId);
      if (list) {
        catalog = list;
        renderToc(Number(chapterId));
      }
    } catch (err) {
      if (err.code === 40301) {
        await handleVip();
      } else if (err.code === 40401) {
        renderError('章节不存在或已下架', err.message || '该章节可能已下架，或链接有误。');
        tocList.innerHTML = '<p class="toc-fail">章节不可用，目录加载已跳过。</p>';
      } else {
        renderError('章节加载失败', err.message || '请稍后重试。');
        tocList.innerHTML = '<p class="toc-fail">目录加载失败。</p>';
      }
    }
  }

  async function handleVip() {
    let meta = null;
    if (novelId) {
      setNovelLink(novelId);
      const list = await fetchCatalog(novelId);
      if (list) {
        catalog = list;
        renderToc(Number(chapterId));
        meta = list.find((item) => Number(item.chapter_id) === Number(chapterId)) || null;
      }
    }
    renderGate(meta);
  }

  async function fetchCatalog(id) {
    if (!id) {
      tocList.innerHTML = '<p class="toc-fail">未能定位所属作品，无法加载目录。</p>';
      return null;
    }
    try {
      const data = await API.getChapters(id);
      return Array.isArray(data.list) ? data.list : [];
    } catch (err) {
      tocList.innerHTML = '<p class="toc-fail">目录加载失败：' + API.esc(err.message) + '</p>';
      return null;
    }
  }

  function renderChapter(data) {
    const title = data.title || '未命名章节';
    const content = String(data.content || '');

    /* 接口正常返回但正文为空且为 VIP 章节时，同样按订阅提示处理 */
    if (data.is_vip === 1 && !content.trim()) {
      renderGate(data);
      return;
    }

    const paragraphs = content.split(/\r?\n/).map((line) => line.trim()).filter(Boolean);
    const vip = data.is_vip === 1
      ? '<span class="badge vip">VIP ¥' + API.money(data.vip_price) + '</span>'
      : '';

    document.title = title + ' · 墨阅';
    main.innerHTML = '' +
      '<header class="chapter-head">' +
        '<p class="chapter-eyebrow">第 ' + data.chapter_no + ' 章</p>' +
        '<h1 class="chapter-title">' + API.esc(API.chapterTitle(title)) + '</h1>' +
        '<div class="chapter-meta">' +
          '<span>约 <span class="num">' + API.words(data.word_count) + '</span></span>' +
          '<span>' + API.esc(data.publish_time || '') + ' 发布</span>' +
          vip +
        '</div>' +
      '</header>' +
      (paragraphs.length
        ? '<div class="chapter-body">' + paragraphs.map((line) => '<p>' + API.esc(line) + '</p>').join('') + '</div>'
        : '<div class="state-box" style="margin-top:26px"><div class="state-icon">稿</div><strong>本章暂无正文</strong><p>作者还在写，稍后再来。</p></div>') +
      '<nav class="reader-nav">' +
        '<button type="button" class="btn" id="prevBtn"' + (data.prev_chapter_id ? '' : ' disabled') + '>← 上一章</button>' +
        '<button type="button" class="btn" id="nextBtn"' + (data.next_chapter_id ? '' : ' disabled') + '>下一章 →</button>' +
      '</nav>';

    main.setAttribute('aria-busy', 'false');

    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');
    if (data.prev_chapter_id) prevBtn.addEventListener('click', () => goChapter(data.prev_chapter_id));
    if (data.next_chapter_id) nextBtn.addEventListener('click', () => goChapter(data.next_chapter_id));
  }

  function renderGate(meta) {
    const price = meta && meta.vip_price !== undefined ? '¥' + API.money(meta.vip_price) : '';
    const head = meta
      ? '<p class="chapter-eyebrow">VIP 章节</p>' +
        '<h1 class="chapter-title">' + API.esc(API.chapterTitle(meta.title)) + '</h1>' +
        '<div class="chapter-meta"><span>第 ' + meta.chapter_no + ' 章</span>' +
        '<span>约 ' + API.words(meta.word_count) + '</span>' +
        '<span class="badge vip">VIP ' + price + '</span></div>'
      : '<p class="chapter-eyebrow">VIP 章节</p>' +
        '<h1 class="chapter-title">本章需要订阅</h1>';

    document.title = (meta ? meta.title : 'VIP 章节') + ' · 墨阅';
    main.innerHTML = '<header class="chapter-head">' + head + '</header>' +
      '<section class="vip-gate">' +
        '<div class="lock">锁</div>' +
        '<h2>本章为 VIP 章节，需订阅后阅读</h2>' +
        '<p>订阅本书或本章后，即可阅读完整正文。</p>' +
        (price ? '<span class="price">' + price + '</span>' : '') +
        '<p>订阅与扣费由账户模块处理，完成后回到本页重新加载即可。</p>' +
        '<div class="gate-actions">' +
          '<button type="button" class="btn" id="gateRetry">已订阅，重新加载</button>' +
          (novelId ? '<a class="btn ghost" href="' + API.esc(API.url('novel.html', { novelId })) + '">返回作品详情</a>' : '') +
        '</div>' +
      '</section>';
    main.setAttribute('aria-busy', 'false');
    document.getElementById('gateRetry').addEventListener('click', () => location.reload());
  }

  function renderToc(currentId) {
    tocCount.textContent = '共 ' + catalog.length + ' 章';
    if (!catalog.length) {
      tocList.innerHTML = '<p class="toc-fail">本书还没有已发布章节。</p>';
      return;
    }
    tocList.innerHTML = catalog.map((chapter) => {
      const current = Number(chapter.chapter_id) === currentId;
      return '<a href="' + API.esc(API.url('read.html', { chapterId: chapter.chapter_id, novelId })) + '"' +
        (current ? ' class="is-current" aria-current="page"' : '') + '>' +
        '<span class="toc-no">' + String(chapter.chapter_no).padStart(2, '0') + '</span>' +
        '<span class="toc-title">' + API.esc(API.chapterTitle(chapter.title)) + '</span>' +
        (chapter.is_vip === 1 ? '<span class="toc-vip">VIP</span>' : '') +
        '</a>';
    }).join('');
  }

  function goChapter(id) {
    location.href = API.url('read.html', { chapterId: id, novelId });
  }

  function skeleton() {
    let html = '<div class="sk sk-line" style="width:90px"></div>' +
      '<div class="sk sk-line" style="width:58%;height:30px;margin-top:14px"></div>' +
      '<div class="sk sk-line" style="width:40%;margin-top:14px;margin-bottom:30px"></div>';
    for (let i = 0; i < 6; i += 1) {
      html += '<div class="sk sk-line" style="margin-bottom:18px"></div>';
    }
    return html;
  }

  function renderError(title, detail) {
    main.setAttribute('aria-busy', 'false');
    main.innerHTML = '<div class="state-box" style="margin-top:26px">' +
      '<div class="state-icon">失</div>' +
      '<strong>' + API.esc(title) + '</strong>' +
      '<p>' + API.esc(detail) + '</p>' +
      '<div class="gate-actions">' +
        '<a class="btn" href="index.html">回到书城</a>' +
        '<button type="button" class="btn ghost" id="retryBtn">重新加载</button>' +
      '</div>' +
      '</div>';
    document.getElementById('retryBtn').addEventListener('click', () => location.reload());
  }

  init();
})();
