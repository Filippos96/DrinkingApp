package com.example.drinkingapp

import android.content.ContentValues.TAG
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.example.drinkingapp.ui.theme.*
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlin.math.pow
import kotlin.random.Random

data class Lobby(
    var players: List<Player> = emptyList(),
    val lobbyKey: String = "",
    var gameStarted: Boolean = false,
    var prompts: List<String> = emptyList()
)

data class Player(
    var username: String,
    var color: String
){
    constructor() : this("", "Color1")
}



class GameRoomViewModel() : ViewModel() {
    private val database = Firebase.database.reference
    private val lobbiesRef = database.child("lobbies")

    private var _username = mutableStateOf("")
    val username: MutableState<String>
        get() = _username


    private var _lobbyKey = mutableStateOf("")
    val lobbyKey: MutableState<String>
        get() = _lobbyKey

    private var _host = mutableStateOf(false)
    val host: MutableState<Boolean>
        get() = _host

    private val _lobbies = listOf<Lobby>()

    private val _lobby = mutableStateOf(Lobby())
    val lobby: MutableState<Lobby>
        get() = _lobby

    private val _prompts = mutableStateListOf<String>()
    val prompts: List<String>
        get() = _prompts

    private val _currentAnswers = mutableStateListOf<String>()
    val currentAnswers: List<String>
        get() = _currentAnswers

    private val _allAnswers = mutableStateListOf<String>()
    val allAnswers: List<String>
        get() = _allAnswers



    fun createNewLobby(username: String, navController: NavController) : String {
        val lobbyKey = generateRandomKey()
        _username.value = username
        _host.value = true
        _lobbyKey.value = lobbyKey
        val newLobby = Lobby(lobbyKey = lobbyKey, players = listOf(Player(username, "color0")))
        lobbiesRef.child(lobbyKey).setValue(newLobby)
        val lobbyReference = database.child("lobbies").child(lobbyKey)
        addLobbyEventListener(navController, lobbyReference)
        return lobbyKey
    }

    private fun addLobbyEventListener(navController: NavController, lobbyReference: DatabaseReference) {
        // [START post_value_event_listener]
        val lobbyListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                val lobby = dataSnapshot.getValue<Lobby>()
                lobby?.let {
                    _lobby.value = lobby
                    if ( it.gameStarted ) {
                        lobbyReference.removeEventListener(this)
                        navController.navigate(Screen.CreatePrompt.route)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
            }
        }
        lobbyReference.addValueEventListener(lobbyListener)
        // [END post_value_event_listener]
    }

    private fun generateRandomKey() : String {
        val seed = System.currentTimeMillis();
        val R = Random(seed)
        val uniqueKey = R.nextInt(0,999).toString()
        return uniqueKey
    }

    fun joinLobby(username: String, lobbyKey: String, navController: NavController) {

        val lobbyToJoinRef = lobbiesRef.child(lobbyKey)

        lobbyToJoinRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val lobbyToJoin = dataSnapshot.getValue(Lobby::class.java)
                if (lobbyToJoin != null) {
                    _username.value = username
                    _lobbyKey.value = lobbyKey
                    val updatedPlayers = lobbyToJoin.players.toMutableList().apply { add(Player(username, "color" + lobbyToJoin.players.size.toString())) }
                    lobbyToJoin.players = updatedPlayers
                    lobbiesRef.child(lobbyToJoin.lobbyKey).setValue(lobbyToJoin)
                } else {
                    Log.e(TAG, "Lobby not found: $lobbyKey")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(TAG, "Error joining lobby: $lobbyKey", databaseError.toException())
            }
        })
        addLobbyEventListener(navController, lobbyToJoinRef)
    }

    fun startGame(){
        val lobbyRef = lobbiesRef.child(_lobbyKey.value)
        lobbyRef.child("gameStarted").setValue(true)
    }

    fun createNewPrompt(prompt: String, navController: NavController) {
        val promptsRef = database.child("lobbies").child(_lobbyKey.value).child("prompts")

        promptsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Get the current list of prompts
                val prompts = dataSnapshot.getValue<List<String>>()
                if(prompts == null) {
                    // Create a new list of answers if the current list is null
                    val newPrompts = mutableListOf(prompt)
                    // Update the list of prompts in the database
                    promptsRef.setValue(newPrompts)
                } else {
                    // Add the new prompt to the list
                    val newPrompts = prompts.toMutableList()
                    newPrompts.add(prompt)
                    // Update the list of prompts in the database
                    promptsRef.setValue(newPrompts)
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
        val currentAnswersRef = database.child("lobbies").child(_lobbyKey.value).child("currentAnswers")
        addPromptsEventListener(navController, promptsRef)
        addCurrentAnswersEventListener(navController, currentAnswersRef)
        navController.navigate(Screen.Waiting.route)
    }

    private fun addPromptsEventListener(navController: NavController, promptsReference: DatabaseReference) {
        val promptsListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val prompts = dataSnapshot.getValue<List<String>>()

                prompts?.let {
                    if ( prompts.size == _lobby.value.players.size ) {
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(1000)
                            _prompts.addAll(prompts)
                            navController.navigate(Screen.Questions.route)
                        }
                        promptsReference.removeEventListener(this)
                    }

                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
            }
        }
        promptsReference.addValueEventListener(promptsListener)
    }

    fun submitAnswer(player: String, navController: NavController) {
        addToTotal(player)

        val currentAnswersRef = database.child("lobbies").child(_lobbyKey.value).child("currentAnswers")

        currentAnswersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val answers = dataSnapshot.getValue<List<String>>()
                if(answers == null) {
                    val newAnswers = mutableListOf(player)
                    currentAnswersRef.setValue(newAnswers)
                } else {
                    val newAnswers = answers.toMutableList()
                    newAnswers.add(player)
                    currentAnswersRef.setValue(newAnswers)
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun addToTotal(player: String) {
        val allAnswers = database.child("lobbies").child(_lobbyKey.value).child("allAnswers")

        allAnswers.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val answers = dataSnapshot.getValue<List<String>>()
                if (answers == null) {
                    val newAnswers = mutableListOf(player)
                    allAnswers.setValue(newAnswers)
                } else {
                    val newAnswers = answers.toMutableList()
                    newAnswers.add(player)
                    allAnswers.setValue(newAnswers)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun addCurrentAnswersEventListener(navController: NavController, currentAnswersRef: DatabaseReference) {
        val currentAnswersListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val forCurrentAnswers = dataSnapshot.getValue<List<String>>()

                forCurrentAnswers?.let {
                    if ( forCurrentAnswers.size == _lobby.value.players.size ) {
                        _currentAnswers.clear()

                        CoroutineScope(Dispatchers.Main).launch {
                            delay(1000)
                            _currentAnswers.addAll(forCurrentAnswers)
                            _allAnswers.addAll(forCurrentAnswers)
                            currentAnswersRef.removeValue()
                            Log.d("Hej", _allAnswers.size.toString())
                            navController.navigate(Screen.Result.route)
                        }
                        if (2.0.pow(_lobby.value.players.size).toInt() == _allAnswers.size + _lobby.value.players.size) {
                            currentAnswersRef.removeEventListener(this)
                        }
                    }

                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
            }
        }
        currentAnswersRef.addValueEventListener(currentAnswersListener)
    }

    fun clearCurrentAnswers() {
        val answersRef = database.child("lobbies").child(_lobbyKey.value).child("currentAnswers")

        answersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Get the current list of prompts
                val answers = dataSnapshot.getValue<List<String>>()
                if(answers == null) {

                } else {
                    // Add the new answer to the list
                    val newAnswers = answers.toMutableList()
                    // Remove the last one
                    newAnswers.clear()
                    _currentAnswers.clear()
                    // Update the list of answers in the database
                    answersRef.setValue(newAnswers)
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    fun popLastPrompt() {
        val promptsRef = database.child("lobbies").child(_lobbyKey.value).child("prompts")

        promptsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Get the current list of prompts
                val prompts = dataSnapshot.getValue<List<String>>()
                if(prompts == null) {

                } else {
                    if (_host.value) {
                        // Add the new prompt to the list
                        val newPrompts = prompts.toMutableList()
                        // Remove the last one
                        newPrompts.removeLast()

                        // Update the list of prompts in the database
                        promptsRef.setValue(newPrompts)
                    }
                    _prompts.removeLast()
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    fun disbandLobby() {
        _host.value = false
        val lobbyToDisbandRef = lobbiesRef.child(_lobbyKey.value)
        lobbyToDisbandRef.removeValue()
    }

    fun removePlayerFromLobby() {
        val playersRef = lobbiesRef.child(_lobbyKey.value).child("players")

        playersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val players = dataSnapshot.value as MutableList<String>
                players.remove(_username.value)
                playersRef.setValue(players)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d("TAG", "onCancelled: ${databaseError.toException()}")
            }
        })

    }

    fun getColorFromPlayer(colorName: String): List<Color>{
        return when (colorName) {
            "color0" -> colorP0
            "color1" -> colorP1
            "color2" -> colorP2
            "color3" -> colorP3
            "color4" -> colorP4
            "color5" -> colorP5
            "color6" -> colorP6
            "color7" -> colorP7
            "color8" -> colorP8
            "color9" -> colorP9
            "color10" -> colorP10
            "color11" -> colorP11
            "color12" -> colorP12
            "color13" -> colorP13
            "color14" -> colorP14
            else -> listOf(Color(0xFFFFE600), Color(0xFF000000))
        }

    }

}