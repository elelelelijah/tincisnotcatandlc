var SETTLEMENT_SCALE = 0.2;
var CITY_SCALE = 0.25;
var SELECTABLE_AREA_SCALE = 0.25;
var SETTLEMENT_SVG_WIDTH = 16;
var CITY_SVG_WIDTH = 100;

var SETTLEMENT_SVG = "<svg class='settlement-icon'><path "
	+ "d='M16 9.5l-3-3v-4.5h-2v2.5l-3-3-8 8v0.5h2v5h5v-3h2v3h5v-5h2z'></path></svg>";
var CITY_SVG = '<svg class="city-icon">'
	+ '<path d="M95.359,55.907H100l-5.063-8.122H54.429V34.388h7'
	+ '.173L30.801,12.026l-10.548,7.658v-4.638H13.22v9.744L0,34.388h7.173 v47.996H2.'
	+ '532v5.591h45.147h3.797h47.258v-5.591h-3.375V55.907z"/></svg>';

var BUILDING = {
	NONE: 1,
	SETTLEMENT: 2,
	CITY: 3,
	KNIGHT: 4
}

/*
 * Construct a new intersection.
 * @param coord1 - the coordinate of the first adjacent tile
 * @param coord2 - the coordinate of the second adjacent tile
 * @param coord3 - the coordinate of the third adjacent tile
 */
function Intersection(coord1, coord2, coord3) {
	this.intersectCoordinates = { coord1: coord1, coord2: coord2, coord3: coord3 };
	this.coordinates = findCenter(coord1, coord2, coord3);
	this.id = ("intersection-x-" + this.coordinates.x + "y-"
		+ this.coordinates.y + "z-" + this.coordinates.z).replace(/[.]/g, "_");

	this.building = BUILDING.NONE;
	this.player;
	this.knightLevel = 1;
	this.knightActive = false;

	this.port = PORT.NONE;

	this.highlighted = false;
	this.canBuildSettlement;

	$("#board-viewport").append("<div class='intersection-select circle' id='" + this.id + "-select'></div>");
	$("#board-viewport").append("<div class='intersection' id='" + this.id + "'></div>");
}

/*
 * Redraws this intersection.
 * @param transX - the x translation of the board
 * @param transY - the y translation of the board
 * @param scale - the scale to draw at
 */
Intersection.prototype.draw = function (transX, transY, scale) {
	var displacement = hexToCartesian(this.coordinates);

	var element = $("#" + this.id);
	element.empty();

	switch (this.building) {
		case BUILDING.SETTLEMENT:
			var size = scale * SETTLEMENT_SCALE;
			var x = transX + displacement.x * scale + Math.sqrt(3) * scale / 4 - size / 4;
			var y = transY + displacement.y * scale + scale / 4 - size / 2;

			// Move intersection to correct location and set size
			element.append(SETTLEMENT_SVG);
			element.css("transform", "translate(" + x + "px, " + y + "px)");
			element.attr("height", size);
			element.attr("width", size);

			// Scale svg element
			var svg = element[0].getElementsByTagName("svg")[0];
			svg.setAttribute("viewBox", "0 0 " + SETTLEMENT_SVG_WIDTH + " " + SETTLEMENT_SVG_WIDTH);
			svg.setAttribute("height", size);
			svg.setAttribute("width", size);

			// Set fill of svg element
			var svg2 = $("#" + this.id).children().first();
			svg2.css("fill", this.player.color);
			break;
		case BUILDING.CITY:
			var size = scale * CITY_SCALE;
			var x = transX + displacement.x * scale + Math.sqrt(3) * scale / 4 - size / 4;
			var y = transY + displacement.y * scale + scale / 4 - size / 2;

			element.append(CITY_SVG);
			element.css("transform", "translate(" + x + "px, " + y + "px)");
			element.attr("height", size);
			element.attr("width", size);

			var svg = element[0].getElementsByTagName("svg")[0];
			svg.setAttribute("viewBox", "0 0 " + CITY_SVG_WIDTH + " " + CITY_SVG_WIDTH);
			svg.setAttribute("height", size);
			svg.setAttribute("width", size);

			var svg2 = $("#" + this.id).children().first();
			svg2.css("fill", this.player.color);

			if (this.metropolis) {
				var mSize = size / 2.5;
				var mColor = "gold";
				if (this.metropolis === "trade") mColor = "#4CAF50"; // Green
				else if (this.metropolis === "politics") mColor = "#2196F3"; // Blue
				else if (this.metropolis === "science") mColor = "#FFC107"; // Yellow

				// Append marker on top
				var metroMarker = '<div style="position:absolute; top:0; right:0; width:' + mSize + 'px; height:' + mSize + 'px; background-color:' + mColor + '; border: 1px solid black; border-radius: 50%; z-index:10;" title="Metropolis: ' + this.metropolis + '"></div>';
				element.append(metroMarker);
			}

			break;
		case BUILDING.KNIGHT:
			var size = scale * SETTLEMENT_SCALE * 1.5; // Slightly larger for knights
			var x = transX + displacement.x * scale + Math.sqrt(3) * scale / 4 - size / 4;
			var y = transY + displacement.y * scale + scale / 4 - size / 2;

			var iconName = "icon-knight-basic.svg";
			if (this.knightLevel === 2) iconName = "icon-knight-strong.svg";
			else if (this.knightLevel === 3) iconName = "icon-knight-mighty.svg";

			var imgHtml = "<img src='images/" + iconName + "' class='knight-icon' style='width:100%; height:100%;'>";
			element.append(imgHtml);

			element.css("transform", "translate(" + x + "px, " + y + "px)");
			element.css("width", size);
			element.css("height", size);

			// Style based on active status
			if (!this.knightActive) {
				element.css("opacity", "0.6");
				element.css("filter", "grayscale(100%)");
			} else {
				element.css("opacity", "1.0");
				element.css("filter", "none");
			}

			// Add a colored border/background to indicate owner?
			// The SVG itself is black/white usually. 
			// We can wrap it in a div with border-color = player.color
			element.css("border", "2px solid " + this.player.color);
			element.css("border-radius", "50%");
			element.css("background-color", "rgba(255,255,255,0.7)");

			break;
		default:
			break;
	}

	// Calculate size and displacement of selectable area
	var size = scale * SELECTABLE_AREA_SCALE;
	var x = transX + displacement.x * scale + Math.sqrt(3) * scale / 4 - size / 4 - 0.020 * scale;
	var y = transY + displacement.y * scale + scale / 4 - size / 2;

	if (this.building === BUILDING.SETTLEMENT) {
		x = x + 0.0075 * scale;
		y = y + 0.015 * scale;
	}

	// Add selectable area to intersection
	var select = $("#" + this.id + "-select");
	select.css("transform", "translate(" + x + "px, " + y + "px)");
	select.css("height", size);
	select.css("width", size);
}

/*
 * Adds a settlement to this intersection.
 * @param player - the player who owns the settlement
 */
Intersection.prototype.addSettlement = function (player) {
	this.building = BUILDING.SETTLEMENT;
	this.player = player;
}

/*
 * Adds a city to this intersection.
 * @param player - the player who owns the city
 */
Intersection.prototype.addCity = function (player, metropolis) {
	this.building = BUILDING.CITY;
	this.player = player;
	this.metropolis = metropolis;
}

Intersection.prototype.addKnight = function (player, level, active) {
	this.building = BUILDING.KNIGHT;
	this.player = player;
	this.knightLevel = level;
	this.knightActive = active;
}

/*
 * Creates an intersection click handler for building cities and settlements.
 */
Intersection.prototype.createIntersectionClickHandler = function () {
	var that = this;
	return function (event) {
		if (that.building === BUILDING.NONE) {
			if (inPlaceSettlementMode) {
				sendPlaceSettlementAction(that.intersectCoordinates);
				exitPlaceSettlementMode();
			} else if (inPlaceKnightMode) { // Need to ensure this global exists or logic handles it
				sendPlaceKnightAction(that.intersectCoordinates);
				exitPlaceKnightMode();
			} else {
				sendBuildSettlementAction(that.intersectCoordinates);
				exitBuildMode();
			}
		} else if (that.building === BUILDING.SETTLEMENT) {
			sendBuildCityAction(that.intersectCoordinates);
			exitBuildMode();
		}
		else if (that.building === BUILDING.CITY && inCityWallMode) {
			sendBuildCityWallAction(that.intersectCoordinates);
			exitCityWallMode();}
		else if (that.building === BUILDING.KNIGHT) {
			// Handle knight clicks (activate, promote, displace?)
			// For now, maybe just log or handle elsewhere. 
			// Usually handled by specific modes (activate/promote buttons then click knight).
			if (inActivateKnightMode) {
				sendActivateKnightAction(that.intersectCoordinates);
				exitActivateKnightMode();
			} else if (inPromoteKnightMode) {
				sendPromoteKnightAction(that.intersectCoordinates);
				exitPromoteKnightMode();
			} else if (inDisplaceKnightMode) { // This mode exists in main.js
				sendDisplaceKnightAction(that.intersectCoordinates);
				exitDisplaceKnightMode();
			}
			else if (inDeserterMode) { // Deserter progress card
				sendDeserterTargetAction(that.player.id, that.intersectCoordinates);
				exitDeserterTargetMode();
			}
		}
	};
}

/*
 * Highlights this intersection.
 */
Intersection.prototype.highlight = function () {
	if (!(this.highlighted)) {
		this.highlighted = true;

		var select = $("#" + this.id + "-select");
		select.addClass("highlighted");

		select.click(this.createIntersectionClickHandler());
	}
}

/*
 * Unhighlights this intersection.
 */
Intersection.prototype.unHighlight = function () {
	if (this.highlighted) {
		this.highlighted = false;

		var select = $("#" + this.id + "-select");
		select.removeClass("highlighted");

		var that = this;
		select.off("click");
	}
}

/*
 * Creates a new intersection from the given path data.
 * @param data - the intersection data
 */
function parseIntersection(data) {
	var position = data.coordinate;
	var intersect = new Intersection(parseHexCoordinates(position.coord1),
		parseHexCoordinates(position.coord2),
		parseHexCoordinates(position.coord3))

	if (data.hasOwnProperty("building")) {
		var player = playersById[data.building.player];
		if (data.building.type === "settlement") {
			intersect.addSettlement(player);
		} else if (data.building.type === "city") {
			intersect.addCity(player, data.building.metropolis);
		} else if (data.building.type === "knight") {
			intersect.addKnight(player, data.building.level, data.building.active);
		}
	}
// C&K: knights are sent separately from buildings
	if (data.hasOwnProperty("knight") && data.knight) {
		intersect.addKnight(playersById[data.knight.player], data.knight.level, data.knight.active);
	}
	if (data.hasOwnProperty("port")) {
		switch (data.port._resource) {
			case "BRICK":
				this.port = PORT.BRICK;
				break;
			case "WOOD":
				this.port = PORT.WOOD;
				break;
			case "ORE":
				this.port = PORT.ORE;
				break;
			case "WHEAT":
				this.port = PORT.WHEAT;
				break;
			case "SHEEP":
				this.port = PORT.SHEEP;
				break;
			case "WILDCARD":
				this.port = PORT.WILDCARD;
				break;
			default:
				break;
		}
	}

	intersect.canBuildSettlement = data.canBuildSettlement;

	return intersect;
}

