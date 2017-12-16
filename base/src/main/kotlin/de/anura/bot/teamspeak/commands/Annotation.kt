package de.anura.bot.teamspeak.commands

@Target(AnnotationTarget.CLASS)
annotation class CommandName(val name: String)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class CommandHelp(val help: String)