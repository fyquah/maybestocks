const jQuery = require("jQuery");
const $ = jQuery;
const React = require("react");
const MainComponent = require("./components/main.js");

React.render(<MainComponent from="2012/09/01" to="2015/09/03" symbol="GOOGL" />, document.getElementById("app"));
