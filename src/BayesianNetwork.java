import java.util.HashMap;

public class BayesianNetwork {

    public HashMap<String,BayesianNode> networkNodes;
    private int numberOfAddOp;
    private int numberOfMulOp;



    public BayesianNetwork( ){
        this.networkNodes = new HashMap<>();
        this.numberOfAddOp  = 0;
        this.numberOfMulOp  = 0;
    }
    public void updateNetFromXml(String XmlFile){
        XmlParser.fileToNetwork(XmlFile,this.networkNodes);
    }

    public BayesianNode returnNodebyName(String name){
        return networkNodes.get(name);
    }
}
