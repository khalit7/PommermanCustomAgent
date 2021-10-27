package players.CustomAgent.heuristics;

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
        int tick, nTeammates, nEnemies, blastStrength;
        boolean canKick;
        int nWoods;
        int ammo;
        static double maxWoods = -1;
        static double maxBlastStrength = 10;



        BoardStats(GameState gs) {
            nEnemies = gs.getAliveEnemyIDs().size();



                this.ammo = gs.getAmmo();
                nTeammates = gs.getAliveTeammateIDs().size();  // We only need to know the alive teammates in team modes
                nEnemies -= 1;  // In team modes there's an extra Dummy agent added that we don't need to care about


            // Save game state information
            this.tick = gs.getTick();
            this.blastStrength = gs.getBlastStrength();
            this.canKick = gs.canKick();

            // Count the number of wood walls
            this.nWoods = 1;
            for (Types.TILETYPE[] gameObjectsTypes : gs.getBoard()) {
                for (Types.TILETYPE gameObjectType : gameObjectsTypes) {
                    if (gameObjectType == Types.TILETYPE.WOOD)
                        nWoods++;
                }
            }
            if (maxWoods == -1) {
                maxWoods = nWoods;
            }
        }

        /**
         * Computes score for a game, in relation to the initial state at the root.
         * Minimizes number of opponents in the game and number of wood walls. Maximizes blast strength and
         * number of teammates, wants to kick.
         * @param futureState the stats of the board at the end of the rollout.
         * @return a score [0, 1]
         */
        double score(MultiObjectiveHeuristicCustom.BoardStats futureState, GameState gs, int objective)
        {

            switch (objective){
                case 0: // objective is safety
                    return safetyHeuristicEvaluation(gs);
                case 1: // objective is power up collection
                    return powerUpHeuristicEvaluation(futureState);
                case 3: // objective is : mid game tactic
                    return midGameHeuristicEvaluation(futureState,gs);
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
            /*if (win == Types.RESULT.INCOMPLETE) // give score if still alive
                score =0.1;*/
            return score;
        }
        private double powerUpHeuristicEvaluation(MultiObjectiveHeuristicCustom.BoardStats futureState){
            int diffWoods = - (futureState.nWoods - this.nWoods);
            int diffCanKick = futureState.canKick ? 1 : 0;
            if (this.canKick) {
                diffCanKick =  0;
            }
            int diffBlastStrength = futureState.blastStrength - this.blastStrength;

            double FACTOR_WOODS = 0.2;
            double FACTOR_CANKCIK = 0.4;
            double FACTOR_BLAST = 0.4;
            return (diffWoods / maxWoods) * FACTOR_WOODS
                    + diffCanKick * FACTOR_CANKCIK + (diffBlastStrength / maxBlastStrength) * FACTOR_BLAST;
        }
        private double midGameHeuristicEvaluation (MultiObjectiveHeuristicCustom.BoardStats futureState , GameState gs) {
            int diffWoods = - (futureState.nWoods - this.nWoods);
            int diffCanKick = futureState.canKick ? 1 : 0;
            if (this.canKick) {
                diffCanKick =  0;
            }
            int diffBlastStrength = futureState.blastStrength - this.blastStrength;
            int diffEnemies = - (futureState.nEnemies - this.nEnemies);
            int diffBombs = futureState.ammo - this.ammo;

            double FACTOR_WOODS = 0.2;
            double FACTOR_CANKCIK = 0.3;
            double FACTOR_BLAST = 0.3;
            double FACTOR_ENEMY = 0.1;
            double FACTOR_LAYBOMB = 0.2;


            boolean gameOver = gs.isTerminal();
            Types.RESULT win = gs.winner();

            double score= (diffEnemies / 3.0) * FACTOR_ENEMY + (diffWoods / maxWoods) * FACTOR_WOODS
                    + diffCanKick * FACTOR_CANKCIK + (diffBlastStrength / maxBlastStrength) * FACTOR_BLAST
                    + diffBombs * FACTOR_LAYBOMB;


            if(gameOver && win == Types.RESULT.LOSS) // if you loose
                score = -1;
            if(gameOver && win == Types.RESULT.WIN) // if you win
                score = 1;

            return score;
        }
    }
}