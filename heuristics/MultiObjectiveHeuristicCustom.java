package players.PommermanCustomAgent.heuristics;

import core.GameState;
import utils.Types;


public class MultiObjectiveHeuristicCustom extends CustomStateHeuristic {
    private MultiObjectiveHeuristicCustom.BoardStats rootBoardStats;

    public MultiObjectiveHeuristicCustom(GameState root_gs){
        rootBoardStats = new MultiObjectiveHeuristicCustom.BoardStats(root_gs);
    }
    @Override
    public double evaluateState(GameState gs,int objective) {
        MultiObjectiveHeuristicCustom.BoardStats lastBoardState = new MultiObjectiveHeuristicCustom.BoardStats(gs);
        double score = rootBoardStats.score(lastBoardState, gs,objective);
        return score;

    }


    // board stats class
    public static class BoardStats
    {




        BoardStats(GameState gs) {


        }


        double score(MultiObjectiveHeuristicCustom.BoardStats futureState, GameState gs, int objective)
        {

            switch (objective){
                case 3: // objective is safety
                    return safetyHeuristicEvaluation(gs);
                    //TODO: maybe add other game tactics if appropiate
                default:
                    return 0; // shouldn't be here
            }

        }
        private double safetyHeuristicEvaluation(GameState gs){
            boolean gameOver = gs.isTerminal();
            Types.RESULT win = gs.winner();

            double score=0;
            if(gameOver && win == Types.RESULT.LOSS) // if you loose
                score = -1;
            if(gameOver && win == Types.RESULT.WIN) // if you win
                score = 1;
            return score;
        }

    }
}
