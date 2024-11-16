#include <iostream>
#include <string>
#include <vector>
#include <algorithm>
#include <map>
#include <ctime>
#include <cstdlib>
#include <thread> // Pro práci s vlákny
#include <iostream>
#include <random>
#include <sstream>
#include <iomanip>
#include <mutex>
#include <shared_mutex>



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
#ifdef _WIN32
#define closeSocket closesocket
#define socklen_t int
#else
#define closeSocket close
#endif


std::shared_mutex gameStateMutex;
int commandPort = 8080; // Port pro příkazy
int pingPort = 8081;    // Port pro PING/PONG
std::string ipv4Address = "127.0.0.1";

// Struktura, která obsahuje karty a skóre hráče
struct PlayerState {
    int socket;
    std::vector<std::string> playerCards;  // Karty hráče
    int playerScore = 0;                   // Skóre hráče
    bool isStanding = false;               // Indikátor, zda hráč stojí
    std::chrono::time_point<std::chrono::steady_clock> lastPingTime; // Nový atribut pro čas posledního pingu
};

struct Room {
    std::string roomId; // ID místnosti
    std::map<std::string, PlayerState> players; // Hráči v místnosti
    bool isWaitingForPlayers = true; // Indikátor, zda čekáme na další hráče
    int maxPlayers; // Maximální počet hráčů v místnosti

    //Room(const std::string& id, int max) : roomId(id), maxPlayers(max) {}
};

// Struktura pro stav celé hry, zahrnující dealera
struct GameState {
    std::vector<std::string> dealerCards;           // Karty dealera
    int dealerScore = 0;                            // Skóre dealera
    //std::map<int, PlayerState> players;
    std::map<std::string, PlayerState> players;     // Mapa hráčů: <hráčské ID, stav hráče>
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
    std::cout << "socket: " << socket << std::endl;
    playerState.socket = socket;
    playerState.lastPingTime = std::chrono::steady_clock::now();

    {
        std::unique_lock lock(gameStateMutex); // Zámek pro bezpečnou práci s herním stavem
        gameState.players[playerId] = playerState; // Přidání hráče do herního stavu
    }

    std::cout << "New player added with ID: " << playerId << " and socket: " << socket << std::endl;
    return playerId;
}



// Funkce pro zpracování připojení hráče
std::string handlePlayerConnection(int socket, const std::string &receivedId, GameState &gameState) {
    if (!receivedId.empty() && gameState.players.find(receivedId) != gameState.players.end()) {
        std::cout << "Player with ID: " << receivedId << " reconnected." << std::endl;
        return receivedId;
    }

    // Voláme addPlayer, pokud se hráč připojuje poprvé
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
    std::vector<std::string> deck = {
        "2H", "3H", "4H", "5H", "6H", "7H", "8H", "9H", "10H", "JH", "QH", "KH", "AH", // Srdce
        "2D", "3D", "4D", "5D", "6D", "7D", "8D", "9D", "10D", "JD", "QD", "KD", "AD", // Káry
        "2S", "3S", "4S", "5S", "6S", "7S", "8S", "9S", "10S", "JS", "QS", "KS", "AS", // Piky
        "2C", "3C", "4C", "5C", "6C", "7C", "8C", "9C", "10C", "JC", "QC", "KC", "AC"  // Kříže
    };
    return deck[rand() % deck.size()];
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

std::string joinRoom(const std::string &playerId, GameState &gameState) {
    // Pokud už existuje místnost, přidáme hráče
    for (auto &roomEntry : gameState.rooms) {
        Room &room = roomEntry.second;
        if (room.isWaitingForPlayers) {
            room.players[playerId] = PlayerState(); // Přidání hráče do místnosti
            std::cout << "Player " << playerId << " joined room " << room.roomId << std::endl;

            // Zkontrolujeme, zda máme dost hráčů pro zahájení hry
            if (room.players.size() == room.maxPlayers) {
                room.isWaitingForPlayers = false; // Zahájit hru
                return "GAME_START"; // Odeslat hráčům zprávu, že hra začíná
            }
            return "WAITING_FOR_OPPONENT"; // Čekáme na dalšího hráče
        }
    }

    // Pokud místnost neexistuje, vytvoř novou

    //Room newRoom(generateUUID(), 2);
    Room newRoom;
    newRoom.roomId = generateUUID(); // Vytvoření nové ID pro místnost
    newRoom.players[playerId] = PlayerState(); // Přidání hráče
    gameState.rooms[newRoom.roomId] = newRoom;
    std::cout << "Player " << playerId << " created and joined new room " << newRoom.roomId << std::endl;
    return "WAITING_FOR_PLAYERS"; // Čekáme na dalšího hráče
}

void checkForInactivePlayers(GameState &state) {
    auto now = std::chrono::steady_clock::now();
    for (auto it = state.players.begin(); it != state.players.end(); ) {
        PlayerState &playerState = it->second;
        // Nastavíme timeout na 60 sekund
        auto inactiveDuration = std::chrono::seconds(60);

        if (now - playerState.lastPingTime > inactiveDuration) {
            std::cout << "Player " << it->first << " is inactive and will be removed." << std::endl;
            closesocket(playerState.socket);
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




// Funkce pro zpracování příkazů a aktualizaci stavu hry
//int clientId
std::string handleCommand(const std::string &command, GameState &state, const std::string &clientId) {
    std::string response;
    //int clientId
    PlayerState &playerState = state.players[clientId];
    std::cout << "Command recieved: " << command << std::endl;
    //std::cout << "Command substring: " << command.substr(0, 9) << std::endl;
/*
    if (command == "PING") {
        // Aktualizace času posledního pingu
        //playerState.lastPingTime = std::chrono::steady_clock::now();
        //response = "PONG";

        // Ihned odpoví na PING bez dalšího zpracování

    }
    */
    if(command == "REFRESH") {
        response = "ROOMS"; // Začátek odpovědi
        for (const auto &roomPair : state.rooms) {
            const Room &room = roomPair.second;
            // Přidání informací o místnosti do odpovědi
            response += "|" + room.roomId + "|" + (room.isWaitingForPlayers ? "waiting" : "full") + "|" + std::to_string(room.players.size()) + "/" + std::to_string(room.maxPlayers);
        }

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
            response = "ROOM_CREATED:" + roomId;
        }   else {
            // Chybné formátování zprávy
            response = "ERROR:INVALID_COMMAND_FORMAT";
        }

    }
    else if (command.substr(0, 9) == "JOIN_ROOM") {
        response = joinRoom(clientId, state);
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
        playerState.playerCards.push_back(newCard);
        playerState.playerScore += getCardValue(newCard);
        response = "PLAYER_CARD " + newCard + " " + std::to_string(playerState.playerScore);
    } else if (command == "STAND") {
        playerState.isStanding = true;
        //std::cout << state.players.size()<< std::endl;
        // Pokud máme pouze jednoho hráče, přejdeme přímo na finální fázi
        if (state.players.size() == 1) {
            // Dealer táhne karty, dokud nemá alespoň 17 bodů
            while (state.dealerScore < 17) {
                std::string newCard = getRandomCard();
                state.dealerCards.push_back(newCard);
                state.dealerScore += getCardValue(newCard);
            }

            // Vytvoření výsledku pro jediného hráče
            std::string finalResult = evaluateGame(playerState, state.dealerScore);
            std::string dealerCards = "DEALER_CARDS ";
            for (const auto &card : state.dealerCards) {
                dealerCards += card + " ";
            }
            // Vytvoření finální zprávy pro hráče
            response = dealerCards + "| FINAL_SCORE " + std::to_string(state.dealerScore) + " | " + finalResult;

            state.dealerCards.clear();
            state.dealerScore = 0;

            playerState.playerCards.clear();
            playerState.playerScore = 0;
            playerState.isStanding = false;

        } else {
            response = "PLAYER_STANDS";
        }
    } else if (command == "NEW_GAME") {
        playerState.playerCards.clear();
        playerState.playerScore = 0;
        playerState.isStanding = false;
        //response = "NEW_GAME_STARTED";
    } else {
        response = "UNKNOWN_COMMAND";
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


void clientHandler(int client_socket, GameState &gameState) {
    std::string playerId = handlePlayerConnection(client_socket, "", gameState);
    bool clientActive = true;

    // Nastavení intervalu pro zasílání "PING"
    auto pingInterval = std::chrono::seconds(10);
    auto lastPingTime = std::chrono::steady_clock::now();

    while (clientActive) {
        // Zkontrolovat, zda je čas odeslat nový "PING"
        auto now = std::chrono::steady_clock::now();
        if (now - lastPingTime >= pingInterval) {
            std::string pingMessage = "PING\n";
            int bytes_sent = send(client_socket, pingMessage.c_str(), pingMessage.size(), 0);
            if (bytes_sent <= 0) {
                std::cout << "Chyba při odesílání PING zprávy klientovi ID: " << playerId << std::endl;
                break;
            }
            lastPingTime = now;
        }

        // Přijímání zpráv od klienta
        char buffer[256];
        memset(buffer, 0, 256);
        int bytes_received = recv(client_socket, buffer, 256, 0);
        if (bytes_received <= 0) {
            std::cout << "Klient odpojen, ID: " << playerId << std::endl;
            std::lock_guard<std::mutex> lock(gameState.gameStateMutex);
            gameState.players.erase(playerId);
            clientActive = false;
            break;
        }

        std::string command(buffer);
        command = trim(command);
        std::cout << "Přijatý příkaz: " << command << std::endl;
        std::string response;

        // Odpověď na příkaz od klienta

        if (command == "PONG") {
            // Aktualizace času poslední odezvy klienta
            std::lock_guard<std::mutex> lock(gameState.gameStateMutex);
            gameState.players[playerId].lastPingTime = now;

        } else {
            response = handleCommand(command, gameState, playerId);
            send(client_socket, response.c_str(), response.size(), 0);
        }

        // Kontrola neaktivních hráčů
        //checkForInactivePlayers(gameState);
    }

    closeSocket(client_socket);
}








int main() {
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

    std::cout << "Server listening on port 8080..." << std::endl;
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
        std::thread(clientHandler, client_socket, std::ref(gameState)).detach();
    }

    closeSocket(server_socket);
#ifdef _WIN32
    WSACleanup();
#endif
    return 0;
}
