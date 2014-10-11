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
        return (regionResult ? regionResult[0] : 'us-east-1');
    }

    function init() {
        var initRegion = getCurrentRegion();
        d3.select('#' + contId).html(buildMarkup(initRegion));
        d3.select('#' + contId + ' select').on('change', function() {
            var targetRegion = this.selectedOptions[0].value;
            console.log("Switching to " + targetRegion);
            switchRegion(targetRegion);
        });
    }

    function getCurrentRegion() {
        return getRegion(getCurrentPage());
    }

    function buildMarkup(initRegion) {
        var markup = [];
        var regions = ['us-east-1', 'us-west-2', 'eu-west-1'];
        var i, selectedText = "";

        markup.push('<div>' +
            '<label for="region">Region</label>' +
            '<select name="region" id="region">');


        for (i = 0; i < regions.length; i++) {
            selectedText = regions[i] === initRegion ? 'selected="true"' : '';
            markup.push('<option value="' + regions[i] + '"' + selectedText + '>' + regions[i] + '</option>');
        }

        markup.push('</select>');
        markup.push('</div>');

        return markup.join('');
    }


    return {
        init : init,
        getCurrentPage : getCurrentPage,
        navigate : navigate
    }

}
