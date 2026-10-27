// ---------- Setup ---------- //

//Establish the WebSocket connection and set up event handlers
if (document.location.hostname == "localhost") {
	// use http
	webSocket = new WebSocket("ws://" + location.hostname + ":" + location.port
		+ "/action/");
} else {
	// we're on heroku - use https:
	webSocket = new WebSocket("wss://" + location.hostname + ":" + location.port
		+ "/action/");
}

// Send a heartbeat on the websocket
function heartbeat() {
	var beat = "HEARTBEAT";
	webSocket.send(JSON.stringify(beat));
}

// Requests to be send when websocket first opens
webSocket.onopen = function () {
	if (document.cookie.indexOf("USER_ID") > -1) {
		sendGetGameStateAction();
	}
	window.setInterval(heartbeat, 10 * 1000);
	sendReloadChatRequest();
};

// Delete all cookies and return to home screen
function deleteAllCookiesAndGoHome() {
	deleteCookie("USER_ID");
	deleteCookie("desiredGroupId");
	deleteCookie("groupName");
	deleteCookie("numPlayersDesired");
	window.location = "/home";
}

// Actions to be taken when websocket is closed
webSocket.onclose = function () {
	if (document.cookie.indexOf("USER_ID") > -1) {
		window.location.reload(true);
	} else {
		deleteAllCookiesAndGoHome();
	}

}

// When a leave game button is pressed, return to home screen
$(".leave-game-btn").click(function (event) {
	var exit = {
		requestType: "gameOver",
		reason: "explicitExit"
	};
	webSocket.send(JSON.stringify(exit));
	deleteAllCookiesAndGoHome();
});

// ////////////////////////////////////////
// Action Senders
// ////////////////////////////////////////

function sendGetGameStateAction() {
	var playersReq = {
		requestType: "getGameState"
	};
	webSocket.send(JSON.stringify(playersReq));
}

function sendReloadChatRequest() {
	var chatReq = {
		requestType: "chat",
		logs: true
	}
	webSocket.send(JSON.stringify(chatReq));
}

function sendGetInitialStateAction() {
	var stateReq = {
		requestType: "action",
		action: "getInitialState"
	};
	webSocket.send(JSON.stringify(stateReq));
}

function sendRollDiceAction() {
	var rollDiceReq = {
		requestType: "action",
		action: "rollDice",
		player: playerId
	};
	webSocket.send(JSON.stringify(rollDiceReq));
}

function sendBuildSettlementAction(intersectCoordinates) {
	var buildReq = {
		requestType: "action",
		action: "buildSettlement",
		coordinate: intersectCoordinates
	};
	webSocket.send(JSON.stringify(buildReq));
}

function sendPlaceSettlementAction(intersectCoordinates) {
	var placeReq = {
		requestType: "action",
		action: "placeSettlement",
		coordinate: intersectCoordinates
	};
	webSocket.send(JSON.stringify(placeReq));
}

function sendBuildFactoryAction(intersectCoordinates) {
	var buildReq = {
		requestType: "action",
		action: "buildFactory",
		coordinate: intersectCoordinates
	};
	webSocket.send(JSON.stringify(buildReq));
}

function sendBuildCapitolAction(intersectCoordinates) {
	var buildReq = {
		requestType: "action",
		action: "buildCapitol",
		coordinate: intersectCoordinates
	};
	webSocket.send(JSON.stringify(buildReq));
}

/*
 * Cursed Catan gold: sends the chosen resources, e.g. {wood: 1, coal: 1}.
 */
function sendGoldResourceAction(resources) {
	var req = {
		requestType: "action",
		action: "goldResource",
		resources: resources
	};
	webSocket.send(JSON.stringify(req));
}

function sendBuildFortressAction(tileCoordinates) {
	var buildReq = {
		requestType: "action",
		action: "buildFortress",
		tile: tileCoordinates
	};
	webSocket.send(JSON.stringify(buildReq));
}

function sendBuildTestSiteAction(tileCoordinates) {
	var buildReq = {
		requestType: "action",
		action: "buildTestSite",
		tile: tileCoordinates
	};
	webSocket.send(JSON.stringify(buildReq));
}

/*
 * Cursed Catan factories: one entry per factory, "keep" or the resource to
 * take for 1 coal, e.g. ["keep", "ore"].
 */
function sendFactoryProductionAction(choices) {
	var req = {
		requestType: "action",
		action: "factoryProduction",
		choices: choices
	};
	webSocket.send(JSON.stringify(req));
}

function sendBuildCityAction(intersectCoordinates) {
	var buildReq = {
		requestType: "action",
		action: "buildCity",
		coordinate: intersectCoordinates
	};
	webSocket.send(JSON.stringify(buildReq));
}

function sendBuildRoadAction(start, end) {
	var buildReq = {
		requestType: "action",
		action: "buildRoad",
		start: start,
		end: end
	};
	webSocket.send(JSON.stringify(buildReq));
}

function sendPlaceRoadAction(start, end) {
	var placeReq = {
		requestType: "action",
		action: "placeRoad",
		start: start,
		end: end
	};
	webSocket.send(JSON.stringify(placeReq));
}

function sendBuyDevCardAction() {
	var buyReq = {
		requestType: "action",
		action: "buyDevCard"
	};
	webSocket.send(JSON.stringify(buyReq));
}

function sendPlayKnightAction() {
	var playReq = {
		requestType: "action",
		action: "playKnight"
	};
	webSocket.send(JSON.stringify(playReq));
}

function sendPlayMonopolyAction(resource) {
	var playReq = {
		requestType: "action",
		action: "playMonopoly",
		resource: resource
	};
	webSocket.send(JSON.stringify(playReq));
}

function sendPlayYearOfPlentyAction(resources) {
	var playReq = {
		requestType: "action",
		action: "playYearOfPlenty",
		resources: resources
	};
	webSocket.send(JSON.stringify(playReq));
}

function sendPlayRoadBuildingAction() {
	var playReq = {
		requestType: "action",
		action: "playRoadBuilding"
	};
	webSocket.send(JSON.stringify(playReq));
}

function sendDropCardsAction(toDrop) {
	var dropReq = {
		requestType: "action",
		action: "dropCards",
		toDrop: toDrop
	};
	webSocket.send(JSON.stringify(dropReq));
}

function sendMoveRobberAction(coord) {
	var dropReq = {
		requestType: "action",
		action: "moveRobber",
		newLocation: coord
	};
	// Cursed Catan: optionally spend a Spy Network to ignore fortresses
	if (typeof spyNetworkSelected === "function" && spyNetworkSelected()) {
		dropReq.useSpyNetwork = true;
	}
	webSocket.send(JSON.stringify(dropReq));
}

function sendPlayBorderRaidAction() {
	var playReq = {
		requestType: "action",
		action: "playBorderRaid"
	};
	webSocket.send(JSON.stringify(playReq));
}

function sendBorderRaidRoadAction(start, end) {
	var req = {
		requestType: "action",
		action: "borderRaidTarget",
		type: "road",
		start: start,
		end: end
	};
	webSocket.send(JSON.stringify(req));
}

function sendBorderRaidFortressAction(tileCoord, ownerId) {
	var req = {
		requestType: "action",
		action: "borderRaidTarget",
		type: "fortress",
		tile: tileCoord,
		owner: ownerId
	};
	webSocket.send(JSON.stringify(req));
}

function sendTakeCardAction(playerId) {
	var takeReq = {
		requestType: "action",
		action: "takeCard",
		takeFrom: playerId
	};
	webSocket.send(JSON.stringify(takeReq));
}

function sendTradeWithBankAction(toGive, toGet, amount) {
	var tradeReq = {
		requestType: "action",
		action: "tradeWithBank",
		toGive: toGive,
		toGet: toGet,
		amount: amount
	};
	webSocket.send(JSON.stringify(tradeReq));
}

function startSetupAction() {
	var startReq = {
		requestType: "action",
		action: "startSetup"
	};
	webSocket.send(JSON.stringify(startReq));
}

// ---------- Cities & Knights Action Senders ---------- //

function sendPlaceKnightAction(intersectCoordinates) {
	var req = {
		requestType: "action",
		action: "placeKnight",
		coordinate: intersectCoordinates
	};
	webSocket.send(JSON.stringify(req));
}

function sendActivateKnightAction(intersectCoordinates) {
	var req = {
		requestType: "action",
		action: "activateKnight",
		coordinate: intersectCoordinates
	};
	webSocket.send(JSON.stringify(req));
}

function sendPromoteKnightAction(intersectCoordinates) {
	var req = {
		requestType: "action",
		action: "promoteKnight",
		coordinate: intersectCoordinates
	};
	webSocket.send(JSON.stringify(req));
}

function sendBuildCityWallAction(intersectCoordinates) {
	var req = {
		requestType: "action",
		action: "buildCityWall",
		coordinate: intersectCoordinates
	};
	webSocket.send(JSON.stringify(req));
}

function sendImproveCityTrackAction(track) {
	var req = {
		requestType: "action",
		action: "improveCityTrack",
		track: track
	};
	webSocket.send(JSON.stringify(req));
}

function sendPlayProgressCardAction(cardName) {
	var req = {
		requestType: "action",
		action: "playProgressCard",
		card: cardName
	};
	webSocket.send(JSON.stringify(req));
}

function sendEndTurnAction() {
	var endReq = {
		requestType: "action",
		action: "endTurn"
	};
	webSocket.send(JSON.stringify(endReq));
}

function sendKnightOrDiceAction(choseKnight) {
	var playReq = {
		requestType: "action",
		action: "knightOrDice",
		choseKnight: choseKnight
	};
	webSocket.send(JSON.stringify(playReq));
}

// --- Progress Card Follow-Up Action Senders --- //

function sendRemoveRoadAction(start, end) {
	var req = {
		requestType: "action",
		action: "removeRoad",
		start: start,
		end: end
	};
	webSocket.send(JSON.stringify(req));
}

function sendDisplaceKnightAction(coordinate) {
	var req = {
		requestType: "action",
		action: "displaceKnight",
		coordinate: coordinate
	};
	webSocket.send(JSON.stringify(req));
}
function sendMoveKnightAction(from, to) {
	var req = {
		requestType: "action",
		action: "moveKnight",
		from: from,
		to: to
	};
	webSocket.send(JSON.stringify(req));
}
function sendRelocateKnightAction(coordinate) {
	var req = {
		requestType: "action",
		action: "relocateKnight",
		coordinate: coordinate
	};
	webSocket.send(JSON.stringify(req));
}
 

function sendStealProgressCardAction(targetPlayer) {
	var req = {
		requestType: "action",
		action: "stealProgressCard",
		targetPlayer: targetPlayer
	};
	webSocket.send(JSON.stringify(req));
}

function sendSwapHexNumbersAction(hex1, hex2) {
	var req = {
		requestType: "action",
		action: "swapHexNumbers",
		hex1: hex1,
		hex2: hex2
	};
	webSocket.send(JSON.stringify(req));
}

function sendTradeCommodityWithBankAction(toGive, toGet) {
	var req = {
		requestType: "action",
		action: "tradeCommodityWithBank",
		toGive: toGive,
		toGet: toGet
	};
	webSocket.send(JSON.stringify(req));
}

function sendCommercialHarborAction() {
	var req = {
		requestType: "action",
		action: "commercialHarbor"
	};
	webSocket.send(JSON.stringify(req));
}

function sendAqueductResourceAction(resource) {
	var req = {
		requestType: "action",
		action: "aqueductResource",
		resource: resource
	};
	webSocket.send(JSON.stringify(req));
}

function sendChooseResourceAction(resource) {
	var req = {
		requestType: "action",
		action: "chooseResource",
		resource: resource
	};
	webSocket.send(JSON.stringify(req));
}

function sendChooseCommodityAction(resource) {
	var req = {
		requestType: "action",
		action: "chooseCommodity",
		resource: resource
	};
	webSocket.send(JSON.stringify(req));
}

function sendChooseOpponentCardsAction(targetPlayer) {
	var req = {
		requestType: "action",
		action: "chooseOpponentCards",
		targetPlayer: targetPlayer
	};
	webSocket.send(JSON.stringify(req));
}

function sendDeserterTargetAction(targetPlayer, coordinate) {
	var req = {
		requestType: "action",
		action: "deserterTarget",
		targetPlayer: targetPlayer,
		coordinate: coordinate
	};
	webSocket.send(JSON.stringify(req));
}

function sendPlaceMerchantAction(hex) {
	var req = {
		requestType: "action",
		action: "placeMerchant",
		hex: hex
	};
	webSocket.send(JSON.stringify(req));
}

function sendChooseDiceAction(redDie, whiteDie, eventDie) {
	var req = {
		requestType: "action",
		action: "chooseDice",
		redDie: redDie,
		whiteDie: whiteDie,
		eventDie: eventDie
	};
	webSocket.send(JSON.stringify(req));
}

function sendProposeTradeAction(trade) {
	var tradeReq = {
		requestType: "action",
		action: "proposeTrade",
		trade: trade
	};
	webSocket.send(JSON.stringify(tradeReq));
}

function sendReviewTradeAction(tradeAccepted) {
	var tradeReq = {
		requestType: "action",
		action: "reviewTrade",
		tradeAccepted: tradeAccepted
	};
	webSocket.send(JSON.stringify(tradeReq));
}

function sendTradeResponseAction(accepted, trader, tradee) {
	var tradeReq = {
		requestType: "action",
		action: "tradeResponse",
		tradeAccepted: accepted,
		trader: trader,
		tradee: tradee
	};
	webSocket.send(JSON.stringify(tradeReq));
}

function sendUpdateResourceAction() {
	var updateReq = {
		requestType: "action",
		action: "updateResource"
	};
	webSocket.send(JSON.stringify(updateReq));
}

// ---------- RESPONSES ---------- //

webSocket.onmessage = function (msg) {
	console.log(msg);
	var data = JSON.parse(msg.data);
	console.log(data);

	if (data.hasOwnProperty("requestType")) {
		switch (data.requestType) {
			case "chat":
				handleChatResponse(data);
				break;
			case "getGameState":
				handleGetGameState(data);
				break;
			case "action":
				handleActionResponse(data);
				break;
			case "setCookie":
				handleSetCookie(data);
				break;
			case "ERROR":
				handleErrorFromSocket(data);
				break;
			case "disconnectedUsers":
				handleDisconnectedUsers(data);
				break;
			case "gameOver":
				handleGameOver(data);
				break;
			case "heartbeat":
				// console.log("heartbeat acknowledged");
				break;
			default:
				console.log("unsupported request type");
				break;
		}
	} else {
		console.log("No request type indicated for response");
	}
};

// ////////////////////////////////////////
// Chat Responses
// ////////////////////////////////////////

var chatReceivedSound = new Audio("audio/message-received.mp3");

// Send a message if it's not empty, then clear the input field
function sendMessage(message) {
	if (message !== "") {
		var pack = {
			"requestType": "chat",
			"message": message
		};
		webSocket.send(JSON.stringify(pack));
		id("message").value = "";
	}
}

/*
 * Create and insert a message into chat.
 * @param msg - the message to insert
 */
function insertChatMessage(msg) {
	var formattedDate = moment(msg.timeStamp).format("LT");
	var fromPlayer = playersById[msg.userId];
	insert("chat", "<div style='border-left-color: " + fromPlayer.color
		+ "'><article><b>" + msg.sender + ":</b><p class='msg-content'>"
		+ "</p><span class='timestamp'>" + formattedDate + "</span></article></div>");
	$(".msg-content").text(msg.content);
	$(".msg-content").removeClass("msg-content");
	$("#chat").scrollTop($("#chat")[0].scrollHeight);
}

/*
 * Update the chat with the given message.
 * @param msg - the msg that was received
 */
function updateChat(msg) {
	if (msg.hasOwnProperty('ERROR')) {
		alert(msg.ERROR);
	} else {
		insertChatMessage(msg);
		if (!muted) {
			chatReceivedSound.play();
		}
	}
}

/*
 * Load chat logs, displaying them in chat window.
 * @param logs - an array of chat messages to display
 */
function loadChatLogs(logs) {
	for (var i = 0; i < logs.length; i++) {
		insertChatMessage(logs[i]);
	}
}

// ////////////////////////////////////////
// Action Handlers
// ////////////////////////////////////////

/*
 * Handles a chat response.
 * @param data - the chat data
 */
function handleChatResponse(data) {
	if (data.hasOwnProperty("logs")) {
		loadChatLogs(data.logs);
	} else {
		updateChat(data);
	}
}

/*
 * Handles an action that was send back to the gui.
 * @param data - the action data
 */
function handleActionResponse(data) {
	if (data.content.hasOwnProperty("message")) {
		addMessage(data.content.message);
	}

	// Check for dice data (C&K or Base game)
	if (data.content.hasOwnProperty("data") && data.content.data != null) {
		if (data.content.data.hasOwnProperty("redDie")) {
			// It's a C&K roll or we updated base game to send this
			updateDiceDisplay(data.content.data.redDie, data.content.data.whiteDie, data.content.data.eventDie);
		} else if (data.content.data.hasOwnProperty("diceRoll")) {
			// Fallback for base game if we didn't send separate dice? 
			// Actually base game RollDice doesn't send data yet, just message. 
			// But my RollDice refactor sends red/white for everyone.
		}
	}

	switch (data.action) {
		case "startGame":
			showStartGameDialogue(data.content);
			break
		case "tradeResponse":
			exitReviewTradeModal();
			break;
		case "reviewTrade":
			handleReviewTradeAction(data.content.data.allDeclined);
			break;
		default:
			break;
	}
}

/*
 * Handle a game over send from the websocket.
 * @param data - the game over data
 */
function handleGameOver(data) {
	switch (data.reason) {
		case "disconnectedUser":
			deleteAllCookiesAndGoHome();
			break;
		case "explicitExit":
			$("#user-exited-name").text(data.departedUser.userName);
			$("#user-exited-modal").modal("show");
			break;
		default:
			console.log("UNSUPPORTED REASON FOR GAME OVER");
	}

}

$("#user-exited-go-home-btn").click(deleteAllCookiesAndGoHome);

/*
 * Handle a followup action.
 * @param action - the followup action.
 */
function handleFollowUp(action) {
	if (action.hasOwnProperty("actionData")
		&& action.actionData.hasOwnProperty("message")) {
		addMessage(action.actionData.message);
	}

	switch (action.actionName) {
		case "moveRobber":
			highlightRobbableTiles();
			break
		case "dropCards":
			enterDiscardModal(action.actionData.numToDrop);
			break;
		case "takeCard":
			enterTakeCardModal(action.actionData.toTake);
			break;
		case "placeRoad":
			enterPlaceRoadMode();
			break;
		case "placeSettlement":
			enterPlaceSettlementMode();
			break;
		case "rollDice":
			showRollDiceModal();
			break;
		case "knightOrDice":
			showKnightOrDiceModal();
			break;
		case "reviewTrade":
			showReviewTradeModal(action.actionData.trade);
			break;
		case "tradeResponse":
			showTradeResponseModal(action.actionData.trade);
			break;
		case "removeRoad":
			enterRemoveRoadMode();
			break;
		case "displaceKnight":
			enterDisplaceKnightMode();
			break;
					case "relocateKnight":
			enterRelocateKnightMode(action.actionData.options);
			break;

		case "stealProgressCard":
			enterStealProgressCardModal();
			break;
		case "swapHexNumbers":
			enterSwapHexNumbersMode();
			break;
		case "chooseResource":
			enterChooseResourceModal("Resource Monopoly", "Choose a resource. Each opponent gives you up to 2.", sendChooseResourceAction);
			break;
		case "chooseCommodity":
			enterChooseResourceModal("Trade Monopoly", "Choose a resource. Each opponent gives you 1.", sendChooseCommodityAction);
			break;
		case "chooseOpponentCards":
			enterChooseOpponentModal("Master Merchant", "Select a player with more VPs to take 2 cards from.", sendChooseOpponentCardsAction);
			break;
		case "deserterTarget":
			enterDeserterTargetMode();
			break;
		case "placeMerchant":
			enterPlaceMerchantMode();
			break;
		case "chooseDice":
			enterChooseDiceModal();
			break;
		case "aqueductResource":
			enterChooseResourceModal("Aqueduct", "You produced nothing. Choose 1 free resource.", sendAqueductResourceAction);
			break;
		case "commercialHarbor":
			sendCommercialHarborAction();
			break;
		case "goldResource":
			enterGoldModal(action.actionData.count);
			break;
		case "borderRaidTarget":
			enterBorderRaidMode();
			break;
		case "factoryProduction":
			enterFactoryModal(action.actionData.units, action.actionData.maxCoal);
			break;
		default:
			break;
	}
}

/*
 * Handles a get games state response. Redraws the board and resets all internal data.
 * @param gameStateData - the game state data
 */
var cursedBoardPositioned = false;

function handleGetGameState(gameStateData) {
	console.log("=== GAME STATE DEBUG ===");
	console.log("settings:", JSON.stringify(gameStateData.settings));
	console.log("isCitiesAndKnights:", gameStateData.settings ? gameStateData.settings.isCitiesAndKnights : "NO SETTINGS");
	console.log("barbarianTrack:", JSON.stringify(gameStateData.barbarianTrack));
	console.log("hand:", JSON.stringify(gameStateData.hand));
	// Set global data
	playerId = gameStateData.playerID;
	currentPlayerTurn = gameStateData.currentTurn;
	gameSettings = gameStateData.settings;
	tradeRates = gameStateData.players[playerId].rates;
	gameStats = gameStateData.stats;

	var activePlayerTab = $("#player-tabs .active").attr("player");
	openedPlayerTab = (activePlayerTab == undefined) ? 0
		: parseInt(activePlayerTab);

	// Create players
	playersById = {};
	players = parsePlayers(gameStateData.players);
	for (var i = 0; i < players.length; i++) {
		playersById[players[i].id] = players[i];
	}

	// Parse and draw hand
	fillPlayerHand(gameStateData.hand);

	// Create player tabs and turn counter
	$("#player-tabs").empty();
	$("#player-tabs-content").empty();
	$("#turn-display-container").empty();

	for (var i = 0; i < players.length; i++) {
		players[i].addPlayerTab();
	}

	// Draw turn counter
	if (gameStateData.hasOwnProperty("turnOrder")) {
		var turnOrder = gameStateData.turnOrder;
		for (var i = 0; i < turnOrder.length; i++) {
			playersById[turnOrder[i]].fillTurnDisplay();
		}
	}

	if (currentPlayerTurn === playerId) {
		$("#end-turn-btn").prop("disabled", false);
	} else {
		$("#end-turn-btn").prop("disabled", true);
	}

	// Show what buildings player can currently buy
	fillPlayerBuyOptions(gameStateData.hand);

	// Draw trade rates
	fillPlayerTradeRates(tradeRates);

	// Handle decimal trade rates
	setDecimalTradeRates(gameSettings.isDecimal);

	// Cursed Catan: the March of Industry
	industryTrack = gameStateData.industryTrack || 0;
	longestTurnPlayerId = (gameStateData.longestTurnPlayer !== undefined) ? gameStateData.longestTurnPlayer : -1;
	longestTurnSeconds = gameStateData.longestTurnSeconds || 0;
	greatWarTriggered = !!gameStateData.greatWarTriggered;
	updateUraniumTradeVisibility();

	// Build current extras tab
	buildExtrasTab();

	// Create board. Cursed Catan's bigger island is positioned on first view.
	if (gameSettings.isCursedCatan && !cursedBoardPositioned) {
		positionCursedBoard(gameStateData.board.tiles);
		cursedBoardPositioned = true;
	}
	board = new Board();
	board.createBoard(gameStateData.board);
	board.draw();

	// C&K: Update Barbarian Track with animation
	if (gameStateData.barbarianTrack && gameSettings.isCitiesAndKnights) {
		var pos = gameStateData.barbarianTrack.position;
		$("#barbarian-track-container").removeClass("hidden");

		var $ship = $("#barbarian-ship-token");
		var $targetStep = $("#track-step-" + pos);

		if ($targetStep.length && $ship.length) {
			// Get current and target positions for animation
			var currentOffset = $ship.offset();
			var targetOffset = $targetStep.offset();

			if (currentOffset && targetOffset && currentOffset.left !== targetOffset.left) {
				// Animate the ship sliding to the new position
				var dx = targetOffset.left - currentOffset.left;
				var dy = targetOffset.top - currentOffset.top;
				$ship.css({
					"position": "relative",
					"transition": "transform 0.6s ease-in-out"
				});
				$ship.css("transform", "translate(" + dx + "px, " + dy + "px)");
				setTimeout(function () {
					$ship.css({ "transition": "none", "transform": "none", "position": "" });
					$(".track-step").removeClass("active");
					$("#track-step-" + pos).addClass("active");
					$targetStep.append($ship);
				}, 650);
			} else {
				$(".track-step").removeClass("active");
				$("#track-step-" + pos).addClass("active");
				$targetStep.append($ship);
			}
		}
	}

	// Mark merchant tile if present
	if (gameStateData.merchantHex && gameStateData.merchantOwner !== undefined) {
		var mHex = gameStateData.merchantHex;
		for (var i = 0; i < board.tiles.length; i++) {
			var t = board.tiles[i];
			if (t.coordinates.x === mHex.x && t.coordinates.y === mHex.y && t.coordinates.z === mHex.z) {
				t.hasMerchant = true;
				t.merchantOwner = gameStateData.merchantOwner;
				t.draw(board.transX, board.transY, board.scaleFactor);
				break;
			}
		}
	}

	// C&K: Show active card effect banners
	$("#active-effects-bar").remove();
	if (gameSettings.isCitiesAndKnights && currentPlayerTurn === playerId) {
		var effects = [];
		if (gameStateData.medicineActive) {
			effects.push("<span class='label label-info' style='margin-right:4px;'>Medicine: City costs 1 Ore + 1 Wheat</span>");
		}
		if (gameStateData.craneActive) {
			effects.push("<span class='label label-success' style='margin-right:4px;'>Crane: Improvement -1 commodity</span>");
		}
		if (gameStateData.merchantFleetActive) {
			effects.push("<span class='label label-warning' style='margin-right:4px;'>Merchant Fleet: All trades 2:1</span>");
		}
		if (effects.length > 0) {
			var bar = $("<div id='active-effects-bar' style='text-align:center;padding:4px;background:#f5f5f5;border-bottom:1px solid #ddd;'>" + effects.join("") + "</div>");
			$("#turn-display-container").after(bar);
		}
	}

	// C&K: Show commodity trade tab if player has Trade level >= 3 OR Merchant Fleet active
	if (gameSettings.isCitiesAndKnights) {
		var myPlayer = playersById[playerId];
		var hasTrade3 = myPlayer && myPlayer.cityImprovements && myPlayer.cityImprovements.trade >= 3;
		var hasMerchantFleet = gameStateData.merchantFleetActive;
		if (hasTrade3 || hasMerchantFleet) {
			$("#commodity-trade-tab-toggle").show();
		} else {
			$("#commodity-trade-tab-toggle").hide();
		}
	}

	// If in place road mode, enter build rode mode
	if (inPlaceRoadMode) {
		enterPlaceRoadMode();
	}

	// If in place settlment mode, enter place settlement mode
	if (inPlaceSettlementMode) {
		enterPlaceSettlementMode();
	}

	// Handle win game
	if (gameStateData.hasOwnProperty("winner")) {
		var winner = gameStateData.winner;
		showWinnerModal(winner, gameStateData.winReason, gameStateData.drawReason);
	} else if (gameStateData.draw) {
		// Cursed Catan: a draw that the tie-break could not settle
		showDrawModal(gameStateData.drawReason);
	}

	// Handle follow up action
	if (gameStateData.hasOwnProperty("followUp")) {
		handleFollowUp(gameStateData.followUp);
	}
}

// Send message if enter is pressed in the input field
id("message").addEventListener("keypress", function (e) {
	if (e.keyCode === 13) {
		sendMessage(e.target.value);
	}
});

/*
 * Handle a disconnected user message.
 * @param disconnectData - the disconnected user data
 */
function handleDisconnectedUsers(disconnectData) {
	if (disconnectData.users.length === 0) {
		hideDisconnectedUsersModal();
	} else {
		showDisconnectedUsersModal(disconnectData);
	}
}

// ---------- SET COOKIE FROM SERVER ---------- //

function handleSetCookie(data) {
	console.log(data);
	for (i = 0; i < data.cookies.length; i++) {
		if (data.cookies[i].name == "USER_ID") {
			var cook = data.cookies[i];
			setCookie(cook.name, cook.value);
			console.log("Cookies set to :" + document.cookie);
			sendGetGameStateAction();
		}
	}
}

// ---------- ERRORS ---------- //

function handleErrorFromSocket(data) {
	if (data.hasOwnProperty("description")) {
		switch (data.description) {
			case "RESET":
				deleteAllCookiesAndGoHome();
				break;
			case "NOT_REGISTERED":
				alert("Internal error : user not registered");
				break;
			case "FULL_GAME":
				$("#full-game-modal").modal("show");
				break;
			case "DUPLICATE_TAB":
				$("#duplicate-tab-modal").modal("show");
				break;
			default:
				console.log(data.description);
		}
	}
}

$("#accept-full-game-btn").click(function (event) {
	$("#full-game-modal").modal("hide");
	deleteAllCookiesAndGoHome();
});

// ---------- COOKIE MANAGEMENT ---------- //

function getCookie(name) {
	var nameEQ = name + "=";
	var ca = document.cookie.split(';');
	for (var i = 0; i < ca.length; i++) {
		var c = ca[i];
		while (c.charAt(0) == ' ')
			c = c.substring(1);
		if (c.indexOf(nameEQ) != -1) {
			return c.substring(nameEQ.length, c.length);
		}
	}
	return null;
}

function setCookie(cookie, value) {
	var eqVal = cookie + "=" + value;
	document.cookie = eqVal;
}

function deleteCookie(name) {
	document.cookie = name + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
};

// Helper function for inserting HTML as the first child of an element
function insert(targetId, message) {
	id(targetId).insertAdjacentHTML("beforeend", message);
}

// Helper function for selecting element by id
function id(id) {
	return document.getElementById(id);
}

