(function () {
    // lazily create a namespace
    var saga = window.__saga = window.__saga || {};
    if (!saga.completed) {
        saga.completed = function() {
            function detectJasmineCompletion() {
                return window.reporter && window.reporter.finished;
            }

            // this space reserved for detecting completeness for other frameworks

            // make sure we return true/false rather than truthy/falsy
            return Boolean(detectJasmineCompletion());
        }
    }
})();
