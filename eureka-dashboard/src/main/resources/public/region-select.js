function RegionSelector(opts) {
    opts = opts || {};
    var contId = opts.containerId || 'region-select';

    function getCurrentPage() {
        return document.location.href;
    }

    function navigate(newLoc) {
        document.location.href = newLoc;
    }

    function switchRegion(targetRegion) {
        var currentLoc = getCurrentPage();
        var curRegion = getRegion(currentLoc);
        var newLoc = currentLoc.replace(curRegion, targetRegion);
        navigate(newLoc);
    }

    function getRegion(loc) {
        var regionResult = /us-east-1|us-west-2|eu-west-1/.exec(loc);
        return (regionResult ? regionResult[0] : '');
    }

    function init() {
        d3.select('#' + contId).html(buildMarkup());
        d3.select('#' + contId + ' select').on('change', function() {
            var targetRegion = this.selectedOptions[0].value;
            console.log("Switching to " + targetRegion);
            switchRegion(targetRegion);
        });
    }

    function buildMarkup() {
        return '<div>' +
            '<label for="region">Region</label>' +
            '<select name="region" id="region">' +
                '<option value="us-east-1">us-east-1</option>' +
                '<option value="us-west-2">us-west-2</option>' +
                '<option value="eu-west-1">eu-west-1</option>' +
            '</select>' +
        '</div>';
    }


    return {
        init : init,
        getCurrentPage : getCurrentPage,
        navigate : navigate
    }

}
