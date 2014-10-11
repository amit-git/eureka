var Utils = (function () {
    var curEnv = {};
    $.get("/getenv", function (data) {
        if (data) {
            curEnv = data;
        }
    }).fail(function () {
        console.log("Call failed to get env");
    });

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
        },

        getCurrentRegion: function () {
            return curEnv['region'];
        },

        getCurrentEnv: function () {
            return curEnv['env'];
        },

        buildNACLinkForInstance : function(inst) {
            var link = 'http://asgard' + curEnv['env'] + '.netflix.com/' + curEnv['region'] + '/instance/show/' + inst;
            return '<a target="_blank" href="' + link + '">' + inst + '</a>';
        }
    }
})();

