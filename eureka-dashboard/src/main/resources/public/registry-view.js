function RegistryView(options) {
    options = options || {};
    var containerId = options.containerId || 'registry';
    var diameter = options.diameter || 660;
    var registry = [];
    var root = { children: [] };
    var utils = Utils();

    var format = d3.format(",d"),
            color = d3.scale.category20c();

    var bubble = d3.layout.pack()
            .sort(null)
            .size([diameter, diameter])
            .padding(1.5);

    var svg = d3.select('#' + containerId).append("svg")
            .attr("width", diameter)
            .attr("height", diameter)
            .attr("class", "bubble");

    function clear() {
        d3.select('#' + containerId).clear();
    }

    function loadData(callback) {
        var ws = new WebSocket('ws://' + utils.getCurrentPageDomain() +'/sub');

        ws.onopen = function () {
            ws.send(JSON.stringify({cmd: 'getStream', ds: 'discovery', refreshMin: 5}));
        };

        ws.onmessage = function (evt) {
            console.log("Got discovery data stream");
            var data = JSON.parse(evt.data);
            registry = data.values.registry;
            if (callback) callback(data);
        };

        return this;
    }

    function buildFlattenedTree() {
        var regItem, i;
        for (i = 0; i < registry.length; i++) {
            regItem = registry[i];
            root.children.push({name: regItem[0], value: regItem[1]});
        }
    }

    function renderBubbleChart() {
        var nodes = svg.selectAll(".node")
                .data(bubble.nodes(root)
                        .filter(function (d) {
                            return !d.children;
                        }), function (d) { return d.name; });

        var node = nodes.enter().append("g")
                .attr("class", "node")
                .attr("transform", function (d) {
                    return "translate(" + d.x + "," + d.y + ")";
                });

        node.append("title")
                .text(function (d) {
                    return d.name + ": " + format(d.value);
                });

        node.append("circle")
                .attr("r", function (d) {
                    return d.r;
                })
                .style("fill", function (d) {
                    return color(d.name);
                }).on('mouseover', function (d) {
                    d3.select(this).style('cursor', 'pointer');
                });

        node.append("text")
                .attr("dy", ".3em")
                .style("text-anchor", "middle")
                .text(function (d) {
                    if (d.name) {
                        return d.name.substring(0, d.r / 3);
                    }
                    return "";
                }).on('mouseover', function (d) {
                    d3.select(this).style('cursor', 'pointer');
                });

        // remove nodes on exit
        nodes.exit().remove();
    }

    d3.select(self.frameElement).style("height", diameter + "px");

    return {
        render     : function () {
            buildFlattenedTree();
            renderBubbleChart();
        },
        loadData   : loadData,
        getRegistry: function () {
            return registry;
        }
    }
}
