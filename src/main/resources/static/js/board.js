var INITIAL_HEX_SIZE = 130;
var MIN_SCALE = 100;
var MAX_SCALE = 300;

var boardX = $(window).width() / 2 - 75;
var boardY = $(window).height() / 2 - 100;
var boardScale = INITIAL_HEX_SIZE;

/*
 * Cursed Catan's island is bigger than the standard board and is not centred
 * on the origin, so the first time it is drawn we shrink the tiles a little
 * and shift the board so the middle of the island is where the middle of a
 * standard board would be. The player can still pan and zoom afterwards.
 * @param tilesData - the tiles of the board, as sent by the server
 */
function positionCursedBoard(tilesData) {
	MIN_SCALE = 60;
	boardScale = 100;

	var sumX = 0;
	var sumY = 0;
	var count = 0;
	for (var i = 0; i < tilesData.length; i++) {
		if (tilesData[i].type === "SEA") {
			continue;
		}
		var c = hexToCartesian(parseHexCoordinates(tilesData[i].hexCoordinate));
		sumX += c.x;
		sumY += c.y;
		count++;
	}
	if (count > 0) {
		boardX -= (sumX / count) * boardScale;
		boardY -= (sumY / count) * boardScale;
	}
}

/*
 * Constructs a new Board object.
 */
function Board() {
	this.transX = boardX;
	this.transY = boardY;
	this.scaleFactor = boardScale;

	this.tiles = [];
	this.intersections = [];
	this.paths = [];
}

/*
 * Translates the board on screen.
 * @param deltaX - the number of pixels to translate in the x direction
 * @param deltaY - the number of pixels to translate in the y direction
 */
Board.prototype.translate = function(deltaX, deltaY) {
	this.transX = this.transX + deltaX;
	this.transY = this.transY + deltaY;

	// Cap translation to bounding box
	this.transX = Math.max(-this.scaleFactor / 2, this.transX);
	this.transX = Math.min($("#board-viewport").width() * 5 / 6 - this.scaleFactor / 2, this.transX);
	this.transY = Math.max(-this.scaleFactor / (2 * Math.sqrt(3)), this.transY);
	this.transY = Math.min($("#board-viewport").height() - this.scaleFactor / (2 * Math.sqrt(3)), this.transY);

	// Update global board translation factors
	boardX = this.transX;
	boardY = this.transY;

	this.draw();
}

/*
 * Scales the board on screen.
 * @param deltaScale - the factor to scale by
 */
Board.prototype.scale = function(deltaScale) {
	this.scaleFactor = this.scaleFactor + deltaScale;
	
	// Cap scale factor between MIN_SCALE and MAX_SCALE
	this.scaleFactor = Math.max(MIN_SCALE, this.scaleFactor);
	this.scaleFactor = Math.min(MAX_SCALE, this.scaleFactor);

	// Update global board scalefactor
	boardScale = this.scaleFactor;

	// Clip edges if they go too far over border
	this.translate(0, 0);
	
	this.draw();
}

/*
 * Redraws the board.
 */
Board.prototype.draw = function() {
	for (var i = 0; i < this.tiles.length; i++) {
		this.tiles[i].draw(this.transX, this.transY, this.scaleFactor);
	}
	
	for (var i = 0; i < this.intersections.length; i++) {
		this.intersections[i].draw(this.transX, this.transY, this.scaleFactor);
	}
	
	for (var i = 0; i < this.paths.length; i++) {
		this.paths[i].draw(this.transX, this.transY, this.scaleFactor);
	}
}

/*
 * Adds a tile to the board.
 * @param tile - the tile to add to the board
 */
Board.prototype.addTile = function(tile) {
	this.tiles.push(tile);
}

/*
 * Adds an intersection to the board.
 * @param tile - the intersection to add to the board
 */
Board.prototype.addIntersection = function(intersection) {
	this.intersections.push(intersection);
}

/*
 * Adds a path to the board.
 * @param tile - the path to add to the board
 */
Board.prototype.addPath = function(path) {
	this.paths.push(path);
}

/*
 * Create a new board from the given board data.
 * @param boardData - the board data 
 */
Board.prototype.createBoard = function(boardData) {
	$("#board-viewport").empty();

	var tiles = boardData.tiles;
	var intersections = boardData.intersections;
	var paths = boardData.paths;

	for (var i = 0; i < tiles.length; i++) {
		var tile = tiles[i];
		this.addTile(parseTile(tiles[i]));
	}

	for (var i = 0; i < intersections.length; i++) {
		this.addIntersection(parseIntersection(intersections[i]));
	}

	for (var i = 0; i < paths.length; i++) {
		this.addPath(parsePath(paths[i]));
	}
}
