function MetricsChart(options) {
    options = options || {};
    var containerId = options.containerId || 'chart';
    var yAxisId = options.yAxisId || (containerId + '-y_axis');
    var width = options.width || 540;
    var height = options.height || 240;
    var palette = new Rickshaw.Color.Palette();

    var data = [
         /*
         { x: -1893456000, y: 25868573 },
         { x: -1577923200, y: 29662053 },
         { x: -1262304000, y: 34427091 },
         { x: -946771200, y: 35976777 },
         { x: -631152000, y: 39477986 },
         { x: -315619200, y: 44677819 },
         { x: 0, y: 49040703 },
         { x: 315532800, y: 49135283 },
         { x: 631152000, y: 50809229 },
         { x: 946684800, y: 53594378 },
         { x: 1262304000, y: 55317240 }
         */
    ];

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

        webSocket.onopen = function () {
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
    }

    init();

    return {
        loadData: loadData,
        render  : render,
        clear   : clear
    }
}