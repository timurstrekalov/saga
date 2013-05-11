(function () {

    var w = window;

    if (w.__saga_timeouts) {
        return;
    }

    var __saga_setTimeout = w.setTimeout;
    var __saga_clearTimeout = w.clearTimeout;
    var __saga_setInterval = w.setInterval;
    var __saga_clearInterval = w.clearInterval;

    w.__saga_timeouts = {};

    var pushTimeout = function (func, args) {
        var timeoutId = (func.apply(this, args));
        w.__saga_timeouts[timeoutId] = 1;

        return timeoutId;
    };

    var removeTimeout = function (func, id) {
        var result = func.call(this, id);
        delete w.__saga_timeouts[id];

        return result;
    };

    w.setTimeout = function () {
        return pushTimeout(__saga_setTimeout, arguments);
    };

    w.clearTimeout = function (id) {
        return removeTimeout(__saga_clearTimeout, id);
    };

    w.setInterval = function () {
        return pushTimeout(__saga_setInterval, arguments);
    };

    w.clearInterval = function (id) {
        return removeTimeout(__saga_clearInterval, id);
    };

})();