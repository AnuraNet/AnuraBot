package de.anura.bot.teamspeak.commands

import de.anura.bot.teamspeak.UserInfo

class CommandHandler {

    private val commands = listOf(Games(), Time(), TimeGroupCmd(), Perms(), Users())

    fun handle(text: String, userInfo: UserInfo): String {
        // Splitting the text of the message
        val array = text.split(' ')
        val commandName = array[0]

        // When the user wants help, he gets help
        if (commandName.equals("help", true)) {
            return buildHelp(userInfo)
        }

        // Searching for the command, if we can't find it there will be a help message for the user
        val command = commands.find { command -> command.name.equals(commandName, true) }
            ?: return "Sorry I couldn't find a command with this name );\n${buildHelp(userInfo)}"

        return command.handle(userInfo, array.subList(1, array.size))
    }

    private fun buildHelp(userInfo: UserInfo): String {
        // The space is needed for trimIndent to work
        val commandList = commands.joinToString(separator = "\n            ") {
            userInfo.bold(it.name) + " - ${it.help}"
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
