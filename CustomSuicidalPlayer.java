package players.PommermanCustomAgent;

import core.GameState;
import players.DoNothingPlayer;
import players.Player;
import utils.Types;

// this is a dummy player just to enable the 1-on-1 game mode.
public class CustomSuicidalPlayer extends Player {
    public CustomSuicidalPlayer(int pId) {
        super(0, pId);
    }

    @Override
    public Types.ACTIONS act(GameState gs) {

        return Types.ACTIONS.ACTION_BOMB;
    }

    @Override
    public int[] getMessage() {
        // default message
        return new int[Types.MESSAGE_LENGTH];
    }

    @Override
    public Player copy() {
        return new DoNothingPlayer(playerID);
    }
}
