function ClusterStatusChart(options) {
    options = options || {};
    var data = [],
        containerId = options.containerId || 'cluster',
        width = options.width || 560,
        height = 500,
        radius = Math.min(width, height) / 2;


    var color = d3.scale.ordinal()
            .domain(["G", "Y", "R"])
            .range(["#c7e9c0", "#ec7014", "#fff7bc"]);

    var arc = d3.svg.arc()
            .outerRadius(radius - 10)
            .innerRadius(radius - 70);

    var pie = d3.layout.pie()
            .sort(null)
            .value(function (d) {
                return d.value;
            });

    var svg = d3.select('#' + containerId).append("svg")
            .attr("width", width)
            .attr("height", height)
            .append("g")
            .attr("transform", "translate(" + width / 2 + "," + height / 2 + ")");


    function loadData(serversInfo) {
        data = serversInfo;
        injectStubValue(data);
        return this;
    }

    function render() {
        var g = svg.selectAll(".arc")
                .data(pie(data), function(d) {return d.data.eip + '_' + d.data.status;})
                .enter().append("g")
                .attr("class", "arc")
                        .on("mouseover", function(d) { d3.select(this).attr('cursor', 'pointer')})
                        .on('click', function(d) { window.open('http://' + d.data.eip + ':7001/discovery')});

        g.append("title")
                .text(function (d) {
                    return 'EIP : ' + d.data.eip;
                });

        g.append("path")
                .attr("d", arc)
                .style("fill", function (d) {
                    return color(d.data.status);
                });

        g.append("text")
                .attr("transform", function (d) {
                    return "translate(" + arc.centroid(d) + ")";
                })
                .attr("dy", ".35em")
                .style("text-anchor", "middle")
                .text(function (d) {
                    return d.data.zone;
                });
    }


    function injectStubValue(clusterInfo) {
        for (var i = 0; i < clusterInfo.length; i++) {
            clusterInfo[i].value = 100;
        }
    }




    return {
        render: render,
        loadData : loadData
    }

}
