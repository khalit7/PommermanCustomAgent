package players.CustomAgent;

import objects.Bomb;
import objects.GameObject;
import utils.Types;
import utils.Vector2d;

import java.util.*;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static utils.Utils.positionIsPassable;

public class Djikstra {
    public Djikstra() {



    }


    public static class Container
    {
        public HashMap<Types.TILETYPE, ArrayList<Vector2d> > items;
        public HashMap<Vector2d, Integer> dist;
        public HashMap<Vector2d, Vector2d> prev;

        Container() { }
    }


    public static Djikstra.Container dijkstra(Types.TILETYPE[][] board, Vector2d myPosition,
                                            ArrayList<GameObject> enemies, int depth){

        HashMap<Types.TILETYPE, ArrayList<Vector2d> > items = new HashMap<>();
        HashMap<Vector2d, Integer> dist = new HashMap<>();
        HashMap<Vector2d, Vector2d> prev = new HashMap<>();
        Random random = new Random(10);
        Queue<Vector2d> Q = new LinkedList<>();

        for(int r = max(0, myPosition.x - depth); r < min(board.length, myPosition.x + depth); r++){
            for(int c = max(0, myPosition.y - depth); c < min(board.length, myPosition.y + depth); c++){

                Vector2d position = new Vector2d(r, c);

                // Determines if two points are out of range of each other.
                boolean out_of_range = (abs(c - myPosition.y) + abs(r - myPosition.x)) > depth;
                if(out_of_range)
                    continue;

                Types.TILETYPE itemType = board[c][r];
                boolean positionInItems = (itemType == Types.TILETYPE.FOG ||
                        itemType == Types.TILETYPE.RIGID || itemType == Types.TILETYPE.FLAMES);
                if(positionInItems)
                    continue;

                ArrayList<Vector2d> itemsTempList = items.get(itemType);
                if(itemsTempList == null) {
                    itemsTempList = new ArrayList<>();
                }
                itemsTempList.add(position);
                items.put(itemType, itemsTempList);

                if(position.equals(myPosition)){
                    Q.add(position);
                    dist.put(position, 0);
                }
                else{
                    dist.put(position, Integer.MAX_VALUE);
                }
            }
        }


        while(!Q.isEmpty()){
            Vector2d position = Q.remove();

            if(positionIsPassable(board, position, enemies)){
                int val = dist.get(position) + 1;

                //Types.DIRECTIONS[] directionsToBeChecked = Types.DIRECTIONS.values();
                Types.DIRECTIONS[] directionsToBeChecked = {Types.DIRECTIONS.LEFT, Types.DIRECTIONS.RIGHT,
                        Types.DIRECTIONS.UP, Types.DIRECTIONS.DOWN};

                for (Types.DIRECTIONS directionToBeChecked : directionsToBeChecked) {

                    Vector2d direction = directionToBeChecked.toVec();
                    Vector2d new_position = new Vector2d(position.x + direction.x, position.y + direction.y);

                    if(!dist.containsKey(new_position))
                        continue;

                    int dist_val = dist.get(new_position);

                    if(val < dist_val){
                        dist.put(new_position, val);
                        prev.put(new_position, position);
                        Q.add(new_position);
                    }
                    else if(val == dist_val && random.nextFloat() < 0.5){
                        dist.put(new_position, val);
                        prev.put(new_position, position);
                    }
                }
            }
        }

        Djikstra.Container container = new Djikstra.Container();
        container.dist = dist;
        container.items = items;
        container.prev = prev;

        return container;
    }
}
