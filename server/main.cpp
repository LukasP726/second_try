#include <iostream>
#include <string>
#include <vector>
#include <algorithm>
#include <map>
#include <ctime>
#include <cstdlib>
#include <thread> // Pro práci s vlákny
#include <random>
#include <sstream>
#include <iomanip>
#include <mutex>
#include <shared_mutex>
#include <unordered_map>
#include <regex>



#include <unistd.h>
#include <cstring>
// Zahrnutí knihoven podle operačního systému
#ifdef _WIN32
#include <winsock2.h>
#include <ws2tcpip.h>
#pragma comment(lib, "Ws2_32.lib") // Připojení knihovny pro Windows
#else
#include <unistd.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/socket.h>


#endif

// Makra pro kompatibilitu funkcí

#define closeSocket close



//std::shared_mutex gameStateMutex;
int commandPort = 8080; // Port pro příkazy
std::string ipv4Address = "192.168.1.1 ";//192.168.1.1 127.0.0.1
// Zámek pro synchronizaci přístupu k socketu
std::mutex pingMutex;
std::mutex socketMutex;  // Globální mutex pro synchronizaci přístupu k socketu



// Struktura, která obsahuje karty a skóre hráče
struct PlayerState {
    int socket;
    std::vector<std::string> playerCards;  // Karty hráče
    int playerScore = 0;                   // Skóre hráče
    bool isStanding = false;               // Indikátor, zda hráč stojí
    std::chrono::time_point<std::chrono::steady_clock> lastPingTime; // Nový atribut pro čas posledního pingu
    std::string inRoomId;
    bool isActive = true;
};

struct Room {
    std::string roomId; // ID místnosti
    std::map<std::string, PlayerState> players; // Hráči v místnosti
    //std::map<std::string, PlayerState> playersPlaying; // Hráči v místnosti
    bool isWaitingForPlayers = true; // Indikátor, zda čekáme na další hráče
    int maxPlayers; // Maximální počet hráčů v místnosti
    bool isStanding = false;
    std::string currentPlayerId; // ID aktuálního hráče

    std::vector<std::string> dealerCards;           // Karty dealera
    int dealerScore = 0;                            // Skóre dealera

    //Room(const std::string& id, int max) : roomId(id), maxPlayers(max) {}
};

// Struktura pro stav celé hry, zahrnující dealera
struct GameState {
    //std::vector<std::string> dealerCards;           // Karty dealera
    //int dealerScore = 0;                            // Skóre dealera
    //std::map<int, PlayerState> players;
    std::map<std::string, PlayerState> players;     // Mapa hráčů: <hráčské ID, stav hráče>
    std::unordered_map<std::string, std::atomic<bool>> disconnectSignals; // Signalizace odpojení
    std::map<std::string, Room> rooms;              // Mapa místností
    std::mutex gameStateMutex;
};



// Generování unikátního ID pro hráče
std::string generateUUID() {
    std::random_device rd; // Získání náhodného zařízení
    std::mt19937 gen(rd()); // Inicializace generátoru
    std::uniform_int_distribution<> dis(0, 15); // Rozsah 0-15

    std::ostringstream oss;

    for (int i = 0; i < 32; ++i) {
        if (i == 8 || i == 13 || i == 18 || i == 23) {
            oss << "-"; // Přidání pomlčky na správná místa
        }
        int randomHex = dis(gen); // Generování náhodného hex čísla
        oss << std::hex << std::setw(1) << std::setfill('0') << randomHex; // Přidání hex čísla do stringu
    }

    return oss.str();
}

std::string addPlayer(int socket, GameState &gameState) {
    std::string playerId = generateUUID(); // Generování unikátního ID hráče
    PlayerState playerState;
    playerState.socket = socket;
    playerState.lastPingTime = std::chrono::steady_clock::now();

    {
        std::lock_guard<std::mutex> lock(gameState.gameStateMutex); // Zámek pro bezpečnou práci s herním stavem
        gameState.players[playerId] = playerState; // Přidání hráče do herního stavu
    }

    std::cout << "New player added with ID: " << playerId << " and socket: " << socket << std::endl;
    return playerId;
}




// Funkce pro zpracování připojení hráče
std::string handlePlayerConnection(int socket, const std::string &receivedId, GameState &gameState) {
    if (!receivedId.empty()) {
        std::lock_guard<std::mutex> lock(gameState.gameStateMutex);
        std::string turn_flag="";
        // Pokud ID již existuje, aktualizujeme stav
        if (gameState.players.find(receivedId) != gameState.players.end()) {
            std::cout << "Player reconnected with ID: " << receivedId <<" and socket: "<< socket <<std::endl;

            gameState.players[receivedId].socket = socket;
            gameState.players[receivedId].lastPingTime = std::chrono::steady_clock::now();
            gameState.players[receivedId].isActive = true; // Nastavíme hráče jako aktivního
            std::string response = "RECONNECT|DEALER_CARDS|";

            std::string roomId = gameState.players[receivedId].inRoomId;
            auto roomIt = gameState.rooms.find(roomId); // Najde místnost podle roomId
            if (roomIt != gameState.rooms.end()) {
                Room &room = roomIt->second;
                room.players[receivedId] = gameState.players[receivedId];
                for (auto &dealerCards : room.dealerCards) {
                    response += dealerCards + "|";
                }
                response+= "DEALER_SCORE|"+std::to_string(room.dealerScore);

                std::string this_player="|PLAYER_ID|"+receivedId+"|PLAYER_CARDS|";
                for(auto const &playerCard : gameState.players[receivedId].playerCards) {
                    this_player +=  playerCard + "|";
                }
                this_player +="PLAYER_SCORE|"+std::to_string(gameState.players[receivedId].playerScore);


                std::string rest="";//"|PLAYER_ID|"+player.first+
                for (auto &player : room.players) {
                    if(receivedId != player.first) {
                        rest += "|PLAYER_ID|"+player.first+"|PLAYER_CARDS|";
                        for(auto const &playerCard : player.second.playerCards) {
                            rest +=  playerCard + "|";
                        }
                        rest += "PLAYER_SCORE|"+std::to_string(player.second.playerScore);
                    }
                    if(room.currentPlayerId == receivedId) {
                        turn_flag = "YOUR_TURN\n";
                    }
                }
                response += this_player + rest;

            }

            response+="\n";

            if(!gameState.players[receivedId].inRoomId.empty()) {
                send(socket, response.c_str(), response.size(), 0);
                if(!turn_flag.empty()) {
                    send(socket, turn_flag.c_str(), turn_flag.size(), 0);
                }

            }

            //gameState.players[receivedId].inRoomId;
            return receivedId;


        }

        // Pokud ID neexistuje, vytvoříme nového hráče
        PlayerState playerState;
        playerState.socket = socket;
        playerState.lastPingTime = std::chrono::steady_clock::now();
        gameState.players[receivedId] = playerState;

        std::cout << "Player connected with new ID: " << receivedId << " on socket: "<<socket<<std::endl;
        return receivedId;
    }

    // Pokud klient neposkytne ID, vygenerujeme nové
    return addPlayer(socket, gameState);
}






// Funkce pro oříznutí bílých znaků
std::string trim(const std::string& str) {
    size_t first = str.find_first_not_of(" \n\r\t");
    if (first == std::string::npos) return ""; // Pokud je řetězec prázdný
    size_t last = str.find_last_not_of(" \n\r\t");
    return str.substr(first, (last - first + 1));
}

// Pomocné funkce pro práci s kartami a skóre
std::string getRandomCard() {
    // Seznam karet v balíčku
    std::vector<std::string> deck = {
        "2H", "3H", "4H", "5H", "6H", "7H", "8H", "9H", "10H", "JH", "QH", "KH", "AH", // Srdce
        "2D", "3D", "4D", "5D", "6D", "7D", "8D", "9D", "10D", "JD", "QD", "KD", "AD", // Káry
        "2S", "3S", "4S", "5S", "6S", "7S", "8S", "9S", "10S", "JS", "QS", "KS", "AS", // Piky
        "2C", "3C", "4C", "5C", "6C", "7C", "8C", "9C", "10C", "JC", "QC", "KC", "AC"  // Kříže
    };

    // Použití std::random_device pro generování kvalitního náhodného čísla
    std::random_device rd;
    std::mt19937 gen(rd()); // Mersenne Twister engine pro náhodná čísla
    std::uniform_int_distribution<> dis(0, deck.size() - 1); // Rozdělení pro indexy balíčku

    return deck[dis(gen)]; // Náhodně vyber kartu z balíčku
}

int getCardValue(const std::string &card) {
    if (card[0] == 'A') return 11; // Eso za 11 bodů
    if (card[0] == 'K' || card[0] == 'Q' || card[0] == 'J') return 10; // K, Q, J za 10 bodů
    return std::stoi(card.substr(0, card.size() - 1)); // Ostatní karty
}

// Funkce pro vyhodnocení výsledků pro hráče a dealera
std::string evaluateGame(const PlayerState &player, int dealerScore) {
    if (player.playerScore > 21) return "PLAYER_BUST";           // Hráč přetáhl 21
    if (dealerScore > 21) return "PLAYER_WINS";                  // Dealer přetáhl 21
    if (player.playerScore == dealerScore) return "DRAW";        // Remíza
    if (player.playerScore > dealerScore) return "PLAYER_WINS";  // Hráč má vyšší skóre
    return "DEALER_WINS";                                        // Dealer má vyšší skóre
}

std::string joinRoom(const std::string &playerId, const std::string &roomId, GameState &gameState) {
    // Nejprve zkontrolujeme, zda je hráč již připojen k nějaké místnosti
    for (auto &roomEntry : gameState.rooms) {
        Room &room = roomEntry.second;
        auto it = room.players.find(playerId);
        if (it != room.players.end()) {
            // Hráč je již v nějaké místnosti, tedy jej musíme odebrat
            std::cout << "Player " << playerId << " is already in room " << room.roomId << ", removing from that room." << std::endl;
            room.players.erase(it);  // Odebereme hráče z místnosti
            if (room.players.empty()) {
                room.isWaitingForPlayers = true;  // Pokud je místnost prázdná, nastavíme ji zpět na čekání na hráče
                std::cout << "Room " << room.roomId << " is now waiting for players." << std::endl;
            }
            break;  // Jakmile najdeme a odebereme hráče, přestaneme hledat
        }
    }

    // Pokračujeme s připojením hráče do nové místnosti
    auto roomIt = gameState.rooms.find(roomId); // Najde místnost podle roomId

    if (roomIt != gameState.rooms.end()) {
        Room &room = roomIt->second;

        // Pokud je místnost stále ve stavu čekání na hráče
        if (room.isWaitingForPlayers) {
            room.players[playerId] =gameState.players[playerId]; //PlayerState(); // Přidání hráče do místnosti
            room.currentPlayerId = playerId;
            gameState.players[playerId].inRoomId = room.roomId;

            std::cout << "Player " << playerId << " joined room " << room.roomId << std::endl;

            // Zkontrolujeme, zda máme dost hráčů pro zahájení hry
            if (room.players.size() == room.maxPlayers) {
                room.isWaitingForPlayers = false; // Zahájit hru
                std::cout << "Game starting in room " << room.roomId << std::endl;
                std::string response = "GAME_START|PLAYER_ID|"+playerId;

                // Pošleme zprávu všem hráčům v místnosti, že hra začíná
                for (const auto &player : room.players){
                    if(player.first != playerId) {
                        response+="|PLAYER_ID|"+player.first;
                    }
                }
                //response + "\n"
                //std::string broadcast = "";
                for (const auto &player : room.players) {
                    std::string broadcast="GAME_START|PLAYER_ID|"+player.first;
                    for (const auto &y : room.players) {
                        // Vynecháme hráče, který právě připojil a spustil tuto logiku
                        if (player.first != y.first) {
                            broadcast+="|PLAYER_ID|"+y.first;
                        }

                    }
                    broadcast+="\n";
                    if(player.first != playerId) {
                        send(player.second.socket, broadcast.c_str(), broadcast.size(), 0);
                    }

                }


                // Odeslat i hráči, který spustil tuto funkci
                return response; // Odeslat hráči zprávu, že hra začíná
            }
            return "WAITING_FOR_OPPONENT"; // Čekáme na dalšího hráče
        } else {
            return "ROOM_FULL"; // Místnost je již plná nebo hra už začala
        }
    } else {
        return "ROOM_NOT_FOUND"; // Místnost nebyla nalezena
    }
}

void resetGame(Room &room) {
    room.dealerCards.clear();
    room.dealerScore = 0;
    for (auto &player : room.players) {
        player.second.playerCards.clear();
        player.second.playerScore = 0;
        player.second.isStanding = false;
        player.second.inRoomId="";
    }
}


std::string leaveRoom(const std::string &playerId, const std::string &roomId, GameState &gameState) {
    for (auto &roomEntry : gameState.rooms) {
        Room &room = roomEntry.second;
        if (room.roomId == roomId) {
            // Zkontrolujeme, zda hráč existuje v místnosti
            auto it = room.players.find(playerId);
            if (it != room.players.end()) {
                resetGame(room);
                room.players.erase(it);  // Odebereme hráče z místnosti
                gameState.players[playerId].inRoomId.erase();
                gameState.players[playerId].playerScore = 0;
                gameState.players[playerId].playerCards.clear();
                //gameState.players.erase(it);
                std::cout << "Player " << playerId << " left room " << room.roomId << std::endl;

                // Zkontrolujeme, zda místnost zůstává prázdná
                if (room.players.empty()) {
                    room.isWaitingForPlayers = true;  // Nastavíme místnost zpět na stav "čeká na hráče"
                    std::cout << "Room " << room.roomId << " is now waiting for players." << std::endl;
                }
                return "PLAYER_LEFT";  // Odpovědní zpráva
            } else {
                return "PLAYER_NOT_FOUND";  // Hráč není v místnosti
            }
        }
    }

    return "ROOM_NOT_FOUND";  // Místnost nenalezena
}



void checkForInactivePlayers(GameState &state) {
    auto now = std::chrono::steady_clock::now();
    for (auto it = state.players.begin(); it != state.players.end(); ) {
        PlayerState &playerState = it->second;
        // Nastavíme timeout na 60 sekund
        auto inactiveDuration = std::chrono::seconds(60);

        if (now - playerState.lastPingTime > inactiveDuration) {
            std::cout << "Player " << it->first << " is inactive and will be removed." << std::endl;
            closeSocket(playerState.socket);
            std::cout << "Closing socket: " << playerState.socket << std::endl;
            it = state.players.erase(it); // Odstraní neaktivního hráče
        } else {
            ++it;
        }
    }
}

Room* getRoomForPlayer(const std::string& clientId, GameState &state) {
    for (auto& [roomId, room] : state.rooms) {
        if (room.players.find(clientId) != room.players.end()) {
            return &room;
        }
    }
    return nullptr; // Hráč není v žádné místnosti
}


std::string getAllRooms(GameState &state) {
    std::string response = "ROOMS"; // Začátek odpovědi
    for (const auto &roomPair : state.rooms) {
        const Room &room = roomPair.second;
        // Přidání informací o místnosti do odpovědi
        response += "|" + room.roomId + "|" + (room.isWaitingForPlayers ? "waiting" : "full") + "|" + std::to_string(room.players.size()) + "/" + std::to_string(room.maxPlayers);
    }
    return response;
}

void addCardToPlayer(PlayerState &playerState, const std::string &newCard) {
    playerState.playerCards.push_back(newCard);
    playerState.playerScore += getCardValue(newCard);
}

void broadcastToPlayers(const Room &room, const std::string &message,  const std::string &excludeClientId) {
    for (const auto &x : room.players) {
        if (x.first != excludeClientId) {
            send(x.second.socket, message.c_str(), message.size(), 0);
        }
    }
}

bool dealerDrawCard(Room &room) {
    if (room.dealerScore < 17) {
        std::string dealersNewCard = getRandomCard();
        room.dealerCards.push_back(dealersNewCard);
        room.dealerScore += getCardValue(dealersNewCard);

        std::string dealerCard = "DEALER_CARD|" + dealersNewCard + "|" + std::to_string(room.dealerScore) + "\n";
        broadcastToPlayers(room, dealerCard, "-1"); // Dealerova akce je broadcastována všem
        return true;
    }
    return false;
}

std::string findNextPlayer(Room &room, const std::string currentPlayerId) {
    auto it = room.players.find(currentPlayerId);
    if (it != room.players.end()) {
        auto nextIt = std::next(it);
        while (nextIt != room.players.end() && nextIt->second.isStanding) {
            nextIt = std::next(nextIt);
        }
        if (nextIt == room.players.end()) {
            nextIt = room.players.begin();
            while (nextIt != it && nextIt->second.isStanding) {
                nextIt = std::next(nextIt);
            }

            dealerDrawCard(room);

        }
        if (nextIt != room.players.end()) {
            return nextIt->first;
        }
    }
    return "-1"; // Pokud žádný hráč nemůže hrát
}






// Funkce pro zpracování příkazů a aktualizaci stavu hry
//int clientId
std::string handleCommand(const std::string &command, GameState &state, const std::string &clientId) {
    std::string response;
    //int clientId
    PlayerState &playerState = state.players[clientId];
    //std::cout << "Command recieved: " << command << std::endl;
    //std::cout << "Command substring: " << command.substr(0, 9) << std::endl;
    if(command == "IN_GAME") {
        std::string roomId = state.players[clientId].inRoomId;
        auto roomIt = state.rooms.find(roomId); // Najde místnost podle roomId
        if (roomIt != state.rooms.end()) {
            Room &room = roomIt->second;
            if(room.currentPlayerId == clientId) {
                response = "YOUR_TURN";
            }
            else {
                response ="NOT_YOUR_TURN";
            }
        }

    }
    else if(command == "REFRESH") {
       response = getAllRooms(state);

    }
    else if (command.substr(0, 11) == "CREATE_ROOM") {
        size_t separatorPos = command.find('|');
        if (separatorPos != std::string::npos) {
            // Extrahování maxPlayers
            std::string maxPlayersStr = command.substr(separatorPos + 1);
            int maxPlayers = std::stoi(maxPlayersStr); // Převod na číslo

            // Generování ID místnosti
            std::string roomId = generateUUID();
            Room newRoom;
            newRoom.roomId = roomId;
            newRoom.maxPlayers = maxPlayers; // Nastavení maximálního počtu hráčů
            state.rooms[roomId] = newRoom; // Přidání nové místnosti do stavu

            // Odpověď na klienta
            response = getAllRooms(state);
        }   else {
            // Chybné formátování zprávy
            response = "ERROR:INVALID_COMMAND_FORMAT";
        }

    }
    else if (command.substr(0, 9) == "JOIN_ROOM") {
        // Extrahování roomId ze zprávy (předpokládáme, že formát je "JOIN_ROOM|roomId")
        std::string roomId = command.substr(10);  // Ořízne "JOIN_ROOM|" a zůstane pouze roomId
        response = joinRoom(clientId, roomId, state);
        //response = "ROOM_JOINED";
    }
//.substr(0, 10)
    else if (command == "LEAVE_ROOM") {
        // Extrahování roomId ze zprávy (předpokládáme, že formát je "JOIN_ROOM|roomId")
        //std::string roomId = command.substr(11);  // Ořízne "JOIN_ROOM|" a zůstane pouze roomId
        Room *room = getRoomForPlayer(clientId, state);
        response = leaveRoom(clientId, room->roomId, state);
        //response = "ROOM_JOINED";
    }

    else if (command == "WAIT_FOR_OPPONENT") {
        //response = "OPPONENT_JOINED";

        Room* room = getRoomForPlayer(clientId, state); // Získání místnosti, ve které je hráč připojen

        if (room != nullptr) {
            if (room->players.size() >= room->maxPlayers) {
                room->isWaitingForPlayers = false;
                response = "READY_TO_START"; // Odpověď, že hra může začít
            } else {
                response = "WAITING_FOR_OPPONENT"; // Odpověď, že čekáme na dalšího hráče
            }
        } else {
            response = "ERROR_NO_ROOM"; // Pokud hráč není v žádné místnosti
        }
    }
    else if (command == "HIT") {
        std::string newCard = getRandomCard();
        addCardToPlayer(playerState, newCard);

        response = newCard + "|" + std::to_string(playerState.playerScore);
        response += (playerState.playerScore > 21) ? "|PLAYER_BUST" : "|PLAYER_OK";

        std::string roomId = state.players[clientId].inRoomId;
        auto roomIt = state.rooms.find(roomId);
        if (roomIt != state.rooms.end()) {
            Room &room = roomIt->second;
            addCardToPlayer(room.players[clientId], newCard);

            std::string broadcast = "ENEMY_CARD|" + response + "\n";
            broadcastToPlayers(room, broadcast, clientId);

            std::string nextPlayerId = findNextPlayer(room, clientId);
            if (nextPlayerId != "-1") {
                room.currentPlayerId = nextPlayerId;
                send(room.players[nextPlayerId].socket, "YOUR_TURN\n", 10, 0);
            }

            //dealerDrawCard(room);
        }
        response = "PLAYER_CARD|" + response;
    }
    else if (command == "STAND") {
        std::string roomId = state.players[clientId].inRoomId;
        auto roomIt = state.rooms.find(roomId);
        std::string result = "";
        bool allPlayersStanding = true;

        if (roomIt != state.rooms.end()) {
            Room &room = roomIt->second;
            room.players[clientId].isStanding = true;

            for (const auto &player : room.players) {
                if (!player.second.isStanding) {
                    allPlayersStanding = false;
                    break;
                }
            }

            if (allPlayersStanding) {
                while(dealerDrawCard(room));
                int maxScore = 0;
                std::string winner = "";
                for (const auto &player : room.players) {
                    if (player.second.playerScore > maxScore && player.second.playerScore <= 21) {
                        maxScore = player.second.playerScore;
                        winner = player.first;
                    }
                }
                result = "RESULT|" + std::to_string(room.dealerScore) + "|";
                if(maxScore == 0) {
                     result += "Dealer\n";
                }
                else {
                    if(maxScore > room.dealerScore && room.dealerScore <= 21) {
                        result += winner+"\n";
                    }
                    else {
                        result += "Dealer\n";
                    }
                    //result += (maxScore < room.dealerScore && room.dealerScore>21 ? winner : "Dealer") + "\n";

                }
                //result = "RESULT|" + std::to_string(room.dealerScore) + "|" + (maxScore < room.dealerScore ? winner : "Dealer") + "\n";
                broadcastToPlayers(room, result, clientId);
                //resetGame(room);
            } else {
                std::string nextPlayerId = findNextPlayer(room, clientId);
                if (nextPlayerId != "-1") {
                    room.currentPlayerId = nextPlayerId;
                    send(room.players[nextPlayerId].socket, "YOUR_TURN\n", 10, 0);
                }
            }
        }
        if (!result.empty() && result.back() == '\n') {
            result.pop_back();
        }
        response = result.empty() ? "STAND_RECEIVED" : result;

    }
    else if (command == "NEW_GAME") {
        playerState.playerCards.clear();
        playerState.playerScore = 0;
        playerState.isStanding = false;
        //response = "NEW_GAME_STARTED";
    } else {
        Room *room = getRoomForPlayer(clientId, state);
        std::string message = "KICKED|" + clientId + "\n";

        for (auto &player : room->players) {
            if (player.first != clientId) {
                send(player.second.socket, message.c_str(), message.size(), 0);
            }
        }

        resetGame(*room);

        {
            std::lock_guard<std::mutex> lock(state.gameStateMutex);
            state.disconnectSignals[clientId] = true; // Nastavení signálu pro odpojení
            state.players.erase(clientId);           // Odebrání hráče ze stavu
        }

        response = "DISCONNECT";
    }

    return response + "\n";
}






// Funkce pro obsluhu klientů v samostatném vlákně
/*
void clientHandler(int client_socket, GameState &gameState) {
    //int playerId = client_socket;
    //std::string playerId = generateUUID();
    std::string playerId = handlePlayerConnection("", gameState);
    gameState.players[playerId] = PlayerState();

    while (true) {

        char buffer[256];
        memset(buffer, 0, 256);
        int bytes_received = recv(client_socket, buffer, 256, 0);
        if (bytes_received <= 0) {
            std::cout << "Client disconnected, ID: " << playerId << std::endl;
            gameState.players.erase(playerId);
            break;
        }

        std::string command(buffer);
        command = trim(command);
        std::string response = handleCommand(command, gameState, playerId);
        send(client_socket, response.c_str(), response.size(), 0);
        //checkForInactivePlayers(gameState);
    }

    closeSocket(client_socket);
}*/

// Funkce pro pravidelnou kontrolu aktivity hráčů
void monitorPing(GameState& gameState) {
    while (true) {
        auto now = std::chrono::steady_clock::now();
        for (auto& player : gameState.players) {
            // Kontrola časového limitu pro poslední PING
            if (std::chrono::duration_cast<std::chrono::seconds>(now - player.second.lastPingTime).count() > 30) {
                std::cout << "Hráč " << player.first << " byl odpojen (timeout)." << std::endl;
                gameState.players.erase(player.first); // Odstranění neaktivního hráče
            }
        }
        std::this_thread::sleep_for(std::chrono::seconds(10)); // Kontrola každých 10 sekund
    }
}



// Funkce pro odesílání PING zpráv
void sendPing(int client_socket) {
    auto pingInterval = std::chrono::seconds(5);  // Interval mezi pingy
    auto lastPingTime = std::chrono::steady_clock::now();

    while (true) {
        // Zkontrolujeme, zda je čas odeslat nový "PING"
        auto now = std::chrono::steady_clock::now();
        if (now - lastPingTime >= pingInterval) {
            std::string pingMessage = "PING\n";

            // Odesílání PING zprávy
            //std::lock_guard<std::mutex> lock(pingMutex);  // Zajistíme bezpečný přístup k socketu
            std::lock_guard<std::mutex> lock(socketMutex);  // Zajistíme bezpečný přístup k socketu
            int bytes_sent = send(client_socket, pingMessage.c_str(), pingMessage.size(), 0);
            std::cout << "PING sent on socket: "<<client_socket << std::endl;
            if (bytes_sent <= 0) {
                std::cout << "Error sending PING message to client" << std::endl;
                break;
            }
            lastPingTime = now;  // Aktualizace posledního odeslaného pingu
        }

        // Krátká pauza, aby proces neblokoval CPU
        std::this_thread::sleep_for(std::chrono::seconds(1));
    }
}

void reconnectThread(const std::string &playerId, GameState &gameState, int oldSocket) {
    constexpr int reconnectWindow = 300; // v sekundách
    auto start = std::chrono::steady_clock::now();

    while (std::chrono::steady_clock::now() - start < std::chrono::seconds(reconnectWindow)) {
        {
            std::lock_guard<std::mutex> lock(gameState.gameStateMutex);

            // Kontrola signálu odpojení
            if (gameState.disconnectSignals[playerId]) {
                std::cout << "Reconnect thread terminated for client ID: " << playerId << std::endl;
                return;
            }

            // Pokud je hráč aktivní, ukončíme reconnect
            if (gameState.players[playerId].isActive && gameState.players[playerId].socket != oldSocket) {
                std::cout << "Player reconnected during reconnect window, ID: " << playerId << std::endl;
                return;
            }
        }

        std::this_thread::sleep_for(std::chrono::milliseconds(500)); // Krátké čekání
    }

    // Pokud hráč neprovedl reconnect v rámci časového okna
    {
        std::lock_guard<std::mutex> lock(gameState.gameStateMutex);

        if (!gameState.players[playerId].isActive) {
            std::cout << "Reconnect window expired for client ID: " << playerId << std::endl;
            Room *room = getRoomForPlayer(playerId, gameState);
            std::string message = "KICKED|" + playerId + "\n";

            for (auto &player : room->players) {
                send(player.second.socket, message.c_str(), message.size(), 0);
            }

            resetGame(*room);
            gameState.players.erase(playerId);
            closeSocket(oldSocket); // Zavřít starý socket
        }
    }
}


Room* getPlayerRoom(const std::string& playerId, GameState& state) {
    auto playerIt = state.players.find(playerId);
    if (playerIt != state.players.end()) {
        const std::string& roomId = playerIt->second.inRoomId;
        auto roomIt = state.rooms.find(roomId);
        if (roomIt != state.rooms.end()) {
            return &roomIt->second;
        }
    }
    return nullptr; // Pokud hráč nebo místnost neexistuje
}






void clientHandler(int client_socket, GameState &gameState) {
    char buffer[256];
    memset(buffer, 0, 256);

    // Inicializace připojení
    int bytes_received = recv(client_socket, buffer, 256, 0);
    if (bytes_received <= 0) {
        std::cout << "Client disconnected during connection initialization." << std::endl;
        closeSocket(client_socket);
        return;
    }

    std::string initialMessage(buffer);
    initialMessage = trim(initialMessage);

    size_t pos1 = initialMessage.find("|");
    std::string userName = initialMessage.substr(pos1 + 1);

    std::string playerId = handlePlayerConnection(client_socket, userName, gameState);
    if (playerId.empty()) {
        std::cout << "Failed to assign ID to client. Disconnecting." << std::endl;
        closeSocket(client_socket);
        return;
    }
    //std::thread pingThread(sendPing, client_socket);
    bool clientActive = true;




    while (clientActive) {
        memset(buffer, 0, 256);
        bytes_received = recv(client_socket, buffer, 256, 0);

        if (bytes_received <= 0) {
                std::string message = "DISCONNECTED|"+playerId+"\n";
                std::cout << "Client seems disconnected, ID: " << playerId << std::endl;
                {
                    std::lock_guard<std::mutex> lock(gameState.gameStateMutex);
                    gameState.players[playerId].isActive = false; // Nastavíme jako neaktivní
                }

                Room *room = getPlayerRoom(playerId, gameState);
                if (room != nullptr) {
                    broadcastToPlayers(*room, message, playerId);
                }



                //TODO:poslat klient is inactive

                std::thread(reconnectThread, playerId, std::ref(gameState), client_socket).detach();
                break;
        } else {
            std::string command(buffer);
            command = trim(command);

            if (command == "PONG") {
                auto now = std::chrono::steady_clock::now();
                std::lock_guard<std::mutex> lock(gameState.gameStateMutex);
                gameState.players[playerId].lastPingTime = now;
            } else {
                std::string response = handleCommand(command, gameState, playerId);
                send(client_socket, response.c_str(), response.size(), 0);
            }
        }
    }

    //pingThread.join();
    closeSocket(client_socket);
}







bool isValidPort(const std::string &portStr) {
    try {
        int port = std::stoi(portStr);
        return port > 0 && port <= 65535;
    } catch (...) {
        return false;
    }
}

bool isValidIPv4(const std::string &ip) {
    std::regex ipv4Regex(
        R"(^((25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)$)");
    return std::regex_match(ip, ipv4Regex);
}


int main(int argc, char* argv[]) {
    if (argc >= 2) {
        if (isValidPort(argv[1])) {
            commandPort = std::stoi(argv[1]); // První argument jako port
        } else {
            std::cerr << "Invalid port number. Please provide a value between 1 and 65535." << std::endl;
            return 1;
        }
    }

    if (argc >= 3) {
        if (isValidIPv4(argv[2])) {
            ipv4Address = argv[2]; // Druhý argument jako IP adresa
        } else {
            std::cerr << "Invalid IPv4 address. Please provide a valid address (e.g., 192.168.1.1)." << std::endl;
            return 1;
        }
    }

    std::cout << "Starting server on " << ipv4Address << ":" << commandPort << "..." << std::endl;
    srand(time(0)); // Inicializace generátoru náhodných čísel

#ifdef _WIN32
    WSADATA wsaData;
    if (WSAStartup(MAKEWORD(2, 2), &wsaData) != 0) {
        std::cerr << "WSAStartup failed." << std::endl;
        return 1;
    }
#endif

    int server_socket = socket(AF_INET, SOCK_STREAM, 0);
    if (server_socket == -1) {
        std::cerr << "Failed to create socket." << std::endl;
        return 1;
    }

    struct sockaddr_in server_addr;
    server_addr.sin_family = AF_INET;
    server_addr.sin_port = htons(commandPort);
    server_addr.sin_addr.s_addr = inet_addr(ipv4Address.c_str());

    if (bind(server_socket, (struct sockaddr*)&server_addr, sizeof(server_addr)) == -1) {
        std::cerr << "Bind failed." << std::endl;
        return 1;
    }

    if (listen(server_socket, 5) == -1) {
        std::cerr << "Listen failed." << std::endl;
        return 1;
    }

    std::cout << "Server listening on "<< ipv4Address <<":"<< commandPort <<"..." << std::endl;
    GameState gameState;

    while (true) {
        struct sockaddr_in client_addr;
        socklen_t client_size = sizeof(client_addr);
        int client_socket = accept(server_socket, (struct sockaddr*)&client_addr, &client_size);
        if (client_socket == -1) {
            std::cerr << "Failed to accept connection." << std::endl;
            continue;
        }

        std::cout << "Client connected, starting new thread..." << std::endl;

        std::thread(sendPing, client_socket).detach();
        std::thread(clientHandler, client_socket, std::ref(gameState)).detach();
    }

    closeSocket(server_socket);

    return 0;
}
