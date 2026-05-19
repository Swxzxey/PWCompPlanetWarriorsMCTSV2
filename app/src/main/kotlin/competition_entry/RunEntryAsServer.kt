package competition_entry

import games.planetwars.agents.GroupNAgents.MCTS.MCTSAgentV2
import json_rmi.GameAgentServer

fun main() {
    val server = GameAgentServer(port = 8080, agentClass = MCTSAgentV2::class)
    server.start(wait = true)
}
