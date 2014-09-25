function Utils() {
    return {
        getCurrentPageDomain : function() {
            var hostName = document.location.hostname;
            var port = document.location.port;
            return hostName + ":" + port;
        }
    }
}
