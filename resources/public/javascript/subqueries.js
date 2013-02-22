function showActiveSubquery() {
  var subquery_select = $("#subquery_select").val();

  $(".subquery_form").hide();
  $("#subquery_form_" + subquery_select).show();
}

$(document).ready(function() {

  showActiveSubquery();
  $("#subquery_select").change(function () {
    showActiveSubquery();
  });
});
