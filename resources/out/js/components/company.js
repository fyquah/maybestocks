var React = require("react");

// Company info!
module.exports = React.createClass({
    render: function(){
        let company = this.props.company;
        console.log(company);
        if (company) {
            return (
                <div>
                    <table className="table">
                        <tr>
                            <th>Column</th>
                            <th>Detail</th>
                        </tr>
                        <tr>
                            <td>Symbol</td>
                            <td><b>{company.symbol}</b></td>
                        </tr>
                        <tr>
                            <td>Name</td>
                            <td><b>{company.company}</b></td>
                        </tr>
                        <tr>
                            <td>Market Cap</td>
                            <td><b>{company.marketcap}</b></td>
                        </tr>
                        <tr>
                            <td>IPO Year</td>
                            <td><b>{company.ipoyear || "N/A"}</b></td>
                        </tr>
                        <tr>
                            <td>Industry</td>
                            <td><b>{company.sector}</b></td>
                        </tr>
                        <tr>
                            <td>Quote Link</td>
                            <td><a target="_blank"
                                    href={company.quotelink}>
                                    <b>View</b>
                            </a></td>
                        </tr>
                    </table>
                </div>
            );
        } else {
            return <div>No company selected yet.</div>
        }

    }
})
