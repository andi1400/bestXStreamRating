var express = require('express');
var path = require('path');
var bodyParser = require('body-parser');
var http = require('http');
var routes = require('./routes/index');
var auth = require('http-auth');

var app = express();

// view engine setup
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'hjs');

//authentication
var basic = auth.basic({
    realm: "bestXStreamRating",
    file: __dirname + "/users.htpasswd"
});
app.use(auth.connect(basic));

app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));

//all the routes and static stuff
app.use('/', routes);
app.use(express.static(path.join(__dirname, 'public')));


// catch 404 and forward to error handler
app.use(function(req, res, next) {
  var err = new Error('Not Found');
  err.status = 404;
  next(err);
});


// production error handler
// no stacktraces leaked to user
app.use(function(err, req, res, next) {
  res.status(err.status || 500);
  res.render('error', {
    message: err.message,
    error: {}
  });
});


http.createServer(app).listen(3030, function() {
	console.log('Express server listening on port ' + 3030);
});


module.exports = app;