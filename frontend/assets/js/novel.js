/* 作品详情：基础信息 + 章节目录 */
(function () {
  'use strict';

  const novelId = new URLSearchParams(location.search).get('novelId');
  const area = document.getElementById('detailArea');
  const crumb = document.getElementById('crumb');

  function init() {
    if (!novelId) {
      renderMissing('缺少作品编号');
      return;
    }
    area.innerHTML = skeleton();
    load();
  }

  async function load() {
    try {
      const [detail, catalog] = await Promise.all([
        API.getNovel(novelId),
        API.getChapters(novelId).catch((err) => ({ error: err, list: [] })),
      ]);
      render(detail, catalog);
    } catch (err) {
      if (err.code === 40401) renderMissing('作品不存在或已下架');
      else renderLoadError(err);
    }
  }

  function render(detail, catalog) {
    const chapters = Array.isArray(catalog.list) ? catalog.list : [];
    const catalogError = catalog.error;
    const title = detail.title || '未命名作品';
    const cover = 'cover--' + ((Number(detail.novel_id) || 1) % 6 + 1);
    const tags = Array.isArray(detail.tags) ? detail.tags : [];
    const statusBadge = detail.status === 1
      ? '<span class="badge done">已完结</span>'
      : detail.status === 0
        ? '<span class="badge serial">连载中</span>'
        : '<span class="badge offline">已下架</span>';

    document.title = title + ' · 墨阅';
    crumb.innerHTML = '<a href="index.html">书城</a><span class="sep">/</span><span>' + API.esc(title) + '</span>';

    const chapterCount = chapters.length || detail.chapter_count || 0;
    const firstChapter = chapters[0];
    const lastChapter = chapters[chapters.length - 1];
    const description = detail.description || '作者还没有填写简介。';

    area.innerHTML = '' +
      '<section class="novel-head">' +
        '<div class="book-cover novel-cover ' + cover + '"><span class="book-cover-char">' + API.esc(Array.from(title)[0] || '书') + '</span></div>' +
        '<div class="novel-head-main">' +
          '<h1 class="novel-title">' + API.esc(title) + '</h1>' +
          '<div class="novel-subline">' +
            '<span>作者 <span class="author">' + API.esc(detail.author_name || '佚名') + '</span></span>' +
            statusBadge +
            '<span>最近更新 ' + API.date(detail.updated_at) + '</span>' +
          '</div>' +
          '<div class="novel-stats">' +
            '<div class="stat"><b>' + API.words(detail.total_words) + '</b><span>总字数</span></div>' +
            '<div class="stat"><b>' + chapterCount + '</b><span>已发布章节</span></div>' +
          '</div>' +
          (tags.length
            ? '<div class="novel-tags">' + tags.map((tag) => '<span class="chip">' + API.esc(tag.tag_name) + '</span>').join('') + '</div>'
            : '') +
          '<p class="novel-desc" id="novelDesc">' + API.esc(description) + '</p>' +
          '<div class="novel-actions">' +
            '<button type="button" class="btn primary" id="startRead"' + (firstChapter ? '' : ' disabled') + '>' +
              (firstChapter ? '开始阅读' : '暂无章节') +
            '</button>' +
            (lastChapter
              ? '<a class="btn ghost" href="' + API.esc(API.url('read.html', { chapterId: lastChapter.chapter_id, novelId: detail.novel_id })) + '">读最新章</a>'
              : '') +
          '</div>' +
          '<p class="latest-line" id="latestLine">' +
            (lastChapter
              ? '最新：第 ' + lastChapter.chapter_no + ' 章 <a href="' + API.esc(API.url('read.html', { chapterId: lastChapter.chapter_id, novelId: detail.novel_id })) + '">' + API.esc(API.chapterTitle(lastChapter.title)) + '</a>'
              : '暂无已发布章节') +
          '</p>' +
        '</div>' +
      '</section>' +
      '<section class="catalog-section">' +
        '<div class="catalog-head">' +
          '<h2>目录</h2>' +
          '<span class="meta-count">共 ' + chapterCount + ' 章</span>' +
        '</div>' +
        (catalogError
          ? '<p class="offline-note">目录加载失败：' + API.esc(catalogError.message) + '</p>'
          : chapters.length
            ? '<ol class="catalog-list">' + chapters.map((chapter) => catalogItem(chapter, detail.novel_id)).join('') + '</ol>'
            : '<div class="state-box"><div class="state-icon">稿</div><strong>还没有已发布章节</strong><p>作者正在准备内容，过些天再来看看。</p></div>') +
      '</section>';

    area.setAttribute('aria-busy', 'false');

    const startBtn = document.getElementById('startRead');
    if (startBtn && firstChapter) {
      startBtn.addEventListener('click', () => {
        location.href = API.url('read.html', { chapterId: firstChapter.chapter_id, novelId: detail.novel_id });
      });
    }
  }

  function catalogItem(chapter, id) {
    const vip = chapter.is_vip === 1
      ? '<span class="badge vip">VIP ¥' + API.money(chapter.vip_price) + '</span>'
      : '';
    return '<li>' +
      '<a class="catalog-item" href="' + API.esc(API.url('read.html', { chapterId: chapter.chapter_id, novelId: id })) + '">' +
        '<span class="catalog-no">' + String(chapter.chapter_no).padStart(2, '0') + '</span>' +
        '<span class="catalog-title">' + API.esc(API.chapterTitle(chapter.title)) + '</span>' +
        '<span class="catalog-words">' + API.words(chapter.word_count) + '</span>' +
        vip +
      '</a>' +
    '</li>';
  }

  function skeleton() {
    let html = '<section class="novel-head">' +
      '<div class="sk sk-cover novel-cover"></div>' +
      '<div class="novel-head-main">' +
        '<div class="sk sk-line" style="width:42%;height:30px"></div>' +
        '<div class="sk sk-line" style="width:30%"></div>' +
        '<div class="sk sk-line" style="width:64%"></div>' +
        '<div class="sk sk-line" style="width:88%"></div>' +
        '<div class="sk sk-line" style="width:76%"></div>' +
      '</div>' +
    '</section>' +
    '<section class="catalog-section"><div class="sk sk-line" style="width:120px;height:22px;margin-bottom:20px"></div>';
    for (let i = 0; i < 4; i += 1) {
      html += '<div class="sk sk-line" style="margin-bottom:16px"></div>';
    }
    return html + '</section>';
  }

  function renderMissing(message) {
    area.setAttribute('aria-busy', 'false');
    area.innerHTML = '<div class="state-box" style="margin-top:26px">' +
      '<div class="state-icon">失</div>' +
      '<strong>作品不存在或已下架</strong>' +
      '<p>' + API.esc(message === '缺少作品编号' ? '链接里没有带作品编号。' : '该作品可能已下架，或链接有误。') + '</p>' +
      '<a class="btn" href="index.html">回到书城</a>' +
      '</div>';
  }

  function renderLoadError(err) {
    area.setAttribute('aria-busy', 'false');
    area.innerHTML = '<div class="state-box" style="margin-top:26px">' +
      '<div class="state-icon">!</div>' +
      '<strong>详情加载失败</strong>' +
      '<p>' + API.esc(err.message || '请稍后重试') + '</p>' +
      '<button type="button" class="btn" id="retryDetail">重新加载</button>' +
      '</div>';
    document.getElementById('retryDetail').addEventListener('click', () => location.reload());
  }

  init();
})();
