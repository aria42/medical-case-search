var $searchBtn = $('#search-submit');
var $searchField = $('#search-field');
function apiCall(query) {
    var searchURI = '/api/0.1/search/' + encodeURIComponent(query);
    $.getJSON(searchURI, function (data) {
    });
}
$searchBtn.on('click', function (e) {
    var text = $searchField.val();
    apiCall(text);
    console.info('search-clicked ' + text);
});
//# sourceMappingURL=ts-compiled.js.map