const React = require("react");
const utils = require("../utils.js");
const ChartComponent = require("./chart.js");

const parseDateString = (dateString) =>  {
    if (typeof dateString === "object") {
        return dateString; // assume it is a date object
    } else {
        let [month, year] = dateString.split("/").map(Number);
        return new Date(year, month);
    }
}

module.exports = React.createClass({
    componentWillMount: function() {

        const from = parseDateString(this.props.from || utils.date());
        const to = parseDateString(this.props.to || utils.date());
        const symbol = this.props.symbol || "AAPL";

        console.log(from);
        console.log(from.getMonth());
        console.log(from.getFullYear());
        console.log(to.getMonth());
        console.log(to.getFullYear());

        utils.httpGet(encodeURI("/prices?" + utils.toQueryString({
            fromMonth: from.getMonth(),
            fromYear: from.getFullYear(),
            toMonth: to.getMonth(),
            toYear: to.getFullYear(),
            symbol: symbol
        }))).
        then((res) => {
            console.log(res);
            this.setState({
                data:           res.data,
                flat_segments:  res.flat
            })
        }).
        catch((a,b,c) => {
            console.log("An error occured");
            console.log(a, b, c);
        });
    },
    getInitialState: function() {
        return {};
    },
    render: function(){
        console.log("Rerender");
        console.log(this.state);
        return (
            <div className="container">
                <ChartComponent data={this.state.data} />
            </div>
        );
    }
});
