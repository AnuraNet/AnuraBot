package de.anura.bot.teamspeak.commands

class CommandHandler {

    private val commands = listOf(Games(), Time(), TimeGroupCmd(), Perms())
    private val help by lazy { buildHelp() }

    fun handle(text: String): String {
        // Splitting the text of the message
        val array = text.split(' ')
        val commandName = array[0]

        // When the user wants help, he gets help
        if (commandName.equals("help", true)) {
            return help
        }

        // Searching for the command, if we can't find it there will be a help message for the user
        val command = commands.find { command -> command.name.equals(commandName, true) } ?:
                return "Sorry I couldn't find a command with this name );\n $help"

        return command.handle(array.subList(1, array.size))
    }

    private fun buildHelp(): String {
        // The space is needed for trimIndent to work
        val commandList = commands.joinToString(separator = "\n            ") {
            "[b]${it.name}[/b] - ${it.help}"
        }

        return """
            Hey I'm a bot and I can do lots of cool things. These cool actions are listed blow.
            You have to transmit your wanted actions with the following syntax (without the '<' & '>'):
            <command> <sub command> <first argument> <second argument>

            These commands are available:
            $commandList

            Have fun with me ;)
        """.trimIndent()
    }

}