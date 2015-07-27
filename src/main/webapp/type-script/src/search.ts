var $searchField = $('#search-field')
var $searchResults = $('#search-results')

interface SearchResult {
  title: string
}

function resultDom(searchResult: SearchResult): HTMLElement  {
  return $("<li></li>").text(searchResult.title)[0]
}

function apiCall(query: string) {
  var searchURI = '/api/0.1/search/' + encodeURIComponent(query)
  $.getJSON(searchURI, function(data) {
    $searchResults.empty()
    var $list = $('<ul></ul>')
    for (var idx in data) {
      $list.append(resultDom(data[idx]))
    }
    $searchResults
      .append($list)
      .appendTo($searchResults)
  })
}

$searchField.keydown(function(e) {
  var code = e.keyCode || e.which
  if (code != 13) {
    return true
  }
  var text = $searchField.val()
  apiCall(text)
})
