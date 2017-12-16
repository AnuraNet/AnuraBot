package de.anura.bot.teamspeak.commands

import com.github.theholywaffle.teamspeak3.TS3Api

class CommandHandler(private val ts: TS3Api) {

    private val commands = listOf(Games(), Time())
    // todo add help command
    private val help by lazy { buildHelp() }

    fun handle(text: String): String {
        // Splitting the text of the message
        val (commandName, arguments) = text.split(' ', limit = 2)

        // Searching for the command, if we can't find it there will be a help message for the user
        val command = commands.find { command -> command.name.equals(commandName, true) } ?:
                return "Sorry I couldn't find a command with this name );\n $help"

        return command.handle(arguments.split(' '))
    }

    private fun buildHelp(): String {
        val commandList = commands.map { command ->
            "${command.name} - ${command.help}"
        }.joinToString { "\n" }

        return """
            Hey I'm a bot and I can do lots of cool things. These cool actions are listed blow.
            You have to transmit your wanted actions with the following syntax (without the '<' & '>'):
            <command> <sub command> <first argument> <second argument>

            These commands are available:
            $commandList

            Have fun with this bot ;)
        """.trimIndent()
    }

}