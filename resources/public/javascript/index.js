function submitText(text) {
  $.post('/run', {text : text},
    function(data) {
      console.log(data);
    }
  );

  $("#runner_output").empty();
  setTimeout(pollAndUpdateRunnerOutput, 1000);
}


$(document).ready(function() {
  editor = ace.edit("editor");
  editor.setTheme("ace/theme/monokai");
  editor.getSession().setMode("ace/mode/clojure");

  function persistEditor() {
    if(localStorage) {
      localStorage["editor"] = editor.getValue();
    }
  }
  function restoreEditor() {
    if(localStorage) {
        if (localStorage["editor"]) {
            editor.setValue(localStorage["editor"]);
            editor.clearSelection();
        }
    }
  }
  window.onbeforeunload = persistEditor;
  restoreEditor();

  $('#submit_link').on('click', function() {
    submitText(editor.getValue());
  })
  $("#reset_storage").click(function(e) {
    e.preventDefault();
    delete localStorage["editor"];
    window.onbeforeunload = null;
    location.reload();
  });
})
