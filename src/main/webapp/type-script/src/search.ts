var $searchField = $('#search-field')
var $searchResults = $('#search-results')

function apiCall(query: string) {
  var searchURI = '/api/0.1/search/' + encodeURIComponent(query)
  $.getJSON(searchURI, function(data) {
    $searchResults.empty()
    var $list = $('<ul></ul>')
    for (result: data) {

    }
    $searchResults.append($list)
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
