Searchdoc = {};

// navigation.js ------------------------------------------

Searchdoc.Navigation = new function() {
    this.initNavigation = function() {
        var _this = this;

        $(document).keydown(function(e) {
            _this.onkeydown(e);
        }).keyup(function(e) {
            _this.onkeyup(e);
        });

        this.navigationActive = true;
    }

    this.setNavigationActive = function(state) {
        this.navigationActive = state;
        this.clearMoveTimeout();
    }


    this.onkeyup = function(e) {
        if (!this.navigationActive) return;
        switch (e.keyCode) {
            case 37: //Event.KEY_LEFT:
            case 38: //Event.KEY_UP:
            case 39: //Event.KEY_RIGHT:
            case 40: //Event.KEY_DOWN:
            case 73: // i - qwerty
            case 74: // j
            case 75: // k
            case 76: // l
            case 67: // c - dvorak
            case 72: // h
            case 84: // t
            case 78: // n
                this.clearMoveTimeout();
                break;
        }
    }

    this.onkeydown = function(e) {
        if (!this.navigationActive) return;
        switch (e.keyCode) {
            case 37: //Event.KEY_LEFT:
            case 74: // j (qwerty)
            case 72: // h (dvorak)
                if (this.moveLeft()) e.preventDefault();
                break;
            case 38: //Event.KEY_UP:
            case 73: // i (qwerty)
            case 67: // c (dvorak)
                if (e.keyCode == 38 || e.ctrlKey) {
                    if (this.moveUp()) e.preventDefault();
                    this.startMoveTimeout(false);
                }
                break;
            case 39: //Event.KEY_RIGHT:
            case 76: // l (qwerty)
            case 78: // n (dvorak)
                if (this.moveRight()) e.preventDefault();
                break;
            case 40: //Event.KEY_DOWN:
            case 75: // k (qwerty)
            case 84: // t (dvorak)
                if (e.keyCode == 40 || e.ctrlKey) {
                    if (this.moveDown()) e.preventDefault();
                    this.startMoveTimeout(true);
                }
                break;
            case 9: //Event.KEY_TAB:
            case 13: //Event.KEY_RETURN:
                if (this.$current) this.select(this.$current);
                break;
            case 83: // s (qwerty)
            case 79: // o (dvorak)
                if (e.ctrlKey) {
                    $('#search').focus();
                    e.preventDefault();
                }
                break;
        }
        if (e.ctrlKey && e.shiftKey) this.select(this.$current);
    }

    this.clearMoveTimeout = function() {
        clearTimeout(this.moveTimeout);
        this.moveTimeout = null;
    }

    this.startMoveTimeout = function(isDown) {
        if (!(/firefox/.test(navigator.userAgent.toLowerCase())) && !(/opera/.test(navigator.userAgent.toLowerCase()))) return;
        if (this.moveTimeout) this.clearMoveTimeout();
        var _this = this;

        var go = function() {
            if (!_this.moveTimeout) return;
            _this[isDown ? 'moveDown' : 'moveUp']();
            _this.moveTimout = setTimeout(go, 100);
        }
        this.moveTimeout = setTimeout(go, 200);
    }

    this.moveRight = function() {}

    this.moveLeft = function() {}

    this.move = function(isDown) {}

    this.moveUp = function() {
        return this.move(false);
    }

    this.moveDown = function() {
        return this.move(true);
    }
}


// scrollIntoView.js --------------------------------------

function scrollIntoView(element, view) {
    if (!element || !view) return
    var offset, viewHeight, viewScroll, height;
    offset = element.offsetTop;
    height = element.offsetHeight;
    viewHeight = view.offsetHeight;
    viewScroll = view.scrollTop;
    if (offset - viewScroll + height > viewHeight) {
        view.scrollTop = offset - viewHeight + height;
    }
    if (offset < viewScroll) {
        view.scrollTop = offset;
    }
}

// panel.js -----------------------------------------------

Searchdoc.Panel = function(element, data, tree, frame) {
    this.$element = $(element);
    this.$input = $('input', element).eq(0);
    this.$result = $('.result ul', element).eq(0);
    this.frame = frame;
    this.$current = null;
    this.$view = this.$result.parent();
    this.data = data;
    this.searcher = new Searcher(data.index);
    // this.tree = new Searchdoc.Tree($('.tree', element), tree, this);
    this.init();
    this.resultData1 = [];
    this.resultData2 = [];
}

Searchdoc.Panel.prototype = $.extend({}, Searchdoc.Navigation, new function() {
    var suid = 1;
    this.searchValue = '';

    this.init = function() {
        var _this = this;
        var observer = function() {
            _this.search(_this.$input[0].value);
        };
        this.$input.keyup(observer);
        this.$input.click(observer); // mac's clear field

        this.searcher.ready(function(results, isLast, state) {
            _this.addResults(results, isLast, state);
        })

        this.$result.click(function(e) {
            if (_this.$current) _this.$current.removeClass('current');
            _this.$current = $(e.target).closest('li').addClass('current');
            _this.select();
            _this.$input.focus();
        });

        this.initNavigation();
        this.setNavigationActive(false);
    }

    this.search = function(value, selectFirstMatch, defaultSelected) {
        value = jQuery.trim(value).toLowerCase();
        this.searchValue = value
        this.selectFirstMatch = selectFirstMatch;
        this.defaultSelected = defaultSelected || -1;
        if (value) {
            // 搜索有输入：打开悬浮结果面板（CSS 上通过 .panel_results_open 定位/显示）
            $('#panel').addClass('panel_results_open').css('height', '')
            $('.clean-search').show()
            // 动态计算面板位置：跟随搜索框正下方 fixed 悬浮，逃出 sidebar 裁剪
            positionResultPanel()
        } else {
            // 无输入：关闭面板
            $('#panel').removeClass('panel_results_open').css('height', '')
            $('.clean-search').hide()
        }
        if (value != this.lastQuery) {
            this.lastQuery = value;
            this.firstRun = true;
            this.searcher.find(value);
        }
    }
    this.getSubTitle = function(title) {
        var lastDotePos = title.lastIndexOf('::');
        // result.namespace = result.title.substr(0, lastDotePos);
        if (lastDotePos > -1) {
            return title.substr(lastDotePos + 2);
        } else {
            return title;
        }
    }

    this.addResults = function(results, isLast, state) {
        var target = this.$result.get(0);
        if (this.firstRun && (results.length > 0 || isLast)) {
            this.$current = null;
            this.$result.empty();
            this.resultData1 = [];
            this.resultData2 = [];
        }
        if ([0, 1, 2].indexOf(state.pass) > -1) {
            this.resultData1 = this.resultData1.concat(results)
        } else {
            for (var i = 0; i < results.length; i++) {
                if (results[i].apiType != 1) {
                    this.resultData2.push(results[i]);
                }
            }
        }
        if (this.firstRun && results.length > 0) {
            this.firstRun = false;
        }
        if (isLast) {

            for (var i = 0; i < this.resultData1.length; i++) {
                var ele = this.resultData1[i];
                //TODO 删掉如果search_index.js取消了层级
                ele.title = this.getSubTitle(ele.title)
                if (ele.title.toLowerCase() == this.searchValue.toLowerCase()) {
                    ele.apiType = 10;
                }
                ele.oldIndex = i;
            }
            for (var i = 0; i < this.resultData2.length; i++) {
                var ele = this.resultData2[i];
                //TODO 删掉如果search_index.js取消了层级
                ele.title = this.getSubTitle(ele.title)
                ele.oldIndex = i;
            }
            var finalResult = this.resultData1.sort(function(a, b) {
                return b.apiType - a.apiType || a.oldIndex - b.oldIndex;
            })
            finalResult = finalResult.concat(this.resultData2.sort(function(a, b) {
                return b.apiType - a.apiType || a.oldIndex - b.oldIndex;
            }))
            for (var i = 0, l = finalResult.length; i < l; i++) {
                target.appendChild(renderItem.call(this, finalResult[i], i));
            };
        }
        if (isLast && this.defaultSelected > -1) {
            this.$current = $(target.children[this.defaultSelected]);
            this.$current.addClass('current');
            if (this.selectFirstMatch) this.select();
            scrollIntoView(this.$current[0], this.$view[0])
        }
        if (/msie/.test(navigator.userAgent.toLowerCase())) this.$element[0].className += '';
    }

    this.open = function(src) {
        this.frame.location.href = '../' + src;
        if (this.frame.highlight) this.frame.highlight(src);
    }

    this.select = function() {
        var href = this.$current.data('path') || ''
        // 无论 href 是否有效，点击后都关闭搜索结果面板
        // （保留搜索框内容，用户可手动清空）
        try {
            $('#panel').removeClass('panel_results_open')
        } catch (e) {}
        if (href && (href.indexOf('undefined') != href.length - 9)) {
            location.href = href;
        }
    }

    this.move = function(isDown) {
        if (!this.$current) return;
        var $next = this.$current[isDown ? 'next' : 'prev']();
        if ($next.length) {
            this.$current.removeClass('current');
            $next.addClass('current');
            scrollIntoView($next[0], this.$view[0]);
            this.$current = $next;
        }
        return true;
    }

    function renderItem(result, index) {
        var li = document.createElement('li'),
            html = '',
            badge = result.badge;

        var hitHtml = hlt(result.title)
        html += '<h1>' + getMainContent(hitHtml);
        // html += '<h1>' + hitHtml;
        // if (result.params) html += '<i>' + result.params + '</i>';
        html += '</h1>';
        html += '<p>';
        if (typeof badge != 'undefined') {
            html += '<span class="badge badge_' + (badge % 6 + 1) + '">' + escapeHTML(this.data.badges[badge] || 'unknown') + '</span>';
        }
        html += hlt(result.namespace) + '</p>';
        if (result.snippet) html += '<p class="snippet">' + sanitizeSnippet(result.snippet) + '</p>';
        li.innerHTML = html;
        var prefix = window.pathPrefix; //
        if (typeof prefix == 'undefined') {
            var regx = new RegExp('/', 'g');
            var res = location.pathname.match(regx);
            var dirCount = res.length - 1;
            prefix = '';
            for (var i = 0; i < dirCount; i++) {
                prefix += '../';
            }
        }
        if (prefix == '') prefix = './';
        // 中文搜索词必须编码后再进入 URL，否则目标页解码乱码
        jQuery.data(li, 'path', prefix + result.path.substr(1) + '?search=' + encodeURIComponent(this.searchValue) + '&i=' + index + '&' + result.params);
        return li;
    }

    function getMainContent(content) {
        var beg = content.indexOf('<b>');
        var end = content.lastIndexOf('</b>');
        if (end > -1) end += 4;
        var curLength = content.substr(beg, end).replace(/<b>/g, '').replace(/<\/b>/g, '').length;
        var maxLength = 50;
        var restLength = (maxLength - curLength) / 2;
        var prefix = '...',
            tail = '...';
        // console.log('-->', beg, end, content, content.substr(beg, end).replace(/<b>/g, '').replace(/<\/b>/g, ''), curLength, restLength)
        if (restLength > 0) {
            beg -= restLength;
            end += restLength;
            var begRest = 0;
            var endRest = 0;
            if (beg < 0) {
                begRest = -beg;
                beg = 0;
            }
            if (end > content.length) {
                endRest = end - content.length;
                end = content.length;
            }
            if (begRest > 0) end += begRest;
            if (endRest > 0) beg -= endRest;
        }
        if (beg <= 0) {
            beg = 0;
            prefix = '';
        }
        if (end >= content.length) {
            end = content.length;
            tail = '';
        }
        // console.log('--<', prefix + content.substr(beg, end) + tail)
        return prefix + content.substr(beg, end) + tail;
    }

    function hlt(html) {
        return escapeHTML(html).replace(/\u0001/g, '<b>').replace(/\u0002/g, '</b>')
    }

    function escapeHTML(html) {
        return html.replace(/[&<>]/g, function(c) {
            return '&#' + c.charCodeAt(0) + ';';
        });
    }

    // 动态定位搜索结果面板（fixed 悬浮）：
    // 让面板始终紧贴搜索框正下方，宽度对齐搜索框
    function positionResultPanel() {
        var $searcher = $('.site-sidebar .searcher')
        var $result = $('#panel.panel_results_open .result')
        if (!$searcher.length || !$result.length) return
        var rect = $searcher[0].getBoundingClientRect()
        var top = rect.bottom
        var left = rect.left
        var width = rect.width
        var vh = window.innerHeight || document.documentElement.clientHeight
        $result.css({
            top: top + 'px',
            left: left + 'px',
            width: width + 'px',
            maxHeight: (vh - top - 20) + 'px'
        })
    }
    // 页面滚动 / 窗口变化时同步位置
    $(window).on('scroll resize', function () {
        if ($('#panel').hasClass('panel_results_open')) {
            positionResultPanel()
        }
    })
    // 侧栏内部滚动（sticky 状态下 searcher 位置不动，但保险起见也监听）
    $(document).on('scroll', '.site-sidebar', function () {
        if ($('#panel').hasClass('panel_results_open')) {
            positionResultPanel()
        }
    })

    // 搜索结果 snippet：白名单渲染部分 HTML 标签（<i>/<b>/<code>/<br>），
    // 其他标签一律剥离；同时把已被转义的 &lt;xxx&gt; 还原回真实标签再重新过滤。
    // 这样可以让 "MSDK 5.20.0开始支持" 这种 <i>...</i> 正常显示成斜体，
    // 而不是原样输出 "<i>MSDK...</i>" 字符串。
    function sanitizeSnippet(html) {
        if (!html) return '';
        // 先把 HTML 实体反转义回真实标签
        var s = String(html)
            .replace(/&lt;/g, '<')
            .replace(/&gt;/g, '>')
            .replace(/&amp;/g, '&');
        // 白名单标签：i, b, code, br（含闭合形式）
        // 步骤：1. 保护白名单标签为占位符  2. 剥离剩余所有 <...>  3. 还原白名单
        var whitelist = ['i', 'b', 'code', 'br'];
        var placeholders = [];
        s = s.replace(/<\/?([a-zA-Z][a-zA-Z0-9]*)\s*[^>]*>/g, function(match, tag) {
            if (whitelist.indexOf(tag.toLowerCase()) !== -1) {
                // 只保留标签本身，剥掉属性
                var isClose = match.charAt(1) === '/';
                var isSelfClose = /\/>$/.test(match);
                var safe = '<' + (isClose ? '/' : '') + tag.toLowerCase() + (isSelfClose ? '/' : '') + '>';
                placeholders.push(safe);
                return '\u0003' + (placeholders.length - 1) + '\u0004';
            }
            return '' // 剥离
        });
        // 剩余的散落 < > 转义
        s = s.replace(/[<>]/g, function(c) { return c === '<' ? '&lt;' : '&gt;'; });
        // 还原白名单标签
        s = s.replace(/\u0003(\d+)\u0004/g, function(_, i) {
            return placeholders[parseInt(i, 10)] || '';
        });
        return s;
    }

});

// tree.js ------------------------------------------------