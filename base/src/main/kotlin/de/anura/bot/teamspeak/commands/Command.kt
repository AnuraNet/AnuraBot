package de.anura.bot.teamspeak.commands

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.functions
import kotlin.reflect.full.primaryConstructor

abstract class Command {

    val name: String by lazy { constructorAnnotation(CommandName::class).name }
    val help: String by lazy { constructorAnnotation(CommandHelp::class).help }
    private val subs: Map<String, SubCommand> by lazy { listSubs() }
    private val compiledHelp by lazy { generateHelp() }

    @Suppress("UNCHECKED_CAST")
    private fun <A : Annotation> constructorAnnotation(clazz: KClass<A>): A {
        val annotations = this::class.primaryConstructor?.annotations
        val matching = annotations
                ?.mapNotNull { annotation -> annotation as? A }
                .orEmpty()

        if (matching.isEmpty()) {
            throw IllegalArgumentException("No @${clazz.simpleName} annotation")
        }

        return matching[0]
    }

    private fun listSubs(): Map<String, SubCommand> {
        return this::class.functions
                // Selecting only functions which return strings
                .filter { function -> function.returnType == String::class }
                // Trying to find the CommandHelp annotation
                .map { function ->
                    val annotation = function.annotations
                            .filter { annotation -> annotation is CommandHelp }
                            .map { annotation -> annotation as CommandHelp }
                            .firstOrNull()

                    Pair(function, annotation)
                }
                // When there's no annotation, we'll filter it out
                .filter { pair -> pair.second != null }
                // Adding the arguments and transforming the annotation into a sub command
                .map { pair ->
                    val help = (pair.second as CommandHelp).help
                    val arguments = pair.first.parameters
                            .map { parameter -> Argument(parameter.name ?: "", parameter.type) }

                    SubCommand(pair.first, pair.first.name, help, arguments)
                }
                // Mapping it with the name as key
                .associateBy { it.name.toLowerCase() }
    }

    /**
     * Generates a help message for a sub command
     */
    private fun generateSubHelp(cmd: SubCommand): String {
        val arguments = cmd.arguments.map { "<${cmd.name}>" }.joinToString { " " }
        return "${cmd.name} $arguments - ${cmd.help}"
    }

    /**
     * Generates a help message for the whole command (including all sub commands)
     */
    private fun generateHelp(): String {
        // Generating a list of a sub commands with their arguments and their description
        val subCommands = subs.values.map { generateSubHelp(it) }.joinToString { "\n" }

        return """
            $name
            $help

            Possible sub commands:
            $subCommands
        """.trimIndent()
    }


    fun handle(arguments: List<String>): String {

        // No sub command is specified => Help
        if (arguments.isEmpty()) {
            return compiledHelp
        }

        // Trying to find the sub command, if not => Help
        val command = subs[arguments[0].toLowerCase()] ?: return compiledHelp

        // If there are too few arguments => Help
        if (command.arguments.size < arguments.size) {
            return "Wrong parameters: $name ${generateSubHelp(command)}"
        }

        // Converting the string arguments to the right data type
        val typedArguments = command.arguments.mapIndexedNotNull { index, required ->
            // The class of the parameter for the method
            val classifier = required.type.classifier ?: return@mapIndexedNotNull null
            // The string which has to be transformed
            val given = arguments[index]

            val any: Any = when (classifier) {
                Int::class -> given.toIntOrNull()
                Long::class -> given.toLongOrNull()
                Double::class -> given.toDoubleOrNull()
                Float::class -> given.toFloatOrNull()
                Boolean::class -> given.toBoolean()
                Byte::class -> given.toByteOrNull()
                Short::class -> given.toShortOrNull()
                else -> classifier
            } ?: return "Please check your input. I couldn't convert everything to the right data type."

            any
        }.toTypedArray()

        // Calling the function of the sub command, if the result is null, we tell the user that something went wrong
        val call = command.function.call(*typedArguments) ?: return "Something went wrong );"

        // If the result isn't a string, we convert it to one and send it back
        return call as? String ?: "Success: $call"
    }

    data class SubCommand(val function: KFunction<*>, val name: String, val help: String, val arguments: List<Argument>)

    data class Argument(val name: String, val type: KType)

}