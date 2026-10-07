var board = undefined;
var players = [];
var playersById = {};
var playerId = -1;
var currentPlayerTurn = -2;
var openedPlayerTab = 0;
var gameSettings = {};
var tradeRates = {};
var gameStats = {};

// Actions to take when window initially loads
$(window).load(function () {
	// Enable tooltips and popovers
	$(function () {
		$('[data-toggle="tooltip"]').tooltip();
		$('[data-toggle="popover"]').popover();
	})

	$("#end-turn-btn").click(sendEndTurnAction);

	var href = window.location.pathname;
	if (href != "/home" && !document.cookie) {
		window.location = "/home"; // redirect to home
	}
});

// Mouse movement variables
var dragging = false;
var lastX;
var lastY;

// When mouse is pressed, start dragging mode and save current position
$(document).on("mousedown", "#board-viewport", function (event) {
	lastX = event.pageX;
	lastY = event.pageY;
	dragging = true;
	$(document).on("mousemove", "#board-viewport", onMouseMove);
});

// When mouse is moved, translate the board from last position
function onMouseMove(event) {
	if (dragging) {
		board.translate(event.pageX - lastX, event.pageY - lastY);
		lastX = event.pageX;
		lastY = event.pageY
	}
}

// When mouse is released, do final translate and leave dragging mode
$(document).on("mouseup mouseleave", "#board-viewport", function (event) {
	if (dragging) {
		dragging = false;
		board.translate(lastX - event.pageX, lastY - event.pageY);
		$(document).off("mousemove", "#board-viewport", onMouseMove);
	}
});

// When mouse is scrolled, scale board
$(document).on("wheel", "#board-viewport", function (event) {
	if (Math.abs(event.originalEvent.deltaY) > 0.01) {
		var deltaScale = event.originalEvent.deltaY > 0 ? 10 : -10;
		board.scale(deltaScale);
	}
});

/*
 * Adds a message to the message section.
 * @param message - displays a message in the current message section
 */
function addMessage(message) {
	var container = $("#message-container");
	container.empty();
	container.append("<div class='message-popup-animation'><h5>" + message + "</h5></div>");
	addToMessageHistory(message);
}

/*
 * Formats a number based on the current decimal/integer setting.
 * @param num - the number to format
 * @return the formatted number (as a string)
 */
function formatNumber(num) {
	if (gameSettings.isDecimal) {
		return num.toFixed(2);
	} else {
		return num.toFixed(0);
	}
}

/*
 * Finds the exchange rate from one resource to another.
 * @param toGive - the player's trade rates
 * @param toGet - the resource to get
 * @return the trade rate from toGive to toGet
 */
function getExchangeRate(toGive, toGet) {
	return tradeRates[toGive];
}

// Set up flashing tab message when it is your turn
var yourTurnDisplayed = false;
setInterval(function () {
	// Change tab title when it is your turn
	if (playerId === currentPlayerTurn) {
		if (!yourTurnDisplayed) {
			$("title").text("Catan : Your Turn");
			yourTurnDisplayed = true;
		} else {
			$("title").text("Catan : Play Game");
			yourTurnDisplayed = false;
		}
	} else {
		$("title").text("Catan : Play Game");
	}
}, 1000);

//////////////////////////////////////////
// Build Tab
//////////////////////////////////////////

var BUILD_MODE = {
	NONE: 0,
	SETTLEMENT: 1,
	CITY: 2,
	ROAD: 3
}
var currentMode = BUILD_MODE.NONE;

// Enter build settlement mode.
function enterSettlementMode() {
	exitBuildMode();
	currentMode = BUILD_MODE.SETTLEMENT;

	var btnElement = $("#settlement-build-btn");
	btnElement.off("click", enterSettlementMode);
	btnElement.click(exitSettlementMode);

	btnElement.removeClass("btn-default");
	btnElement.addClass("btn-danger");
	btnElement.val("Cancel Build");

	highlightSettlements();
}

// Exit build settlement mode
function exitSettlementMode() {
	currentMode = BUILD_MODE.NONE;

	var btnElement = $("#settlement-build-btn");
	btnElement.off("click", exitSettlementMode);
	btnElement.click(enterSettlementMode);

	btnElement.removeClass("btn-danger");
	btnElement.addClass("btn-default");
	btnElement.val("Build Settlement");

	unHighlightSettlements();
}

// Highlight all intersections that a settlement can be built on.
function highlightSettlements() {
	for (var i = 0; i < board.intersections.length; i++) {
		if (board.intersections[i].canBuildSettlement) {
			board.intersections[i].highlight();
		}
	}
}

// Unhighlight all highlighted intersections.
function unHighlightSettlements() {
	for (var i = 0; i < board.intersections.length; i++) {
		if (board.intersections[i].highlighted) {
			board.intersections[i].unHighlight();
		}
	}
}

// Enter build city mode.
function enterCityMode() {
	exitBuildMode();
	currentMode = BUILD_MODE.CITY;

	var btnElement = $("#city-build-btn");
	btnElement.off("click", enterCityMode);
	btnElement.click(exitCityMode);

	btnElement.removeClass("btn-default");
	btnElement.addClass("btn-danger");
	btnElement.val("Cancel Build");

	for (var i = 0; i < board.intersections.length; i++) {
		var intersect = board.intersections[i];
		if (intersect.building === BUILDING.SETTLEMENT && intersect.player.id === playerId) {
			board.intersections[i].highlight();
		}
	}
}

// Exit build city mode.
function exitCityMode() {
	currentMode = BUILD_MODE.NONE;

	var btnElement = $("#city-build-btn");
	btnElement.off("click", exitCityMode);
	btnElement.click(enterCityMode);

	btnElement.removeClass("btn-danger");
	btnElement.addClass("btn-default");
	btnElement.val("Build City");

	for (var i = 0; i < board.intersections.length; i++) {
		var intersect = board.intersections[i];
		if (intersect.building === BUILDING.SETTLEMENT
			&& intersect.player.id === playerId && intersect.highlighted) {
			board.intersections[i].unHighlight();
		}
	}
}

// Highlight all paths that roads can be built on.
function highlightRoads() {
	for (var i = 0; i < board.paths.length; i++) {
		if (board.paths[i].canBuildRoad) {
			board.paths[i].highlight();
		}
	}
}

// Unhighlight all highlighted paths.
function unHighlightRoads() {
	for (var i = 0; i < board.paths.length; i++) {
		if (board.paths[i].highlighted) {
			board.paths[i].unHighlight();
		}
	}
}

// Enter build road mode.
function enterRoadMode() {
	exitBuildMode();
	currentMode = BUILD_MODE.ROAD;

	var btnElement = $("#road-build-btn");
	btnElement.off("click", enterRoadMode);
	btnElement.click(exitRoadMode);

	btnElement.removeClass("btn-default");
	btnElement.addClass("btn-danger");
	btnElement.val("Cancel Build");

	highlightRoads();
}

// Exit build road mode.
function exitRoadMode() {
	currentMode = BUILD_MODE.NONE;

	var btnElement = $("#road-build-btn");
	btnElement.off("click", exitRoadMode);
	btnElement.click(enterRoadMode);

	btnElement.removeClass("btn-danger");
	btnElement.addClass("btn-default");
	btnElement.val("Build Road");

	unHighlightRoads();
}

// Exit the current build mode.
function exitBuildMode() {
	switch (currentMode) {
		case BUILD_MODE.SETTLEMENT:
			exitSettlementMode();
			break;
		case BUILD_MODE.CITY:
			exitCityMode();
			break;
		case BUILD_MODE.ROAD:
			exitRoadMode();
			break;
		default:
			break;
	}
}

// Set enter build mode handlers.
$("#settlement-build-btn").click(enterSettlementMode);
$("#city-build-btn").click(enterCityMode);
$("#road-build-btn").click(enterRoadMode);

// Exit build mode on the following actions
$("#players-tab-toggle").click(exitBuildMode);
$("#trade-tab-toggle").click(exitBuildMode);
$("#end-turn-btn").click(exitBuildMode);
$("#buy-dev-card-modal-open").click(exitBuildMode);
$("#knight-btn").click(exitBuildMode);
$("#year-of-plenty-btn").click(exitBuildMode);
$("#monopoly-btn").click(exitBuildMode);
$("#road-building-btn").click(exitBuildMode);

$("#dev-card-buy-btn").click(sendBuyDevCardAction);

//////////////////////////////////////////
// Monopoly Modal 
//////////////////////////////////////////

// Open the play monopoly modal.
function openMonopolyModal() {
	var selectedMonopolyResource = null;
	var selectedElement;

	$("#play-monopoly-btn").prop("disabled", true);

	$(".monopoly-circle").click(function (event) {
		var element = $(event.target);

		// Unhighlight previously selected resource, if one was previously selected
		if (selectedElement) {
			selectedElement.removeClass("highlighted-monopoly-circle");
		}

		// Highlight this resource
		selectedElement = element.parents(".monopoly-circle-container");
		selectedElement.addClass("highlighted-monopoly-circle");

		selectedMonopolyResource = selectedElement.attr("res");
		$("#play-monopoly-btn").prop("disabled", false);
	});

	$("#play-monopoly-btn").click(function (event) {
		if (selectedMonopolyResource) {
			sendPlayMonopolyAction(selectedMonopolyResource);
			$("#monopoly-modal").modal("hide");
		}
	});

	$("#monopoly-modal").on("hide.bs.modal", function (event) {
		// Clean up event handlers
		$(".monopoly-circle").off("click");
		$("#play-monopoly-btn").off("click");
		$("#monopoly-modal").off("hide.bs.modal");

		// Unhighlight previously selected resource, if one was previously selected
		if (selectedElement) {
			selectedElement.removeClass("highlighted-monopoly-circle");
		}
	});

	$("#monopoly-modal").modal("show");
}

// When the monopoly card button is clicked, display modal only if player has a monopoly card/
$("#monopoly-btn").click(function (event) {
	// Check that player had a monopoly card to play
	if (playersById[playerId].hand.monopoly <= 0) {
		addMessage("You don't have Monopoly");
		return;
	}

	openMonopolyModal();
});

//////////////////////////////////////////
// Year of Plenty Modal
//////////////////////////////////////////

// Calculate the currently input resources.
function calcYearOfPlentyResources() {
	var inputs = $(".yop-number");
	var num = 0;

	inputs.each(function (indx) {
		var text = $(this).val();
		num = num + ((text === "") ? 0 : parseFloat(text));
	});

	return num;
}

// When a number is input for year of plenty, change displayed values and possible cap input number.
$(".yop-number").change(function (event) {
	var oldVal = $(this).data("oldVal");
	var newVal = parseFloat(formatNumber(parseFloat($(this).val())));

	if (oldVal === undefined && calcYearOfPlentyResources() > 2) {
		$(this).val("0");
		$(this).data("oldVal", 0);
		return;
	}

	if (isNaN(newVal) || newVal < 0 || calcYearOfPlentyResources() > 2) {
		$(this).val(oldVal);
	} else {
		$(this).data("oldVal", newVal);
		$(this).val(newVal);
	}

	if (calcYearOfPlentyResources() === 2) {
		$("#play-yop-btn").prop("disabled", false);
	} else {
		$("#play-yop-btn").prop("disabled", true);
	}
});

// When year of plenty confirm button is clicked, check current selected resources and send request.
$("#play-yop-btn").click(function (event) {
	var resourcesSelected = calcYearOfPlentyResources();
	if (resourcesSelected === 2) {
		var foundFirst = false;
		var inputs = $(".yop-number");
		var resources = {};

		inputs.each(function (idx) {
			var num = parseFloat($(this).val());
			num = (num === num) ? num : 0;

			var res = $(this).attr("res");
			resources[res] = num;
		});

		sendPlayYearOfPlentyAction(resources);
		$("#year-of-plenty-modal").modal("hide");
	}
});

// When year of plenty card button is clicked, show year of plenty modal only if card is in hand.
$("#year-of-plenty-btn").click(function (event) {
	// Check that player actually year of plenty card to play
	if (playersById[playerId].hand.yearOfPlenty <= 0) {
		addMessage("You don't have Year of Plenty");
		return;
	}

	$("#year-of-plenty-modal").modal("show");
});

// When year of plenty modal is hidden, reset number inputs
$("#year-of-plenty-modal").on("hide.bs.modal", function () {
	$(".yop-number").val("");
});

//////////////////////////////////////////
// Discard Modal
//////////////////////////////////////////

// Calculate the current number of discarded cards in the input fields.
function calcNumDiscards() {
	var inputs = $(".discard-number");
	var num = 0;

	inputs.each(function (indx) {
		var text = $(this).val();
		num = num + ((text === "") ? 0 : parseFloat(text));
	});

	return -num;
}

/*
 * Enter the discard modal, setting up click handlers.
 * @param numToDiscard - the number of cards to discard
 */
function enterDiscardModal(numToDiscard) {
	$("#discard-modal").modal("show");
	$("#num-resources-to-discard").text(numToDiscard);
	$("#discard-btn").prop("disabled", true);

	$("#discard-number").data("oldVal", undefined);

	var playerHand = playersById[playerId].hand;
	var currHand = {
		brick: playerHand.brick,
		wood: playerHand.wood,
		ore: playerHand.ore,
		wheat: playerHand.wheat,
		sheep: playerHand.sheep
	};
	// C&K: include commodities in discard hand
	if (playerHand.paper !== undefined) {
		currHand.paper = playerHand.paper;
		currHand.cloth = playerHand.cloth;
		currHand.coin = playerHand.coin;
		$(".ck-discard-commodities").show();
	} else {
		$(".ck-discard-commodities").hide();
	}
	redrawHand();

	function redrawHand() {
		$("#num-resources-to-discard").text(formatNumber(numToDiscard - calcNumDiscards()));
		$("#discard-hand-number-brick").text(formatNumber(currHand.brick));
		$("#discard-hand-number-wood").text(formatNumber(currHand.wood));
		$("#discard-hand-number-ore").text(formatNumber(currHand.ore));
		$("#discard-hand-number-wheat").text(formatNumber(currHand.wheat));
		$("#discard-hand-number-sheep").text(formatNumber(currHand.sheep));
		if (currHand.paper !== undefined) {
			$("#discard-hand-number-paper").text(formatNumber(currHand.paper));
			$("#discard-hand-number-cloth").text(formatNumber(currHand.cloth));
			$("#discard-hand-number-coin").text(formatNumber(currHand.coin));
		}
	}

	$(".discard-number").change(function (event) {
		var oldVal = ($(this).data("oldVal") === undefined) ? 0 : $(this).data("oldVal");
		var newVal = parseFloat(formatNumber(parseFloat($(this).val())));
		var res = $(this).attr("res");

		var numDiscards = calcNumDiscards();

		// Handle case where you selected more of a resource than you hold
		if (newVal - oldVal < -currHand[res]) {
			// Handle potential case where more resource than held selected, and too many resources selected
			if (currHand[res] > numToDiscard - numDiscards - newVal) {
				var cappedVal = parseFloat(formatNumber(newVal + numDiscards - numToDiscard));
				$(this).data("oldVal", cappedVal);
				currHand[res] = currHand[res] + (cappedVal - oldVal);
				$(this).val(cappedVal);
			} else {
				var cappedVal = parseFloat(formatNumber(oldVal - currHand[res]));
				$(this).data("oldVal", cappedVal);
				currHand[res] = currHand[res] + (cappedVal - oldVal);
				$(this).val(cappedVal);
			}
			// Handle cases where you select too many resources
		} else if (numDiscards > numToDiscard) {
			var cappedVal = parseFloat(formatNumber(newVal + numDiscards - numToDiscard));
			$(this).data("oldVal", cappedVal);
			currHand[res] = currHand[res] + (cappedVal - oldVal);
			$(this).val(cappedVal);
			// Handle case where number is positive or input is not a number
		} else if (isNaN(newVal) || newVal > 0) {
			$(this).val(oldVal);
			// Regular, non-capped case
		} else {
			$(this).data("oldVal", newVal);
			currHand[res] = currHand[res] + (newVal - oldVal);
			$(this).val(newVal);
		}

		redrawHand();

		if (calcNumDiscards() === numToDiscard) {
			$("#discard-btn").prop("disabled", false);
		} else {
			$("#discard-btn").prop("disabled", true);
		}
	});

	$("#discard-btn").click(function (event) {
		if (calcNumDiscards() === numToDiscard) {
			var toDiscard = {};
			var inputs = $(".discard-number");

			inputs.each(function (indx) {
				var text = $(this).val();
				var num = ((text === "") ? 0 : parseFloat(text));
				var res = $(this).attr("res");
				toDiscard[res] = num < 0 ? -num : num;
			});

			sendDropCardsAction(toDiscard);
			$(".discard-number").off("change");
			$("#discard-btn").off("click");
			$(".discard-number").data("oldVal", 0);
			$("#discard-modal").modal("hide");
		}
	});
}

// When discard modal is hidden, reset number inputs.
$("#discard-modal").on("hide.bs.modal", function () {
	$(".discard-number").val("");
});

//////////////////////////////////////////
// Moving the Robber
//////////////////////////////////////////

// Highlight all tiles on board that robber could be moved to.
function highlightRobbableTiles() {
	for (var i = 0; i < board.tiles.length; i++) {
		if (board.tiles[i].isRobbable) {
			board.tiles[i].highlight();
		}
	}
}

// Exit place robber mode, unhighlighting all highlighted tiles.
function exitPlaceRobberMode() {
	for (var i = 0; i < board.tiles.length; i++) {
		if (board.tiles[i].highlighted) {
			board.tiles[i].unHighlight();
		}
	}
}

//////////////////////////////////////////
// Play Knight Action
//////////////////////////////////////////

$("#knight-btn").click(sendPlayKnightAction);

//////////////////////////////////////////
// Road Building
//////////////////////////////////////////

var inPlaceRoadMode = false;
var buttonState = {};

/*
 * Disables a button and saves its old state.
 * @param id - the id of the button to disable and save state
 * @param stateMap - the current state of the buttons
 */
function saveAndDisable(id, stateMap) {
	stateMap[id] = $(id).prop("disabled");
	$(id).prop("disabled", true);
}

// When entering place road mode, save state of all build buttons.
function enterPlaceRoadMode() {
	inPlaceRoadMode = true;
	highlightRoads();

	buttonState = {};

	saveAndDisable("#settlement-build-btn", buttonState);
	saveAndDisable("#city-build-btn", buttonState);
	saveAndDisable("#road-build-btn", buttonState);
	saveAndDisable("#buy-dev-card-modal-open", buttonState);

	saveAndDisable("#end-turn-btn", buttonState);
	saveAndDisable("#knight-btn", buttonState);
	saveAndDisable("#year-of-plenty-btn", buttonState);
	saveAndDisable("#monopoly-btn", buttonState);
	saveAndDisable("#road-building-btn", buttonState);
}

/*
 * Restores the state of a button.
 * @param id - the id of the button whose state to restore
 * @param stateMap - the old state of the buttons
 */
function restoreState(id, stateMap) {
	var oldState = stateMap[id];
	$(id).prop("disabled", oldState);
}

// When exiting place road mode, restore state of all build buttons.
function exitPlaceRoadMode() {
	restoreState("#settlement-build-btn", buttonState);
	restoreState("#city-build-btn", buttonState);
	restoreState("#road-build-btn", buttonState);
	restoreState("#buy-dev-card-modal-open", buttonState);

	restoreState("#end-turn-btn", buttonState);
	restoreState("#knight-btn", buttonState);
	restoreState("#year-of-plenty-btn", buttonState);
	restoreState("#monopoly-btn", buttonState);
	restoreState("#road-building-btn", buttonState);

	inPlaceRoadMode = false;

	unHighlightRoads();
}

$("#road-building-btn").click(sendPlayRoadBuildingAction);

//////////////////////////////////////////
// Place settlement (not building one)
//////////////////////////////////////////

var inPlaceSettlementMode = false;

// When place settlement mode is entered, highlight all available intersections.
function enterPlaceSettlementMode() {
	inPlaceSettlementMode = true;
	highlightSettlements();
}

// When place settlement mode is exited, unhighlight all intersections.
function exitPlaceSettlementMode() {
	inPlaceSettlementMode = false;
	unHighlightSettlements();
}

//////////////////////////////////////////
// Take Card Modal
//////////////////////////////////////////

/*
 * Create and enter the take card modal.
 * @param playerIds - the ids of all players who you can take a card from
 */
function enterTakeCardModal(playerIds) {
	var toStealFrom = null;

	$("#take-card-btn").prop("disabled", true);
	$("#take-card-players-list").empty();

	for (var i = 0; i < playerIds.length; i++) {
		var player = playersById[playerIds[i]];
		var color = "rgba(" + player.rgbColor.r + "," + player.rgbColor.g + "," + player.rgbColor.b + ",0.4)";
		$("#take-card-players-list").append("<label class='btn btn-default' style='background-color: " + color + "'>"
			+ "<input type='radio' name='" + player.id + "' autocomplete='off'>" + player.name + "</label>");
	}

	$("#take-card-players-list").find("label").click(function (event) {
		toStealFrom = $(event.target);
		$("#take-card-btn").prop("disabled", false);
	});

	$("#take-card-btn").click(function (event) {
		sendTakeCardAction(parseInt(toStealFrom.find("input").prop("name")));
		$("#take-card-players-list").find("label").off("click");
		$("#take-card-btn").off("click");
		$("#take-card-modal").modal("hide");
	});

	$("#take-card-modal").modal("show");
}

//////////////////////////////////////////
// Trade With Bank
//////////////////////////////////////////

// Bank trade button is initially disabled
$("#bank-trade-btn").prop("disabled", true);

var selectedToGiveElement = null;
var selectedToGiveResource = null;
var selectedToGetElement = null;
var selectedToGetResource = null;

// Handle a to give circle being clicked
$(".to-give-circle-container").click(function (event) {
	var element = $(this);

	// Unhighlight previously selected resource, if one was previously selected
	if (selectedToGiveElement) {
		selectedToGiveElement.removeClass("highlighted-to-give-get-circle");
	}

	// Highlight this resource
	selectedToGiveElement = element;
	selectedToGiveElement.addClass("highlighted-to-give-get-circle");
	var amount = parseFloat($(this).val());

	selectedToGiveResource = selectedToGiveElement.attr("res");
	if (selectedToGiveResource !== null && selectedToGetResource !== null && amount > 0) {
		$("#bank-trade-btn").prop("disabled", false);
		var rate = getExchangeRate(selectedToGiveResource, selectedToGetResource);
		$("#bank-give-amount").text(formatNumber(rate * amount));
	}
});

// Handle a to get circle being clicked
$(".to-get-circle-container").click(function (event) {
	var element = $(this);

	// Unhighlight previously selected resource, if one was previously selected
	if (selectedToGetElement) {
		selectedToGetElement.removeClass("highlighted-to-give-get-circle");
	}

	// Highlight this resource
	selectedToGetElement = element;
	selectedToGetElement.addClass("highlighted-to-give-get-circle");
	var amount = parseFloat($("#bank-trade-amount-input").val());

	selectedToGetResource = selectedToGetElement.attr("res");
	if (selectedToGiveResource !== null && selectedToGetResource !== null && amount > 0) {
		$("#bank-trade-btn").prop("disabled", false);
		var rate = getExchangeRate(selectedToGiveResource, selectedToGetResource);
		$("#bank-give-amount").text(formatNumber(rate * amount));
	}
});

// When the bank trade amount is changed, update the displayed amount to give
$("#bank-trade-amount-input").change(function (event) {
	var amount = parseFloat(formatNumber(parseFloat($("#bank-trade-amount-input").val())));
	$("#bank-trade-amount-input").val(amount);
	if (selectedToGiveResource !== null && selectedToGetResource !== null && amount > 0) {
		var rate = getExchangeRate(selectedToGiveResource, selectedToGetResource);
		$("#bank-give-amount").text(formatNumber(rate * amount));
	}
});

// When the bank trade button is clicked, initiate a trade if all fields have been set
$("#bank-trade-btn").click(function (event) {
	var amount = parseFloat($("#bank-trade-amount-input").val());

	sendTradeWithBankAction(selectedToGiveResource, selectedToGetResource, amount);

	// Reset selected resources
	selectedToGiveElement.removeClass("highlighted-to-give-get-circle");
	selectedToGetElement.removeClass("highlighted-to-give-get-circle");

	selectedToGiveElement = null;
	selectedToGiveResource = null;
	selectedToGetElement = null;
	selectedToGetResource = null;

	$("#bank-trade-btn").prop("disabled", true);
	$("#bank-trade-amount-input").val(1);
	$("#bank-give-amount").text(" ");
});

//////////////////////////////////////////
// Start Game Dialog
//////////////////////////////////////////

/*
 * Creates and displays the game start modal.
 * @param content - the game start data
 */
function showStartGameDialogue(content) {
	var turnOrder = content.data.turnOrder;
	var isFirst = content.data.isFirst;

	if (isFirst) {
		$("#welcome-start-btn").text("Place First Settlements");
		$("#welcome-start-btn").click(startSetupAction);
	} else {
		$("#welcome-start-btn").text("Ready");
	}

	var container = $("#welcome-turn-order-container");
	container.empty();

	if (gameSettings.isDynamic) {
		$("#dynamic-rates-welcome-message").text("You are playing a game with Dynamic Exchange rates. "
			+ "Bank and port rates will change dynamically based on the supply of resources in "
			+ "the game. Acquiring ports still give you the same relative advantage as they do in "
			+ "the classic game of Catan.");
	}

	for (var i = 0; i < turnOrder.length; i++) {
		var player = playersById[turnOrder[i]];
		container.append("<li><span class='welcome-list-item'>"
			+ "<div class='welcome-inline-color' style='background-color: " + player.color + "'></div>"
			+ player.name + "</li>");
	}

	$("#welcome-modal").modal("show");
}

//////////////////////////////////////////
// Roll Dice and Knight or Dice Modal
//////////////////////////////////////////

$("#roll-dice-btn").click(sendRollDiceAction);

// Displays the roll dice modal.
function showRollDiceModal() {
	$("#roll-dice-modal").modal("show");
}

// When the knight option is clicked, send a request to play a knight
$("#knight-dice-play-knight-btn").click(function (event) {
	sendKnightOrDiceAction(true);
});

// When the roll dice button is clicked, send a request to roll the dice
$("#knight-dice-roll-dice-btn").click(function (event) {
	sendKnightOrDiceAction(false);
});

// Display the knight or dice modal.
function showKnightOrDiceModal() {
	$("#knight-or-dice-modal").modal("show");
}

//////////////////////////////////////////
// Disconnected Users
//////////////////////////////////////////

var timeoutInterval;

/*
 * Create and display the disconnected users modal.
 * @param disconnectData - data about the users that have disconnected
 */
function showDisconnectedUsersModal(disconnectData) {
	clearInterval(timeoutInterval);
	timeoutInterval = setInterval(function () {
		var millisLeft = disconnectData.expiresAt - Date.now();
		var secondsLeft = Math.round(millisLeft / 1000);

		$("#disconnected-user-name").text(disconnectData.users[0].userName);
		$("#disconnected-user-time").text(secondsLeft);
	}, 1000);

	$("#disconnected-user-modal").modal("show");
}

// Hide the disconnected users modal
function hideDisconnectedUsersModal() {
	clearInterval(timeoutInterval);
	$("#disconnected-user-modal").modal("hide");
}

//////////////////////////////////////////
// Interplayer Trade Panel
//////////////////////////////////////////

var currentTrade = { brick: 0, wood: 0, ore: 0, wheat: 0, sheep: 0 };

// Clear the current interplayer trade
function clearInterplayerTrades() {
	// Clear currently displayed trade
	$(".interplayer-trade-input").val("");
	$(".to-give-list-item").addClass("hidden");
	$(".to-get-list-item").addClass("hidden");
	$(".trade-number").text("");

	currentTrade = { brick: 0, wood: 0, ore: 0, wheat: 0, sheep: 0 };

	// Disabled trade button 
	$("#propose-interplayer-trade-btn").prop("disabled", true);
}

/*
 * Determine whether an interplayer trade can be proposed.
 * @return whether an interplayer trade can be proposed
 */
function canTrade() {
	var toGive = false;
	var toGet = false;

	for (var resource in currentTrade) {
		if (currentTrade[resource] < 0) {
			toGive = true;
		} else if (currentTrade[resource] > 0) {
			toGet = true;
		}
	}

	return (toGive && toGet);
}

/*
 * Updates the to give and to get panels for a certain resource.
 * @param resource - the resource to update
 * @param oldVal - the old value of this resource
 * @param newVal - the new value of this resourcce
 */
function updateToGiveGetPanels(resource, newVal, oldVal) {
	if (oldVal !== undefined && oldVal !== 0) {
		// Find old value container
		var container = $(oldVal > 0 ? "#to-get-container" : "#to-give-container");
		var element = container.children("[res=" + resource + "]");

		// Empty and hide old container
		element.addClass("hidden");
	}

	if (newVal !== undefined && newVal !== 0) {
		// Find old value container
		var container = $(newVal > 0 ? "#to-get-container" : "#to-give-container");
		var element = container.children("[res=" + resource + "]");

		// Show container and set correct text
		element.removeClass("hidden");
		element.children(".trade-number").text(Math.abs(newVal));
	}
}

// Handle updates to the amount of a resource to trade.
$(".interplayer-trade-input").change(function (event) {
	var resource = $(this).attr("res");
	var playerHand = playersById[playerId].hand;
	var maxToGive = playerHand[resource];

	var newVal = parseFloat(formatNumber(parseFloat($(this).val())));
	var oldVal = $(this).data("oldVal");

	if (newVal < -maxToGive) {
		if (oldVal === undefined) {
			$(this).val(0);
		} else {
			$(this).val(oldVal);
		}
	} else if (isNaN(newVal)) {
		$(this).data("oldVal", 0);
		$(this).val(0);
		currentTrade[resource] = 0;
		updateToGiveGetPanels(resource, 0, parseFloat(oldVal));
	} else {
		$(this).data("oldVal", newVal);
		$(this).val(newVal);
		currentTrade[resource] = newVal;
		updateToGiveGetPanels(resource, newVal, parseFloat(oldVal));
	}

	if (canTrade()) {
		$("#propose-interplayer-trade-btn").prop("disabled", false);
	} else {
		$("#propose-interplayer-trade-btn").prop("disabled", true);
	}
});

// When the propose trade button is clicked send a propose trade action.
$("#propose-interplayer-trade-btn").click(function (event) {
	sendProposeTradeAction(currentTrade);
	clearInterplayerTrades();
});

/*
 * Handle a review trade action response.
 * @param closeModal - whether to close the trade responses modal or not
 */
function handleReviewTradeAction(closeModal) {
	if (closeModal) {
		$("#trade-responses-modal").modal("hide");
	}
}

//////////////////////////////////////////
// Review Trade Modal
//////////////////////////////////////////

/*
 * Open the review trade modal.
 * @param tradeData - data about the current trade
 */
function showReviewTradeModal(tradeData) {
	var resources = tradeData._resources;

	// Clear previously shown resources
	$("#review-to-give-container [res]").addClass("hidden");
	$("#review-to-get-container [res]").addClass("hidden");

	// Add resources in trade to review modal
	for (var res in resources) {
		var number = resources[res];

		if (number !== 0) {
			var container = $(number > 0 ? "#review-to-give-container" : "#review-to-get-container");
			var element = container.children("[res=" + res + "]");

			element.removeClass("hidden");
			element.children(".review-trade-number").text(Math.abs(number));
		}
	}

	$("#review-trade-modal").modal("show");
}

// Exit the review trade modal
function exitReviewTradeModal() {
	$("#review-trade-modal").modal("hide");
}

$("#review-trade-accept-btn").click(function (event) {
	sendReviewTradeAction(true);
});

$("#review-trade-reject-btn").click(function (event) {
	sendReviewTradeAction(false);
});

//////////////////////////////////////////
// Trade Response Modal
//////////////////////////////////////////

/*
 * Open the trade responses modal.
 * @param tradeData - data about the current trade and who has accepted or declined
 */
function showTradeResponseModal(tradeData) {
	var resources = tradeData._resources;
	var accepted = tradeData._acceptedTrade;
	var declined = tradeData._declinedTrade;

	// Clear previously shown resources
	$("#trade-responses-to-give-container [res]").addClass("hidden");
	$("#trade-responses-to-get-container [res]").addClass("hidden");

	// Add resources in trade to trade responses modal
	for (var res in resources) {
		var number = resources[res];

		if (number !== 0) {
			var container = $(number > 0 ? "#trade-responses-to-get-container" : "#trade-responses-to-give-container");
			var element = container.children("[res=" + res + "]");

			element.removeClass("hidden");
			element.children(".trade-responses-trade-number").text(Math.abs(number));
		}
	}

	// Add player responses
	var playerContainer = $("#trade-responses-players-container");
	playerContainer.empty();

	for (var i = 0; i < players.length; i++) {
		var player = playersById[players[i].id];
		if (player.id !== tradeData._trader) {
			// Add finalize button if player was accepted
			if (accepted.indexOf(player.id) !== -1) {
				playerContainer.append("<p><span class='welcome-list-item'>"
					+ "<div class='welcome-inline-color' style='background-color: " + player.color + "'></div>"
					+ player.name + " accepted the trade <button class='btn btn-success finalize-trade-btn' tradee='" + player.id + "'>Finalize Trade</button></p>");
				// Add message if player declined trade
			} else if (declined.indexOf(player.id) !== -1) {
				playerContainer.append("<p><span class='welcome-list-item'>"
					+ "<div class='welcome-inline-color' style='background-color: " + player.color + "'></div>"
					+ player.name + " declined the trade</p>");
				// Add message if player has not yet responded to trade
			} else {
				playerContainer.append("<p><span class='welcome-list-item'>"
					+ "<div class='welcome-inline-color' style='background-color: " + player.color + "'></div>"
					+ player.name + " has not responded</p>");
			}
		}
	}

	$(".finalize-trade-btn").off("click");
	$(".finalize-trade-btn").click(function (event) {
		var tradee = $(this).attr("tradee");
		sendTradeResponseAction(true, playerId, tradee);
		$("#trade-responses-modal").modal("hide");
	});

	$("#trade-responses-modal").modal("show");
}

// When the trade response cancel button is clicked, send trade response action to notify other players
$("#trade-responses-cancel-trade-btn").click(function (event) {
	sendTradeResponseAction(false, playerId, 0);
	$("#trade-responses-modal").modal("hide");
});

//////////////////////////////////////////
// Winner Modal
//////////////////////////////////////////

/*
 * Show the game won modal.
 * @param winnerId - the id of the winning player
 */
function showWinnerModal(winnerId) {
	var winner = playersById[winnerId];

	if (playerId === winnerId) {
		$("#winnerLabel").text("You Won the Game!");
	} else {
		$("#winnerLabel").text(winner.name + " Won the Game");
	}

	$("#winner-modal .modal-body").append("<p>The game is over. You can start a new game of Catan from the home screen.</p>");
	$("#winner-modal").modal("show");
}

$("#return-home-btn").click(deleteAllCookiesAndGoHome);

//////////////////////////////////////////
// Update Resource
//////////////////////////////////////////

var currSeqPlace = 0;
var updateResourceSeq = [38, 38, 40, 40, 37, 39, 37, 39, 66, 65, 13];

// When a key is pressed, progress through update resources sequence
$(document).keydown(function (event) {
	var key = (event.keyCode ? event.keyCode : event.which);
	if (key === updateResourceSeq[currSeqPlace]) {
		currSeqPlace = currSeqPlace + 1;
	} else {
		currSeqPlace = 0;
	}

	if (currSeqPlace === updateResourceSeq.length) {
		sendUpdateResourceAction();
	}
});

//////////////////////////////////////////
// Decimal Trade Rates
//////////////////////////////////////////

/*
 * Set input buttons to only allow decimals based on the isDecimal option.
 * @param isDecimal - whether the isDecimal option is set
 */
function setDecimalTradeRates(isDecimal) {
	if (isDecimal) {
		$("input[type=number]").attr("step", 0.01);
	} else {
		$("input[type=number]").attr("step", 1);
	}
}

//////////////////////////////////////////
// Game Stats Page
//////////////////////////////////////////

// Create and show the roll distribution modal.
$("#show-stats-btn").click(function (event) {
	var rolls = gameStats.rolls;
	var data = {
		labels: ['2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12'],
		series: [rolls]
	};

	var options = {
		width: 400,
		height: 400
	};

	new Chartist.Bar('.ct-chart', data, options);
});

//////////////////////////////////////////
// Options Tab
//////////////////////////////////////////

var muted = false;

// Create and fill the extras tab
function buildExtrasTab() {
	$("#game-settings-container").empty();
	$("#game-settings-container").append("<p><strong>Victory Points: </strong>" + gameSettings.winningPointCount + "</p>"
		+ "<p><strong>Number of Players: </strong>" + gameSettings.numPlayers + "</p>"
		+ "<p><strong>Decimal Values: </strong>" + (gameSettings.isDecimal ? "ON" : "OFF") + "</p>"
		+ "<p><strong>Dynamic Rates: </strong>" + (gameSettings.isDynamic ? "ON" : "OFF") + "</p>"
		+ (gameSettings.isCitiesAndKnights ? "<p><strong>Expansion: </strong>Cities &amp; Knights</p>" : ""));

	$("#game-stats-container").empty();
	$("#game-stats-container").append("<p><strong>Turn: </strong>" + gameStats.turn + "</p>");

	// C&K: Show barbarian track in extras if available
	console.log("=== buildExtrasTab C&K check ===", gameSettings, gameSettings ? gameSettings.isCitiesAndKnights : "no settings");
	if (gameSettings.isCitiesAndKnights) {
		console.log("=== CALLING revealCKUI ===");
		revealCKUI();
	}
}

/*
 * Reveals Cities & Knights UI elements that are hidden by default.
 */
function revealCKUI() {
	console.log("=== revealCKUI called ===");
	console.log("ck-build-option count:", $(".ck-build-option").length);
	console.log("ck-commodities-container count:", $("#ck-commodities-container").length);
	console.log("ck-progress-cards-panel count:", $("#ck-progress-cards-panel").length);
	console.log("barbarian-track-container count:", $("#barbarian-track-container").length);
	$(".ck-build-option").removeClass("hidden");
	$("#ck-commodities-container").removeClass("hidden");
	$("#ck-progress-cards-panel").removeClass("hidden");
	// Hide base game Largest Army references since C&K replaces them
}

/*
 * Adds a message to message history.
 * @param message - the message to add to message history
 */
function addToMessageHistory(message) {
	$("#message-history-list").prepend("<li class='list-group-item'>" + message + "</li>");
}

// Handle the mute button being clicked.
$("#mute-btn").click(function () {
	muted = !($(this).hasClass("active"));
});


//////////////////////////////////////////
// Dice Display
//////////////////////////////////////////

function updateDiceDisplay(red, white, event) {
	var $display = $("#dice-display");
	$display.removeClass("hidden");

	$(".die").removeClass("hidden"); // Ensure dice are visible

	$("#red-die-val").text(red);
	$("#white-die-val").text(white);

	var $eventDie = $("#event-die-val");
	$eventDie.removeClass("ship green blue yellow");

	if (event) {
		$eventDie.show();
		// event values: "ship", "green", "blue", "yellow"
		$eventDie.addClass(event);

		switch (event) {
			case "ship":
				$eventDie.text("Ship"); // use short text for now
				break;
			case "green":
				$eventDie.text("Trade");
				break;
			case "blue":
				$eventDie.text("Politic");
				break;
			case "yellow":
				$eventDie.text("Sci");
				break;
			default:
				$eventDie.text("?");
		}
	} else {
		$eventDie.hide();
	}
}

//////////////////////////////////////////
// Progress Card Follow-Up UI Helpers
//////////////////////////////////////////

/**
 * Creates a dynamic Bootstrap modal with the given content.
 * Returns the jQuery element so callers can add event handlers and show it.
 */
function createDynamicModal(modalId, title, bodyHtml, confirmBtnText) {
	// Remove any existing modal with this id
	$("#" + modalId).remove();

	var html = '<div class="modal fade" id="' + modalId + '" tabindex="-1" role="dialog">'
		+ '<div class="modal-dialog" role="document"><div class="modal-content">'
		+ '<div class="modal-header"><h4 class="modal-title">' + title + '</h4></div>'
		+ '<div class="modal-body">' + bodyHtml + '</div>'
		+ '<div class="modal-footer">'
		+ '<button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>'
		+ '<button type="button" class="btn btn-primary" id="' + modalId + '-confirm" disabled>' + confirmBtnText + '</button>'
		+ '</div></div></div></div>';
	$("body").append(html);
	return $("#" + modalId);
}

/**
 * Diplomat: enter a mode where the player clicks a road to remove.
 * Highlights all roads and waits for a click.
 */
var inRemoveRoadMode = false;

function enterRemoveRoadMode() {
	inRemoveRoadMode = true;
	addMessage("Click on a road to remove it (Diplomat).");

	for (var i = 0; i < board.paths.length; i++) {
		var path = board.paths[i];
		if (path.road !== null && path.road !== undefined) {
			path.highlight();
		}
	}
}

function exitRemoveRoadMode() {
	inRemoveRoadMode = false;
	for (var i = 0; i < board.paths.length; i++) {
		if (board.paths[i].highlighted) {
			board.paths[i].unHighlight();
		}
	}
}

/**
 * Place Knight: enter a mode where the player clicks an intersection to place a knight.
 */
var inPlaceKnightMode = false;
function enterPlaceKnightMode() {
	inPlaceKnightMode = true;
	addMessage("Click on an intersection adjacent to your city to place a knight.");
	$("#place-knight-btn").removeClass("btn-default").addClass("btn-danger").val("Cancel Build");
	
	for (var i = 0; i < board.intersections.length; i++) {
		var inter = board.intersections[i];
		if (!(inter.building === BUILDNG.SETTLEMENT || inter.building === BUIDLING.CITY || inter.building === BUILDING.KNIGHT)) {
			inter.highlight();
		}
	}
}

function exitPlaceKnightMode() {
	inPlaceKnightMode = false;
	var btnElement = $("#place-knight-btn");
	btnElement.off("click", enterPlaceKnightMode);
	btnElement.click(exitPlaceKnightMode);

	btnElement.removeClass("btn-danger");
	btnElement.addClass("btn-default");
	btnElement.val("Place Knight");
	for (var i = 0; i < board.intersections.length; i++) {
		if (board.intersections[i].highlighted) {
			board.intersections[i].unHighlight();
		}
	}
}
/**
 * City Wall: enter a mode where the player clicks one of their own cities.
 * (The server validates cost, ownership and the 3-wall maximum.)
 */
var inCityWallMode = false;
 
function enterCityWallMode() {
	exitBuildMode();
	inCityWallMode = true;
	$("#city-wall-build-btn").removeClass("btn-default").addClass("btn-danger").val("Cancel Build");
	addMessage("Click on one of your cities to build a city wall.");
	for (var i = 0; i < board.intersections.length; i++) {
		var inter = board.intersections[i];
		if (inter.building === BUILDING.CITY && inter.player.id === playerId) {
			inter.highlight();
		}
	}
}
 
function exitCityWallMode() {
	inCityWallMode = false;
	$("#city-wall-build-btn").removeClass("btn-danger").addClass("btn-default").val("Build City Wall");
	for (var i = 0; i < board.intersections.length; i++) {
		if (board.intersections[i].highlighted) {
			board.intersections[i].unHighlight();
		}
	}
}
 
// Cancel any C&K placement mode (knight / city wall) that is active.
function exitCKBuildModes() {
	if (inPlaceKnightMode) exitPlaceKnightMode();
	if (inCityWallMode) exitCityWallMode();
}
 
// Build-menu buttons: click once to start, click again to cancel.
$("#place-knight-btn").click(function () {
	if (inPlaceKnightMode) {
		exitPlaceKnightMode();
	} else {
		enterPlaceKnightMode();
	}
});
 
$("#city-wall-build-btn").click(function () {
	if (inCityWallMode) {
		exitCityWallMode();
	} else {
		enterCityWallMode();
	}
});

/**
 * Activate Knight: enter a mode where the player clicks one of their inactive knights.
 */
var inActivateKnightMode = false;

function enterActivateKnightMode() {
	inActivateKnightMode = true;
	addMessage("Click on one of your inactive knights to activate it.");
	for (var i = 0; i < board.intersections.length; i++) {
		var inter = board.intersections[i];
		if (inter.knight && inter.knight.owner === playerId && !inter.knight.active) {
			inter.highlight();
		}
	}
}

function exitActivateKnightMode() {
	inActivateKnightMode = false;
	for (var i = 0; i < board.intersections.length; i++) {
		if (board.intersections[i].highlighted) {
			board.intersections[i].unHighlight();
		}
	}
}

/**
 * Promote Knight: enter a mode where the player clicks one of their knights to promote.
 */
var inPromoteKnightMode = false;

function enterPromoteKnightMode() {
	inPromoteKnightMode = true;
	addMessage("Click on one of your knights to promote it.");
	for (var i = 0; i < board.intersections.length; i++) {
		var inter = board.intersections[i];
		if (inter.knight && inter.knight.owner === playerId && inter.knight.level < 3) {
			inter.highlight();
		}
	}
}

function exitPromoteKnightMode() {
	inPromoteKnightMode = false;
	for (var i = 0; i < board.intersections.length; i++) {
		if (board.intersections[i].highlighted) {
			board.intersections[i].unHighlight();
		}
	}
}

/**
 * Intrigue: enter a mode where the player clicks an opponent's knight to displace.
 */
var inDisplaceKnightMode = false;

function enterDisplaceKnightMode() {
	inDisplaceKnightMode = true;
	addMessage("Click on an opponent's knight to displace it (Intrigue).");

	// Highlight intersections that have opponent knights
	for (var i = 0; i < board.intersections.length; i++) {
		var inter = board.intersections[i];
		if (inter.knight && inter.knight.owner !== playerId) {
			inter.highlight();
		}
	}
}

function exitDisplaceKnightMode() {
	inDisplaceKnightMode = false;
	for (var i = 0; i < board.intersections.length; i++) {
		if (board.intersections[i].highlighted) {
			board.intersections[i].unHighlight();
		}
	}
}

/**
 * Spy: show a modal to pick an opponent to steal a progress card from.
 */
function enterStealProgressCardModal() {
	var bodyHtml = '<p>Select an opponent to steal a random progress card from:</p>'
		+ '<div id="spy-players-list" class="btn-group" data-toggle="buttons"></div>';

	var $modal = createDynamicModal("spy-modal", "Spy", bodyHtml, "Steal Card");
	var selectedTarget = null;

	for (var pid in playersById) {
		if (parseInt(pid) !== playerId) {
			var p = playersById[pid];
			var color = "rgba(" + p.rgbColor.r + "," + p.rgbColor.g + "," + p.rgbColor.b + ",0.4)";
			$("#spy-players-list").append("<label class='btn btn-default' style='background-color: " + color + "'>"
				+ "<input type='radio' name='" + p.id + "' autocomplete='off'>" + p.name + "</label>");
		}
	}

	$("#spy-players-list").find("label").click(function (event) {
		selectedTarget = parseInt($(event.target).find("input").prop("name") || $(event.target).closest("label").find("input").prop("name"));
		$("#spy-modal-confirm").prop("disabled", false);
	});

	$("#spy-modal-confirm").click(function () {
		if (selectedTarget !== null) {
			sendStealProgressCardAction(selectedTarget);
			$modal.modal("hide");
		}
	});

	$modal.on("hidden.bs.modal", function () {
		$modal.remove();
	});

	$modal.modal("show");
}

/**
 * Inventor: enter a mode where the player clicks two hex tiles to swap numbers.
 */
var inventorSelectedHexes = [];

function enterSwapHexNumbersMode() {
	inventorSelectedHexes = [];
	addMessage("Click two hex tiles to swap their number tokens (Inventor). Numbers 2, 6, 8, 12 cannot be swapped.");

	for (var i = 0; i < board.tiles.length; i++) {
		var tile = board.tiles[i];
		// Highlight non-desert tiles with swappable numbers
		if (tile.rollNum && tile.rollNum !== 0
			&& tile.rollNum !== 2 && tile.rollNum !== 6
			&& tile.rollNum !== 8 && tile.rollNum !== 12) {
			tile.highlight();
		}
	}
}

function exitSwapHexNumbersMode() {
	inventorSelectedHexes = [];
	for (var i = 0; i < board.tiles.length; i++) {
		if (board.tiles[i].highlighted) {
			board.tiles[i].unHighlight();
		}
	}
}

/**
 * Resource Monopoly / Trade Monopoly: show a modal to pick a resource.
 * Reusable for both card types via the sendFn parameter.
 */
function enterChooseResourceModal(title, description, sendFn) {
	var resources = ["brick", "wood", "ore", "wheat", "sheep"];
	var circlesHtml = '<p>' + description + '</p><div class="btn-group" data-toggle="buttons" id="res-monopoly-list">';
	for (var i = 0; i < resources.length; i++) {
		circlesHtml += "<label class='btn btn-default' res='" + resources[i] + "'>"
			+ "<input type='radio' autocomplete='off'>" + resources[i].charAt(0).toUpperCase() + resources[i].slice(1) + "</label>";
	}
	circlesHtml += '</div>';

	var $modal = createDynamicModal("res-monopoly-modal", title, circlesHtml, "Confirm");
	var selectedResource = null;

	$("#res-monopoly-list").find("label").click(function (event) {
		selectedResource = $(this).attr("res");
		$("#res-monopoly-modal-confirm").prop("disabled", false);
	});

	$("#res-monopoly-modal-confirm").click(function () {
		if (selectedResource) {
			sendFn(selectedResource);
			$modal.modal("hide");
		}
	});

	$modal.on("hidden.bs.modal", function () {
		$modal.remove();
	});

	$modal.modal("show");
}

/**
 * Master Merchant: show a modal to pick an opponent with more VPs.
 */
function enterChooseOpponentModal(title, description, sendFn) {
	var bodyHtml = '<p>' + description + '</p>'
		+ '<div id="opponent-select-list" class="btn-group" data-toggle="buttons"></div>';

	var $modal = createDynamicModal("opponent-select-modal", title, bodyHtml, "Confirm");
	var selectedTarget = null;

	for (var pid in playersById) {
		if (parseInt(pid) !== playerId) {
			var p = playersById[pid];
			var color = "rgba(" + p.rgbColor.r + "," + p.rgbColor.g + "," + p.rgbColor.b + ",0.4)";
			$("#opponent-select-list").append("<label class='btn btn-default' style='background-color: " + color + "'>"
				+ "<input type='radio' name='" + p.id + "' autocomplete='off'>" + p.name + "</label>");
		}
	}

	$("#opponent-select-list").find("label").click(function (event) {
		selectedTarget = parseInt($(event.target).find("input").prop("name") || $(event.target).closest("label").find("input").prop("name"));
		$("#opponent-select-modal-confirm").prop("disabled", false);
	});

	$("#opponent-select-modal-confirm").click(function () {
		if (selectedTarget !== null) {
			sendFn(selectedTarget);
			$modal.modal("hide");
		}
	});

	$modal.on("hidden.bs.modal", function () {
		$modal.remove();
	});

	$modal.modal("show");
}

/**
 * Deserter: enter a mode where the player clicks an opponent's knight to remove.
 * Similar to displace but for a different card.
 */
var inDeserterMode = false;

function enterDeserterTargetMode() {
	inDeserterMode = true;
	addMessage("Click on an opponent's knight to remove it with Deserter.");

	for (var i = 0; i < board.intersections.length; i++) {
		var inter = board.intersections[i];
		if (inter.knight && inter.knight.owner !== playerId) {
			inter.highlight();
		}
	}
}

function exitDeserterTargetMode() {
	inDeserterMode = false;
	for (var i = 0; i < board.intersections.length; i++) {
		if (board.intersections[i].highlighted) {
			board.intersections[i].unHighlight();
		}
	}
}

/**
 * Merchant: enter a mode where the player clicks a land hex adjacent
 * to one of their buildings to place the merchant pawn.
 */
var inPlaceMerchantMode = false;

function enterPlaceMerchantMode() {
	inPlaceMerchantMode = true;
	addMessage("Click on a land hex adjacent to your building to place the Merchant.");

	for (var i = 0; i < board.tiles.length; i++) {
		var tile = board.tiles[i];
		if (tile.tileType === TILE_TYPE.SEA || tile.tileType === TILE_TYPE.DESERT) {
			continue;
		}
		// Check if this tile is adjacent to any of our buildings
		var isAdjacent = false;
		for (var j = 0; j < board.intersections.length; j++) {
			var inter = board.intersections[j];
			if (inter.building && inter.building.owner === playerId) {
				var coords = inter.coordinates;
				if ((coords.coord1.x === tile.coordinates.x && coords.coord1.y === tile.coordinates.y && coords.coord1.z === tile.coordinates.z) ||
					(coords.coord2.x === tile.coordinates.x && coords.coord2.y === tile.coordinates.y && coords.coord2.z === tile.coordinates.z) ||
					(coords.coord3.x === tile.coordinates.x && coords.coord3.y === tile.coordinates.y && coords.coord3.z === tile.coordinates.z)) {
					isAdjacent = true;
					break;
				}
			}
		}
		if (isAdjacent) {
			// Highlight the tile and add a click handler
			var numberCircle = $("#" + tile.id + "-wrapper .number-circle");
			numberCircle.addClass("number-circle-highlighted");
			tile.highlighted = true;

			(function (t) {
				numberCircle.off("click");
				numberCircle.click(function (event) {
					sendPlaceMerchantAction(t.coordinates);
					exitPlaceMerchantMode();
				});
			})(tile);
		}
	}
}

function exitPlaceMerchantMode() {
	inPlaceMerchantMode = false;
	for (var i = 0; i < board.tiles.length; i++) {
		if (board.tiles[i].highlighted) {
			board.tiles[i].unHighlight();
		}
	}
}

/**
 * Alchemist: show a modal where the player picks red die (1-6) and white die (1-6).
 */
// --- Commodity trade button handler (Trade Level 3) ---
$(document).on("click", ".commodity-trade-btn", function () {
	var toGive = $(this).data("give");
	var toGet = $(this).data("get");
	sendTradeCommodityWithBankAction(toGive, toGet);
});

function enterChooseDiceModal() {
	var bodyHtml = '<p>Choose values for all three dice (1-6 each):</p>'
		+ '<div class="form-group"><label>Red Die:</label>'
		+ '<select id="alchemist-red-die" class="form-control" style="width:80px;display:inline-block;margin-left:8px;">'
		+ '<option value="1">1</option><option value="2">2</option><option value="3">3</option>'
		+ '<option value="4">4</option><option value="5">5</option><option value="6">6</option>'
		+ '</select></div>'
		+ '<div class="form-group"><label>White Die:</label>'
		+ '<select id="alchemist-white-die" class="form-control" style="width:80px;display:inline-block;margin-left:8px;">'
		+ '<option value="1">1</option><option value="2">2</option><option value="3">3</option>'
		+ '<option value="4">4</option><option value="5">5</option><option value="6">6</option>'
		+ '</select></div>'
		+ '<div class="form-group"><label>Event Die:</label>'
		+ '<select id="alchemist-event-die" class="form-control" style="width:120px;display:inline-block;margin-left:8px;">'
		+ '<option value="1">1 (Ship)</option><option value="2">2 (Ship)</option><option value="3">3 (Ship)</option>'
		+ '<option value="4">4 (Trade)</option><option value="5">5 (Politics)</option><option value="6">6 (Science)</option>'
		+ '</select></div>';

	var $modal = createDynamicModal("alchemist-modal", "Alchemist", bodyHtml, "Confirm");
	// Dropdowns always have a valid default value, so enable confirm immediately
	$("#alchemist-modal-confirm").prop("disabled", false);

	$("#alchemist-modal-confirm").click(function () {
		var redDie = parseInt($("#alchemist-red-die").val());
		var whiteDie = parseInt($("#alchemist-white-die").val());
		var eventDie = parseInt($("#alchemist-event-die").val());
		sendChooseDiceAction(redDie, whiteDie, eventDie);
		$modal.modal("hide");
	});

	$modal.on("hidden.bs.modal", function () {
		$modal.remove();
	});

	$modal.modal("show");
}
