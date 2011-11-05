function orderBy(func) {
  return function(x, y) { return func(x) - func(y) }
}

function distanceFromScreenTop(element) {
  return Math.abs($(element).position().top - window.scrollY)
}

function topVisibleElement(elements) {
  return $(elements).sort(orderBy(distanceFromScreenTop)).first()
}

function hasFragment(link, fragment) {
  return $(link).attr("href").indexOf("#" + fragment) != -1
}

function findLinkByFragment(elements, fragment) {
  return $(elements).filter(function(i, e) { return hasFragment(e, fragment)}).first()
}

function setCurrentVarLink() {
  var name = topVisibleElement('.public').attr('id')
  var link = findLinkByFragment('#vars a', name)

  $('#vars li').removeClass('current')
  link.parent().addClass('current')
}

$(window).load(setCurrentVarLink)
$(window).scroll(setCurrentVarLink)
