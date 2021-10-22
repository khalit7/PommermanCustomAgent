package players.CustomAgent.heuristics;

import core.GameState;

public abstract class CustomStateHeuristic {
    public abstract double evaluateState(GameState gs,int objective) ;
}
