const React = require("react");
const utils = require("../utils.js");

const formatDate = function(date) {
    return `${date.getFullYear()}-${date.getMonth()}-${date.getDay()}`
}

const isDecisionCorrect = function(o) {
    return (o.action === "BUY" && o.open_p <= o.close_p) ||
        (o.action === "SELL" && o.open_p >= o.close_p)
}

const calculateProfit = function(open, close, action) {
    if (action.toUpperCase() === "BUY") {
        return close - open;
    } else {
        return open - close;
    }
}

const daysDifference = function(from, to) {
    let seconds = (from.getTime() - to.getTime()) / 1000;
    return Math.floor(Math.abs(seconds / 86400));
}

module.exports = React.createClass({
    fetchAndUpdate: function(symbol) {
        var symbol = symbol || this.props.symbol;
        if(typeof symbol === "undefined") {
            return;
        }

        utils.httpGet("/simulation?symbol=" + symbol).
        then((res) => {
            console.log("Got results")
            this.setState({
                simulations: res
            })
        }).catch((e) => {
            console.error(e);
        })
    },
    getInitialState: function(){ return {} },
    componentDidMount: function() {
        this.fetchAndUpdate();
    },
    componentWillReceiveProps: function(nextProps) {
        if (nextProps.symbol !== this.props.symbol) {
            this.fetchAndUpdate(nextProps.symbol)
        }
    },
    render: function(){
        if (this.state.simulations) {
            let profit = this.state.simulations.reduce((m, o) => {
                return m + calculateProfit(+o.open_p, +o.close_p, o.action);
            }, 0).toFixed(2);
            let accuracy = this.state.simulations.reduce((m, o) => {
                    return m + (isDecisionCorrect(o) ? 1 : 0);
            }, 0) / this.state.simulations.length
            let number_of_days = daysDifference(this.props.from, this.props.to);

            return (
                <div>
                    <div className="row">
                        <div className="col-xs-6">
                            <h3>Algorithm Summary</h3>
                            <br />
                            <table className="table">
                                <tbody>
                                    <tr>
                                        <td>Total profit</td>
                                        <td><b>{profit}</b></td>
                                    </tr>
                                    <tr>
                                        <td>Accuracy</td>
                                        <td><b>{ (accuracy * 100).toFixed(2)}%</b></td>
                                    </tr>
                                    <tr>
                                        <td>Number of transactions</td>
                                        <td><b>{this.state.simulations.length}</b></td>
                                    </tr>
                                    <tr>
                                        <td>Number of Days</td>
                                        <td><b>{ number_of_days }</b> day(s)</td>
                                    </tr>
                                    <tr>
                                        <td>Number of trades per day</td>
                                        <td><b>{ (this.state.simulations.length / number_of_days).toFixed(2) }</b></td>
                                    </tr>
                                </tbody>
                            </table>

                        </div>
                        <div className="col-xs-6">
                        </div>
                    </div>

                    <h3>Simulated Transactions</h3>
                    <table className="table">
                        <thead>
                            <thead>
                                <tr>
                                    <td>Open Price</td>
                                    <td>Close Price</td>
                                    <td>Action</td>
                                    <td>Date</td>
                                    <td>Profit</td>
                                </tr>
                            </thead>
                        </thead>
                        <tbody>
                            {this.state.simulations.map((o)=> {
                                return (
                                    <tr>
                                        <td>{o.open_p}</td>
                                        <td>{o.close_p}</td>
                                        <td>{o.action}</td>
                                        <td>{formatDate(new Date(o.date_ex))}</td>
                                        <td>{calculateProfit(+o.open_p, +o.close_p, o.action).toFixed(2)}</td>
                                    </tr>
                                )
                            })}
                        </tbody>
                    </table>
                </div>
            );
        } else {
            return <div>Results not available. It might be loading</div>
        }
    }
})
