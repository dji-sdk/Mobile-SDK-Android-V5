var panel
;(function () {
  if (!$('.documentation-page').length) return false

  var sidebar = $('.documentation-page .site-sidebar')
  // 真正的滚动容器现在是 .site-sidebar 本身（而非内层 .nav.scrollable），
  // 修正引用，否则"跳转后自动滚动定位到当前项"的逻辑会作用在错误的元素上，
  // 导致每次点击跳转后侧边栏都停留在顶部、看不到当前高亮项
  var sidebarInner = sidebar
  var currentTitle = sidebar.find('a.active:first-child')
  var footer = $('.site-footer')
  var w = $(window)
  var titles = sidebar.find('dl > dd > a')

  var isCacheKey = function(hash){
    return hash.lastIndexOf('_key') == (hash.length - 4)
  }
  var initHashScroll = function () {
    // first time scroll
    var hash = window.location.hash.toLowerCase()
    if (!!hash) {
      window.location.hash = ''
      setTimeout(function () {
        window.history.pushState(null, null, hash)
        scrollTo($(hash))
      }, 1)
    }

    // click #hash to scroll
    $('a[href^="#"]').click(function (e) {
      e.preventDefault()
      var target = $(this)
      // if(!target.hasClass('trigger')){
        var hash = target.attr('href').toLowerCase()
        var targetDom = null
        if(isCacheKey(hash)){
          targetDom = target
        }else{
          targetDom = $(hash)
        }
        scrollTo(targetDom, function() {
          window.history.pushState(null, null, hash)
        })
      // }
    })
    var openHashTag = function(hash, callback){
      if(isCacheKey(hash)){
        // pass
      }else if(hash.lastIndexOf('_inline') != (hash.length - 7)){
        hash += '_inline'
      }
      var targetModuleDom = $('a[href="' + hash + '"]')
      function doClick(){
        setTimeout(
            function(){
              targetModuleDom.click()
            }, 400
          )
      }
      if(targetModuleDom && targetModuleDom.length > 0){
        var preDocs = targetModuleDom.parents('.inline-doc')
        if(preDocs && preDocs.length > 0){
          var preId = preDocs.eq(0).attr('id')
          openHashTag('#' + preId, doClick)
        }else{
          doClick()
          callback && callback()
        }
      }
    }
    openHashTag(hash)
    $(window).bind('hashchange', function(e){
      var hash = window.location.hash.toLowerCase()
      openHashTag(hash)
    })
  }

  var initSidebar = function () {
    // fix & resize sidebar
    window.addEventListener('scroll', function () {
      if (w.scrollTop() > 60) {
        sidebar.addClass('fixed')
      } else if (sidebar.hasClass('fixed')) {
        sidebar.removeClass('fixed')
      }
      resizeSidebar()
    }, false)
    window.addEventListener('resize', function () {
      resizeSidebar()
    }, false)
    resizeSidebar()
  }

  var initSubMenuClick = function () {
    // sub menu toggle click event
    // 交互规则（一级菜单 sub-toggle 点击）：
    //   1. 已展开(active) + 当前页不是该菜单主页 → 跳转到主页（保持展开，不折叠）
    //   2. 已展开(active) + 当前页就是该菜单主页 → 折叠（移除 active）
    //   3. 未展开 → 展开 + 跳转到主页
    // 特殊：data-href 为空（该一级菜单本身无主页）→ 已展开时直接折叠，未展开时直接展开
    $('.site-sidebar .sub-toggle').on('click', function (e) {
      var target = $(this)
      var href = target.attr('data-href')
      var hasHref = !!(href && href.length)

      // 判断"当前页是否就是该菜单主页"：比较 pathname 结尾（兼容相对/绝对路径）
      var isCurrentPage = false
      if (hasHref) {
        var currentPath = window.location.pathname
        if (currentPath.endsWith(href) || window.location.href.endsWith(href)) {
          isCurrentPage = true
        }
      }

      if (target.hasClass('active')) {
        // 已展开
        if (!hasHref || isCurrentPage) {
          // 场景 2 / 无主页：折叠
          target.removeClass('active')
        } else {
          // 场景 1：当前页在子项 → 仅跳转到主页，不折叠
          window.location.href = href
        }
      } else {
        // 未展开
        target.addClass('active')
        if (hasHref) {
          // 场景 3：展开 + 跳转
          window.location.href = href
        }
        // 无主页：仅展开
      }
    })
  }

  // init sidebar scroll poistion to active title
  var SIDEBAR_SCROLL_KEY = 'docSidebarScrollTop'

  // 只在"从左栏点击链接"时记录侧栏滚动位置。
  // 用捕获阶段的 click，早于 .sub-toggle 那个 click 处理器里的同步跳转。
  // 不使用 pagehide/beforeunload：那会覆盖所有卸载场景（含正文/TOC 跳转），
  // 导致无法区分"左栏跳转要保持位置" vs "其他跳转要居中定位到当前项"
  if (sidebar.length && sidebar[0]) {
    sidebar[0].addEventListener('click', function (e) {
      var t = e.target
      while (t && t !== sidebar[0]) {
        if (t.tagName === 'A') {
          try {
            sessionStorage.setItem(SIDEBAR_SCROLL_KEY, String(sidebarInner.scrollTop()))
          } catch (err) {}
          return
        }
        t = t.parentNode
      }
    }, true)
  }

  var initSidebarScroll = function () {
    // 第一步：识别当前高亮项，并给它的所有父级 sub-toggle 加 active，
    // 从而展开对应的二级折叠列表（这一步无论是否有缓存滚动位置都要执行）
    if(currentTitle.length > 0){
      currentTitle = currentTitle.eq(0)
    }else{
      var mainTitles = $('.sub-toggle')
      for(var i = 0; i < mainTitles.length; i++){
        if(location.href.endsWith(mainTitles.eq(i).attr('data-href'))){
          currentTitle = mainTitles.eq(i)
          currentTitle.addClass('active')
          break
        }
      }
    }

    var parentMainTitles = currentTitle.parents('.sub-menu')
    for(var i = 0; i < parentMainTitles.length; i++){
      var parentSubTitle = parentMainTitles.eq(i).siblings('.sub-toggle')
      if(parentSubTitle){
        parentSubTitle.addClass('active')
      }
    }

    // 第二步：优先使用记录的滚动位置（保持点击前的位置，实现"位置不变"的效果）
    var savedTop = null
    try {
      savedTop = sessionStorage.getItem(SIDEBAR_SCROLL_KEY)
    } catch (e) {}
    if (savedTop !== null) {
      try { sessionStorage.removeItem(SIDEBAR_SCROLL_KEY) } catch (e) {}
      var targetTop = parseInt(savedTop, 10) || 0

      // 立即恢复一次
      sidebarInner.scrollTop(targetTop)

      // 兜底：有其他代码（如 initHashScroll 触发的锚点跳转、jQuery 事件处理器等）
      // 会在 initSidebarScroll 之后同步或异步地把 sidebar 的 scrollTop 改回 0。
      // 用 rAF + 短延迟多次覆盖，确保最终位置是我们想要的目标值。
      window.requestAnimationFrame(function () {
        sidebarInner.scrollTop(targetTop)
      })
      setTimeout(function () {
        sidebarInner.scrollTop(targetTop)
      }, 100)
      setTimeout(function () {
        sidebarInner.scrollTop(targetTop)
      }, 400)
      return
    }

    // 没有记录时（跳转来源不是左栏，例如从正文/TOC/外链跳转、或直接打开链接/刷新）：
    // 把当前高亮项滚动到侧栏可视区域垂直居中位置
    if (currentTitle[0] == sidebarInner.find('a')[0]) return false;
    var sidebarHeight = sidebarInner.height()
    var itemTop = currentTitle.position().top
    var itemHeight = currentTitle.outerHeight()
    // 目标 scrollTop：让当前项中心对齐侧栏可视区域中心
    var top = itemTop - (sidebarHeight - itemHeight) / 2
    if (top < 0) top = 0
    var targetTop = top
    // 立即设置 + 兜底覆盖（防止被其他 JS 改回 0）
    sidebarInner.scrollTop(targetTop)
    window.requestAnimationFrame(function () {
      sidebarInner.scrollTop(targetTop)
    })
    setTimeout(function () {
      sidebarInner.scrollTop(targetTop)
    }, 100)
    setTimeout(function () {
      sidebarInner.scrollTop(targetTop)
    }, 400)
  }

  // make .article table scrollable
  var initArticleTable = function () {
    var table = $('.article table').not('.highlight table')
    table.wrap('<div class="table-wrap"></div>')
  }

  // helpers
  var resizeSidebar = function() {
    // vertical height
    var marginB = 40
    var winScroll = w.scrollTop()
    var newH = 0
    var winHeight = w.height()
    var docHeight = $('.documentation-page .site-main').height()
    var footerArise = winHeight - (footer.position().top - winScroll)
    if (document.documentElement.scrollHeight < document.documentElement.clientHeight) {
      newH = $('body').height() - sidebarInner.position.top - marginB
    } else {
      if (sidebar.hasClass('fixed')) {
        newH = winHeight - sidebarInner.position().top - sidebar.position().top - marginB
      } else {
        newH = winHeight - sidebarInner.position().top + winScroll - marginB
      }
      if (footerArise >= 0) {
        newH -= footerArise
      }
    }
    sidebarInner.css({
      height: newH
    })

    // horizontal position
    if (sidebar.hasClass('fixed')) {
      sidebar.css({
        transform: 'translateX(-'+w.scrollLeft()+'px)'
      })
    } else {
      sidebar.css({
        transform: 'translateX(0px)'
      })
    }
  }

  var scrollTo = function (target, cb) {
    if(target.length > 0) {
      cb = cb || function() {}
      $('html, body').stop().animate({scrollTop: Math.ceil($(target).offset().top - 50)}, cb);
    }
  }

  function placeholder() {
    if ($('<input type="text">')[0].placeholder !== undefined) return;

    $('#search-label').click(function() {
        $('#search').focus();
        $('#search-label').hide();
    });

    $('#search').focus(function() {
        $('#search-label').hide();
    });
    $('#search').blur(function() {
        this.value == '' && $('#search-label').show()
    });

    $('#search')[0].value == '' && $('#search-label').show();
  }
  function getUrlParam(name){
    var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
    var r = window.location.search.substr(1).match(reg);
    // 读取端使用 decodeURIComponent（UTF-8 安全）；unescape 已废弃且按 Latin-1 解码，
    // 会把中文搜索词变成乱码。同时将 + 还原为空格（URL 编码约定）
    if (r != null && r != 'undefined') return decodeURIComponent(r[2].replace(/\+/g, ' ')); return null;
  }

  $(function() {
      placeholder();
      var tree = [];
      // var searchKey = getUrlParam('search')
      $('#links').hide();
      panel = new Searchdoc.Panel($('#panel'), search_data, tree, top.frames[1]);
      $('#search').focus();

      // var s = window.parent.location.search.match(/\?q=([^&]+)/);
      var s = getUrlParam('search'), index = parseInt(getUrlParam('i'))
      if (s) {
          // s = decodeURIComponent(s[1]).replace(/\+/g, ' ');
          if (s.length > 0)
          {
            $('#search').val(s);
            panel.search(s, false, index);
          }
      }
      setTimeout(function(){
        initHashScroll()
      }, 300)
      $('.clean-search').on('click', function(){
        $('#search').val('');
        panel.search('', false, 0);
      })
  })

  // sidebarInner.css({ height: 0 })

  initSubMenuClick()
  // initSidebar()
   initSidebarScroll()
  // initArticleTable()

  // 右侧 TOC 是 fixed 定位（sidebar 用 CSS sticky，无需 JS 调整）。
  // 根据窗口滚动位置动态调整 TOC 的 top / height：
  //   - top：窗口 scrollTop <= 60px → 110px；> 60px → 50px
  //   - height：默认 100vh - top - 20，但若 footer 已进入视口则再向上收缩，避免与 footer 重叠
  var $tocPanel = $('.site-toc')
  function updateTocTop() {
    if (!$tocPanel.length) return
    var scrollTop = $(window).scrollTop()
    var top = scrollTop > 60 ? 50 : 110
    var winH = window.innerHeight
    var maxH = winH - top - 20  // 距底部 20px 呼吸空间

    // 检测 footer 是否进入视口
    var $footer = $('.site-footer').first()
    if ($footer.length) {
      var footerRect = $footer[0].getBoundingClientRect()
      if (footerRect.top < winH) {
        // footer 已进入视口：TOC 底部 = footer 顶部 - 16px 间距
        var availH = footerRect.top - top - 16
        if (availH < 160) availH = 160  // 最小高度兜底
        maxH = Math.min(maxH, availH)
      }
    }

    $tocPanel.css({ top: top + 'px', height: maxH + 'px' })
  }
  $(window).on('scroll resize', updateTocTop)
  updateTocTop()
})()


;(function() {
  // 取消折叠：.trigger 不再绑定点击折叠，成员内容始终平铺展开
  // （保留 .trigger 元素本身，供锚点定位使用，但阻止其默认跳转与折叠行为）
  $('.trigger').each(function(idx, trigger) {
    $(trigger).on('click', function (e) {
      e.preventDefault()
    })
  })
})()


// 平铺展开 + 右侧本页目录(TOC)
;(function() {
  if (!$('.documentation-page').length) return false

  // 1. 默认展开所有 inline-doc（平铺阅读）
  $('.inline-doc').css('display', 'block')

  // 方案A：隐藏"有展开框"的顶层成员行，避免与展开框内 <h6> 标题重复；
  // 保留可跳转外链行作为唯一入口。
  $('.site-main .api-row').each(function() {
    var $row = $(this)
    if ($row.parents('.inline-doc').length) return
    var $trigger = $row.find('.api-col.right a.trigger')
    if ($trigger.length) {
      var href = $trigger.attr('href') // #xxx_inline
      if (href && $(href).length) {
        $row.addClass('api-row-hidden')
      }
    }
  })

  // 标记可跳转行（含外链 API，非本页展开项），便于高亮区分
  $('.site-main .api-row').each(function() {
    var $row = $(this)
    if ($row.parents('.inline-doc').length) return
    var $link = $row.find('.api-col.right a:not(.trigger)')
    if ($link.length) {
      $row.addClass('api-row-link')
      var href = $link.eq(0).attr('href')
      // 整行可点跳转
      $row.on('click', function(e) {
        if (e.target.tagName.toLowerCase() === 'a') return
        if (href) window.location.href = href
      })
    }
  })

  // @Deprecated 检测：inline-doc 内的代码块若含 "@Deprecated" 文本，
  // 给对应的 .api-row 和 inline-doc 打上 .api-deprecated 类。
  // 同时：把描述里的 <span class="deprecated-reason">...</span> 抽出，
  // 追加到顶部标题 (h6) 旁作为红色警示标签
  ;(function () {
    $('.site-main .inline-doc').each(function () {
      var $doc = $(this)
      var deprecated = false
      $doc.find('pre, code, .highlight').each(function () {
        if (/@Deprecated\b/.test($(this).text())) {
          deprecated = true
          return false
        }
      })
      if (!deprecated) return
      $doc.addClass('api-deprecated')
      var id = $doc.attr('id')
      if (id) {
        var rowId = id.replace(/_inline$/, '')
        $('#' + rowId).addClass('api-deprecated')
      }
      // 抽取 .deprecated-reason 并追加到展开框顶部标题 h6 后
      // 注意：hexo 生成的 inline-doc 起始 tag 没闭合，导致 blockquote 有时是 inline-doc
      // 的兄弟而非子节点。用多重回退保证一定能找到目标位置。
      var $reason = $doc.find('.deprecated-reason').first()
      // 若 inline-doc 内找不到，尝试从紧邻 inline-doc 之后的兄弟节点里找
      if (!$reason.length) {
        $reason = $doc.nextUntil('.api-row, .inline-doc').find('.deprecated-reason').first()
      }
      if ($reason.length) {
        var $badge = $reason.clone()
          .removeClass('deprecated-reason')
          .addClass('deprecated-reason-badge')
        // 找 h6：先在 inline-doc 内找，找不到就在紧邻兄弟里找
        var $h6 = $doc.find('h6').first()
        if (!$h6.length) {
          $h6 = $doc.nextUntil('.api-row, .inline-doc').filter('blockquote').find('h6').first()
        }
        if (!$h6.length) {
          $h6 = $doc.nextUntil('.api-row, .inline-doc').find('h6').first()
        }
        if ($h6.length) {
          // 把 h6 里的纯文本节点包一层 <span class="h6-text">，
          // 便于 CSS 单独给它加删除线（避免 h6 整体删除线跨越徽章）
          $h6.contents().each(function () {
            if (this.nodeType === 3 && $.trim(this.nodeValue).length) {
              $(this).replaceWith($('<span class="h6-text"></span>').text(this.nodeValue))
            }
          })
          $h6.append(' ').append($badge)
        } else {
          // 最后兜底：直接放到 reason 原位置的父段落之前
          $reason.parent().before($badge)
        }
        $reason.remove()
      }
    })
  })()

  // 正文结构重排：
  //   [类成员:]   → 所有 method 行 + 对应 inline-doc
  //   [相关类:]   → 所有 class 行（就地展开为类似 inline-doc 的 fetch 展开框） + 所有 enum 行 + 对应 inline-doc
  //
  // 关键决策变更（Leader Review）：
  //   1. 撤销 class 网格（.related-classes-grid）—— class 不再抽到顶部
  //   2. class 就地保留为 .api-row-link，仍可点击跳转到独立页
  //   3. 每个 class 行下方 fetch 独立页内容，构造成图2 样式的展开框（顶部标题 + 代码 + 描述块）
  //   4. TOC 里 class 作为普通条目出现在"相关类"组（不再有"class 类成员"总锚点）
  //
  // 特殊页面（如 DJIValue）：作者用 <h4> 自定义了主题分组标题（Gimbal / Camera / Common ...），
  // 判定策略：如果 article 里存在 <h4>，视为"手工分组页"，跳过 enum 迁移和"相关类"标题创建。
  ;(function () {
    var $article = $('.site-main .article').eq(0)
    if (!$article.length) return

    // 收集顶层 api-row（不含 inline-doc 内部的嵌套行）
    var $allRows = $('.site-main .api-row').filter(function () {
      return !$(this).parents('.inline-doc').length
    })
    if (!$allRows.length) return

    var $classRows = $allRows.filter('.api-row-link') // class 跳转卡片
    var $enumRows = $allRows.filter(function () {
      var t = $.trim($(this).find('.api-col.middle').text()).toLowerCase()
      return t === 'enum'
    })

    // 是否为"手工分组页"（作者用 h4 自建分组标题）
    // 判定排除两类非分组 h4：
    // 1. inline-doc 内部的 h4（卡片内的子标题）
    // 2. 成员区类标题（如 ICameraStreamManager 相关类型区 PinPoint 卡片后的
    //    <h4>Members</h4>，属于成员子标题而非业务分组，误判会导致
    //    custom-grouped-page 误加、keepCustomToc 误生效、TOC 回退手工分组）
    // 3. 页面含 method 成员时不算手工分组页——纯 class/enum 聚合页（如 DJIValue）
    //    才是类型 B；含 method 的管理页（如 IFlyZoneManager）其 h4 仅为成员区
    //    业务分组标题，应走标准 ifm（类型 A），否则 TOC 会错误保留手工分组
    var hasCustomH4Groups = $article.find('h4').filter(function () {
      if ($(this).parents('.inline-doc').length) return false
      if (/^(members|类成员|class members)\s*:?\s*$/i.test($.trim($(this).text()))) return false
      return true
    }).length > 0
    if (hasCustomH4Groups) {
      var hasMethodRow = $allRows.filter(function () {
        return $.trim($(this).find('.api-col.middle').text()).toLowerCase() === 'method'
      }).length > 0
      if (hasMethodRow) hasCustomH4Groups = false
    }
    if (hasCustomH4Groups) {
      $article.addClass('custom-grouped-page')
      // 手工分组页：不做 enum 迁移，保留原文档 h4 分组结构。
      // 但 class 仍然要走 fetch 展开框注入（与其他页面统一），确保 class 内容可读。
      injectClassInlineDocs()
      return
    }

    // 收集 class + enum 行（连同紧随其后的 inline-doc）作为"相关类"迁移片段。
    // 必须单轮遍历 $allRows（DOM 文档顺序）收集：hexo 畸形 DOM 下各行父元素可能不同，
    // 分两轮收集（先 enum 后 class）或用 $row.index()（兄弟内序号，跨父元素不可比）
    // 都会丢失 YAML 文档的交错顺序，导致相关类型区与 TOC 退化为 class 块 + enum 块
    var relatedFragments = []
    $allRows.each(function () {
      var $row = $(this)
      var t = $.trim($row.find('.api-col.middle').text()).toLowerCase()
      if (t === 'enum') {
        // enum：行 + 紧随其后的 inline-doc 打包
        var $frag = $row
        var $next = $row.next()
        if ($next.hasClass('inline-doc')) {
          $frag = $frag.add($next)
        }
        relatedFragments.push({ type: 'enum', $frag: $frag.detach() })
      } else if (t === 'class') {
        // class：单行；离线构建已预置 class-inline-doc 时随行一起迁移
        var rowId = $row.attr('id') || ''
        var $fragC = $row
        var $prebuilt = rowId ? $(document.getElementById(rowId + '_inline')) : $()
        if ($prebuilt.length) $fragC = $fragC.add($prebuilt)
        relatedFragments.push({ type: 'class', $frag: $fragC.detach() })
      }
    })

    // 若没有相关类内容，直接结束
    if (!relatedFragments.length) return

    // 移除原文档里的 "Related:" h5
    $('.site-main .article h5').each(function () {
      var txt = $.trim($(this).text())
      if (/^(相关类|Related)[:：]?$/i.test(txt)) {
        $(this).remove()
      }
    })

    // 追加"相关类:"标题 + 相关类内容
    var $relatedH5 = $('<h5 id="related-title">相关类:</h5>')
    $article.append($relatedH5)
    // 按 YAML 文档原始顺序追加（不再 class 整块在前、enum 整块在后；
    // TOC 相关类型组沿用该 DOM 顺序，与 YAML 文档排序一致）
    relatedFragments.forEach(function (f) { $article.append(f.$frag) })

    // ===== class 展开框 fetch 注入 =====
    // 为每个 class 行 fetch 其独立页 HTML，抽取 h1 定义代码块 + 描述段落，
    // 构造成图2 样式的展开框插入到 class 行下方
    injectClassInlineDocs()

    // 页面级特化增强改放到文件末尾（所有 IIFE 之后）执行，
    // 避免被后续 "平铺展开" 逻辑覆盖
  })()

  // 页面级特化增强入口：按 URL 分派
  // 挂到 window 以便文件末尾 IIFE 之后调用（确保跑在 "平铺展开" 逻辑之后）
  window.__enhancePageSpecific = function enhancePageSpecific() {
    var path = (window.location.pathname || '').toLowerCase()

    if (!/\/components\//.test(path)) return

    // ===== IIntelligentFlightManager：敲定版本，专属 workflow + interface kind =====
    if (/\/iintelligentflightmanager\.html$/.test(path)) {
      enhanceManagerLikePage({
        kind: 'interface',
        breadcrumb: [
          { name: 'Common APIs', href: null },
          { name: 'IIntelligentFlightManager', href: null }
        ],
        workflow: {
          note: 'Follow / Focus / Orbit / Fly To are executed via the corresponding sub-Managers',
          steps: [
            { num: '01', title: 'Monitor flight status', desc: 'addIntelligentFlightInfoListener obtains supported intelligent flight features and the current running state' },
            { num: '02', title: 'Enable auto sensing', desc: 'startAutoSensing + addAutoSensingInfoListener detects people / vehicles / boats in the view' },
            { num: '03', title: 'Get sub-Manager', desc: 'Select the SmartTrack / SpotLight / POI / FlyTo task manager by business scenario' },
            { num: '04', title: 'Execute & control', desc: 'Execute via the sub-Manager, stop when done, then stopAutoSensing to release' }
          ]
        }
      })
      return
    }

    // ===== DJIValue：手工分组页专属入口（类型 B）=====
    // 保留手工分组 TOC（h4 + subGroup 按文档顺序），面包屑与全局推导格式一致
    if (/\/djivalue\.html$/.test(path)) {
      enhanceManagerLikePage({
        kind: 'class',
        breadcrumb: [
          { name: 'Common APIs', href: null },
          { name: 'IKeyManager', href: 'IKeyManager.html' },
          { name: 'DJIValue', href: null }
        ],
        keepCustomToc: true
      })
      return
    }

    // ===== 全局默认：Components 下所有页面统一 ifm 化 =====
    // 类型 B（手工分组页）由 getGlobalIfmConfig 内根据
    // custom-grouped-page 类（article 内存在业务 h4 时由前置 IIFE 添加）自动判定 keepCustomToc；
    // 旧定制页（IWaypointMissionManager / IARManager / IRemoteViewPageManager）
    // 的专属逻辑已移除，统一走标准 ifm 流程
    enhanceManagerLikePage(getGlobalIfmConfig(path))
  }

  // 全局 ifm 化：按页面名返回 config（Manager 类页面有 workflow）
  // 适用 Components 下所有页面；IMediaDataCenter 系列行为与原专用配置完全一致
  function getGlobalIfmConfig(path) {
    // 用原始 pathname 推导页面名与目录名：文件名是驼峰（如 ICameraStreamManager.html）。
    // 若用 toLowerCase 后的路径推导，面包屑会拼出全小写链接——在大小写敏感的
    // 线上服务器（Linux）上会 404（macOS 本地大小写不敏感，预览时难以发现）。
    var rawPath = window.location.pathname || ''
    var rawPageName = rawPath.split('/').pop().replace(/\.html$/i, '')
    var dirName = rawPath.split('/').slice(-2, -1)[0] || ''
    var pageName = rawPageName.toLowerCase()

    // ===== 面包屑：优先从 sidebar 树推导（站点层级权威）=====
    // sidebar 的层级（dt 分类 → dd 一级节点 → ul.sub-menu 嵌套子项）与实际文档结构一致，
    // 且显示名更友好（如 ISDKManager 目录的根页面文件名是 DJISDKManager.html，
    // sidebar 一级节点显示名却是 ISDKManager；子项显示名是 APPVersion 而非 ISDKManager_APPVersion）。
    // 依赖"目录名 === 根页面文件名"的旧规则在这种目录会拼出错误层级与 404 链接。
    // 当前页节点特征（hexo 模板）：叶子页 <a href="" class="active">；
    // 分支根页 <a data-href="" class="sub-toggle">（如 DJISDKManager.html 对应 ISDKManager 节点）。
    var crumbs = null
    var $sidebar = $('.site-sidebar')
    if ($sidebar.length) {
      // 注意：运行时主题 JS 会给「展开链路」上的所有节点都加 active
      //（如 ICameraStreamManager 页：IMediaDataCenter 与 ICameraStreamManager 两个 active），
      // 当前页是链路最末端，必须取 .last()；取 .first() 会丢失最后一级当前页
      var $cur = $sidebar.find('a.active').last()
      if (!$cur.length) {
        $cur = $sidebar.find('a.sub-toggle').filter(function () {
          return !$(this).attr('data-href') && !$(this).attr('href')
        }).first()
      }
      if ($cur.length) {
        var chain = []
        // 自底向上收集 dd 层级（parents 返回最近→最远，reverse 后为根→叶）
        $($cur.parents('dd').get().reverse()).each(function () {
          var $a = $(this).children('a').first()
          var txt = $a.length ? $.trim($a.clone().children('i').remove().end().text()) : ''
          if (txt) chain.push({ name: txt, href: $a.attr('data-href') || $a.attr('href') || null })
        })
        // dt 分类前缀（dt 与 dd 平级，不在 dd 祖先链上，单独取）
        var dtTxt = $.trim($cur.closest('dl').children('dt').first().text())
        if (dtTxt) chain.unshift({ name: dtTxt, href: null })
        // 最后一级为当前页，不可再点击
        if (chain.length > 1) {
          chain[chain.length - 1].href = null
          crumbs = chain
        }
      }
    }

    // ===== fallback：sidebar 未命中时按目录名 + _ 派生层级 =====
    // href 使用同目录相对路径（与全站 hexo relative_link 策略一致）：
    // file:// 本地预览、根路径部署、子路径部署均可正常跳转；
    // 绝对路径 /Components/... 在 file:// 下会丢失盘符指向文件系统根。
    if (!crumbs) {
      var segments = rawPageName.split('_')
      crumbs = [{ name: 'Common APIs', href: null }]
      var isRootPage = pageName === dirName.toLowerCase()
      if (isRootPage) {
        // URL 文件名可能被浏览器规范化为小写；根页始终只显示一次标准名称。
        crumbs.push({ name: dirName, href: null })
      } else {
        crumbs.push({ name: dirName, href: dirName + '.html' })
        var segHref = ''
        segments.forEach(function (seg, i) {
          segHref = i === 0 ? seg : segHref + '_' + seg
          crumbs.push({ name: seg, href: segHref + '.html' })
        })
      }
    }

    // 类型 B（手工分组页）：前置 IIFE 已判定并添加 custom-grouped-page 类
    //（article 内存在 h4 分组标题，如 DJIValue），保留全局手工分组 TOC
    var keepCustomToc = $('.site-main .article').first().hasClass('custom-grouped-page')

    var WORKFLOWS = {
      'IMediaDataCenter.html': {
        note: 'Select the corresponding sub-Manager by business scenario',
        steps: [
          { num: '01', title: 'Get entry instance', desc: 'Get the IMediaDataCenter instance via KeyTools' },
          { num: '02', title: 'Stream management', desc: 'getCameraStreamManager displays camera stream, obtains stream and frame data (recommended)' },
          { num: '03', title: 'Media files', desc: 'getMediaManager pulls the file list, preview, download and video playback' },
          { num: '04', title: 'Live streaming', desc: 'getLiveStreamManager configures RTMP / RTSP / GB28181 / Agora live streaming' }
        ]
      },
      'ICameraStreamManager.html': {
        note: 'CameraStreamManager abstracts VideoStreamManager and hides the underlying channel concept',
        steps: [
          { num: '01', title: 'Monitor available cameras', desc: 'addAvailableCameraUpdatedListener obtains the available camera index' },
          { num: '02', title: 'Display stream', desc: 'putCameraStreamSurface renders the camera stream to a Surface' },
          { num: '03', title: 'Receive stream data', desc: 'addReceiveStreamListener receives stream data for self-decoding or third-party live streaming' },
          { num: '04', title: 'Receive frame data', desc: 'addFrameListener receives decoded frame data for AI algorithm processing' }
        ]
      },
      'IMediaManager.html': {
        note: 'Camera cannot take photos or record in file management mode; restored after exit',
        steps: [
          { num: '01', title: 'Enter file management', desc: 'enable enters media file management mode (camera cannot take photos or record)' },
          { num: '02', title: 'Monitor list state', desc: 'addMediaFileListStateListener obtains the media file list state' },
          { num: '03', title: 'Pull file list', desc: 'setMediaFileDataSource + pullMediaFileListFromCamera, then getMediaFileListData when the state is UP_TO_DATE' },
          { num: '04', title: 'File operations', desc: 'deleteMediaFiles / playVideoToSurface / download; call disable to exit' }
        ]
      },
      'ILiveStreamManager.html': {
        note: 'Supports RTMP / RTSP / GB28181 / Agora live streaming protocols',
        steps: [
          { num: '01', title: 'Configure live settings', desc: 'setLiveStreamSettings sets protocol, quality, bitrate mode, etc.' },
          { num: '02', title: 'Set live camera', desc: 'setCameraIndex specifies the camera index for live streaming' },
          { num: '03', title: 'Start streaming', desc: 'startStream starts pushing the stream' },
          { num: '04', title: 'Monitor & control', desc: 'addLiveStreamStatusListener monitors state; setLiveStreamQuality adjusts; stopStream stops' }
        ]
      },
      'IVideoStreamManager.html': {
        note: 'Deprecated since MSDK 5.8.0. Use ICameraStreamManager instead',
        steps: [
          { num: '01', title: 'Get stream sources', desc: 'getAvailableStreamSources / addStreamSourcesListener obtains available stream sources' },
          { num: '02', title: 'Get video channels', desc: 'getAvailableVideoChannels obtains available video channels' },
          { num: '03', title: 'Bind and start', desc: 'IVideoChannel.startChannel binds the stream source and starts the channel' },
          { num: '04', title: 'Decode & display', desc: 'addStreamDataListener for self-decoding / IVideoDecoder DJI decoder rendering' }
        ]
      },
      'IVideoStreamManager_IVideoChannel.html': {
        note: 'Each video channel can bind one stream source for data transmission',
        steps: [
          { num: '01', title: 'Start channel', desc: 'startChannel binds the stream source and starts stream transmission' },
          { num: '02', title: 'Monitor channel state', desc: 'addVideoChannelStateChangeListener obtains the channel state' },
          { num: '03', title: 'Receive stream data', desc: 'addStreamDataListener receives stream data for self-decoding' },
          { num: '04', title: 'Close channel', desc: 'closeChannel stops receiving stream data' }
        ]
      },
      'IVideoStreamManager_IVideoDecoder.html': {
        note: 'Supports YUV and SURFACE output modes',
        steps: [
          { num: '01', title: 'Construct decoder', desc: 'constructor specifies channel type, output mode (YUV / SURFACE), Surface' },
          { num: '02', title: 'Monitor state', desc: 'addDecoderStateChangeListener obtains the decoder state' },
          { num: '03', title: 'Decode & play', desc: 'SURFACE mode renders automatically / YUV mode addYuvDataListener obtains YUV data' },
          { num: '04', title: 'Lifecycle', desc: 'onPause / onResume to pause and resume; destory to release resources' }
        ]
      }
    }
    // WORKFLOWS 键为驼峰文件名，pageName 来自 URL 可能是小写：
    // 用大小写不敏感方式查找，否则 7 个 Manager 页面的典型调用流程永远匹配不到
    var workflow = null
    Object.keys(WORKFLOWS).some(function (key) {
      if (key.toLowerCase() === pageName + '.html') {
        workflow = WORKFLOWS[key]
        return true
      }
      return false
    })
    return { breadcrumb: crumbs, workflow: workflow, keepCustomToc: keepCustomToc }
  }


  // ============================================================
  // 通用 Manager 风格页面增强（基于 IIntelligentFlightManager 风格）
  // config: { className?, kind?, workflow? }
  // ============================================================
  // 复制文本到剪贴板：优先 clipboard API，file:// 等非安全上下文回退 execCommand；
  // 反馈直接作用在按钮上（成功变绿勾，失败变红叉），避免依赖 toast 组件
  function ifmCopyText(text, $btn) {
    var $svg = $btn.find('svg').first()
    var origHtml = $svg.html()
    var restoreTimer = null
    function done(ok) {
      $btn.toggleClass('ifm-copied', ok).toggleClass('ifm-copy-fail', !ok)
      $svg.html(ok ? '<path d="M5 12l5 5 9-10"/>' : '<path d="M6 6l12 12M18 6 6 18"/>')
      if (restoreTimer) clearTimeout(restoreTimer)
      restoreTimer = setTimeout(function () {
        $btn.removeClass('ifm-copied ifm-copy-fail')
        $svg.html(origHtml)
      }, 1200)
    }
    function fallback() {
      try {
        var area = document.createElement('textarea')
        area.value = text
        area.style.position = 'fixed'
        area.style.opacity = '0'
        document.body.appendChild(area)
        area.select()
        var ok = document.execCommand('copy')
        area.remove()
        done(ok)
      } catch (_) { done(false) }
    }
    try {
      if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(text).then(function () { done(true) }, fallback)
      } else {
        fallback()
      }
    } catch (_) { fallback() }
  }

  function enhanceManagerLikePage(config) {
    config = config || {}
    var $article = $('.site-main .article').first()
    if (!$article.length) return
    $article.addClass('ifm-page')

    var escapeHtml = function (s) {
      return String(s).replace(/[&<>"']/g, function (c) {
        return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }[c]
      })
    }

    // ===== Java 签名语法着色（按语法角色识别，不做单词全局替换） =====
    // 颜色与 _documentation.scss 的 .jv-* 一致：
    // 关键字与类型 #85288F / 注解 #978725 / 方法名与类名 #3D689D /
    // 限定名前缀、形参名、标点 #454545
    var JAVA_KEYWORDS = {
      abstract: 1, assert: 1, boolean: 1, break: 1, byte: 1, case: 1, catch: 1, char: 1,
      class: 1, const: 1, continue: 1, default: 1, do: 1, double: 1, else: 1, enum: 1,
      extends: 1, final: 1, finally: 1, float: 1, for: 1, goto: 1, if: 1, implements: 1,
      import: 1, instanceof: 1, int: 1, interface: 1, long: 1, native: 1, new: 1,
      package: 1, private: 1, protected: 1, public: 1, return: 1, short: 1, static: 1,
      strictfp: 1, super: 1, switch: 1, synchronized: 1, this: 1, throw: 1, throws: 1,
      transient: 1, try: 1, void: 1, volatile: 1, while: 1, true: 1, false: 1, null: 1
    }

    function tokenizeJava(text) {
      var tokens = []
      var i = 0
      while (i < text.length) {
        var ch = text.charAt(i)
        var rest = text.slice(i)
        var m
        if (/\s/.test(ch)) {
          m = /^\s+/.exec(rest)
          tokens.push({ type: 'ws', text: m[0], start: i, end: i + m[0].length })
          i += m[0].length
        } else if (ch === '@' && (m = /^@[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)*/.exec(rest))) {
          tokens.push({ type: 'annotation', text: m[0], start: i, end: i + m[0].length })
          i += m[0].length
        } else if (/[A-Za-z_$]/.test(ch) && (m = /^[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)*/.exec(rest))) {
          tokens.push({ type: 'qualified', text: m[0], start: i, end: i + m[0].length })
          i += m[0].length
        } else if (/[0-9]/.test(ch) && (m = /^[0-9][\w.]*/.exec(rest))) {
          tokens.push({ type: 'literal', text: m[0], start: i, end: i + m[0].length })
          i += m[0].length
        } else if (ch === '"' || ch === "'") {
          var quote = ch
          var j = i + 1
          while (j < text.length) {
            if (text.charAt(j) === '\\') j += 2
            else if (text.charAt(j) === quote) { j++; break }
            else j++
          }
          tokens.push({ type: 'literal', text: text.slice(i, j), start: i, end: j })
          i = j
        } else if (text.slice(i, i + 3) === '...') {
          tokens.push({ type: 'punct', text: '...', start: i, end: i + 3 })
          i += 3
        } else {
          tokens.push({ type: 'punct', text: ch, start: i, end: i + 1 })
          i++
        }
      }
      for (var k = 0; k < tokens.length; k++) tokens[k].idx = k
      return tokens
    }

    function findJavaAnchor(start, end, anchors) {
      if (!anchors) return ''
      for (var i = 0; i < anchors.length; i++) {
        if (start >= anchors[i].start && end <= anchors[i].end) return anchors[i].href
      }
      return ''
    }

    // 限定名：最后一段用实际类型/方法色，前面的包名或外层类型用深灰，点号用标点色
    function renderQualifiedJava(text, lastClass, start, anchors) {
      var parts = String(text).split('.')
      var out = []
      var pos = (start == null) ? 0 : start
      for (var i = 0; i < parts.length; i++) {
        if (i > 0) {
          out.push('<wbr><span class="jv-punct">.</span>')
          pos += 1
        }
        var segStart = pos
        var segEnd = pos + parts[i].length
        var cls = (i === parts.length - 1) ? lastClass : 'jv-prefix'
        var html = '<span class="' + cls + '">' + escapeHtml(parts[i]) + '</span>'
        var href = findJavaAnchor(segStart, segEnd, anchors)
        if (href) html = '<a href="' + escapeHtml(href) + '">' + html + '</a>'
        out.push(html)
        pos = segEnd
      }
      return out.join('')
    }

    function renderJavaRole(token, anchors) {
      var text = escapeHtml(token.text)
      var role = token.role
      var html
      if (!role || role === 'plain' || role === 'ws') html = text
      else if (role === 'annotation') html = '<span class="jv-annotation">' + text + '</span>'
      else if (role === 'keyword') html = '<span class="jv-keyword">' + text + '</span>'
      else if (role === 'method') html = renderQualifiedJava(token.text, 'jv-method', token.start, anchors)
      else if (role === 'class-name') html = renderQualifiedJava(token.text, 'jv-class', token.start, anchors)
      else if (role === 'type') html = renderQualifiedJava(token.text, 'jv-type', token.start, anchors)
      else if (role === 'param') html = '<span class="jv-param">' + text + '</span>'
      else if (role === 'punct') html = '<span class="jv-punct">' + text + '</span>'
      else html = text
      return (token.wbrBefore ? '<wbr>' : '') + html
    }

    // 标记类型表达式的结构断点（对应 Javadoc 的 <wbr> 用法）：
    // 注解与类型之间、泛型 < 之前、泛型逗号之后、限定名 . 之前。
    function markJavaBreakOpportunities(tokens) {
      var angleDepth = 0
      var prevNonWs = null
      for (var i = 0; i < tokens.length; i++) {
        var t = tokens[i]
        if (t.type === 'ws') continue
        if (prevNonWs && prevNonWs.type === 'annotation') t.wbrBefore = true
        if (t.type === 'punct' && t.text === '<') t.wbrBefore = true
        if (prevNonWs && prevNonWs.type === 'punct' && prevNonWs.text === ',' && angleDepth > 0) t.wbrBefore = true
        if (t.type === 'punct' && t.text === '<') angleDepth++
        else if (t.type === 'punct' && t.text === '>') { if (angleDepth > 0) angleDepth-- }
        prevNonWs = t
      }
    }

    function findJavaSignatureStructure(tokens) {
      var depth = 0
      var paramsOpen = -1
      var paramsClose = -1
      var methodNameIdx = -1
      var declaration = false
      for (var i = 0; i < tokens.length; i++) {
        var t = tokens[i]
        if (t.type === 'annotation') {
          // 跳过注解参数 @Foo(...)，避免误判为方法参数
          var k = i + 1
          while (k < tokens.length && tokens[k].type === 'ws') k++
          if (k < tokens.length && tokens[k].text === '(') {
            var d = 1
            i = k
            while (i + 1 < tokens.length && d > 0) {
              i++
              if (tokens[i].text === '(') d++
              else if (tokens[i].text === ')') d--
            }
          }
          continue
        }
        if (t.type === 'punct' && t.text === '(') {
          if (depth === 0 && paramsOpen < 0) {
            var p = i - 1
            while (p >= 0 && tokens[p].type === 'ws') p--
            if (p >= 0 && tokens[p].type === 'qualified') {
              methodNameIdx = p
              paramsOpen = i
            }
          }
          depth++
        } else if (t.type === 'punct' && t.text === ')') {
          depth--
          if (depth === 0 && paramsOpen >= 0 && paramsClose < 0) paramsClose = i
        }
      }
      if (paramsOpen < 0) {
        for (var j = 0; j < tokens.length; j++) {
          if (tokens[j].type === 'qualified' &&
              (tokens[j].text === 'class' || tokens[j].text === 'interface' || tokens[j].text === 'enum')) {
            declaration = true
            break
          }
        }
      }
      return { methodNameIdx: methodNameIdx, paramsOpen: paramsOpen, paramsClose: paramsClose, declaration: declaration }
    }

    function assignJavaParamRoles(tokens, open, close) {
      var segments = []
      var current = []
      var paren = 0, angle = 0, bracket = 0, brace = 0
      for (var i = open + 1; i < close; i++) {
        var t = tokens[i]
        if (t.type === 'punct') {
          var txt = t.text
          if (txt === '(') paren++
          else if (txt === ')') paren--
          else if (txt === '<') angle++
          else if (txt === '>') { if (angle > 0) angle-- }
          else if (txt === '[') bracket++
          else if (txt === ']') bracket--
          else if (txt === '{') brace++
          else if (txt === '}') brace--
          else if (txt === ',' && paren === 0 && angle === 0 && bracket === 0 && brace === 0) {
            segments.push(current)
            current = []
            continue
          }
        }
        current.push(i)
      }
      if (current.length) segments.push(current)

      segments.forEach(function (idxs) {
        // 参数段中最后一个普通标识符是形参名，其余标识符是类型
        var nameIdx = -1
        for (var k = idxs.length - 1; k >= 0; k--) {
          var tk = tokens[idxs[k]]
          if (tk.type === 'ws' || tk.type === 'punct') continue
          if (tk.type === 'annotation') continue
          if (tk.type === 'qualified') { nameIdx = idxs[k]; break }
        }
        idxs.forEach(function (idx) {
          var tt = tokens[idx]
          if (tt.type === 'ws' || tt.role) return
          if (tt.type === 'annotation') { tt.role = 'annotation'; return }
          if (tt.type === 'literal') { tt.role = 'plain'; return }
          if (tt.type === 'punct') { tt.role = 'punct'; return }
          if (idx === nameIdx) { tt.role = 'param'; return }
          if (JAVA_KEYWORDS[tt.text]) { tt.role = 'keyword'; return }
          tt.role = 'type'
        })
      })
    }

    function assignJavaRoles(tokens, structure) {
      var i, t
      if (structure.methodNameIdx >= 0) {
        for (i = 0; i < structure.paramsOpen; i++) {
          t = tokens[i]
          if (t.type === 'ws' || t.role) continue
          if (t.type === 'annotation') t.role = 'annotation'
          else if (t.type === 'literal') t.role = 'plain'
          else if (t.type === 'punct') t.role = 'punct'
          else if (i === structure.methodNameIdx) t.role = 'method'
          else if (JAVA_KEYWORDS[t.text]) t.role = 'keyword'
          else t.role = 'type'
        }
        tokens[structure.paramsOpen].role = 'punct'
        tokens[structure.paramsClose].role = 'punct'
        assignJavaParamRoles(tokens, structure.paramsOpen, structure.paramsClose)
        for (i = structure.paramsClose + 1; i < tokens.length; i++) {
          t = tokens[i]
          if (t.type === 'ws' || t.role) continue
          if (t.type === 'annotation') t.role = 'annotation'
          else if (t.type === 'literal') t.role = 'plain'
          else if (t.type === 'punct') t.role = 'punct'
          else if (JAVA_KEYWORDS[t.text]) t.role = 'keyword'
          else t.role = 'type'
        }
      } else if (structure.declaration) {
        var afterTypeKeyword = false
        for (i = 0; i < tokens.length; i++) {
          t = tokens[i]
          if (t.type === 'ws' || t.role) continue
          if (t.type === 'annotation') { t.role = 'annotation'; continue }
          if (t.type === 'literal') { t.role = 'plain'; continue }
          if (t.type === 'punct') { t.role = 'punct'; continue }
          if (JAVA_KEYWORDS[t.text]) {
            t.role = 'keyword'
            // class / interface / enum 后的第一个标识符是声明名，保持蓝色
            if (t.text === 'class' || t.text === 'interface' || t.text === 'enum') afterTypeKeyword = true
            continue
          }
          if (afterTypeKeyword) {
            t.role = 'class-name'
            afterTypeKeyword = false
          } else {
            t.role = 'type'
          }
        }
      }
    }

    function colorizeJavaSignature($fig) {
      if ($fig.attr('data-jv-colored')) return
      var $lines = $fig.find('.line')
      if (!$lines.length) return
      var texts = []
      $lines.each(function () { texts.push($(this).text()) })
      var tokens = tokenizeJava(texts.join('\n'))
      var structure = findJavaSignatureStructure(tokens)
      // 只处理方法签名与 class/interface/enum 声明，避免影响字段/常量
      if (structure.methodNameIdx < 0 && !structure.declaration) return
      assignJavaRoles(tokens, structure)
      markJavaBreakOpportunities(tokens)

      // 按换行拆回每行 token，保留原有 .line 行结构
      var lineTokens = [[]]
      for (var i = 0; i < tokens.length; i++) {
        var t = tokens[i]
        if (t.type === 'ws' && t.text.indexOf('\n') >= 0) {
          var parts = t.text.split('\n')
          for (var p = 0; p < parts.length; p++) {
            if (p > 0) lineTokens.push([])
            if (parts[p]) lineTokens[lineTokens.length - 1].push({ type: 'ws', text: parts[p], role: 'ws', idx: -1 })
          }
        } else {
          lineTokens[lineTokens.length - 1].push(t)
        }
      }
      if (lineTokens.length === $lines.length + 1 && !lineTokens[lineTokens.length - 1].length) lineTokens.pop()
      if (lineTokens.length !== $lines.length) return

      var pOpen = structure.paramsOpen
      var pClose = structure.paramsClose
      $lines.each(function (lineIndex) {
        var html = []
        var opened = false
        var list = lineTokens[lineIndex]
        for (var k = 0; k < list.length; k++) {
          var tk = list[k]
          var isParam = (pOpen >= 0 && tk.idx >= pOpen && tk.idx <= pClose)
          if (isParam && !opened) { html.push('<span class="params">'); opened = true }
          if (!isParam && opened) { html.push('</span>'); opened = false }
          html.push(renderJavaRole(tk, null))
        }
        if (opened) html.push('</span>')
        $(this).html(html.join(''))
      })
      $fig.attr('data-jv-colored', '1')
    }

    function colorizeParamTypes($inline) {
      $inline.find('table.ifm-params-table').each(function () {
        var $tbl = $(this)
        if ($tbl.hasClass('ifm-enum-table')) return
        var isReturnTable = $tbl.hasClass('ifm-return-table')
        $tbl.find('tbody tr').each(function () {
          var $tds = $(this).children('td')
          var $typeCode = (isReturnTable ? $tds.eq(0) : $tds.eq(1)).find('code').first()
          if (!$typeCode.length || $typeCode.attr('data-jv-colored')) return
        var anchors = []
        var text = ''
        ;(function walk(node) {
          for (var c = node.firstChild; c; c = c.nextSibling) {
            if (c.nodeType === 3) {
              text += c.nodeValue
            } else if (c.nodeType === 1) {
              if (c.tagName === 'A') {
                var start = text.length
                text += c.textContent
                anchors.push({ start: start, end: text.length, href: c.getAttribute('href') || '' })
              } else {
                walk(c)
              }
            }
          }
        })($typeCode[0])

        var tokens = tokenizeJava(text)
        for (var i = 0; i < tokens.length; i++) {
          var t = tokens[i]
          if (t.type === 'ws') { t.role = 'ws'; continue }
          if (t.type === 'annotation') { t.role = 'annotation'; continue }
          if (t.type === 'literal') { t.role = 'plain'; continue }
          if (t.type === 'punct') { t.role = 'punct'; continue }
          t.role = JAVA_KEYWORDS[t.text] ? 'keyword' : 'type'
        }
        markJavaBreakOpportunities(tokens)
        var html = []
        for (var j = 0; j < tokens.length; j++) html.push(renderJavaRole(tokens[j], anchors))
          if (!$typeCode.parent().hasClass('ifm-type-scroll')) {
            $typeCode.wrap('<span class="ifm-type-scroll"></span>')
          }
          $typeCode.html(html.join(''))
          $typeCode.attr('data-jv-colored', '1')
        })
      })
    }

    // ===== 通用转换函数：将 inline-doc 转为 ifm-method-doc 卡片样式 =====
    function transformInlineDoc($inline, type, name) {
      if ($inline.hasClass('ifm-processed')) return
      $inline.addClass('ifm-method-doc ifm-processed')

      // class-inline-doc 由异步请求填充，无法被页面初始化时的全局
      // @Deprecated 扫描覆盖；在转换时补充废弃状态。
      if (/@Deprecated\b/.test($inline.find('figure.highlight, pre, code').text())) {
        $inline.addClass('api-deprecated')
        var inlineId = $inline.attr('id') || ''
        if (inlineId) $('#' + inlineId.replace(/_inline$/, '')).addClass('api-deprecated')
      }

      var $h6 = $inline.find('h6').first()
      var displayName = name || $.trim($h6.text().replace(/^(method|enum|class)\s+/i, ''))

      // 提取版本
      var memberSince = ''
      var $memberTag = $inline.find('.msdk-support-tag code')
      if ($memberTag.length) {
        var sm = $memberTag.text().match(/MSDK\s+([\d.]+)/)
        if (sm) memberSince = sm[1]
      }
      if (!memberSince) {
        $inline.find('code').each(function () {
          var m2 = $(this).text().match(/MSDK\s+([\d.]+)\s*开始支持/) || $(this).text().match(/Supported\s+since\s+MSDK\s*([\d.]+)/i)
          if (m2) { memberSince = m2[1]; return false }
        })
      }
      var sinceBadge = memberSince ? '<span class="ifm-badge ifm-since-badge">Since MSDK ' + memberSince + '</span>' : ''
      // 废弃卡片：标题行 Since 左侧显示"已废弃"badge（英文站同步时改为 Deprecated）
      var deprecatedBadge = $inline.hasClass('api-deprecated')
        ? '<span class="ifm-badge ifm-deprecated-badge">Deprecated</span>'
        : ''

      // a) blockquote+h6 → 标题行
      // 注意：hexo 生成的 HTML 有格式缺陷（<div class="inline-doc" ... 后未闭合 > 就换行），
      // 浏览器解析后 <blockquote> 和其内部 <div class="article"><h6> 可能被拆成兄弟节点，
      // $h6.closest('blockquote') 返回空 → 走 else 分支只 prepend title-row，
      // 结果残留的 <div class="article"><h6>method xxx</h6></div> 变成"多余的一行标题"。
      // 修复：无论 blockquote 是否找到，都主动清理所有相关残留节点。
      var $blockquote = $h6.closest('blockquote')
      var titleRowHtml =
        '<div class="ifm-method-title-row">' +
        '<span class="ifm-type-badge">' + escapeHtml(type) + '</span>' +
        '<h3>' + escapeHtml(displayName) + '</h3>' +
        deprecatedBadge +
        sinceBadge +
        '</div>'
      if ($blockquote.length) {
        $blockquote.replaceWith(titleRowHtml)
      } else {
        // class-inline-doc 或畸形 HTML：前置插入标题行
        $inline.prepend(titleRowHtml)
      }
      // 兜底清理：删除 inline-doc 内所有残留的 h6 及其祖先 .article 包裹
      // （这些是原始 blockquote/article 结构被浏览器解析畸形后遗留下来的）
      $inline.find('h6').each(function () {
        var $h6r = $(this)
        // 若 h6 已经在 ifm-method-title-row 内（新建的），跳过
        if ($h6r.closest('.ifm-method-title-row').length) return
        var $ancestorArticle = $h6r.closest('div.article')
        if ($ancestorArticle.length && $ancestorArticle[0] !== $inline[0]) {
          $ancestorArticle.remove()
        } else {
          $h6r.remove()
        }
      })
      // 兜底清理：删除 inline-doc 内空的 blockquote 或空的 .article 包裹（无实际内容）
      $inline.find('blockquote').each(function () {
        if (!$.trim($(this).text())) $(this).remove()
      })
      $inline.find('> div.article').each(function () {
        if (!$.trim($(this).text())) $(this).remove()
      })

      // b) figure.highlight
      // enum 的签名（@Keep enum XXX）冗余，与标题重复，隐藏
      if (/^enum$/i.test(type)) {
        $inline.find('figure.highlight').first().hide()
      } else {
        $inline.find('figure.highlight').first().addClass('ifm-signature-figure')
      }
      // 签名代码块行首 trim：生成器输出的签名行统一带前导缩进（全局 378/378 个
      // 签名块均如此，如「@NonNull\n    void xxx()」），导致注解行与方法签名行
      // 起始位置不对齐；仅对签名块（ifm-signature-figure）做行首空白 trim，
      // 不影响用户代码示例块的正常缩进。同时复位横向滚动，防止长签名行
      // 触发 overflow 滚动后注解行首字符被裁切
      // 计算 params 文本中的“顶层参数逗号”位置：忽略泛型 <A, B> 与注解参数 @X(a, b)
      function getSignatureParamBreaks(text) {
        var breaks = []
        var parenDepth = 0
        var angleDepth = 0
        for (var i = 0; i < text.length; i++) {
          var ch = text.charAt(i)
          if (ch === '(') parenDepth++
          else if (ch === ')') parenDepth--
          else if (ch === '<') angleDepth++
          else if (ch === '>' && angleDepth > 0) angleDepth--
          else if (ch === ',' && parenDepth === 1 && angleDepth === 0) breaks.push(i + 1)
        }
        return breaks
      }

      function insertSignatureBreak($params, offset, indentPx) {
        var nodes = []
        var walker = document.createTreeWalker($params[0], NodeFilter.SHOW_TEXT, null, false)
        var node
        var cursor = 0
        while ((node = walker.nextNode())) {
          var end = cursor + node.nodeValue.length
          nodes.push({ node: node, start: cursor, end: end })
          cursor = end
        }
        // 从参数文本偏移定位到原始 text node；break 按倒序插入，较低偏移不受影响
        for (var i = 0; i < nodes.length; i++) {
          var item = nodes[i]
          if (offset >= item.start && offset <= item.end) {
            var right = item.node.splitText(offset - item.start)
            right.nodeValue = right.nodeValue.replace(/^\s+/, '')
            var br = document.createElement('br')
            var indent = document.createElement('span')
            indent.className = 'ifm-signature-param-indent'
            indent.style.width = Math.max(0, indentPx) + 'px'
            right.parentNode.insertBefore(br, right)
            right.parentNode.insertBefore(indent, right)
            return
          }
        }
      }

      function wrapLongSignatureParams($fig) {
        if ($fig.attr('data-ifm-param-wrapped')) return
        var pre = $fig.find('pre')[0]
        if (!pre || !pre.clientWidth) return
        var preStyle = window.getComputedStyle(pre)
        var contentWidth = pre.clientWidth - parseFloat(preStyle.paddingLeft || 0) - parseFloat(preStyle.paddingRight || 0)
        if (contentWidth <= 0) return
        var canvas = document.createElement('canvas')
        var ctx = canvas.getContext('2d')
        ctx.font = [preStyle.fontStyle, preStyle.fontVariant, preStyle.fontWeight, preStyle.fontSize, preStyle.fontFamily].join(' ')

        $fig.find('.params').each(function () {
          var $params = $(this)
          if ($params.find('br').length) return
          var $line = $params.closest('.line')
          var paramsText = $params.text()
          var breaks = getSignatureParamBreaks(paramsText)
          if (!breaks.length) return
          // 源码已多行格式化时，当前 params 只含“第一组参数 + 逗号”，
          // 后续 .line 也有 params；保留原始缩进，不插入空白行
          var afterLastBreak = paramsText.slice(breaks[breaks.length - 1]).trim()
          if (!afterLastBreak && $line.nextAll('.line').first().find('.params').length) return
          if (ctx.measureText($line.text()).width <= contentWidth) return

          // 不用 paramsRect：CSS pre-wrap 可能已先自动折行，inline span 的 rect 左侧不可靠。
          // 以“返回类型 + 方法名 + (”的实际等宽文本宽度计算续行缩进，使参数对齐左括号内首参数
          var lineText = $line.text()
          var paramsStart = lineText.indexOf(paramsText)
          var prefix = paramsStart > -1 ? lineText.slice(0, paramsStart) : ''
          var indentPx = ctx.measureText(prefix + '(').width
          // 倒序插入，避免前一次 splitText 改变后续 offset
          for (var j = breaks.length - 1; j >= 0; j--) insertSignatureBreak($params, breaks[j], indentPx)
          $fig.attr('data-ifm-param-wrapped', '1')
        })
      }

      $inline.find('figure.highlight.ifm-signature-figure').each(function () {
        var $fig = $(this)
        $fig.find('.line').each(function () {
          var node = this.firstChild
          if (node && node.nodeType === 3) {
            node.nodeValue = node.nodeValue.replace(/^\s+/, '')
          }
        })
        colorizeJavaSignature($fig)
        $fig.find('pre').scrollLeft(0)
        // 等布局完成后按真实组件宽度判断；仅超宽签名按参数组换行
        var format = function () { wrapLongSignatureParams($fig) }
        if (window.requestAnimationFrame) window.requestAnimationFrame(format)
        else setTimeout(format, 0)
      })

      // c) 隐藏 table-supportedby
      $inline.find('.table-supportedby').hide()

      // d) h5 标题处理
      $inline.find('h5').each(function () {
        var $h5 = $(this)
        var txt = $.trim($h5.text())
        if (/^(描述|Description)/.test(txt)) {
          $h5.remove()
        } else if (/^(输入参数|Input Parameters)/.test(txt)) {
          $h5.replaceWith('<h3 class="ifm-plain-heading">Parameters</h3>')
        } else if (/^(返回值|Return Value)/.test(txt)) {
          $h5.replaceWith('<h3 class="ifm-plain-heading ifm-return-heading">Return Value</h3>')
        } else if (/^(Enum Members)/i.test(txt)) {
          $h5.replaceWith('<h3 class="ifm-plain-heading">Enum Members</h3>')
        } else if (/^(Class Members)/i.test(txt)) {
          // Class Members: 通常紧跟一个空块，直接移除
          $h5.remove()
        } else {
          $h5.replaceWith('<h3 class="ifm-plain-heading">' + escapeHtml(txt.replace(/[:：]$/, '')) + '</h3>')
        }
      })

      // e) 参数/枚举成员表 → leader 风格
      $inline.find('.table-inline-parameters').each(function () {
        var $tbl = $(this)
        var rows = []
        $tbl.find('tr').each(function () {
          var $tds = $(this).find('td')
          if ($tds.length >= 2) {
            var $typeTd = $tds.eq(0).clone()
            var $nameFont = $typeTd.find('font[color="#000"]')
            var paramName = $.trim($nameFont.text())
            $nameFont.remove()
            var paramTypeHtml = $typeTd.html()
            // 说明列：文本提取 + 单独保留示意图（如 IMUCalibrationOrientation 的
            // IMU 方向图）。直接用 text() 会把 <img> 一并丢掉。
            var $descTd = $tds.eq(1).clone()
            var paramImgsHtml = ''
            $descTd.find('img').each(function () {
              var src = $(this).attr('src') || ''
              if (src) paramImgsHtml += '<div class="ifm-param-img"><img src="' + escapeHtml(src) + '" alt=""></div>'
            })
            $descTd.find('img').remove()
            var paramDesc = $.trim($descTd.text())
            // 枚举成员：只有类型+说明两列，name 为空
            rows.push({
              name: paramName,
              typeHtml: paramTypeHtml,
              desc: paramDesc,
              imgs: paramImgsHtml
            })
          }
        })
        if (rows.length) {
          var hasName = rows.some(function (r) { return !!r.name })
          // 返回值表：列头应为「类型」，而非枚举成员表的「名称」
          var $prevHeading = $tbl.prevAll('h3').first()
          var isReturnTable = $prevHeading.hasClass('ifm-return-heading')
          // 含示意图的表格（如 IMUCalibrationOrientation）：追加独立的「图片示例」列，
          // 图片不再挤在说明列内；无图表格保持原有列结构不变
          var hasImgs = rows.some(function (r) { return !!(r.imgs) })
          // 枚举成员表（两列）标记 ifm-enum-table；返回值表标记 ifm-return-table
          var tableCls = 'ifm-params-table' +
            (isReturnTable ? ' ifm-return-table' : (!hasName ? ' ifm-enum-table' : '')) +
            (hasImgs ? ' ifm-has-imgs' : '')
          var $newTbl
          if (hasName) {
            $newTbl = $(
              '<table class="' + tableCls + '">' +
              '<thead><tr><th>Name</th><th>Type</th><th>Description</th>' +
              (hasImgs ? '<th>Image</th>' : '') +
              '</tr></thead><tbody></tbody></table>'
            )
            rows.forEach(function (r) {
              $newTbl.find('tbody').append(
                '<tr><td><code>' + escapeHtml(r.name) + '</code></td>' +
                '<td><code>' + r.typeHtml + '</code></td>' +
                '<td>' + escapeHtml(r.desc) + '</td>' +
                (hasImgs ? '<td>' + (r.imgs || '') + '</td>' : '') +
                '</tr>'
              )
            })
          } else if (isReturnTable) {
            // 返回值表：只显示 "类型 | 说明" 两列
            $newTbl = $(
              '<table class="' + tableCls + '">' +
              '<thead><tr><th>Type</th><th>Description</th>' +
              (hasImgs ? '<th>Image</th>' : '') +
              '</tr></thead><tbody></tbody></table>'
            )
            rows.forEach(function (r) {
              $newTbl.find('tbody').append(
                '<tr><td><code>' + r.typeHtml + '</code></td>' +
                '<td>' + escapeHtml(r.desc) + '</td>' +
                (hasImgs ? '<td>' + (r.imgs || '') + '</td>' : '') +
                '</tr>'
              )
            })
          } else {
            // 枚举成员表：只显示 "名称 | 说明" 两列（typeHtml 里含枚举名）
            $newTbl = $(
              '<table class="' + tableCls + '">' +
              '<thead><tr><th>Name</th><th>Description</th>' +
              (hasImgs ? '<th>Image</th>' : '') +
              '</tr></thead><tbody></tbody></table>'
            )
            rows.forEach(function (r) {
              $newTbl.find('tbody').append(
                '<tr><td><code>' + r.typeHtml + '</code></td>' +
                '<td>' + escapeHtml(r.desc) + '</td>' +
                (hasImgs ? '<td>' + (r.imgs || '') + '</td>' : '') +
                '</tr>'
              )
            })
          }
          $tbl.replaceWith($newTbl)
        }
      })

      // e2) 参数表「类型」列：按 Java 语法角色着色，保留原有 <a> 跳转
      colorizeParamTypes($inline)

      // f) Note/注意 <b> → callout
      // YAML 源头已统一 Note 块缩进，生成 md 时保持 b 内容为单行，
      // 无需在适配层搬移/重组列表，避免误捕获卡片内其他 ol/ul
      $inline.find('p b').each(function () {
        var $b = $(this)
        var bText = $b.text()
        if (/^(注意|Notice|Note)[：:]/.test(bText) || /^(注意|Notice|Note)/.test(bText)) {
          var noteHtml = $b.html().replace(/^(注意|Notice|Note)[：:]\s*/, '')
          $b.replaceWith(
            '<div class="ifm-callout"><strong>Note</strong><br>' + noteHtml + '</div>'
          )
        }
      })

      // g) 隐藏底部版本标签（已迁至标题行）
      $inline.find('.msdk-support-tag').hide()
      // class-inline-doc 是异步 fetch 后组装的，版本号可能直接嵌在描述段 <p> 内的
      // <code><i>MSDK X.X.X开始支持</i></code>，msdk-support-tag 提取 IIFE 抓不到
      // → 这里主动清理描述段里剩余的版本标记 code
      $inline.find('p code, p i').each(function () {
        var t = $.trim($(this).text())
        if (/^MSDK\s+[\d.]+\s*开始支持$/.test(t) || /Supported\s+since\s+MSDK/i.test(t)) {
          // 移除包裹这个版本标记的最近的 code 节点
          var $c = $(this).closest('code')
          if ($c.length) $c.remove()
          else $(this).remove()
        }
      })
      // 清理描述段末尾遗留的连续 <br>（含嵌套在 <font> 内的情况）
      $inline.find('p, p font').each(function () {
        var $el = $(this)
        var html = $el.html()
        if (html) {
          // 反复剔除末尾的 &nbsp;/空白/<br>（不区分大小写、允许自闭合）
          var cleaned = html.replace(/(?:\s|&nbsp;|<br\s*\/?>)+$/gi, '')
          if (cleaned !== html) $el.html(cleaned)
        }
      })
    }

    // ===== 1. 提取版本号 + 类名 + kind =====
    var since = ''
    $article.find('code').each(function () {
      var match = $(this).text().match(/MSDK\s+([\d.]+)/)
      if (match) { since = match[1]; return false }
    })
    // 自动提取 className / kind（从顶部 h1：<font color="#AAA">class </font>XXX）
    var $topH1 = $article.children('.article').first().find('h1').first()
    if (!$topH1.length) $topH1 = $article.find('h1').first()
    var h1Text = $.trim($topH1.text())
    var kindMatch = h1Text.match(/^(interface|class|enum)\s+(.+)$/i)
    var className = config.className || (kindMatch ? kindMatch[2].trim() : h1Text)
    var kind = config.kind || (kindMatch ? kindMatch[1].toLowerCase() : 'class')
    var $topSignature = $article.children('figure.highlight').first()
    if (!$topSignature.length) $topSignature = $article.find('figure.highlight').first()
    var topDeprecated = /@Deprecated\b/.test($topSignature.text())

    // 隐藏原有 h1 + figure + table-supportedby
    $article.children('.article').first().hide()
    $article.children('figure.highlight').first().hide()
    $article.children('html').first().hide()

    // 提取类描述 + 包名
    var $descH5 = $article.find('h5').filter(function () { return /^(描述|Description)/.test($.trim($(this).text())) }).first()
    var classLead = ''
    if ($descH5.length) {
      var $descP = $descH5.nextAll('p').first()
      if ($descP.length) {
        var $clone = $descP.clone()
        $clone.find('code').each(function () {
          if (/(?:MSDK\s+[\d.]+\s*开始支持|Supported\s+since\s+MSDK\s*[\d.]+)/i.test($(this).text())) $(this).remove()
        })
        $clone.find('b').remove()
        var text = $.trim($clone.text())
        var m = text.match(/^[\s\S]*?[。.!?！？](?=\s|$)/)
        classLead = m ? m[0] : text
      }
    }
    var $pkgTable = $article.find('.table-supportedby').first()
    var pkgName = ''
    if ($pkgTable.length) pkgName = $.trim($pkgTable.find('td').eq(1).text())

    // 从描述区域提取 <b>注意</b> 转 callout
    var $descCallout = null
    if ($descH5.length) {
      var $scan = $descH5.next()
      while ($scan.length && !$scan.is('h5')) {
        var $b = $scan.find('b').addBack('b').first()
        if ($b.length && /^(注意|Notice|Note)/.test($.trim($b.text()))) {
          var bHtml = $b.html()
          bHtml = bHtml.replace(/^(注意|Notice|Note)[：:]\s*/i, '')
          bHtml = bHtml.replace(/^(<br\s*\/?>)+\s*/i, '')
          $descCallout = $('<div class="ifm-callout"><strong>Note</strong> ' + bHtml + '</div>')
          break
        }
        $scan = $scan.next()
      }
    }

    // 顶部描述中的原始 <li> 使用流程：转为结构化步骤卡片。
    // Hexo 生成的 <p> 内嵌 <li> 属于不规范 HTML，浏览器会拆分为兄弟节点，
    // 因此仅收集“描述”与下一 h5 之间的 li，绝不批量隐藏整个区间。
    var $descriptionFlow = null
    var $descriptionParagraph = $descH5.length ? $descH5.nextAll('p').first() : $()
    if ($descH5.length) {
      // 描述区边界：除下一个 h5 外，遇到成员行/卡片也必须终止——
      // 英文站方法区没有 h5 标题，nextUntil('h5') 会一路吞到文章末尾，
      // 导致 $flowItems.hide() 误隐藏全页 li（含 Note 列表内容）
      var $descriptionNodes = $descH5.nextUntil('h5, .api-row, .inline-doc, .class-inline-doc')
      var $flowItems = $descriptionNodes.filter('li').add($descriptionNodes.find('li'))
      if ($flowItems.length) {
        var flowHtml = ''
        $flowItems.each(function () {
          var itemHtml = $(this).html().replace(/^\s*\d+[\.、]\s*/, '')
          flowHtml += '<li>' + itemHtml + '</li>'
        })
        $descriptionFlow = $(
          '<section class="ifm-description-flow" aria-label="Usage flow">' +
          '<h2>Usage Flow</h2><ol>' + flowHtml + '</ol></section>'
        )
        $flowItems.hide()
      }
      // 原始注意已复制为 callout，版本标记已由 Overview 的 Since 徽章表达；
      // 隐藏源节点，避免浏览器修复不规范 <p>/<li> 结构后出现残留内容。
      $descriptionNodes.filter('b').add($descriptionNodes.find('b')).filter(function () {
        return /^(注意|Notice|Note)/.test($.trim($(this).text()))
      }).hide()
      $descriptionNodes.find('code').each(function () {
        if (/^(?:MSDK\s+[\d.]+\s*开始支持|Supported\s+since\s+MSDK\s*[\d.]+)$/.test($.trim($(this).text()))) $(this).hide()
      })
    }

    // ===== 2. 面包屑 + Overview Header =====
    // 面包屑层级：config.breadcrumb 优先，否则默认 Common APIs › className
    var crumbs = config.breadcrumb || [
      { name: 'Common APIs', href: null },
      { name: className, href: null }
    ]
    var breadcrumbHtml = '<nav class="ifm-breadcrumb" aria-label="Breadcrumb">'
    crumbs.forEach(function (c, i) {
      if (i > 0) breadcrumbHtml += '<span class="ifm-breadcrumb-sep">›</span>'
      if (c.href && i < crumbs.length - 1) {
        breadcrumbHtml += '<a href="' + escapeHtml(c.href) + '">' + escapeHtml(c.name) + '</a>'
      } else {
        breadcrumbHtml += '<span>' + escapeHtml(c.name) + '</span>'
      }
    })
    breadcrumbHtml += '</nav>'
    var $breadcrumb = $(breadcrumbHtml)
    var sinceBadge = since ? '<span class="ifm-badge ifm-since-badge">Since MSDK ' + since + '</span>' : ''
    var deprecatedBadge = topDeprecated ? '<span class="ifm-badge ifm-deprecated-badge">Deprecated</span>' : ''
    var $eyebrow = $(
      '<div class="ifm-eyebrow">' +
      '<span class="ifm-badge ifm-kind-badge">' + escapeHtml(kind) + '</span>' +
      sinceBadge +
      deprecatedBadge +
      '</div>' +
      '<h1 class="ifm-title">' + escapeHtml(className) + '</h1>' +
      '<p class="ifm-lead">' + escapeHtml(classLead) + '</p>' +
      '<div class="ifm-meta-line">' +
      '<span class="ifm-meta-item">' +
      '<svg class="ifm-meta-icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><path d="M4 7.5 12 3l8 4.5v9L12 21l-8-4.5z"/><path d="m4 7.5 8 4.5 8-4.5M12 12v9"/></svg>' +
      'Package <code>' + escapeHtml(pkgName) + '</code>' +
      '<button class="ifm-mini-copy" type="button" data-copy="' + escapeHtml(pkgName) + '" aria-label="Copy package name">' +
      '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><rect x="8" y="8" width="11" height="11" rx="2"/><path d="M16 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h2"/></svg>' +
      '</button></span>' +
      '<span class="ifm-meta-item">' +
      '<svg class="ifm-meta-icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>' +
      'Android · Java / Kotlin</span>' +
      '</div>'
    )
    if (topDeprecated) $eyebrow.addClass('ifm-overview-deprecated')
    // 包名复制按钮
    $eyebrow.find('.ifm-mini-copy').on('click', function () {
      ifmCopyText($(this).attr('data-copy') || '', $(this))
    })

    // 典型调用流程（仅 config.workflow 存在时渲染）
    var $workflow = null
    if (config.workflow && config.workflow.steps && config.workflow.steps.length) {
      var wf = config.workflow
      var stepsHtml = wf.steps.map(function (s) {
        return '<div class="ifm-step"><div class="step-num">' + escapeHtml(s.num) + '</div>' +
          '<div class="step-title">' + escapeHtml(s.title) + '</div>' +
          '<div class="step-desc">' + escapeHtml(s.desc) + '</div></div>'
      }).join('')
      $workflow = $(
        '<div class="ifm-workflow-section" id="ifm-workflow">' +
        '<div class="ifm-section-head"><h2>典型调用流程</h2>' +
        (wf.note ? '<span class="ifm-section-note">' + escapeHtml(wf.note) + '</span>' : '') +
        '</div>' +
        '<div class="ifm-workflow">' + stepsHtml + '</div></div>'
      )
    }

    if ($descH5.length) {
      $descH5.before($breadcrumb).before($eyebrow)
      if ($descriptionFlow) $descH5.before($descriptionFlow)
      if ($descCallout) $descH5.before($descCallout)
      if ($workflow) $descH5.before($workflow)
      // 原始描述首段已浓缩至 Overview lead；其余内容不能整体隐藏，
      // 否则会因 Hexo 畸形 DOM 误伤后续成员区。
      $descH5.hide()
      $descriptionParagraph.hide()
      var $topTag = $article.children('.msdk-support-tag').first()
      if ($topTag.length) $topTag.hide()

      // 收尾：隐藏描述区残留的可见占位容器。Hexo 将 <p> 内嵌 <li>/<b>/<code> 的
      // 不规范结构交给浏览器解析，foster parenting 会把内容拆进裸 <font> 等容器：
      // 其中的 li/b/code 已逐项 hide()（注意 hide() 写入的是 inline style 而非
      // hidden 属性，判定必须用 css('display')），但容器内的 <br> 与空 <p> 仍可见，
      // 会撑起「典型调用流程」与「成员」之间的大段异常空白。
      // 判定：顶层节点若仅剩 <br>、已隐藏节点与空段落（无任何可见文本），整体隐藏。
      // 收尾：清理描述区 foster parenting 残留。Hexo 将 <p> 内嵌 <li>/<b>/<code> 的
      // 不规范结构交给浏览器解析后，会拆出未闭合的裸 <font> 容器，把描述区之后的
      // 大量正常内容（成员列表、相关类型等）一并包为后代——因此 font 容器本身
      // 绝不能整体隐藏。其中的 li/b/code 已逐项 hide()，但游离在内容容器之外的
      // <br> 与空 <p> 仍可见，会撑起「典型调用流程」与「成员」之间的大段异常空白。
      // 处理：仅隐藏 font 直接子级中、不在任何内容容器（成员区/成员行/卡片等）内的
      // 游离 <br> 与空段落/空容器。
      if ($descriptionNodes && $descriptionNodes.length) {
        $descriptionNodes.filter('font').each(function () {
          var $font = $(this)
          if ($font.css('display') === 'none') return
          $font.contents().each(function () {
            var $c = $(this)
            // 跳过已被正常内容容器承接的节点（成员区、相关类型行、卡片、section 标题等）
            if ($c.closest('.ifm-members-section, .api-row, .inline-doc, .ifm-section-head, .ifm-related-heading').length) return
            if (this.nodeType === 1) {
              if (this.tagName === 'BR') { $c.hide(); return }
              if (!($.trim($c.text()) || $c.find('img,iframe,video').length)) $c.hide()
            }
          })
        })
      }
    } else {
      $article.prepend($breadcrumb, $eyebrow)
      if ($descriptionFlow) $article.prepend($descriptionFlow)
      if ($descCallout) $article.prepend($descCallout)
      if ($workflow) $article.prepend($workflow)
    }

    // ===== 3. "类成员" 标题（仅顶层 h5，避免误伤 inline-doc 内的 "Class Members"）=====
    $article.find('h5').filter(function () {
      return $(this).parents('.inline-doc').length === 0
    }).each(function () {
      var $h5 = $(this)
      var txt = $.trim($h5.text())
      if (/^(类成员|Class Members|Members)/i.test(txt)) {
        $h5.replaceWith('<div class="ifm-section-head" id="ifm-members-anchor"><h2>Members</h2></div>')
      }
    })

    // ===== 3.5 成员元数据：DOM 自动派生（监听 / 方法 / 枚举 / 相关类型）=====
    // 扫描所有顶层 api-row，自动判定 type / category / group
    var allMembers = []
    $article.find('.api-row').each(function () {
      var $row = $(this)
      if ($row.parents('.inline-doc').length) return
      var type = $.trim($row.find('.api-col.middle').text()).toLowerCase() || 'method'
      if (type === 'unknown') type = 'method'  // 数据类字段标记为 unknown
      var $link = $row.find('.api-col.right a').first()
      var label = $.trim($link.text())
      var domId = $row.attr('id') || ''
      if (!label || !domId) return

      var category, group, isEnum = false
      if (type === 'enum') {
        category = 'enum'; group = 'enum'; isEnum = true
      } else if (type === 'class') {
        category = 'class'; group = 'class'  // 相关类型，不进成员列表
      } else {
        if (/^(add|remove|removeall|clear).*listener$/i.test(label)) {
          category = 'listener'; group = 'listener'
        } else {
          category = 'method'; group = 'method'
        }
      }

      // 摘要从 inline-doc 提取
      var summary = ''
      var $inlineEl = $(document.getElementById(domId + '_inline'))
      if ($inlineEl.length) {
        var $dH5 = $inlineEl.find('h5').filter(function () { return /^(描述|Description)/.test($.trim($(this).text())) }).first()
        var $dP = $dH5.length ? $dH5.nextAll('p').first() : $inlineEl.find('p').first()
        if ($dP.length) {
          var $c2 = $dP.clone()
          $c2.find('code').each(function () {
            if (/(?:MSDK\s+[\d.]+\s*开始支持|Supported\s+since\s+MSDK\s*[\d.]+)/i.test($(this).text())) $(this).remove()
          })
          $c2.find('b').remove()
          var t2 = $.trim($c2.text())
          var s2 = t2.match(/^[\s\S]*?[。.!?！？](?=\s|$)/)
          summary = s2 ? s2[0] : t2.substring(0, 80)
        }
      }

      allMembers.push({
        domId: domId, label: label, type: type,
        category: category, group: group, isEnum: isEnum,
        summary: summary
      })
    })

    // groupDefs 自动构建（按顺序：监听 → 方法 → 枚举）
    var IFM_GROUP_DEFS = []
    if (allMembers.some(function (m) { return m.group === 'listener' })) IFM_GROUP_DEFS.push({ id: 'listener', label: 'Listener' })
    if (allMembers.some(function (m) { return m.group === 'method' })) IFM_GROUP_DEFS.push({ id: 'method', label: 'Method' })
    if (allMembers.some(function (m) { return m.group === 'enum' })) IFM_GROUP_DEFS.push({ id: 'enum', label: 'Enum' })
    if (allMembers.some(function (m) { return m.group === 'class' })) IFM_GROUP_DEFS.push({ id: 'class', label: 'Class' })

    // 进入成员列表的成员（排除 class，class 归相关类型）
    // class 相关类型作为「Class」分组进入成员列表（点击滚动到底部对应卡片）；
    // 注意 TOC 的「方法」组过滤条件需同步排除 class，否则 TOC 会错误吸收 class 条目
    var ifmMembers = allMembers

    // 构建成员列表 UI
    if (ifmMembers.length) {
      // 筛选按钮：根据实际分组动态生成（只有存在对应分组的才显示按钮）
      var filterBtns = '<button class="ifm-filter-btn" data-filter="all" aria-pressed="true">All</button>'
      IFM_GROUP_DEFS.forEach(function (grp) {
        filterBtns += '<button class="ifm-filter-btn" data-filter="' + grp.id + '">' + grp.label + '</button>'
      })
      var $ifmMembersSection = $(
        '<div class="ifm-members-section" id="ifm-members-nav">' +
        '<div class="ifm-section-head"><h2>Members</h2>' +
        '<span class="ifm-section-note"><span class="ifm-count">' + ifmMembers.length + '</span> members</span></div>' +
        '<div class="ifm-member-tools">' +
        '<div class="ifm-member-search">' +
        '<svg class="ifm-search-icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.4-3.4"/></svg>' +
        '<input type="search" id="ifm-search" placeholder="Filter by name, parameter or description" aria-label="Filter members">' +
        '<button class="ifm-search-clear" type="button" aria-label="Clear search" hidden>' +
        '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M6 6l12 12M18 6 6 18"/></svg>' +
        '</button></div>' +
        '<div class="ifm-filter-group">' + filterBtns + '</div></div>' +
        '<div class="ifm-member-groups" id="ifm-groups"></div>' +
        '<div class="ifm-empty-state" id="ifm-empty" style="display:none">No matching members. Try a shorter keyword.</div>' +
        '</div>'
      )

      var $ifmGroupsContainer = $ifmMembersSection.find('#ifm-groups')
      IFM_GROUP_DEFS.forEach(function (grp) {
        var grpMembers = ifmMembers.filter(function (m) { return m.group === grp.id })
        if (!grpMembers.length) return
        var $grp = $('<div class="ifm-member-group" data-group="' + grp.id + '"></div>')
        $grp.append('<div class="ifm-member-group-head"><h3>' + grp.label + '</h3><span class="ifm-member-count">' + grpMembers.length + '</span></div>')
        var $list = $('<div class="ifm-member-list"></div>')
        grpMembers.forEach(function (m) {
          var searchText = (m.label + ' ' + m.summary).toLowerCase()
          var badge = m.isEnum ? 'enum' : (m.group === 'class' ? 'class' : 'method')
          var $row = $(
            '<button class="ifm-member-row" type="button" data-target="' + m.domId + '_inline" data-category="' + m.category + '">' +
            '<span class="ifm-member-badge">' + badge + '</span>' +
            '<span class="ifm-member-name">' + escapeHtml(m.label) + '</span>' +
            '<span class="ifm-member-desc">' + escapeHtml(m.summary) + '</span>' +
            '<span class="ifm-member-arrow">›</span>' +
            '</button>'
          )
          $row.attr('data-search', searchText)
          $list.append($row)
        })
        $grp.append($list)
        $ifmGroupsContainer.append($grp)
      })

      // 插入到 "成员" section head 之前（替代它）
      var $memberSectionHead = $article.find('#ifm-members-anchor').first()
      if ($memberSectionHead.length) {
        $memberSectionHead.replaceWith($ifmMembersSection)
      } else {
        // 兜底：插到相关类之前 / article 末尾
        $article.append($ifmMembersSection)
      }

      // 成员行点击：滚动到对应 inline-doc + 闪烁高亮
      $article.on('click', '.ifm-member-row', function () {
        var targetId = $(this).attr('data-target')
        var $target = $(document.getElementById(targetId))
        if (!$target.length) return
        $('html, body').stop().animate({ scrollTop: $target.offset().top - 80 }, 300)
        $target.addClass('ifm-flash')
        setTimeout(function () { $target.removeClass('ifm-flash') }, 1600)
      })

      // 搜索 + 筛选
      var ifmActiveFilter = 'all'
      function ifmApplyFilter() {
        var term = ($('#ifm-search').val() || '').trim().toLowerCase()
        var count = 0
        $('.ifm-member-row').each(function () {
          var $r = $(this)
          var catMatch = ifmActiveFilter === 'all' || $r.attr('data-category') === ifmActiveFilter
          var search = ($r.attr('data-search') || '').toLowerCase()
          // 双通道匹配：data-search（名称+摘要）+ 行文本兜底，均忽略大小写、支持子串模糊匹配
          var termMatch = !term || search.indexOf(term) >= 0 || $r.text().toLowerCase().indexOf(term) >= 0
          $r.prop('hidden', !(catMatch && termMatch))
          if (catMatch && termMatch) count++
        })
        $('.ifm-member-group').each(function () {
          var $g = $(this)
          $g.prop('hidden', !$g.find('.ifm-member-row').not('[hidden]').length)
        })
        $('.ifm-count').text(count)
        $('#ifm-empty').toggle(count === 0)
      }
      // 搜索框：有内容时显示清空按钮，点击一键清空并复位列表
      var $ifmSearchInput = $('#ifm-search')
      var $ifmSearchClear = $ifmSearchInput.siblings('.ifm-search-clear')
      $ifmSearchInput.on('input', function () {
        $ifmSearchClear.prop('hidden', !$.trim($ifmSearchInput.val()))
        ifmApplyFilter()
      })
      $ifmSearchClear.on('click', function () {
        $ifmSearchInput.val('').trigger('input').focus()
      })
      $article.on('click', '.ifm-filter-btn', function () {
        ifmActiveFilter = $(this).data('filter')
        $('.ifm-filter-btn').attr('aria-pressed', function () {
          return $(this).data('filter') === ifmActiveFilter ? 'true' : 'false'
        })
        ifmApplyFilter()
      })
    }

    // ===== 4. 处理所有 method / enum / class 的 api-row + inline-doc =====
    var $allTopRows = $article.find('.api-row').filter(function () {
      return $(this).find('.api-col.middle').length > 0 &&
             $(this).parents('.inline-doc').length === 0
    })

    $allTopRows.each(function () {
      var $row = $(this)
      $row.addClass('api-row-hidden')

      var type = $.trim($row.find('.api-col.middle').text()) || 'method'
      var $link = $row.find('.api-col.right a').first()
      var name = $.trim($link.text())
      var rowId = $row.attr('id') || ''

      var $inline = null
      if (rowId) {
        var el = document.getElementById(rowId + '_inline')
        if (el) $inline = $(el)
      }
      if (!$inline || !$inline.length) {
        var $next = $row.next()
        if ($next.hasClass('inline-doc')) $inline = $next
      }
      if (!$inline || !$inline.length) return

      // class-inline-doc 由异步 fetch 填充；若尚未填充，仅标记数据待稍后处理
      if ($inline.hasClass('class-inline-doc') && $inline.children().length === 0) {
        $inline.addClass('ifm-method-doc')
        $inline.attr('data-ifm-type', type)
        $inline.attr('data-ifm-name', name)
      } else {
        transformInlineDoc($inline, type, name)
      }
    })

    // ===== 5. "相关类:" h5 → section heading =====
    $article.find('h5, #related-title').each(function () {
      var $h5 = $(this)
      var txt = $.trim($h5.text())
      if (/^(相关类|Related)/i.test(txt)) {
        $h5.replaceWith('<div class="ifm-related-heading"><h2>Related Types</h2></div>')
      }
    })

    // ===== 6. 监听 class-inline-doc 异步填充完毕事件（由 renderClassInlineDoc 触发） =====
    $(document).off('classInlineDocReady.ifm').on('classInlineDocReady.ifm', function (e) {
      var $doc = $(e.target)
      if ($doc.hasClass('ifm-processed')) return
      var type = $doc.attr('data-ifm-type') || 'class'
      var name = $doc.attr('data-ifm-name') || ''
      transformInlineDoc($doc, type, name)
    })

    // 兜底：轮询检查未处理的 class-inline-doc（防止事件未触发）
    var pollCount = 0
    var pollTimer = setInterval(function () {
      pollCount++
      $('.class-inline-doc.ifm-method-doc:not(.ifm-processed)').each(function () {
        var $doc = $(this)
        if ($doc.children().length > 0) {
          var type = $doc.attr('data-ifm-type') || 'class'
          var name = $doc.attr('data-ifm-name') || ''
          transformInlineDoc($doc, type, name)
        }
      })
      if (pollCount > 40) clearInterval(pollTimer)  // 最多轮询 8 秒
    }, 200)

    // ===== 7. 重建 TOC：概览 / 成员索引 / 方法与枚举 / 相关类型 =====
    // 收集顶层 class api-rows（在 injectClassInlineDocs 后仍保留 id）
    var ifmTocItems = []  // { anchorId, label } — 供 onScroll 判定高亮
    var ifmOnScrollBound = false

    function ifmOnScroll() {
      if (!ifmTocItems.length) return
      var $tocList = $('.site-toc-list')
      if (!$tocList.length) return
      var scrollTop = $(window).scrollTop()
      var threshold = scrollTop + 150
      var current = ifmTocItems[0].anchorId
      for (var i = 0; i < ifmTocItems.length; i++) {
        var el = document.getElementById(ifmTocItems[i].anchorId)
        if (!el) continue
        var $el = $(el)
        if ($el.css('display') === 'none') continue
        if ($el.offset().top <= threshold) {
          current = ifmTocItems[i].anchorId
        }
      }
      $tocList.find('a').removeClass('active')
      if (current) {
        var $activeLink = $tocList.find('a[data-target="' + current + '"]').addClass('active')
        // TOC 内自动滚动：让 active 项保持可见
        if ($activeLink.length) {
          var $tocScrollBox = $tocList.closest('.site-toc-inner')
          if ($tocScrollBox.length) {
            var boxTop = $tocScrollBox.offset().top
            var boxH = $tocScrollBox.outerHeight()
            var linkTop = $activeLink.offset().top
            var linkH = $activeLink.outerHeight()
            if (linkTop < boxTop + 40 || linkTop + linkH > boxTop + boxH - 40) {
              var curScroll = $tocScrollBox.scrollTop()
              var relTop = linkTop - boxTop + curScroll
              var target = relTop - (boxH - linkH) / 2
              if (target < 0) target = 0
              $tocScrollBox.stop().animate({ scrollTop: target }, 200)
            }
          }
        }
      }
    }

    function rebuildIfmToc() {
      var $tocList = $('.site-toc-list')
      if (!$tocList.length) return
      $tocList.off('click')
      $tocList.empty()
      ifmTocItems = []

      // 给 Overview 区加一个可跳转的 id
      var $overviewEl = $article.find('.ifm-eyebrow').first()
      if ($overviewEl.length && !$overviewEl.attr('id')) {
        $overviewEl.attr('id', 'ifm-overview')
      }

      function appendTocLink(anchorId, label, extraClass, kind) {
        var $li = $('<li>')
        if (extraClass) $li.addClass(extraClass)
        var $a = $('<a>')
          .attr('href', '#' + anchorId)
          .attr('data-scroll', anchorId)
          .attr('data-target', anchorId)  // 兼容 onScroll 高亮匹配
        // 加类型缩写小 badge：method→M / class→C / enum→E
        if (kind) {
          var kindLetter = { method: 'M', class: 'C', enum: 'E' }[kind] || kind.charAt(0).toUpperCase()
          $a.append(
            $('<span>')
              .addClass('ifm-toc-kind')
              .addClass('ifm-toc-kind-' + kind)
              .text(kindLetter)
          )
        }
        $a.append(document.createTextNode(label))
        $li.append($a)
        $tocList.append($li)
        ifmTocItems.push({ anchorId: anchorId, label: label })
      }

      appendTocLink('ifm-overview', 'Overview')
      if ($article.find('#ifm-workflow').length) {
        appendTocLink('ifm-workflow', 'Typical Invocation Flow')
      }
      if ($article.find('#ifm-members-nav').length) {
        appendTocLink('ifm-members-nav', 'Member Index')
      }

      // 方法分组：TOC 保持既有口径——method + listener 均归「方法」，仅排除 enum 与 class
      // （class/enum 均归入下方「相关类型」组，按原始 DOM 顺序混排，不做类型重排；
      //   ifmMembers 纳入 class 后必须显式排除，否则 TOC「方法」组会错误吸收 class 条目）
      var ifmMethods = ifmMembers.filter(function (m) { return !m.isEnum && m.group !== 'class' })
      var ifmEnums = ifmMembers.filter(function (m) { return m.isEnum })

      if (ifmMethods.length) {
        $tocList.append('<li class="toc-group"><span class="toc-group-title">Methods</span></li>')
        ifmMethods.forEach(function (m) {
          appendTocLink(m.domId + '_inline', m.label, 'ifm-toc-method', 'method')
        })
      }

      // 相关类型：严格沿用原始 DOM 顺序，class 与 enum 可以交错出现。
      // allMembers 在页面 api-row 的扫描顺序中构建，可与内容区保持一致。
      var relatedItems = allMembers.filter(function (m) {
        return m.group === 'class' || m.group === 'enum'
      })
      if (relatedItems.length) {
        $tocList.append('<li class="toc-group"><span class="toc-group-title">Related Types</span></li>')
        relatedItems.forEach(function (m) {
          appendTocLink(m.domId + '_inline', m.label, 'ifm-toc-related', m.type)
        })
      }

      // TOC 点击：平滑滚动
      $tocList.on('click', 'a', function (e) {
        var scrollTarget = $(this).attr('data-scroll')
        if (!scrollTarget) return
        e.preventDefault()
        var $target = $(document.getElementById(scrollTarget))
        if ($target.length) {
          $('html, body').stop().animate({ scrollTop: $target.offset().top - 80 }, 300)
        }
      })

      // 绑定 ifm 专属滚动高亮（只绑定一次）
      if (!ifmOnScrollBound) {
        $(window).on('scroll.ifmToc resize.ifmToc', ifmOnScroll)
        ifmOnScrollBound = true
      }
      // 触发一次高亮 + TOC 高度重算
      ifmOnScroll()
      $(window).trigger('scroll')
    }

    if (config.keepCustomToc) {
      // 手工分组页（DJIValue 等）：保留全局 TOC（按文档顺序 + h4/subGroup 主题分组），
      // 不执行 rebuildIfmToc。但成员行被隐藏后，TOC 锚点需重定向到对应的 inline 卡片。
      $article.find('.api-row.api-row-hidden').each(function () {
        var rowId = $(this).attr('id') || ''
        if (!rowId) return
        var $trigger = $(this).find('a.trigger').first()
        var inlineId = $trigger.attr('href')
          ? $trigger.attr('href').replace('#', '')
          : rowId + '_inline'
        $('.site-toc-list')
          .find('a[href="#' + rowId + '"]')
          .attr('href', '#' + inlineId)
          .attr('data-target', inlineId)
          .attr('data-scroll', inlineId)
      })
      // TOC 顶部补充「概览 / 成员索引」，与 IMediaDataCenter 风格同步
      var $customTocList = $('.site-toc-list')
      if ($customTocList.length) {
        var $idxLi = $('<li>').append(
          $('<a>').attr('href', '#ifm-members-nav').attr('data-target', 'ifm-members-nav').text('Member Index')
        )
        var $ovLi = $('<li>').append(
          $('<a>').attr('href', '#ifm-overview').attr('data-target', 'ifm-overview').text('Overview')
        )
        $customTocList.prepend($idxLi).prepend($ovLi)
        // 全局 TOC 的初始 onScroll 跑在本分支之前（当时概览/成员索引尚未 prepend，
        // 高亮会落空），补触发一次滚动计算让初始高亮正确落在「概览」
        $(window).trigger('scroll')
      }
    } else {
      // 立即重建一次；异步 class-inline-doc 完成后再重建（确保相关类 desc 也齐全）
      rebuildIfmToc()
      $(document).off('classInlineDocReady.ifmToc').on('classInlineDocReady.ifmToc', function () {
        // 稍延迟以确保 DOM 稳定
        setTimeout(rebuildIfmToc, 100)
      })
    }

    // 枚举成员示意图：点击当前页面弹窗预览（不另开窗口），
    // 点击遮罩任意处或按 ESC 关闭
    $article.on('click', '.ifm-param-img img', function () {
      var src = $(this).attr('src')
      if (!src) return
      var $overlay = $(
        '<div class="ifm-lightbox" role="dialog" aria-label="Image preview">' +
        '<img src="' + escapeHtml(src) + '" alt=""></div>'
      )
      $('body').append($overlay)
      $overlay.on('click', function () {
        $overlay.remove()
        $(document).off('keydown.ifmLightbox')
      })
      $(document).on('keydown.ifmLightbox', function (e) {
        if (e.keyCode === 27) {
          $overlay.remove()
          $(document).off('keydown.ifmLightbox')
        }
      })
    })
  }

  function injectClassInlineDocs() {
    $('.site-main .api-row.api-row-link').each(function () {
      var $row = $(this)
      if ($row.parents('.inline-doc').length) return
      var $anchor = $row.find('.api-col.right a').eq(0)
      var href = $anchor.attr('href')
      if (!href) return
      var name = $.trim($anchor.text())
      var rowId = $row.attr('id') || ''

      // 构建期已内联的离线预览：跳过 AJAX，保留同一套 class 卡片和 TOC 逻辑。
      var $prebuilt = rowId ? $(document.getElementById(rowId + '_inline')) : $()
      if ($prebuilt.length) {
        $prebuilt
          .addClass('class-inline-doc')
          .attr('data-class-href', href)
          .attr('data-class-name', name)
          .css('display', 'block')
          .off('click.offlineClass')
          .on('click.offlineClass', function (e) {
            if ($(e.target).closest('a').length) return
            window.location.href = href
          })
        $row.addClass('api-row-hidden').attr('data-class-inline-id', rowId + '_inline')
        return
      }

      // 占位 inline-doc（先插入空壳，异步填充）
      // class 行 id 保留（TOC 遍历需要），展开框用 `${id}_inline`（与 method/enum 一致）
      var $doc = $('<div class="inline-doc class-inline-doc"></div>')
        .attr('id', rowId + '_inline')
        .attr('data-class-href', href)
        .attr('data-class-name', name)
        .css('display', 'block')
      $row.after($doc)
      // 隐藏外层 class 行（顶部 CLASS 标题栏由展开框内部的 blockquote+h6 承担，
      // 与 enum/method 结构完全一致）
      $row.addClass('api-row-hidden')
      // 标记 class 行以便 TOC 定位到展开框
      $row.attr('data-class-inline-id', rowId + '_inline')

      // 异步 fetch
      $.ajax({
        url: href,
        dataType: 'html',
        cache: true,
        success: function (html) {
          renderClassInlineDoc($doc, html, name, href)
        },
        error: function () {
          $doc.remove() // 拉不到就移除占位
        }
      })
    })
  }

  function renderClassInlineDoc($doc, html, name, href) {
    // 解析远端 HTML，抽取 .site-main .article 的内容
    var $wrap = $('<div>' + html + '</div>')
    var $remoteArticle = $wrap.find('.site-main .article').eq(0)
    if (!$remoteArticle.length) {
      $doc.remove()
      return
    }

    // 只取"代码定义 + 描述段落"这类顶部内容，与 method/enum inline-doc 结构一致：
    //   - <blockquote><div class="article"><h6>class 名称</h6></div></blockquote>（可选）
    //   - 首个 figure.highlight（代码定义签名）
    //   - "描述:"/"Description:" h5 及其后续段落（直到下一个 h5）
    //   - "MSDK X.X.X 开始支持" 版本标签（会被外层通用逻辑抽到底部）
    var $content = $('<div></div>')

    // 顶部 h6 标题：模仿 method/enum 的 <blockquote><div class="article"><h6>xxx</h6></div></blockquote> 结构，
    // class 名后追加 ↗ 箭头暗示可点击跳转到独立页
    $content.append(
      $('<blockquote><div class="article"><h6><font color="#AAA">class </font>' + name + ' <span class="class-jump-arrow">\u2197</span></h6></div></blockquote>')
    )

    // 代码定义块
    var $firstFigure = $remoteArticle.find('figure.highlight').eq(0)
    if ($firstFigure.length) {
      $content.append($firstFigure.clone())
    }

    // 描述段落：相关类型卡片只保留首段摘要与版本信息。
    // 不复制后续流程列表、注意事项、Class Members / Related，避免不规范 HTML
    // 被浏览器重组后将整个页面剩余内容吞入 class 卡片。
    var $descH5 = null
    $remoteArticle.find('h5').each(function () {
      var t = $.trim($(this).text())
      if (/^(描述|Description)[:：]?$/i.test(t)) {
        $descH5 = $(this)
        return false
      }
    })
    if ($descH5 && $descH5.length) {
      var $summary = $descH5.nextAll('p').first().clone()
      if ($summary.length) {
        $summary.find('li, ul, ol, b').remove()
        $summary.find('code').filter(function () {
          return /MSDK\s+[\d.]+\s*开始支持|Supported\s+since\s+MSDK/i.test($(this).text())
        }).remove()
        // 清理摘要末尾因移除流程/版本产生的空白换行。
        var summaryHtml = $summary.html()
        if (summaryHtml) {
          $summary.html(summaryHtml.replace(/(?:\s|&nbsp;|<br\s*\/?>)+$/gi, ''))
        }
        if ($.trim($summary.text())) $content.append($summary)
      }

      // 在“描述”到下一 h5 的范围内只抽取版本 code，交由 transformInlineDoc
      // 统一渲染为标题右侧的 Since 徽标。
      var $sib = $descH5.next()
      while ($sib.length && !$sib.is('h5')) {
        var $sinceCode = $sib.is('code')
          ? $sib.filter(function () {
            return /MSDK\s+[\d.]+\s*开始支持|Supported\s+since\s+MSDK/i.test($(this).text())
          })
          : $sib.find('code').filter(function () {
            return /MSDK\s+[\d.]+\s*开始支持|Supported\s+since\s+MSDK/i.test($(this).text())
          }).first()
        if ($sinceCode.length) {
          $content.append($('<p></p>').append($sinceCode.clone()))
          break
        }
        $sib = $sib.next()
      }
    }

    $doc.empty().append($content.children())
    // 让整块可点击跳转到 class 独立页
    $doc.on('click', function (e) {
      // 内部真实链接允许正常工作
      if ($(e.target).closest('a').length) return
      window.location.href = href
    })
    // 触发事件，通知页面级增强器（如 ifm-page）可以对该 class-inline-doc 进行样式转换
    $doc.trigger('classInlineDocReady')
  }


  // 将每个内容块里的"MSDK X.X.X 开始支持"标签（<code><i>MSDK ...开始支持</i></code>）
  // 从描述段落中抽取，移到该内容块的最底部，作为独立的版本标签展示
  ;(function () {
    var re = /MSDK\s+[\d.]+\s*开始支持|Supported\s+since\s+MSDK\s*[\d.]+/i
    // 内容块的定义：
    //   · 顶层描述块 = .site-main .article 本体（放到 article 最后一个直接子元素之后不合适，
    //     应放在"描述:"段落之后、"类成员:"之前）
    //   · 每个 inline-doc = 独立内容块，放到其自身最底部
    function extractFrom($scope, $appendTarget) {
      var $tag = null
      $scope.find('code').each(function () {
        var $c = $(this)
        if (re.test($c.text())) {
          $tag = $c
          return false
        }
      })
      if (!$tag) return
      var $wrap = $('<div class="msdk-support-tag"></div>').append($tag.clone())
      $tag.remove()
      $appendTarget.append($wrap)
    }

    // 处理每个 inline-doc：放到 inline-doc 最底部
    $('.site-main .inline-doc').each(function () {
      var $doc = $(this)
      extractFrom($doc, $doc)
    })

    // 处理顶层描述块：把顶层（不在 inline-doc 内的）版本标签放到"类成员:"h5 之前
    var $article = $('.site-main .article').eq(0)
    var $topScope = $article.children().filter(function () {
      // 排除所有 inline-doc 及其后代已处理的内容
      return !$(this).hasClass('inline-doc')
    })
    // 用一个虚拟 wrapper 承载查找范围
    var $tmpTarget = $('<div></div>')
    // 从"描述:"h5 之后到"类成员:"h5 之前的区间提取
    var $memberH5 = null
    $article.children('h5').each(function () {
      var t = $.trim($(this).text())
      if (/^(类成员|Class Members|Members)[:：]?$/i.test(t)) {
        $memberH5 = $(this)
        return false
      }
    })
    if ($memberH5 && $memberH5.length) {
      var $range = $memberH5.prevAll()
      var $tagCode = null
      $range.find('code').each(function () {
        var $c = $(this)
        if (re.test($c.text())) {
          $tagCode = $c
          return false
        }
      })
      if ($tagCode) {
        var $wrap = $('<div class="msdk-support-tag"></div>').append($tagCode.clone())
        $tagCode.remove()
        $memberH5.before($wrap)
      }
    }
  })()

  // 2. 生成右侧 TOC：遍历所有 API 成员行（class / enum / method 都作为普通条目）
  var $tocList = $('.site-toc-list')
  if (!$tocList.length) return false

  // 收集 items：遍历 .article 内部所有 h4 与 api-row 按文档顺序，
  // 同时记录每行所属的最近 h4 分组（若无 h4 则为 null）。
  // 也记录 .api-col.left 里的中文小分类文本（如"显示模式"），供手工分组页 TOC 使用。
  var isCustomGroupedPage = $('.site-main .article').hasClass('custom-grouped-page')
  var items = []
  var currentH4 = null
  var currentLeft = null // 上一个非空的 .api-col.left 文本，用于让同分类相邻行继承
  var $walkRoot = $('.site-main .article').eq(0)
  if ($walkRoot.length) {
    $walkRoot.find('h4, .api-row').each(function () {
      var $el = $(this)
      // 跳过嵌套在展开框内的成员行
      if ($el.parents('.inline-doc').length) return
      if ($el.is('h4')) {
        currentH4 = { id: $el.attr('id') || '', title: $.trim($el.text()) }
        currentLeft = null // h4 切换时重置小分类
        return
      }
      // api-row
      var id = $el.attr('id')
      if (!id) return
      var type = $.trim($el.find('.api-col.middle').text())
      // 采集 name 时排除废弃徽章文本（避免污染 TOC 条目名）
      var $rightClone = $el.find('.api-col.right').clone()
      $rightClone.find('.deprecated-reason-badge').remove()
      var name = $.trim($rightClone.text())
      if (!name) return
      var leftText = $.trim($el.find('.api-col.left').text())
      if (leftText) currentLeft = leftText // 遇到非空 left 就更新当前小分类
      // 隐藏行（有展开框）：锚点指向其展开框，保证滚动定位准确
      var anchorId = id
      var $trigger = $el.find('.api-col.right a.trigger')
      if ($el.hasClass('api-row-hidden') && $trigger.length) {
        anchorId = $trigger.attr('href').replace('#', '')
      }
      // class 行也重定向到展开框（保持外链 <a> 不加 trigger 类，避免破坏跳转样式）
      var classInlineId = $el.attr('data-class-inline-id')
      if (classInlineId) {
        anchorId = classInlineId
      }
      items.push({
        id: id,
        anchorId: anchorId,
        type: type,
        name: name,
        h4: currentH4,       // 当前所属 h4 分组（可能是 null）
        subGroup: currentLeft // 当前所属小分类（可能是 null）
      })
    })
  }

  if (!items.length) {
    $('.site-toc').hide()
    return false
  }

  // TOC 分组构造
  function appendGroupTitle(text) {
    $tocList.append(
      $('<li class="toc-group">').append(
        $('<span class="toc-group-title">').text(text)
      )
    )
  }
  // 小分类二级标题（如"显示模式"），不可跳转、仅视觉分组
  function appendSubGroupTitle(text) {
    $tocList.append(
      $('<li class="toc-subgroup">').append(
        $('<span class="toc-subgroup-title">').text(text)
      )
    )
  }
  function appendItem(it) {
    var $a = $('<a>')
      .attr('href', '#' + it.anchorId)
      .attr('data-target', it.anchorId)
    if (it.type) {
      $a.append($('<span class="toc-type">').text(it.type))
    }
    $a.append(document.createTextNode(it.name))
    $tocList.append($('<li>').append($a))
  }

  if (isCustomGroupedPage) {
    // ===== 手工分组页（DJIValue 等）：按 h4 分组，组内按 subGroup 二级分组 =====
    // items 已按文档顺序，直接遍历渲染
    $tocList.addClass('toc-list-grouped')
    // 让 TOC 加宽（380px），三层层级视觉更清晰
    $('.site-toc').addClass('site-toc-wide')
    var lastH4Title = null
    var lastSubGroup = null
    items.forEach(function (it) {
      var h4Title = it.h4 ? it.h4.title : ''
      if (h4Title && h4Title !== lastH4Title) {
        appendGroupTitle(h4Title)
        lastH4Title = h4Title
        lastSubGroup = null
      }
      if (it.subGroup && it.subGroup !== lastSubGroup) {
        appendSubGroupTitle(it.subGroup)
        lastSubGroup = it.subGroup
      }
      appendItem(it)
    })
    // 概览 / 成员索引纳入滚动高亮序列（DOM 由 keepCustomToc 分支稍后 prepend 到 TOC 顶部）。
    // 不加入的话，onScroll 的初始 fallback 会高亮 items[0]（第一个成员）而非「概览」
    items.unshift({ anchorId: 'ifm-overview' }, { anchorId: 'ifm-members-nav' })
  } else {
    // ===== 普通页：按 type 分为"类成员"（method）/"相关类"（class/enum） =====
    var relatedItems = []
    var memberItems = []
    items.forEach(function (it) {
      var t = (it.type || '').toLowerCase()
      if (t === 'method') {
        memberItems.push(it)
      } else {
        relatedItems.push(it)
      }
    })
    if (memberItems.length) {
      appendGroupTitle('Class Members')
      memberItems.forEach(appendItem)
    }
    if (relatedItems.length) {
      appendGroupTitle('Related Classes')
      relatedItems.forEach(appendItem)
    }
  }

  // 点击 TOC 平滑滚动
  $tocList.on('click', 'a', function(e) {
    e.preventDefault()
    var id = $(this).attr('data-target')
    var $target = $('#' + id)
    if ($target.length) {
      $('html, body').animate({ scrollTop: $target.offset().top - 80 }, 300)
      window.history.pushState(null, null, '#' + id)
    }
  })

  // 滚动高亮当前项（默认高亮第一项，滚动后按位置切换）
  // 判定线放在 sub-nav 吸顶位置下方一点（视口 150px 处，
  // 即 header 60 + sub-nav 50 + 40 缓冲）：元素顶部越过这条线就算"进入阅读区"
  function onScroll() {
    var scrollTop = $(window).scrollTop()
    var threshold = scrollTop + 150
    var current = items[0] ? items[0].anchorId : null
    for (var i = 0; i < items.length; i++) {
      var $el = $('#' + items[i].anchorId)
      // 跳过不存在或位置无效的元素
      if (!$el.length) continue
      if ($el.css('display') === 'none') {
        // ifm 化页面成员行被隐藏（api-row-hidden），高亮定位回退到其展开卡片
        var $inlineEl = $('#' + items[i].anchorId + '_inline')
        if (!$inlineEl.length) continue
        $el = $inlineEl
      }
      if ($el.offset().top <= threshold) {
        current = items[i].anchorId
      }
    }

    $tocList.find('a').removeClass('active')
    if (current) {
      var $activeLink = $tocList.find('a[data-target="' + current + '"]').addClass('active')
      // TOC 内部自动滚动：active 项若不在可视区域，滚动到居中
      if ($activeLink.length) {
        var $tocScrollBox = $tocList.closest('.site-toc-inner')
        if ($tocScrollBox.length) {
          var boxTop = $tocScrollBox.offset().top
          var boxH = $tocScrollBox.outerHeight()
          var linkTop = $activeLink.offset().top
          var linkH = $activeLink.outerHeight()
          var curScroll = $tocScrollBox.scrollTop()
          var relTop = linkTop - boxTop + curScroll
          // 若 active 项超出上/下边界，居中定位
          if (linkTop < boxTop + 40 || linkTop + linkH > boxTop + boxH - 40) {
            var target = relTop - (boxH - linkH) / 2
            if (target < 0) target = 0
            $tocScrollBox.stop().animate({ scrollTop: target }, 200)
          }
        }
      }
    }
  }

  $(window).on('scroll', onScroll)
  onScroll()
})()

// ============================================================
// 页面级特化增强触发（放在所有 IIFE 之后，最后执行）
// 确保跑在 "平铺展开 / api-row-hidden" 逻辑之后，不被覆盖
// ============================================================
;(function () {
  if (!$('.documentation-page').length) return
  if (typeof window.__enhancePageSpecific === 'function') {
    window.__enhancePageSpecific()
  }
})()
