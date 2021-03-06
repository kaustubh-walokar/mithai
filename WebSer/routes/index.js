var express = require('express');
var cache = require('memory-cache');
var router = express.Router();
var x = "{ nodes:[ { id: 'node1', group: 0 },{ id: 'node2', group: 1 },{ id: 'node3', group: 2 },{ id: 'node5', group: 3 },{ id: 'sensor1', group: 0 },{ id: 'sensor2', group: 0 },{ id: 'sensor3', group: 0 } ],links:[ { source: 'node1', target: 'node2', value: 30 },{ source: 'node1', target: 'node3', value: 30 },{ source: 'node1', target: 'node5', value: 30 },{ source: 'node1', target: 'sensor1', value: 10 },{ source: 'sensor1', target: 'sensor2', value: 1 },{ source: 'sensor1', target: 'sensor3', value: 1 },{ source: 'node1', target: 'sensor2', value: 10 },{ source: 'sensor2', target: 'sensor1', value: 1 },{ source: 'sensor2', target: 'sensor3', value: 1 },{ source: 'node1', target: 'sensor3', value: 10 },{ source: 'sensor3', target: 'sensor1', value: 1 },{ source: 'sensor3', target: 'sensor2', value: 1 } ] }";
var y = {
    nodes: [{id: 'node1', group: 0}, {id: 'node2', group: 1}, {id: 'node3', group: 2}, {
        id: 'node5',
        group: 3
    }, {id: 'sensor1', group: 0}, {id: 'sensor2', group: 0}, {id: 'sensor3', group: 0}],
    links: [{source: 'node1', target: 'node2', value: 30}, {
        source: 'node1',
        target: 'node3',
        value: 30
    }, {source: 'node1', target: 'node5', value: 30}, {
        source: 'node1',
        target: 'sensor1',
        value: 10
    }, {source: 'sensor1', target: 'sensor2', value: 1}, {
        source: 'sensor1',
        target: 'sensor3',
        value: 1
    }, {source: 'node1', target: 'sensor2', value: 10}, {
        source: 'sensor2',
        target: 'sensor1',
        value: 1
    }, {source: 'sensor2', target: 'sensor3', value: 1}, {
        source: 'node1',
        target: 'sensor3',
        value: 10
    }, {source: 'sensor3', target: 'sensor1', value: 1}, {source: 'sensor3', target: 'sensor2', value: 1}]
};
/* GET Test page. */
router.get('/', function(req, res, next) {
    res.send("<h1>Test successful!</h1>");
});

/*Endpoint to accept data*/
router.post('/ingress', function(req, res, next) {
    var d = {
        nodes: [],
        links: []
    };

    var raw = req.body;
    if (raw !== "undefined" || raw != "" || raw != null) {
        console.log(raw);
        raw.nodes.forEach(function (e, i) {
            d.nodes.push({id: e._1, group: e._3});
        });
        raw.links.forEach(function (e, i) {
            d.links.push({source: e._1, target: e._2, value: e._3 <= 0 ? 10 : e._3});
        });
        console.log(d);
        cache.put('visualizationData', d);
    }
    res.send("Hello world!");
});

/*Endpoint to get data expeced by client*/
router.get('/data', function (req, res, next) {
    var data = cache.get('visualizationData');
    if (data == null || data == "" || data === "undefined")
        data = y;
    res.send(JSON.stringify(data));
});

module.exports = router;
