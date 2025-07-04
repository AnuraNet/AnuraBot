package de.anura.bot.teamspeak.commands

import de.anura.bot.teamspeak.UserInfo
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.cast
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredFunctions

abstract class Command {

    val name: String by lazy { classAnnotation(CommandName::class).name }
    val help: String by lazy { classAnnotation(CommandHelp::class).help }
    private val subCommands: Map<String, SubCommand> by lazy { searchSubCommands() }

    @Suppress("UNCHECKED_CAST")
    private fun <A : Annotation> classAnnotation(clazz: KClass<A>): A {
        val matching = this::class.annotations
            .filter { clazz.isInstance(it) }
            .map { clazz.cast(it) }

        if (matching.isEmpty()) {
            throw IllegalArgumentException("No @${clazz.simpleName} annotation")
        }

        return matching[0]
    }

    private fun searchSubCommands(): Map<String, SubCommand> {
        val userInfoKType = UserInfo::class.createType()

        return this::class.declaredFunctions
            // Trying to find the CommandHelp annotation
            .map { function ->
                val annotation = function.annotations
                    .filterIsInstance<CommandHelp>()
                    .map { annotation -> annotation }
                    .firstOrNull()

                Pair(function, annotation)
            }
            // When there's no annotation, we'll filter it out
            .filter { pair -> pair.second != null }
            // Adding the arguments and transforming the annotation into a sub command
            .map { pair ->
                val help = (pair.second as CommandHelp).help
                val allArguments = pair.first.parameters
                    .filter { it.kind == KParameter.Kind.VALUE }
                    .map { Argument(it.name ?: "", it.type) }

                val userArguments = allArguments.filter { it.type != userInfoKType }

                SubCommand(pair.first, pair.first.name, help, allArguments, userArguments)
            }
            // Mapping it with the name as key
            .associateBy { it.name.lowercase() }
    }

    /**
     * Generates a help message for a sub command
     */
    private fun generateSubHelp(cmd: SubCommand, userInfo: UserInfo): String {

        val arguments = if (cmd.userArguments.isNotEmpty())
            cmd.userArguments.joinToString(separator = " ", postfix = " ") { "<${it.name}>" }
        else
            ""

        return userInfo.code("${cmd.name} $arguments") + " - ${cmd.help}"
    }

    /**
     * Generates a help message for the whole command (including all sub commands)
     */
    private fun generateHelp(userInfo: UserInfo): String {
        // Generating a list of a sub commands with their arguments and their description
        // The space is needed for trimIndent()
        val subCommands =
            subCommands.values.joinToString(separator = "\n            ") { generateSubHelp(it, userInfo) }

        return """
            $name
            $help

            Possible sub commands:
            $subCommands
        """.trimIndent()
    }


    fun handle(userInfo: UserInfo, arguments: List<String>): String {

        // No sub command is specified => Help
        if (arguments.isEmpty()) {
            return generateHelp(userInfo)
        }

        // Trying to find the sub command, if not => Help
        val command = subCommands[arguments[0].lowercase()] ?: return generateHelp(userInfo)

        // If there are too few arguments => Help
        if (command.userArguments.size > arguments.size - 1) {
            return "Wrong parameters: $name ${generateSubHelp(command, userInfo)}"
        }

        // Keeping track of the correct index for the userArguments
        var userArgumentIndex = 1

        // Converting the string arguments to the right data type
        val typedArguments = command.allArguments.mapIndexedNotNull { _, required ->
            // The class of the parameter for the method
            val classifier = required.type.classifier ?: return@mapIndexedNotNull null

            // This command wants information about the user
            if (classifier == UserInfo::class) {
                return@mapIndexedNotNull userInfo
            }

            // The string which has to be transformed
            // We add one to the index because we don't want the sub command name
            val given = arguments[userArgumentIndex++]

            val any: Any = when (classifier) {
                Int::class -> given.toIntOrNull()
                Long::class -> given.toLongOrNull()
                Double::class -> given.toDoubleOrNull()
                Float::class -> given.toFloatOrNull()
                Boolean::class -> given.toBoolean()
                Byte::class -> given.toByteOrNull()
                Short::class -> given.toShortOrNull()
                else -> given
            } ?: return "Please check your input. I couldn't convert everything to the right data type."

            any
        }.toMutableList()

        // Add the instance of the class to the arguments
        typedArguments.add(0, this)

        // Calling the function of the sub command, if the result is null, we tell the user that something went wrong
        val call = command.function.call(*typedArguments.toTypedArray()) ?: return "Something went wrong );"

        // If the result isn't a string, we convert it to one and send it back
        return call as? String ?: "Success: $call"
    }

    data class SubCommand(
        val function: KFunction<*>, val name: String, val help: String,
        val allArguments: List<Argument>, val userArguments: List<Argument>
    )

    data class Argument(val name: String, val type: KType)

}
