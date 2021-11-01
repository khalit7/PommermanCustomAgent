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
    private int safetythreshold=10;

    private boolean justLayedBoomb=false;

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
                    return ruleBasedAction(gs);
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

        for (int x = 0; x < boardSizeX; x++) {
            for (int y = 0; y < boardSizeY; y++) {

                Types.TILETYPE type = board[y][x];
                if (type == Types.TILETYPE.EXTRABOMB ||
                        type == Types.TILETYPE.INCRRANGE ||
                        type == Types.TILETYPE.KICK) {
                    powerUpsPosition.add(new Vector2d(x,y));
                }
                else if (type == Types.TILETYPE.WOOD) woodsPosition.add(new Vector2d(x,y));

            }
        }
        if (powerUpsPosition.size() > 0){
            // pickup powerup
            Vector2d closestPowerUp = getClosestItem(myPosition,powerUpsPosition);
            if(closestPowerUp.dist(myPosition)<7) {
                Vector2d nextNode = getNextNodeToDestination(closestPowerUp,myPosition);
                return directionToAction(getDirection(myPosition, closestPowerUp));
            }

        }
        // else
            // go to wood
            Vector2d closestWood = getClosestItem(myPosition,woodsPosition);
            if(closestWood.dist(myPosition)>1) {
                Vector2d nextNode = getNextNodeToDestination(closestWood,myPosition);
                return directionToAction(getDirection(myPosition, closestWood));
            }
            else {
                //lay bomb to destroy wood
                this.justLayedBoomb = true;
                return Types.ACTIONS.ACTION_BOMB;
            }
        }
ArrayList<Vector2d> path = new ArrayList<>();
    private Vector2d getNextNodeToDestination(Vector2d node , Vector2d myposition){

        if(myposition.x<0 || myposition.y<0 || myposition.x>11 || myposition.y>11){
            // out of board
            return path.get(path.size()-1) ;
        }
        path.add(new Vector2d(myposition.x-1,myposition.y))
        getNextNodeToDestination(node,new Vector2d(myposition.x-1,myposition.y) );
        path.remove(path.size()-1);
        getNextNodeToDestination(node,new Vector2d(myposition.x+1,myposition.y) );
        getNextNodeToDestination(node,new Vector2d(myposition.x,myposition.y-1) );
        getNextNodeToDestination(node,new Vector2d(myposition.x-1,myposition.y+1));
        return new Vector2d();
    }
    private Vector2d getClosestItem(Vector2d myPosition,ArrayList<Vector2d>items) {
        // BFS
        List<Vector2d> q = new ArrayList<>();
        q.add(myPosition);
        List<Vector2d> visited = new ArrayList<>();
        for (int i =0;i<11;i++){
            for (int j=0;j<11;j++){

            }
        }
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
        if(this.justLayedBoomb) {
            this.justLayedBoomb=false;
            return false;
        }
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


/*
// 1) Initialise the required information off GameState
        Vector2d myPosition = gs.getPosition();

        Types.TILETYPE[][] board = gs.getBoard();
        int[][] bombBlastStrength = gs.getBombBlastStrength();
        int[][] bombLife = gs.getBombLife();

        int ammo = gs.getAmmo();
        int blastStrength = gs.getBlastStrength();

        ArrayList<Types.TILETYPE> enemiesObs = gs.getAliveEnemyIDs();

        int boardSizeX = board.length;
        int boardSizeY = board[0].length;

        ArrayList<Bomb> bombs = new ArrayList<>();
        ArrayList<GameObject> enemies = new ArrayList<>();

        for (int x = 0; x < boardSizeX; x++) {
            for (int y = 0; y < boardSizeY; y++) {

                Types.TILETYPE type = board[y][x];

                if(type == Types.TILETYPE.BOMB || bombBlastStrength[y][x] > 0){
                    // Create bomb object
                    Bomb bomb = new Bomb();
                    bomb.setPosition(new Vector2d(x, y));
                    bomb.setBlastStrength(bombBlastStrength[y][x]);
                    bomb.setLife(bombLife[y][x]);
                    bombs.add(bomb);
                }
                else if(Types.TILETYPE.getAgentTypes().contains(type) &&
                        type.getKey() != gs.getPlayerId()){ // May be an enemy
                    if(enemiesObs.contains(type)) { // Is enemy
                        // Create enemy object
                        GameObject enemy = new GameObject(type);
                        enemy.setPosition(new Vector2d(x, y));
                        enemies.add(enemy); // no copy needed
                    }
                }
            }
        }
        //
        int depth = 10;
        ArrayList<GameObject> powerUps = new ArrayList<>();
        Djikstra.Container from_dijkstra = Djikstra.dijkstra(board, myPosition, bombs, enemies, 10);
        HashMap<Types.TILETYPE, ArrayList<Vector2d>> items = from_dijkstra.items;
        HashMap<Vector2d, Integer> dist = from_dijkstra.dist;
        HashMap<Vector2d, Vector2d> prev = from_dijkstra.prev;
        //  Move towards a pickup if there is one within two reachable spaces.
        Iterator it;
        it = items.entrySet().iterator();
        Vector2d previousNode = new Vector2d(-1, -1); // placeholder, these values are not actually used
        int distance = Integer.MAX_VALUE;
        while (it.hasNext()){
            Map.Entry<Types.TILETYPE, ArrayList<Vector2d> > entry = (Map.Entry)it.next();
            // check pickup entries on the board
            if (Types.TILETYPE.getPowerUpTypes().contains(entry.getKey())){
                // no need to store just get closest
                for (Vector2d coords: entry.getValue()){
                    if (dist.get(coords) < distance){
                        distance = dist.get(coords);
                        previousNode = coords;
                    }
                }
            }
        }
        if (distance <= 2){
            // iterate until we get to the immadiate next node
            if (myPosition.equals(previousNode)){
                return directionToAction(getDirection(myPosition, previousNode));
            }else
            while (!myPosition.equals(prev.get(previousNode))){ ;
                previousNode = prev.get(previousNode);
            }
            return directionToAction(getDirection(myPosition, previousNode));
        }
        // 6) Maybe lay a bomb if we are within a space of a wooden wall.
        it = items.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Types.TILETYPE, ArrayList<Vector2d>> entry = (Map.Entry) it.next();
            // check pickup entries on the board
            if (entry.getKey().equals(Types.TILETYPE.WOOD) ) {
                // check the distance from the wooden planks
                for (Vector2d coords: entry.getValue()){
                    if (dist.get(coords) == 1){
                        this.justLayedBoomb = true;
                            return Types.ACTIONS.ACTION_BOMB;

                    }
                }
                // 7) Move towards a wooden wall if there is one within two reachable spaces and you have a bomb.
                if (ammo < 1) continue;
                for (Vector2d coords:entry.getValue()){
                    // max 2 reachable space
                    if (dist.get(coords) <= 2){
                        previousNode = coords;
                        while (!myPosition.equals(prev.get(previousNode))){
                            previousNode = prev.get(previousNode);
                        }
                        Types.DIRECTIONS direction = getDirection(myPosition, previousNode);
                        if (direction != null){
                            ArrayList<Types.DIRECTIONS> dirArray = new ArrayList<>();
                            dirArray.add(direction);

                            if (dirArray.size() > 0){
                                return directionToAction(dirArray.get(0));
                            }
                        }


                    }
                }
            }
        }

// 8) Choose a random but valid direction.
        ArrayList<Types.DIRECTIONS> directions = new ArrayList<>();
        directions.add(Types.DIRECTIONS.UP);
        directions.add(Types.DIRECTIONS.DOWN);
        directions.add(Types.DIRECTIONS.LEFT);
        directions.add(Types.DIRECTIONS.RIGHT);
        ArrayList<Types.DIRECTIONS> validDirections = filterInvalidDirections(board, myPosition, directions, enemies);
        validDirections = filterUnsafeDirections(myPosition, validDirections, bombs );
        validDirections = filterRecentlyVisited(validDirections, myPosition, this.recentlyVisitedPositions);

        // 9) Add this position to the recently visited uninteresting positions so we don't return immediately.
        recentlyVisitedPositions.add(myPosition);
        if (recentlyVisitedPositions.size() > recentlyVisitedLength)
            recentlyVisitedPositions.remove(0);

        if (validDirections.size() > 0){
            int actionIdx = random.nextInt(validDirections.size());
            return directionToAction(validDirections.get(actionIdx));
        }

        return Types.ACTIONS.ACTION_STOP;
 */