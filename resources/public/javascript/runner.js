function constructCompileCollapsible(data) {
  var compile_output = data["compile-output"];
  var compile_errors = data["compile-errors"];

  var compile_collapse = $("<div">).accordion-group

  var compile_output_div = $("<pre>");
  compile_output_div.text(compile_output);

  var compile_errors_div = $("<pre>");
  compile_errors_div.text(compile_errors);

}

function updateRunnerOutput(data) {
  var compile

  $("#runner_output").text(JSON.stringify(data));
}

function pollAndUpdateRunnerOutput() {
  // Lazy man's polling
  $.get("/get-runner-output", {},
    function(data) {
      updateRunnerOutput(JSON.parse(data));
      setTimeout(pollAndUpdateRunnerOutput(), 1000);
    }
  );
}

$(document).ready(function() {
  pollAndUpdateRunnerOutput();
})
