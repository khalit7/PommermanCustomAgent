package players.CustomAgent;

import com.sun.org.apache.xpath.internal.operations.Bool;
import core.GameState;
import objects.Bomb;
import players.CustomAgent.MCTSParams;
import players.CustomAgent.CustomPlayer;
import players.CustomAgent.SingleTreeNode;
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
    public MCTSParams params;

    // custom made parameters
    private int safetythreshold=10;

    public CustomPlayer(long seed, int id) {
        this(seed, id, new MCTSParams(),10);
    }

    public CustomPlayer(long seed, int id, int safetythreshold) {
        this(seed, id, new MCTSParams(),safetythreshold);
    }

    public CustomPlayer(long seed, int id, MCTSParams params,int safetythreshold) {
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

        this.params = (MCTSParams) getParameters();
        if (this.params == null) {
            this.params = new MCTSParams();
            super.setParameters(this.params);
        }
    }

    @Override
    public Types.ACTIONS act(GameState gs) {
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
        //System.out.println("this is the objective : " + objective);
        // Root of the tree
        SingleTreeNode m_root = new SingleTreeNode(params, m_rnd, num_actions, actions,objective);
        m_root.setRootGameState(gs);

        //Determine the action using MCTS...
        m_root.mctsSearch(ect);

        //Determine the best action to take and return it.
        int action = m_root.mostVisitedAction();

        // TODO update message memory

        //... and return it.
        return actions[action];
    }
    private int identifyObjective(GameState gs) {
        // 0:safety
        // 1:powerUpCollection
        // 2: MCTS normal play with advanced heuristic
        Boolean is_safe = evaluateSafety(gs,4); // returns True if no threat is nearby, false otherwise
        System.out.println(is_safe);
        if (is_safe) return 1;
        else // if not safe
         {
            // either seek shelter
            // or fight back
        // seek shelter ... maybe this happens at the beginning of the game or when I have no good powerups
             return 0;
         // fight back ... maybe towards the end of the game or if I have enough powerups
           // return 2;
        }


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

            if(type == Types.TILETYPE.BOMB) {
                threats.add(new Vector2d(x, y)); // add the position of the bomb
            }else if(Types.TILETYPE.getAgentTypes().contains(type) &&
                    type.getKey() != gs.getPlayerId()) // maybe an enemmy
            {
                if(enemiesObs.contains(type)) // it is an enemy
                {
                    threats.add(new Vector2d(x, y)); // add the position of the enemy
                }
            }
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
