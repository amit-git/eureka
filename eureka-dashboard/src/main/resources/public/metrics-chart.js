function MetricsChart(options) {
    options = options || {};
    var containerId = options.containerId || 'chart';
    var yAxisId = options.yAxisId || (containerId + '-y_axis');
    var width = options.width || 540;
    var height = options.height || 240;
    var palette = new Rickshaw.Color.Palette();

    var data = [];

    var graph; // chart obj

    function init() {
        graph = new Rickshaw.Graph({
            element : document.querySelector("#" + containerId),
            width   : width,
            height  : height,
            renderer: 'line',
            series  : new Rickshaw.Series([
                {
                    name : "Northeast",
                    data : data,
                    color: palette.color()
                }
            ])
        });

        new Rickshaw.Graph.Axis.Time({ graph: graph });

        var y_axis = new Rickshaw.Graph.Axis.Y({
            graph      : graph,
            orientation: 'left',
            tickFormat : Rickshaw.Fixtures.Number.formatKMBT,
            element    : document.getElementById(yAxisId)
        });

    }

    function render() {
        graph.render();
    }

    function clear() {
        d3.select('#' + containerId).remove();
        d3.select('#' + containerId + '-cont').remove();
    }

    var webSocket;

    function loadData(dataSrc) {
        webSocket = new WebSocket("ws://localhost:9000/sub");

        webSocket.onopen = function (evt) {
            console.log("On open (" + dataSrc + ")");
            webSocket.send(JSON.stringify({cmd: "getStream", ds: dataSrc}));
        };

        webSocket.onmessage = function (evt) {
            console.log("got data for " + dataSrc);
            if (evt.data) {
                var jsonResp = JSON.parse(evt.data);
                if (jsonResp.values && 'length' in jsonResp.values) {
                    var newData = jsonResp.values.map(function (val) {
                        return { x: (val[0] / 1000), y: +val[1]};
                    });

                    // update series data vector
                    for (var i = 0; i < newData.length; i++) {
                        data[i] = newData[i];
                    }

                    console.log(data);
                    graph.update();
                }
            }
        };

        webSocket.onerror = function(err) {
            console.log("got error ");
            console.log(err);
        };

    }

    init();

    return {
        loadData: loadData,
        render  : render,
        clear   : clear
    }
}