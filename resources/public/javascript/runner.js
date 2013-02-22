function createCollapsible(header, content, id) {
  var collapsible_content_id = id + "_content";
  var collapsible = $("#collapsible-template").clone().attr("id", id);

  collapsible.find(".collapse_link").text(header).click(function() {
    collapsible.find(".content").toggle();
  });

  collapsible.find(".content").attr("id", collapsible_content_id).append(content);
  collapsible.show();
  return collapsible;
}

function updateCollapsible(collapsible, header, content) {
  collapsible.find(".collapse_link").text(header);
  collapsible.find(".content").empty().append(content);
}

function updateCompileCollapsible(runner_output, data) {
  var compile_collapse_id = "compile_output_collapsible";
  if (runner_output.find("#" + compile_collapse_id).length == 0) {
    runner_output.append(createCollapsible("Compile Output", "In progress", compile_collapse_id));
  }

  var compile_collapsible = runner_output.find("#" + compile_collapse_id);

  var compile_output = data["compile-output"].join("\n");
  var compile_errors = data["compile-errors"].join("\n");
  var compile_collapse_content = $("<div>");

  if (compile_output && compile_output.length > 0) {
    var compile_output_div = $("<pre>");
    compile_output_div.text(compile_output);

    compile_collapse_content.append("<h3>Compile Output</h3>");
    compile_collapse_content.append(compile_output_div);
  }

  if (compile_errors && compile_errors.length > 0) {
    var compile_errors_div = $("<pre>");
    compile_errors_div.text(compile_errors);
    compile_collapse_content.append("<h3>Compile Errors</h3>");
    compile_collapse_content.append(compile_errors_div);
  }

  var runner_status = data["status"];
  var header = "Compile Output";
  if (runner_status == "compiling")
    header += " (in progress)";
  else if (runner_status == "compile-fail")
    header += " (failed)";

  updateCollapsible(compile_collapsible, header, compile_collapse_content);
}

function updateCallCollapsible(runner_output, data) {
  var call_collapse_id = "call_output_collapsible";
  if (runner_output.find("#" + call_collapse_id).length == 0) {
    runner_output.append(createCollapsible("Call Output", "In progress", call_collapse_id));
  }

  var call_collapsible = runner_output.find("#" + call_collapse_id);

  var call_errors = data["call-errors"].join("\n");
  var call_collapse_content = $("<div>");

  if (call_errors && call_errors.length > 0) {
    var call_errors_div = $("<pre>");
    call_errors_div.text(call_errors);
    call_collapse_content.append("<h3>Console Output</h3>");
    call_collapse_content.append(call_errors_div);
  }

  var runner_status = data["status"];
  var header = "Call Console Output";
  if (runner_status == "calling")
    header += " (in progress)";
  else if (runner_status == "call-fail")
    header += " (failed)";

  updateCollapsible(call_collapsible, header, call_collapse_content);
}

function updateRunnerOutput(data) {
  var runner_output = $("#runner_output");
  updateCompileCollapsible(runner_output, data);
  updateCallCollapsible(runner_output, data);
}

function pollAndUpdateRunnerOutput() {
  // Lazy man's polling
  $.get("/get-runner-output", {},
    function(data) {
      updateRunnerOutput(data);
      if (data.running) {
        setTimeout(pollAndUpdateRunnerOutput, 1000);
      }
    },
    "json"
  );
}

$(document).ready(function() {

})
