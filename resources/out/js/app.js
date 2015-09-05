const jQuery = require("jQuery");
const $ = jQuery;
const React = require("react");
const MainComponent = require("./components/main.js");

React.render(<MainComponent from="09/2017" />, document.getElementById("app"));
