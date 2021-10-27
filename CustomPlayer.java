package players.CustomAgent;

import core.GameState;
import players.optimisers.ParameterizedPlayer;
import players.Player;
import utils.ElapsedCpuTimer;
import utils.Types;
import utils.Vector2d;

import java.util.ArrayList;
import java.util.Random;

public class CustomPlayer extends ParameterizedPlayer {

    /**
     * Random generator.
     */
    private Random m_rnd;

    /**
     * All actions available.
     */
    public Types.ACTIONS[] actions;

    /**
     * Params for this MCTS
     */
    public CustomMCTSParams params;

    // custom made parameters
    private int safetythreshold=10;

    public CustomPlayer(long seed, int id) {
        this(seed, id, new CustomMCTSParams(),10);
    }

    public CustomPlayer(long seed, int id, int safetythreshold) {
        this(seed, id, new CustomMCTSParams(),safetythreshold);
    }

    public CustomPlayer(long seed, int id, CustomMCTSParams params, int safetythreshold) {
        super(seed, id, params);
        reset(seed, id);

        ArrayList<Types.ACTIONS> actionsList = Types.ACTIONS.all();
        actions = new Types.ACTIONS[actionsList.size()];
        int i = 0;
        for (Types.ACTIONS act : actionsList) {
            actions[i++] = act;
        }
        this.safetythreshold = safetythreshold;
    }

    @Override
    public void reset(long seed, int playerID) {
        super.reset(seed, playerID);
        m_rnd = new Random(seed);

        this.params = (CustomMCTSParams) getParameters();
        if (this.params == null) {
            this.params = new CustomMCTSParams();
            super.setParameters(this.params);
        }
    }

    @Override
    public Types.ACTIONS act(GameState gs)  {
        // TODO update gs
        if (gs.getGameMode().equals(Types.GAME_MODE.TEAM_RADIO)){
            int[] msg = gs.getMessage();
        }

        ElapsedCpuTimer ect = new ElapsedCpuTimer();
        ect.setMaxTimeMillis(params.num_time);

        // Number of actions available
        int num_actions = actions.length;

        // identify the objective we are trying to achive from this game state
        int objective = identifyObjective(gs);
        //TODO: depending on the objective ... modify params
        //System.out.println(objective);
        switch (objective){
            case 0: // pure_safety
                params.rollout_depth=10;
                break;
                case 1: // pure_power_up_collection
                    params.rollout_depth=10;
                    break;
            case 2: // endgame tactic
                params.rollout_depth=12;
                params.heuristic_method = params.ADVANCED_HEURISTIC;
                break;
            case 3: // mid game tactic
                params.rollout_depth = 8;
                break;

        }
        //System.out.println("check " + (params.heuristic_method == params.MULTI_OBJECTIVE_HEURISTIC));
        // Root of the tree

        CustomSingleTreeNode m_root = new CustomSingleTreeNode(params, m_rnd, num_actions, actions,objective);
        m_root.setRootGameState(gs);

        //Determine the action using MCTS...
        m_root.mctsSearch(ect);

        //Determine the best action to take and return it.
        int action = m_root.mostVisitedAction();
        //int action = m_root.bestAction();

        // TODO update message memory

        //... and return it.
        return actions[action];
    }
    private int identifyObjective(GameState gs) {
        // 0:pure_safety
        // 1:pure_powerUpCollection
        // 2: end game
        // 3: mid game
        Boolean is_safe = evaluateSafety(gs,3); // returns True if no threat is nearby, false otherwise
        //System.out.println(is_safe);
        if (is_safe && gs.getTick() <250) // if safe and at beginning of game .. objective => pure power up collection
            return 1;
        else if (!is_safe && gs.getTick() <250) // if not safe and beginning of the game .. objective => pure safety
            return 0;
        else if (gs.getTick()>600) // end of the game ... objective => end game tactic
            return 2;
        else  // middle of the game ... mid game tactic
            return 3;




    }
private boolean evaluateSafety(GameState gs,int safetythreshold) {
    Vector2d myPosition = gs.getPosition();

    Types.TILETYPE[][] board = gs.getBoard();

    int boardSizeX = board.length;
    int boardSizeY = board[0].length;
    ArrayList<Types.TILETYPE> enemiesObs = gs.getAliveEnemyIDs();

    ArrayList<Vector2d> threats = new ArrayList<>(); // either enmey or a bomb

    for (int x = 0; x < boardSizeX; x++) {
        for (int y = 0; y < boardSizeY; y++) {
            Types.TILETYPE type = board[y][x];

            if(type == Types.TILETYPE.BOMB || type == Types.TILETYPE.FLAMES) {
                threats.add(new Vector2d(x, y)); // add the position of the bomb
            }/*else if(Types.TILETYPE.getAgentTypes().contains(type) &&
                    type.getKey() != gs.getPlayerId()) // maybe an enemmy
            {
                if(enemiesObs.contains(type)) // it is an enemy
                {
                    threats.add(new Vector2d(x, y)); // add the position of the enemy
                }
            }*/
        }
    }

    // check if there is a nearby threat using Manhattan distance
    int minManhattanDisatance = 1000 ;// very large number
    for (int i=0;i<threats.size();i++) {
        Vector2d distVector = myPosition.subtract(threats.get(i));
        int manhattanDisatance = Math.abs(distVector.x) + Math.abs(distVector.y);
        if (manhattanDisatance < minManhattanDisatance) minManhattanDisatance = manhattanDisatance;
    }
    if (minManhattanDisatance<safetythreshold) return false; // NOT SAFE .. SOME THREAT IS NEAR !!
    else return true ;// safe .. no threats are close

}
    @Override
    public int[] getMessage() {
        // default message
        int[] message = new int[Types.MESSAGE_LENGTH];
        message[0] = 1;
        return message;
    }

    @Override
    public Player copy() {
        return new CustomPlayer(seed, playerID, params,safetythreshold);
    }

}
