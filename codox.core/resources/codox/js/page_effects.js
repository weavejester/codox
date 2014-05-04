function visibleInParent(element) {
    var position = $(element).position().top
    return position > -50 && position < ($(element).offsetParent().height() - 50)
}

function hasFragment(link, fragment) {
    return $(link).attr("href").indexOf("#" + fragment) != -1
}

function findLinkByFragment(elements, fragment) {
    return $(elements).filter(function(i, e) { return hasFragment(e, fragment)}).first()
}

function scrollToCurrentVarLink(elements) {
    var elements = $(elements);
    var parent   = elements.offsetParent();

    if (elements.length == 0) return;

    var top    = elements.first().position().top;
    var bottom = elements.last().position().top + elements.last().height();

    if (top >= 0 && bottom <= parent.height()) return;

    if (top < 0) {
        parent.scrollTop(parent.scrollTop() + top);
    }
    else if (bottom > parent.height()) {
        parent.scrollTop(parent.scrollTop() + bottom - parent.height());
    }
}

function setCurrentVarLink() {
    $('#vars li').removeClass('current')
    $('.public').
        filter(function(index) { return visibleInParent(this) }).
        each(function(index, element) {
            findLinkByFragment("#vars a", element.id).
                parent().
                addClass('current')
        });
    scrollToCurrentVarLink('#vars li.current');
}

var hasStorage = (function() { try { return localStorage.getItem } catch(e) {} }())

function scrollPositionId(element) {
    var directory = window.location.href.replace(/[^\/]+\.html$/, '')
    return 'scroll::' + $(element).attr('id') + '::' + directory
}

function storeScrollPosition(element) {
    if (!hasStorage) return;
    localStorage.setItem(scrollPositionId(element) + "::x", $(element).scrollLeft())
    localStorage.setItem(scrollPositionId(element) + "::y", $(element).scrollTop())
}

function recallScrollPosition(element) {
    if (!hasStorage) return;
    $(element).scrollLeft(localStorage.getItem(scrollPositionId(element) + "::x"))
    $(element).scrollTop(localStorage.getItem(scrollPositionId(element) + "::y"))
}

function persistScrollPosition(element) {
    recallScrollPosition(element)
    $(element).scroll(function() { storeScrollPosition(element) })
}

function sidebarContentWidth(element) {
    var widths = $(element).find('span').map(function() { return $(this).width() })
    return Math.max.apply(Math, widths)
}

function resizeNamespaces() {
    var width = sidebarContentWidth('#namespaces') + 30
    $('#namespaces').css('width', width)
    $('#vars, .namespace-index').css('left', width + 1)
    $('.namespace-docs').css('left', $('#vars').width() + width + 2)
}

function resizeVars() {
    if ($('#vars').length == 0) return;
    var width = sidebarContentWidth('#vars') + 30
    $('#vars').css('width', width)
    $('.namespace-docs').css('left', $('#namespaces').width() + width + 2)
}

function resizeSidebars() {
    resizeNamespaces()
    resizeVars()
}

$(window).ready(resizeSidebars)
$(window).ready(setCurrentVarLink)
$(window).ready(function() { persistScrollPosition('#namespaces')})
$(window).ready(function() {
    $('#content').scroll(setCurrentVarLink)
    $(window).resize(setCurrentVarLink)
})
