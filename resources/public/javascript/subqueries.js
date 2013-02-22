function showActiveSubquery() {
  var subquery_select = $("#subquery_select").val();

  $(".subquery_form").hide();
  $("#subquery_form_" + subquery_select).show();
}

function submitInputSubquery(form) {
  var file_path = form.find("#subquery_form_input_file-path").val();

  $.get("/input-template", {"file-path" : file_path},
    function(input_template) {
      form.find(".output .input_template").text(input_template);

      $.get("/preview-file", {"file-path" : file_path},
        function(file_preview) {
          form.find(".output .file_preview").text(file_preview);
        }
      )
    }
  );
}

function submitGenericSubquery(type, form) {
  var arg_map  = {subquery_type : type};

  var some_empty = false;
  form.find(".subquery_input").each(function() {
    if ((!$(this).val()) || $(this).val() == "")
      some_empty = true;

    arg_map[$(this).attr("name")] = $(this).val();
  });

  if (!some_empty) {
    $.get("/get-subquery-template", arg_map,
      function(query_template) {
        form.find(".output .query_template").text(query_template);
      }
    );
  }
}


function submitForm(form) {
  var subquery_type = form.attr("subquery_type");

  if (subquery_type == "input") {
    submitInputSubquery(form)
  } else {
    submitGenericSubquery(subquery_type, form)
  }
}

function submitParentForm(input) {
  submitForm(input.parents(".subquery_form").first());
}

$(document).ready(function() {

  showActiveSubquery();
  $("#subquery_select").change(function () {
    showActiveSubquery();
  });

  $(".subquery_input").change(function(e) {
    submitParentForm($(this));
  });
});
