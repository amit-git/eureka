function RegistryView(options) {
    options = options || {};
    var containerId = options.containerId || 'registry';
    var diameter = options.diameter || 660;
    var registry = [];
    var appIdToRegistryMap = {};
    var appsList = [];
    var root = { children: [] };
    var totalInstCount = 0;

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

    var instancesTblView, // ref to SimpleTreeView for instance list
            selectAppAutoCompleteBox;

    function clear() {
        d3.select('#' + containerId).clear();
    }

    function loadData(callback) {
        var ws = new WebSocket('ws://' + Utils.getCurrentPageDomain() + '/sub');

        ws.onopen = function () {
            ws.send(JSON.stringify({cmd: 'getStream', ds: 'discovery', refreshMin: 5}));
        };

        ws.onmessage = function (evt) {
            console.log("Got discovery data stream");
            var data = JSON.parse(evt.data);
            registry = data.values.registry;
            appIdToRegistryMap = {};
            appsList = [];
            if (callback) callback(data);
        };

        return this;
    }

    function buildFlattenedTree() {
        var regItem, i;
        totalInstCount = 0;
        for (i = 0; i < registry.length; i++) {
            regItem = registry[i];
            root.children.push({name: regItem['app'], value: regItem['count']});
            totalInstCount += regItem['count'];
            var appName = regItem['app'].toLowerCase();
            appIdToRegistryMap[appName] = regItem;
            appsList.push(appName);
        }
    }

    function sortRegistryData() {
        registry.sort(function (a, b) {
            return b['count'] - a['count'];
        });
    }

    function showTotalCount() {
        var regCntElm = d3.select('.reg-count');
        regCntElm.select('.val').text(totalInstCount);
        regCntElm.style('display', 'block');
    }

    function showInstanceList(app, instances) {
        console.log("Instances ");
        console.log(instances);
        instancesTblView = SimpleTreeView({contId: 'inst-list', data: buildInstanceNACLinks(instances)});
        instancesTblView.init();

        $('.inst-list-cont .app').html('&ldquo;' + app + '&rdquo;');
        $('.inst-list-cont').show();
    }

    function buildInstanceNACLinks(instances) {
        var instListOut = [];
        Object.keys(instances).forEach(function (stat) {
            var instList = instances[stat];
            var instListWithLinks = [];
            instList.forEach(function (inst) {
                instListWithLinks.push(Utils.buildNACLinkForInstance(inst));
            });

            instListOut[stat] = instListWithLinks;
        });
        return instListOut;
    }

    function searchApp(app) {
        var registryForApp = appIdToRegistryMap[app] || {};
        showInstanceList(app, registryForApp.instances || []);
    }

    function renderBubbleChart() {
        var nodes = svg.selectAll(".node")
                .data(bubble.nodes(root)
                        .filter(function (d) {
                            return !d.children;
                        }), function (d) {
                    return d.name;
                });

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
                }).on('click', function (d) {
                    handleOnClickNode(d);
                });

        var labels = node.append("text")
                .attr("dy", ".3em")
                .style("text-anchor", "middle")
                .text(function (d) {
                    if (d.name) {
                        return d.name.substring(0, d.r / 3);
                    }
                    return "";
                }).on('mouseover', function (d) {
                    d3.select(this).style('cursor', 'pointer');
                }).on('click', function (d) {
                    handleOnClickNode(d);
                });

        labels.append('tspan')
                .attr('y', function (d) {
                    return (d.r / 2.0);
                })
                .attr('x', '0')
                .style("text-anchor", "middle")
                .text(function (d) {
                    if (d.r > 20) {
                        return format(d.value);
                    }
                    return '';
                });

        // remove nodes on exit
        nodes.exit().remove();
    }

    d3.select(self.frameElement).style("height", diameter + "px");

    function handleOnClickNode(d) {
        showInstanceList(d.name.toLowerCase(), appIdToRegistryMap[d.name.toLowerCase()].instances || []);
        $('#search-app').val(d.name.toLowerCase()); // update search box text to keep it in sync
    }

    function wireSearchBox() {
        selectAppAutoCompleteBox = $('#search-app').autocomplete({
            source: appsList,
            select: function (event, ui) {
                searchApp(ui.item.value);
            }
        });
    }

    return {
        render     : function () {
            sortRegistryData();
            buildFlattenedTree();
            renderBubbleChart();
            showTotalCount();
            wireSearchBox();
        },
        loadData   : loadData,
        getRegistry: function () {
            return registry;
        }
    }
}
