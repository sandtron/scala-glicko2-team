# Scala implementation of the Glicko-2 system

A standalone application for managing a set of players given a history of games.

> The Glicko rating system and Glicko-2 rating system are methods for assessing a player's strength in games of skill, such as chess and go.

[Glicko2 on Wikipedia](https://en.wikipedia.org/wiki/Glicko_rating_system)

> The Glicko-2 rating system is intended to estimate the skill of player performance in head-to-head competition in a variety of games.

[Description of Glicko-2 system](http://www.glicko.net/ratings/glicko2desc.pdf)

For more details, please, look at [the Mark Glickman site](http://www.glicko.net/)
and the [example of the Glicko-2 system](http://www.glicko.net/glicko/glicko2.pdf)

## Team implementation

Team implementation is based off of information from [rhetoricstudios.com paper](http://rhetoricstudios.com/downloads/AbstractingGlicko2ForTeamGames.pdf)

## Requirements

Java must be installed on the system.
Scala is required for building.

## Building

`sbt assembly` will build the app into ./target...

build.sh will compile the app and move it to the glicko2team directory

## Usage

- Add matches to match-history.csv
- Configure using glicko2team.properties
  
```bash
## pass in the properties file
sh ./buildAndRun.sh glicko2team.properties
## OR the script will look for it in the same directory as the script.
sh ./buildAndRun.sh
```

## Configuration

The app is configured using a properties file. By default it is glicko2team.properties.

`logback.path`
the logback configuration

`matchSource.path`
Path to the match history csv file

`matchSource.csv.dateFormat`
Date format of date/time for match times in the matchSource csv (uses java formatting syntax)
See [SimpleDateformat](https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html)

`glicko2.team.ratingPeriod.games`
Rating period can be either in days or number of games the number of games within a rating period.

`player settings`
player.default.rating=1500
player.default.deviation=350
player.default.volatility=0.06

`glicko2.team.algorithm`
the glicko2 algorithm for team evaluation.

> COMPOSITE (default)

The Composite Opponent Update Method considers each outcome as a match against asingle player possessing the average rating and deviation of the opposing team players. Ineffect, the update method creates a composite player out of the opposing team and uses thisplayer’s resulting rating and deviation when updating a player.8

For example, if Alice competes against a team consisting of Bob, Carol, Dan, and Eve, the game will update Alice’s rating by considering the outcome as a 1­versus­1 match againsta composite player having the average rating and deviation of Bob, Carol, Dan, and Eve.

> INDIVIDUAL

The Individual Update Method takes the naive approach to updating players after a game. This method treats each match outcome as a set of wins or losses, one for each member of the opposing team. This method takes advantage of rating systems like Elo or Glicko by abstracting a team match as a set of 1­versus­1 matches.

For example, consider a game where Alice participates on a team versus an opposing team consisting of Bob, Carol, Dan, and Eve. If Alice’s team wins, the game updates Alice’srating by considering this game as four wins: one against Bob, one against Carol, one againstDan, and one against Eve.



## Special thanks

[rhetoricstudios.com](http://rhetoricstudios.com)

This software builds upon the work of [Andriy Kuba](https://github.com/andriykuba) who developed the initial scala-Glicko2 implementation that is used within within this application.
