package players.PommermanCustomAgent;

import core.GameState;
import players.PommermanCustomAgent.heuristics.MultiObjectiveHeuristicCustom;
import players.PommermanCustomAgent.heuristics.CustomAdvancedHeuristic;
import players.PommermanCustomAgent.heuristics.CustomStateHeuristic;
import players.PommermanCustomAgent.heuristics.ProgressiveBiasHeuristics;
import utils.ElapsedCpuTimer;
import utils.Types;
import utils.Utils;
import utils.Vector2d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CustomSingleTreeNode
{

    public CustomMCTSParams params;

    private CustomSingleTreeNode parent;
    private CustomSingleTreeNode[] children;
    private double totValue;
    private int nVisits;
    private Random m_rnd;
    private int m_depth;
    private double[] bounds = new double[]{Double.MAX_VALUE, -Double.MAX_VALUE};
    private int childIdx;
    private int fmCallsCount;
    private int objective;

    private double selection_bias_value; // this will hold the value for progressive bias.

    private int num_actions;
    private Types.ACTIONS[] actions;

    private GameState rootState;
    private CustomStateHeuristic rootStateHeuristic;

    CustomSingleTreeNode(CustomMCTSParams p, Random rnd, int num_actions, Types.ACTIONS[] actions, int objective) {
        this(p, null, -1, rnd, num_actions, actions, 0, null,objective);
    }

    private CustomSingleTreeNode(CustomMCTSParams p, CustomSingleTreeNode parent, int childIdx, Random rnd, int num_actions,
                                 Types.ACTIONS[] actions, int fmCallsCount, CustomStateHeuristic sh, int objective) {
        this.params = p;
        this.fmCallsCount = fmCallsCount;
        this.parent = parent;
        this.m_rnd = rnd;
        this.num_actions = num_actions;
        this.actions = actions;
        children = new CustomSingleTreeNode[num_actions];
        totValue = 0.0;
        this.childIdx = childIdx;
        this.objective = objective;

        if(parent != null) {
            m_depth = parent.m_depth + 1;
            this.rootStateHeuristic = sh;
        }
        else
            m_depth = 0;
    }

    void setRootGameState(GameState gs)
    {
        this.rootState = gs;
//        if (params.heuristic_method == params.CUSTOM_HEURISTIC)
//            this.rootStateHeuristic = new CustomHeuristic(gs);
//        else if (params.heuristic_method == params.ADVANCED_HEURISTIC) // New method: combined heuristics
//            this.rootStateHeuristic = new AdvancedHeuristic(gs, m_rnd);
        if (params.heuristic_method == params.MULTI_OBJECTIVE_HEURISTIC)
            this.rootStateHeuristic = new MultiObjectiveHeuristicCustom(gs);
        else if (params.heuristic_method == params.ADVANCED_HEURISTIC)
            this.rootStateHeuristic = new CustomAdvancedHeuristic(gs,m_rnd);

    }


    void mctsSearch(ElapsedCpuTimer elapsedTimer) {

        double avgTimeTaken;
        double acumTimeTaken = 0;
        long remaining;
        int numIters = 0;

        int remainingLimit = 5;
        boolean stop = false;

        while(!stop){

            GameState state = rootState.copy();
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
            CustomSingleTreeNode selected = treePolicy(state);
            double delta = selected.rollOut(state);
            backUp(selected, delta);

            //Stopping condition
            if(params.stop_type == params.STOP_TIME) {
                numIters++;
                acumTimeTaken += (elapsedTimerIteration.elapsedMillis()) ;
                avgTimeTaken  = acumTimeTaken/numIters;
                remaining = elapsedTimer.remainingTimeMillis();
                stop = remaining <= 2 * avgTimeTaken || remaining <= remainingLimit;
            }else if(params.stop_type == params.STOP_ITERATIONS) {
                numIters++;
                stop = numIters >= params.num_iterations;
            }else if(params.stop_type == params.STOP_FMCALLS)
            {
                fmCallsCount+=params.rollout_depth;
                stop = (fmCallsCount + params.rollout_depth) > params.num_fmcalls;
            }
        }
        //System.out.println(" ITERS " + numIters);
    }

    private CustomSingleTreeNode treePolicy(GameState state) {

        CustomSingleTreeNode cur = this;

        while (!state.isTerminal() && cur.m_depth < params.rollout_depth)
        {
            if (cur.notFullyExpanded()) {
                return cur.expand(state); // expansion step

            } else {
                calculate_progressive_bias(cur); // assign progressive bias heuristic value for all children of cur
                cur = cur.uct(state); // selection step
            }
        }

        return cur;
    }

    private void calculate_progressive_bias(CustomSingleTreeNode currentNode)
    {
     // first calculate Dijkstra if needed
        // then select which progressive bias heuristic u r choosing
    }
    private CustomSingleTreeNode expand(GameState state) {

        int bestAction = 0;
        double bestValue = -1;

        for (int i = 0; i < children.length; i++) {
            double x = m_rnd.nextDouble();
            if (x > bestValue && children[i] == null) {
                bestAction = i;
                bestValue = x;
            }
        }
        //Roll the state
        roll(state, actions[bestAction]);

        CustomSingleTreeNode tn = new CustomSingleTreeNode(params,this,bestAction,this.m_rnd,num_actions,
                actions, fmCallsCount, rootStateHeuristic,this.objective);
        children[bestAction] = tn;
        return tn;
    }

    private void roll(GameState gs, Types.ACTIONS act)
    {
        //Simple, all random first, then my position.
        int nPlayers = 4;
        Types.ACTIONS[] actionsAll = new Types.ACTIONS[4];
        int playerId = gs.getPlayerId() - Types.TILETYPE.AGENT0.getKey();

        for(int i = 0; i < nPlayers; ++i)
        {
            if(playerId == i)
            {
                actionsAll[i] = act;
            }else {
                int actionIdx = m_rnd.nextInt(gs.nActions());
                actionsAll[i] = Types.ACTIONS.all().get(actionIdx);
            }
        }

        gs.next(actionsAll);

    }

    private CustomSingleTreeNode uct(GameState state) {
        CustomSingleTreeNode selected = null;
        double bestValue = -Double.MAX_VALUE;
        for (CustomSingleTreeNode child : this.children)
        {
            double hvVal = child.totValue;
            double childValue =  hvVal / (child.nVisits + params.epsilon);
            double progressive_bias = this.selection_bias_value/ (1+ child.nVisits);


            childValue = Utils.normalise(childValue, bounds[0], bounds[1]);

            double uctValue = childValue +
                    params.K * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + params.epsilon)) +
                    progressive_bias;

            uctValue = Utils.noise(uctValue, params.epsilon, this.m_rnd.nextDouble());     //break ties randomly

            // small sampleRandom numbers: break ties in unexpanded nodes
            if (uctValue > bestValue) {
                selected = child;
                bestValue = uctValue;
            }
        }
        if (selected == null)
        {
            throw new RuntimeException("Warning! returning null: " + bestValue + " : " + this.children.length + " " +
                    + bounds[0] + " " + bounds[1]);
        }

        //Roll the state:
        roll(state, actions[selected.childIdx]);

        return selected;
    }

    private double rollOut(GameState state)  {
        int thisDepth = this.m_depth;

        while (!finishRollout(state,thisDepth)) {
            //int action = safeRandomAction(state);
            int action = biasedAction(state); // biasing rollouts
            roll(state, actions[action]);
            thisDepth++;
        }

        return rootStateHeuristic.evaluateState(state,this.objective);
    }
private int biasedAction(GameState gs) {
    ArrayList<Types.ACTIONS> actionsToTry = getViableActions(gs); // returns actions that dosen't kill you nor make u try to go through the wall
    HashMap<Types.ACTIONS, Double> actions_Values = new HashMap<Types.ACTIONS, Double>();
        double max_value = -10000; // large arbitrary negative number
        for (int i =0; i<actionsToTry.size();i++){
            GameState gsCopy = gs.copy();
            roll(gsCopy,actionsToTry.get(i));
            double stateEvaluation = rootStateHeuristic.evaluateState(gsCopy,objective);
            actions_Values.put(actionsToTry.get(i),stateEvaluation);
            if (stateEvaluation > max_value) {
                max_value = stateEvaluation;
            }
        }
        // get all actions with value = max_value
    ArrayList<Types.ACTIONS> best_Actions = new  ArrayList<Types.ACTIONS>();
    for (Map.Entry<Types.ACTIONS, Double> entry : actions_Values.entrySet()) {
        Types.ACTIONS action = entry.getKey();
        double value = entry.getValue();
        if (value == max_value) best_Actions.add(action);
    }

    //chose randomly between best actions
        return best_Actions.get(m_rnd.nextInt(best_Actions.size())).getKey();
}
    private ArrayList<Types.ACTIONS> getViableActions(GameState state)
    {
        Types.TILETYPE[][] board = state.getBoard();
        ArrayList<Types.ACTIONS> allActions = Types.ACTIONS.all();
        int width = board.length;
        int height = board[0].length;

        for (int i=0;i<allActions.size();i++){

            Types.ACTIONS act = allActions.get(i);
            Vector2d dir = act.getDirection().toVec();

            Vector2d pos = state.getPosition();
            int x = pos.x + dir.x;
            int y = pos.y + dir.y;

            if (x >= 0 && x < width && y >= 0 && y < height)
                if(board[y][x] != Types.TILETYPE.FLAMES) {
                    continue;
                }

            allActions.remove(i);
        }
        return allActions;
    }

    private int safeRandomAction(GameState state)
    {
        Types.TILETYPE[][] board = state.getBoard();
        ArrayList<Types.ACTIONS> actionsToTry = Types.ACTIONS.all();
        int width = board.length;
        int height = board[0].length;

        while(actionsToTry.size() > 0) {

            int nAction = m_rnd.nextInt(actionsToTry.size());
            Types.ACTIONS act = actionsToTry.get(nAction);
            Vector2d dir = act.getDirection().toVec();

            Vector2d pos = state.getPosition();
            int x = pos.x + dir.x;
            int y = pos.y + dir.y;

            if (x >= 0 && x < width && y >= 0 && y < height)
                if(board[y][x] != Types.TILETYPE.FLAMES)
                    return nAction;

            actionsToTry.remove(nAction);
        }

        //Uh oh...
        return m_rnd.nextInt(num_actions);
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean finishRollout(GameState rollerState, int depth)
    {
        if (depth >= params.rollout_depth)      //rollout end condition.
            return true;

        if (rollerState.isTerminal())               //end of game
            return true;

        return false;
    }

    private void backUp(CustomSingleTreeNode node, double result)
    {
        CustomSingleTreeNode n = node;
        while(n != null)
        {
            n.nVisits++;
            n.totValue += result;
            if (result < n.bounds[0]) {
                n.bounds[0] = result;
            }
            if (result > n.bounds[1]) {
                n.bounds[1] = result;
            }
            n = n.parent;
        }
    }


    int mostVisitedAction() {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;
        boolean allEqual = true;
        double first = -1;

        for (int i=0; i<children.length; i++) {

            if(children[i] != null)
            {
                if(first == -1)
                    first = children[i].nVisits;
                else if(first != children[i].nVisits)
                {
                    allEqual = false;
                }

                double childValue = children[i].nVisits;
                childValue = Utils.noise(childValue, params.epsilon, this.m_rnd.nextDouble());     //break ties randomly
                if (childValue > bestValue) {
                    bestValue = childValue;
                    selected = i;
                }
            }
        }

        if (selected == -1)
        {
            selected = 0;
        }else if(allEqual)
        {
            //If all are equal, we opt to choose for the one with the best Q.
            selected = bestAction();
        }

        return selected;
    }

    public int bestAction()
    {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;

        for (int i=0; i<children.length; i++) {

            if(children[i] != null) {
                double childValue = children[i].totValue / (children[i].nVisits + params.epsilon);
                childValue = Utils.noise(childValue, params.epsilon, this.m_rnd.nextDouble());     //break ties randomly
                if (childValue > bestValue) {
                    bestValue = childValue;
                    selected = i;
                }
            }
        }

        if (selected == -1)
        {
            System.out.println("Unexpected selection!");
            selected = 0;
        }

        return selected;
    }


    private boolean notFullyExpanded() {
        for (CustomSingleTreeNode tn : children) {
            if (tn == null) {
                return true;
            }
        }

        return false;
    }
}
