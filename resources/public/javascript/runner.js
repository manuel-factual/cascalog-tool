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

function updateSingleHadoopJobCollapsible(runner_output, job_id, job_status_map) {
  var collapse_id = "hadoop_job_collapsible_" + job_id;
  var link_url_content = $("<a>").attr("href", job_status_map["job_url"]).attr("target", "_blank").text("Job Tracker Url");
  if (runner_output.find("#" + collapse_id).length == 0) {
    runner_output.append(createCollapsible("Step " + job_status_map["step"], link_url_content, collapse_id));
  } else {
    var header = "Step " + job_status_map["step"];
    if (job_status_map["job_status"]) {
      header += " (" + job_status_map["job_status"] + ")";
    }

    updateCollapsible(runner_output.find("#" + collapse_id), header, link_url_content, collapse_id);
  }
}

function updateHadoopJobCollapsibles(runner_output, data) {
  var hadoop_job_statuses = data["hadoop-job-status"]["steps"]

  for (var idx in hadoop_job_statuses) {
    var this_status = hadoop_job_statuses[idx];
    updateSingleHadoopJobCollapsible(runner_output, this_status["job_id"], this_status);
  }
}

function updateOutputFileCollapsibles(runner_output, data) {
  var output_files = data["hadoop-job-status"]["output"];
  if (data["status"] == "completed") {
    for (var idx in output_files) {

      var output_file = output_files[idx];

      var file_path = output_file["file_path"];
      var file_url = output_file["file_url"];

      var content_div = $("<div>").append($("<a>").attr("href", file_url).attr("target","_blank").text("Link to HDFS File Browser"));

      runner_output.append(createCollapsible("Completed Output - " + file_path, content_div, "output_" + idx));
    }
  }
}

function updateRunnerOutput(data) {
  var runner_output = $("#runner_output");
  updateCompileCollapsible(runner_output, data);
  updateCallCollapsible(runner_output, data);
  updateHadoopJobCollapsibles(runner_output, data);
  updateOutputFileCollapsibles(runner_output, data);
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
