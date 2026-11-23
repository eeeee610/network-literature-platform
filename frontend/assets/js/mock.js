/* 演示数据：后端不可达或 ?mock=1 时使用。字段与接口文档 v1.0 保持一致 */
window.MockData = (function () {
  'use strict';

  const NOW = '2026-11-23 10:20:00';
  const AUTHOR_ID = 10001;
  const AUTHOR_NAME = '沈砚';

  const tags = [
    { tag_id: 1, tag_name: '玄幻', is_active: 1 },
    { tag_id: 2, tag_name: '热血', is_active: 1 },
    { tag_id: 3, tag_name: '都市', is_active: 1 },
    { tag_id: 4, tag_name: '悬疑', is_active: 1 },
    { tag_id: 5, tag_name: '科幻', is_active: 1 },
    { tag_id: 6, tag_name: '治愈', is_active: 1 },
    { tag_id: 7, tag_name: '美食', is_active: 1 },
    { tag_id: 8, tag_name: '冒险', is_active: 1 },
    { tag_id: 9, tag_name: '权谋', is_active: 1 },
    { tag_id: 10, tag_name: '日常', is_active: 1 },
  ];

  const books = [
    {
      novel_id: 1001,
      author_id: AUTHOR_ID,
      author_name: AUTHOR_NAME,
      title: '剑起长安',
      cover_url: '',
      status: 0,
      audit_status: 1,
      tag_ids: [1, 2, 9],
      created_at: '2026-06-02 10:00:00',
      updated_at: '2026-11-23 09:12:00',
      description: '长安城的雨下了三天，铁匠铺里多了一柄没有开刃的剑。\n\n灰衣少年沈砚带着它进城，只为查清十年前那场大火。剑未出鞘，朝堂与江湖已经先一步听见了风声。\n\n这是一个关于旧账、旧友与旧誓言的故事。',
      hero: '沈砚',
      opening: '长安的雨下了三天，朱雀门上的铜钉被雨水洗得发亮。',
      lines: [
        '城南的旧巷里，铁匠铺的炉火整夜未熄。沈砚推门进去，看见墙角靠着一柄没有开刃的剑，剑身上刻着极细的云纹。',
        '掌柜没有抬头，只说了一句：“剑是死的，握剑的人才是活的。”沈砚没有答话，把剑提起来，手腕一沉，竟觉得有千斤重。',
        '雨停的时候，街上有马蹄声掠过。沈砚把剑背在身后，走过长街，谁也没有注意到这个穿灰衣的年轻人。',
        '驿站的檐下挂着一盏旧灯，灯下坐着个老人。老人看了他一眼，说：“你身上有长安的味道。”',
        '夜里他做了个梦，梦见城楼上的旗子全部倒向同一个方向。醒来时窗外起风，纸窗被吹得作响。',
        '他把昨夜的笔记翻到最后，写下四个字：明日入城。',
      ],
    },
    {
      novel_id: 1002,
      author_id: 10002,
      author_name: '林见鹿',
      title: '雾港来信',
      cover_url: '',
      status: 0,
      audit_status: 1,
      tag_ids: [4, 3],
      created_at: '2026-08-11 15:30:00',
      updated_at: '2026-11-22 21:40:00',
      description: '港口的雾每年十一月准时到来，也总有人在这个时候收到不该收到的信。\n\n林知夏是唯一一个把每封信都留下来的人。三年后她回到雾港，发现寄信的人一直没有离开。',
      hero: '林知夏',
      opening: '雾从海面漫上来的时候，港口的钟刚好敲了七下。',
      lines: [
        '林知夏把最后一封信塞进大衣内袋，指尖被纸边割出一道细口，血珠很小，像一粒红砂。',
        '灯塔的光每十二秒扫过防波堤一次。她数了三遍，确认没有第六个影子跟在她身后。',
        '旧仓库的门没有锁，门轴发出一声很长的呻吟。里面只有一张桌子和一盏亮着的灯，桌上摊着一封写了一半的回信。',
        '信纸的右下角有一个褪色的邮戳，日期是三年前。她抬起头，听见楼上有人把椅子轻轻推开。',
        '“你来得比我预计的早。”声音从楼梯拐角传来，不紧不慢，像是在等一个迟到的熟人。',
        '她把信举到灯下，看清了那句被水渍晕开的字：不要相信第一个告诉你真相的人。',
      ],
    },
    {
      novel_id: 1003,
      author_id: 10003,
      author_name: '白露未晞',
      title: '山海食堂',
      cover_url: '',
      status: 1,
      audit_status: 1,
      tag_ids: [6, 7, 10],
      created_at: '2026-03-18 09:00:00',
      updated_at: '2026-11-18 19:05:00',
      description: '巷子最深处有一家只备十二份食材的小店，卖完就关门。\n\n老板娘不问你从哪里来，只问你今天想吃什么。十二个客人，十二段被热气慢慢熨平的日子。',
      hero: '老板娘',
      opening: '山海食堂开在巷子最深处，门口一株老槐，夏天落下一地碎影。',
      lines: [
        '今天的汤是萝卜炖排骨，火候到了，汤色泛白，撒一把葱花，香气就从后厨一路飘到门口。',
        '第一个客人是个加班的年轻人，眼睛里全是倦意。她把碗推过去，说：“先喝汤，别急着说事。”',
        '窗外下起小雨，屋檐滴水的声音一下一下。店里没有人说话，只有勺子碰到碗沿的轻响。',
        '墙上挂着一块小黑板，写着今日的菜名，字迹被蒸汽熏得发软。',
        '临走时年轻人在门口站了一会儿，回头说：“明天还开吗？”老板娘笑着指了指门口的木牌：照常。',
        '收拾完最后一副碗筷，她把灯熄了，街上的风带着饭菜的余温，慢慢散开。',
      ],
    },
    {
      novel_id: 1004,
      author_id: 10004,
      author_name: '顾青崖',
      title: '归墟灯塔',
      cover_url: '',
      status: 0,
      audit_status: 1,
      tag_ids: [5, 8],
      created_at: '2026-05-09 20:00:00',
      updated_at: '2026-11-23 08:30:00',
      description: '第七次跃迁之后，飞船的仪表全部归零，舷窗外只剩一片缓慢流动的暗蓝色。\n\n他们在不该存在的卫星上找到一座会呼吸的灯塔。灯亮起的那一刻，所有人都看见了回家的航线——也许。',
      hero: '周澈',
      opening: '第七次跃迁结束后，飞船的仪表全部归零。',
      lines: [
        '舷窗外没有星光，只有一片缓慢流动的暗蓝色，像深海。周澈在舰桥上站了很久，才意识到自己一直屏着呼吸。',
        '“我们到了。”他按下通讯键，声音在空舱里回荡。回应他的只有电流的杂音，和远处一声无法判断方向的回响。',
        '灯塔建在一颗不该存在的卫星上，塔身由某种会呼吸的金属构成，每一次脉冲都让导航仪短暂失灵。',
        '登陆舱落在环形山边缘，扬起的尘埃在低重力里悬了很久。周澈看见塔底刻着一行字，不是任何已知语言。',
        '当他伸手触碰塔门，整座灯塔亮了起来，光从内部涌出，照亮了他们回家的航线，也可能是另一条路。',
        '通讯恢复时，他只传回一句话：“记录坐标，不要派第二艘船。”',
      ],
    },
    {
      novel_id: 1006,
      author_id: AUTHOR_ID,
      author_name: AUTHOR_NAME,
      title: '长夜行舟',
      cover_url: '',
      status: 0,
      audit_status: 0,
      tag_ids: [9, 1],
      created_at: '2026-11-20 22:00:00',
      updated_at: '2026-11-21 23:10:00',
      description: '一条船、一江夜雾，和三个各怀心事的人。新书筹备中。',
      hero: '沈砚',
      opening: '',
      lines: [],
    },
    {
      novel_id: 1007,
      author_id: AUTHOR_ID,
      author_name: AUTHOR_NAME,
      title: '旧城拾遗',
      cover_url: '',
      status: 0,
      audit_status: 2,
      tag_ids: [3, 10],
      created_at: '2026-10-02 11:00:00',
      updated_at: '2026-10-09 16:20:00',
      description: '关于旧城改造的短篇集。因简介涉及具体地名，审核未通过，待修改后重新提交。',
      hero: '叙述者',
      opening: '',
      lines: [],
    },
    {
      novel_id: 1008,
      author_id: AUTHOR_ID,
      author_name: AUTHOR_NAME,
      title: '雪落无声',
      cover_url: '',
      status: 2,
      audit_status: 1,
      tag_ids: [4],
      created_at: '2026-01-06 08:00:00',
      updated_at: '2026-09-15 12:00:00',
      description: '一部已经下架的旧作，仅作者本人可见。',
      hero: '叙述者',
      opening: '',
      lines: [],
    },
  ];

  const titles = {
    1001: ['陨落的天才', '灰衣少年', '铁匠铺的剑', '雨夜叩门', '长安旧梦', '城楼风起', '第一场雪', '剑试天下', '故人来信', '未开刃的锋'],
    1002: ['雾里的钟声', '第三封信', '废弃仓库', '邮戳上的日期', '楼上的椅子', '潮汐时刻', '灯塔守夜人', '十二秒的光', '潮湿的真相', '最后一个影子'],
    1003: ['开张的第一碗', '萝卜与排骨', '加班的客人', '雨天的空位', '一碗长寿面', '打烊之前', '巷口的槐树', '打包一份春天', '老食客', '照常营业'],
    1004: ['第七次跃迁', '不该存在的卫星', '会呼吸的塔', '低重力尘埃', '未知语言', '坐标之外', '深蓝色的海', '第二次脉冲', '一条航线', '不要派船'],
  };

  const vipOf = {
    1001: { 9: 0.12, 10: 0.15 },
    1002: { 9: 0.09, 10: 0.09 },
    1003: { 10: 0.10 },
    1004: { 8: 0.15, 9: 0.15, 10: 0.18 },
  };

  const chapters = [];

  function fail(code, message) {
    const e = new Error(message);
    e.code = code;
    return e;
  }

  function pad(n) {
    return n < 10 ? '0' + n : String(n);
  }

  function countWords(text) {
    return text ? text.replace(/\s+/g, '').length : 0;
  }

  function buildContent(book, no) {
    const pool = book.lines;
    if (!pool.length) return '';
    const pick = (i) => pool[i % pool.length];
    return [
      book.opening + '这是入冬后的第' + no + '天，比往年冷一些。',
      pick(no - 1),
      pick(no + 1),
      pick(no + 3),
      '这一章到这里，故事还没有结束。',
    ].join('\n\n');
  }

  function initChapters() {
    Object.keys(titles).forEach((key) => {
      const book = books.find((b) => String(b.novel_id) === key);
      if (!book) return;
      const vipMap = vipOf[book.novel_id] || {};
      titles[key].forEach((title, index) => {
        const no = index + 1;
        const isVip = vipMap[no] ? 1 : 0;
        const content = isVip ? null : buildContent(book, no);
        chapters.push({
          chapter_id: book.novel_id * 1000 + no,
          novel_id: book.novel_id,
          chapter_no: no,
          title: '第' + no + '章 ' + title,
          content,
          is_vip: isVip,
          vip_price: isVip ? vipMap[no] : 0,
          word_count: 2800 + (no * 173) % 900,
          status: 1,
          audit_status: 1,
          publish_time: '2026-11-' + pad(3 + no) + ' 20:00:00',
          updated_at: '2026-11-' + pad(3 + no) + ' 20:00:00',
        });
      });
    });

    /* 作者演示：草稿 / 待审 / 驳回各一，外加一条审核通过待发布的章节 */
    const extra = [
      { no: 11, title: '未寄出的信', audit: 1, vip: 0, price: 0 },
      { no: 12, title: '灯下', audit: null, vip: 0, price: 0 },
      { no: 13, title: '旧友', audit: 2, vip: 1, price: 0.10 },
    ];
    extra.forEach((item) => {
      const content = buildContent(books[0], item.no);
      chapters.push({
        chapter_id: 1001 * 1000 + item.no,
        novel_id: 1001,
        chapter_no: item.no,
        title: '第' + item.no + '章 ' + item.title,
        content,
        is_vip: item.vip,
        vip_price: item.price,
        word_count: countWords(content),
        status: 0,
        audit_status: item.audit,
        publish_time: null,
        updated_at: '2026-11-21 22:10:00',
      });
    });
  }

  initChapters();

  function tagName(id) {
    const t = tags.find((item) => item.tag_id === id);
    return t ? t.tag_name : '';
  }

  function publishedChapters(novelId) {
    return chapters
      .filter((c) => c.novel_id === novelId && c.status === 1 && c.audit_status === 1)
      .sort((a, b) => a.chapter_no - b.chapter_no);
  }

  function recompute(book) {
    const pubs = publishedChapters(book.novel_id);
    book.total_words = pubs.reduce((sum, c) => sum + (c.word_count || 0), 0);
    book.latest_chapter_id = pubs.length ? pubs[pubs.length - 1].chapter_id : null;
  }

  books.forEach(recompute);

  function displayable(book) {
    return book.audit_status === 1 && book.status !== 2;
  }

  function toListItem(book) {
    return {
      novel_id: book.novel_id,
      title: book.title,
      cover_url: book.cover_url,
      description: book.description,
      status: book.status,
      total_words: book.total_words,
      latest_chapter_id: book.latest_chapter_id,
      author_id: book.author_id,
      author_name: book.author_name,
      tag_names: book.tag_ids.map(tagName).filter(Boolean),
      updated_at: book.updated_at,
    };
  }

  function paginate(list, query) {
    const pageSize = Math.min(Math.max(Number(query.page_size) || 20, 1), 50);
    const page = Math.max(Number(query.page) || 1, 1);
    const start = (page - 1) * pageSize;
    return { page, page_size: pageSize, total: list.length, list: list.slice(start, start + pageSize) };
  }

  function findNovel(id) {
    return books.find((b) => b.novel_id === Number(id));
  }

  function novelOr404(id) {
    const book = findNovel(id);
    if (!book || !displayable(book)) throw fail(40401, '作品不存在或已下架');
    return book;
  }

  function normalTime() {
    const d = new Date();
    return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' '
      + pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds());
  }

  function handle(req) {
    const method = (req.method || 'GET').toUpperCase();
    const path = String(req.path || '').split('?')[0];
    const q = req.query || {};
    const body = req.body || {};
    let m;

    if (method === 'GET' && path === '/tags') {
      return tags.filter((t) => t.is_active === 1);
    }

    if (method === 'GET' && path === '/novels') {
      let list = books.filter(displayable);
      if (q.status !== undefined && q.status !== '') list = list.filter((b) => b.status === Number(q.status));
      if (q.tag_id) list = list.filter((b) => b.tag_ids.indexOf(Number(q.tag_id)) >= 0);
      const sort = q.sort || 'update';
      list = list.slice().sort((a, b) => {
        if (sort === 'words') return b.total_words - a.total_words;
        if (sort === 'new') return b.created_at.localeCompare(a.created_at);
        return b.updated_at.localeCompare(a.updated_at);
      });
      const pageData = paginate(list, q);
      pageData.list = pageData.list.map(toListItem);
      return pageData;
    }

    if (method === 'GET' && path === '/novels/search') {
      const keyword = String(q.keyword || '').trim();
      if (!keyword && !q.tag_id) throw fail(40001, '关键词与标签至少填写一个');
      let list = books.filter(displayable);
      if (keyword) {
        list = list.filter((b) => b.title.indexOf(keyword) === 0 || b.author_name.indexOf(keyword) >= 0);
      }
      if (q.tag_id) list = list.filter((b) => b.tag_ids.indexOf(Number(q.tag_id)) >= 0);
      const pageData = paginate(list, q);
      pageData.list = pageData.list.map(toListItem);
      return pageData;
    }

    if (method === 'POST' && path === '/novels') {
      const title = String(body.title || '').trim();
      if (!title) throw fail(40001, '书名不能为空');
      if (Array.isArray(body.tag_ids) && body.tag_ids.length > 10) throw fail(40001, '标签数量不能超过 10 个');
      const id = books.reduce((max, b) => Math.max(max, b.novel_id), 1000) + 1;
      books.push({
        novel_id: id,
        author_id: AUTHOR_ID,
        author_name: AUTHOR_NAME,
        title,
        cover_url: body.cover_url || '',
        status: 0,
        audit_status: 0,
        tag_ids: (body.tag_ids || []).map(Number),
        created_at: normalTime(),
        updated_at: normalTime(),
        description: body.description || '',
        hero: '未命名',
        opening: '',
        lines: [],
      });
      recompute(findNovel(id));
      return { novel_id: id };
    }

    if (method === 'GET' && (m = path.match(/^\/novels\/(\d+)\/chapters$/))) {
      const book = novelOr404(m[1]);
      return {
        novel_id: book.novel_id,
        list: publishedChapters(book.novel_id).map((c) => ({
          chapter_id: c.chapter_id,
          chapter_no: c.chapter_no,
          title: c.title,
          is_vip: c.is_vip,
          vip_price: c.vip_price,
          word_count: c.word_count,
          publish_time: c.publish_time,
        })),
      };
    }

    if (method === 'GET' && (m = path.match(/^\/novels\/(\d+)$/))) {
      const book = novelOr404(m[1]);
      const pubs = publishedChapters(book.novel_id);
      return {
        novel_id: book.novel_id,
        title: book.title,
        cover_url: book.cover_url,
        description: book.description,
        status: book.status,
        total_words: book.total_words,
        latest_chapter_id: book.latest_chapter_id,
        author_id: book.author_id,
        author_name: book.author_name,
        tags: book.tag_ids.map((id, index) => ({ tag_id: id, tag_name: tagName(id), sort_order: index })),
        chapter_count: pubs.length,
        last_chapter_title: pubs.length ? pubs[pubs.length - 1].title : null,
        updated_at: book.updated_at,
      };
    }

    if (method === 'POST' && (m = path.match(/^\/novels\/(\d+)\/chapters$/))) {
      const book = findNovel(m[1]);
      if (!book) throw fail(40401, '作品不存在');
      const title = String(body.title || '').trim();
      const content = String(body.content || '');
      if (!title) throw fail(40001, '章节标题不能为空');
      if (!content.trim()) throw fail(40001, '正文不能为空');
      const isVip = Number(body.is_vip) === 1 ? 1 : 0;
      const price = isVip ? Number(body.vip_price) : 0;
      if (isVip && !(price > 0)) throw fail(40001, 'VIP 章节价格必须大于 0');
      const no = chapters.filter((c) => c.novel_id === book.novel_id)
        .reduce((max, c) => Math.max(max, c.chapter_no), 0) + 1;
      const id = Date.now();
      const wc = countWords(content);
      chapters.push({
        chapter_id: id,
        novel_id: book.novel_id,
        chapter_no: no,
        title,
        content,
        is_vip: isVip,
        vip_price: price,
        word_count: wc,
        status: 0,
        audit_status: null,
        publish_time: null,
        updated_at: normalTime(),
      });
      book.updated_at = normalTime();
      return { chapter_id: id, chapter_no: no, word_count: wc };
    }

    if (method === 'GET' && (m = path.match(/^\/author\/novels\/(\d+)\/chapters$/))) {
      const book = findNovel(m[1]);
      if (!book) throw fail(40401, '作品不存在');
      return {
        novel_id: book.novel_id,
        list: chapters
          .filter((c) => c.novel_id === book.novel_id)
          .sort((a, b) => a.chapter_no - b.chapter_no)
          .map((c) => ({
            chapter_id: c.chapter_id,
            chapter_no: c.chapter_no,
            title: c.title,
            content: c.content !== null && c.content !== undefined
              ? c.content
              : '（演示数据未提供本章全文，可在此编辑后保存。）',
            is_vip: c.is_vip,
            vip_price: c.vip_price,
            word_count: c.word_count,
            status: c.status,
            audit_status: c.audit_status,
            publish_time: c.publish_time,
            updated_at: c.updated_at,
          })),
      };
    }

    if (method === 'GET' && path === '/author/novels') {
      let list = books.filter((b) => b.author_id === AUTHOR_ID);
      if (q.status !== undefined && q.status !== '') list = list.filter((b) => b.status === Number(q.status));
      const pageData = paginate(list, q);
      pageData.list = pageData.list.map((book) => Object.assign(toListItem(book), {
        audit_status: book.audit_status,
        chapter_count: chapters.filter((c) => c.novel_id === book.novel_id && c.status === 1).length,
      }));
      return pageData;
    }

    if (method === 'GET' && (m = path.match(/^\/chapters\/(\d+)$/))) {
      const chapter = chapters.find((c) => c.chapter_id === Number(m[1]));
      if (!chapter) throw fail(40401, '章节不存在或已下架');
      const book = findNovel(chapter.novel_id);
      if (!book || !displayable(book) || chapter.status !== 1 || chapter.audit_status !== 1 || !chapter.publish_time) {
        throw fail(40401, '章节不存在或已下架');
      }
      const pubs = publishedChapters(chapter.novel_id);
      const index = pubs.findIndex((c) => c.chapter_id === chapter.chapter_id);
      if (chapter.is_vip === 1) throw fail(40301, '本章为 VIP 章节，需订阅后阅读');
      return {
        chapter_id: chapter.chapter_id,
        novel_id: chapter.novel_id,
        chapter_no: chapter.chapter_no,
        title: chapter.title,
        content: chapter.content,
        is_vip: chapter.is_vip,
        vip_price: chapter.vip_price,
        word_count: chapter.word_count,
        publish_time: chapter.publish_time,
        prev_chapter_id: index > 0 ? pubs[index - 1].chapter_id : null,
        next_chapter_id: index >= 0 && index < pubs.length - 1 ? pubs[index + 1].chapter_id : null,
      };
    }

    if (method === 'PUT' && (m = path.match(/^\/chapters\/(\d+)$/))) {
      const chapter = chapters.find((c) => c.chapter_id === Number(m[1]));
      if (!chapter) throw fail(40401, '章节不存在');
      if (chapter.status === 2) throw fail(40902, '已下架章节不可编辑');
      const title = String(body.title || '').trim();
      const content = String(body.content || '');
      if (!title) throw fail(40001, '章节标题不能为空');
      if (!content.trim()) throw fail(40001, '正文不能为空');
      const isVip = Number(body.is_vip) === 1 ? 1 : 0;
      const price = isVip ? Number(body.vip_price) : 0;
      if (isVip && !(price > 0)) throw fail(40001, 'VIP 章节价格必须大于 0');
      chapter.title = title;
      chapter.content = content;
      chapter.is_vip = isVip;
      chapter.vip_price = price;
      chapter.word_count = countWords(content);
      chapter.status = 0;
      chapter.audit_status = 0;
      chapter.updated_at = normalTime();
      const book = findNovel(chapter.novel_id);
      if (book) { book.updated_at = normalTime(); recompute(book); }
      return { chapter_id: chapter.chapter_id, chapter_no: chapter.chapter_no, word_count: chapter.word_count };
    }

    if (method === 'POST' && (m = path.match(/^\/chapters\/(\d+)\/publish$/))) {
      const chapter = chapters.find((c) => c.chapter_id === Number(m[1]));
      if (!chapter) throw fail(40401, '章节不存在');
      if (chapter.audit_status !== 1) throw fail(40902, '章节未通过审核，不能发布');
      if (chapter.status === 1) throw fail(40902, '章节已发布，请勿重复发布');
      if (chapter.status === 2) throw fail(40902, '已下架章节不可发布');
      chapter.status = 1;
      chapter.publish_time = normalTime();
      const book = findNovel(chapter.novel_id);
      if (book) { recompute(book); book.updated_at = normalTime(); }
      return null;
    }

    if (method === 'POST' && (m = path.match(/^\/chapters\/(\d+)\/offline$/))) {
      const chapter = chapters.find((c) => c.chapter_id === Number(m[1]));
      if (!chapter) throw fail(40401, '章节不存在');
      if (chapter.status === 2) throw fail(40902, '章节已处于下架状态');
      chapter.status = 2;
      const book = findNovel(chapter.novel_id);
      if (book) { recompute(book); book.updated_at = normalTime(); }
      return null;
    }

    if (method === 'GET' && path === '/dev/token') {
      const userId = Number(q.userId);
      if (!userId || userId <= 0) throw fail(40001, 'userId 必须为正整数');
      const role = String(q.role || 'AUTHOR').toUpperCase();
      if (['AUTHOR', 'ADMIN', 'READER'].indexOf(role) < 0) throw fail(40001, 'role 只能为 AUTHOR/ADMIN/READER');
      return {
        token: 'mock-token.' + userId + '.' + role + '.' + Date.now(),
        token_type: 'Bearer',
        expires_in: 86400,
        user_id: userId,
        role,
      };
    }

    throw fail(40401, '演示数据中没有该接口：' + method + ' ' + path);
  }

  return { handle, tags, books, chapters, now: NOW, authorId: AUTHOR_ID };
})();
