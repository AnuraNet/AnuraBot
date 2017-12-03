# AnuraBot
The Teamspeak Bot for the Anura network. 
It can give users of your Teamspeak who visited it for certain amount of time a special rank 
and groups for games they own on Steam.

Currently **under heavy development** so don't use it or it'll break everything ;)

![Cards Icon](icon/cards@0.5.png) 

## How to use

### Building
Just clone this project and build it with Maven:
```
mvn package
```

### Running
You can run two versions of this bot:

##### Built with web server
If you don't know which version to use, **use this one**. 
It includes everything you need to run the bot and use all of its features.
To get the jar for this version go to the folder `web/target` and use the 
`jar-with-dependencies`.

##### Built without web server
I recommend this version if you already integrated Steam authentication
onto your website. Thats why this version is built without the web server.
You can find the `jar-with-dependencies` in the `base/target` folder.

Copy the `jar-with-dependencies` of your selected version into a direction used for the bot.
Now start the bot with 
```bash
java -jar anura-bot-1.0-SNAPSHOT.jar
```
A new config file should appear in this folder and the program will stop after that.
Then edit the file (it's usally called `config.ini`) and start the jar again.

Congratulations your bot is now up and running.
If you encounter any issues feel free to report them.

## Libraries
We're using the following libraries for this project:
### Basics
* [Kotlin](https://kotlinlang.org)
* [ini4j](http://ini4j.sourceforge.net)
* [Jdbi](http://jdbi.org)
* [Teamspeak3 Java API](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API)
### Steam Web Authentification
* [http4k](https://www.http4k.org) with [Netty](http://netty.io)
* [OpenId for Java](https://github.com/jbufu/openid4java)

## Icon
Our icon is a creation of Cody and published on [Material Design Icons](https://materialdesignicons.com/icon/cards).
Thanks Cody!

## Contribute
If something is wrong or missing feel free to open a issue or a pull request. 