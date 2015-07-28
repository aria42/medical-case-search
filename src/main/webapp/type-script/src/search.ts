var $searchField = $('#search-field')
var $searchResults = $('#search-results')

interface SearchResult {
  title: string
  authors: Array<string>
  journalTitle: string
  year: number
  snippet: string
}

function ellipsize(str: string, maxChars: number): string {
  var ellipsis = "..."
  var numCharsFromStr = maxChars - ellipsis.length;
  if (str.length <= maxChars) {
    return str
  }
  str.substring(0, numCharsFromStr) + ellipsis
}

function resultDom(searchResult: SearchResult): HTMLElement  {
  var titleLine = searchResult.title + " - " +
                  searchResult.journalTitle + " (" + searchResult.year + ")"
  var $elem = $("<li></li>")
    .addClass("snippet")
    .append($("<div></div>")
              .addClass("title")
              .text(titleLine))
    .append($("<div></div>").addClass("author")
            .text(ellipsize(searchResult.authors.join(), 100)))
    .append($("<div></div>").addClass("abstract")
            .text(ellipsize(searchResult.snippet, 250)))
  // pull out the HTMLElement
  return $elem[0]
}

function apiCall(query: string) {
  var searchURI = '/api/0.1/search/' + encodeURIComponent(query)
  $.getJSON(searchURI, function(data) {
    $searchResults.empty()
    var $list = $('<ul></ul>').addClass("snippet-results")
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
