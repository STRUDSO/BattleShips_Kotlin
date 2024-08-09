package battleship

data class Coordinate(val row:Char, val column:Int){
    companion object { /*...*/ }
    override fun toString() = "$row$column"
    fun to(other: Coordinate, expand:Int = 0): Set<Coordinate> {
        return buildSet {
            val lrow = minOf(row, other.row)
            val rrow = maxOf(row, other.row)
            val lcol = minOf(column, other.column)
            val rcol = maxOf(column, other.column)
            for (c in lrow-expand..rrow+expand) {
                for (i in lcol-expand..rcol+expand) {
                    add(Coordinate(c, i))
                }
            }
        }
    }

    fun inlineWith(back: Coordinate): Boolean {
        return row != back.row && column != back.column
    }
}

private fun Coordinate.Companion.from(f: String): Coordinate {
    val error =
        PlacementError(
            "You entered the wrong coordinates!",
            "Invalid coordinate, must be in the format letter first between A-J and a number from 1-10 'A1' your input '$f'"
        )

    if(f.length !in 2..3){
        throw error
    }
    val f1 = f.first()
    val f2 = f.drop(1).toIntOrNull()
    if (f1 !in 'A'..'J' || f2 == null || f2 !in 1..10) {
        throw error
    }
    return Coordinate(f1, f2)
}

class Ship(private val front: Coordinate, private val back: Coordinate, private val length: Int, private val name: String) {
    val area:Set<Coordinate> = front.to(back)
    val map = area.associateWith { Field.SHIP }.toMutableMap()
    var sunk:Boolean = false

    init {
        val placementError =
            PlacementError(
                "Wrong length of the $name!",
                "$name $length $front $back"
            )
        if(front.inlineWith(back)){
            throw placementError.copy(pretty = "Wrong ship location!")
        }
        if(area.size != length){
            throw placementError
        }
    }
    private val expand:Set<Coordinate> = front.to(back, 1)
    override fun toString() = "Ship from $front to $back"
    fun near(ac: Ship) = expand.intersect(ac.area).isNotEmpty()
    fun shoot(shot: Coordinate): Outcome {
        require(area.contains(shot)) { "this ship doesn't have $shot in it's area: $area"}
        map[shot] = Field.HIT
        sunk = map.values.all { it == Field.HIT }
        return if(!sunk) Outcome.Hit
        else Outcome.Sunk 
    }

}

enum class Outcome {
    Hit{
        override fun toString() = "You hit a ship!"
    },
    Sunk {
        override fun toString() = "You sank a ship!"    
    },
    Youwon {
        override fun toString() = "You sank the last ship. You won. Congratulations!"
    },
    Miss {
        override fun toString() = "You missed!"
    }
}


class Board {
    private var ships = mutableListOf<Ship>()
    private var misses = mutableSetOf<Coordinate>()

    fun draw(reveal:Boolean = false){
        val map = buildMap<Char, MutableMap<Int, Field>> {
            for (i in 'A'..'J'){
                put(i, mutableMapOf())
            }
        }
        for(s in ships) {
            s.map.forEach { (it, value) ->
                map[it.row]!![it.column] = value.show(reveal)
            }
        }
        for (m in misses) {
            map[m.row]!![m.column] = Field.MISS
        }
        println("  1 2 3 4 5 6 7 8 9 10")
        for (i in 'A'..'J'){
            val fields = map[i]!!
            val s = buildString {
                append("$i ")
                for (j in 1..10) {
                    append(fields[j] ?: Field.FOG);
                    append(" ")
                }
            }
            println(s.trimEnd())
        }
    }

    fun place(ac: Ship) {
        if(ships.any{ it.near(ac)})
            throw PlacementError("You placed it too close to another one.")
        ships.add(ac)
    }
    fun shoot(shot: Coordinate): Outcome {
        val affected = ships.find { it.area.contains(shot) }
        if(affected == null) {
            misses.add(shot)
            return Outcome.Miss 
        }

        val normalResult = affected.shoot(shot)
        if(ships.all(Ship::sunk))
            return Outcome.Youwon
        return normalResult
    }
}

enum class Field {
    FOG, SHIP, HIT, MISS;

    override fun toString() = when(this){
        FOG -> "~"
        SHIP -> "O"
        HIT -> "X"
        MISS -> "M"
    }
    fun show(reveal: Boolean): Field {
        return if(reveal) 
            this 
        else 
            this.takeIf { it == HIT || it == MISS } ?: FOG
    }
}

data class PlacementError(val pretty: String, val debug: String = pretty) : Error(debug) {
    override fun toString() = pretty
}
const val TESTING = false



fun main() {
    val player1Setup = listOf("F3 F7", "A1 D1", "J7 J10", "J10 J8", "B9 D8", "B9 D9", "E6 D6", "I2 J2")
    val player2Setup = listOf("H2 H6", "F3 F6", "H8 F8", "D4 D6", "D8 C8")

    fun generateInput(strings: List<String>) = if (TESTING) {
        strings.asSequence()
    } else {
        generateSequence(::readLine)
    }.iterator()
    
    val board = Board()
    println("Player 1, place your ships on the game field")
    println()
    board.draw()
    setup(generateInput(player1Setup), board)

    PassOver()

    println("Player 2, place your ships on the game field")
    println()

    val board2 = Board()
    board2.draw()
    setup(generateInput(player2Setup), board2)

    PassOver()

    val playSequence
            =
        if(TESTING) {
            listOf("I3", "A1").asSequence()
        }else {
            generateSequence(::readLine)
        }.iterator()
    
    play(listOf(board, board2), playSequence)

}

private fun PassOver() {
    println("Press Enter and pass the move to another player")
    println("***")
    if(!TESTING) {
        readln()
    }
    else {
        println()
        println("> (enter)")
    }

}

fun play(boards: List<Board>, inputSequence: Iterator<String>) {
//    println()
//    println("The game starts!")
    var player = 0
    var opponent = 1
    fun opponentBoard() =  boards[opponent]
    fun playerBoard() =  boards[player]

    fun drawBoards() {
        opponentBoard().draw()
        println("---------------------")
        playerBoard().draw(true)
    }

    var done = false
    var retry = false;
    do{
        println()
        drawBoards()
        if(!retry) {
            println()
            println("Player ${player+1}, it's your turn:")
        }
        retry = false;
        try {
            val f = inputSequence.next()
            Debug(f)
            val shot = Coordinate.from(f)
            val outcome = opponentBoard().shoot(shot)
            println(outcome)
            println()
            if (outcome == Outcome.Youwon) {
                done = true;
            }
//            else {
//                println()
//                board.draw(true)
//            }
            PassOver()
            opponent = player.also { player = opponent }
        }catch (e:PlacementError){
            HandlePlacementError(e)
            retry = true;
        }
        
    }while(!done)
    
}

private fun setup(
    inputSequence: Iterator<String>,
    board: Board
) {
    val ships = mutableListOf(
        ("Aircraft Carrier" to 5),
        ("Battleship" to 4),
        ("Submarine" to 3),
        ("Cruiser" to 3),
        ("Destroyer" to 2)
    )
    for ((name, length) in ships) {
        var placed = false
        println()
        println("Enter the coordinates of the $name ($length cells):")
        do {
            try {
                val pair = inputSequence.next()
                Debug(pair);
                val (f, b) = pair.split(' ')
                val ship = Ship(Coordinate.from(f), Coordinate.from(b), length, name)
                board.place(ship)
                println()
                board.draw(true)
                placed = true
            } catch (e: PlacementError) {
                HandlePlacementError(e)
            }
        } while (!placed)
    }
}

private fun HandlePlacementError(e: PlacementError) {
    val (_, debug) = e
    println("Error! $e Try again:")
    Debug(debug);
}

fun Debug(debug: Any) {
    if (TESTING) {
        println()
        println("> $debug")
        println()
    }
}
