package players.PommermanCustomAgent;

import core.GameState;
import objects.Bomb;
import objects.GameObject;
import players.SimplePlayer;
import players.optimisers.ParameterizedPlayer;
import players.Player;
import utils.ElapsedCpuTimer;
import utils.Types;
import utils.Vector2d;

import java.beans.VetoableChangeListener;
import java.util.*;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static utils.Utils.directionToAction;
import static utils.Utils.getDirection;

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

    private boolean justLayedBoomb=false;
    private Boolean collectMorePowerUps=true;


    public CustomPlayer(long seed, int id) {
        this(seed, id, new CustomMCTSParams());
    }

    public CustomPlayer(long seed, int id, CustomMCTSParams params) {
        super(seed, id, params);
        reset(seed, id);

        ArrayList<Types.ACTIONS> actionsList = Types.ACTIONS.all();
        actions = new Types.ACTIONS[actionsList.size()];
        int i = 0;
        for (Types.ACTIONS act : actionsList) {
            actions[i++] = act;
        }
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
        System.out.println(objective);
        switch (objective){
            case 0: // trapped .. do nothing
                return Types.ACTIONS.ACTION_STOP;
                case 1: // pure_power_up_collection
                    return ruleBasedAction(gs);
            case 2: // endgame tactic
                params.rollout_depth=12;
                params.heuristic_method = params.ADVANCED_HEURISTIC;
                break;
            case 3:
                params.rollout_depth=10;
                params.heuristic_method = params.MULTI_OBJECTIVE_HEURISTIC;

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
    public Types.ACTIONS ruleBasedAction(GameState gs){
        // get all power up positions
        Vector2d myPosition = gs.getPosition();

        Types.TILETYPE[][] board = gs.getBoard();
        int ammo = gs.getAmmo();
        ArrayList<Types.TILETYPE> enemiesObs = gs.getAliveEnemyIDs();

        int boardSizeX = board.length;
        int boardSizeY = board[0].length;
        ArrayList<Vector2d> powerUpsPosition = new ArrayList<>();
        ArrayList<Vector2d> woodsPosition = new ArrayList<>();
        ArrayList<Vector2d> obsticalsPositions = new ArrayList<>();

        for (int x = 0; x < boardSizeX; x++) {
            for (int y = 0; y < boardSizeY; y++) {

                Types.TILETYPE type = board[y][x];
                if (type == Types.TILETYPE.EXTRABOMB ||
                        type == Types.TILETYPE.INCRRANGE ||
                        type == Types.TILETYPE.KICK) {
                    powerUpsPosition.add(new Vector2d(x,y));
                }
                else if (type == Types.TILETYPE.WOOD) woodsPosition.add(new Vector2d(x,y));
                else if (type == Types.TILETYPE.RIGID) obsticalsPositions.add(new Vector2d(x,y));
            }
        }
        //
        this.collectMorePowerUps = woodsPosition.size()>0 || powerUpsPosition.size()>0;
        //
        if (powerUpsPosition.size() > 0){
            // pickup powerup
            Vector2d closestPowerUp = getClosestItem(myPosition,powerUpsPosition);
            if(closestPowerUp.dist(myPosition)<7) {
                ArrayList<Vector2d> allObsticals = new ArrayList<Vector2d>();
                allObsticals.addAll(obsticalsPositions);
                allObsticals.addAll(woodsPosition);
                HashMap<Vector2d, Vector2d> prev = getNextNodeToDestination(closestPowerUp, myPosition, allObsticals, board);
                if (prev.get(myPosition) == null){ // if there is a path
                    while (!prev.get(closestPowerUp).equals(myPosition)) {
                        closestPowerUp = prev.get(closestPowerUp);
                    }

                return directionToAction(getDirection(myPosition, closestPowerUp));
            }
            }

        }
        // else
        if (woodsPosition.size()==0) {
            return Types.ACTIONS.ACTION_STOP; //should never reach this line
        }
            // go to wood
            Vector2d closestWood = getClosestItem(myPosition,woodsPosition);
            if(closestWood.dist(myPosition)!=1.0) {
                HashMap<Vector2d,Vector2d> prev = getNextNodeToDestination(closestWood,myPosition,obsticalsPositions,board);
                while (!prev.get(closestWood).equals(myPosition)) {
                    closestWood = prev.get(closestWood);
                }

                return directionToAction(getDirection(myPosition, closestWood));
            }
            else {
                //lay bomb to destroy wood
                this.justLayedBoomb = true;
                return Types.ACTIONS.ACTION_BOMB;
            }
        }
ArrayList<Vector2d> path = new ArrayList<>();
    private HashMap getNextNodeToDestination(Vector2d node , Vector2d myposition , ArrayList<Vector2d> obsticals , Types.TILETYPE[][] board){
        HashMap<Vector2d, Vector2d> prev = new HashMap<Vector2d, Vector2d>();
        ArrayList<Vector2d> allNodesExpanded = new ArrayList<Vector2d>();
        ArrayList<Vector2d> visited = new ArrayList<Vector2d>();
        ArrayList<Vector2d> q = new ArrayList<Vector2d>();
        //
        q.add(myposition);
        while (q.size()>0) {
            //1. pop last element in queue
            int elementToPopIndex = 0; // BFS
            Vector2d popedElement = q.get(elementToPopIndex);
            q.remove(elementToPopIndex);
            // 2. mark this element as visited
            visited.add(popedElement);
            // 3. check if this element is the destination
            if (popedElement.equals(node)) {
                // found solution
                return prev;
            }
            // 4. expand nearby elements if they haven't been visited and are not obstacles and in bound
            Vector2d right = new Vector2d(popedElement.x+1,popedElement.y);
            Vector2d left = new Vector2d(popedElement.x-1,popedElement.y);
            Vector2d up = new Vector2d(popedElement.x,popedElement.y+1);
            Vector2d down = new Vector2d(popedElement.x,popedElement.y-1);
            ArrayList<Vector2d> children = new ArrayList<Vector2d>();
            children.add(right);
            children.add(left);
            children.add(up);
            children.add(down);
            for(Vector2d child:children) {
                if (validTile(child,obsticals,board) && !visited.contains(child)) {
                    q.add(child);
                    prev.put(child,popedElement);
                }
            }

        }
        // no solution found
        HashMap<Vector2d,Vector2d> failsaif = new HashMap<Vector2d,Vector2d>(); // incase a path wasn't found
        failsaif.put(myposition,new Vector2d(-1,-1));
        return failsaif;
    }
    private Boolean validTile (Vector2d tile, ArrayList<Vector2d> obstacles,Types.TILETYPE[][] board) {
        if(tile.x >= 0 && tile.y >= 0 && tile.x < board[0].length && tile.y < board.length && !obstacles.contains(tile))  // in board and not obstical
        {
            return true;
        }
        else return false;
    }
    private Vector2d getClosestItem(Vector2d myPosition,ArrayList<Vector2d>items) {
        double minDistance = 1000; // arbitrary large value
        int minDistanceindex=0;
        for (int i=0;i<items.size();i++) {
            double distance = myPosition.dist(items.get(i));
            if(distance<minDistance) {
                minDistance = distance;
                minDistanceindex = i;
            }
        }

        return items.get(minDistanceindex);
    }
    private boolean isTrapped (Vector2d myposition ,Types.TILETYPE[][] board ) {
        int borardX = board[0].length;
        int borardY = board[1].length;
        Vector2d right = new Vector2d(myposition.x+1,myposition.y);
        Vector2d left = new Vector2d(myposition.x-1,myposition.y);
        Vector2d up = new Vector2d(myposition.x,myposition.y+1);
        Vector2d down = new Vector2d(myposition.x,myposition.y-1);
        ArrayList<Vector2d> allDirections = new ArrayList<Vector2d>();
        allDirections.add(right);
        allDirections.add(left);
        allDirections.add(up);
        allDirections.add(down);
        int counter = 0 ;
        for (Vector2d dir : allDirections) {
            if (dir.x >= 0 && dir.y >= 0 && dir.x < board[0].length && dir.y < board.length) {
                Types.TILETYPE directionTile = board[dir.y][dir.x];
                Boolean blocked = directionTile == Types.TILETYPE.FLAMES || directionTile == Types.TILETYPE.RIGID || directionTile== Types.TILETYPE.WOOD;
                if (blocked) counter+=1;
            }else {
                counter+=1;
            }
        }

        if (counter == 4) return true;
        else return false;

    }
    private int identifyObjective(GameState gs) {
        // if trapped.. stay still
        Types.TILETYPE[][] board = gs.getBoard();
        Vector2d myposition = gs.getPosition();
        if (isTrapped(myposition,board))
            return 0;
        //0: dont move
        // 1:pure_powerUpCollection
        // 2: general
        // 3: pure safety

        int safetythreshold = gs.getBlastStrength();
        System.out.print("safety threshold "); System.out.println(safetythreshold);
        Boolean is_safe = evaluateSafety(gs,safetythreshold); // returns True if no threat is nearby, false otherwise
        //System.out.println(is_safe);
        if (is_safe&& collectMorePowerUps) // if safe and there are still power ups to collect .. objective => pure power up collection
            return 1;
        else if (!is_safe && collectMorePowerUps) // if not safe and there are still power ups to collect .. objective => pure safety
            return 3;
        else  // general
            return 2; // reset this to 3




    }
private boolean evaluateSafety(GameState gs,int safetythreshold) {
        if(this.justLayedBoomb) {
            this.justLayedBoomb=false;
            return false;
        }
    Vector2d myPosition = gs.getPosition();

    Types.TILETYPE[][] board = gs.getBoard();

    int boardSizeX = board.length;
    int boardSizeY = board[0].length;
    ArrayList<Types.TILETYPE> enemiesObs = gs.getAliveEnemyIDs();

    ArrayList<Vector2d> threats = new ArrayList<>(); // either a bomb or flame

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

    // check if there is a nearby threat
    double closestThreatDistance = 1000 ;// very large number
    for (int i=0;i<threats.size();i++) {
        double distance = myPosition.dist(threats.get(i));
        if (distance < closestThreatDistance) closestThreatDistance = distance;
    }
    if (Math.floor(closestThreatDistance)<=safetythreshold) return false; // NOT SAFE .. SOME THREAT IS NEAR !!
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
        return new CustomPlayer(seed, playerID, params);
    }

}