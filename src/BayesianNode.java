import java.util.ArrayList;
import java.util.HashMap;

public class BayesianNode {
    public ArrayList<String> outComes;
    public ArrayList<String> parents;
    public ArrayList<String> children;
    public HashMap<String,Double> cpt;
    private String name;
    public BayesianNode(String name, ArrayList<String> outComes){
        this.name = name;
        this.cpt = new HashMap<>();
        this.parents = new ArrayList<>();
        this.children = new ArrayList<>();
        this.outComes = new ArrayList<>();
        this.outComes.addAll(outComes);
    }
    public void addParent(String parent){
        this.parents.add(parent);
    }
    public void addChild(String child){
        this.children.add(child);
    }
    public  String getName(){
        return this.name;
    }

}
