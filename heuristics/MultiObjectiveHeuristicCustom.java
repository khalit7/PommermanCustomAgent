package players.PommermanCustomAgent.heuristics;

import core.GameState;
import objects.Bomb;
import objects.GameObject;
import players.PommermanCustomAgent.Djikstra;
import utils.Types;
import utils.Vector2d;
import players.PommermanCustomAgent.Djikstra.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import static java.lang.Math.max;
import static java.lang.Math.min;


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
        int closestPowerUpDistance;
        int closestWoodDistance;
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


            ////
            Vector2d myPosition = gs.getPosition();
            Types.TILETYPE[][] board = gs.getBoard();
            int depth = 10;
            int closestPowerUpDistance=10000;
            int closestWoodDistance=10000;
            // get closest powerup using Manhattan distance
            for(int x = max(0, myPosition.x - depth); x < min(board.length, myPosition.x + depth); x++){
                for(int y = max(0, myPosition.y - depth); y < min(board.length, myPosition.y + depth); y++){

                    Types.TILETYPE type = board[y][x];

                    if( type == Types.TILETYPE.EXTRABOMB ||
                            type == Types.TILETYPE.INCRRANGE ||
                            (type == Types.TILETYPE.KICK && this.canKick ==false )){

                        Vector2d distVector = new Vector2d(Math.abs(myPosition.x-x),Math.abs(myPosition.y-y));
                        int manhattanDisatance = Math.abs(distVector.x) + Math.abs(distVector.y);
                        if (manhattanDisatance < closestPowerUpDistance) closestPowerUpDistance =manhattanDisatance;
                    }
                    if( type == Types.TILETYPE.WOOD ){

                        Vector2d distVector = new Vector2d(Math.abs(myPosition.x-x),Math.abs(myPosition.y-y));
                        int manhattanDisatance = Math.abs(distVector.x) + Math.abs(distVector.y);
                        if (manhattanDisatance < closestWoodDistance) closestWoodDistance =manhattanDisatance;
                    }

                }
            }
            this.closestPowerUpDistance = closestPowerUpDistance;
            this.closestWoodDistance = closestWoodDistance;
            ////
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
