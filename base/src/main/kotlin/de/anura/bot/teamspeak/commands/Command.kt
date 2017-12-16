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
                .associateBy { it.name }
    }

    private fun generateHelp(): String {
        // Generating a list of a sub commands with their arguments and their description
        val subCommands = subs.values.map {
            val arguments = it.arguments.map { "<${it.name}>" }.joinToString { " " }
            return "${it.name} $arguments - ${it.help}"
        }.joinToString { "\n" }

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
        val command = subs[arguments[0]] ?: return compiledHelp

        // Calling the function of the sub command, if the result is null, we tell the user that something went wrong
        val call = command.function.call() ?: return "Something went wrong );"

        // If the result isn't a string, we convert it to one and send it back
        return call as? String ?: "Success: $call"
    }

    data class SubCommand(val function: KFunction<*>, val name: String, val help: String, val arguments: List<Argument>)

    data class Argument(val name: String, val type: KType)

}