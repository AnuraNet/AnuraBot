# AnuraBot
The Teamspeak Bot for the Anura network. 
It assigns users of your Teamspeak server groups for their online time and also groups for their games on Steam.

This bot supports Teamspeak clients using version 3 and also clients using version 5.

![Cards Icon](icon/cards@0.5.png) 

## Building
Just clone this project and build it with Maven:
```
mvn package
```

## Running
You can run two versions of this bot:

### Built with web server
If you don't know which version to use, **use this one**.  
It includes everything you need to run the bot and use all of its features.
To get the jar for this version go to the folder `web/target` and use the 
`jar-with-dependencies`.

### Built without web server
I recommend this version if you already integrated Steam authentication
into your website. Thats why this version is built without the web server.
You can find the `jar-with-dependencies` in the `base/target` folder.

### First start

Copy the `jar-with-dependencies` of your selected version into a direction used for the bot.
Now start the bot with 
```bash
java -jar anura-bot-1.0-SNAPSHOT.jar
```
A new config file should appear in this folder and the program will stop after that.
Then edit the file (it's usally called `config.ini`) and start the jar again.

You may want to interact with the bot. To do this you have to assign yourself a permission. 
After the first run go to the database table `ts_user`, search for your 
`uid` (The unique id of your Teamspeak identity) and set the `permission` column to `1`. 
Restart and enjoy controlling the bot via chat.

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
If something is wrong or missing feel free to open an issue, or a pull request. 

### Updating dependencies

To check if there are any dependency updates available run,
```bash
mvn versions:display-dependency-updates
```