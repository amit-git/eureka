function Utils() {
    return {
        getCurrentPageDomain: function () {
            var hostName = document.location.hostname;
            var port = document.location.port;
            return hostName + ":" + port;
        },

        formatNumber: function (num) {
            if (typeof num === 'number') {
                if (num.toString().indexOf('.') > -1) {
                    // decimal
                    return num.toFixed(2);
                }
            }
            return num;
        }
    }
}
