const React = require("react");
const utils = require("../utils.js");

module.exports = React.createClass({
    renderChart: function(input_data) {
        if(!input_data) { return; }

        let chart = React.findDOMNode(this.refs.chart);

        // do the data processing actions here ...
        var margin = {top: 20, right: 20, bottom: 30, left: 50},
            width = 960 - margin.left - margin.right,
            height = 500 - margin.top - margin.bottom;

        var parseDate = d3.time.format("%d-%b-%y").parse;

        var x = techan.scale.financetime()
                .range([0, width]);

        var y = d3.scale.linear()
                .range([height, 0]);

        var candlestick = techan.plot.candlestick()
                .xScale(x)
                .yScale(y);

        var xAxis = d3.svg.axis()
                .scale(x)
                .orient("bottom");

        var yAxis = d3.svg.axis()
                .scale(y)
                .orient("left");

        var svg = d3.select(chart).append("svg")
                .attr("width", width + margin.left + margin.right)
                .attr("height", height + margin.top + margin.bottom)
                .append("g")
                .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

        var accessor = candlestick.accessor(),
                timestart = Date.now();

        const data = input_data.map(function(d) {
            return {
                date: new Date(+d.date),
                open: +d.open,
                high: +d.high,
                low: +d.low,
                close: +d.close,
                volume: +d.volume
            };
        }).sort(function(a, b) { return d3.ascending(accessor.d(a), accessor.d(b)); });

        x.domain(data.map(accessor.d));
        y.domain(techan.scale.plot.ohlc(data, accessor).domain());

        svg.append("g")
                .datum(data)
                .attr("class", "candlestick")
                .call(candlestick);

        svg.append("g")
                .attr("class", "x axis")
                .attr("transform", "translate(0," + height + ")")
                .call(xAxis);

        svg.append("g")
                .attr("class", "y axis")
                .call(yAxis)
                .append("text")
                .attr("transform", "rotate(-90)")
                .attr("y", 6)
                .attr("dy", ".71em")
                .style("text-anchor", "end")
                .text("Price ($)");

        console.log("Render time: " + (Date.now()-timestart));
    },
    componentWillReceiveProps: function(nextProps) {

    },
    componentDidMount: function() {
        this.renderChart(this.props.data);
    },
    shouldComponentUpdate: function(nextProps, nextState){
        this.renderChart(nextProps.data);
        return false;
    },
    render: function() {
        return (
            <div>
                <div id="chart" ref="chart"></div>
                <input ref="input" />
            </div>
        );
    }
})
