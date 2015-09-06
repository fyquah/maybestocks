const React = require("react");
const utils = require("../utils.js");

module.exports = React.createClass({
    renderChart: function(props) {
        var { data, flats, company } = props;
        if (!data) { return; }

        let chart = React.findDOMNode(this.refs.chart);
        // remove all its children
        d3.select(chart).select("svg").remove();

        var dim = {
            width: chart.parentNode.offsetWidth, height: 500,
            margin: { top: 20, right: 50, bottom: 30, left: 50 },
            ohlc: { height: 305 },
            indicator: { height: 65, padding: 5 }
        };
        dim.plot = {
            width: dim.width - dim.margin.left - dim.margin.right,
            height: dim.height - dim.margin.top - dim.margin.bottom
        };
        dim.indicator.top = dim.ohlc.height+dim.indicator.padding;
        dim.indicator.bottom = dim.indicator.top+dim.indicator.height+dim.indicator.padding;

        var indicatorTop = d3.scale.linear()
                .range([dim.indicator.top, dim.indicator.bottom]);

        var parseDate = d3.time.format("%d-%b-%y").parse;

        var zoom = d3.behavior.zoom()
                .on("zoom", draw);

        var zoomPercent = d3.behavior.zoom();

        var x = techan.scale.financetime()
                .range([0, dim.plot.width]);

        var y = d3.scale.linear()
                .range([dim.ohlc.height, 0]);

        var yPercent = y.copy();   // Same as y at this stage, will get a different domain later

        var yVolume = d3.scale.linear()
                .range([y(0), y(0.2)]);

        var candlestick = techan.plot.candlestick()
                .xScale(x)
                .yScale(y);

        var sma1 = techan.plot.sma()
                .xScale(x)
                .yScale(y);

        var ema2 = techan.plot.ema()
                .xScale(x)
                .yScale(y);

        var flatLines = flats.map(()=> techan.plot.ema().xScale(x).yScale(y));

        var volume = techan.plot.volume()
                .accessor(candlestick.accessor())   // Set the accessor to a ohlc accessor so we get highlighted bars
                .xScale(x)
                .yScale(yVolume);

        var trendline = techan.plot.trendline()
                .xScale(x)
                .yScale(y);

        var supstance = techan.plot.supstance()
                .xScale(x)
                .yScale(y);

        var xAxis = d3.svg.axis()
                .scale(x)
                .orient("bottom");

        var timeAnnotation = techan.plot.axisannotation()
                .axis(xAxis)
                .format(d3.time.format('%Y-%m-%d'))
                .width(65)
                .translate([0, dim.plot.height]);

        var yAxis = d3.svg.axis()
                .scale(y)
                .orient("right");

        var ohlcAnnotation = techan.plot.axisannotation()
                .axis(yAxis)
                .format(d3.format(',.2fs'))
                .translate([x(1), 0]);

        var closeAnnotation = techan.plot.axisannotation()
                .axis(yAxis)
                .accessor(candlestick.accessor())
                .format(d3.format(',.2fs'))
                .translate([x(1), 0]);

        var percentAxis = d3.svg.axis()
                .scale(yPercent)
                .orient("left")
                .tickFormat(d3.format('+.1%'));

        var percentAnnotation = techan.plot.axisannotation()
                .axis(percentAxis);

        var volumeAxis = d3.svg.axis()
                .scale(yVolume)
                .orient("right")
                .ticks(3)
                .tickFormat(d3.format(",.3s"));

        var volumeAnnotation = techan.plot.axisannotation()
                .axis(volumeAxis)
                .width(35);

        var macdScale = d3.scale.linear()
                .range([indicatorTop(0)+dim.indicator.height, indicatorTop(0)]);

        var rsiScale = macdScale.copy()
                .range([indicatorTop(1)+dim.indicator.height, indicatorTop(1)]);

        var macd = techan.plot.macd()
                .xScale(x)
                .yScale(macdScale);

        var macdAxis = d3.svg.axis()
                .scale(macdScale)
                .ticks(3)
                .orient("right");

        var macdAnnotation = techan.plot.axisannotation()
                .axis(macdAxis)
                .format(d3.format(',.2fs'))
                .translate([x(1), 0]);

        var macdAxisLeft = d3.svg.axis()
                .scale(macdScale)
                .ticks(3)
                .orient("left");

        var macdAnnotationLeft = techan.plot.axisannotation()
                .axis(macdAxisLeft)
                .format(d3.format(',.2fs'));

        var rsi = techan.plot.rsi()
                .xScale(x)
                .yScale(rsiScale);

        var rsiAxis = d3.svg.axis()
                .scale(rsiScale)
                .ticks(3)
                .orient("right");

        var rsiAnnotation = techan.plot.axisannotation()
                .axis(rsiAxis)
                .format(d3.format(',.2fs'))
                .translate([x(1), 0]);

        var rsiAxisLeft = d3.svg.axis()
                .scale(rsiScale)
                .ticks(3)
                .orient("left");

        var rsiAnnotationLeft = techan.plot.axisannotation()
                .axis(rsiAxisLeft)
                .format(d3.format(',.2fs'));

        var ohlcCrosshair = techan.plot.crosshair()
                .xScale(timeAnnotation.axis().scale())
                .yScale(ohlcAnnotation.axis().scale())
                .xAnnotation(timeAnnotation)
                .yAnnotation([ohlcAnnotation, percentAnnotation, volumeAnnotation])
                .verticalWireRange([0, dim.plot.height]);

        var macdCrosshair = techan.plot.crosshair()
                .xScale(timeAnnotation.axis().scale())
                .yScale(macdAnnotation.axis().scale())
                .xAnnotation(timeAnnotation)
                .yAnnotation([macdAnnotation, macdAnnotationLeft])
                .verticalWireRange([0, dim.plot.height]);

        var rsiCrosshair = techan.plot.crosshair()
                .xScale(timeAnnotation.axis().scale())
                .yScale(rsiAnnotation.axis().scale())
                .xAnnotation(timeAnnotation)
                .yAnnotation([rsiAnnotation, rsiAnnotationLeft])
                .verticalWireRange([0, dim.plot.height]);

        var svg = d3.select(chart).append("svg")
                .attr("width", dim.width)
                .attr("height", dim.height);

        var defs = svg.append("defs");

        defs.append("clipPath")
                .attr("id", "ohlcClip")
            .append("rect")
                .attr("x", 0)
                .attr("y", 0)
                .attr("width", dim.plot.width)
                .attr("height", dim.ohlc.height);

        defs.selectAll("indicatorClip").data([0, 1])
            .enter()
                .append("clipPath")
                .attr("id", function(d, i) { return "indicatorClip-" + i; })
            .append("rect")
                .attr("x", 0)
                .attr("y", function(d, i) { return indicatorTop(i); })
                .attr("width", dim.plot.width)
                .attr("height", dim.indicator.height);

        svg = svg.append("g")
                .attr("transform", "translate(" + dim.margin.left + "," + dim.margin.top + ")");

        svg.append('text')
                .attr("class", "symbol")
                .attr("x", 20)
                .text(company.company);

        svg.append("g")
                .attr("class", "x axis")
                .attr("transform", "translate(0," + dim.plot.height + ")");

        var ohlcSelection = svg.append("g")
                .attr("class", "ohlc")
                .attr("transform", "translate(0,0)");

        ohlcSelection.append("g")
                .attr("class", "axis")
                .attr("transform", "translate(" + x(1) + ",0)")
            .append("text")
                .attr("transform", "rotate(-90)")
                .attr("y", -12)
                .attr("dy", ".71em")
                .style("text-anchor", "end")
                .text("Price ($)");

        ohlcSelection.append("g")
                .attr("class", "close annotation up");

        ohlcSelection.append("g")
                .attr("class", "volume")
                .attr("clip-path", "url(#ohlcClip)");

        ohlcSelection.append("g")
                .attr("class", "candlestick")
                .attr("clip-path", "url(#ohlcClip)");

        flats.forEach(function(o, idx){
            ohlcSelection.append("g")
                .attr("class", "indicator sma flat-line line-" + idx)
                .attr("clip-path", "url(#ohlcClip)");
        });

        ohlcSelection.append("g")
                .attr("class", "indicator sma ma-0")
                .attr("clip-path", "url(#ohlcClip)");

        ohlcSelection.append("g")
                .attr("class", "indicator sma ma-1")
                .attr("clip-path", "url(#ohlcClip)");

        ohlcSelection.append("g")
                .attr("class", "indicator ema ma-2")
                .attr("clip-path", "url(#ohlcClip)");

        ohlcSelection.append("g")
                .attr("class", "percent axis");

        ohlcSelection.append("g")
                .attr("class", "volume axis");

        var indicatorSelection = svg.selectAll("svg > g.indicator").data(["macd", "rsi"]).enter()
                 .append("g")
                    .attr("class", function(d) { return d + " indicator"; });

        indicatorSelection.append("g")
                .attr("class", "axis right")
                .attr("transform", "translate(" + x(1) + ",0)");

        indicatorSelection.append("g")
                .attr("class", "axis left")
                .attr("transform", "translate(" + x(0) + ",0)");

        indicatorSelection.append("g")
                .attr("class", "indicator-plot")
                .attr("clip-path", function(d, i) { return "url(#indicatorClip-" + i + ")"; });

        // Add trendlines and other interactions last to be above zoom pane
        svg.append('g')
                .attr("class", "crosshair ohlc");

        svg.append('g')
                .attr("class", "crosshair macd");

        svg.append('g')
                .attr("class", "crosshair rsi");

        svg.append("g")
                .attr("class", "trendlines analysis")
                .attr("clip-path", "url(#ohlcClip)");
        svg.append("g")
                .attr("class", "supstances analysis")
                .attr("clip-path", "url(#ohlcClip)");

        this.reset = reset;

        var accessor = candlestick.accessor(),
            indicatorPreRoll = 33;  // Don't show where indicators don't have data

        var data = data.map(function(d) {
            return {
                date: new Date(+d.date),
                open: +d.open,
                high: +d.high,
                low: +d.low,
                close: +d.close,
                volume: +d.volume
            };
        }).sort(function(a, b) { return d3.ascending(accessor.d(a), accessor.d(b)); });

        x.domain(techan.scale.plot.time(data).domain());
        y.domain(techan.scale.plot.ohlc(data.slice(indicatorPreRoll)).domain());
        yPercent.domain(techan.scale.plot.percent(y, accessor(data[indicatorPreRoll])).domain());
        yVolume.domain(techan.scale.plot.volume(data).domain());

        var trendlineData = [
            { start: { date: this.props.from, value: 72.50 }, end: { date: this.props.to, value: 63.34 } },
            { start: { date: this.props.from, value: 43 }, end: { date: this.props.to, value: 70.50 } }
        ];

        var supstanceData = [
            { start: this.props.from, end: this.props.to, value: 63.64 },
            { start: this.props.from, end: this.props.to, value: 55.50 }
        ];

        var macdData = techan.indicator.macd()(data);
        macdScale.domain(techan.scale.plot.macd(macdData).domain());
        var rsiData = techan.indicator.rsi()(data);
        rsiScale.domain(techan.scale.plot.rsi(rsiData).domain());

        svg.select("g.candlestick").datum(data).call(candlestick);
        svg.select("g.close.annotation").datum([data[data.length-1]]).call(closeAnnotation);
        svg.select("g.volume").datum(data).call(volume);
        svg.select("g.macd .indicator-plot").datum(macdData).call(macd);
        svg.select("g.rsi .indicator-plot").datum(rsiData).call(rsi);

        flats.forEach(function(o, idx){
            ohlcSelection.append("g")
                .attr("class", "indicator sma line-" + idx)
                .attr("clip-path", "url(#ohlcClip)");
        });

        flats.forEach(function(flat, idx){
            svg.select("g.sma.line-" + idx).
                datum(flat.map(function(o){
                    return {
                        date:   o.date,
                        value:  o.close
                    };
                })).
                call(flatLines[idx]);
        });

        // svg.select("g.sma.ma-0").datum(data.map(function(o){
        //     return {
        //         "date":     o.date,
        //         "value":    o.close
        //     };
        // })).call(sma0);
        // Indicator lines (SMA and EMA)
        // svg.select("g.sma.ma-0").datum(techan.indicator.sma().period(10)(data)).call(sma0);
        // svg.select("g.sma.ma-1").datum(techan.indicator.sma().period(20)(data)).call(sma1);
        // svg.select("g.ema.ma-2").datum(techan.indicator.ema().period(50)(data)).call(ema2);


        svg.select("g.crosshair.ohlc").call(ohlcCrosshair).call(zoom);
        svg.select("g.crosshair.macd").call(macdCrosshair).call(zoom);
        svg.select("g.crosshair.rsi").call(rsiCrosshair).call(zoom);
        svg.select("g.trendlines").datum(trendlineData).call(trendline).call(trendline.drag);
        svg.select("g.supstances").datum(supstanceData).call(supstance).call(supstance.drag);


        var zoomable = x.zoomable();
        zoomable.domain([indicatorPreRoll, data.length]); // Zoom in a little to hide indicator preroll

        draw();

        // Associate the zoom with the scale after a domain has been applied
        zoom.x(zoomable).y(y);
        zoomPercent.y(yPercent);

        function reset() {
            zoom.scale(1);
            zoom.translate([0,0]);
            draw();
        }

        function draw() {
            zoomPercent.translate(zoom.translate());
            zoomPercent.scale(zoom.scale());

            svg.select("g.x.axis").call(xAxis);
            svg.select("g.ohlc .axis").call(yAxis);
            svg.select("g.volume.axis").call(volumeAxis);
            svg.select("g.percent.axis").call(percentAxis);
            svg.select("g.macd .axis.right").call(macdAxis);
            svg.select("g.rsi .axis.right").call(rsiAxis);
            svg.select("g.macd .axis.left").call(macdAxisLeft);
            svg.select("g.rsi .axis.left").call(rsiAxisLeft);

            // We know the data does not change, a simple refresh that does not perform data joins will suffice.
            svg.select("g.candlestick").call(candlestick.refresh);
            svg.select("g.close.annotation").call(closeAnnotation.refresh);
            svg.select("g.volume").call(volume.refresh);

            // Flat lines
            flats.forEach((o, idx) => {
                svg.select("g .sma.flat-line.line-" + idx).call(flatLines[idx].refresh);
            });

            // SMA and EMA lines
            // svg.select("g .sma.ma-0").call(sma0.refresh);
            // svg.select("g .sma.ma-1").call(sma1.refresh);
            // svg.select("g .ema.ma-2").call(ema2.refresh);

            svg.select("g.macd .indicator-plot").call(macd.refresh);
            svg.select("g.rsi .indicator-plot").call(rsi.refresh);
            svg.select("g.crosshair.ohlc").call(ohlcCrosshair.refresh);
            svg.select("g.crosshair.macd").call(macdCrosshair.refresh);
            svg.select("g.crosshair.rsi").call(rsiCrosshair.refresh);
            svg.select("g.trendlines").call(trendline.refresh);
            svg.select("g.supstances").call(supstance.refresh);

        }
    },
    componentDidMount: function() {
        this.renderChart(this.props);
    },
    shouldComponentUpdate: function(nextProps, nextState){
        console.log("rerender");
        this.renderChart(nextProps);
        return false;
    },
    render: function() {
        return (
            <div>
                <div id="chart" ref="chart"></div>
            </div>
        );
    }
})
