function submitText(text) {
  $.post('/run', {text : text},
    function(data) {
      console.log(data);
    }
  );
}

$(document).ready(function() {
  var editor = ace.edit("editor");
  editor.setTheme("ace/theme/monokai");
  editor.getSession().setMode("ace/mode/clojure");

  $('#submit_link').on('click', function() {
    submitText(editor.getValue());
  })
})
