import java.util.Map;

public class Ex1 {

    public static void main(String[] args) {
        BayesianNetwork net = new BayesianNetwork();
        net.updateNetFromXml("alarm_net.xml");
        System.out.println(net.networkNodes.size());
        for(Map.Entry<String,BayesianNode> set: net.networkNodes.entrySet()){
            System.out.println(set.getValue().name+" : "+set.getValue().outComes+ " cpt = "+set.getValue().cpt+
                    " Children : "+set.getValue().children+" Parents :"+set.getValue().parents);
        }
    }

}
