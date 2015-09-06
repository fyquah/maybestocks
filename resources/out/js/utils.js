const jQuery = require("jQuery");
const httpGet = function() {
    const promises = {};

    return function(url, fresh){
        if (promises[url] && !fresh) {
            return promises[url];
        } else {
            return promises[url] = new Promise((resolve, reject) => {
                jQuery.get(url).success(resolve).error(reject);
            });
        }
    }
}();
const date = () => Object.freeze(new Date());
const toQueryString = (obj) => {
    // assume object is not deep
    let arr = []

    for (let key of Object.keys(obj)) {
        arr.push(key + "=" + obj[key]);
    }

    return arr.join("&");
}
const padZero = function(n) {
    if (n < 10) {
        return "0" + n.toString()
    } else {
        return n.toString()
    }
}
const formatDate = function(date) {
    return `${date.getFullYear()}-${padZero(1 + date.getMonth())}-${padZero(date.getDay())}`
}

module.exports = { httpGet, date, toQueryString, formatDate };
