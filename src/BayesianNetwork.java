import java.text.DecimalFormat;
import java.util.*;

public class BayesianNetwork {

    public HashMap<String, BayesianNode> networkNodes;
    private int numberOfAddOp;
    private int numberOfMulOp;
    public static double numerator = 0;
    public static double denominator = 0;
    public static int counter = 0;


    public BayesianNetwork() {
        this.networkNodes = new HashMap<>();
        this.numberOfAddOp = 0;
        this.numberOfMulOp = 0;
    }

    public void updateNetFromXml(String XmlFile) {
        XmlParser.fileToNetwork(XmlFile, this.networkNodes);
    }

    public BayesianNode returnNodebyName(String name) {
        return networkNodes.get(name);
    }

    public static void resetAns() {
        numerator = 0;
        denominator = 0;
    }

    public String naiveAnsweringQuery(ArrayList<String> evidence, ArrayList<String> hidden, String queryVar, String outComequeryVar) {
        System.out.println("query - "+"P(B=T|J=T,M=T)");
        resetAns();
        ArrayList<Boolean> taken = new ArrayList<>(Arrays.asList(new Boolean[hidden.size()]));
        Collections.fill(taken, Boolean.FALSE);
        AllCombinationToCompute(evidence, hidden, taken, 0, queryVar, outComequeryVar, false);
        hidden.add(queryVar);
        evidence.remove(queryVar+"="+outComequeryVar);
        ArrayList<Boolean> taken2 = new ArrayList<>(Arrays.asList(new Boolean[hidden.size()]));
        Collections.fill(taken2, Boolean.FALSE);
        AllCombinationToCompute(evidence, hidden, taken2, 0, queryVar, outComequeryVar, true);
        String ans = (String.format("%.5f", numerator/(denominator+numerator)));
//       double final_ans = Double.parseDouble(String.format("%.5f", finalAns));
//        System.out.println(final_ans);

        return ans;
    }

    public void AllCombinationToCompute(ArrayList<String> str, ArrayList<String> hidden, ArrayList<Boolean> taken, int index, String queryVar, String outComequeryVar, Boolean flag) {
        if (index == hidden.size()) {
            if (!flag) {
                numerator += answerForOneRow(str);
            }else {
                System.out.println(str);
                denominator+= answerForOneRow(str);
            }
            return;
        }
        for (int i = index; i < hidden.size(); i++) {
            for (int j = 0; j < networkNodes.get(hidden.get(i)).outComes.size(); j++) {
                if (!flag) {
                    if (taken.get(i)) continue;
                    str.add(hidden.get(i) + "=" + networkNodes.get(hidden.get(i)).outComes.get(j));
                    taken.set(i, true);
                    AllCombinationToCompute(str, hidden, taken, index + 1,queryVar,outComequeryVar, flag);
                    str.remove(str.size() - 1);
                    taken.set(i, false);
                } else {
                    if (hidden.get(i).equals(queryVar) && networkNodes.get(hidden.get(i)).outComes.get(j).equals(outComequeryVar)) continue;
                    if (taken.get(i)) continue;
                    str.add(hidden.get(i) + "=" + networkNodes.get(hidden.get(i)).outComes.get(j));
                    taken.set(i, true);
                    AllCombinationToCompute(str, hidden, taken, index + 1,queryVar,outComequeryVar, flag);
                    str.remove(str.size() - 1);
                    taken.set(i, false);
                }
            }

        }

    }

    public double answerForOneRow(ArrayList<String> rowToCompute) {
        HashMap<String, String> helper = new HashMap<>();
        double ans = 0;
        boolean first = true;
        String keyToCompute;
        System.out.println(rowToCompute+" -check if its the right multiply ");
        System.out.println("---------------------------------");
        for (int i = 0; i < rowToCompute.size(); i++) {
            int indexForVar = rowToCompute.get(i).indexOf('=');
            String var = rowToCompute.get(i).substring(0, indexForVar);
            String varOutCome = rowToCompute.get(i).substring(indexForVar + 1);
            helper.put(var, varOutCome);
        }
        for (Map.Entry<String, String> set :
                helper.entrySet()) {
            keyToCompute = "P(";
            keyToCompute += set.getKey() + "=" + set.getValue();
            if (networkNodes.get(set.getKey()).parents.size() > 0) keyToCompute += '|';
            for (int i = 0; i < networkNodes.get(set.getKey()).parents.size(); i++) {
                String currParent = networkNodes.get(set.getKey()).parents.get(i);
                keyToCompute += currParent + "=" + helper.get(currParent);
                if (i < networkNodes.get(set.getKey()).parents.size() - 1) {
                    keyToCompute += ',';
                }
            }
            keyToCompute += ')';
                if (first) {

                    ans = networkNodes.get(set.getKey()).cpt.get(keyToCompute);
                    first = false;
                } else {
                    ans *= networkNodes.get(set.getKey()).cpt.get(keyToCompute);
                }
            System.out.println(keyToCompute+"="+networkNodes.get(set.getKey()).cpt.get(keyToCompute));

        }
        System.out.println("---------------------------------");
        return ans;

    }

}
