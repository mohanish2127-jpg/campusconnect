(function () {
    var LOCAL = "http://localhost:8080";
    var host = window.location.hostname;
    var isWeb = window.location.protocol.indexOf("http") === 0;
    var isLocalHost = host === "localhost" || host === "127.0.0.1";

    var base = LOCAL;
    if (typeof window.CC_API_BASE === "string" && window.CC_API_BASE) {
        base = window.CC_API_BASE.replace(/\/+$/, "");
    } else if (isWeb && !isLocalHost) {
        base = window.location.origin;
    } else if (isWeb && isLocalHost && window.location.port === "8080") {
        base = window.location.origin;
    }
    window.CC_BASE = base;

    if (base !== LOCAL && window.fetch) {
        var original = window.fetch.bind(window);
        window.fetch = function (input, init) {
            if (typeof input === "string" && input.indexOf(LOCAL) === 0) {
                input = base + input.substring(LOCAL.length);
            }
            return original(input, init);
        };
    }
})();
