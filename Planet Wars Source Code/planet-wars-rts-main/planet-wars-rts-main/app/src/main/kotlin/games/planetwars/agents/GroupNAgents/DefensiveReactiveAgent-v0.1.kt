package games.planetwars.agents.GroupNAgents

import games.planetwars.agents.Action
import games.planetwars.agents.PlanetWarsPlayer
import games.planetwars.core.*

class DefensiveReactiveAgent() : PlanetWarsPlayer() { //New agent defined as subclass of PlanetWarsPlayer()
    private val halfwayLine = params.width / 2 //Get the halfway line of the game space to determine which side we are defending

    override fun getAction(gameState: GameState): Action { //Override getAction from original class, pass gameState in and return action.

        // This agent has no random chance. Is made to choose the source as the planet with the most ships out of the ones owned.
        // The target is to be determined by multiple arguments. We first go through all neutral planets on our side an capture any remaining, we then
        // go through a list of all of our owned planets and find if any of the planets on our side are going to be taken over by the enemy
        // (judged by net transporter weights being sent) we will target it with optimal amount of ships.
        //
        // Possible issues: Multiple actions per game tick? Not accounting for transporters being sent in current action in calculations so
        // too many transporters sent to one planet to defend still.
        // Too slow in decision making, looks through too much, not making enough actions per tick.

        //Define planets on our side.

        var halfPlanets: List<Planet>

        if (player == Player.Player1) { //If Player1, choose from planets on left side.
            halfPlanets = gameState.planets.filter {it.position.x <= halfwayLine}
            //Filter for planets on left half of screen where the agent owns it or is neutral
        }

        else { //If Player2, choose from planets on right side
            halfPlanets = gameState.planets.filter{ it.position.x >= halfwayLine}
            //Filter for planets on right half of screen where the agent owns it or is neutral
        }

        // Identify owned planets not currently in-action
        val myPlanets = gameState.planets.filter{ it.owner == player && it.transporter == null} //Identify planets owned by player not doing an action
        if (myPlanets.isEmpty()) { //If no planets returned, no action
            return Action.doNothing()
        }

        // Identify enemy planets
        val enemyPlanets = gameState.planets.filter{it.owner == player.opponent()} //Identify planets owned by enemy
        // Identify enemy planets in-action
        val attackingEnemyPlanets = enemyPlanets.filter{it.transporter != null}
        val source = myPlanets.maxBy{it.nShips}

        if (source.nShips <= 5) {
            return Action.doNothing()
        }

        val myAttackingPlanets = gameState.planets.filter{it.owner == player && it.transporter != null && it != source}
        // myPlanets = myPlanets.filter{it != source}


        val neutralPlanets = halfPlanets.filter{it.owner == Player.Neutral}
        if (! neutralPlanets.isEmpty()) {
            return Action(player, source.id, neutralPlanets.minBy{it.position.distance(source.position)}.id, source.nShips/2)
        }

        //Return list of both players planets' nships transporter weight who are targetting the input planet.
        fun bothPlayerTransporterTravellingFromWeights(planet: Planet, myPlanets: List<Planet>, enemyPlanets: List<Planet>): List<Double> {
            return myPlanets.filter{it.transporter?.destinationIndex == planet.id}.mapNotNull{it.transporter?.nShips} + enemyPlanets.filter{it.transporter?.destinationIndex == planet.id}.mapNotNull{it.transporter?.nShips}.map{-it}
        }

        for (planet in halfPlanets.filter{it.owner == player}) {
            // If there is transporters moving towards the planet and the net transporter weight is negative.
            val planetsAttackTarget = bothPlayerTransporterTravellingFromWeights(planet, myAttackingPlanets, attackingEnemyPlanets)
            if (planetsAttackTarget.size != 0) {
                //If the source ship can send enough ships to defend planet, send them.
                if (planetsAttackTarget.sum()+planet.nShips <= 0) {
                    if (source.nShips > ((planetsAttackTarget.sum() * -1) - planet.nShips)) {
                        return Action(player, source.id, planet.id, kotlin.math.abs(planetsAttackTarget.sum()) + 1)
                    }
                    else {
                        return Action(player, source.id, planet.id, source.nShips / 2)
                    }
                }
            }
        }

        // Can define source after the target using a heuristic for closet + highest nships to target.


        // Filter for planets that belong to opponent or are neutral.
        if (enemyPlanets.isEmpty()) {
            return Action.doNothing() // if there are no planets that belong to opponent or neutral, do nothing
        }
        val target = enemyPlanets.minBy{it.position.distance(source.position)}
        return Action(player, source.id, target.id, source.nShips/2)
        //Return action command with appropriate collected information
    }

    override fun getAgentType(): String {
        return "Defensive Random Agent 2.0"
    }

}



    fun main() {
    val agent = DefensiveRandomAgent()
    agent.prepareToPlayAs(Player.Player1, GameParams())
    val gameState = GameStateFactory(GameParams()).createGame()
    val action = agent.getAction(gameState)
    println(action)
}