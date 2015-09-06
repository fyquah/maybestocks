const React = require("react");
const utils = require("../utils.js");
const ChartComponent = require("./chart.js");
const CompanyInfoComponent = require("./company.js");
const SimulationComponent = require("./simulation.js");

const parseDateString = (dateString) =>  {
    if (typeof dateString === "object") {
        return dateString; // assume it is a date object
    } else {
        let [year, month, day] = dateString.split("/").map(Number);
        return new Date(year, month, day);
    }
}
const formatDate = (date) => {
    const pad = function(n) {
        if (n < 10) {
            return "0" + n.toString()
        } else {
            return n.toString();
        }
    }
    console.log(date);
    var x = `${date.getFullYear()}-${pad(date.getMonth())}-${pad(date.getDay())}`;
    return x;
}

module.exports = React.createClass({
    fetchAndUpdateData: function(from, to, symbol) {
        // from and to are Date objects
        // symbol is the ticker symbol of the relevant company
        // update price
        utils.httpGet(encodeURI("/prices?" + utils.toQueryString({
            fromMonth: from.getMonth(),
            fromYear: from.getFullYear(),
            toMonth: to.getMonth(),
            toYear: to.getFullYear(),
            symbol: symbol
        }))).
        then((res) => {
            this.setState({
                data:   res.data.concat([]),
                flats:  res.flat.map((seq)=>{
                    return seq.map((entry)=> {
                        let [date, close] = entry;
                        return { close, date };
                    })
                })
            })
        }).
        catch((a,b,c) => {
            console.log("An error occured");
            console.log(a, b, c);
        });

        // update symbol
        utils.httpGet("/company?symbol=" + symbol).
        then((res) => {
            this.setState({
                company: res
            });
        }).
        catch((e) => {
            console.log(e);
        });
    },
    componentDidMount: function() {
        const from = parseDateString(this.props.from || utils.date());
        const to = parseDateString(this.props.to || utils.date());
        const symbol = this.props.symbol || "AAPL";

        this.fetchAndUpdateData(from, to, symbol)

        React.findDOMNode(this.refs.from).value = formatDate(from);
        React.findDOMNode(this.refs.to).value = formatDate(to);
        React.findDOMNode(this.refs.symbol).value = this.props.symbol;

        this.setState({
            from: from,
            to: to,
            symbol: symbol
        })

        utils.httpGet("/companies").
        then((res) => {
            this.setState({
                companies_list: res
            })
        }).catch((e) => {
            console.error(e);
        })
    },
    getInitialState: function() {
        return {};
    },
    handleClickReset: function(e) {
        e.preventDefault();
        this.refs.chartComponent.reset();
    },
    handleSelectDate: function(e) {
        e.preventDefault();
        const from = React.findDOMNode(this.refs.from);
        const to = React.findDOMNode(this.refs.to);
        const symbol = React.findDOMNode(this.refs.symbol);

        if (from.value && to.value && symbol.value) {
            this.fetchAndUpdateData(new Date(from.value), new Date(to.value), symbol.value);
            this.setState({
                from: new Date(from.value),
                to: new Date(to.value),
                symbol: symbol.value.toUpperCase()
            })
        }
    },
    render: function(){
        return (
            <div className="container">
                <h1>Maybe Stocks</h1>
                <div className="col-md-9">
                    {() => {
                        if(this.state.data &&
                            this.state.flats &&
                            this.state.company) {
                            return (
                                <ChartComponent
                                    ref="chartComponent"
                                    from={this.state.from}
                                    to={this.state.to}
                                    data={this.state.data}
                                    flats={this.state.flats}
                                    company={this.state.company} />
                            )
                        }
                    }()}

                </div>
                <div className="col-md-3">
                    <p>
                        {(() => {
                            console.log(this.state);
                            if (this.state.companies_list) {
                                return <div className="form-group">
                                    <select className="form-control">
                                        {this.state.companies_list.map(function(o){
                                            return <option>{o.company} ({o.symbol})</option>
                                        })}
                                    </select>
                                </div>

                            } else {
                                return <div></div>
                            }
                        })()}
                        <div onBlur={this.handleSelectDate} className="form-group">
                            <label>From</label>
                            <input className="form-control" type="date" name="from" ref="from" />
                        </div>
                        <div onBlur={this.handleSelectDate} className="form-group">
                            <label>To</label>
                            <input className="form-control" type="date" name="to" ref="to" />
                        </div>
                        <div onBlur={this.handleSelectDate} className="form-group">
                            <label>Company</label>
                            <input className="form-control" type="text" name="symbol" ref="symbol" placeholder="Company Symbol" />
                        </div>
                        <button onClick={this.handleSelectDate} className="btn btn-primary" ref="button">
                            Update Data
                        </button>{ " " }
                        <button onClick={this.handleClickReset} className="btn btn-warning" ref="button">
                            Zoom Out
                        </button>
                    </p>
                    <br />
                    <CompanyInfoComponent company={this.state.company} />
                </div>
                <div>
                    <SimulationComponent symbol={this.state.symbol}
                        from={this.state.from}
                        to={this.state.to}/>
                </div>
            </div>
        );
    }
});
