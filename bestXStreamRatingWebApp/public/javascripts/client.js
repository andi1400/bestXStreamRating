var i = 0;
var app = angular.module('bestXStreamRatingWebApp', ['emguo.poller']);
app.controller('myCtrl', ['$scope', '$interval', 'poller', function($scope, $interval, poller) {	
    //initialize list for flink and spark with empty
    $scope.flink = [];
    $scope.spark = [];
    $scope.argsArrayFlink = [{params:{v: 1}}];
    $scope.argsArraySpark = [{params:{v: 1}}];

	
    // create poller for regular long polling - Spark
    var sparkPoller = poller.get('../spark', {
        action: 'get',
        delay: 1000,
        smart:true,
        argumentsArray: $scope.argsArraySpark
    });
    sparkPoller.promise.then(null, null, function(result){
    	console.log("Spark: result obtained");
    	$scope.spark = result.data.data;	
    	
    	$scope.argsArraySpark[0].params.v = result.data.version;
    }); 
    
    // create poller for regular long polling - Flink
    var flinkPoller = poller.get('../flink', {
        action: 'get',
        delay: 1000,
        smart:true, 
        argumentsArray: $scope.argsArrayFlink
    });
    flinkPoller.promise.then(null, null, function(result){
    	console.log("Flink result obtained");
    	$scope.flink = result.data.data;
    	
    	$scope.argsArrayFlink[0].params.v = result.data.version;
    }); 
    
}]);
