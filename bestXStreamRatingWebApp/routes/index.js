var express = require('express');
var router = express.Router();
var redis = require("redis");
var SortedList = require("sortedlist");

/////////////////
/// REDIS
/////////////////
var redisClientSpark = redis.createClient(process.argv[2], "localhost");

//hold state that is continously updated from streamed analysis
var sparkState = {_version: 0};
var flinkState = {_version: 0};

var onStatus = {
	spark: false,
	flink: false
};

redisClientSpark.on("error", function (err) {
    console.log("REDIS Error " + err);
});


/* GET home page. */
router.get('/', function(req, res, next) {
    res.render('index', { title: 'Express' });
});

/* GET spark info */
router.get('/spark', function(req, res, next) {
	checkIfOn("spark");
	
	var lastVersion = req.query['v'];
	
	//handle the request, make it wait if necessary
	checkAndServeNewVersion(sparkState, lastVersion, res, 0);
});

/* GET flink info */
router.get('/flink', function(req, res, next) {
	checkIfOn("flink");
	
	var lastVersion = req.query['v'];
	
	//handle the request, make it wait if necessary
	checkAndServeNewVersion(flinkState, lastVersion, res, 0);	
});

/**
 * Asynchroneously serve a new version of the state
 * 
 */
var checkAndServeNewVersion = function(state, lastVersion, res, recursionDepth){
	if(lastVersion == null || lastVersion < state["_version"]){
		//console.log("Serving new version.. Last Version " + lastVersion);
		//just construct the reply and send it
		
		//update age before serving
		for(j in state){
			if(j != "_version"){
				state[j]["age"]  = (new Date().getTime() - state[j]["lastUpdate"]) / 1000;	
			}
		}
		
		res.send(constructReplyFromInternalState(state));
		res.end();
	}else{
		//console.log("No new version, waiting... Last Version  " +  lastVersion);
		
		if(recursionDepth < 10){
			setTimeout(function(){
				checkAndServeNewVersion(state, lastVersion, res, recursionDepth + 1);
			}, 500);	
		}else{
			//ok not modified - send result because of updated age
			
			//update age before serving
			for(j in state){
				if(j != "version"){
					state[j]["age"]  = (new Date().getTime() - state[j]["lastUpdate"]) / 1000;	
				}
			}
			res.send(constructReplyFromInternalState(state));
			res.end();
		}
	}
};


/**
 * Asynchroneous polling method for polling the redis DB
 */
var pollRedisForNewState = function(/*redisClient*/ currentRedisClient, /*String*/ queueName){
	//console.log("Polling redis for new state for " + queueName);
	//get all that has happend in redis
	currentRedisClient.lrange(queueName, 0, -1, function(err, reply) {
		//console.log("got reply");
		//console.log("Reply was empty?: " + reply.length == 0);
		
		if(err){			
			console.log("Redis error!");
			console.err("Redis Error!");
			console.log(err);
			res.end();
		}else{
			//ok the reply is empty
			if(reply == null || reply === undefined || reply.length == 0){
				//try again in 500ms
				setTimeout(function(){pollRedisForNewState(currentRedisClient, queueName);}, 500);
			}else if(reply.length > 0){
				//parse all the replies in blocking mode
		    	for(var t = 0; t < reply.length; t++){
		    		var parsedReply = JSON.parse(reply[t]);
		    		if(queueName == "flink"){
		    			parseRedisReply(parsedReply, flinkState);	
		    		}else if(queueName == "spark"){
		    			parseRedisReply(parsedReply, sparkState);
		    		}else{
		    			console.log("Queue unknown!");
		    		}
		    	}
		    	
		    	//OK finished, now try again in 500ms
				setTimeout(function(){pollRedisForNewState(currentRedisClient, queueName);}, 500);
	    	}else{
	    		console.log("redis reply is unexpected " + JSON.stringify(reply));
	    		
	    		//failed, still try again, but longer timeout
				setTimeout(function(){pollRedisForNewState(currentRedisClient, queueName);}, 5000);
	    	}
		}
	});    
};

var parseRedisReply = function(/*Jparsed Object*/ parsedReply, /*object*/ internalState){
	//give the reply a new version timestamp 
	internalState["_version"] = new Date().getTime();
	
	//parse the redis reply
	for(k in parsedReply){
		var e = parsedReply[k];
		//update the previous change state
		if(internalState[e["name"]] != undefined){
			 internalState[e["name"]]["change"] = e["sentiment"] - internalState[e["name"]]["sentiment"];
		}else{
			//create new entry
			internalState[e["name"]] = {
	    		"name": e["name"],
	    		"sentiment": 0,
	    		"change": 0,
	    		"retweets": "unknown",
	    		"occurence": 0,
	    		"stype": "info"
	    	};
		}
		
		//update current data
		internalState[e["name"]]["sentiment"]  = e["sentiment"];
		internalState[e["name"]]["stype"]  = getLabelBySentiment(e["sentiment"]);
		internalState[e["name"]]["occurence"]  = e["occurence"];
		internalState[e["name"]]["image"]  = e["image"];
		internalState[e["name"]]["displayName"]  = e["displayName"];
		internalState[e["name"]]["lastUpdate"]  = new Date();
		internalState[e["name"]]["age"]  = 0;
		
		//console.log("updated state to ",JSON.stringify(internalState[e["name"]]));	
	}
	return internalState;
};

var constructReplyFromInternalState = function(internalState){
	//sort it
	var sortedList = SortedList.create({"compare": function(a,b){
		return -(a["sentiment"] - b["sentiment"]);
	}});
	for(k in internalState){
		if(k == "_version"){
			continue;
		}
		sortedList.insertOne(internalState[k]);
	}
	
	//console.log("have sorted list now " + JSON.stringify(sortedList.toArray()));
	
	//return the sorted list together with the version
	var returnObj = {
		version: internalState["_version"],
		data: sortedList.toArray()
	};
	
	return returnObj;
};


var getLabelBySentiment = function(/*int*/ sentiment){
	if(sentiment <= 0.2){
		return "danger";
	}else if(sentiment <= 0.4){
		return "warning";
	}else if (sentiment < 0.6){
		return "info";
	}else if(sentiment >= 0.6){
		return "success";
	}
	
};

//check if redis polling is activated or not
var checkIfOn = function(/*String*/ queueName){
	if(!onStatus[queueName]){
		onStatus[queueName] = true; 
		
		setTimeout(function(){pollRedisForNewState(redisClientSpark, queueName);}, 0);
	}
};



module.exports = router;
