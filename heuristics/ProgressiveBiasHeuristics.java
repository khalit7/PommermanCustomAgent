package players.PommermanCustomAgent.heuristics;

import core.GameState;
import utils.Types;
import utils.Vector2d;

import java.util.ArrayList;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class ProgressiveBiasHeuristics {
static double biasConstant = 3.0; //TODO: try different values for this parameter
    public static double heuristic1 (GameState gs , int action, int objective) {
        if (objective == 3) // objective is safety
        {
            // give high value for the actions that takes you out of danger
            int depth = 5 ; // look at a 5*5 grid around you
            Vector2d myPosition = gs.getPosition();
            Types.TILETYPE [][] board = gs.getBoard();
            int num_thrests_right=0;
            int num_thrests_left=0;
            int num_thrests_up=0;
            int num_thrests_down=0;
            for (int r = max(0, myPosition.x - depth); r < min(board.length, myPosition.x + depth); r++) {
                for (int c = max(0, myPosition.y - depth); c < min(board.length, myPosition.y + depth); c++) {
                    Types.TILETYPE tile = board[c][r];
                    if (tile == Types.TILETYPE.BOMB || tile == Types.TILETYPE.FLAMES) { // this is a threat
                        if (myPosition.x > r) num_thrests_left+=1 ;
                        if (myPosition.x < r) num_thrests_right+=1 ;
                        if (myPosition.y > c) num_thrests_up+=1 ;
                        if (myPosition.y < c) num_thrests_down+=1 ;
                    }
                }
                }

            switch (action){
                case 1: // action up
                    return biasConstant* (num_thrests_down - num_thrests_up);
                case 2: // action down
                    return biasConstant* (num_thrests_up - num_thrests_down);
                case 3: // action left
                    return biasConstant* (num_thrests_right-num_thrests_left);
                case 4: // action right
                    return biasConstant* (num_thrests_left-num_thrests_right);
                case 5: // action bomb
                    return -biasConstant; // when objective is safety, it's better to lay less bombs
                case 0: // action stop
                    return  0;
                default: // should never be here !
                    return 0;
            }


        }
        else return 0;
    }
    //TODO: maybe add other progressive bias techniques here
}
