function visibleInParent(element) {
  var position = $(element).position().top
  return position >= 0 && position < $(element).offsetParent().height()
}

function hasFragment(link, fragment) {
  return $(link).attr("href").indexOf("#" + fragment) != -1
}

function findLinkByFragment(elements, fragment) {
  return $(elements).filter(function(i, e) { return hasFragment(e, fragment)}).first()
}

function setCurrentVarLink() {
  $('#vars li').removeClass('current')
  $('.public').
    filter(function(index) { return visibleInParent(this) }).
    each(function(index, element) {
      findLinkByFragment("#vars a", element.id).
        parent().
        addClass('current')
    })
}

$(window).load(setCurrentVarLink)
$(window).load(function() { $('#content').scroll(setCurrentVarLink) })
