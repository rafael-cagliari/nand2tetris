import java.io.File

private const val C_INSTRUCT_PREFIX = "111"

private var symbolAddress = 16

val symbolsList: MutableMap<String, Int> =
    mutableMapOf(
        "SP" to 0,
        "LCL" to 1,
        "ARG" to 2,
        "THIS" to 3,
        "THAT" to 4,
        "R0" to 0,
        "R1" to 1,
        "R2" to 2,
        "R3" to 3,
        "R4" to 4,
        "R5" to 5,
        "R6" to 6,
        "R7" to 7,
        "R8" to 8,
        "R9" to 9,
        "R10" to 10,
        "R11" to 11,
        "R12" to 12,
        "R13" to 13,
        "R14" to 14,
        "R15" to 15,
        "SCREEN" to 16384,
        "KBD" to 24576,
        )

fun main(args: Array<String>) {
    val file = File(args.first())

    if (!file.exists()) {
        println("Arquivo não encontrado: ${args.first()}")
        return
    }

    var firstReadIndex = 0
    val labelSymbolsResolvedFile: MutableList<String> = mutableListOf()

    //adds loops to predefined symbols
    file.readLines().forEach{ line ->
        val trimmedLine = line.trim()
        val isSkippableLine = trimmedLine.startsWith("//") || trimmedLine.isEmpty()

        if(trimmedLine.contains("(") && !isSkippableLine) { addLabelSymbol(trimmedLine, firstReadIndex) }
        else labelSymbolsResolvedFile.add(trimmedLine)

        if(!isSkippableLine && !trimmedLine.contains("(")) firstReadIndex++
    }

    val parsedLines = labelSymbolsResolvedFile.mapNotNull { line ->
        parser(line)
    }

    val hackFilePath = "${args.first().replace(".asm","")}.hack"
    val hackFile = File(hackFilePath)

    hackFile.printWriter().use { writer ->
        parsedLines.map { line ->
            writer.println(line)
        }
    }
}

private fun parser(line: String): String? {
    if(line.startsWith("//") || line.isEmpty()) return null
    if(line.startsWith("@")) return toAInstruction(line)
        else return toCInstruction(line)
}

private fun toAInstruction(instruction: String): String {
    val address = instruction.drop(1)
    lateinit var addressMemory: String
    when {
        symbolsList.containsKey(address) -> addressMemory = symbolsList[address].toString()

        !address.first().isDigit() -> {
            addVariableSymbol(address)
            addressMemory = symbolsList[address].toString()
        }

        else -> addressMemory = address
    }
    return "0${Integer.toBinaryString(addressMemory.toInt()).padStart(15,'0')}"
}

private fun toCInstruction(instruction: String): String {
    val parts = instruction.split("=", ";")
    val hasDestination = instruction.contains("=")
    val hasJump = instruction.contains(";")

    val comp = if(hasDestination) parts[1] else parts[0].trim()
    val dest = if(hasDestination) parts[0].trim() else null
    val jmp = if(hasJump) parts.last().trim() else null

    return C_INSTRUCT_PREFIX + getComputationCode(comp) + getDestinationCode(dest) + getJumpCode(jmp)
}

private fun getComputationCode(computation: String): String {
    val aDigit = if(computation.contains("M")) "1" else "0"
    val computationCode = when (computation) {
        "0" -> "101010"
        "1" -> "111111"
        "-1" -> "111010"
        "D" -> "001100"
        "A" -> "110000"
        "M" -> "110000"
        "!D" -> "001101"
        "!A" ->  "110001"
        "!M" -> "110001"
        "-D" -> "001111"
        "-A" ->  "110011"
        "-M" ->  "110011"
        "D+1" -> "011111"
        "A+1" ->  "110111"
        "M+1" ->  "110111"
        "D-1" -> "001110"
        "A-1" ->  "110010"
        "M-1" ->  "110010"
        "D+A" ->  "000010"
        "D+M" ->  "000010"
        "D-A" ->  "010011"
        "D-M" ->  "010011"
        "A-D" ->  "000111"
        "M-D" ->  "000111"
        "D&A" ->  "000000"
        "D&M" ->  "000000"
        "D|A" ->  "010101"
        "D|M" ->  "010101"
        else -> throw IllegalArgumentException("Invalid computation instruction $computation")
    }
    return aDigit+computationCode
}

private fun getDestinationCode(dest: String?) =
    when(dest) {
        null -> "000"
        "M" -> "001"
        "D" -> "010"
        "DM" -> "011"
        "MD" -> "011"
        "A" -> "100"
        "AM" -> "101"
        "AD" -> "110"
        "ADM" -> "111"
        else -> throw IllegalArgumentException("Invalid destination instruction $dest")
    }

private fun getJumpCode(jmp: String?) =
    when(jmp) {
        null -> "000"
        "JGT" -> "001"
        "JEQ" -> "010"
        "JGE" -> "011"
        "JLT" -> "100"
        "JNE" -> "101"
        "JLE" -> "110"
        "JMP" -> "111"
        else -> throw IllegalArgumentException("Invalid JUMP instruction")
    }

private fun addLabelSymbol(line: String, address: Int){
    val parsedLine = line.replace("(", "").replace(")", "")
    symbolsList[parsedLine] = address
}

private fun addVariableSymbol(symbol: String) {
    symbolsList[symbol] = symbolAddress
    symbolAddress ++
}

