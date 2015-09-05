const React = require("react");
const utils = require("../utils.js");

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

        utils.httpGet(encodeURI("/prices?" + utils.toQueryString({
            fromMonth: from.getMonth(),
            fromYear: from.getYear(),
            toMonth: to.getMonth(),
            toYear: to.getYear(),
            symbol: symbol
        }))).
            then((res) => {
                console.log(res);
            }).
            catch((a,b,c) => {
                console.log("An error occured");
            });
    },
    render: function(){
        return (<div className="container">Hello world</div>);
    }
});
